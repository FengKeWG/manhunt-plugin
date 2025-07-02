package org.windguest.manhunt.world;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.windguest.manhunt.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class WorldManager {
    private static final Main plugin = Main.getInstance();
    private static final List<Location> glassBlocks = new ArrayList<>();
    private static Location spawnLocation = null;

    public static Location getSpawnLocation() {
        return spawnLocation;
    }

    public static void createHollowGlassCube(Location center, int size, int height) {
        World world = center.getWorld();
        if (world == null) return;
        int halfSize = size / 2;
        for (int x = -halfSize; x <= halfSize; x++) {
            for (int z = -halfSize; z <= halfSize; z++) {
                for (int y = 0; y < height; y++) {
                    Location loc = center.clone().add(x, y, z);
                    Block block = loc.getBlock();
                    boolean isEdge = (x == -halfSize || x == halfSize) ||
                            (y == 0 || y == height - 1) ||
                            (z == -halfSize || z == halfSize);
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

    private Logger getLogger() {
        return plugin.getLogger();
    }

    public void loadWorld() {
        if (Bukkit.getWorld("hub") == null) {
            WorldCreator wc = new WorldCreator("hub");
            Bukkit.createWorld(wc);
        }
    }

    public void getNearestNonOceanBiomeLocation() {
        World world = Bukkit.getWorld("world");
        int maxRadius = 10000;
        int step = 32;
        int x = 0, z = 0;
        int dx = 0, dz = -1;
        int maxI = (maxRadius / step) * (maxRadius / step) * 4;
        for (int i = 0; i < maxI; i++) {
            if ((-maxRadius <= x) && (x <= maxRadius) && (-maxRadius <= z) && (z <= maxRadius)) {
                int y = world.getHighestBlockYAt(x, z);
                Biome biome = world.getBiome(x, y + 1, z);
                if (!isOceanBiome(biome)) {
                    getLogger().info("找到非海洋生物群系: " + biome.name() + " 在 (" + x + ", " + y + ", " + z + ")");
                    spawnLocation = new Location(world, x, y, z);
                    return;
                }
            }
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) {
                int temp = dx;
                dx = -dz;
                dz = temp;
            }
            x += dx * step;
            z += dz * step;
        }
        int y = world.getHighestBlockYAt(0, 0);
        spawnLocation = new Location(world, 0, y, 0);
        getLogger().info("未找到非海洋生物群系，返回原点");
    }

    private boolean isOceanBiome(Biome biome) {
        return switch (biome) {
            case OCEAN, DEEP_OCEAN, FROZEN_OCEAN, COLD_OCEAN, LUKEWARM_OCEAN, WARM_OCEAN, DEEP_FROZEN_OCEAN,
                 DEEP_COLD_OCEAN, DEEP_LUKEWARM_OCEAN -> true;
            default -> false;
        };
    }
}
