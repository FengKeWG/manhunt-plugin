package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import com.google.gson.*;
import org.windguest.manhunt.Main;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class SimpleEndConverter {
    private static final Main plugin = Main.getInstance();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 简单转换：将结构复制到末地生成
     */
    public static boolean convertToEndDatapack(File originalDatapack) {
        Path tempDir = null;
        Path endPacksFolder = Paths.get(Bukkit.getWorldContainer().getPath(), "world", "endpacks");
        
        try {
            // 确保endpacks文件夹存在
            Files.createDirectories(endPacksFolder);
            
            // 创建临时目录
            tempDir = Files.createTempDirectory("simple_end_convert_");
            plugin.getLogger().info("使用临时目录: " + tempDir);
            
            // 解压原始数据包
            plugin.getLogger().info("解压数据包: " + originalDatapack.getName());
            unzipFile(originalDatapack.toPath(), tempDir);
            
            // 修复现有的末地数据包问题
            plugin.getLogger().info("修复现有末地数据包问题...");
            fixExistingEndIssues(tempDir);
            
            // 简单转换：只修改结构文件中的生物群系
            plugin.getLogger().info("转换结构到末地...");
            simplyConvertStructures(tempDir);
            
            // 创建新的数据包名称
            String originalName = originalDatapack.getName();
            String baseName = originalName.replace(".zip", "").replace(".ZIP", "");
            String endPackName = baseName + "_simple_end.zip";
            Path endPackPath = endPacksFolder.resolve(endPackName);
            
            // 压缩为新的数据包
            plugin.getLogger().info("创建简化版末地数据包: " + endPackName);
            zipFolder(tempDir, endPackPath);
            
            plugin.getLogger().info("✓ 成功创建简化版末地数据包: " + endPackName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("构建简化版末地数据包失败: " + e.getMessage());
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
     * 修复现有的末地数据包问题
     */
    private static void fixExistingEndIssues(Path datapackDir) throws IOException {
        Path dataDir = datapackDir.resolve("data");
        if (!Files.exists(dataDir)) {
            return;
        }
        
        // 查找并修复所有维度相关的配置文件
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".json")) {
                    String filePath = file.toString().replace("\\", "/");
                    
                    // 修复维度配置文件
                    if (filePath.contains("dimension_type") || 
                        filePath.contains("dimension") ||
                        filePath.contains("worldgen/noise_settings") ||
                        filePath.contains("worldgen/biome")) {
                        fixDimensionFile(file);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 修复维度配置文件
     */
    private static void fixDimensionFile(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(content, JsonElement.class);
            
            if (!element.isJsonObject()) {
                return;
            }
            
            JsonObject json = element.getAsJsonObject();
            String fileName = jsonFile.getFileName().toString();
            
            // 记录文件内容以便调试
            plugin.getLogger().fine("检查文件: " + fileName);
            
            // 对于维度类型文件，确保它们是有效的
            if (json.has("ambient_light") || json.has("fixed_time") || json.has("has_skylight")) {
                plugin.getLogger().info("修复维度类型文件: " + fileName);
                ensureValidDimensionType(json);
            }
            
            // 保存修复后的文件
            String newContent = gson.toJson(json);
            Files.write(jsonFile, newContent.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            plugin.getLogger().warning("修复文件失败: " + jsonFile + " - " + e.getMessage());
            // 不抛出异常，继续处理其他文件
        }
    }
    
    /**
     * 确保维度类型有效
     */
    private static void ensureValidDimensionType(JsonObject dimensionType) {
        // 确保有必要的字段
        if (!dimensionType.has("ambient_light")) {
            dimensionType.addProperty("ambient_light", 0.0f);
        }
        if (!dimensionType.has("fixed_time")) {
            dimensionType.addProperty("fixed_time", 6000L);
        }
        if (!dimensionType.has("has_skylight")) {
            dimensionType.addProperty("has_skylight", true);
        }
        if (!dimensionType.has("has_ceiling")) {
            dimensionType.addProperty("has_ceiling", false);
        }
        if (!dimensionType.has("ultrawarm")) {
            dimensionType.addProperty("ultrawarm", false);
        }
        if (!dimensionType.has("natural")) {
            dimensionType.addProperty("natural", true);
        }
        if (!dimensionType.has("coordinate_scale")) {
            dimensionType.addProperty("coordinate_scale", 1.0);
        }
        if (!dimensionType.has("bed_works")) {
            dimensionType.addProperty("bed_works", true);
        }
        if (!dimensionType.has("respawn_anchor_works")) {
            dimensionType.addProperty("respawn_anchor_works", false);
        }
        if (!dimensionType.has("min_y")) {
            dimensionType.addProperty("min_y", -64);
        }
        if (!dimensionType.has("height")) {
            dimensionType.addProperty("height", 384);
        }
        if (!dimensionType.has("logical_height")) {
            dimensionType.addProperty("logical_height", 384);
        }
        if (!dimensionType.has("infiniburn")) {
            dimensionType.add("infiniburn", new JsonPrimitive("#minecraft:infiniburn_overworld"));
        }
        if (!dimensionType.has("effects")) {
            dimensionType.addProperty("effects", "minecraft:overworld");
        }
    }
    
    /**
     * 简单转换：只修改结构文件中的生物群系
     */
    private static void simplyConvertStructures(Path datapackDir) throws IOException {
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
        
        // 处理结构文件：修改生物群系
        for (Path file : structureFiles) {
            simplyConvertStructureFile(file);
        }
        
        // 处理结构集文件：修改维度和生物群系过滤器
        for (Path file : structureSetFiles) {
            simplyConvertStructureSetFile(file);
        }
    }
    
    /**
     * 简单转换结构文件
     */
    private static void simplyConvertStructureFile(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(content, JsonElement.class);
            
            if (!element.isJsonObject()) {
                return;
            }
            
            JsonObject json = element.getAsJsonObject();
            String fileName = jsonFile.getFileName().toString();
            
            // 检查是否是有效的结构文件
            if (!json.has("type") || !json.has("biomes")) {
                return;
            }
            
            // 只修改生物群系为所有末地外岛 + Nullscape
            JsonArray endBiomes = new JsonArray();
            endBiomes.add("minecraft:end_highlands");
            endBiomes.add("minecraft:end_midlands");
            endBiomes.add("minecraft:end_barrens");
            endBiomes.add("minecraft:small_end_islands");
            //endBiomes.add("nullscape:crystal_peaks");
            //endBiomes.add("nullscape:shadowlands");
            //endBiomes.add("nullscape:void_barrens");
            json.add("biomes", endBiomes);
            
            // 保存文件
            String newContent = gson.toJson(json);
            Files.write(jsonFile, newContent.getBytes(StandardCharsets.UTF_8));
            plugin.getLogger().fine("已转换结构文件支持外岛 + Nullscape: " + fileName);
            
        } catch (Exception e) {
            plugin.getLogger().warning("转换结构文件失败: " + jsonFile + " - " + e.getMessage());
        }
    }
    
    /**
     * 简单转换结构集文件
     */
    private static void simplyConvertStructureSetFile(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(content, JsonElement.class);
            
            if (!element.isJsonObject()) {
                return;
            }
            
            JsonObject json = element.getAsJsonObject();
            String fileName = jsonFile.getFileName().toString();
            
            // 检查是否有placement部分
            if (!json.has("placement")) {
                return;
            }
            
            JsonElement placementElem = json.get("placement");
            if (!placementElem.isJsonObject()) {
                return;
            }
            
            JsonObject placement = placementElem.getAsJsonObject();
            boolean modified = false;
            
            // 修改维度为末地（如果存在）
            if (placement.has("dimension")) {
                placement.addProperty("dimension", "minecraft:the_end");
                modified = true;
            }
            
            // 修改生物群系过滤器为末地外岛 + Nullscape（如果存在）
            if (placement.has("biome_filter")) {
                JsonArray endBiomeFilter = new JsonArray();
                endBiomeFilter.add("minecraft:end_highlands");
                endBiomeFilter.add("minecraft:end_midlands");
                endBiomeFilter.add("minecraft:end_barrens");
                endBiomeFilter.add("minecraft:small_end_islands");
                //endBiomeFilter.add("nullscape:crystal_peaks");
                //endBiomeFilter.add("nullscape:shadowlands");
                //endBiomeFilter.add("nullscape:void_barrens");
                placement.add("biome_filter", endBiomeFilter);
                modified = true;
            }
            
            // 确保不是 "minecraft:end"
            if (placement.has("type")) {
                String currentType = placement.get("type").getAsString();
                if ("minecraft:end".equals(currentType)) {
                    placement.addProperty("type", "minecraft:random_spread");
                    modified = true;
                }
            }
            
            // 如果修改了，保存文件
            if (modified) {
                String newContent = gson.toJson(json);
                Files.write(jsonFile, newContent.getBytes(StandardCharsets.UTF_8));
                plugin.getLogger().fine("已转换结构集文件: " + fileName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("转换结构集文件失败: " + jsonFile + " - " + e.getMessage());
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
    
    /**
     * 检查是否是Yggdrasil数据包并修复
     */
    public static boolean fixYggdrasilDatapack(File endDatapack) {
        // 检查是否是Yggdrasil数据包
        if (!endDatapack.getName().toLowerCase().contains("yggdrasil")) {
            return false;
        }
        
        Path tempDir = null;
        Path endPacksFolder = Paths.get(Bukkit.getWorldContainer().getPath(), "world", "endpacks");
        
        try {
            // 确保endpacks文件夹存在
            Files.createDirectories(endPacksFolder);
            
            // 创建临时目录
            tempDir = Files.createTempDirectory("yggdrasil_fix_");
            plugin.getLogger().info("修复Yggdrasil数据包，使用临时目录: " + tempDir);
            
            // 解压数据包
            plugin.getLogger().info("解压数据包: " + endDatapack.getName());
            unzipFile(endDatapack.toPath(), tempDir);
            
            // 修复结构文件
            plugin.getLogger().info("修复Yggdrasil结构文件...");
            fixYggdrasilStructures(tempDir);
            
            // 修复维度文件
            plugin.getLogger().info("修复维度配置...");
            fixYggdrasilDimensions(tempDir);
            
            // 保存修复后的数据包
            String originalName = endDatapack.getName();
            String fixedName = originalName.replace(".zip", "").replace(".ZIP", "") + "_fixed.zip";
            Path fixedPath = endPacksFolder.resolve(fixedName);
            
            plugin.getLogger().info("创建修复版数据包: " + fixedName);
            zipFolder(tempDir, fixedPath);
            
            plugin.getLogger().info("✓ 成功修复Yggdrasil数据包: " + fixedName);
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().severe("修复Yggdrasil数据包失败: " + e.getMessage());
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
     * 修复Yggdrasil结构文件
     */
    private static void fixYggdrasilStructures(Path datapackDir) throws IOException {
        Path dataDir = datapackDir.resolve("data").resolve("yggdrasil").resolve("worldgen").resolve("structure");
        if (!Files.exists(dataDir)) {
            return;
        }
        
        // 查找所有Yggdrasil结构文件
        Files.walkFileTree(dataDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".json")) {
                    fixYggdrasilStructureFile(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    /**
     * 修复单个Yggdrasil结构文件
     */
    private static void fixYggdrasilStructureFile(Path jsonFile) {
        try {
            String content = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
            JsonElement element = gson.fromJson(content, JsonElement.class);
            
            if (!element.isJsonObject()) {
                return;
            }
            
            JsonObject json = element.getAsJsonObject();
            String fileName = jsonFile.getFileName().toString();
            
            // 检查文件类型
            if (!json.has("type") || !json.has("biomes")) {
                return;
            }
            
            // 对于Yggdrasil数据包，将生物群系标签改为所有末地外岛 + Nullscape
            JsonElement biomesElem = json.get("biomes");
            if (biomesElem.isJsonPrimitive() && biomesElem.getAsString().startsWith("#yggdrasil:")) {
                // 改为末地外岛 + Nullscape
                JsonArray endBiomes = new JsonArray();
                endBiomes.add("minecraft:end_highlands");
                endBiomes.add("minecraft:end_midlands");
                endBiomes.add("minecraft:end_barrens");
                endBiomes.add("minecraft:small_end_islands");
                //endBiomes.add("nullscape:crystal_peaks");
                //endBiomes.add("nullscape:shadowlands");
                //endBiomes.add("nullscape:void_barrens");
                json.add("biomes", endBiomes);
                
                plugin.getLogger().info("修复Yggdrasil结构生物群系: " + fileName);
            }
            
            // 保存文件
            String newContent = gson.toJson(json);
            Files.write(jsonFile, newContent.getBytes(StandardCharsets.UTF_8));
            
        } catch (Exception e) {
            plugin.getLogger().warning("修复Yggdrasil结构文件失败: " + jsonFile + " - " + e.getMessage());
        }
    }
    
    /**
     * 修复Yggdrasil维度配置
     */
    private static void fixYggdrasilDimensions(Path datapackDir) throws IOException {
        // 移除可能引起问题的维度配置
        Path dimensionDir = datapackDir.resolve("data").resolve("yggdrasil").resolve("dimension_type");
        if (Files.exists(dimensionDir)) {
            deleteDirectory(dimensionDir);
            plugin.getLogger().info("已移除Yggdrasil维度类型配置");
        }
        
        // 移除自定义维度
        Path customDimensionDir = datapackDir.resolve("data").resolve("yggdrasil").resolve("dimension");
        if (Files.exists(customDimensionDir)) {
            deleteDirectory(customDimensionDir);
            plugin.getLogger().info("已移除Yggdrasil自定义维度配置");
        }
    }
}