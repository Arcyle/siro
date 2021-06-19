package de.tzimom.siro.commands;

import de.tzimom.siro.Main;
import de.tzimom.siro.utils.Team;
import de.tzimom.siro.utils.CustomPlayer;
import de.tzimom.siro.utils.Permission;
import de.tzimom.siro.utils.Usage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public class TeamCommand implements CommandExecutor {

    private final Main plugin = Main.getInstance();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permission.COMMAND_TEAM)) {
            sender.sendMessage(plugin.prefix + plugin.noPermission);
            return true;
        }

        if ((args.length == 3 || args.length == 4) && args[0].equalsIgnoreCase("create")) {
            String teamName = args[1];
            String[] membersStrings = new String[] { args[2], args.length == 4 ? args[3] : null };

            if (teamName.length() > Team.MAX_NAME_LENGTH) {
                sender.sendMessage(plugin.prefix + "§cDer Teamname darf nicht länger als " + Team.MAX_NAME_LENGTH +
                        " Zeichen sein");
                return true;
            }

            CustomPlayer[] members = new CustomPlayer[membersStrings.length];

            for (int i = 0; i < membersStrings.length; i++) {
                if (membersStrings[i] == null || membersStrings[i].isEmpty())
                    continue;

                CustomPlayer customPlayer = CustomPlayer.getPlayer(membersStrings[i]);

                if (plugin.getGameManager().getTeamManager().getTeam(teamName) != null) {
                    sender.sendMessage(plugin.prefix + "§cDas Team existiert bereits");
                    return true;
                }

                if (customPlayer.getTeam() != null) {
                    sender.sendMessage(plugin.prefix + "§cDer Spieler " + customPlayer.getName() + " ist bereits in einem Team: "
                            + customPlayer.getTeam().getTeamName());
                    return true;
                }

                members[i] = customPlayer;
            }

            if (members[0] == members[1])
                members[1] = null;

            Team team = new Team(teamName, members);
            plugin.getGameManager().getTeamManager().registerTeam(team);
            sender.sendMessage(plugin.prefix + "§7Das Team §6" + teamName + " §7wurde erstellt");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            String teamName = args[1];
            Team team = plugin.getGameManager().getTeamManager().getTeam(teamName);

            if (team == null) {
                sender.sendMessage(plugin.prefix + "§cDas Team existiert nicht");
                return true;
            }

            plugin.getGameManager().getTeamManager().deleteTeam(team);
            sender.sendMessage(plugin.prefix + "§cDas Team " + team.getTeamName() + " wurde gelöscht");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            Set<Team> teams = plugin.getGameManager().getTeamManager().getTeams();

            if (teams.isEmpty()) {
                sender.sendMessage(plugin.prefix + "§cEs gibt keine Teams");
            } else {
                sender.sendMessage(plugin.prefix + "§6§lTeams:");
                teams.forEach(team -> sender.sendMessage(plugin.prefix + "§8- §6" + team.getTeamName()));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("of")) {
            if (args[1].equalsIgnoreCase("*")) {
                Collection<? extends Player> players = Bukkit.getOnlinePlayers();

                if (players.isEmpty()) {
                    sender.sendMessage(plugin.prefix + "§cEs sind keine Spieler online");
                    return true;
                }

                sender.sendMessage(plugin.prefix + "§6§lSpieler Teams:");
                players.forEach(player -> {
                    final UUID uuid = player.getUniqueId();
                    final CustomPlayer customPlayer = CustomPlayer.getPlayer(uuid);
                    final Team team = customPlayer.getTeam();

                    sender.sendMessage(plugin.prefix + "§8- §b" + player.getName() + "§8: " +
                            (team == null ? "§cKein Team" : "§7" + team.getTeamName()));
                });

                return true;
            }

            final Player player = Bukkit.getPlayer(args[1]);

            if (player == null) {
                sender.sendMessage(plugin.prefix + "§cDer Spieler ist nicht online");
                return true;
            }

            final UUID uuid = player.getUniqueId();
            final CustomPlayer customPlayer = CustomPlayer.getPlayer(uuid);
            final Team team = customPlayer.getTeam();

            if (team == null) {
                sender.sendMessage(plugin.prefix + "§cDer Spieler ist in keinem Team");
                return true;
            }

            sender.sendMessage(plugin.prefix + "§6" + player.getName() + " §7ist im Team §b" + team.getTeamName());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("rename")) {
            final String teamName = args[1];
            final Team team = plugin.getGameManager().getTeamManager().getTeam(teamName);

            if (team == null) {
                sender.sendMessage(plugin.prefix + "§cDas Team existiert nicht");
                return true;
            }

            final String newTeamName = args[2];

            if (plugin.getGameManager().getTeamManager().getTeam(newTeamName) != null) {
                sender.sendMessage(plugin.prefix + "§cEin Team mit dem Namen " + teamName + " existiert bereits");
                return true;
            }

            if (newTeamName.length() > Team.MAX_NAME_LENGTH) {
                sender.sendMessage(plugin.prefix + "§cDer Teamname darf nicht länger als " + Team.MAX_NAME_LENGTH +
                        " Zeichen sein");
                return true;
            }

            team.rename(newTeamName);
            sender.sendMessage(plugin.prefix + "§7Das Team §6" + teamName + " §7wurde zu §6" + newTeamName + " §7umbenannt");
        } else {
            Usage.TEAM.send(sender);
        }

        return true;
    }

}
