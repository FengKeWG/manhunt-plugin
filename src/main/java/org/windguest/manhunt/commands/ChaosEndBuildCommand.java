package org.windguest.manhunt.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.windguest.manhunt.utils.EndDatapackBuilder;
import org.windguest.manhunt.utils.SimpleEndConverter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChaosEndBuildCommand implements CommandExecutor, TabCompleter {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, String[] args) {
        
        // 检查权限
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用这个命令！");
            return true;
        }
        
        // 检查参数
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "====== 混沌末地数据包工具 ======");
            sender.sendMessage(ChatColor.AQUA + "/mh chaosend build <数据包名称> [simple|force]" + ChatColor.GRAY + " - 构建末地数据包");
            sender.sendMessage(ChatColor.GRAY + "  simple: 简化模式，只修改生物群系");
            sender.sendMessage(ChatColor.GRAY + "  force: 强制模式，即使文件名包含_end也强制转换");
            sender.sendMessage(ChatColor.AQUA + "/mh chaosend build all [simple|force]" + ChatColor.GRAY + " - 一键构建所有可用数据包");
            sender.sendMessage(ChatColor.AQUA + "/mh chaosend fix <数据包名称>" + ChatColor.GRAY + " - 修复末地数据包");
            sender.sendMessage(ChatColor.AQUA + "/mh chaosend list" + ChatColor.GRAY + " - 列出可用数据包");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "示例:");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend build village_pack" + ChatColor.DARK_GRAY + " - 标准模式");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend build village_pack simple" + ChatColor.DARK_GRAY + " - 简化模式");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend build yggdrasil_end force" + ChatColor.DARK_GRAY + " - 强制转换");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend build all" + ChatColor.DARK_GRAY + " - 一键构建所有数据包");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend build all simple" + ChatColor.DARK_GRAY + " - 一键构建所有数据包(简化模式)");
            sender.sendMessage(ChatColor.GRAY + "  /mh chaosend fix yggdrasil_end" + ChatColor.DARK_GRAY + " - 修复Yggdrasil数据包");
            sender.sendMessage(ChatColor.YELLOW + "================================");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        // 处理子命令
        switch (subCommand) {
            case "build":
                return handleBuildCommand(sender, args);
            case "fix":
                return handleFixCommand(sender, args);
            case "list":
                return handleListCommand(sender);
            default:
                sender.sendMessage(ChatColor.RED + "未知的子命令！");
                sender.sendMessage(ChatColor.YELLOW + "使用: /mh chaosend 查看可用命令");
                return true;
        }
    }
    
    /**
     * 处理 build 命令
     */
    private boolean handleBuildCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用方法: /mh chaosend build <数据包名称> [simple|force]");
            sender.sendMessage(ChatColor.RED + "或: /mh chaosend build all [simple|force]");
            sender.sendMessage(ChatColor.GRAY + "simple: 简化模式，只修改生物群系和维度");
            sender.sendMessage(ChatColor.GRAY + "force: 强制模式，即使文件名包含_end也强制转换");
            return true;
        }
        
        // 检查是否是一键构建所有数据包
        if (args[1].equalsIgnoreCase("all")) {
            return handleBuildAllCommand(sender, args);
        }
        
        // 处理带空格的数据包名称
        String datapackName;
        String modeArg = null;
        boolean simpleMode = false;
        boolean forceMode = false;
        
        // 检查最后一个参数是否是模式参数
        if (args.length > 2) {
            String lastArg = args[args.length - 1].toLowerCase();
            if ("simple".equals(lastArg) || "force".equals(lastArg)) {
                // 最后一个参数是模式，前面的都是数据包名称
                modeArg = lastArg;
                datapackName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
            } else {
                // 没有模式参数，所有参数都是数据包名称的一部分
                datapackName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        } else {
            // 只有数据包名称
            datapackName = args[1];
        }
        
        // 解析模式参数
        if (modeArg != null) {
            if ("simple".equals(modeArg)) {
                simpleMode = true;
            } else if ("force".equals(modeArg)) {
                forceMode = true;
            }
        }
        
        // 检查数据包是否存在
        File commonPacksFolder = new File(Bukkit.getWorldContainer(), "world/commonpacks");
        File datapackFile = findDatapackFile(commonPacksFolder, datapackName);
        
        if (datapackFile == null) {
            sender.sendMessage(ChatColor.RED + "数据包 '" + datapackName + "' 在 commonpacks 文件夹中不存在！");
            // 提供更友好的错误信息，显示实际文件名
            if (datapackName.contains(" ")) {
                sender.sendMessage(ChatColor.GRAY + "提示: 带空格的文件名请确保使用引号或正确指定完整文件名");
            }
            sender.sendMessage(ChatColor.YELLOW + "可用数据包:");
            listDatapacks(sender, commonPacksFolder);
            return true;
        }
        
        // 检查是否是末地数据包（避免重复转换）除非是强制模式
        if (EndDatapackBuilder.isEndDatapack(datapackFile) && !forceMode) {
            sender.sendMessage(ChatColor.YELLOW + "⚠ 这个数据包看起来已经是末地版本了！");
            sender.sendMessage(ChatColor.GRAY + "文件名包含 '_end'、'_simple_end' 或 '_fixed'，可能是已经转换过的。");
            sender.sendMessage(ChatColor.GRAY + "如果需要强制重新转换，请使用 force 参数:");
            sender.sendMessage(ChatColor.AQUA + "  /mh chaosend build " + datapackName + " force");
            sender.sendMessage(ChatColor.GRAY + "或者先删除或重命名原文件。");
            return true;
        }
        
        // 构建末地版本数据包
        if (simpleMode) {
            sender.sendMessage(ChatColor.YELLOW + "正在使用简化模式构建混沌末地数据包...");
            sender.sendMessage(ChatColor.GRAY + "来源: " + datapackFile.getName());
            sender.sendMessage(ChatColor.GRAY + "模式: 简化（只修改生物群系和维度）");
        } else if (forceMode) {
            sender.sendMessage(ChatColor.YELLOW + "正在使用强制模式构建混沌末地数据包...");
            sender.sendMessage(ChatColor.GRAY + "来源: " + datapackFile.getName());
            sender.sendMessage(ChatColor.GRAY + "模式: 强制（忽略文件名检查）");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "正在使用标准模式构建混沌末地数据包...");
            sender.sendMessage(ChatColor.GRAY + "来源: " + datapackFile.getName());
            sender.sendMessage(ChatColor.GRAY + "模式: 标准（包含地形适配和错误修复）");
        }
        
        try {
            boolean success;
            String endPackName;
            
            if (simpleMode) {
                success = EndDatapackBuilder.buildSimpleEndDatapack(datapackFile);
                endPackName = datapackFile.getName().replace(".zip", "").replace(".ZIP", "") + "_simple_end.zip";
            } else {
                success = EndDatapackBuilder.buildEndDatapack(datapackFile);
                endPackName = datapackFile.getName().replace(".zip", "").replace(".ZIP", "") + "_end.zip";
            }
            
            if (success) {
                sender.sendMessage(ChatColor.GREEN + "✓ 成功创建混沌末地数据包: " + endPackName);
                sender.sendMessage(ChatColor.GRAY + "已保存到: world/endpacks/");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + "使用说明:");
                sender.sendMessage(ChatColor.GRAY + "1. 使用 " + ChatColor.AQUA + "/mh choose end confirm" + ChatColor.GRAY + " 切换到混沌末地模式");
                sender.sendMessage(ChatColor.GRAY + "2. 服务器将重启并加载新数据包");
                sender.sendMessage(ChatColor.GRAY + "3. 开始游戏，结构将在末地随机生成");
                
                // 如果当前模式是混沌末地模式，提示需要重启
                if (org.windguest.manhunt.game.Mode.getCurrentMode() == 
                    org.windguest.manhunt.game.Mode.GameMode.END) {
                    sender.sendMessage("");
                    sender.sendMessage(ChatColor.YELLOW + "提示: 当前为混沌末地模式，使用 /mh choose end confirm 重启以应用新数据包");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "✗ 构建混沌末地数据包失败！");
                sender.sendMessage(ChatColor.GRAY + "请检查控制台日志获取详细错误信息。");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "构建过程中发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.GRAY + "详细信息请查看控制台。");
        }
        
        return true;
    }
    
    /**
     * 处理一键构建所有数据包命令
     */
    private boolean handleBuildAllCommand(CommandSender sender, String[] args) {
        // 解析模式参数
        boolean simpleMode = false;
        boolean forceMode = false;
        
        if (args.length > 2) {
            String modeArg = args[2].toLowerCase();
            if ("simple".equals(modeArg)) {
                simpleMode = true;
            } else if ("force".equals(modeArg)) {
                forceMode = true;
            }
        }
        
        File commonPacksFolder = new File(Bukkit.getWorldContainer(), "world/commonpacks");
        if (!commonPacksFolder.exists() || !commonPacksFolder.isDirectory()) {
            sender.sendMessage(ChatColor.RED + "commonpacks 文件夹不存在或不可访问！");
            return true;
        }
        
        File[] allFiles = commonPacksFolder.listFiles();
        if (allFiles == null || allFiles.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "commonpacks 文件夹中没有数据包！");
            return true;
        }
        
        // 过滤出ZIP文件
        List<File> datapackFiles = Arrays.stream(allFiles)
                .filter(file -> file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP")))
                .collect(Collectors.toList());
        
        if (datapackFiles.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "没有找到可用的数据包文件！");
            return true;
        }
        
        // 如果不是强制模式，过滤掉已经是末地版本的数据包
        if (!forceMode) {
            datapackFiles = datapackFiles.stream()
                    .filter(file -> !EndDatapackBuilder.isEndDatapack(file))
                    .collect(Collectors.toList());
        }
        
        if (datapackFiles.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "所有数据包看起来都已经是末地版本了！");
            if (!forceMode) {
                sender.sendMessage(ChatColor.GRAY + "如果需要强制重新构建所有数据包，请使用 force 参数:");
                sender.sendMessage(ChatColor.AQUA + "  /mh chaosend build all force");
            }
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "====== 开始一键构建所有数据包 ======");
        sender.sendMessage(ChatColor.GRAY + "找到 " + datapackFiles.size() + " 个数据包需要处理");
        sender.sendMessage(ChatColor.GRAY + "模式: " + (simpleMode ? "简化" : forceMode ? "强制" : "标准"));
        sender.sendMessage("");
        
        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < datapackFiles.size(); i++) {
            File datapackFile = datapackFiles.get(i);
            String fileName = datapackFile.getName();
            
            // 发送进度消息
            sender.sendMessage(ChatColor.GRAY + "[" + (i + 1) + "/" + datapackFiles.size() + "] 处理: " + fileName);
            
            try {
                // 检查是否是末地数据包（避免重复转换）除非是强制模式
                if (EndDatapackBuilder.isEndDatapack(datapackFile) && !forceMode) {
                    sender.sendMessage(ChatColor.DARK_GRAY + "  → 跳过: 已经是末地版本");
                    skipCount++;
                    continue;
                }
                
                boolean success;
                
                if (simpleMode) {
                    success = EndDatapackBuilder.buildSimpleEndDatapack(datapackFile);
                } else {
                    success = EndDatapackBuilder.buildEndDatapack(datapackFile);
                }
                
                if (success) {
                    sender.sendMessage(ChatColor.GREEN + "  ✓ 成功");
                    successCount++;
                } else {
                    sender.sendMessage(ChatColor.RED + "  ✗ 失败");
                    failCount++;
                }
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "  ✗ 错误: " + e.getMessage());
                failCount++;
            }
        }
        
        sender.sendMessage("");
        sender.sendMessage(ChatColor.YELLOW + "====== 一键构建完成 ======");
        sender.sendMessage(ChatColor.GREEN + "成功: " + successCount + " 个");
        if (skipCount > 0) {
            sender.sendMessage(ChatColor.YELLOW + "跳过: " + skipCount + " 个（已是末地版本）");
        }
        if (failCount > 0) {
            sender.sendMessage(ChatColor.RED + "失败: " + failCount + " 个");
            sender.sendMessage(ChatColor.GRAY + "请检查控制台日志获取详细错误信息。");
        }
        
        if (successCount > 0) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.YELLOW + "使用说明:");
            sender.sendMessage(ChatColor.GRAY + "1. 使用 " + ChatColor.AQUA + "/mh choose end confirm" + ChatColor.GRAY + " 切换到混沌末地模式");
            sender.sendMessage(ChatColor.GRAY + "2. 服务器将重启并加载新数据包");
            sender.sendMessage(ChatColor.GRAY + "3. 开始游戏，结构将在末地随机生成");
        }
        
        return true;
    }
    
    /**
     * 处理 fix 命令
     */
    private boolean handleFixCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "使用方法: /mh chaosend fix <数据包名称>");
            sender.sendMessage(ChatColor.GRAY + "修复末地数据包中的问题");
            return true;
        }
        
        // 处理带空格的数据包名称
        String datapackName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // 检查数据包是否存在
        File endPacksFolder = new File(Bukkit.getWorldContainer(), "world/endpacks");
        File datapackFile = findDatapackFile(endPacksFolder, datapackName);
        
        if (datapackFile == null) {
            sender.sendMessage(ChatColor.RED + "数据包 '" + datapackName + "' 在 endpacks 文件夹中不存在！");
            sender.sendMessage(ChatColor.YELLOW + "endpacks 中的可用数据包:");
            listDatapacks(sender, endPacksFolder);
            return true;
        }
        
        // 检查是否是Yggdrasil数据包
        boolean isYggdrasil = datapackFile.getName().toLowerCase().contains("yggdrasil");
        
        if (isYggdrasil) {
            sender.sendMessage(ChatColor.YELLOW + "正在修复Yggdrasil数据包...");
            sender.sendMessage(ChatColor.GRAY + "修复: " + datapackFile.getName());
            sender.sendMessage(ChatColor.GRAY + "操作: 修复生物群系标签和移除问题维度");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "正在修复数据包...");
            sender.sendMessage(ChatColor.GRAY + "修复: " + datapackFile.getName());
            sender.sendMessage(ChatColor.GRAY + "操作: 修复结构和维度配置");
        }
        
        try {
            boolean success;
            
            if (isYggdrasil) {
                success = EndDatapackBuilder.fixYggdrasilDatapack(datapackFile);
            } else {
                // 对于非Yggdrasil数据包，使用SimpleEndConverter修复
                success = SimpleEndConverter.fixYggdrasilDatapack(datapackFile);
            }
            
            if (success) {
                String fixedName = datapackFile.getName().replace(".zip", "").replace(".ZIP", "") + "_fixed.zip";
                sender.sendMessage(ChatColor.GREEN + "✓ 成功修复数据包: " + fixedName);
                sender.sendMessage(ChatColor.GRAY + "已保存到: world/endpacks/");
                sender.sendMessage("");
                sender.sendMessage(ChatColor.YELLOW + "修复内容:");
                if (isYggdrasil) {
                    sender.sendMessage(ChatColor.GRAY + "1. 修复Yggdrasil生物群系标签");
                    sender.sendMessage(ChatColor.GRAY + "2. 移除问题维度配置");
                    sender.sendMessage(ChatColor.GRAY + "3. 确保结构在末地正确生成");
                } else {
                    sender.sendMessage(ChatColor.GRAY + "1. 修复结构生物群系");
                    sender.sendMessage(ChatColor.GRAY + "2. 修复维度配置");
                    sender.sendMessage(ChatColor.GRAY + "3. 确保兼容性");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "✗ 修复数据包失败！");
                sender.sendMessage(ChatColor.GRAY + "请检查控制台日志获取详细错误信息。");
            }
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "修复过程中发生错误: " + e.getMessage());
            sender.sendMessage(ChatColor.GRAY + "详细信息请查看控制台。");
        }
        
        return true;
    }
    
    /**
     * 处理 list 命令
     */
    private boolean handleListCommand(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "====== 数据包列表 ======");
        
        // 列出 commonpacks 中的数据包
        File commonPacksFolder = new File(Bukkit.getWorldContainer(), "world/commonpacks");
        if (commonPacksFolder.exists() && commonPacksFolder.isDirectory()) {
            sender.sendMessage(ChatColor.AQUA + "commonpacks (普通模式):");
            File[] commonFiles = commonPacksFolder.listFiles();
            if (commonFiles != null && commonFiles.length > 0) {
                for (File file : commonFiles) {
                    if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP"))) {
                        String name = file.getName().replace(".zip", "").replace(".ZIP", "");
                        if (EndDatapackBuilder.isEndDatapack(file)) {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.RED + name + ChatColor.DARK_GRAY + " (末地版本)");
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.GREEN + name);
                        }
                    }
                }
            } else {
                sender.sendMessage(ChatColor.GRAY + "  (空)");
            }
        }
        
        // 列出 endpacks 中的数据包
        File endPacksFolder = new File(Bukkit.getWorldContainer(), "world/endpacks");
        if (endPacksFolder.exists() && endPacksFolder.isDirectory()) {
            sender.sendMessage("");
            sender.sendMessage(ChatColor.AQUA + "endpacks (混沌末地模式):");
            File[] endFiles = endPacksFolder.listFiles();
            if (endFiles != null && endFiles.length > 0) {
                for (File file : endFiles) {
                    if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP"))) {
                        String name = file.getName();
                        if (name.contains("_simple_end")) {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.YELLOW + name + ChatColor.DARK_GRAY + " (简化版)");
                        } else if (name.contains("_fixed")) {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.GOLD + name + ChatColor.DARK_GRAY + " (修复版)");
                        } else if (name.contains("_end")) {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.AQUA + name + ChatColor.DARK_GRAY + " (标准版)");
                        } else {
                            sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.WHITE + name);
                        }
                    }
                }
            } else {
                sender.sendMessage(ChatColor.GRAY + "  (空)");
            }
        }
        
        sender.sendMessage(ChatColor.YELLOW + "========================");
        sender.sendMessage(ChatColor.GRAY + "提示: 使用 " + ChatColor.AQUA + "/mh chaosend build <名称>" + ChatColor.GRAY + " 从commonpacks构建末地版本");
        sender.sendMessage(ChatColor.GRAY + "提示: 使用 " + ChatColor.AQUA + "/mh chaosend build all" + ChatColor.GRAY + " 一键构建所有数据包");
        return true;
    }
    
    /**
     * 查找数据包文件
     */
    private File findDatapackFile(File folder, String datapackName) {
        // 查找数据包文件（支持 .zip 扩展名或不带扩展名）
        File[] possibleFiles = {
            new File(folder, datapackName),
            new File(folder, datapackName + ".zip"),
            new File(folder, datapackName + ".ZIP")
        };
        
        for (File file : possibleFiles) {
            if (file.exists() && file.isFile()) {
                return file;
            }
        }
        return null;
    }
    
    /**
     * 列出数据包
     */
    private void listDatapacks(CommandSender sender, File folder) {
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP"))) {
                        String name = file.getName().replace(".zip", "").replace(".ZIP", "");
                        sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.AQUA + name);
                    }
                }
            } else {
                sender.sendMessage(ChatColor.GRAY + "  (空)");
            }
        } else {
            sender.sendMessage(ChatColor.GRAY + "  (文件夹不存在)");
        }
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, 
                                               @NotNull Command command, 
                                               @NotNull String alias, 
                                               String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一个参数：子命令
            completions.add("build");
            completions.add("fix");
            completions.add("list");
        } else if (args.length == 2) {
            // 第二个参数：数据包名称或"all"
            String firstArg = args[0].toLowerCase();
            
            if (firstArg.equals("build")) {
                // build 命令：从 commonpacks 中补全，包括"all"
                completions.add("all");
                File commonPacksFolder = new File(Bukkit.getWorldContainer(), "world/commonpacks");
                if (commonPacksFolder.exists() && commonPacksFolder.isDirectory()) {
                    File[] files = commonPacksFolder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP"))) {
                                String name = file.getName().replace(".zip", "").replace(".ZIP", "");
                                // 跳过已经是末地版本的数据包（但允许在force模式下）
                                if (!EndDatapackBuilder.isEndDatapack(file)) {
                                    completions.add(name);
                                } else {
                                    // 如果是末地版本，也显示但加标记
                                    completions.add(name + " [末地版]");
                                }
                            }
                        }
                    }
                }
            } else if (firstArg.equals("fix")) {
                // fix 命令：从 endpacks 中补全
                File endPacksFolder = new File(Bukkit.getWorldContainer(), "world/endpacks");
                if (endPacksFolder.exists() && endPacksFolder.isDirectory()) {
                    File[] files = endPacksFolder.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile() && (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP"))) {
                                String name = file.getName().replace(".zip", "").replace(".ZIP", "");
                                completions.add(name);
                            }
                        }
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("build")) {
            // 第三个参数：build 命令的选项
            completions.add("simple");
            completions.add("force");
        }
        
        // 过滤匹配项
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(name -> name.toLowerCase().startsWith(partial.replace("[末地版]", "").trim()))
                .collect(Collectors.toList());
    }
}