package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.windguest.manhunt.game.Mode;

import java.util.ArrayList;
import java.util.Map;

public class ModesMenu {

    public static void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, "游戏模式投票");
        Map<Player, Mode.GameMode> preferences = Mode.getPreferences();
        ItemStack item1 = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta1 = item1.getItemMeta();
        meta1.setDisplayName("§a追杀模式");
        ArrayList<String> lore1 = new ArrayList<>();
        lore1.add("");
        for (Player p : preferences.keySet()) {
            if (preferences.get(p).equals(Mode.GameMode.MANHUNT)) {
                lore1.add("§7" + p.getName());
            }
        }
        meta1.setLore(lore1);
        item1.setItemMeta(meta1);
            ItemStack item2 = new ItemStack(Material.CHEST);
        ItemMeta meta2 = item2.getItemMeta();
        meta2.setDisplayName("§b团队模式");
        ArrayList<String> lore2 = new ArrayList<>();
        lore2.add("");
        for (Player p : preferences.keySet()) {
            if (preferences.get(p).equals(Mode.GameMode.TEAM)) {
                lore2.add("§7" + p.getName());
            }
        }
        meta2.setLore(lore2);
        item2.setItemMeta(meta2);
        ItemStack item3 = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta3 = item3.getItemMeta();
        meta3.setDisplayName("§d浑沌末地");
        ArrayList<String> lore3 = new ArrayList<>();
        lore3.add("");
        for (Player p : preferences.keySet()) {
            if (preferences.get(p).equals(Mode.GameMode.END)) {
                lore3.add("§7" + p.getName());
            }
        }
        meta3.setLore(lore3);
        item3.setItemMeta(meta3);
        menu.setItem(11, item1);
        menu.setItem(13, item2);
        menu.setItem(15, item3);
        player.openInventory(menu);
    }
}
