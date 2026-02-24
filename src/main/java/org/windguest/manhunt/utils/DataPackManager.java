package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.windguest.manhunt.Main;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DataPackManager {
    private static final Main plugin = Main.getInstance();
    private static final File WORLD_FOLDER = Bukkit.getWorldContainer();
    private static final File DATAPACKS_FOLDER = new File(WORLD_FOLDER, "world/datapacks");
    private static final File COMMON_PACKS_FOLDER = new File(WORLD_FOLDER, "world/commonpacks");
    private static final File END_PACKS_FOLDER = new File(WORLD_FOLDER, "world/endpacks");
    
    /**
     * 在服务器启动时检查数据包状态
     */
    public static void checkDatapackStatus() {
        try {
            ensureFoldersExist();
            
            plugin.getLogger().info("检查数据包状态...");
            plugin.getLogger().info("- datapacks 文件数: " + countFiles(DATAPACKS_FOLDER));
            plugin.getLogger().info("- commonpacks 文件数: " + countFiles(COMMON_PACKS_FOLDER));
            plugin.getLogger().info("- endpacks 文件数: " + countFiles(END_PACKS_FOLDER));
            
            // 获取当前模式
            String modeName = plugin.getConfig().getString("current-mode", "null");
            
            if (modeName.equals("END")) {
                plugin.getLogger().info("当前配置为混沌末地模式");
            } else {
                plugin.getLogger().info("当前配置为普通模式或未设置模式");
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("检查数据包状态时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 切换到混沌末地模式的数据包
     * 将endpacks的内容复制到datapacks
     */
    public static boolean switchToEndMode() {
        try {
            plugin.getLogger().info("正在切换到混沌末地数据包...");
            
            ensureFoldersExist();
            
            // 检查endpacks是否有内容
            if (!END_PACKS_FOLDER.exists() || !END_PACKS_FOLDER.isDirectory()) {
                END_PACKS_FOLDER.mkdirs();
                plugin.getLogger().info("endpacks文件夹不存在，已创建空文件夹");
            }
            
            // 即使endpacks为空也继续执行
            if (countFiles(END_PACKS_FOLDER) == 0) {
                plugin.getLogger().info("endpacks为空，datapacks将被清空");
            }
            
            // 1. 备份当前datapacks到datapacks_backup
            backupDatapacks();
            
            // 2. 清空datapacks文件夹
            if (DATAPACKS_FOLDER.exists()) {
                deleteFolder(DATAPACKS_FOLDER);
            }
            DATAPACKS_FOLDER.mkdirs();
            
            // 3. 复制endpacks到datapacks（如果有内容）
            if (countFiles(END_PACKS_FOLDER) > 0) {
                // 首先验证endpacks中的文件
                List<File> endPackFiles = listAllFiles(END_PACKS_FOLDER);
                plugin.getLogger().info("endpacks中找到 " + endPackFiles.size() + " 个文件");
                
                int successCount = 0;
                for (File sourceFile : endPackFiles) {
                    try {
                        String relativePath = END_PACKS_FOLDER.toPath().relativize(sourceFile.toPath()).toString();
                        File targetFile = new File(DATAPACKS_FOLDER, relativePath);
                        
                        // 确保目标目录存在
                        targetFile.getParentFile().mkdirs();
                        
                        // 安全复制文件
                        copyFileWithTemp(sourceFile, targetFile);
                        
                        if (targetFile.exists() && targetFile.length() > 0) {
                            successCount++;
                        } else {
                            plugin.getLogger().warning("复制失败: " + sourceFile.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("复制文件失败 " + sourceFile.getName() + ": " + e.getMessage());
                    }
                }
                
                plugin.getLogger().info("已从 endpacks 复制 " + successCount + "/" + endPackFiles.size() + " 个文件到 datapacks");
            } else {
                plugin.getLogger().info("endpacks 为空，datapacks 保持为空");
            }
            
            // 4. 验证复制结果
            int finalFileCount = countFiles(DATAPACKS_FOLDER);
            plugin.getLogger().info("datapacks 最终文件数: " + finalFileCount);
            
            if (countFiles(END_PACKS_FOLDER) > 0 && finalFileCount == 0) {
                plugin.getLogger().warning("复制可能失败，尝试恢复备份");
                restoreDatapacksBackup();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("切换到混沌末地数据包时出错: " + e.getMessage());
            e.printStackTrace();
            restoreDatapacksBackup();
            return false;
        }
    }
    
    /**
     * 切换回普通模式的数据包
     * 将commonpacks的内容复制到datapacks
     */
    public static boolean switchToNormalMode() {
        try {
            plugin.getLogger().info("正在切换回普通数据包...");
            
            ensureFoldersExist();
            
            // 检查commonpacks是否有内容
            if (!COMMON_PACKS_FOLDER.exists() || !COMMON_PACKS_FOLDER.isDirectory()) {
                COMMON_PACKS_FOLDER.mkdirs();
                plugin.getLogger().info("commonpacks文件夹不存在，已创建空文件夹");
            }
            
            // 即使commonpacks为空也继续执行
            if (countFiles(COMMON_PACKS_FOLDER) == 0) {
                plugin.getLogger().info("commonpacks为空，datapacks将被清空");
            }
            
            // 1. 备份当前datapacks到datapacks_backup
            backupDatapacks();
            
            // 2. 清空datapacks文件夹
            if (DATAPACKS_FOLDER.exists()) {
                deleteFolder(DATAPACKS_FOLDER);
            }
            DATAPACKS_FOLDER.mkdirs();
            
            // 3. 复制commonpacks到datapacks（如果有内容）
            if (countFiles(COMMON_PACKS_FOLDER) > 0) {
                // 首先验证commonpacks中的文件
                List<File> commonPackFiles = listAllFiles(COMMON_PACKS_FOLDER);
                plugin.getLogger().info("commonpacks中找到 " + commonPackFiles.size() + " 个文件");
                
                int successCount = 0;
                for (File sourceFile : commonPackFiles) {
                    try {
                        String relativePath = COMMON_PACKS_FOLDER.toPath().relativize(sourceFile.toPath()).toString();
                        File targetFile = new File(DATAPACKS_FOLDER, relativePath);
                        
                        // 确保目标目录存在
                        targetFile.getParentFile().mkdirs();
                        
                        // 安全复制文件
                        copyFileWithTemp(sourceFile, targetFile);
                        
                        if (targetFile.exists() && targetFile.length() > 0) {
                            successCount++;
                        } else {
                            plugin.getLogger().warning("复制失败: " + sourceFile.getName());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("复制文件失败 " + sourceFile.getName() + ": " + e.getMessage());
                    }
                }
                
                plugin.getLogger().info("已从 commonpacks 复制 " + successCount + "/" + commonPackFiles.size() + " 个文件到 datapacks");
            } else {
                plugin.getLogger().info("commonpacks 为空，datapacks 保持为空");
            }
            
            // 4. 验证复制结果
            int finalFileCount = countFiles(DATAPACKS_FOLDER);
            plugin.getLogger().info("datapacks 最终文件数: " + finalFileCount);
            
            if (countFiles(COMMON_PACKS_FOLDER) > 0 && finalFileCount == 0) {
                plugin.getLogger().warning("复制可能失败，尝试恢复备份");
                restoreDatapacksBackup();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("切换回普通数据包时出错: " + e.getMessage());
            e.printStackTrace();
            restoreDatapacksBackup();
            return false;
        }
    }
    
    /**
     * 初始化数据包文件夹结构
     */
    public static boolean initializeDatapackFolders() {
        try {
            ensureFoldersExist();
            
            plugin.getLogger().info("初始化数据包文件夹结构...");
            
            // 1. 初始化commonpacks（普通模式数据包）
            if (countFiles(COMMON_PACKS_FOLDER) == 0) {
                plugin.getLogger().info("commonpacks 文件夹为空，正在初始化...");
                
                // 如果当前datapacks有内容，复制到commonpacks
                if (DATAPACKS_FOLDER.exists() && DATAPACKS_FOLDER.isDirectory() && countFiles(DATAPACKS_FOLDER) > 0) {
                    safeCopyFolder(DATAPACKS_FOLDER, COMMON_PACKS_FOLDER);
                    plugin.getLogger().info("已将当前 datapacks 复制到 commonpacks 作为初始普通数据包");
                } else {
                    plugin.getLogger().info("datapacks 为空，commonpacks 将保持为空");
                    plugin.getLogger().info("请将普通模式数据包放入 world/commonpacks 文件夹");
                }
            }
            
            // 2. 初始化endpacks（混沌末地模式数据包）
            if (countFiles(END_PACKS_FOLDER) == 0) {
                plugin.getLogger().info("endpacks 文件夹为空，正在创建空文件夹...");
                END_PACKS_FOLDER.mkdirs();
                plugin.getLogger().info("endpacks 为空，请在 world/endpacks 文件夹中放置混沌末地数据包");
            }
            
            plugin.getLogger().info("数据包文件夹初始化完成");
            plugin.getLogger().info("- datapacks 文件数: " + countFiles(DATAPACKS_FOLDER));
            plugin.getLogger().info("- commonpacks 文件数: " + countFiles(COMMON_PACKS_FOLDER));
            plugin.getLogger().info("- endpacks 文件数: " + countFiles(END_PACKS_FOLDER));
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("初始化数据包文件夹时出错: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 重启服务器
     */
    public static void restartServer() {
        plugin.getLogger().info("服务器将在10秒后重启...");
        Bukkit.broadcastMessage("§e[!] 服务器将在10秒后重启以应用更改！");
        
        // 延迟10秒后重启
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 等待文件操作完成
            try {
                Thread.sleep(5000); // 额外等待5秒确保文件复制完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 踢出所有玩家
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.kickPlayer("§e服务器正在重启以切换游戏模式...");
            });
            
            // 延迟1秒后关闭服务器
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.shutdown();
            }, 20L);
        }, 100L); // 5秒后开始重启流程
    }
    
    /**
     * 确保所有必需的文件夹都存在
     */
    public static void ensureFoldersExist() {
        if (!WORLD_FOLDER.exists()) {
            WORLD_FOLDER.mkdirs();
        }
        
        // 创建world文件夹
        File worldFolder = new File(WORLD_FOLDER, "world");
        if (!worldFolder.exists()) {
            worldFolder.mkdirs();
        }
        
        // 创建必要的子文件夹
        DATAPACKS_FOLDER.mkdirs();
        COMMON_PACKS_FOLDER.mkdirs();
        END_PACKS_FOLDER.mkdirs();
    }
    
    /**
     * 计算文件夹中的文件数量
     */
    private static int countFiles(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return 0;
        }
        
        int count = 0;
        for (File file : files) {
            if (file.isFile()) {
                count++;
            } else if (file.isDirectory()) {
                count += countFiles(file);
            }
        }
        return count;
    }
    
    /**
     * 递归列出文件夹中的所有文件
     */
    private static List<File> listAllFiles(File folder) {
        List<File> fileList = new ArrayList<>();
        if (!folder.exists() || !folder.isDirectory()) {
            return fileList;
        }
        
        File[] files = folder.listFiles();
        if (files == null) {
            return fileList;
        }
        
        for (File file : files) {
            if (file.isFile()) {
                fileList.add(file);
            } else if (file.isDirectory()) {
                fileList.addAll(listAllFiles(file));
            }
        }
        return fileList;
    }
    
    /**
     * 复制文件夹
     */
    private static void safeCopyFolder(File sourceFolder, File targetFolder) throws IOException {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            throw new IOException("源文件夹不存在或不是目录: " + sourceFolder);
        }
        
        // 确保目标文件夹存在
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        
        File[] files = sourceFolder.listFiles();
        if (files == null) {
            return;
        }
        
        for (File sourceFile : files) {
            File targetFile = new File(targetFolder, sourceFile.getName());
            
            if (sourceFile.isDirectory()) {
                safeCopyFolder(sourceFile, targetFile);
            } else {
                copyFileWithTemp(sourceFile, targetFile);
            }
        }
    }
    
    /**
     * 使用临时文件复制文件（避免直接覆盖源文件）
     */
    private static void copyFileWithTemp(File sourceFile, File targetFile) throws IOException {
        // 验证源文件
        if (!sourceFile.exists() || !sourceFile.canRead()) {
            throw new IOException("源文件不存在或不可读: " + sourceFile.getAbsolutePath());
        }
        
        long sourceSize = sourceFile.length();
        
        // 创建临时文件（在与目标相同的目录，但使用不同的扩展名）
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".tmp");
        
        try (InputStream in = new FileInputStream(sourceFile);
             OutputStream out = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
        }
        
        // 验证临时文件
        if (!tempFile.exists()) {
            throw new IOException("临时文件创建失败");
        }
        
        long tempSize = tempFile.length();
        if (sourceSize > 0 && tempSize == 0) {
            throw new IOException("复制后文件大小为0: " + sourceFile.getName());
        }
        
        // 重命名临时文件为目标文件
        if (!tempFile.renameTo(targetFile)) {
            // 如果重命名失败，尝试使用Files.copy
            tempFile.delete();
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        
        // 最终验证
        if (!targetFile.exists()) {
            throw new IOException("目标文件创建失败: " + targetFile.getAbsolutePath());
        }
    }
    
    /**
     * 备份当前的datapacks
     */
    private static void backupDatapacks() {
        try {
            File backupDir = new File(WORLD_FOLDER, "world/datapacks_backup");
            if (backupDir.exists()) {
                deleteFolder(backupDir);
            }
            backupDir.mkdirs();
            
            if (DATAPACKS_FOLDER.exists() && DATAPACKS_FOLDER.isDirectory() && countFiles(DATAPACKS_FOLDER) > 0) {
                safeCopyFolder(DATAPACKS_FOLDER, backupDir);
                plugin.getLogger().info("已备份当前datapacks到: " + backupDir.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("备份datapacks时出错: " + e.getMessage());
        }
    }
    
    /**
     * 恢复datapacks备份
     */
    private static void restoreDatapacksBackup() {
        try {
            File backupDir = new File(WORLD_FOLDER, "world/datapacks_backup");
            if (!backupDir.exists() || !backupDir.isDirectory() || countFiles(backupDir) == 0) {
                plugin.getLogger().info("没有可用的备份");
                return;
            }
            
            // 清空当前的datapacks
            if (DATAPACKS_FOLDER.exists()) {
                deleteFolder(DATAPACKS_FOLDER);
            }
            DATAPACKS_FOLDER.mkdirs();
            
            // 从备份恢复
            safeCopyFolder(backupDir, DATAPACKS_FOLDER);
            plugin.getLogger().info("已从备份恢复datapacks");
            
            // 清理备份
            deleteFolder(backupDir);
        } catch (Exception e) {
            plugin.getLogger().warning("恢复datapacks备份时出错: " + e.getMessage());
        }
    }
    
    /**
     * 删除文件夹
     */
    private static void deleteFolder(File folder) {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteFolder(file);
                    } else {
                        file.delete();
                    }
                }
            }
            folder.delete();
        }
    }
    
    /**
     * 检查当前datapacks是否与endpacks内容相同
     */
    public static boolean isDatapacksUsingEnd() {
        try {
            ensureFoldersExist();
            
            // 如果endpacks为空，则datapacks可以有任何内容
            if (countFiles(END_PACKS_FOLDER) == 0) {
                return true;
            }
            
            // 获取datapacks和endpacks的文件名列表
            Set<String> datapackFiles = getFileNames(DATAPACKS_FOLDER);
            Set<String> endpackFiles = getFileNames(END_PACKS_FOLDER);
            
            // 如果endpacks的文件在datapacks中都有，就认为是使用end数据包
            for (String endFile : endpackFiles) {
                if (!datapackFiles.contains(endFile)) {
                    plugin.getLogger().warning("datapacks缺少endpacks中的文件: " + endFile);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("检查是否使用end数据包时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查当前datapacks是否与commonpacks内容相同
     */
    public static boolean isDatapacksUsingCommon() {
        try {
            ensureFoldersExist();
            
            // 如果commonpacks为空，则datapacks可以有任何内容
            if (countFiles(COMMON_PACKS_FOLDER) == 0) {
                return true;
            }
            
            // 获取datapacks和commonpacks的文件名列表
            Set<String> datapackFiles = getFileNames(DATAPACKS_FOLDER);
            Set<String> commonpackFiles = getFileNames(COMMON_PACKS_FOLDER);
            
            // 如果commonpacks的文件在datapacks中都有，就认为是使用common数据包
            for (String commonFile : commonpackFiles) {
                if (!datapackFiles.contains(commonFile)) {
                    plugin.getLogger().warning("datapacks缺少commonpacks中的文件: " + commonFile);
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("检查是否使用common数据包时出错: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取文件夹中所有文件的文件名
     */
    private static Set<String> getFileNames(File folder) {
        Set<String> fileNames = new HashSet<>();
        if (!folder.exists() || !folder.isDirectory()) {
            return fileNames;
        }
        
        collectFileNames(folder, "", fileNames);
        return fileNames;
    }
    
    /**
     * 递归收集文件名
     */
    private static void collectFileNames(File folder, String path, Set<String> fileNames) {
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            String fileName = path + file.getName();
            if (file.isFile()) {
                fileNames.add(fileName);
            } else if (file.isDirectory()) {
                collectFileNames(file, fileName + "/", fileNames);
            }
        }
    }
    
    /**
     * 检查当前数据包是否与模式匹配
     */
    public static boolean isDatapacksMatchingMode(org.windguest.manhunt.game.Mode.GameMode mode) {
        if (mode == org.windguest.manhunt.game.Mode.GameMode.END) {
            return isDatapacksUsingEnd();
        } else if (mode == org.windguest.manhunt.game.Mode.GameMode.MANHUNT || 
                   mode == org.windguest.manhunt.game.Mode.GameMode.TEAM ||
                   mode == null) {
            return isDatapacksUsingCommon();
        }
        return true;
    }
    
    /**
     * 尝试切换到与模式匹配的数据包（如果不匹配）
     */
    public static boolean ensureDatapacksMatchMode(org.windguest.manhunt.game.Mode.GameMode mode) {
        try {
            if (mode == org.windguest.manhunt.game.Mode.GameMode.END) {
                if (!isDatapacksUsingEnd()) {
                    plugin.getLogger().warning("datapacks与混沌末地模式不匹配，正在切换...");
                    return switchToEndMode();
                }
            } else if (mode == org.windguest.manhunt.game.Mode.GameMode.MANHUNT || 
                       mode == org.windguest.manhunt.game.Mode.GameMode.TEAM ||
                       mode == null) {
                if (!isDatapacksUsingCommon()) {
                    plugin.getLogger().warning("datapacks与普通模式不匹配，正在切换...");
                    return switchToNormalMode();
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("确保数据包匹配模式时出错: " + e.getMessage());
            return false;
        }
    }
}