package org.windguest.manhunt.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;

import java.util.Arrays;
import java.util.List;

public class Compass {

    public static void giveHubCompass(Player player) {
        ItemStack teamSelector = new ItemStack(Material.COMPASS);
        ItemMeta meta = teamSelector.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§a大厅指南针§7（左键或右键点击）");
            List<String> lore = Arrays.asList("§7按下§f[左键]§7打开游戏规则", "§7按下§f[右键]§7选择模式/队伍");
            meta.setLore(lore);
            teamSelector.setItemMeta(meta);
        }
        player.getInventory().setItem(0, teamSelector);
    }

    public static void giveGameCompass(Player player) {
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            String displayName = "§a敌人指南针§7（左键或右键点击）";
            meta.setDisplayName(displayName);
            List<String> lore = Arrays.asList("§7按下§f[左键]§7打开传送菜单", "§7按下§f[右键]§7打开共享背包");
            meta.setLore(lore);
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }

    public static void updateCompass() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Player nearestEnemy = findNearestEnemy(player);
            if (nearestEnemy != null) {
                int distance = (int) player.getLocation().distance(nearestEnemy.getLocation());
                String direction = getHorizontalDirectionToTarget(player, nearestEnemy);
                Team team = TeamsManager.getPlayerTeam(nearestEnemy);
                if (team == null) continue;
                player.sendActionBar("§9最近的敌人: " + direction + " §r" + team.getColorString() + nearestEnemy.getName() + "（" + distance + "格）");
                player.setCompassTarget(nearestEnemy.getLocation());
                continue;
            }
            player.sendActionBar("§a没有敌人和你在一个世界！");
        }
    }

    private static Player findNearestEnemy(Player player) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (player == p) continue;
            if (!TeamsManager.areSameTeam(player, p)) {
                double distance;
                if (!player.getWorld().equals(p.getWorld()) || !((distance = player.getLocation().distance(p.getLocation())) < nearestDistance))
                    continue;
                nearestDistance = distance;
                nearest = p;
            }
        }
        return nearest;
    }

    private static String getHorizontalDirectionToTarget(Player player, Player target) {
        Vector playerDirection = player.getLocation().getDirection().setY(0).normalize();
        Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
        double angle = Math.toDegrees(Math.atan2(toTarget.getZ(), toTarget.getX()) - Math.atan2(playerDirection.getZ(), playerDirection.getX()));
        if ((angle = (angle + 360.0) % 360.0) >= 337.5 || angle < 22.5) {
            return "↑";
        }
        if (angle >= 22.5 && angle < 67.5) {
            return "§l↗";
        }
        if (angle >= 67.5 && angle < 112.5) {
            return "→";
        }
        if (angle >= 112.5 && angle < 157.5) {
            return "§l↘";
        }
        if (angle >= 157.5 && angle < 202.5) {
            return "↓";
        }
        if (angle >= 202.5 && angle < 247.5) {
            return "§l↙";
        }
        if (angle >= 247.5 && angle < 292.5) {
            return "←";
        }
        if (angle >= 292.5) {
            return "§l↖";
        }
        return "？";
    }
}
