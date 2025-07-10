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
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class ListenerPortal implements Listener {

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getTo().getWorld().getEnvironment() == World.Environment.THE_END) {
            Player player = event.getPlayer();
            Team playerTeam = TeamsManager.getPlayerTeam(player);
            if (playerTeam == null)
                return;

            // 首次进入逻辑
            if (!playerTeam.isEndPortalOpened()) {
                playerTeam.setEndPortalOpened(true);

                // 给予物品
                ItemStack elytra = new ItemStack(Material.ELYTRA);
                Damageable elytraMeta = (Damageable) elytra.getItemMeta();
                elytraMeta.setDamage(elytra.getType().getMaxDurability() - 120);
                elytra.setItemMeta((ItemMeta) elytraMeta);
                player.getInventory().addItem(elytra);

                ItemStack fireworks = new ItemStack(Material.FIREWORK_ROCKET, 20);
                player.getInventory().addItem(fireworks);

                // 广播消息
                Location portalLocation = event.getFrom();
                String coordinates = String.format("X=%d Y=%d Z=%d",
                        portalLocation.getBlockX(),
                        portalLocation.getBlockY(),
                        portalLocation.getBlockZ());
                String mainTitle = playerTeam.getColorString() + playerTeam.getName() + "首次进入末地！";
                String subTitle = playerTeam.getColorString() + coordinates;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(mainTitle, subTitle, 10, 70, 20);
                }
                String chatMessage = playerTeam.getColorString() + "[" + playerTeam.getIcon() + "] "
                        + playerTeam.getName() + "首次进入末地！位置: " + coordinates;
                Bukkit.broadcastMessage(chatMessage);

                // 设置龙血（如果需要）
                for (Entity entity : event.getTo().getWorld().getEntities()) {
                    if (entity instanceof EnderDragon dragon) {
                        double newMaxHealth = 1000.0;
                        dragon.setMaxHealth(newMaxHealth);
                    }
                }
            }

            // 设置传送坐标
            World endWorld = event.getTo().getWorld();
            double angle = Math.random() * 2.0 * Math.PI;
            double x = 1000.0 * Math.cos(angle);
            double z = 1000.0 * Math.sin(angle);
            double y = 192.0;
            Location targetLocation = new Location(endWorld, x, y, z);
            event.setTo(targetLocation);

            // 给予缓降
            Utils.endDown(player);
        }
    }
}
