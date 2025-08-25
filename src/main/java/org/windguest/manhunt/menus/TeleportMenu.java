package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.jobs.JobsManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TeleportMenu {

    public static void open(Player player, int pageIndex) {
        Inventory teleportMenu = Bukkit.createInventory(null, 54, ("传送 - 第 " + (pageIndex + 1) + " 页"));
        ItemStack previousPage = new ItemStack(Material.ARROW);
        ItemMeta previousPageMeta = previousPage.getItemMeta();
        if (previousPageMeta != null) {
            previousPageMeta.setDisplayName("§e上一页");
            previousPage.setItemMeta(previousPageMeta);
        }
        teleportMenu.setItem(48, previousPage);
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c关闭");
            closeItem.setItemMeta(closeMeta);
        }
        teleportMenu.setItem(49, closeItem);
        ItemStack nextPage = new ItemStack(Material.ARROW);
        ItemMeta nextPageMeta = nextPage.getItemMeta();
        if (nextPageMeta != null) {
            nextPageMeta.setDisplayName("§e下一页");
            nextPage.setItemMeta(nextPageMeta);
        }
        teleportMenu.setItem(50, nextPage);
        ItemStack endPortalItem = new ItemStack(Material.END_PORTAL_FRAME);
        ItemMeta endPortalMeta = endPortalItem.getItemMeta();
        if (endPortalMeta != null) {
            endPortalMeta.setDisplayName("§a传送至末地");
            List<String> endPortalLore = List.of("§7仅在有队友进入过末地后开启");
            endPortalMeta.setLore(endPortalLore);
            endPortalItem.setItemMeta(endPortalMeta);
        }
        teleportMenu.setItem(45, endPortalItem);
        ItemStack rulesItem = new ItemStack(Material.CRAFTING_TABLE);
        ItemMeta rulesMeta = rulesItem.getItemMeta();
        if (rulesMeta != null) {
            rulesMeta.setDisplayName("§a游戏规则");
            rulesItem.setItemMeta(rulesMeta);
        }
        teleportMenu.setItem(53, rulesItem);
        Set<Player> allPlayersSet = TeamsManager.getAllGamingPlayers();
        allPlayersSet.remove(player);
        ArrayList<Player> allPlayers = new ArrayList<>(allPlayersSet);
        int totalPages = (int) Math.ceil((double) allPlayers.size() / 28.0);
        if (pageIndex < 0) {
            pageIndex = 0;
        }
        if (pageIndex >= totalPages) {
            pageIndex = totalPages - 1;
        }
        int startIndex = pageIndex * 28;
        int endIndex = Math.min(startIndex + 28, allPlayers.size());
        int[] slots = new int[] { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43 };
        for (int i = startIndex; i < endIndex; ++i) {
            Player p = allPlayers.get(i);
            Team playerTeam = TeamsManager.getPlayerTeam(player);
            Team pTeam = TeamsManager.getPlayerTeam(p);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null && pTeam != null) {
                meta.setDisplayName(pTeam.getColorString() + pTeam.getIcon() + " " + p.getName());
                meta.setOwningPlayer(p);
                ArrayList<String> lore = new ArrayList<>();
                lore.add("");
                boolean isGhost = org.windguest.manhunt.jobs.JobsManager.isGhost(p);
                if (!isGhost && player.getWorld().equals(p.getWorld())) {
                    int distance = (int) player.getLocation().distance(p.getLocation());
                    lore.add(pTeam.getColorString() + getWorldName(p.getWorld()) + " [" + p.getLocation().getBlockX()
                            + ", " + p.getLocation().getBlockY() + ", " + p.getLocation().getBlockZ() + "]");
                    lore.add(pTeam.getColorString() + "距离: " + distance + "格");
                } else {
                    if (isGhost) {
                        lore.add(pTeam.getColorString() + "位置: §o???");
                    } else {
                        lore.add(pTeam.getColorString() + getWorldName(p.getWorld()));
                        lore.add(pTeam.getColorString() + "不同世界");
                    }
                }

                boolean sameTeam = playerTeam != null && playerTeam.equals(pTeam);
                if (sameTeam) {
                    lore.add("");
                    lore.add("§e点击消耗 §f9.5 §c❤");
                    lore.add("§e传送到他的位置");
                } else {
                    lore.add("");
                    lore.add("§e点击消耗 §f9.5 §c❤");
                    lore.add("§e传送到他附近100格");
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
            }
            teleportMenu.setItem(slots[i - startIndex], skull);
        }
        player.setMetadata("teleport_page", new FixedMetadataValue(Main.getInstance(), pageIndex));
        player.openInventory(teleportMenu);
    }

    private static String getWorldName(World world) {
        switch (world.getEnvironment()) {
            case NORMAL:
                return "主世界";
            case NETHER:
                return "下界";
            case THE_END:
                return "末地";
            default:
                return "未知";
        }
    }
}
