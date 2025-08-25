package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class PlaySelectionMenu {

    public static void open(Player player) {
        Inventory hunterMenu = Bukkit.createInventory(null, 27, "中途加入");
        ItemStack hunterItem = new ItemStack(Material.BOW);
        ItemMeta hunterMeta = hunterItem.getItemMeta();
        hunterMeta.setDisplayName("§a我想作为玩家中途加入游戏");
        hunterItem.setItemMeta(hunterMeta);
        ItemStack spectatorItem = new ItemStack(Material.ENDER_EYE);
        ItemMeta spectatorMeta = spectatorItem.getItemMeta();
        spectatorMeta.setDisplayName("§7我想作为旁观者观战");
        spectatorItem.setItemMeta(spectatorMeta);
        hunterMenu.setItem(11, hunterItem);
        hunterMenu.setItem(15, spectatorItem);
        player.openInventory(hunterMenu);
    }
}
