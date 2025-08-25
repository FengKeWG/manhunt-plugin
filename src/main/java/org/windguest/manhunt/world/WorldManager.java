package org.windguest.manhunt.world;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.windguest.manhunt.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class WorldManager {
    private static final Main plugin = Main.getInstance();
    private static final List<Location> glassBlocks = new ArrayList<>();
    private static final Set<Biome> OCEAN_BIOMES = Set.of(Biome.OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN,
            Biome.COLD_OCEAN, Biome.LUKEWARM_OCEAN, Biome.WARM_OCEAN, Biome.DEEP_FROZEN_OCEAN, Biome.DEEP_COLD_OCEAN,
            Biome.DEEP_LUKEWARM_OCEAN);
    private static Location spawnLocation = null;

    public static Location getSpawnLocation() {
        return spawnLocation;
    }

    public static void setSpawnLocation(Location location) {
        spawnLocation = location;
    }

    public static void createHollowGlassCube(Location center, int size, int height) {
        World world = center.getWorld();
        if (world == null)
            return;
        int halfSize = size / 2;
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                for (int y = 0; y < height; y++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    boolean isEdge = (x == -halfSize || x == halfSize) || (y == 0 || y == height - 1)
                            || (z == -halfSize || z == halfSize);
                    if (isEdge) {
                        block.setType(Material.GLASS);
                        glassBlocks.add(loc);
                    }
                }
            }
        }
    }

    public static void breakGlassCube() {
        for (Location loc : glassBlocks) {
            World world = loc.getWorld();
            if (world != null) {
                world.spawnParticle(Particle.BLOCK, loc, 30, 0.5, 0.5, 0.5, 0.1, Material.GLASS.createBlockData());
                world.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                Block block = loc.getBlock();
                if (block.getType() == Material.GLASS) {
                    block.setType(Material.AIR);
                }
            }
        }
        glassBlocks.clear();
    }

    private static Logger getLogger() {
        return plugin.getLogger();
    }

    public static void getNearestNonOceanBiomeLocation() {
        World world = Bukkit.getWorld("world");
        if (world == null)
            return;

        int searchRadius = 1000;
        int attempts = 120;
        java.util.Random rand = new java.util.Random();

        Location center = world.getSpawnLocation();

        for (int i = 0; i < attempts; i++) {
            double angle = rand.nextDouble() * 2 * Math.PI;
            double distance = rand.nextDouble() * searchRadius;
            int x = center.getBlockX() + (int) (Math.cos(angle) * distance);
            int z = center.getBlockZ() + (int) (Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Biome biome = world.getBiome(x, y + 1, z);
            if (!isOceanBiome(biome)) {
                spawnLocation = new Location(world, x + 0.5, y, z + 0.5);
                getLogger().info("找到非海洋生物群系:  在 (" + x + ", " + y + ", " + z + ")");
                return;
            }
        }
        int y = world.getHighestBlockYAt(0, 0);
        spawnLocation = new Location(world, 0.5, y, 0.5);
        getLogger().warning("随机采样未找到非海洋生物群系，使用世界出生点");
    }

    private static boolean isOceanBiome(Biome biome) {
        return OCEAN_BIOMES.contains(biome);
    }

    public void loadWorld() {
        if (Bukkit.getWorld("hub") == null) {
            WorldCreator wc = new WorldCreator("hub");
            Bukkit.createWorld(wc);
        }
    }
}
