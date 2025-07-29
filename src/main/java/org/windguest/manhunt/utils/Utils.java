package org.windguest.manhunt.utils;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;

import java.util.Random;

public class Utils {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();

    public static Location findRandomLocationNear(Location location, double y, double radius) {
        World world = location.getWorld();
        double angle = rand.nextDouble() * 2.0 * Math.PI;
        double xOffset = Math.cos(angle) * radius;
        double zOffset = Math.sin(angle) * radius;
        Location newLocation = location.clone().add(xOffset, 0.0, zOffset);
        if (y == -100.0) {
            y = world.getHighestBlockYAt(newLocation) + 1.0;
        }
        newLocation.setY(y);
        return newLocation;
    }

    public static boolean isIllegalItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName().equals("Explosive Arrow");
        }
        return false;
    }

    public static boolean isAxe(Material material) {
        return switch (material) {
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> true;
            default -> false;
        };
    }

    public static void spawnFireworkAtPlayer(Player player, Color color) {
        Firework firework = player.getWorld().spawn(player.getLocation().add(0.0, 2.0, 0.0), Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffect(FireworkEffect.builder().withColor(color).with(FireworkEffect.Type.BALL).trail(false)
                .flicker(false).build());
        fireworkMeta.setPower(0);
        firework.setFireworkMeta(fireworkMeta);
        firework.setMetadata("noDamage", new FixedMetadataValue(plugin, true));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!firework.isDead()) {
                firework.detonate();
            }
        }, 1L);
    }

    public static void spawnManyRandomFireworks(Location location) {
        new BukkitRunnable() {
            public void run() {
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = 5 + Math.random() * 5;
                    double offsetX = radius * Math.cos(angle);
                    double offsetZ = radius * Math.sin(angle);
                    Location randomLocation = location.clone().add(offsetX, 0, offsetZ);
                    spawnFireworkAtLocation(randomLocation);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static void spawnFireworkAtLocation(Location location) {
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        FireworkEffect.Type type = FireworkEffect.Type.values()[rand.nextInt(FireworkEffect.Type.values().length)];
        Color color1 = Color.fromRGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        Color color2 = Color.fromRGB(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
        FireworkEffect effect = FireworkEffect.builder().with(type).withColor(color1).withFade(color2)
                .flicker(rand.nextBoolean()).trail(rand.nextBoolean()).build();
        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(1);
        firework.setFireworkMeta(fireworkMeta);
    }

    private boolean hasCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.COMPASS
                    || !item.getItemMeta().getDisplayName().equals("§a敌人指南针§7（左键或右键点击）"))
                continue;
            return true;
        }
        return false;
    }

    public static void endDown(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 2400, 0));
        BukkitRunnable task = new BukkitRunnable() {
            public void run() {
                if (player.getLocation().getY() < 32.0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 200, 4));
                }
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
        Bukkit.getScheduler().runTaskLater(plugin, task::cancel, 2400L);
    }
}
