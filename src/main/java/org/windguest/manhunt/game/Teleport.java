package org.windguest.manhunt.game;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;
import org.windguest.manhunt.world.EndLocationManager;
import org.windguest.manhunt.world.WorldManager;

import java.util.Random;
import java.util.Set;

public class Teleport {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static final int SPAWN_DISTANCE = 150;

    public static void teleportPlayersToTeamBases() {
        if (Mode.getCurrentMode() == Mode.GameMode.END) {
            // 1. 开启所有队伍的末地门
            for (Team team : TeamsManager.getTeams()) {
                team.setEndPortalOpened(true);
            }

            // 2. 获取预先生成的基地位置
            Location redBase = EndLocationManager.getRedEndBase();
            Location blueBase = EndLocationManager.getBlueEndBase();
            if (redBase == null || blueBase == null) {
                plugin.getLogger().severe("末地基地位置缺失，回退到旧传送逻辑！");
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.getInventory().clear();
                    Team t = TeamsManager.getPlayerTeam(player);
                    if (t == null) continue;
                    Utils.teleportToEnd(player);
                }
                return;
            }

            // 3. 创建玻璃
            World endWorld = redBase.getWorld();
            if (endWorld != null) {
                int cubeHeight = 5;
                int cubeSize = 5;
                double centerY = redBase.getY() + (cubeHeight - 1) / 2.0; // 240 + 2 = 242

                Location redCenter = redBase.clone();
                redCenter.setY(centerY);
                Location blueCenter = blueBase.clone();
                blueCenter.setY(centerY);

                WorldManager.createHollowGlassCube(redCenter, cubeSize, cubeHeight);
                WorldManager.createHollowGlassCube(blueCenter, cubeSize, cubeHeight);
            }

            // 4. 传送玩家并立即给予漂浮效果
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().clear();
                Team playerTeam = TeamsManager.getPlayerTeam(player);
                if (playerTeam == null) continue;

                if ("红队".equals(playerTeam.getName())) {
                    player.teleport(redBase);
                } else if ("蓝队".equals(playerTeam.getName())) {
                    player.teleport(blueBase);
                } else {
                    Utils.teleportToEnd(player);
                }
                Utils.endDown(player); // 防虚空漂浮
            }
            return;
        }

        World world = Bukkit.getWorld("world");
        Location location = WorldManager.getSpawnLocation();
        int redX = location.getBlockX() + SPAWN_DISTANCE / 2;
        int redZ = location.getBlockZ() + SPAWN_DISTANCE / 2;
        int blueX = location.getBlockX() - SPAWN_DISTANCE / 2;
        int blueZ = location.getBlockZ() - SPAWN_DISTANCE / 2;
        if (world == null) {
            return;
        }
        int[][] coordinates = {
                { redX, redZ },
                { blueX, blueZ }
        };
        int cubeHeight = 5;
        int cubeSize = 5;
        for (int[] coordinate : coordinates) {
            int x = coordinate[0];
            int z = coordinate[1];
            int y = world.getHighestBlockYAt(x, z) + 10;
            Location center = new Location(world, x, y, z);
            WorldManager.createHollowGlassCube(center, cubeSize, cubeHeight);
        }

        Team team1 = TeamsManager.getTeams().stream().findFirst().orElse(null);
        Team team2 = team1 != null ? team1.getOpponent() : null;

        if (team1 == null || team2 == null) {
            System.out.println("Error: Teams not initialized correctly.");
            return;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getInventory();
            inventory.clear();
            Team playerTeam = TeamsManager.getPlayerTeam(player);
            if (playerTeam == null)
                continue;

            if (playerTeam.equals(team1)) {
                int y = world.getHighestBlockYAt(coordinates[0][0], coordinates[0][1]) - 3;
                Location teleportLocation = new Location(world, coordinates[0][0], y, coordinates[0][1]);
                player.teleport(teleportLocation);
            } else if (playerTeam.equals(team2)) {
                int y = world.getHighestBlockYAt(coordinates[1][0], coordinates[1][1]) - 3;
                Location teleportLocation = new Location(world, coordinates[1][0], y, coordinates[1][1]);
                player.teleport(teleportLocation);
            }
        }
    }

    public static void teleportToRandomTeamPlayer(Player player, Team team) {
        Set<Player> src;
        if (team != null) {
            src = team.getPlayers();
        } else {
            src = TeamsManager.getAllGamingPlayers();
        }
        Set<Player> candidates = new java.util.HashSet<>(src);
        candidates.remove(player);
        if (candidates.isEmpty()) {
            return;
        }
        candidates.stream()
                .skip(rand.nextInt(candidates.size()))
                .findFirst()
                .ifPresent(randomPlayer -> player.teleport(randomPlayer.getLocation()));
    }

    public static void startTeleportCountdown(Player player, Player targetPlayer, int cost) {
        if (org.windguest.manhunt.game.Game.getGameElapsedTime() < 300) {
            player.sendMessage("§c[❌] 游戏开始 5 分钟内无法传送！");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        player.setMetadata("teleporting", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        player.closeInventory();
        new BukkitRunnable() {
            int countdown = 5;

            public void run() {
                ChatColor color;
                if (countdown <= 0) {
                    teleportPlayer(player, targetPlayer, cost);
                    player.removeMetadata("teleporting", plugin);
                    cancel();
                    return;
                }
                switch (countdown) {
                    case 3:
                    case 4:
                    case 5: {
                        color = ChatColor.RED;
                        break;
                    }
                    case 2: {
                        color = ChatColor.YELLOW;
                        break;
                    }
                    default: {
                        color = ChatColor.GREEN;
                    }
                }
                player.sendTitle("", color + String.valueOf(countdown), 0, 20, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, 1.0f);
                targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
                --countdown;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void teleportPlayer(Player player, Player targetPlayer, int cost) {
        Team playerTeam = TeamsManager.getPlayerTeam(player);
        Team targetTeam = TeamsManager.getPlayerTeam(targetPlayer);
        double regularHealth = player.getHealth();
        double absorptionHealth = player.getAbsorptionAmount();
        double currentHealth = absorptionHealth + (regularHealth);
        if (currentHealth > (double) cost) {
            if (absorptionHealth >= (double) cost) {
                player.setAbsorptionAmount(absorptionHealth - (double) cost);
            } else {
                player.setAbsorptionAmount(0.0);
                player.setHealth(regularHealth - ((double) cost - absorptionHealth));
            }
        } else {
            player.sendMessage("§c[❌] 你的血量不足以进行传送！");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.removeMetadata("teleporting", plugin);
            return;
        }
        if (targetPlayer.getWorld().getEnvironment() == World.Environment.THE_END) {
            if (!playerTeam.isEndPortalOpened()) {
                player.sendMessage("§c[❌] 你的队伍还没有进入过末地，你不能传送到末地的敌人！");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                player.removeMetadata("teleporting", plugin);
                return;
            }
        }
        Location targetLocation = targetPlayer.getLocation();
        World.Environment targetEnvironment = targetPlayer.getWorld().getEnvironment();
        String locationMessage = null;
        boolean sameTeam = TeamsManager.areSameTeam(player, targetPlayer);
        if (sameTeam) {
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 的位置";
        } else if (targetEnvironment == World.Environment.NORMAL) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), -100.0, 100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
        } else if (targetEnvironment == World.Environment.NETHER) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), targetPlayer.getLocation().getY(),
                    100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
        } else if (targetEnvironment == World.Environment.THE_END) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), 192.0, 100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
            Utils.endDown(player);
        }
        player.teleport(targetLocation);
        if (locationMessage != null) {
            player.sendMessage(locationMessage);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            targetPlayer.playSound(targetPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 300, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 300, 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 2));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 200, 10));
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 900, 0));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 900, 0));
            if (playerTeam == null || targetTeam == null)
                return;

            if (sameTeam) {
                Bukkit.broadcastMessage(
                        (playerTeam.getColorString() + playerTeam.getIcon() + " " + player.getName() + " §7传送到了 "
                                + targetTeam.getColorString() + targetTeam.getIcon() + " " + targetPlayer.getName()
                                + " §7的位置"));
            } else {
                Bukkit.broadcastMessage(
                        (playerTeam.getColorString() + playerTeam.getIcon() + " " + player.getName() + " §7传送到了 "
                                + targetTeam.getColorString() + targetTeam.getIcon() + " " + targetPlayer.getName()
                                + " §7附近100格的随机位置"));
            }
        } else {
            player.sendMessage("§c[❌] 传送失败，棍母原因！");
            player.removeMetadata("teleporting", plugin);
        }
    }
}