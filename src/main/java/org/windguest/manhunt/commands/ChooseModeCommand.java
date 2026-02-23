package org.windguest.manhunt.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChooseModeCommand implements CommandExecutor, TabCompleter {
    
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
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /mh choose <模式> [confirm|确认]");
            sender.sendMessage(ChatColor.GRAY + "可用模式: manhunt, team, end");
            sender.sendMessage(ChatColor.YELLOW + "示例:");
            sender.sendMessage(ChatColor.GRAY + "  /mh choose end confirm - 切换到混沌末地模式（需要确认）");
            sender.sendMessage(ChatColor.GRAY + "  /mh choose team - 切换到团队模式");
            return true;
        }
        
        // 检查游戏状态
        if (Game.getCurrentState() != Game.GameState.WAITING) {
            sender.sendMessage(ChatColor.RED + "游戏已经开始，无法更改模式！");
            return true;
        }
        
        String modeArg = args[0].toLowerCase();
        Mode.GameMode selectedMode;
        
        // 解析模式
        switch (modeArg) {
            case "manhunt":
                selectedMode = Mode.GameMode.MANHUNT;
                break;
            case "team":
                selectedMode = Mode.GameMode.TEAM;
                break;
            case "end":
                selectedMode = Mode.GameMode.END;
                // 检查是否有确认参数
                if (args.length < 2 || (!args[1].equalsIgnoreCase("confirm") && !args[1].equalsIgnoreCase("确认"))) {
                    sender.sendMessage(ChatColor.RED + "⚠：切换到混沌末地模式将立即切换数据包并重启服务器！");
                    sender.sendMessage(ChatColor.RED + "所有在线玩家将被踢出，服务器将重启！");
                    sender.sendMessage(ChatColor.YELLOW + "请确认使用: /mh choose end confirm");
                    sender.sendMessage(ChatColor.GRAY + "或: /mh choose end 确认");
                    return true;
                }
                break;
            case "浑沌末地":
            case "混沌末地":
                selectedMode = Mode.GameMode.END;
                // 检查是否有确认参数
                if (args.length < 2 || (!args[1].equalsIgnoreCase("confirm") && !args[1].equalsIgnoreCase("确认"))) {
                    sender.sendMessage(ChatColor.RED + "⚠：切换到混沌末地模式将立即切换数据包并重启服务器！");
                    sender.sendMessage(ChatColor.RED + "所有在线玩家将被踢出，服务器将重启！");
                    sender.sendMessage(ChatColor.YELLOW + "请确认使用: /mh choose 浑沌末地 confirm");
                    sender.sendMessage(ChatColor.GRAY + "或: /mh choose 浑沌末地 确认");
                    return true;
                }
                break;
            case "追杀模式":
                selectedMode = Mode.GameMode.MANHUNT;
                break;
            case "团队模式":
                selectedMode = Mode.GameMode.TEAM;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "未知的游戏模式！");
                sender.sendMessage(ChatColor.YELLOW + "可用模式: manhunt, team, end");
                sender.sendMessage(ChatColor.YELLOW + "或: 追杀模式, 团队模式, 浑沌末地");
                return true;
        }
        
        // 获取当前模式
        Mode.GameMode currentMode = Mode.getCurrentMode();
        
        // 如果模式没有变化
        if (currentMode == selectedMode) {
            sender.sendMessage(ChatColor.YELLOW + "当前已经是 " + Mode.getModeName(selectedMode) + ChatColor.YELLOW + " 模式！");
            return true;
        }
        
        // 设置模式（会自动处理数据包切换和重启）
        Mode.setCurrentMode(selectedMode);
        
        return true;
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, 
                                               @NotNull Command command, 
                                               @NotNull String alias, 
                                               String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 第一个参数：模式名称
            List<String> modes = Arrays.asList("manhunt", "team", "end", 
                                             "追杀模式", "团队模式", "浑沌末地");
            
            String partial = args[0].toLowerCase();
            for (String mode : modes) {
                if (mode.toLowerCase().startsWith(partial)) {
                    completions.add(mode);
                }
            }
        } else if (args.length == 2) {
            // 第二个参数：确认词（仅在end模式时需要）
            String firstArg = args[0].toLowerCase();
            if (firstArg.equals("end") || firstArg.equals("浑沌末地") || firstArg.equals("混沌末地")) {
                List<String> confirmOptions = Arrays.asList("confirm", "确认");
                
                String partial = args[1].toLowerCase();
                for (String option : confirmOptions) {
                    if (option.startsWith(partial)) {
                        completions.add(option);
                    }
                }
            }
        }
        
        return completions;
    }
}