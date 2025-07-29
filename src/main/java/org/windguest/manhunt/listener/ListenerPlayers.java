package org.windguest.manhunt.listener;

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.utils.Utils;

public class ListenerPlayers implements Listener {

    @EventHandler
    public void onPlayerPickupExperience(PlayerPickupExperienceEvent event) {
        int originalExp = event.getExperienceOrb().getExperience();
        int multipliedExp = originalExp * 10;
        event.getExperienceOrb().setExperience(multipliedExp);
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        ItemStack pickedItem = event.getItem().getItemStack();
        if (Utils.isIllegalItem(pickedItem)) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.COMPASS) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName() && meta.getDisplayName().equals("§a选择阵营§7（左键或右键点击）") || meta.getDisplayName().equals("§a敌人指南针§7（左键或右键点击）") || meta.getDisplayName().equals("§a大厅指南针§7（左键或右键点击）")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player player;
        ItemStack chestplate;
        ProjectileSource shooter;
        if (event.getEntity() instanceof Arrow && (shooter = event.getEntity().getShooter()) instanceof Player && (chestplate = (player = (Player) shooter).getInventory().getChestplate()) != null && chestplate.getType() == Material.ELYTRA) {
            event.setCancelled(true);
            player.sendMessage("§c[❌] 你不能在穿着鞘翅时射箭！");
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        if (JobsManager.isGhost(player)) {
            event.message(null); // 取消聊天广播与toast
        }
    }
}
