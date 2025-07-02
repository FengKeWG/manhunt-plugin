package org.windguest.manhunt.listener;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;

public class ListenerPortal implements Listener {

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            Player player = event.getPlayer();
            World endWorld = event.getTo().getWorld();
            Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft("end/root"));
            if (advancement != null && !player.getAdvancementProgress(advancement).isDone()) {
                ItemStack elytra = new ItemStack(Material.ELYTRA);
                elytra.setDurability((short) (elytra.getType().getMaxDurability() - 120));
                player.getInventory().addItem(elytra);
                ItemStack fireworks = new ItemStack(Material.FIREWORK_ROCKET, 20);
                player.getInventory().addItem(fireworks);

                if (red.contains(player) && !enderPortalOpenedRed) {
                    Location portalLocation = event.getFrom();
                    String coordinates = String.format("X=%d Y=%d Z=%d",
                            portalLocation.getBlockX(),
                            portalLocation.getBlockY(),
                            portalLocation.getBlockZ());
                    String mainTitle = ChatColor.RED + "红队首次进入末地！";
                    String subTitle = ChatColor.RED + coordinates;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(mainTitle, subTitle, 10, 70, 20);
                    }
                    String chatMessage = ChatColor.RED + "[🏹] 红队首次进入末地！位置: " + coordinates;
                    Bukkit.broadcastMessage(chatMessage);
                }
                if (blue.contains(player) && !enderPortalOpenedBlue) {
                    Location portalLocation = event.getFrom();
                    String coordinates = String.format("X=%d Y=%d Z=%d",
                            portalLocation.getBlockX(),
                            portalLocation.getBlockY(),
                            portalLocation.getBlockZ());
                    String mainTitle = ChatColor.BLUE + "蓝队首次进入末地！";
                    String subTitle = ChatColor.BLUE + coordinates;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle(mainTitle, subTitle, 10, 70, 20);
                    }
                    String chatMessage = ChatColor.BLUE + "[⚔] 蓝队首次进入末地！位置: " + coordinates;
                    Bukkit.broadcastMessage(chatMessage);
                    for (Entity entity : endWorld.getEntities()) {
                        if (entity instanceof EnderDragon dragon) {
                            double newMaxHealth = 1000.0;
                            dragon.setMaxHealth(newMaxHealth);
                        }
                    }
                }
            }
            if (red.contains(player)) {
                enderPortalOpenedRed = true;
            } else if (blue.contains(player)) {
                enderPortalOpenedBlue = true;
            }
            double angle = Math.random() * 2.0 * Math.PI;
            double x = 1000.0 * Math.cos(angle);
            double z = 1000.0 * Math.sin(angle);
            double y = 192.0;
            Location targetLocation = new Location(endWorld, x, y, z);
            event.setTo(targetLocation);
            endDown(player);
        }
    }
}
