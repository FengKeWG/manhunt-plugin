package org.windguest.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.windguest.manhunt.Main;

import java.util.Random;

public class EndLocationManager {
    private static final Main plugin = Main.getInstance();
    private static final Random random = new Random();
    private static Location redEndBase = null;
    private static Location blueEndBase = null;
    
    // 搜索半径（适当增大以容纳2000格外的区域）
    private static final int SEARCH_RADIUS = 3000;
    // 两队最小距离
    private static final int MIN_TEAM_DISTANCE = 2000;
    // 距离主岛最小距离（2000格）
    private static final int MIN_DIST_FROM_SPAWN = 2000;
    // 固定高度
    private static final int FIXED_Y = 365;

    public static Location getRedEndBase() { return redEndBase; }
    public static Location getBlueEndBase() { return blueEndBase; }

    public static void findEndBaseLocations() {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            plugin.getLogger().severe("末地世界不存在，无法寻找队伍基地！");
            return;
        }

        // 寻找红队基地，必须距离主岛 ≥2000
        redEndBase = findSuitableLocation(endWorld, 0, 0, SEARCH_RADIUS, 0, MIN_DIST_FROM_SPAWN);
        if (redEndBase == null) {
            plugin.getLogger().warning("未找到合适的红队基地，使用默认位置 (2000," + FIXED_Y + ",2000)");
            redEndBase = new Location(endWorld, 2000, FIXED_Y, 2000);
        }

        // 寻找蓝队基地，必须距离红队 ≥2000 且距离主岛 ≥2000
        blueEndBase = findSuitableLocation(endWorld,
                redEndBase.getBlockX(), redEndBase.getBlockZ(),
                SEARCH_RADIUS, MIN_TEAM_DISTANCE, MIN_DIST_FROM_SPAWN);
        if (blueEndBase == null) {
            plugin.getLogger().warning("未找到满足条件的蓝队基地，使用偏移位置");
            // 尝试在红队基础上偏移 ±2000，并检查是否满足距离主岛要求
            int[] offsets = {2000, -2000};
            for (int dx : offsets) {
                for (int dz : offsets) {
                    int x = redEndBase.getBlockX() + dx;
                    int z = redEndBase.getBlockZ() + dz;
                    if (Math.sqrt(x*x + z*z) >= MIN_DIST_FROM_SPAWN) {
                        blueEndBase = new Location(endWorld, x + 0.5, FIXED_Y, z + 0.5);
                        break;
                    }
                }
                if (blueEndBase != null) break;
            }
            if (blueEndBase == null) {
                blueEndBase = new Location(endWorld, -2000, FIXED_Y, -2000);
            }
        }

        plugin.getLogger().info("末地队伍基地已确定: 红队 " + format(redEndBase) +
                ", 蓝队 " + format(blueEndBase));
    }

    /**
     * 寻找合适位置
     * @param world 世界
     * @param avoidX 避开点的X坐标（用于队伍间距）
     * @param avoidZ 避开点的Z坐标
     * @param radius 搜索半径
     * @param minTeamDist 与避开点的最小距离
     * @param minSpawnDist 距离主岛(0,0)的最小距离
     */
    private static Location findSuitableLocation(World world, int avoidX, int avoidZ,
                                                 int radius, int minTeamDist, int minSpawnDist) {
        int attempts = 400; // 增加尝试次数
        for (int i = 0; i < attempts; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = random.nextDouble() * radius;
            int x = (int) (Math.cos(angle) * distance);
            int z = (int) (Math.sin(angle) * distance);
            x = Math.max(-radius, Math.min(radius, x));
            z = Math.max(-radius, Math.min(radius, z));
            
            // 检查距离主岛
            if (Math.sqrt(x*x + z*z) < minSpawnDist) continue;
            
            // 检查与避免点的距离
            if (minTeamDist > 0) {
                int dx = x - avoidX;
                int dz = z - avoidZ;
                if (dx * dx + dz * dz < minTeamDist * minTeamDist) continue;
            }
            
            return new Location(world, x + 0.5, FIXED_Y, z + 0.5);
        }
        return null;
    }

    private static String format(Location loc) {
        return "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}