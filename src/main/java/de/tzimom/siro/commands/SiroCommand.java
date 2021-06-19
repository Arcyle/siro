package de.tzimom.siro.commands;

import de.tzimom.siro.Main;
import de.tzimom.siro.managers.GameManager;
import de.tzimom.siro.managers.SpawnPointManager;
import de.tzimom.siro.utils.CustomPlayer;
import de.tzimom.siro.utils.Permission;
import de.tzimom.siro.utils.Usage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class SiroCommand implements CommandExecutor {

    private final Main plugin = Main.getInstance();
    private final GameManager gameManager = plugin.getGameManager();

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(Permission.COMMAND_SIRO)) {
            sender.sendMessage(plugin.prefix + plugin.noPermission);
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
            if (gameManager.getPlayers().size() > gameManager.getSpawnPointManager().getSpawns().size()) {
                sender.sendMessage(plugin.prefix + "§cEs gibt weniger Spawns als Spieler");
                return true;
            }

            if (!gameManager.startCountdown())
                sender.sendMessage(plugin.prefix + "§cDas Spiel läuft bereits");
            else
                sender.sendMessage("§aDer Countdown wurde gestartet");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("stop")) {
            if (!gameManager.stopGame())
                sender.sendMessage(plugin.prefix + "§cDas Spiel ist nicht gestartet");
        } else if (args.length == 1 && args[0].equalsIgnoreCase("addspawn")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.prefix + plugin.noPlayer);
                return true;
            }

            final Player player = (Player) sender;
            final Location location = player.getLocation();
            final SpawnPointManager spawnPointManager = gameManager.getSpawnPointManager();

            if (spawnPointManager.addSpawn(location))
                sender.sendMessage(plugin.prefix + "§7Der Spawn wurde hinzugefügt");
            else
                sender.sendMessage(plugin.prefix + "§cAuf dem Block ist bereits ein Spawn");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("removespawn")) {
            final SpawnPointManager spawnPointManager = gameManager.getSpawnPointManager();

            if (args[1].equalsIgnoreCase("*")) {
                if (spawnPointManager.removeAll())
                    sender.sendMessage(plugin.prefix + "§7Alle Spawns wurden entfernt");
                else
                    sender.sendMessage(plugin.prefix + "§cEs gibt keine Spawns");

                return true;
            }

            int id;

            try {
                id = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(plugin.prefix + "§c" + args[1] + " ist keine gültige Zahl");
                return true;
            }

            if (spawnPointManager.removeSpawn(id))
                sender.sendMessage(plugin.prefix + "§7Der Spawn mit der §6ID " + id + " §7wurde entfernt");
            else
                sender.sendMessage(plugin.prefix + "§cEs gibt keinen Spawn mit der ID " + id);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("spawns")) {
            final SpawnPointManager spawnPointManager = gameManager.getSpawnPointManager();
            final Map<Integer, Location> spawns = spawnPointManager.getSpawns();

            if (spawns.isEmpty()) {
                sender.sendMessage(plugin.prefix + "§cEs gibt keine Spawns");
            } else {
                sender.sendMessage(plugin.prefix + "§6§lSpawns:");
                spawns.forEach((id, location) -> spawnPointManager.display(id, location, sender));
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("ban")) {
            final String playerName = args[1];
            final Player player = Bukkit.getPlayer(playerName);

            if (player == null) {
                sender.sendMessage(plugin.prefix + "§cDer Spieler ist nicht online");
                return true;
            }

            UUID uuid = player.getUniqueId();
            CustomPlayer customPlayer = CustomPlayer.getPlayer(uuid);

            if (!plugin.getGameManager().isRunning()) {
                sender.sendMessage(plugin.prefix + "§cDas Spiel ist noch nicht gestartet");
                return true;
            }

            if (customPlayer.isBanned()) {
                sender.sendMessage(plugin.prefix + "§cDer Spieler ist bereits ausgeschieden");
                return true;
            }

            customPlayer.ban();
            sender.sendMessage(plugin.prefix + "§7Der Spieler §6" + player.getName() + " §7wurde aus dem Projekt ausgeschieden.");
        } else
            Usage.SIRO.send(sender);

        return true;
    }

}
