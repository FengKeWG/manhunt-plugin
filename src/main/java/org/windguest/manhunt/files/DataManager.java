package org.windguest.manhunt.files;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Mode;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class DataManager {
    private static final Main plugin = Main.getInstance();
    private static final File dataFolder = plugin.getDataFolder();

    public static void createPlayerFileIfNotExists(Player player) {
        UUID playerUUID = player.getUniqueId();
        File playerFile = new File(dataFolder + "/users", playerUUID + ".yml");
        if (!playerFile.exists()) {
            try {
                playerFile.createNewFile();
                YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
                for (Mode.GameMode mode : Mode.GameMode.values()) {
                    String modeKey = mode.name();
                    if (mode == Mode.GameMode.MANHUNT) {
                        for (String role : new String[] { "runner", "hunter" }) {
                            config.set(modeKey + "." + role + ".wins", 0);
                            config.set(modeKey + "." + role + ".games", 0);
                            config.set(modeKey + "." + role + ".kills", 0);
                            config.set(modeKey + "." + role + ".deaths", 0);
                        }
                    } else {
                        config.set(modeKey + ".wins", 0);
                        config.set(modeKey + ".games", 0);
                        config.set(modeKey + ".kills", 0);
                        config.set(modeKey + ".deaths", 0);
                    }
                }
                config.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void incrementPlayerData(Player player, Mode.GameMode mode, String key, int amount) {
        incrementPlayerData(player, mode, null, key, amount);
    }

    public static void incrementPlayerData(Player player, Mode.GameMode mode, String role, String key, int amount) {
        UUID playerUUID = player.getUniqueId();
        File playerFile = new File(dataFolder + "/users", playerUUID + ".yml");
        if (!playerFile.exists()) {
            createPlayerFileIfNotExists(player);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        String fullKey = mode.name() + "." + (role != null ? role + "." : "") + key;
        int value = config.getInt(fullKey, 0);
        config.set(fullKey, value + amount);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setPlayerData(Player player, Mode.GameMode mode, String role, String key, int value) {
        UUID playerUUID = player.getUniqueId();
        File playerFile = new File(dataFolder + "/users", playerUUID + ".yml");
        if (!playerFile.exists()) {
            createPlayerFileIfNotExists(player);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        String fullKey = mode.name() + "." + (role != null ? role + "." : "") + key;
        config.set(fullKey, value);
        try {
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createUsersFolder() {
        File usersFolder = new File(dataFolder, "users");
        if (!usersFolder.exists()) {
            usersFolder.mkdirs();
        }
    }

    public static int getPlayerData(Player player, Mode.GameMode mode, String key) {
        File playerFile = new File(dataFolder + "/users", player.getUniqueId() + ".yml");
        if (!playerFile.exists()) {
            createPlayerFileIfNotExists(player);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        if (mode == Mode.GameMode.MANHUNT) {
            int sum = 0;
            for (String role : new String[] { "runner", "hunter" }) {
                sum += config.getInt(mode.name() + "." + role + "." + key, 0);
            }
            return sum;
        }
        return config.getInt(mode.name() + "." + key, 0);
    }

    public static int getPlayerData(Player player, Mode.GameMode mode, String role, String key) {
        File playerFile = new File(dataFolder + "/users", player.getUniqueId() + ".yml");
        if (!playerFile.exists()) {
            createPlayerFileIfNotExists(player);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return config.getInt(mode.name() + "." + (role != null ? role + "." : "") + key, 0);
    }

    public static double getTotalPlayerKDR(Player player) {
        int totalKills = 0;
        int totalDeaths = 0;
        for (Mode.GameMode mode : Mode.GameMode.values()) {
            totalKills += getPlayerData(player, mode, "kills");
            totalDeaths += getPlayerData(player, mode, "deaths");
        }
        if (totalDeaths == 0)
            totalDeaths = 1;
        return (double) totalKills / totalDeaths;
    }

    public static double getTotalPlayerWR(Player player) {
        int totalWins = 0;
        int totalGames = 0;
        for (Mode.GameMode mode : Mode.GameMode.values()) {
            totalWins += getPlayerData(player, mode, "wins");
            totalGames += getPlayerData(player, mode, "games");
        }
        if (totalGames == 0)
            return 0.0;
        return (double) totalWins / totalGames;
    }

    public static double calculateTotalPlayerScore(Player player) {
        int totalWins = 0;
        int totalGames = 0;
        int totalKills = 0;
        int totalDeaths = 0;
        int runnerWins = getPlayerData(player, Mode.GameMode.MANHUNT, "runner", "wins");
        int runnerGames = getPlayerData(player, Mode.GameMode.MANHUNT, "runner", "games");
        int runnerKills = getPlayerData(player, Mode.GameMode.MANHUNT, "runner", "kills");
        for (Mode.GameMode mode : Mode.GameMode.values()) {
            totalWins += getPlayerData(player, mode, "wins");
            totalGames += getPlayerData(player, mode, "games");
            totalKills += getPlayerData(player, mode, "kills");
            totalDeaths += getPlayerData(player, mode, "deaths");
        }
        totalWins += runnerWins * 0.5;
        totalKills += runnerKills * 0.5;
        totalGames += runnerGames * 0.5;
        if (totalGames == 0) {
            return 0;
        }
        int priorGames = 20;
        int priorWins = 10;
        double wr = (double) (totalWins + priorWins) / (totalGames + priorGames);
        double wrScore = wr * 200;
        int priorKills = 10;
        int priorDeaths = 10;
        double kdr = (double) (totalKills + priorKills) / (totalDeaths + priorDeaths);
        double kdrScore = (Math.min(kdr, 5.0) / 5.0) * 200;
        double expScore = (Math.min(totalGames, 1000) / 1000.0) * 100;
        return wrScore + kdrScore + expScore;
    }
}
