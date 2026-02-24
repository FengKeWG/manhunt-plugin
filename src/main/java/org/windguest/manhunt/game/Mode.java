package org.windguest.manhunt.game;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.DataPackManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Mode {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static final Map<Player, GameMode> gamemodePreferences = new HashMap<>();
    private static GameMode currentMode = null;
    private static boolean isVotingStarted = false;
    private static BukkitRunnable votingTask = null; 

    public static GameMode getCurrentMode() {
        return currentMode;
    }

    
    /**
     * 检查是否正在投票
     */
    public static boolean isVoting() {
        return isVotingStarted;
    }
    
    /**
     * 强制停止投票
     */
    public static void stopVoting() {
        if (votingTask != null) {
            votingTask.cancel();
            votingTask = null;
        }
        isVotingStarted = false;
        gamemodePreferences.clear();
        Bukkit.broadcastMessage("§e[!] 投票已停止");
    }
    
    /**
     * 清理投票状态（用于服务器启动时）
     */
    public static void cleanupVotingState() {
        if (isVotingStarted) {
            plugin.getLogger().warning("检测到异常的投票状态，正在清理...");
            stopVoting();
        }
    }
    
    
    /**
     * 启动游戏模式投票（60秒）
     * 在混沌末地模式下禁止投票
     */
    public static void startVoting() {
        // 如果当前是END模式，直接返回不启动投票
        if (currentMode == GameMode.END) {
            plugin.getLogger().warning("混沌末地模式下不允许启动投票！");
            Bukkit.broadcastMessage("§c[!] 混沌末地模式下不允许投票！");
            return;
        }
        
        if (isVotingStarted) {
            plugin.getLogger().info("投票已经在进行中");
            return; 
        }
        
        isVotingStarted = true;
        
        votingTask = new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                // 检查是否仍然是投票状态
                if (!isVotingStarted) {
                    this.cancel();
                    return;
                }
                
                // 检查玩家人数，如果少于2人则暂停投票
                int playerCount = Bukkit.getOnlinePlayers().size();
                if (playerCount < 2) {
                    // 非END模式：仅暂停投票，不重置模式
                    if (time % 30 == 0) { // 每30秒提醒一次
                        Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，投票暂停...");
                    }
                    return; // 不减时间，暂停投票
                }
                
                // 如果中途切换到END模式，立即停止投票
                if (currentMode == GameMode.END) {
                    this.cancel();
                    isVotingStarted = false;
                    gamemodePreferences.clear();
                    Bukkit.broadcastMessage("§e[!] 切换到混沌末地模式，投票已取消！");
                    return;
                }
                
                // 广播倒计时
                if (time == 60 || time == 30 || time == 15 || time == 10 || (time <= 5 && time > 0)) {
                    Bukkit.broadcastMessage("§e[!] 游戏模式投票还剩 " + time + " 秒！");
                    Bukkit.getOnlinePlayers()
                            .forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f));
                }
                
                // 投票结束
                if (time <= 0) {
                    this.cancel();
                    calculateWinner();
                    isVotingStarted = false;
                    votingTask = null;
                }
                time--;
            }
        };
        
        votingTask.runTaskTimer(plugin, 0L, 20L);
        Bukkit.broadcastMessage("§a[✔] 游戏模式投票已开始，60秒后结束！");
    }
    
    
    public static void setCurrentMode(GameMode newMode) {
        GameMode oldMode = currentMode;
        
        // 如果模式没有变化，不做任何事
        if (oldMode == newMode) {
            return;
        }
        
        currentMode = newMode;
        
        // 保存当前模式到配置文件
        saveModeToConfig();
        
        // 如果模式是null（重置），则不切换数据包
        if (newMode == null) {
            plugin.getLogger().info("游戏模式已重置为null，等待投票选择");
            Bukkit.broadcastMessage("§e[!] 游戏模式已重置，请投票选择新模式！");
            
            // 如果之前是END模式，需要清理投票状态
            if (oldMode == GameMode.END) {
                cleanupVotingState();
            }
            return;
        }
        
        //当切换到END模式时，强制停止任何正在进行的投票
        if (newMode == GameMode.END && isVotingStarted) {
            plugin.getLogger().info("切换到混沌末地模式，停止正在进行的投票");
            stopVoting();
        }
        
        // 清空投票记录
        gamemodePreferences.clear();
        
        // 广播模式变更
        String modeName = getModeName(newMode);
        Bukkit.broadcastMessage("§6游戏模式已变更为: " + modeName);
        
        // 检查数据包是否需要切换
        if (newMode == GameMode.END) {
            // 检查当前是否已经是END数据包
            boolean usingEndPacks = DataPackManager.isDatapacksUsingEnd();
            if (!usingEndPacks) {
                // 需要切换到混沌末地数据包
                Bukkit.broadcastMessage("§6混沌末地模式需要切换数据包");
                Bukkit.broadcastMessage("§6服务器将在5秒后重启...");
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean success = DataPackManager.switchToEndMode();
                    if (success) {
                        DataPackManager.restartServer();
                    } else {
                        Bukkit.broadcastMessage("§c[❌] 切换混沌末地数据包失败！");
                        // 恢复原来的模式
                        currentMode = oldMode;
                        saveModeToConfig();
                    }
                }, 100L);
            } else {
                // 已经是END数据包，不需要切换
                Bukkit.broadcastMessage("§a[✔] 已经是混沌末地数据包，无需重启");
                
                // 检查是否可以立即开始等待阶段
                if (Game.getCurrentState() == Game.GameState.WAITING && Bukkit.getOnlinePlayers().size() >= 2) {
                    Bukkit.broadcastMessage("§d[🌌] 混沌末地模式确认，开始等待阶段！");
                    // 立即开始等待阶段
                    Game.startWaitingCountdown();
                }
            }
        } else if (newMode == GameMode.MANHUNT || newMode == GameMode.TEAM) {
            // 检查当前是否已经是普通数据包
            
            if (oldMode == GameMode.END) {
                // 从END模式切换到普通模式，需要切换数据包
                Bukkit.broadcastMessage("§6正在切换回普通模式");
                Bukkit.broadcastMessage("§6服务器将在5秒后重启...");
                
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    boolean success = DataPackManager.switchToNormalMode();
                    if (success) {
                        DataPackManager.restartServer();
                    } else {
                        Bukkit.broadcastMessage("§c[❌] 切换普通数据包失败！");
                        // 恢复原来的模式
                        currentMode = oldMode;
                        saveModeToConfig();
                    }
                }, 100L);
            } else {
                // 已经是普通数据包，不需要切换
                Bukkit.broadcastMessage("§6切换完成！");
                Bukkit.broadcastMessage("§6══════════════════════════");
                
                // 检查是否可以立即开始等待阶段
                if (Game.getCurrentState() == Game.GameState.WAITING && Bukkit.getOnlinePlayers().size() >= 2) {
                    Bukkit.broadcastMessage("§6游戏模式已确定，开始等待阶段！");
                    // 立即开始等待阶段
                    Game.startWaitingCountdown();
                }
            }
        }
    }
    
    public static Map<Player, GameMode> getPreferences() {
        return gamemodePreferences;
    }

    public static void setPreference(Player player, GameMode mode) {
        gamemodePreferences.put(player, mode);
    }
    
    private static void calculateWinner() {
        GameMode oldMode = currentMode;
        
        if (gamemodePreferences.isEmpty()) {
            // 没有投票，随机选择但排除END模式
            java.util.List<GameMode> availableModes = java.util.Arrays.stream(GameMode.values())
                    .filter(m -> m != GameMode.END)
                    .collect(java.util.stream.Collectors.toList());
            if (!availableModes.isEmpty()) {
                currentMode = availableModes.get(rand.nextInt(availableModes.size()));
            } else {
                currentMode = GameMode.TEAM; // 默认团队模式
            }
            Bukkit.broadcastMessage("§e[⚠] 没有玩家投票，随机选择模式：" + getModeName(currentMode));
        } else {
            Map<GameMode, Long> votes = gamemodePreferences.values().stream()
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            
            // 找出最高票数
            long maxVotes = 0;
            for (Long voteCount : votes.values()) {
                if (voteCount > maxVotes) {
                    maxVotes = voteCount;
                }
            }
            
            // 找出所有得票最高的模式
            final long finalMaxVotes = maxVotes;
            java.util.List<GameMode> winners = votes.entrySet().stream()
                    .filter(entry -> entry.getValue() == finalMaxVotes)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            
            // 随机选择一个获胜模式
            currentMode = winners.get(rand.nextInt(winners.size()));
            Bukkit.broadcastMessage("§a[✔] 投票结束！最终模式为：" + getModeName(currentMode));
        }
        
        // 保存当前模式
        saveModeToConfig();
        
        // 清空投票记录
        gamemodePreferences.clear();
        
        // 根据投票结果决定下一步
        if (currentMode == GameMode.END) {
            // 混沌末地模式：切换数据包并重启
            boolean usingEndPacks = DataPackManager.isDatapacksUsingEnd();
            if (!usingEndPacks) {
                // 切换到混沌末地模式 - 需要重启
                boolean success = DataPackManager.switchToEndMode();
                if (success) {
                    Bukkit.broadcastMessage("§d[🌌] 正在切换到混沌末地模式，服务器将重启...");
                    DataPackManager.restartServer();
                } else {
                    Bukkit.broadcastMessage("§c[❌] 切换混沌末地模式失败，请检查控制台！");
                    // 恢复原来的模式
                    currentMode = oldMode;
                    saveModeToConfig();
                }
            } else {
                // 已经是END数据包，不需要重启，直接开始游戏
                Bukkit.broadcastMessage("§a[✔] 已经是混沌末地数据包，无需重启");
                Bukkit.broadcastMessage("§d[🌌] 混沌末地模式确认，开始等待阶段！");
                
                // 开始游戏等待倒计时
                if (Game.getCurrentState() == Game.GameState.WAITING && Bukkit.getOnlinePlayers().size() >= 2) {
                    Game.startWaitingCountdown();
                }
            }
        } else {
            // 非END模式（MANHUNT或TEAM）：直接开始游戏
            Bukkit.broadcastMessage("§a[✔] 模式切换完成！");
            Bukkit.broadcastMessage("§6开始等待阶段...");
            
            // 开始游戏等待倒计时
            if (Game.getCurrentState() == Game.GameState.WAITING && Bukkit.getOnlinePlayers().size() >= 2) {
                Game.startWaitingCountdown();
            }
            
            // 启动队伍倾向投票
            TeamsManager.startPrefVoting();
        }
    }

    public static String getModeName(GameMode mode) {
        switch (mode) {
            case MANHUNT:
                return "§a追杀模式";
            case TEAM:
                return "§b团队模式";
            case END:
                return "§d浑沌末地";
            default:
                return "§7未开始";
        }
    }

    /**
     * 保存当前模式到配置文件
     */
    private static void saveModeToConfig() {
        try {
            plugin.getConfig().set("current-mode", currentMode != null ? currentMode.name() : "null");
            plugin.saveConfig();
            plugin.getLogger().info("已保存当前模式到配置文件: " + (currentMode != null ? getModeName(currentMode) : "null"));
        } catch (Exception e) {
            plugin.getLogger().severe("保存模式到配置文件时出错: " + e.getMessage());
        }
    }

    /**
     * 从配置文件加载模式
     */
    public static void loadModeFromConfig() {
        try {
            plugin.reloadConfig();  // 确保配置是最新的
            String modeName = plugin.getConfig().getString("current-mode", "null");
            
            if (modeName != null && !modeName.equals("null")) {
                try {
                    GameMode loadedMode = GameMode.valueOf(modeName);
                    
                    // 只有END模式才保留，其他模式都设为null
                    if (loadedMode == GameMode.END) {
                        currentMode = loadedMode;
                        plugin.getLogger().info("已从配置文件加载游戏模式: " + getModeName(currentMode));
                    } else {
                        // MANHUNT/TEAM模式清空，让玩家投票选择
                        currentMode = null;
                        plugin.getLogger().info("非末地模式已清空，等待投票选择");
                        
                        // 同时保存配置，确保下次启动时也是null
                        plugin.getConfig().set("current-mode", "null");
                        plugin.saveConfig();
                    }
                } catch (IllegalArgumentException e) {
                    currentMode = null;
                    plugin.getLogger().warning("配置文件中游戏模式无效: " + modeName);
                }
            } else {
                currentMode = null;
                plugin.getLogger().info("未找到保存的游戏模式，使用默认值 (null)");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("从配置文件加载模式时出错: " + e.getMessage());
            currentMode = null;
        }
    }

    /**
     * 获取当前模式名称（用于显示）
     */
    public static String getCurrentModeName() {
        return currentMode != null ? getModeName(currentMode) : "§7未开始";
    }

    public enum GameMode {
        MANHUNT, TEAM, END
    }
}