package de.tzimom.siro.managers;

import de.tzimom.siro.utils.CustomPlayer;

import java.util.UUID;

public class PlayerManager extends FileManager {

    public PlayerManager() {
        if (!getConfig().contains("players"))
            return;

        if (!getConfig().isConfigurationSection("players"))
            return;

        getConfig().getConfigurationSection("players").getKeys(false).forEach(key -> {
            try {
                String prefix = "players." + key + ".";

                CustomPlayer customPlayer = CustomPlayer.fromKey(key);

                getConfig().getConfigurationSection(prefix + "playTimes").getKeys(false).forEach(dayString -> {
                    Long day = Long.parseLong(dayString);
                    customPlayer.getPlayTimes().put(day, getConfig().getLong(prefix + "playTimes." + dayString));
                });

                customPlayer.setNextDay(getConfig().getBoolean(prefix + "nextDay"));
                customPlayer.setBanned(getConfig().getBoolean(prefix + "banned"));
            } catch (NumberFormatException | NullPointerException ignored) {
            }
        });
    }

    protected void saveConfig() {
        getConfig().set("players", null);

        CustomPlayer.getCustomPlayers().forEach((customPlayer) -> {
            String prefix = "players." + customPlayer.asKey() + ".";

            customPlayer.getPlayTimes().forEach((day, playTime) -> getConfig().set(prefix + "playTimes." + day, playTime));
            getConfig().set(prefix + "nextDay", customPlayer.isNextDay());
            getConfig().set(prefix + "banned", customPlayer.isBanned());
            getConfig().set(prefix + "name", customPlayer.getName());
        });

        super.saveConfig();
    }

    public String getFileName() {
        return "players";
    }

}
