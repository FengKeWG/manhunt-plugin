package org.windguest.manhunt.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.menus.ModesMenu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainCommand implements CommandExecutor, TabCompleter {
    
    private final ChooseModeCommand chooseModeCommand = new ChooseModeCommand();
    private final ChaosEndBuildCommand chaosEndBuildCommand = new ChaosEndBuildCommand();
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, 
                           @NotNull String label, String[] args) {
        
        if (args.length == 0) {
            // 没有参数，打开投票菜单（如果是玩家）
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ModesMenu.open(player);
                return true;
            } else {
                sender.sendMessage("控制台使用方法: /mh choose <模式>");
                sender.sendMessage("              /mh chaosend <build|fix|list>");
                return true;
            }
        }
        
        String subCommand = args[0].toLowerCase();
        
        // 处理子命令
        switch (subCommand) {
            case "choose":
                return chooseModeCommand.onCommand(sender, command, label, 
                                                 Arrays.copyOfRange(args, 1, args.length));
            case "chaosend":
                return handleChaosEndCommand(sender, command, label, args);
            case "start":
                return handleStartCommand(sender);
            case "stop":
                return handleStopCommand(sender);
            case "vote":
                return handleVoteCommand(sender);
            case "help":
                return handleHelpCommand(sender);
            case "mode":
            case "modes":
                return handleModeCommand(sender);
            case "status":
                return handleStatusCommand(sender);
            default:
                sender.sendMessage("§c未知的子命令！输入 §a/mh help §c查看可用命令");
                return true;
        }
    }
    
    /**
     * 处理 chaosend 命令
     */
    private boolean handleChaosEndCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 1) {
            // 传递所有参数给 ChaosEndBuildCommand
            return chaosEndBuildCommand.onCommand(sender, command, label, 
                                                Arrays.copyOfRange(args, 1, args.length));
        } else {
            // 如果没有子命令，显示帮助
            sender.sendMessage("§c使用方法: /mh chaosend <build|fix|list>");
            sender.sendMessage("§7build <数据包名称> [simple] - 构建混沌末地数据包");
            sender.sendMessage("§7fix <数据包名称> - 修复末地数据包");
            sender.sendMessage("§7list - 列出可用数据包");
            sender.sendMessage("");
            sender.sendMessage("§e示例:");
            sender.sendMessage("§7  /mh chaosend build village_pack");
            sender.sendMessage("§7  /mh chaosend build village_pack simple");
            sender.sendMessage("§7  /mh chaosend fix yggdrasil_end");
            sender.sendMessage("§7  /mh chaosend list");
            return true;
        }
    }
    
    /**
     * 处理 start 命令
     */
    private boolean handleStartCommand(CommandSender sender) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("§c你没有权限使用这个命令！");
            return true;
        }
        
        // 检查游戏状态
        if (Game.getCurrentState() != Game.GameState.WAITING) {
            sender.sendMessage("§c游戏已经在进行中，无法重新开始！");
            return true;
        }
        
        // 检查模式是否已确定
        Mode.GameMode currentMode = Mode.getCurrentMode();
        if (currentMode == null) {
            sender.sendMessage("§c[❌] 游戏模式未选择，无法开始游戏！");
            sender.sendMessage("§7请先使用 §a/mh vote §7投票选择模式");
            sender.sendMessage("§7或使用 §a/mh choose <模式> §7直接设置模式");
            return true;
        }
        
        try {
            // 直接调用Game.startWaitingCountdown()，它会根据游戏状态检查是否可以开始
            Game.startWaitingCountdown();
            sender.sendMessage("§a✓ 已开始游戏倒计时！");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c开始游戏失败: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * 处理 stop 命令
     */
    private boolean handleStopCommand(CommandSender sender) {
        if (!sender.hasPermission("manhunt.admin")) {
            sender.sendMessage("§c你没有权限使用这个命令！");
            return true;
        }
        
        Game.setCurrentState(Game.GameState.WAITING);
        sender.sendMessage("§a✓ 已停止当前游戏！");
        return true;
    }
    
    /**
     * 处理 vote 命令
     */
    private boolean handleVoteCommand(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            ModesMenu.open(player);
        } else {
            sender.sendMessage("§c只有玩家可以投票！");
        }
        return true;
    }
    
    /**
     * 处理 help 命令
     */
    private boolean handleHelpCommand(CommandSender sender) {
        sender.sendMessage("§6ManHunt 帮助");
        sender.sendMessage("§a/mh §7- 打开模式投票菜单（玩家）");
        sender.sendMessage("§a/mh vote §7- 打开投票菜单");
        sender.sendMessage("§a/mh modes §7- 查看当前模式和投票状态");
        sender.sendMessage("§a/mh status §7- 查看游戏状态");
        sender.sendMessage("§a/mh choose <模式> §7- 管理员直接设置模式");
        sender.sendMessage("§a/mh chaosend §7- 混沌末地数据包工具");
        sender.sendMessage("§a/mh start §7- 开始游戏（管理员）");
        sender.sendMessage("§a/mh stop §7- 停止游戏（管理员）");
        sender.sendMessage("§a/mh help §7- 显示此帮助");
        sender.sendMessage("");
        sender.sendMessage("§6混沌末地数据包命令:");
        sender.sendMessage("§a/mh chaosend build <数据包> [simple]");
        sender.sendMessage("§7  - 构建混沌末地数据包");
        sender.sendMessage("§a/mh chaosend fix <数据包>");
        sender.sendMessage("§7  - 修复末地数据包");
        sender.sendMessage("§a/mh chaosend list");
        sender.sendMessage("§7  - 列出可用数据包");
        return true;
    }
    
    /**
     * 处理 mode 命令
     */
    private boolean handleModeCommand(CommandSender sender) {
        Mode.GameMode currentMode = Mode.getCurrentMode();
        String modeName = Mode.getCurrentModeName();
        
        sender.sendMessage("§6====== 当前游戏模式 ======");
        sender.sendMessage("§7模式: " + modeName);
        sender.sendMessage("§7状态: " + getGameStatusText());
        
        if (currentMode == null) {
            sender.sendMessage("");
            sender.sendMessage("§e游戏模式尚未选择！");
            sender.sendMessage("§7请使用 §a/mh vote §7投票或");
            sender.sendMessage("§7管理员使用 §a/mh choose §7设置模式");
        }
        
        // 显示投票信息
        int voteCount = Mode.getPreferences().size();
        if (voteCount > 0) {
            sender.sendMessage("");
            sender.sendMessage("§7当前投票人数: §a" + voteCount);
            
            // 统计各模式票数
            java.util.Map<Mode.GameMode, Long> votes = 
                Mode.getPreferences().values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(e -> e, java.util.stream.Collectors.counting()));
            
            votes.forEach((mode, count) -> {
                String name = Mode.getModeName(mode);
                sender.sendMessage("§7  " + name + "§7: §a" + count + " 票");
            });
        }
        
        sender.sendMessage("§6=========================");
        return true;
    }
    
    /**
     * 处理 status 命令
     */
    private boolean handleStatusCommand(CommandSender sender) {
        Game.GameState state = Game.getCurrentState();
        Mode.GameMode mode = Mode.getCurrentMode();
        
        sender.sendMessage("§6====== 游戏状态 ======");
        sender.sendMessage("§7状态: " + getGameStateText(state));
        sender.sendMessage("§7模式: " + (mode != null ? Mode.getModeName(mode) : "§7未选择"));
        
        switch (state) {
            case WAITING:
                sender.sendMessage("§7等待玩家加入...");
                break;
            case COUNTDOWN_STARTED:
                int countdown = Game.getCountdown();
                sender.sendMessage("§7倒计时: §a" + countdown + " 秒");
                break;
            case FROZEN:
                int freezeTime = Game.getCountdown();
                sender.sendMessage("§7冻结时间: §a" + freezeTime + " 秒");
                break;
            case RUNNING:
                long elapsed = Game.getGameElapsedTime();
                sender.sendMessage("§7游戏已进行: §a" + elapsed + " 秒");
                break;
            case ENDED:
                sender.sendMessage("§7游戏已结束");
                break;
        }
        
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        sender.sendMessage("§7在线玩家: §a" + onlinePlayers);
        
        sender.sendMessage("§6====================");
        return true;
    }
    
    /**
     * 获取游戏状态文本
     */
    private String getGameStateText(Game.GameState state) {
        switch (state) {
            case WAITING:
                return "§a等待中";
            case COUNTDOWN_STARTED:
                return "§6倒计时中";
            case FROZEN:
                return "§b冻结中";
            case RUNNING:
                return "§c进行中";
            case PAUSED:
                return "§e暂停中";
            case ENDED:
                return "§7已结束";
            default:
                return "§7未知";
        }
    }
    
    /**
     * 获取游戏状态文本
     */
    private String getGameStatusText() {
        Game.GameState state = Game.getCurrentState();
        return getGameStateText(state);
    }
    
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, 
                                               @NotNull Command command, 
                                               @NotNull String alias, 
                                               String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // 主命令补全
            completions.add("choose");
            completions.add("chaosend");
            completions.add("start");
            completions.add("stop");
            completions.add("vote");
            completions.add("modes");
            completions.add("status");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("choose")) {
            // /mh choose 的子命令补全
            return chooseModeCommand.onTabComplete(sender, command, alias, 
                                                 Arrays.copyOfRange(args, 1, args.length));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("chaosend")) {
            // /mh chaosend 的子命令
            completions.add("build");
            completions.add("fix");
            completions.add("list");
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("chaosend")) {
            // 将剩余参数传递给 ChaosEndBuildCommand 进行补全
            return chaosEndBuildCommand.onTabComplete(sender, command, alias,
                                                    Arrays.copyOfRange(args, 1, args.length));
        }
        
        // 过滤匹配项
        String partial = args[args.length - 1].toLowerCase();
        List<String> filtered = new ArrayList<>();
        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(partial)) {
                filtered.add(completion);
            }
        }
        
        return filtered;
    }
}