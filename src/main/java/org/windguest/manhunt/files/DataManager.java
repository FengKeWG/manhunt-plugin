package org.windguest.manhunt.files;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;

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
                config.set("wins", 0);
                config.set("games", 0);
                config.set("kills", 0);
                config.set("deaths", 0);
                config.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updatePlayerData(Player player, String key) {
        UUID playerUUID = player.getUniqueId();
        File playerFile = new File(dataFolder + "/users", playerUUID + ".yml");
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            int value = config.getInt(key);
            config.set(key, (value + 1));
            try {
                config.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void createUsersFolder() {
        File usersFolder = new File(dataFolder, "users");
        if (!usersFolder.exists()) {
            usersFolder.mkdirs();
        }
    }

    public int getPlayerData(Player player, String key) {
        File playerFile = new File(dataFolder + "/users", player.getUniqueId() + ".yml");
        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            return config.getInt(key);
        }
        return 0;
    }

    public double getPlayerKDR(Player player) {
        int kills = getPlayerData(player, "kills");
        int deaths = getPlayerData(player, "deaths");
        if (deaths == 0) deaths = 1;
        return (double) kills / deaths;
    }

    public double getPlayerWR(Player player) {
        int wins = getPlayerData(player, "wins");
        int games = getPlayerData(player, "games");
        if (games == 0) return 0.0;
        return (double) wins / games;
    }

    public double calculatePlayerScore(Player player) {
        double kdr = getPlayerKDR(player);
        double wr = getPlayerWR(player);
        double games = getPlayerData(player, "games");
        if (games == 0) games = 1;
        return Math.min(Math.log(games) / Math.log(2), 20) + Math.min(kdr * 30, 45) + Math.min(wr * 50, 35);
    }
}
