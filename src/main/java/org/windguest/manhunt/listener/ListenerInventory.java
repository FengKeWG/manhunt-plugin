package org.windguest.manhunt.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.jobs.Job;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.utils.Utils;

import java.util.MissingFormatWidthException;
import java.util.Random;

public class ListenerInventory implements Listener {

    private static final Main plugin = Main.getInstance();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack currentItem;
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        Material itemType = clickedItem.getType();
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals("游戏模式投票")) {
            if (itemType == Material.DRAGON_EGG) {
                Mode.setPreference(player, Mode.GameMode.MANHUNT);
                Bukkit.broadcastMessage("§c[🏹] " + player.getName() + " 希望成为猎杀者");
            } else if (itemType == Material.CHEST) {
                Mode.setPreference(player, Mode.GameMode.TEAM);
            } else if (itemType == Material.ENDER_EYE) {
                Mode.setPreference(player, Mode.GameMode.END);
            }
        }
        if (event.getView().getTitle().equals("选择职业")) {
            event.setCancelled(true);
            int clickedSlot = event.getRawSlot();
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                Job selectedJob = JobsManager.getJobFromSlot(clickedSlot);
                if (selectedJob != null) {
                    selectedJob.giveKit(player);
                    Bukkit.broadcastMessage(("§a[✔] " + player.getName() + " 选择了职业：" + selectedJob.getDisplayName()));
                    player.closeInventory();
                }
            }
            if (clickedItem.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                player.closeInventory();
            }
        } else if (event.getView().getTitle().startsWith("传送")) {
            event.setCancelled(true);
            Player targetPlayer;
            OfflinePlayer target;
            SkullMeta meta;
            if (clickedItem.getType() == Material.END_PORTAL_FRAME) {
                player.closeInventory();
                if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                    player.sendMessage("§c[❌] 你已经在末地，无法再次传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (!enderPortalOpenedBlue && blue.contains(player)) {
                    player.sendMessage("§c[❌] 还没有队友进入末地，你无法传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                if (!enderPortalOpenedRed && red.contains(player)) {
                    player.sendMessage("§c[❌] 还没有队友进入末地，你无法传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                double healthCost = 19.0;
                double absorptionHealth = player.getAbsorptionAmount();
                double remainingHealthCost = healthCost;
                if (absorptionHealth > 0.0) {
                    if (absorptionHealth >= remainingHealthCost) {
                        player.setAbsorptionAmount(absorptionHealth - remainingHealthCost);
                        remainingHealthCost = 0.0;
                    } else {
                        remainingHealthCost -= absorptionHealth;
                        player.setAbsorptionAmount(0.0);
                    }
                }
                if (player.getHealth() > remainingHealthCost) {
                    player.setHealth(player.getHealth() - remainingHealthCost);
                    double radius = 1000.0;
                    double angle = Math.random() * 2.0 * Math.PI;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    double y = 192.0;
                    player.teleport(new Location(Bukkit.getWorld("world_the_end"), x, y, z));
                    endDown(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    String playerIcon = getPlayerTeamIcon(player);
                    Bukkit.broadcastMessage((playerIcon + " " + player.getName() + " §7传送到了末地"));
                } else {
                    player.sendMessage("§c[❌] 你的血量不足以进行传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else if (clickedItem.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                player.closeInventory();
            } else if (clickedItem.getType() == Material.ARROW) {
                int currentPage = playerPageIndex.getOrDefault(player, 0);
                if (clickedItem.getItemMeta().getDisplayName().equals("§e上一页")) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                    openTeleportMenu(player, currentPage - 1);
                } else if (clickedItem.getItemMeta().getDisplayName().equals("§e下一页")) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                    openTeleportMenu(player, currentPage + 1);
                }
            } else if (clickedItem.getType() == Material.CRAFTING_TABLE) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                openRulesMenu(player);
            } else if (clickedItem.getType() == Material.PLAYER_HEAD && (meta = (SkullMeta) clickedItem.getItemMeta()) != null && (target = meta.getOwningPlayer()) != null && target.isOnline() && (targetPlayer = target.getPlayer()) != null) {
                if (targetPlayer.getWorld().getEnvironment() == World.Environment.THE_END) {
                    if (red.contains(player) && !enderPortalOpenedRed) {
                        player.sendMessage("§c[❌] 你的队伍还没有进入过末地，你不能传送到末地的敌人！");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                    if (blue.contains(player) && !enderPortalOpenedBlue) {
                        player.sendMessage("§c[❌] 你的队伍还没有进入过末地，你不能传送到末地的敌人！");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        return;
                    }
                }
                int cost = 19;
                double totalHealth = player.getHealth() + player.getAbsorptionAmount();
                if (totalHealth > (double) cost) {
                    player.setMetadata("teleporting", new FixedMetadataValue(this, true));
                    startTeleportCountdown(player, targetPlayer, cost);
                    player.closeInventory();
                } else {
                    player.sendMessage("§c[❌] 你的血量不足以进行传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            }
        } else if (event.getView().getTitle().startsWith("游戏规则")) {
            clickedItem = event.getCurrentItem();
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                player.closeInventory();
            }
        } else if (event.getView().getTitle().equals("中途加入")) {
            clickedItem = event.getCurrentItem();
            event.setCancelled(true);
            if (clickedItem != null) {
                if (clickedItem.getType() == Material.BOW) {
                    boolean joinRed = red.size() < blue.size();
                    if (red.size() == blue.size()) {
                        joinRed = new Random().nextBoolean();
                    }
                    if (joinRed) {
                        player.sendMessage("§c[🏹] 你加入了红队继续游戏！");
                        red.add(player);
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        teleportToRandomPlayer(player, "red");
                    } else {
                        player.sendMessage("§9[⚔] 你加入了蓝队继续游戏！");
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        blue.add(player);
                        teleportToRandomPlayer(player, "blue");
                    }
                    player.getInventory().remove(Material.COMPASS);
                    if (frozenStarted) {
                        player.setInvisible(true);
                        player.setInvulnerable(true);
                        player.setGameMode(GameMode.ADVENTURE);
                    } else {
                        player.setInvisible(false);
                        player.setInvulnerable(false);
                        player.setGameMode(GameMode.SURVIVAL);
                        giveGameCompass(player);
                    }
                    updatePlayerConfig(player, "games");
                    player.closeInventory();
                } else if (clickedItem.getType() == Material.ENDER_EYE) {
                    player.sendMessage("§7[🚫] 你选择了作为旁观者观战");
                    spectators.add(player);
                    player.setGameMode(GameMode.SPECTATOR);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                    teleportToRandomPlayer(player, "all");
                    player.closeInventory();
                }
            }
        }
        if ((event.getSlotType() == InventoryType.SlotType.CONTAINER || event.getSlotType() == InventoryType.SlotType.QUICKBAR) && (currentItem = event.getCurrentItem()) != null && currentItem.getType() == Material.COMPASS && currentItem.getItemMeta().getDisplayName().equals("§a大厅指南针§7（左键或右键点击）")) {
            event.setCancelled(true);
        }
        Inventory clickedInventory = event.getInventory();
        ItemStack currentItem2 = event.getCurrentItem();
        if ((clickedInventory.equals(redSharedChest) || clickedInventory.equals(blueSharedChest)) && (event.getAction() == InventoryAction.PLACE_ALL || event.getAction() == InventoryAction.PLACE_ONE || event.getAction() == InventoryAction.PLACE_SOME || event.getAction() == InventoryAction.SWAP_WITH_CURSOR) && currentItem2 != null && currentItem2.getType() == Material.COMPASS) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();
        if (closedInventory.equals(redSharedChest) || closedInventory.equals(blueSharedChest)) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.3f, 1.0f);
        }
        if (event.getView().getTitle().equals("中途加入") && !red.contains(player) && !blue.contains(player) && !spectators.contains(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(event.getInventory()), 5L);
        }
        if (event.getView().getTitle().equals("选择职业") && !player.hasMetadata("professionChosen")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> openProfessionSelector(player), 5L);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem.getType() == Material.COMPASS && draggedItem.getItemMeta().getDisplayName().equals("§a大厅指南针§7（左键或右键点击）")) {
            event.setCancelled(true);
        }
        if (draggedItem.getType() == Material.COMPASS && (event.getInventory().equals(redSharedChest) || event.getInventory().equals(blueSharedChest))) {
            event.setCancelled(true);
        }
        if (event.getView().getTitle().startsWith("游戏规则")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && Utils.isIllegalItem(event.getCurrentItem())) {
            event.setCancelled(true);
            event.getWhoClicked().getInventory().remove(event.getCurrentItem());
        }
    }
}
