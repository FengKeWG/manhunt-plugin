package org.windguest.manhunt.world;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;
import org.popcraft.chunky.api.ChunkyAPI;
import org.windguest.manhunt.Main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ChunkyManager {
    private static final Main plugin = Main.getInstance();
    /**
     * 用于记录当前仍在进行中的任务世界列表。当该集合为空时，说明所有任务均已完成。
     */
    private static final Set<String> pendingTasks = new HashSet<>();
    private static final java.util.Map<String, Float> progressMap = new java.util.concurrent.ConcurrentHashMap<>();
    private static ChunkyAPI chunkyApi;
    private static BukkitRunnable progressLogger;
    private static boolean swapped = false;

    /**
     * 判断当前是否处于凌晨 02:00-07:00 的地图预生成窗口（北京时间）。
     */
    public static boolean isMaintenanceWindow() {
        ZoneId beijing = ZoneId.of("Asia/Shanghai");
        LocalTime now = LocalTime.now(beijing);
        return !now.isBefore(LocalTime.of(2, 0)) && now.isBefore(LocalTime.of(7, 0));
    }

    /**
     * 服务器启动时调用：若 data/maps 中有一份预生成地图，则同步移动到服务器根目录并加载。
     * 返回 true 表示已加载，false 表示未找到地图或加载失败。
     */
    public static boolean swapInPreGeneratedWorldIfExists() {
        if (swapped) return true;

        Path mapsDir = plugin.getDataFolder().toPath().resolve("maps");
        if (!Files.exists(mapsDir)) return false;

        Path batchDir;
        try (java.util.stream.Stream<Path> stream = Files.list(mapsDir)) {
            batchDir = stream.filter(Files::isDirectory).findFirst().orElse(null);
        } catch (IOException e) {
            return false;
        }
        if (batchDir == null) return false;

        String[] worlds = {"world", "world_nether", "world_the_end"};
        Path serverDir = Bukkit.getServer().getWorldContainer().toPath();

        try {
            plugin.getLogger().info("[ChunkyManager] 检测到预生成批次 " + batchDir.getFileName() + " , 开始替换世界...");
            for (String w : worlds) {
                Path src = batchDir.resolve(w);
                if (!Files.exists(src)) {
                    plugin.getLogger().warning("[ChunkyManager] 预生成批次缺少世界目录 " + src);
                    return false;
                }
                // 尝试卸载已加载的世界
                if (Bukkit.getWorld(w) != null) {
                    plugin.getLogger().info("[ChunkyManager] 卸载已加载世界 " + w);
                    Bukkit.unloadWorld(w, false);
                }

                Path dest = serverDir.resolve(w);
                if (Files.exists(dest)) {
                    plugin.getLogger().info("[ChunkyManager] 删除旧世界目录 " + dest);
                    deleteDirectory(dest);
                }
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("[ChunkyManager] 已移动 " + w);
            }

            // 读取 spawn.txt
            Path spawnFile = batchDir.resolve("spawn.txt");
            if (Files.exists(spawnFile)) {
                String[] parts = Files.readString(spawnFile).trim().split(" ");
                if (parts.length >= 3) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        WorldCreator wc = new WorldCreator("world");
                        Bukkit.createWorld(wc);
                        WorldManager.setSpawnLocation(new Location(Bukkit.getWorld("world"), x, y, z));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // 加载其他两个世界
            Bukkit.createWorld(new WorldCreator("world_nether"));
            Bukkit.createWorld(new WorldCreator("world_the_end"));

            plugin.getLogger().info("[ChunkyManager] 世界加载完成，删除批次目录");
            deleteDirectory(batchDir);
            swapped = true;
            plugin.getLogger().info("[ChunkyManager] 已加载预生成地图批次 " + batchDir.getFileName());
            return true;
        } catch (IOException e) {
            plugin.getLogger().warning("[ChunkyManager] 加载预生成地图失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 在插件加载时调用，负责注册 Chunky 的任务完成监听器。
     */
    public static void initialize() {
        // 尝试通过服务注册获取 ChunkyAPI
        if (chunkyApi == null) {
            RegisteredServiceProvider<ChunkyAPI> provider = Bukkit.getServer().getServicesManager().getRegistration(ChunkyAPI.class);
            if (provider != null) {
                chunkyApi = provider.getProvider();
            }
        }

        if (chunkyApi == null) {
            plugin.getLogger().severe("[ChunkyManager] 未找到 Chunky API，自动地图生成功能已停用！");
            return;
        }

        // 注册任务完成事件
        chunkyApi.onGenerationComplete(event -> {
            String world = event.world();
            plugin.getLogger().info("[ChunkyManager] 世界 " + world + " 的区块生成已完成。");

            synchronized (pendingTasks) {
                pendingTasks.remove(world);
                // 当所有世界都生成完毕时，执行后续逻辑
                if (pendingTasks.isEmpty()) {
                    if (progressLogger != null) progressLogger.cancel();
                    Bukkit.getScheduler().runTask(plugin, ChunkyManager::handleAllTasksComplete);
                }
            }
        });

        // 记录进度变化
        chunkyApi.onGenerationProgress(event -> {
            progressMap.put(event.world(), event.progress());
        });
    }

    /**
     * 启动新一轮的世界预生成任务。
     * 该方法会首先取消旧任务，然后为主世界/下界/末地各自创建新的任务。
     */
    public static void runStartCommand() {
        if (chunkyApi == null) {
            return;
        }

        // 若已存在 100 份批次，则不再生成新的地图
        Path mapsDir = plugin.getDataFolder().toPath().resolve("maps");
        try {
            if (Files.exists(mapsDir)) {
                long count = Files.list(mapsDir).filter(Files::isDirectory).count();
                if (count >= 100) {
                    plugin.getLogger().info("[ChunkyManager] 已达到 100 份预生成地图上限，取消本次区块预生成。");
                    return;
                }
            }
        } catch (IOException ignored) {
        }

        if (!isMaintenanceWindow()) {
            plugin.getLogger().info("[ChunkyManager] 当前不在 02:00-07:00 (北京时间) 时间段，跳过区块预生成。");
            return;
        }

        // 先取消现有任务，以防万一
        chunkyApi.cancelTask("world");
        chunkyApi.cancelTask("world_the_end");
        chunkyApi.cancelTask("world_nether");

        synchronized (pendingTasks) {
            pendingTasks.clear();
        }

        // 配置并开始主世界的区块生成
        Location mainWorldSpawnLoc = WorldManager.getSpawnLocation();
        if (Bukkit.getWorld("world") != null) {
            int centerX = 0;
            int centerZ = 0;
            if (mainWorldSpawnLoc != null && mainWorldSpawnLoc.getWorld() != null) {
                centerX = mainWorldSpawnLoc.getBlockX();
                centerZ = mainWorldSpawnLoc.getBlockZ();
            }
            chunkyApi.startTask("world", "sphere", centerX, centerZ, 1500, 1500, "" // 使用默认模式
            );
            synchronized (pendingTasks) {
                pendingTasks.add("world");
            }

            startProgressLogger();
        }

        // 配置并开始末地的区块生成
        if (Bukkit.getWorld("world_the_end") != null) {
            chunkyApi.startTask("world_the_end", "sphere", 0, 0, 2000, 2000, "");
            synchronized (pendingTasks) {
                pendingTasks.add("world_the_end");
            }

            startProgressLogger();
        }

        // 配置并开始下界的区块生成
        if (Bukkit.getWorld("world_nether") != null) {
            chunkyApi.startTask("world_nether", "sphere", 0, 0, 800, 800, "");
            synchronized (pendingTasks) {
                pendingTasks.add("world_nether");
            }

            startProgressLogger();
        }
    }

    /**
     * 当所有预生成任务全部完成时调用。
     * 主要负责：
     * 1. 卸载世界
     * 2. 打包/移动世界文件到插件 data/maps 目录
     * 3. 重启服务器，等待下一次启动后继续生成下一批地图
     */
    private static void handleAllTasksComplete() {
        plugin.getLogger().info("[ChunkyManager] 所有世界区块已生成，开始打包并准备重启服务器...");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());

        Path mapsDir = plugin.getDataFolder().toPath().resolve("maps");
        try {
            Files.createDirectories(mapsDir);
        } catch (IOException e) {
            plugin.getLogger().severe("[ChunkyManager] 无法创建地图存储目录: " + e.getMessage());
            return;
        }

        // 不再在此处删除旧批次，限制逻辑放在 runStartCommand()

        // 当前批次的存储目录 maps/<timestamp>/
        Path batchDir = mapsDir.resolve(timestamp);
        try {
            Files.createDirectories(batchDir);
        } catch (IOException e) {
            plugin.getLogger().severe("[ChunkyManager] 无法创建批次目录: " + e.getMessage());
            return;
        }

        // 先踢出在线玩家，避免卸载世界失败
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.kickPlayer("服务器正自动重启以继续预生成地图，请稍后再加入!");
        }

        // 保存当前地图的出生点坐标（取主世界 spawnLocation 或 world 实际 spawn）
        Location spawnLoc = WorldManager.getSpawnLocation();
        if (spawnLoc == null && Bukkit.getWorld("world") != null) {
            spawnLoc = Bukkit.getWorld("world").getSpawnLocation();
        }
        if (spawnLoc != null) {
            Path spawnFile = batchDir.resolve("spawn.txt");
            String content = spawnLoc.getBlockX() + " " + spawnLoc.getBlockY() + " " + spawnLoc.getBlockZ();
            try {
                Files.writeString(spawnFile, content);
            } catch (IOException e) {
                plugin.getLogger().warning("[ChunkyManager] 写入出生点坐标失败: " + e.getMessage());
            }
        }

        String[] worlds = {"world", "world_nether", "world_the_end"};
        for (String w : worlds) {
            if (Bukkit.getWorld(w) != null) {
                Bukkit.unloadWorld(w, true);
            }
        }

        // 延迟 100 tick (~5 秒) 再移动目录，确保世界完全保存并释放文件句柄
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (String w : worlds) {
                Path worldDir = Bukkit.getServer().getWorldContainer().toPath().resolve(w);
                if (Files.exists(worldDir)) {
                    // 尝试删除 session.lock 以避免 Windows 文件占用
                    try {
                        Path lockFile = worldDir.resolve("session.lock");
                        Files.deleteIfExists(lockFile);
                    } catch (IOException ignored) {
                    }
                    Path targetDir = batchDir.resolve(w);
                    try {
                        Files.move(worldDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        plugin.getLogger().warning("[ChunkyManager] 移动世界 " + w + " 失败: " + e.getMessage() + "，尝试复制...");
                        try {
                            copyDirectory(worldDir, targetDir);
                        } catch (IOException ex) {
                            plugin.getLogger().warning("[ChunkyManager] 复制世界 " + w + " 亦失败: " + ex.getMessage());
                        }
                    }
                }
            }
            plugin.getLogger().info("[ChunkyManager] 地图已保存至 " + batchDir.toAbsolutePath());

            Bukkit.shutdown();
        }, 100L);
    }

    /**
     * 当游戏即将开始时调用：
     * 1. 停止 Chunky 任务
     * 2. 若 maps 目录中存在已预生成的地图，则替换当前世界并读取 spawn 坐标
     * 3. 返回是否成功替换
     */
    public static boolean prepareWorldForGame() {
        // 步骤1：停止预生成
        runStopCommand();

        Path mapsDir = plugin.getDataFolder().toPath().resolve("maps");
        if (!Files.exists(mapsDir)) {
            return false;
        }

        try {
            // 寻找一个批次目录 maps/<timestamp>
            Path batchDir;
            try (java.util.stream.Stream<Path> stream = Files.list(mapsDir)) {
                batchDir = stream.filter(Files::isDirectory).findFirst().orElse(null);
            }
            if (batchDir == null) return false;

            Path spawnFile = batchDir.resolve("spawn.txt");

            String[] worldNames = {"world", "world_nether", "world_the_end"};
            Path serverDir = Bukkit.getServer().getWorldContainer().toPath();

            for (String w : worldNames) {
                if (Bukkit.getWorld(w) != null) {
                    Bukkit.unloadWorld(w, false);
                }

                Path targetDir = serverDir.resolve(w);
                if (Files.exists(targetDir)) {
                    deleteDirectory(targetDir);
                }

                Path sourceDir = batchDir.resolve(w);
                if (!Files.exists(sourceDir)) {
                    plugin.getLogger().warning("[ChunkyManager] 缺少世界目录 " + sourceDir.toString());
                    return false;
                }
                Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);

                Bukkit.createWorld(new WorldCreator(w));
            }

            // 读取出生点
            if (Files.exists(spawnFile)) {
                String content = Files.readString(spawnFile).trim();
                String[] parts = content.split(" ");
                if (parts.length >= 3) {
                    try {
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        Location loc = new Location(Bukkit.getWorld("world"), x, y, z);
                        WorldManager.setSpawnLocation(loc);
                    } catch (NumberFormatException ignore) {
                        plugin.getLogger().warning("[ChunkyManager] 解析出生点坐标失败: " + content);
                    }
                }
            }

            // 使用完毕后删除整个批次目录，节省空间
            deleteDirectory(batchDir);
        } catch (IOException e) {
            plugin.getLogger().warning("[ChunkyManager] 准备地图时出错: " + e.getMessage());
            return false;
        }
        return true;
    }

    private static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        java.util.List<Path> paths = new java.util.ArrayList<>();
        Files.walk(path).sorted(java.util.Comparator.reverseOrder()).forEach(paths::add);
        for (Path p : paths) {
            Files.deleteIfExists(p);
        }
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(path -> {
            try {
                Path dest = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ignored) {
            }
        });
    }

    public static void runStopCommand() {
        if (chunkyApi == null) {
            plugin.getLogger().severe("Chunky API not found, please install Chunky plugin.");
            return;
        }
        chunkyApi.cancelTask("world");
        chunkyApi.cancelTask("world_the_end");
        chunkyApi.cancelTask("world_nether");

        synchronized (pendingTasks) {
            pendingTasks.clear();
        }
    }

    private static void startProgressLogger() {
        if (progressLogger != null && !progressLogger.isCancelled()) return;
        progressLogger = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingTasks.isEmpty()) {
                    this.cancel();
                    return;
                }
                StringBuilder sb = new StringBuilder("[Chunky] 进度: ");
                for (String w : pendingTasks) {
                    float p = progressMap.getOrDefault(w, 0f);
                    sb.append(w).append(" ").append(String.format("%.2f%%", p)).append("; ");
                }
                plugin.getLogger().info(sb.toString());
            }
        };
        progressLogger.runTaskTimer(plugin, 0L, 600L); // 30s
    }
}