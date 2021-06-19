package de.tzimom.siro.listeners;

import de.tzimom.siro.Main;
import de.tzimom.siro.utils.CustomPlayer;
import de.tzimom.siro.utils.Team;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class PlayerDeathEventListener implements Listener {

    private final Main plugin = Main.getInstance();

    @EventHandler
    public void handlePlayerDeathEvent(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        CustomPlayer.getPlayer(uuid).onDie();

        Bukkit.getOnlinePlayers().forEach(target -> target.playSound(player.getLocation(), Sound.AMBIENCE_THUNDER, 1f, 1f));

        Team winnerTeam = null;

        for (Team team : plugin.getGameManager().getTeamManager().getTeams()) {
            boolean multipleTeams = false;

            for (CustomPlayer member : team.getMembers()) {
                if (member == null) continue;
                if (member.isBanned()) continue;

                if (winnerTeam != null) {
                    multipleTeams = true;
                    winnerTeam = null;
                    break;
                }

                winnerTeam = team;
                break;
            }

            if (multipleTeams)
                break;
        }

        if (winnerTeam != null) {
            Bukkit.broadcastMessage(plugin.prefix + "§aDas Team §6" + winnerTeam.getTeamName() + " §ahat gewonnen");
            Bukkit.getOnlinePlayers().forEach(target -> target.playSound(target.getLocation(), Sound.LEVEL_UP, 1f, 1f));
            plugin.getGameManager().stopGame();
        }
    }

}
