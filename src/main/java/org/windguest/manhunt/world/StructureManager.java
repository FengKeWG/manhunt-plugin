package org.windguest.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class StructureManager {
    private static final Main plugin = Main.getInstance();
    private static final Map<StructureType, Set<Location>> nearestStructureCache = new ConcurrentHashMap<>();
    private static final Map<StructureType, Map<Player, Location>> playerNearestStructure = new ConcurrentHashMap<>();

    // 新增：初始化缓存并预加载要塞位置
    public static void init() {
        // 初始化两个缓存 Map
        for (StructureType type : StructureType.getStructureTypes().values()) {
            nearestStructureCache.putIfAbsent(type, ConcurrentHashMap.newKeySet());
            playerNearestStructure.putIfAbsent(type, new ConcurrentHashMap<>());
        }
        // 取消预加载要塞位置，避免启动延迟
    }

    private static List<Location> generatePredefinedLocations(World world) {
        List<Location> locations = new ArrayList<>();
        Location spawnLocation = WorldManager.getSpawnLocation();
        for (int n = 0; n <= 5; n++) {
            if (n == 0) {
                locations.add(spawnLocation.clone());
                continue;
            }
            for (int xOffset = -n; xOffset <= n; xOffset++) {
                for (int zOffset = -n; zOffset <= n; zOffset++) {
                    if (Math.abs(xOffset) != n && Math.abs(zOffset) != n) {
                        continue;
                    }
                    double x = spawnLocation.getX() + xOffset * 1000;
                    double z = spawnLocation.getZ() + zOffset * 1000;
                    double y = spawnLocation.getY();
                    Location loc = new Location(world, x, y, z);
                    locations.add(loc);
                }
            }
        }
        return locations;
    }

    public static void handleStructureUpdate(Player player, StructureType structureType, double distanceThreshold) {
        Location playerLocation = player.getLocation();
        Set<Location> sharedCache = nearestStructureCache.get(structureType);
        Map<Player, Location> playerCache = playerNearestStructure.get(structureType);
        Location cachedStructureLocation = null;
        double thresholdSquared = distanceThreshold * distanceThreshold;
        for (Location loc : sharedCache) {
            double distanceSquared = calculateXZDistanceSquared(playerLocation, loc);
            if (distanceSquared <= thresholdSquared) {
                cachedStructureLocation = loc;
                break;
            }
        }
        if (cachedStructureLocation != null) {
            playerCache.put(player, cachedStructureLocation);
        } else {
            Location foundStructure = player.getWorld().locateNearestStructure(playerLocation, structureType, 100,
                    false);
            if (foundStructure != null) {
                sharedCache.add(foundStructure);
                playerCache.put(player, foundStructure);
            } else {
                playerCache.remove(player);
            }
        }
    }

    public static void startNearestStructureUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    World world = player.getWorld();
                    World.Environment environment = world.getEnvironment();
                    switch (environment) {
                        case NORMAL:
                            handleStructureUpdate(player, StructureType.STRONGHOLD, 1500);
                            break;
                        case NETHER:
                            handleStructureUpdate(player, StructureType.NETHER_FORTRESS, 200);
                            handleStructureUpdate(player, StructureType.BASTION_REMNANT, 200);
                            break;
                        default:
                            break;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 6000L);
    }

    private static double calculateXZDistanceSquared(Location loc1, Location loc2) {
        double deltaX = loc1.getX() - loc2.getX();
        double deltaZ = loc1.getZ() - loc2.getZ();
        return deltaX * deltaX + deltaZ * deltaZ;
    }

    public static Location getNearestStructure(StructureType type, Player player) {
        Map<Player, Location> playerMap = playerNearestStructure.get(type);
        if (playerMap != null) {
            return playerMap.get(player);
        }
        return null;
    }

    private void initializeCaches() {
        for (StructureType type : StructureType.getStructureTypes().values()) {
            nearestStructureCache.put(type, new HashSet<>());
            playerNearestStructure.put(type, new ConcurrentHashMap<>());
        }
    }

    private Logger getLogger() {
        return plugin.getLogger();
    }

    public void prepopulateStrongholdCache() {
        new BukkitRunnable() {
            @Override
            public void run() {
                World overworld = Bukkit.getWorld("world");
                List<Location> generatedLocations = generatePredefinedLocations(overworld);
                for (Location loc : generatedLocations) {
                    Location searchLocation = loc.clone();
                    Location foundStructure = overworld.locateNearestStructure(searchLocation, StructureType.STRONGHOLD,
                            1500, false);
                    if (foundStructure != null) {
                        nearestStructureCache.get(StructureType.STRONGHOLD).add(foundStructure);
                        getLogger().info(String.format("已缓存要塞位置：查找位置 (%.0f, %.0f, %.0f) -> 要塞位置 (%.0f, %.0f, %.0f)",
                                searchLocation.getX(), searchLocation.getY(), searchLocation.getZ(),
                                foundStructure.getX(), foundStructure.getY(), foundStructure.getZ()));
                    } else {
                        getLogger().warning(String.format("在位置 (%.0f, %.0f, %.0f) 未找到要塞。", searchLocation.getX(),
                                searchLocation.getY(), searchLocation.getZ()));
                    }
                }
                getLogger().info("预先查找要塞并缓存完成。");
            }
        }.runTask(plugin);
    }
}
