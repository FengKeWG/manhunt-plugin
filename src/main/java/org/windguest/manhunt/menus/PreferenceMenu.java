package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.teams.TeamsManager;

import java.util.ArrayList;
import java.util.Map;

/**
 * 根据当前模式，为玩家展示队伍倾向选择界面。
 */
public class PreferenceMenu {

    public static void open(Player player) {
        if (Mode.getCurrentMode() == null)
            return;
        Inventory menu = Bukkit.createInventory(null, 27, "队伍倾向选择");
        Map<Player, TeamsManager.TeamPreference> prefMap = TeamsManager.getTeamPreferences();

        switch (Mode.getCurrentMode()) {
            case TEAM:
                // 红队
                ItemStack redItem = new ItemStack(Material.RED_WOOL);
                ItemMeta redMeta = redItem.getItemMeta();
                redMeta.setDisplayName("§c红队");
                ArrayList<String> redLore = new ArrayList<>();
                redLore.add("");
                prefMap.forEach((p, pref) -> {
                    if (pref == TeamsManager.TeamPreference.RED) {
                        redLore.add("§7" + p.getName());
                    }
                });
                redMeta.setLore(redLore);
                redItem.setItemMeta(redMeta);

                // 蓝队
                ItemStack blueItem = new ItemStack(Material.BLUE_WOOL);
                ItemMeta blueMeta = blueItem.getItemMeta();
                blueMeta.setDisplayName("§9蓝队");
                ArrayList<String> blueLore = new ArrayList<>();
                blueLore.add("");
                prefMap.forEach((p, pref) -> {
                    if (pref == TeamsManager.TeamPreference.BLUE) {
                        blueLore.add("§7" + p.getName());
                    }
                });
                blueMeta.setLore(blueLore);
                blueItem.setItemMeta(blueMeta);

                menu.setItem(11, redItem);
                menu.setItem(15, blueItem);
                break;
            case MANHUNT:
                // 逃生者
                ItemStack runnerItem = new ItemStack(Material.TOTEM_OF_UNDYING);
                ItemMeta rMeta = runnerItem.getItemMeta();
                rMeta.setDisplayName("§a逃生者");
                ArrayList<String> rLore = new ArrayList<>();
                rLore.add("");
                prefMap.forEach((p, pref) -> {
                    if (pref == TeamsManager.TeamPreference.RUNNER) {
                        rLore.add("§7" + p.getName());
                    }
                });
                rMeta.setLore(rLore);
                runnerItem.setItemMeta(rMeta);

                // 猎杀者
                ItemStack hunterItem = new ItemStack(Material.IRON_SWORD);
                ItemMeta hMeta = hunterItem.getItemMeta();
                hMeta.setDisplayName("§c猎杀者");
                ArrayList<String> hLore = new ArrayList<>();
                hLore.add("");
                prefMap.forEach((p, pref) -> {
                    if (pref == TeamsManager.TeamPreference.HUNTER) {
                        hLore.add("§7" + p.getName());
                    }
                });
                hMeta.setLore(hLore);
                hunterItem.setItemMeta(hMeta);

                menu.setItem(11, runnerItem);
                menu.setItem(15, hunterItem);
                break;
            default:
                break;
        }
        player.openInventory(menu);
    }
}