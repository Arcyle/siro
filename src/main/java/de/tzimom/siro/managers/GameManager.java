package de.tzimom.siro.managers;

import de.tzimom.siro.Main;
import de.tzimom.siro.utils.CustomPlayer;
import de.tzimom.siro.utils.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager extends FileManager {

    public static final LocalTime SERVER_OPENING_TIME = LocalTime.parse("07:00");
    public static final LocalTime SERVER_CLOSING_TIME = LocalTime.parse("23:00");

    private final Main plugin = Main.getInstance();
    private boolean running = false;

    private final PlayerManager playerManager = new PlayerManager();
    private final TeamManager teamManager = new TeamManager();
    private final BorderManager borderManager = new BorderManager();
    private final SpawnPointManager spawnPointManager = new SpawnPointManager();
    private final CountDown countDown = new CountDown();

    public GameManager() {
        if (getConfig().contains("running"))
            running = getConfig().getBoolean("running");

        if (getConfig().contains("passedDays"))
            borderManager.passedDays = getConfig().getInt("passedDays");

        if (getConfig().contains("closed"))
            borderManager.closed = getConfig().getBoolean("closed");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!running)
                return;

            Bukkit.getOnlinePlayers().forEach(player -> {
                final CustomPlayer customPlayer = CustomPlayer.getPlayer(player.getUniqueId());

                if (player.isOp() && customPlayer.getTeam() == null)
                    return;

                if (hasClosed())
                    customPlayer.kick("§cDer Server hat jetzt geschlossen. Er öffnet wieder um " +
                            SERVER_OPENING_TIME.toString() + " Uhr", false);

                customPlayer.playTimeCheck();
            });
        }, 0, 1);
    }

    protected void saveConfig() {
        getConfig().set("running", running);
        getConfig().set("passedDays", borderManager.passedDays);
        getConfig().set("closed", borderManager.closed);
        super.saveConfig();
    }

    public boolean startCountdown() {
        if (running || countDown.running)
            return false;

        final Collection<Location> spawns = spawnPointManager.getSpawns().values();
        final List<Location> spawnsSorted = spawns.stream().sorted(Comparator.comparing
                (spawnPointManager::distanceFromCenterSquared)).collect(Collectors.toList());

        for (Team team : teamManager.getTeams()) {
            if (spawnsSorted.isEmpty())
                break;

            System.out.println("Team: " + team.getTeamName());
            for (CustomPlayer customPlayer : team.getMembers()) {
                if (customPlayer == null)
                    continue;

                System.out.println("Team: " + team.getTeamName() + " " + customPlayer.getName());
            }
        }

        countDown.start();

        Bukkit.getOnlinePlayers().forEach(player -> CustomPlayer.getPlayer(player.getUniqueId()).prepare());

        Bukkit.getWorlds().forEach(world -> {
            world.setTime(0);
            world.setStorm(false);
            world.setThundering(false);

            world.getEntities().forEach(entity -> {
                if (!(entity instanceof Player))
                    entity.remove();
            });
        });

        return true;
    }

    private void startGame() {
        CustomPlayer.getCustomPlayers().forEach(player -> {
            player.reset();
            player.onPreLogin(false);
            player.playSound(Sound.LEVEL_UP);
        });

        running = true;

        CustomPlayer.getCustomPlayers().forEach(player -> {
            player.prepare();
            player.onJoin();
        });

        borderManager.reset();
        borderManager.start();
    }

    public boolean stopGame() {
        if (running) {
            running = false;
            borderManager.cancel();

            CustomPlayer.getCustomPlayers().forEach(player -> {
                player.reset();
                player.prepare();
            });

            Bukkit.broadcastMessage(plugin.prefix + "§cDas Spiel wurde beendet");
            return true;
        }

        if (countDown.running) {
            CustomPlayer.getCustomPlayers().forEach(player -> {
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

    public boolean isCountDownRunning() {
        return countDown.running;
    }

    public static long getCurrentDay() {
        long currentTime = System.currentTimeMillis();
        return (long) Math.floor(currentTime / 1000d / 60d / 60d / 24d);
    }

    public Set<CustomPlayer> getPlayers() {
        Set<CustomPlayer> players = new HashSet<>();

        teamManager.getTeams().forEach(team -> {
            for (CustomPlayer member : team.getMembers()) {
                if (member != null) players.add(member);
            }
        });

        return players;
    }

    public TeamManager getTeamManager() {
        return teamManager;
    }

    public SpawnPointManager getSpawnPointManager() {
        return spawnPointManager;
    }

    public BorderManager getBorderManager() {
        return borderManager;
    }

    private class CountDown {
        private static final int COUNTDOWN_TIME = 30;

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

    public class BorderManager {
        private static final int BLOCKS_PER_PLAYER_OVERWORLD = 350 * 350;
        private static final int BLOCKS_PER_PLAYER_NETHER = 200 * 200;
        private static final int FINAL_RADIUS = 400;
        private static final int SHRINK_INTERVAL = 3;
        private static final int TIME_TO_FINAL = 24;

        private final WorldBorder overworld;
        private final WorldBorder nether;

        private int task;
        private boolean running;

        private int startingOverworldRadius;

        private int passedDays;
        private boolean closed;

        private BorderManager() {
            overworld = Bukkit.getWorld("world").getWorldBorder();
            nether = Bukkit.getWorld("world_nether").getWorldBorder();
        }

        public void start() {
            if (running)
                return;

            running = true;

            task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (!closed && hasClosed()) {
                    passedDays ++;

                    if (passedDays >= TIME_TO_FINAL) {
                        cancel();
                        overworld.setSize(FINAL_RADIUS);
                        return;
                    }

                    if (passedDays % SHRINK_INTERVAL == 0) {
                        int shrink = (int) Math.floor((startingOverworldRadius - FINAL_RADIUS) / (double) TIME_TO_FINAL);
                        overworld.setSize(startingOverworldRadius - (shrink * passedDays));
                    }
                }

                closed = hasClosed();
            }, 0, 20 * 60);
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

            overworld.setCenter(0d, 0d);
            nether.setCenter(0d, 0d);

            overworld.setDamageAmount(1d);
            overworld.setDamageBuffer(0d);

            startingOverworldRadius = Math.max((int) Math.floor(Math.sqrt(BLOCKS_PER_PLAYER_OVERWORLD * playerCount)), FINAL_RADIUS);
            int radiusNether = Math.max((int) Math.floor(Math.sqrt(BLOCKS_PER_PLAYER_NETHER * playerCount)), FINAL_RADIUS);

            overworld.setSize(GameManager.this.running ? startingOverworldRadius : FINAL_RADIUS);
            nether.setSize(radiusNether);
        }
    }

    public String getFileName() {
        return "game";
    }

}
