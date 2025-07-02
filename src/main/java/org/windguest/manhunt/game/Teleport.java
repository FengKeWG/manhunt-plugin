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
import org.windguest.manhunt.world.WorldManager;

import java.util.Random;
import java.util.Set;

public class Teleport {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static final int SPAWN_DISTANCE = 150;

    public static void teleportPlayersToTeamBases() {
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
                {redX, redZ},
                {blueX, blueZ}
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
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getInventory();
            inventory.clear();
            if (blue.contains(player)) {
                updatePlayerConfig(player, "games");
                int y = world.getHighestBlockYAt(redX, redZ) - 3;
                Location teleportLocation = new Location(world, redX, y, redZ);
                player.teleport(teleportLocation);
            } else if (red.contains(player)) {
                updatePlayerConfig(player, "games");
                int y = world.getHighestBlockYAt(blueX, blueZ) - 3;
                Location teleportLocation = new Location(world, blueX, y, blueZ);
                player.teleport(teleportLocation);
            }
        }
    }

    public static void teleportToRandomTeamPlayer(Player player, Team team) {

        Set<Player> players;
        if (team != null) {
            players = team.getPlayers();
        } else {
            players = TeamsManager.getAllGamingPlayers();
        }
        players.remove(player);
        if (players.isEmpty()) {
            return;
        }
        players.stream()
                .skip(rand.nextInt(players.size()))
                .findFirst()
                .ifPresent(randomPlayer -> player.teleport(randomPlayer.getLocation()));
    }

    private void startTeleportCountdown(Player player, Player targetPlayer, int cost) {
        player.closeInventory();
        new BukkitRunnable() {
            int countdown = 5;

            public void run() {
                ChatColor color;
                if (countdown <= 0) {
                    teleportPlayer(player, targetPlayer, cost);
                    player.removeMetadata("teleporting", ManHuntTeam.this);
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
        }.runTaskTimer(this, 0L, 20L);
    }


    public void teleportPlayer(Player player, Player targetPlayer, int cost) {
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
            return;
        }
        Location targetLocation = targetPlayer.getLocation();
        World.Environment targetEnvironment = targetPlayer.getWorld().getEnvironment();
        String locationMessage = null;
        boolean sameTeam = blue.contains(player) && blue.contains(targetPlayer) || red.contains(player) && red.contains(targetPlayer);
        if (sameTeam) {
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 的位置";
        } else if (targetEnvironment == World.Environment.NORMAL) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), -100.0, 100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
        } else if (targetEnvironment == World.Environment.NETHER) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), targetPlayer.getLocation().getY(), 100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
        } else if (targetEnvironment == World.Environment.THE_END) {
            targetLocation = Utils.findRandomLocationNear(targetPlayer.getLocation(), 192.0, 100.0);
            locationMessage = "§a[✔] 你已传送到 " + targetPlayer.getName() + " 附近100格的随机位置";
            endDown(player);
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
            if (sameTeam) {
                Bukkit.broadcastMessage((getPlayerTeamIcon(player) + " " + player.getName() + " §7传送到了 " + getPlayerTeamIcon(targetPlayer) + " " + targetPlayer.getName() + " §7附近100格的随机位置"));
            } else {
                Bukkit.broadcastMessage((getPlayerTeamIcon(player) + " " + player.getName() + " §7传送到了 " + getPlayerTeamIcon(targetPlayer) + " " + targetPlayer.getName() + " §7的位置"));
            }
        } else {
            player.sendMessage("§c[❌] 传送失败，未知原因！");
        }
    }
}
