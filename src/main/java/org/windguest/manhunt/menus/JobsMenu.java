package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.jobs.Job;
import org.windguest.manhunt.jobs.JobsManager;

import java.util.Map;

public class JobsMenu {
    private static final Main plugin = Main.getInstance();

    public static void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 45, "选择职业");
        Map<Integer, Job> jobs = JobsManager.getJobs();
        for (Job job : jobs.values()) {
            ItemStack item = new ItemStack(job.getIconMaterial());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(job.getDisplayName());
                meta.setLore(job.getLore());
                item.setItemMeta(meta);
            }
            menu.setItem(job.getMenuSlot(), item);
        }
        player.openInventory(menu);
    }
}
