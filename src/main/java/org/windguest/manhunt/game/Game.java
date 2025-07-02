package org.windguest.manhunt.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.menus.JobsMenu;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.ChunkyManager;
import org.windguest.manhunt.utils.Utils;
import org.windguest.manhunt.world.StructureManager;
import org.windguest.manhunt.world.WorldManager;

import java.util.Random;

public class Game {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static GameState currentState = GameState.WAITING;
    private static long gameStartTime = -1L;
    private static int countdown = 120;
    private static Location endLocation;

    public static GameState getCurrentState() {
        return currentState;
    }

    public static void setCurrentState(GameState currentState) {
        Game.currentState = currentState;
    }

    public static void startWaitingCountdown() {
        currentState = GameState.COUNTDOWN_STARTED;
        countdown = 120;
        new BukkitRunnable() {

            public void run() {
                if (countdown <= 0) {
                    Bukkit.getWorld("world").setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
                    assignTeams();
                    Teleport.teleportPlayersToTeamBases();
                    StructureManager.startNearestStructureUpdater();
                    startFrozenCountdown();
                    cancel();
                    return;
                }
                if (Bukkit.getOnlinePlayers().size() < 2) {
                    currentState = GameState.WAITING;
                    countdown = 120;
                    cancel();
                    return;
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static void startFrozenCountdown() {
        currentState = GameState.FROZEN;
        gameStartTime = System.currentTimeMillis();
        countdown = 60;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!JobsManager.hasChosenJob(player)) {
                JobsMenu.open(player);
            }
        }
        new BukkitRunnable() {
            public void run() {
                if (countdown <= 0) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        World world = Bukkit.getWorld("world");
                        if (player.getWorld() != world && world != null) {
                            Location worldSpawn = world.getSpawnLocation();
                            player.teleport(worldSpawn);
                        }
                        ChunkyManager.runStopCommand();
                        currentState = GameState.RUNNING;
                        Compass.giveGameCompass(player);
                        player.sendTitle("§a游戏开始！", "§c击杀末影龙或者杀死对手！", 10, 70, 20);
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                        player.setGameMode(org.bukkit.GameMode.SURVIVAL);
                        player.setInvulnerable(false);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 4));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 1200, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1200, 0));
                        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
                    }
                    WorldManager.breakGlassCube();
                    new BukkitRunnable() {
                        public void run() {
                            if (currentState != GameState.RUNNING) {
                                cancel();
                                return;
                            }
                            Compass.updateCompass();
                        }
                    }.runTaskTimer(plugin, 0L, 20L);
                    cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    ChatColor color = countdown > 5 ? ChatColor.RED : (countdown > 2 ? ChatColor.YELLOW : ChatColor.GREEN);
                    player.sendTitle(color + String.valueOf(countdown), "观察四周！", 10, 20, 10);
                    if (countdown > 5) continue;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                }
                --countdown;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void endGame(Team wonTeam) {
        if (currentState == GameState.ENDED) {
            return;
        }
        currentState = GameState.ENDED;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(org.bukkit.GameMode.SPECTATOR);
            player.teleport(endLocation);
            wonTeam.sendWinMessage(player);
            Team team = TeamsManager.getPlayerTeam(player);
            if (team == null) {
                team = TeamsManager.getDeadTeam(player);
            }
            if (team == null) {
                continue;
            }
            if (team.equals(wonTeam)) {
                DataManager.updatePlayerData(player, "wins");
            }
        }
        Utils.spawnManyRandomFireworks(endLocation);
        Bukkit.getScheduler().runTask(plugin, () ->
        {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), Sound.MUSIC_DISC_CHIRP, 3.0f, 1.0f);
            }
        });
        Bukkit.getScheduler().runTaskLater(plugin, Bukkit::shutdown, 300L);
    }

    public static long getGameElapsedTime() {
        if (gameStartTime == -1L) {
            return 0L;
        }
        return (System.currentTimeMillis() - gameStartTime) / 1000L;
    }

    public enum GameState {
        WAITING,
        COUNTDOWN_STARTED,
        FROZEN,
        RUNNING,
        PAUSED,
        ENDED,
    }
}
