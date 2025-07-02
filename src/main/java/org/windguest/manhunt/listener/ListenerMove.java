package org.windguest.manhunt.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ListenerMove implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getChestplate() != null && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 40, 0, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 9, true, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, true, false));
        }
        if (player.getWorld() == Bukkit.getWorld("hub") && player.getY() < 70.0) {
            World hub = Bukkit.getWorld("hub");
            Location hubLocation = new Location(hub, 0.5, 81.0, 0.5);
            player.teleport(hubLocation);
        }
        if (player.getGameMode() == GameMode.SPECTATOR) {
            checkAndTeleport(player);
        }
    }

    private void checkAndTeleport(Player player) {
        Location playerLocation = player.getLocation();
        boolean hasNearbyPlayer = false;
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player p : player.getWorld().getPlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) {
                double distance = playerLocation.distance(p.getLocation());
                if (distance <= 96) {
                    hasNearbyPlayer = true;
                    break;
                }
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = p;
                }
            }
        }
        if (!hasNearbyPlayer && nearestPlayer != null) {
            player.teleport(nearestPlayer.getLocation());
            player.sendMessage("§c你不能距离玩家太远！");
        }
    }
}
