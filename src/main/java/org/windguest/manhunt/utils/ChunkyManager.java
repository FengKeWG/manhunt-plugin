package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.world.WorldManager;

public class ChunkyManager {
    private static final Main plugin = Main.getInstance();
    private static final long CMD_DELAY = 10L;

    public static void runStartCommand() {
        Location mainWorldSpawnLoc = WorldManager.getSpawnLocation();
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky cancel");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky confirm");
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky world world");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky radius 2000");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky center " + mainWorldSpawnLoc.getBlockX() + " " + mainWorldSpawnLoc.getBlockZ());
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky start");
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky world world_the_end");
                                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky radius 1000");
                                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky center 0 0");
                                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky start");
                                        }, CMD_DELAY);
                                    }, CMD_DELAY);
                                }, CMD_DELAY);
                            }, CMD_DELAY * 2);
                        }, CMD_DELAY);
                    }, CMD_DELAY);
                }, CMD_DELAY);
            }, CMD_DELAY);
        }, CMD_DELAY);
    }

    public static void runStopCommand() {
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky cancel");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), "chunky confirm");
        }, CMD_DELAY);
    }
}