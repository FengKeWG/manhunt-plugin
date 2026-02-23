package org.windguest.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.windguest.manhunt.commands.MainCommand;
import org.windguest.manhunt.commands.ShoutCommand;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.listener.*;
import org.windguest.manhunt.placeholder.Placeholder;
import org.windguest.manhunt.utils.DataPackManager;
import org.windguest.manhunt.utils.MessagesManager;
import org.windguest.manhunt.world.ChunkyManager;
import org.windguest.manhunt.world.StructureManager;
import org.windguest.manhunt.world.WorldManager;

import java.io.File;
import java.io.IOException;

public final class Main extends JavaPlugin {

    private static Main instance;
    private int endModeStartTaskId = -1;
    private boolean startupLogicHandled = false;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        startupLogicHandled = false; // 重置状态
        
        // 确保插件数据文件夹存在
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        
        // 保存默认配置文件（如果不存在）
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().info("创建默认配置文件...");
            try {
                configFile.getParentFile().mkdirs();
                configFile.createNewFile();
                
                java.io.FileWriter writer = new java.io.FileWriter(configFile);
                writer.write("# ManHunt 插件配置文件\n");
                writer.write("# 当前游戏模式 (MANHUNT, TEAM, END, 或 null)\n");
                writer.write("current-mode: \"null\"\n");
                writer.close();
                
                getLogger().info("默认配置文件创建成功");
            } catch (IOException e) {
                getLogger().severe("创建配置文件失败: " + e.getMessage());
            }
        }
        
        // 重新加载配置
        reloadConfig();
        
        // 确保数据包文件夹存在并初始化
        DataPackManager.ensureFoldersExist();
        DataPackManager.initializeDatapackFolders();
        
        // 加载保存的游戏模式
        Mode.loadModeFromConfig();
        
        // 检查数据包状态
        DataPackManager.checkDatapackStatus();
        
        // 只在启动时检查一次模式，不自动切换
        Mode.GameMode currentMode = Mode.getCurrentMode();
        getLogger().info("当前游戏模式: " + (currentMode != null ? Mode.getModeName(currentMode) : "null"));
        
        // 如果模式为END，检查数据包但不自动重启
        if (currentMode == Mode.GameMode.END) {
            boolean usingEndPacks = DataPackManager.isDatapacksUsingEnd();
            if (!usingEndPacks) {
                getLogger().warning("配置为混沌末地模式，但数据包可能不匹配！");
                getLogger().warning("建议手动使用命令切换: /mh choose end confirm");
                
                // 警告，不阻止后续加载，让玩家能进入大厅
                DataManager.createUsersFolder();
                JobsManager.initializeJobs();
                // 继续执行后面的世界加载等流程
            }
        }
        
        DataManager.createUsersFolder();
        JobsManager.initializeJobs();
        
        // 注册命令
        MainCommand mainCommand = new MainCommand();
        this.getCommand("manhunt").setExecutor(mainCommand);
        this.getCommand("manhunt").setTabCompleter(mainCommand);
        
        ShoutCommand shoutCommand = new ShoutCommand();
        this.getCommand("s").setExecutor(shoutCommand);

        // 尝试加载世界
        try {
            WorldManager worldManager = new WorldManager();
            worldManager.loadWorld();
            getLogger().info("世界加载成功");
        } catch (Exception e) {
            getLogger().severe("加载世界时出错: " + e.getMessage());
            getLogger().severe("这可能是由于数据包不匹配或配置错误导致的");
        }

        MessagesManager.startScheduledMessages();

        StructureManager.init();
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ListenerJoin(), this);
        pm.registerEvents(new ListenerQuit(), this);
        pm.registerEvents(new ListenerChat(), this);
        pm.registerEvents(new ListenerMove(), this);
        pm.registerEvents(new ListenerPlayers(), this);
        pm.registerEvents(new ListenerBlock(), this);
        pm.registerEvents(new ListenerDamage(), this);
        pm.registerEvents(new ListenerDeath(), this);
        pm.registerEvents(new ListenerInteract(), this);
        pm.registerEvents(new ListenerInventory(), this);
        pm.registerEvents(new ListenerPortal(), this);
        pm.registerEvents(new ListenerWorld(), this);
        
        // 初始化 ChunkyManager
        try {
            ChunkyManager.initialize();
        } catch (Exception e) {
            getLogger().warning("初始化 ChunkyManager 时出错: " + e.getMessage());
        }
        
        // 只在不是混沌末地模式时尝试获取生物群系位置
        if (Mode.getCurrentMode() != Mode.GameMode.END) {
            try {
                WorldManager.getNearestNonOceanBiomeLocation();
            } catch (Exception e) {
                getLogger().warning("获取生物群系位置时出错: " + e.getMessage());
            }
        }
        
        // 注册 PlaceholderAPI 扩展
        try {
            new Placeholder().register();
        } catch (Exception e) {
            getLogger().warning("注册 PlaceholderAPI 扩展时出错: " + e.getMessage());
        }
        
        getLogger().info("ManHunt 插件已启用！");
        getLogger().info("当前游戏模式: " + Mode.getCurrentModeName());
        
        // 延迟执行启动后的逻辑，确保插件完全加载
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!startupLogicHandled) {
                handleStartupLogic();
                startupLogicHandled = true;
            }
        }, 60L); // 延迟3秒，确保所有组件已加载
    }
    
    /**
     * 注册基本组件（数据包不匹配时使用）
     */
    private void registerBasicComponents() {
        MainCommand mainCommand = new MainCommand();
        this.getCommand("manhunt").setExecutor(mainCommand);
        this.getCommand("manhunt").setTabCompleter(mainCommand);
        
        ShoutCommand shoutCommand = new ShoutCommand();
        this.getCommand("s").setExecutor(shoutCommand);
        
        getLogger().info("ManHunt 插件已启用（基础模式）！");
        getLogger().warning("混沌末地数据包不匹配，请使用命令修复");
    }
    
    /**
     * 服务器启动逻辑
     */
    private void handleStartupLogic() {
        Mode.GameMode currentMode = Mode.getCurrentMode();
        
        if (currentMode == Mode.GameMode.END) {
            // END模式，直接进入等待阶段，不投票
            getLogger().info("检测到混沌末地模式，直接进入等待阶段");
            
            // 确保没有投票在进行
            if (Mode.isVoting()) {
                getLogger().warning("检测到异常投票状态，正在清理...");
                Mode.stopVoting();
            }
            
            Bukkit.broadcastMessage("§6══════════════════════════");
            Bukkit.broadcastMessage("§d[🌌] 混沌末地模式已激活！");
            Bukkit.broadcastMessage("§7等待玩家加入...");
            Bukkit.broadcastMessage("§6══════════════════════════");
            
            // 检查数据包匹配
            boolean usingEndPacks = DataPackManager.isDatapacksUsingEnd();
            if (!usingEndPacks) {
                getLogger().warning("混沌末地模式但数据包不匹配！");
                Bukkit.broadcastMessage("§c[⚠] 混沌末地模式数据包不匹配，请检查配置！");
                // 警告，继续执行等待阶段
            }
            
            // 检查是否有足够玩家，有则开始等待阶段
            if (Bukkit.getOnlinePlayers().size() >= 2) {
                startEndModeWaitingPhase();
            } else {
                getLogger().info("等待更多玩家加入混沌末地模式...");
                Bukkit.broadcastMessage("§e[!] 等待更多玩家加入混沌末地模式...");
            }
        } else if (currentMode == null) {
            // 模式为null，需要先投票
            getLogger().info("游戏模式未设置，等待玩家投票...");
            Bukkit.broadcastMessage("§6══════════════════════════");
            Bukkit.broadcastMessage("§6[!] 需要选择游戏模式！");
            Bukkit.broadcastMessage("§6使用 §a/mh vote §6可以投票选择模式");
            Bukkit.broadcastMessage("§6或管理员使用 §a/mh choose <模式> §6直接设置");
            Bukkit.broadcastMessage("§6══════════════════════════");
            
            // 检查是否有足够玩家开始投票
            if (Bukkit.getOnlinePlayers().size() >= 2) {
                startVotingPhase();
            } else {
                getLogger().info("等待更多玩家加入以开始投票...");
                Bukkit.broadcastMessage("§e[!] 等待更多玩家加入以开始投票...");
            }
        } else {
            // 模式为MANHUNT或TEAM，直接进入等待阶段
            getLogger().info("当前游戏模式: " + Mode.getCurrentModeName());
            Bukkit.broadcastMessage("§6当前游戏模式: " + Mode.getCurrentModeName());
            
            // 检查是否有足够玩家开始等待阶段
            if (Bukkit.getOnlinePlayers().size() >= 2) {
                startWaitingPhase();
            } else {
                getLogger().info("等待更多玩家加入以开始游戏...");
                Bukkit.broadcastMessage("§e[!] 等待更多玩家加入以开始游戏...");
            }
        }
    }

    /**
     * 开始END模式等待阶段（60秒）
     */
    private void startEndModeWaitingPhase() {
        getLogger().info("开始混沌末地模式等待阶段（60秒）...");
        Bukkit.broadcastMessage("§d[🌌] 混沌末地游戏将在60秒后开始！");
        
        // 清理之前的倒计时任务
        cancelEndModeCountdown();
        
        final int[] countdown = {60};
        
        endModeStartTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // 检查游戏状态
                if (Game.getCurrentState() != Game.GameState.WAITING || Mode.getCurrentMode() != Mode.GameMode.END) {
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 检查玩家人数
                int playerCount = Bukkit.getOnlinePlayers().size();
                if (playerCount < 2) {
                    Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，混沌末地等待取消！");
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 倒计时结束，开始游戏
                if (countdown[0] <= 0) {
                    Bukkit.broadcastMessage("§6混沌末地游戏即将开始！");
                    
                    // 播放音效
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    });
                    
                    // 开始游戏等待倒计时
                    Game.startWaitingCountdown();
                    
                    // 取消任务
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 广播倒计时
                if (countdown[0] == 60 || countdown[0] == 30 || countdown[0] == 15 || 
                    countdown[0] == 10 || countdown[0] == 5 || (countdown[0] <= 3 && countdown[0] > 0)) {
                    String message = "§d[🌌] 混沌末地游戏将在 §e" + countdown[0] + " §d秒后开始！";
                    Bukkit.broadcastMessage(message);
                    
                    // 播放提示音效
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                    });
                }
                
                countdown[0]--;
            }
        }, 0L, 20L);
    }

    /**
     * 开始投票阶段（60秒）
     */
    private void startVotingPhase() {
        getLogger().info("开始游戏模式投票阶段（60秒）...");
        Bukkit.broadcastMessage("§6[!] 开始60秒投票选择游戏模式！");
        
        // 启动投票
        Mode.startVoting();
    }

    /**
     * 开始非END模式的等待阶段（60秒）
     */
    private void startWaitingPhase() {
        getLogger().info("开始游戏等待阶段（60秒）...");
        
        // 清理之前的倒计时任务
        cancelEndModeCountdown();
        
        final int[] countdown = {60};
        
        endModeStartTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                // 检查游戏状态和模式
                if (Game.getCurrentState() != Game.GameState.WAITING || Mode.getCurrentMode() == null) {
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 检查玩家人数
                int playerCount = Bukkit.getOnlinePlayers().size();
                if (playerCount < 2) {
                    Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，游戏等待取消！");
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 倒计时结束，开始游戏
                if (countdown[0] <= 0) {
                    Bukkit.broadcastMessage("§6游戏即将开始！");
                    
                    // 播放音效
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
                    });
                    
                    // 开始游戏等待倒计时
                    Game.startWaitingCountdown();
                    
                    // 取消任务
                    Bukkit.getScheduler().cancelTask(endModeStartTaskId);
                    endModeStartTaskId = -1;
                    return;
                }
                
                // 广播倒计时
                if (countdown[0] == 60 || countdown[0] == 30 || countdown[0] == 15 || 
                    countdown[0] == 10 || countdown[0] == 5 || (countdown[0] <= 3 && countdown[0] > 0)) {
                    String message = "§6游戏将在 §e" + countdown[0] + " §6秒后开始！";
                    Bukkit.broadcastMessage(message);
                    
                    // 播放提示音效
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                    });
                }
                
                countdown[0]--;
            }
        }, 0L, 20L);
    }
    
    @Override
    public void onDisable() {
        // 取消所有正在运行的任务
        if (endModeStartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(endModeStartTaskId);
        }
        
        // 重置启动逻辑状态
        startupLogicHandled = false;
        
        // 保存当前模式到配置文件
        if (Mode.getCurrentMode() != null) {
            getLogger().info("正在保存当前游戏模式...");
            try {
                getLogger().info("游戏模式已保存: " + Mode.getCurrentModeName());
            } catch (Exception e) {
                getLogger().severe("保存游戏模式时出错: " + e.getMessage());
            }
        }
        getLogger().info("ManHunt 插件已禁用！");
    }
    
    /**
     * 安全地重新加载配置文件
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        // 确保配置中有必要的默认值
        if (!getConfig().contains("current-mode")) {
            getConfig().set("current-mode", "null");
            saveConfig();
        }
    }
    
    /**
     * 获取混沌末地模式倒计时任务ID
     */
    public int getEndModeStartTaskId() {
        return endModeStartTaskId;
    }
    
    /**
     * 取消混沌末地模式倒计时
     */
    public void cancelEndModeCountdown() {
        if (endModeStartTaskId != -1) {
            Bukkit.getScheduler().cancelTask(endModeStartTaskId);
            endModeStartTaskId = -1;
            getLogger().info("已取消混沌末地模式自动开始倒计时");
        }
    }
}