package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ManhuntJoinMenu {

    public static void open(Player player, String joinableTeam) {
        Inventory menu = Bukkit.createInventory(null, 27, "中途加入 - 猎人模式");
        ItemStack joinItem;
        ItemMeta joinMeta;
        if ("猎人".equals(joinableTeam)) {
            joinItem = new ItemStack(Material.IRON_SWORD);
            joinMeta = joinItem.getItemMeta();
            joinMeta.setDisplayName("§c加入猎人阵营");
            List<String> lore = new ArrayList<>();
            lore.add("§7当前猎人/逃生者比例较低");
            lore.add("§7你可以作为猎人加入游戏！");
            joinMeta.setLore(lore);
        } else {
            joinItem = new ItemStack(Material.TOTEM_OF_UNDYING);
            joinMeta = joinItem.getItemMeta();
            joinMeta.setDisplayName("§a加入逃生者阵营");
            List<String> lore = new ArrayList<>();
            lore.add("§7当前猎人/逃生者比例较高");
            lore.add("§7你可以作为逃生者加入游戏！");
            joinMeta.setLore(lore);
        }
        joinItem.setItemMeta(joinMeta);
        menu.setItem(11, joinItem);
        ItemStack spectatorItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta spectatorMeta = spectatorItem.getItemMeta();
        spectatorMeta.setDisplayName("§7成为旁观者");
        List<String> lore = new ArrayList<>();
        lore.add("§7你将以旁观者模式观战");
        spectatorMeta.setLore(lore);
        spectatorItem.setItemMeta(spectatorMeta);
        menu.setItem(15, spectatorItem);

        player.openInventory(menu);
    }
}