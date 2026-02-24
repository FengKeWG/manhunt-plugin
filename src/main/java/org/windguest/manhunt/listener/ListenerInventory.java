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
import org.windguest.manhunt.world.EndLocationManager;

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
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        Material itemType = clickedItem.getType();
        Player player = (Player) event.getWhoClicked();

        if (event.getView().getTitle().equals("游戏模式投票")) {
            if (Mode.getCurrentMode() == Mode.GameMode.END) {
                player.sendMessage("§c[!] 混沌末地模式下不允许投票！");
                player.closeInventory();
                event.setCancelled(true);
                return;
            }
            if (!Mode.isVoting()) {
                player.sendMessage("§c[!] 投票未在进行中！");
                player.closeInventory();
                event.setCancelled(true);
                return;
            }
            if (itemType == Material.DRAGON_EGG) {
                Mode.setPreference(player, Mode.GameMode.MANHUNT);
                Bukkit.broadcastMessage("§a[✔] " + player.getName() + " 投给了 §a追杀模式");
            } else if (itemType == Material.CHEST) {
                Mode.setPreference(player, Mode.GameMode.TEAM);
                Bukkit.broadcastMessage("§a[✔] " + player.getName() + " 投给了 §b团队模式");
            } else if (itemType == Material.ENDER_EYE) {
                Mode.setPreference(player, Mode.GameMode.END);
                Bukkit.broadcastMessage("§d[✔] " + player.getName() + " 投给了 §5浑沌末地");
                if (Mode.getCurrentMode() != Mode.GameMode.END) {
                    Bukkit.broadcastMessage("§c[⚠] 注意：选择混沌末地模式将需要重启服务器！");
                }
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
                    if (pTeam != null) {
                        Bukkit.broadcastMessage((pTeam.getColorString() + "[" + pTeam.getIcon() + "] " + player.getName()
                                + " 选择了职业：" + selectedJob.getDisplayName()));
                    } else {
                        player.sendMessage("§a[✔] 你选择了职业：" + selectedJob.getDisplayName());
                    }
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
                if (Mode.getCurrentMode() == Mode.GameMode.END) {
                    player.sendMessage("§d[🌌] 浑沌末地模式中，所有玩家已在末地！");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }
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
        } else if (event.getView().getTitle().startsWith("游戏规则")) {
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

            if (itemType == Material.TOTEM_OF_UNDYING) {
                player.sendMessage("§a[✔] 你加入了逃生者阵营！");
                runnerTeam.addPlayer(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2, 60));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 4, 120));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4, 120));
            } else if (itemType == Material.IRON_SWORD) {
                player.sendMessage("§c[🏹] 你加入了猎人阵营！");
                hunterTeam.addPlayer(player);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 2, 600));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 4, 1200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 4, 1200));
            } else if (itemType == Material.ENDER_EYE) {
                player.sendMessage("§7[🚫] 你选择了作为旁观者观战");
                spectators.add(player);
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                Teleport.teleportToRandomTeamPlayer(player, null);
                player.closeInventory();
                return;
            }
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
                Team joinTeam = TeamsManager.getPlayerTeam(player);
                if (joinTeam != null) {
                    Teleport.teleportToRandomTeamPlayer(player, joinTeam);
                }
            }
            player.closeInventory();
            return;
        } else if (event.getView().getTitle().equals("中途加入")) {
            event.setCancelled(true);
            if (clickedItem.getType() == Material.BOW) {
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

                    // 如果是混沌末地模式，传送到对应队伍的基地
                    if (Mode.getCurrentMode() == Mode.GameMode.END) {
                        Team playerTeam = TeamsManager.getPlayerTeam(player);
                        if (playerTeam != null) {
                            Location baseLoc = "红队".equals(playerTeam.getName())
                                    ? EndLocationManager.getRedEndBase()
                                    : EndLocationManager.getBlueEndBase();
                            if (baseLoc != null) {
                                player.teleport(baseLoc);
                            } else {
                                Utils.teleportToEnd(player);
                            }
                        } else {
                            Utils.teleportToEnd(player);
                        }
                        Utils.endDown(player);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 1));
                    } else {
                        // 非末地模式：传送到主世界出生点
                        World world = Bukkit.getWorld("world");
                        if (player.getWorld() != world && world != null) {
                            Location worldSpawn = world.getSpawnLocation();
                            player.teleport(worldSpawn);
                        }
                    }
                }
                player.closeInventory();
            } else if (clickedItem.getType() == Material.ENDER_EYE) {
                player.sendMessage("§7[🚫] 你选择了作为旁观者观战");
                spectators.add(player);
                player.setGameMode(GameMode.SPECTATOR);
                player.addPotionEffect(
                        new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
                if (Mode.getCurrentMode() == Mode.GameMode.END) {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getWorld().getEnvironment() == World.Environment.THE_END) {
                            player.teleport(onlinePlayer.getLocation());
                            break;
                        }
                    }
                } else {
                    Teleport.teleportToRandomTeamPlayer(player, null);
                }
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

        if (clickedItem != null && clickedItem.getType() == Material.COMPASS && clickedItem.hasItemMeta()) {
            boolean isSpecial = clickedItem.getItemMeta().getPersistentDataContainer()
                    .has(new NamespacedKey(plugin, "hub_compass"))
                    || clickedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "job_compass"))
                    || clickedItem.getItemMeta().getPersistentDataContainer()
                            .has(new NamespacedKey(plugin, "game_compass"));
            if (isSpecial) {
                switch (event.getAction()) {
                    case DROP_ALL_CURSOR:
                    case DROP_ONE_CURSOR:
                    case DROP_ALL_SLOT:
                    case DROP_ONE_SLOT:
                        event.setCancelled(true);
                        return;
                    default:
                        break;
                }
                if (event.getClickedInventory() == null) {
                    event.setCancelled(true);
                }
            }
        }
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