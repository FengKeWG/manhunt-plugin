package org.windguest.manhunt.listener;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Compass;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.game.Teleport;
import org.windguest.manhunt.jobs.Job;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.menus.RulesMenu;
import org.windguest.manhunt.menus.TeleportMenu;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;

import java.util.HashSet;
import java.util.Set;

public class ListenerInventory implements Listener {

    private static final Main plugin = Main.getInstance();
    private static final Set<Player> spectators = new HashSet<>();

    public static Set<Player> getSpectators() {
        return spectators;
    }

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
                Bukkit.broadcastMessage("§a[✔] " + player.getName() + " 投给了 §a追杀模式");
            } else if (itemType == Material.CHEST) {
                Mode.setPreference(player, Mode.GameMode.TEAM);
                Bukkit.broadcastMessage("§a[✔] " + player.getName() + " 投给了 §b团队模式");
            } else if (itemType == Material.ENDER_EYE) {
                // Mode.setPreference(player, Mode.GameMode.END);
                player.sendMessage("§c[❌] 暂未开放！");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            player.closeInventory();
            event.setCancelled(true);
        }
        if (event.getView().getTitle().equals("选择职业")) {
            event.setCancelled(true);
            int clickedSlot = event.getRawSlot();
            if (event.getClickedInventory() != null
                    && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                Job selectedJob = JobsManager.getJobFromSlot(clickedSlot);
                if (selectedJob != null) {
                    JobsManager.setChosenJob(player, selectedJob);
                    selectedJob.giveKit(player);
                    Team pTeam = TeamsManager.getPlayerTeam(player);
                    Bukkit.broadcastMessage((pTeam.getColorString() + "[" + pTeam.getIcon() + "] " + player.getName()
                            + " 选择了职业：" + selectedJob.getDisplayName()));

                    // Remove job compass
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.COMPASS && item.hasItemMeta()
                                && item.getItemMeta().getPersistentDataContainer()
                                        .has(new NamespacedKey(plugin, "job_compass"))) {
                            player.getInventory().remove(item);
                            break;
                        }
                    }
                    player.closeInventory();
                }
            }
            if (clickedItem.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                player.closeInventory();
            }
        } else if (event.getView().getTitle().startsWith("传送")) {
            event.setCancelled(true);
            Team playerTeam = TeamsManager.getPlayerTeam(player);
            if (playerTeam == null)
                return;

            if (clickedItem.getType() == Material.END_PORTAL_FRAME) {
                if (!playerTeam.isEndPortalOpened()) {
                    player.sendMessage("§c[❌] 还没有队友进入末地，你无法传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
                player.closeInventory();
                if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                    player.sendMessage("§c[❌] 你已经在末地，无法再次传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                // Health check logic should be consistent, maybe move to Teleport class
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
                    double angle = Math.random() * 2.0 * Math.PI;
                    double x = 1000.0 * Math.cos(angle);
                    double z = 1000.0 * Math.sin(angle);
                    Location targetLocation = new Location(Bukkit.getWorld("world_the_end"), x, 192.0, z);
                    player.teleport(targetLocation);
                    Utils.endDown(player);
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    Bukkit.broadcastMessage((playerTeam.getIcon() + " " + player.getName() + " §7传送到了末地"));
                } else {
                    player.sendMessage("§c[❌] 你的血量不足以进行传送！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                }
            } else if (clickedItem.getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
                OfflinePlayer target = meta.getOwningPlayer();
                if (target != null && target.isOnline()) {
                    Player targetPlayer = target.getPlayer();
                    int cost = 19;
                    Teleport.startTeleportCountdown(player, targetPlayer, cost);
                    player.closeInventory();
                }
            }
        } else if (clickedItem.getType() == Material.BARRIER) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            player.closeInventory();
        } else if (clickedItem.getType() == Material.ARROW) {
            int currentPage = player.hasMetadata("teleport_page") ? player.getMetadata("teleport_page").get(0).asInt()
                    : 0;
            if (clickedItem.getItemMeta().getDisplayName().equals("§e上一页")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                TeleportMenu.open(player, currentPage - 1);
            } else if (clickedItem.getItemMeta().getDisplayName().equals("§e下一页")) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                TeleportMenu.open(player, currentPage + 1);
            }
        }
        // 移除默认工作台点击触发，改为仅在传送菜单中处理
        else if (event.getView().getTitle().startsWith("游戏规则")) {
            clickedItem = event.getCurrentItem();
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType() == Material.BARRIER) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                player.closeInventory();
            }
        } else if (event.getView().getTitle().equals("中途加入 - 猎人模式")) {
            event.setCancelled(true);
            Team runnerTeam = TeamsManager.getTeamByName("逃生者");
            Team hunterTeam = TeamsManager.getTeamByName("猎杀者");
            if (runnerTeam == null || hunterTeam == null)
                return;

            if (itemType == Material.TOTEM_OF_UNDYING) { // Join Runner
                player.sendMessage("§a[✔] 你加入了逃生者阵营！");
                runnerTeam.addPlayer(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2, 60));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4, 120));
            } else if (itemType == Material.IRON_SWORD) { // Join Hunter
                player.sendMessage("§c[🏹] 你加入了猎人阵营！");
                hunterTeam.addPlayer(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2, 600));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4, 1200));
            } else if (itemType == Material.ENDER_EYE) { // Spectator
                player.sendMessage("§7[🚫] 你选择了作为旁观者观战");
                spectators.add(player);
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                Teleport.teleportToRandomTeamPlayer(player, null);
                player.closeInventory();
                return; // Return early for spectators
            }

            // Common logic for players joining a team
            player.getInventory().remove(Material.COMPASS);
            if (Game.getCurrentState() == Game.GameState.FROZEN) {
                player.setInvisible(true);
                player.setInvulnerable(true);
                player.setGameMode(GameMode.ADVENTURE);
            } else {
                player.setInvisible(false);
                player.setInvulnerable(false);
                player.setGameMode(GameMode.SURVIVAL);
                Compass.giveGameCompass(player);

                // 立即传送到随机队友附近，增强游戏体验
                Team joinTeam = TeamsManager.getPlayerTeam(player);
                if (joinTeam != null) {
                    Teleport.teleportToRandomTeamPlayer(player, joinTeam);
                }
            }
            // PAPI.updatePlayerConfig(player, "games"); // Temporarily comment out
            player.closeInventory();
            return;
        } else if (event.getView().getTitle().equals("中途加入")) { // Old menu, now for TEAM mode
            event.setCancelled(true);
            if (clickedItem.getType() == Material.BOW) { // Join Game
                Team red = TeamsManager.getTeamByName("红队");
                Team blue = TeamsManager.getTeamByName("蓝队");
                if (red == null || blue == null)
                    return;

                boolean joinRed = red.getPlayerCount() <= blue.getPlayerCount();
                if (joinRed) {
                    player.sendMessage("§c[🎈] 你加入了红队继续游戏！");
                    red.addPlayer(player);
                } else {
                    player.sendMessage("§9[🎯] 你加入了蓝队继续游戏！");
                    blue.addPlayer(player);
                }
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2, 600));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4, 1200));
                player.getInventory().remove(Material.COMPASS);
                if (Game.getCurrentState() == Game.GameState.FROZEN) {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    player.setGameMode(GameMode.ADVENTURE);
                } else {
                    player.setInvisible(false);
                    player.setInvulnerable(false);
                    player.setGameMode(GameMode.SURVIVAL);
                    Compass.giveGameCompass(player);
                }
                // PAPI.updatePlayerConfig(player, "games"); // Temporarily comment out
                player.closeInventory();
            } else if (clickedItem.getType() == Material.ENDER_EYE) {
                player.sendMessage("§7[🚫] 你选择了作为旁观者观战");
                spectators.add(player);
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                Teleport.teleportToRandomTeamPlayer(player, null);
                player.closeInventory();
            }
            return;
        } else if (event.getView().getTitle().equals("队伍倾向选择")) {
            Material type = itemType;
            if (type == Material.RED_WOOL) {
                TeamsManager.setTeamPreference(player, TeamsManager.TeamPreference.RED);
                Bukkit.broadcastMessage("§c[🎈] " + player.getName() + " 倾向加入红队");
            } else if (type == Material.BLUE_WOOL) {
                TeamsManager.setTeamPreference(player, TeamsManager.TeamPreference.BLUE);
                Bukkit.broadcastMessage("§9[🎯] " + player.getName() + " 倾向加入蓝队");
            } else if (type == Material.TOTEM_OF_UNDYING) {
                TeamsManager.setTeamPreference(player, TeamsManager.TeamPreference.RUNNER);
                Bukkit.broadcastMessage("§a[🐉] " + player.getName() + " 倾向成为逃生者");
            } else if (type == Material.IRON_SWORD) {
                TeamsManager.setTeamPreference(player, TeamsManager.TeamPreference.HUNTER);
                Bukkit.broadcastMessage("§c[🏹] " + player.getName() + " 倾向成为猎杀者");
            }
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
            player.closeInventory();
            event.setCancelled(true);
            return;
        }

        // 允许在背包格子间移动特殊指南针，但禁止把它丢出背包（Q/拖到空白处）或放进共享背包
        if (clickedItem != null && clickedItem.getType() == Material.COMPASS && clickedItem.hasItemMeta()) {
            boolean isSpecial = clickedItem.getItemMeta().getPersistentDataContainer()
                    .has(new NamespacedKey(plugin, "hub_compass"))
                    || clickedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "job_compass"))
                    || clickedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "game_compass"));
            if (isSpecial) {
                // 丢弃或点击背包外部
                switch (event.getAction()) {
                    case DROP_ALL_CURSOR:
                    case DROP_ONE_CURSOR:
                    case DROP_ALL_SLOT:
                    case DROP_ONE_SLOT:
                        event.setCancelled(true);
                        return;
                    default:
                        // 其他动作允许（移动、交换等）
                        break;
                }
                if (event.getClickedInventory() == null) { // 点击背包外部
                    event.setCancelled(true);
                }
            }
        }

        // Prevent moving special compasses into shared chests
        Team team = TeamsManager.getPlayerTeam(player);
        if (team != null && event.getClickedInventory() != null
                && event.getClickedInventory().equals(team.getSharedChest())) {
            if (clickedItem != null && clickedItem.getType() == Material.COMPASS) {
                event.setCancelled(true);
            }
        }

        if (event.getCurrentItem() != null && Utils.isIllegalItem(event.getCurrentItem())) {
            event.setCancelled(true);
            event.getWhoClicked().getInventory().remove(event.getCurrentItem());
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory closedInventory = event.getInventory();
        Team team = TeamsManager.getPlayerTeam(player);
        if (team != null && (closedInventory.equals(team.getSharedChest())
                || closedInventory.equals(team.getOpponent().getSharedChest()))) {
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.3f, 1.0f);
        }

        String title = event.getView().getTitle();
        if (title.contains("中途加入") && TeamsManager.getPlayerTeam(player) == null && !spectators.contains(player)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(event.getInventory()), 5L);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack draggedItem = event.getOldCursor();
        if (draggedItem != null && draggedItem.getType() == Material.COMPASS && draggedItem.hasItemMeta()) {
            if (draggedItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "hub_compass"))
                    || draggedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "job_compass"))
                    || draggedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "game_compass"))) {
                event.setCancelled(true);
            }
        }

        if (event.getView().getTitle().startsWith("游戏规则")) {
            event.setCancelled(true);
        }
    }
}
