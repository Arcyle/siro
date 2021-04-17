package de.tzimom.siro.managers;

import de.tzimom.siro.Main;
import de.tzimom.siro.utils.CustomPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import javax.swing.border.Border;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GameManager extends FileManager {

    public static final LocalTime SERVER_OPENING_TIME = LocalTime.parse("07:00");
    public static final LocalTime SERVER_CLOSING_TIME = LocalTime.parse("23:00");

    private static final int COUNTDOWN_TIME = 30;

    private final Main plugin = Main.getInstance();
    private boolean running = false;

    private final TeamManager teamManager = new TeamManager();
    private final PlayerManager playerManager = new PlayerManager();
    private final BorderManager borderManager = new BorderManager();
    private final CountDown countDown = new CountDown();

    public GameManager() {
        if (getConfig().contains("running"))
            running = getConfig().getBoolean("running");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!running)
                return;

            Bukkit.getOnlinePlayers().forEach(player -> {
                CustomPlayer customPlayer = CustomPlayer.getPlayer(player.getUniqueId());

                if (hasClosed())
                    customPlayer.kick("§cDer Server hat jetzt geschlossen. Er öffnet wieder um " +
                            SERVER_OPENING_TIME.toString() + " Uhr", false);

                customPlayer.playTimeCheck();
            });
        }, 0, 1);
    }

    protected void saveConfig() {
        getConfig().set("running", running);
        super.saveConfig();
    }

    public boolean startCountdown() {
        if (running || countDown.running)
            return false;

        countDown.start();
        return true;
    }

    private void startGame() {
        CustomPlayer.getCustomPlayers().forEach((uuid, player) -> {
            player.reset();
            player.onPreLogin(false);
            player.playSound(Sound.LEVEL_UP);
        });

        running = true;

        CustomPlayer.getCustomPlayers().forEach((uuid, player) -> {
            player.prepare();
            player.onJoin();
        });

        borderManager.start();
    }

    public boolean stopGame() {
        if (running) {
            CustomPlayer.getCustomPlayers().forEach((uuid, player) -> {
                player.reset();
                player.prepare();
            });

            borderManager.cancel();
            Bukkit.broadcastMessage(plugin.prefix + "§cDas Spiel wurde beendet");
            running = false;
            return true;
        }

        if (countDown.running) {
            CustomPlayer.getCustomPlayers().forEach((uuid, player) -> {
                player.reset();
                player.prepare();
                player.playSound(Sound.NOTE_BASS);
            });

            Bukkit.broadcastMessage(plugin.prefix + "§cDer Countdown wurde abgebrochen");
            countDown.cancel();
            return true;
        }

        return false;
    }

    public boolean hasClosed() {
        LocalTime time = LocalTime.now(ZoneId.of("CET"));
        return running && (time.isBefore(SERVER_OPENING_TIME) || time.isAfter(SERVER_CLOSING_TIME));
    }

    public boolean isRunning() {
        return running;
    }

    public static long getCurrentDay() {
        long currentTime = System.currentTimeMillis();
        return (long) Math.floor(currentTime / 1000d / 60d / 60d / 24d);
    }

    private Set<CustomPlayer> getPlayers() {
        Set<CustomPlayer> players = new HashSet<>();

        teamManager.getTeams().forEach(team -> {
            for (UUID member : team.getMembers())
                players.add(CustomPlayer.getPlayer(member));
        });

        return players;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    private class CountDown {
        private boolean running;
        private int task;
        private int timeLeft;

        private void start() {
            if (running)
                return;

            running = true;
            reset();

            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (timeLeft <= 0) {
                    Bukkit.broadcastMessage(plugin.prefix + "§aDas Spiel startet §6jetzt");

                    startGame();
                    cancel();
                    return;
                }

                Bukkit.getOnlinePlayers().forEach(player -> player.setLevel(timeLeft));

                if (timeLeft % 10 == 0 || timeLeft <= 5) {
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.sendMessage(plugin.prefix + "§aDas Spiel startet in §6" + timeLeft + " Sekunden");
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    });
                }

                timeLeft--;
            }, 0, 20);
        }

        private void cancel() {
            if (!running)
                return;

            running = false;
            Bukkit.getScheduler().cancelTask(task);
        }

        private void reset() {
            timeLeft = COUNTDOWN_TIME;
        }
    }

    private class BorderManager {
        private static final int BLOCKS_PER_PLAYER_OVERWORLD = 350 * 350;
        private static final int BLOCKS_PER_PLAYER_NETHER = 200 * 200;
        private static final int FINAL_RADIUS = 400;
        private static final int SHRINK_INTERVAL = 3;
        private static final int TIME_TO_FINAL = 24;

        private final WorldBorder overworld;
        private final WorldBorder nether;

        private int task;
        private boolean running;

        private int radiusOverworld;

        private int passedDays;
        private boolean closed;

        private BorderManager() {
            overworld = Bukkit.getWorld("world").getWorldBorder();
            nether = Bukkit.getWorld("world_nether").getWorldBorder();

            overworld.setCenter(0d, 0d);
            nether.setCenter(0d, 0d);
        }

        private void start() {
            if (running)
                return;

            running = true;
            reset();

            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!closed && hasClosed()) {
                    passedDays ++;

                    if (passedDays >= TIME_TO_FINAL) {
                        cancel();
                        overworld.setSize(FINAL_RADIUS);
                        return;
                    }

                    if (passedDays % SHRINK_INTERVAL == 0) {
                        int shrinkAmount = (int) Math.floor((radiusOverworld - FINAL_RADIUS) * (double) SHRINK_INTERVAL / TIME_TO_FINAL);
                        overworld.setSize(overworld.getSize() - shrinkAmount);
                    }
                }

                closed = hasClosed();
            }, 0, 20);
        }

        private void cancel() {
            if (!running)
                return;

            running = false;
            Bukkit.getScheduler().cancelTask(task);
        }

        private void reset() {
            passedDays = 0;
            closed = hasClosed();

            int playerCount = getPlayers().size();

            radiusOverworld = (int) Math.floor(Math.sqrt(BLOCKS_PER_PLAYER_OVERWORLD * playerCount));
            int radiusNether = (int) Math.floor(Math.sqrt(BLOCKS_PER_PLAYER_NETHER * playerCount));

            overworld.setSize(radiusOverworld);
            nether.setSize(radiusNether);
        }
    }

    public String getFileName() {
        return "game";
    }

}
