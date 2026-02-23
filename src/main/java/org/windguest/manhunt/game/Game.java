package org.windguest.manhunt.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.jobs.Job;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.DataPackManager;
import org.windguest.manhunt.utils.Utils;
import org.windguest.manhunt.world.ChunkyManager;
import org.windguest.manhunt.world.StructureManager;
import org.windguest.manhunt.world.WorldManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static GameState currentState = GameState.WAITING;
    private static long gameStartTime = -1L;
    private static int countdown = 60; 
    private static Location endLocation;
    private static BukkitRunnable waitingCountdownTask = null;
    private static BukkitRunnable frozenCountdownTask = null;
    private static boolean isWaitingStarted = false;

    public static Location getEndLocation() {
        return endLocation;
    }

    public static void setEndLocation(Location loc) {
        endLocation = loc;
    }

    public static GameState getCurrentState() {
        return currentState;
    }

    public static void setCurrentState(GameState newState) {
        // 清理旧任务
        if (currentState == GameState.COUNTDOWN_STARTED && waitingCountdownTask != null) {
            waitingCountdownTask.cancel();
            waitingCountdownTask = null;
            isWaitingStarted = false; // 重置标记
        }
        if (currentState == GameState.FROZEN && frozenCountdownTask != null) {
            frozenCountdownTask.cancel();
            frozenCountdownTask = null;
        }
        
        currentState = newState;
    }

    public static int getCountdown() {
        return countdown;
    }

    /**
     * 检查是否可以开始等待阶段
     */
    public static boolean canStartWaiting() {
        // 必须满足以下条件：
        // 1. 当前处于WAITING状态
        // 2. 有至少2名玩家在线
        // 3. 游戏模式已确定（不为null）
        return currentState == GameState.WAITING && 
               Bukkit.getOnlinePlayers().size() >= 2 && 
               Mode.getCurrentMode() != null;
    }

    /**
     * 开始游戏等待倒计时（60秒）
     */
    public static void startWaitingCountdown() {
        // 检查模式是否已确定
        Mode.GameMode currentMode = Mode.getCurrentMode();
        if (currentMode == null) {
            plugin.getLogger().warning("尝试开始游戏但模式未选择！");
            Bukkit.broadcastMessage("§c[❌] 游戏模式未选择，无法开始游戏！");
            Bukkit.broadcastMessage("§7请先投票选择游戏模式");
            return;
        }
        
        if (currentState != GameState.WAITING) {
            Bukkit.broadcastMessage("§c[!] 游戏已经在进行中，无法重新开始！");
            return; // 避免重复启动导致多个定时任务并行
        }
        
        // 检查玩家人数
        if (Bukkit.getOnlinePlayers().size() < 2) {
            plugin.getLogger().info("玩家人数不足2人，无法开始等待倒计时");
            Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，无法开始游戏！");
            return;
        }
        
        // 清理旧任务
        if (waitingCountdownTask != null) {
            waitingCountdownTask.cancel();
            waitingCountdownTask = null;
        }
        
        currentState = GameState.COUNTDOWN_STARTED;
        countdown = 60; // 60秒等待
        isWaitingStarted = true;
        
        // 广播开始信息
        String modeName = Mode.getCurrentModeName();
        Bukkit.broadcastMessage("§6══════════════════════════");
        Bukkit.broadcastMessage("§6游戏将在60秒后开始！");
        Bukkit.broadcastMessage("§6当前模式: " + modeName);
        Bukkit.broadcastMessage("§6══════════════════════════");
        
        waitingCountdownTask = new BukkitRunnable() {
            public void run() {
                // 检查玩家人数
                int playerCount = Bukkit.getOnlinePlayers().size();
                
                // 玩家人数不足，处理不同模式
                if (playerCount < 2) {
                    Mode.GameMode currentMode = Mode.getCurrentMode();
                    
                    if (currentMode == Mode.GameMode.END) {
                        // END模式：取消倒计时并重置状态
                        Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，混沌末地游戏取消！");
                        resetToWaiting();
                        this.cancel();
                        waitingCountdownTask = null;
                        return;
                    } else {
                        // 非END模式：仅暂停倒计时，不重置模式
                        if (countdown % 30 == 0 || countdown == 60) { // 每30秒或开始时提醒
                            Bukkit.broadcastMessage("§e[!] 玩家人数不足2人，等待更多玩家加入...");
                        }
                        // 不减倒计时，保持当前值
                        return;
                    }
                }
                
                // 倒计时结束
                if (countdown <= 0) {
                    // 尝试停止 Chunky 并替换为已预生成地图（若存在）
                    boolean replaced = ChunkyManager.prepareWorldForGame();
                    if (replaced) {
                        Bukkit.broadcastMessage("§e[Chunky] 已加载预生成地图，加速开始游戏！");
                    }

                    Bukkit.getWorld("world").setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
                    
                    // 先检查游戏模式
                    Mode.GameMode gameMode = Mode.getCurrentMode();
                    if (gameMode == null) {
                        plugin.getLogger().severe("游戏模式为null，无法分配队伍！");
                        Bukkit.broadcastMessage("§c[❌] 游戏模式未设置，无法开始游戏！");
                        resetToWaiting();
                        this.cancel();
                        waitingCountdownTask = null;
                        return;
                    }
                    
                    // 分配队伍
                    plugin.getLogger().info("开始分配队伍...");
                    TeamsManager.assignTeams();
                    
                    // 检查队伍分配情况
                    int playersWithTeams = 0;
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        Team team = TeamsManager.getPlayerTeam(player);
                        if (team != null) {
                            playersWithTeams++;
                            plugin.getLogger().info("玩家 " + player.getName() + " 被分配到队伍: " + team.getName());
                        } else {
                            plugin.getLogger().warning("玩家 " + player.getName() + " 未被分配到任何队伍！");
                        }
                    }
                    
                    if (playersWithTeams == 0) {
                        plugin.getLogger().severe("没有玩家被分配到队伍，无法开始游戏！");
                        Bukkit.broadcastMessage("§c[❌] 队伍分配失败，无法开始游戏！");
                        resetToWaiting();
                        this.cancel();
                        waitingCountdownTask = null;
                        return;
                    }
                    
                    // 传送玩家到队伍基地
                    plugin.getLogger().info("开始传送玩家到队伍基地...");
                    Teleport.teleportPlayersToTeamBases();
                    
                    StructureManager.startNearestStructureUpdater();
                    startFrozenCountdown();
                    this.cancel();
                    waitingCountdownTask = null;
                    isWaitingStarted = false;
                    return;
                }
                
                // 广播倒计时
                if (countdown == 60 || countdown == 30 || countdown == 15 || countdown == 10 || 
                    (countdown <= 5 && countdown > 0)) {
                    String message = "§6[!] 游戏将在 §e" + countdown + " §6秒后开始！";
                    Bukkit.broadcastMessage(message);
                    
                    // 播放提示音效
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.0f);
                    });
                }
                
                countdown--;
            }
        };
        
        waitingCountdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private static void startFrozenCountdown() {
        
        currentState = GameState.FROZEN;
        gameStartTime = System.currentTimeMillis();
        countdown = 60;
        
        // 清理旧任务
        if (frozenCountdownTask != null) {
            frozenCountdownTask.cancel();
        }
        
        // 确保所有玩家都有队伍
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = TeamsManager.getPlayerTeam(player);
            if (team == null) {
                plugin.getLogger().warning("玩家 " + player.getName() + " 在冻结阶段没有队伍，尝试分配");
                // 尝试分配到第一个可用队伍
                java.util.Set<Team> teams = TeamsManager.getTeams();
                if (!teams.isEmpty()) {
                    for (Team t : teams) {
                        t.addPlayer(player);
                        break;
                    }
                }
            }
        }
        
        // 给予所有玩家职业指南针
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 猎杀者不发职业指南针
            if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                Team pt = TeamsManager.getPlayerTeam(player);
                if (pt != null && "猎杀者".equals(pt.getName())) {
                    continue;
                }
            }
            Compass.giveJobCompass(player);
        }
        
        frozenCountdownTask = new BukkitRunnable() {
            public void run() {
                if (countdown <= 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (TeamsManager.isSpectator(player)) {
                            continue;
                        }
                        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                            String role = TeamsManager.getPlayerTeam(player).getName().equals("逃生者") ? "runner"
                                    : "hunter";
                            DataManager.incrementPlayerData(player, Mode.getCurrentMode(), role, "games", 1);
                        } else {
                            DataManager.incrementPlayerData(player, Mode.getCurrentMode(), "games", 1);
                        }
                        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                            Team pt = TeamsManager.getPlayerTeam(player);
                            if (pt != null && "猎杀者".equals(pt.getName())) {
                                // 猎杀者不能选职业
                                JobsManager.setChosenJob(player, null);
                            }
                        }

                        if (!JobsManager.hasChosenJob(player)) {
                            List<Job> availableJobs = new ArrayList<>(JobsManager.getJobs().values());
                            if (!availableJobs.isEmpty()) {
                                Job randomJob = availableJobs.get(rand.nextInt(availableJobs.size()));
                                JobsManager.setChosenJob(player, randomJob);
                                randomJob.giveKit(player);
                                player.sendMessage("§e[⚠] 系统为你随机选择了职业: " + randomJob.getDisplayName());
                            }
                        }
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                                    && item.getItemMeta().getPersistentDataContainer()
                                            .has(new NamespacedKey(plugin, "job_compass"))) {
                                player.getInventory().remove(item);
                            }
                        }

                        // 末地模式不传送到主世界
                        if (Mode.getCurrentMode() != Mode.GameMode.END) {
                            World world = Bukkit.getWorld("world");
                            if (player.getWorld() != world && world != null) {
                                Location worldSpawn = world.getSpawnLocation();
                                player.teleport(worldSpawn);
                            }
                        }
                    
                        ChunkyManager.runStopCommand();
                        Compass.giveGameCompass(player);
                        player.sendTitle("§a游戏开始！", "§c击杀末影龙或者杀死对手！", 10, 70, 20);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        player.setInvulnerable(false);
                    
                        // 给予所有模式相同的药水效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 1200, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1200, 0));
                    
                        // 如果是末地模式，额外给予防虚空效果
                        if (Mode.getCurrentMode() == Mode.GameMode.END) {
                            Utils.endDown(player); // 缓慢下降和防虚空漂浮
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 1)); // 额外缓慢下降
                        }
                    
                        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
                    }
                    
                    WorldManager.breakGlassCube();
                    currentState = GameState.RUNNING;
                    
                    // 启动指南针更新任务
                    new BukkitRunnable() {
                        public void run() {
                            if (currentState != GameState.RUNNING) {
                                cancel();
                                return;
                            }
                            Compass.updateCompass();
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                    
                    this.cancel();
                    frozenCountdownTask = null;
                    return;
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ChatColor color = countdown > 5 ? ChatColor.RED
                            : (countdown > 2 ? ChatColor.YELLOW : ChatColor.GREEN);
                    player.sendTitle(color + String.valueOf(countdown), "观察四周！", 10, 20, 10);
                    if (countdown > 5)
                        continue;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                }
                --countdown;
            }
        };
        
        frozenCountdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    public static void endGame(Team wonTeam) {
        if (currentState == GameState.ENDED) {
            return;
        }

        if (endLocation == null) {
            World world = Bukkit.getWorld("world");
            endLocation = world != null ? world.getSpawnLocation() : new Location(Bukkit.getWorlds().get(0), 0, 100, 0);
        }
        currentState = GameState.ENDED;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.teleport(endLocation);
            wonTeam.sendWinMessage(player);
            // Title
            String titleColor;
            switch (wonTeam.getName()) {
                case "红队":
                    titleColor = "§c🎈 ";
                    break;
                case "蓝队":
                    titleColor = "§9🎯 ";
                    break;
                case "逃生者":
                    titleColor = "§a🐉 ";
                    break;
                case "猎杀者":
                    titleColor = "§c🏹 ";
                    break;
                default:
                    titleColor = wonTeam.getColorString();
            }
            player.sendTitle("", titleColor + wonTeam.getName() + "获胜！", 10, 70, 20);
            Team team = TeamsManager.getPlayerTeam(player);
            if (team == null) {
                team = TeamsManager.getDeadTeam(player);
            }
            if (team == null) {
                continue;
            }
            if (team.equals(wonTeam)) {
                if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                    String role = team.getName().equals("逃生者") ? "runner" : "hunter";
                    DataManager.incrementPlayerData(player, Mode.getCurrentMode(), role, "wins", 1);
                } else {
                    DataManager.incrementPlayerData(player, Mode.getCurrentMode(), "wins", 1);
                }
            }
        }
        Utils.spawnManyRandomFireworks(endLocation);
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.MUSIC_DISC_CHIRP, 3.0f, 1.0f);
            }
        });
        
        // 如果是混沌末地模式，重置模式为null并切换到普通数据包
        if (Mode.getCurrentMode() == Mode.GameMode.END) {
            Bukkit.broadcastMessage("§6混沌末地游戏结束！");
            Bukkit.broadcastMessage("§6正在切换到普通数据包并准备重启服务器...");
            
            // 延迟5秒后执行
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    // 1. 切换到普通数据包
                    boolean switchSuccess = DataPackManager.switchToNormalMode();
                    
                    if (switchSuccess) {
                        // 2. 重置模式为null
                        Mode.setCurrentMode(null);
                        
                        // 3. 重启服务器
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            // 踢出所有玩家
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                player.kickPlayer("§e服务器正在重启以切换游戏模式...");
                            }
                            
                            // 延迟1秒后关闭服务器
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                Bukkit.shutdown();
                            }, 20L);
                        }, 100L); // 延迟5秒重启
                    } else {
                        Bukkit.broadcastMessage("§c[❌] 切换普通数据包失败！");
                        // 仍然重置模式，但提示手动重启
                        Mode.setCurrentMode(null);
                        Bukkit.broadcastMessage("§e请手动重启服务器以应用更改");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("切换数据包失败: " + e.getMessage());
                    Bukkit.broadcastMessage("§c[❌] 切换数据包时发生错误！");
                }
            }, 100L); // 5秒后开始切换
        } else {
            // 普通模式，延迟300秒后关闭服务器
            Bukkit.broadcastMessage("§6游戏结束！服务器将在5分钟后关闭...");
            Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 300L);
        }
    }

    public static long getGameElapsedTime() {
        if (gameStartTime == -1L) {
            return 0L;
        }
        return (System.currentTimeMillis() - gameStartTime) / 1000L;
    }

    /**
     * 重置游戏到等待状态
     */
    public static void resetToWaiting() {
        setCurrentState(GameState.WAITING);
        countdown = 60;
        gameStartTime = -1L;
        endLocation = null;
        isWaitingStarted = false;
        
        // 清理所有任务
        if (waitingCountdownTask != null) {
            waitingCountdownTask.cancel();
            waitingCountdownTask = null;
        }
        if (frozenCountdownTask != null) {
            frozenCountdownTask.cancel();
            frozenCountdownTask = null;
        }
        
        // 清理玩家状态
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
            player.getInventory().clear();
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.setSaturation(20);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        }
        
        Bukkit.broadcastMessage("§e[!] 游戏已重置到等待状态！");
    }
    
    /**
     * 检查是否已开始等待倒计时
     */
    public static boolean isWaitingStarted() {
        return isWaitingStarted;
    }

    public enum GameState {
        WAITING, COUNTDOWN_STARTED, FROZEN, RUNNING, PAUSED, ENDED
    }
}