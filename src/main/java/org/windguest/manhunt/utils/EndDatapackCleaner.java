package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import org.windguest.manhunt.Main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class EndDatapackCleaner {
    private static final Main plugin = Main.getInstance();
    
    /**
     * 清理末地数据包，移除自定义维度、生物群系等引起问题的配置
     */
    public static boolean cleanEndDatapack(File datapackFile) {
        Path tempDir = null;
        Path endPacksFolder = Paths.get(Bukkit.getWorldContainer().getPath(), "world", "endpacks");
        
        try {
            // 确保endpacks文件夹存在
            Files.createDirectories(endPacksFolder);
            
            // 创建临时目录
            tempDir = Files.createTempDirectory("end_datapack_clean_");
            plugin.getLogger().info("清理数据包，使用临时目录: " + tempDir);
            
            // 解压数据包
            plugin.getLogger().info("解压数据包: " + datapackFile.getName());
            unzipFile(datapackFile.toPath(), tempDir);
            
            // 清理问题配置
            plugin.getLogger().info("清理问题配置...");
            cleanProblematicConfigs(tempDir);
            
            // 修改结构文件，确保它们在末地生成
            plugin.getLogger().info("修改结构文件...");
            fixStructureConfigs(tempDir);
            
            // 创建清理后的数据包名称
            String originalName = datapackFile.getName();
            String baseName = originalName.replace(".zip", "").replace(".ZIP", "");
            String cleanName;
            
            if (baseName.endsWith("_fixed") || baseName.endsWith("_simple_end") || baseName.endsWith("_end")) {
                cleanName = baseName + "_clean.zip";
            } else {
                cleanName = baseName + "_end_clean.zip";
            }
            
            Path cleanPath = endPacksFolder.resolve(cleanName);
            
            // 压缩为新的数据包
            plugin.getLogger().info("创建清理版数据包: " + cleanName);
            zipFolder(tempDir, cleanPath);
            
            plugin.getLogger().info("✓ 成功清理末地数据包: " + cleanName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("清理末地数据包失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // 清理临时目录
            if (tempDir != null) {
                try {
                    deleteDirectory(tempDir);
                    plugin.getLogger().info("清理临时目录完成");
                } catch (IOException e) {
                    plugin.getLogger().warning("清理临时目录时出错: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 清理问题配置
     */
    private static void cleanProblematicConfigs(Path datapackDir) throws IOException {
        Path dataDir = datapackDir.resolve("data");
        if (!Files.exists(dataDir)) {
            return;
        }
        
        // 遍历所有命名空间
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String filePath = file.toString().replace("\\", "/");
                
                // 检查并清理问题文件
                if (filePath.contains("dimension_type/") || 
                    filePath.contains("dimension/") ||
                    filePath.contains("worldgen/noise_settings/") ||
                    filePath.contains("worldgen/biome/")) {
                    
                    // 记录要删除的文件
                    plugin.getLogger().info("删除问题配置文件: " + file.getFileName());
                    
                    // 检查是否是要保留的末地相关文件
                    if (shouldKeepFile(filePath)) {
                        plugin.getLogger().info("保留文件: " + file.getFileName());
                        return FileVisitResult.CONTINUE;
                    }
                    
                    // 删除文件
                    Files.delete(file);
                    
                    // 尝试删除空文件夹
                    deleteEmptyParentDirectories(file);
                }
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                // 尝试删除空目录
                if (Files.exists(dir) && Files.isDirectory(dir) && isDirectoryEmpty(dir)) {
                    try {
                        Files.delete(dir);
                        plugin.getLogger().fine("删除空目录: " + dir);
                    } catch (Exception e) {
                        // 忽略删除失败的情况
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 检查文件是否应该保留
     */
    private static boolean shouldKeepFile(String filePath) {
        // 保留与末地相关的标准配置
        if (filePath.contains("the_end") || 
            filePath.contains("end_") ||
            filePath.contains("_end.")) {
            return true;
        }
        
        // 保留minecraft命名空间的标准配置
        if (filePath.contains("/minecraft/")) {
            // 检查是否是标准minecraft配置
            String fileName = Paths.get(filePath).getFileName().toString();
            return fileName.contains("end") || 
                   fileName.equals("plains.json") || 
                   fileName.equals("ocean.json") ||
                   fileName.equals("desert.json");
        }
        
        return false;
    }
    
    /**
     * 删除空的父目录
     */
    private static void deleteEmptyParentDirectories(Path file) throws IOException {
        Path parent = file.getParent();
        while (parent != null && Files.exists(parent)) {
            if (isDirectoryEmpty(parent)) {
                Files.delete(parent);
                plugin.getLogger().fine("删除空父目录: " + parent);
                parent = parent.getParent();
            } else {
                break;
            }
        }
    }
    
    /**
     * 检查目录是否为空
     */
    private static boolean isDirectoryEmpty(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            return true;
        }
        
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }
    
    /**
     * 修复结构配置
     */
    private static void fixStructureConfigs(Path datapackDir) throws IOException {
        Path dataDir = datapackDir.resolve("data");
        if (!Files.exists(dataDir)) {
            return;
        }
        
        // 收集所有结构相关的文件
        List<Path> structureFiles = new ArrayList<>();
        List<Path> structureSetFiles = new ArrayList<>();
        
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".json")) {
                    String filePath = file.toString().replace("\\", "/");
                    
                    if (filePath.contains("worldgen/structure/") && 
                        !filePath.contains("structure_set/")) {
                        structureFiles.add(file);
                    } else if (filePath.contains("worldgen/structure_set/")) {
                        structureSetFiles.add(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        
        plugin.getLogger().info("找到 " + structureFiles.size() + " 个结构文件");
        plugin.getLogger().info("找到 " + structureSetFiles.size() + " 个结构集文件");
        
        // 修复结构文件
        for (Path file : structureFiles) {
            ensureStructureInEnd(file);
        }
        
        // 修复结构集文件
        for (Path file : structureSetFiles) {
            ensureStructureSetInEnd(file);
        }
    }
    
    /**
     * 确保结构在末地生成
     */
    private static void ensureStructureInEnd(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            
            // 简单检查：如果已经包含"the_end"，则跳过
            if (content.contains("\"minecraft:the_end\"") || 
                content.contains("the_end")) {
                return;
            }
            
            // 修改内容：替换生物群系为末地
            String modifiedContent = content.replaceAll(
                "\"biomes\"\\s*:\\s*\\[[^\\]]*\\]", 
                "\"biomes\": [\"minecraft:the_end\"]"
            );
            
            // 如果替换成功，写入文件
            if (!modifiedContent.equals(content)) {
                Files.write(jsonFile, modifiedContent.getBytes(StandardCharsets.UTF_8));
                plugin.getLogger().info("修复结构文件生物群系: " + jsonFile.getFileName());
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("修复结构文件失败: " + jsonFile + " - " + e.getMessage());
        }
    }
    
    /**
     * 确保结构集在末地生成
     */
    private static void ensureStructureSetInEnd(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            
            // 检查是否已经是末地维度
            if (content.contains("\"dimension\"\\s*:\\s*\"minecraft:the_end\"")) {
                return;
            }
            
            // 修改内容：替换维度为末地
            String modifiedContent = content.replaceAll(
                "\"dimension\"\\s*:\\s*\"[^\"]*\"", 
                "\"dimension\": \"minecraft:the_end\""
            );
            
            // 如果替换成功，写入文件
            if (!modifiedContent.equals(content)) {
                Files.write(jsonFile, modifiedContent.getBytes(StandardCharsets.UTF_8));
                plugin.getLogger().info("修复结构集文件维度: " + jsonFile.getFileName());
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("修复结构集文件失败: " + jsonFile + " - " + e.getMessage());
        }
    }
    
    /**
     * 解压ZIP文件
     */
    private static void unzipFile(Path zipFile, Path outputDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            zip.stream().forEach(entry -> {
                try {
                    Path entryPath = outputDir.resolve(entry.getName());
                    
                    if (entry.isDirectory()) {
                        Files.createDirectories(entryPath);
                    } else {
                        Files.createDirectories(entryPath.getParent());
                        try (InputStream is = zip.getInputStream(entry)) {
                            Files.copy(is, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
    
    /**
     * 压缩文件夹为ZIP
     */
    private static void zipFolder(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile.toFile()))) {
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = sourceDir.relativize(file);
                    zos.putNextEntry(new ZipEntry(relativePath.toString().replace("\\", "/")));
                    
                    Files.copy(file, zos);
                    zos.closeEntry();
                    
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(sourceDir)) {
                        Path relativePath = sourceDir.relativize(dir);
                        zos.putNextEntry(new ZipEntry(relativePath.toString().replace("\\", "/") + "/"));
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    
    /**
     * 删除目录
     */
    private static void deleteDirectory(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}