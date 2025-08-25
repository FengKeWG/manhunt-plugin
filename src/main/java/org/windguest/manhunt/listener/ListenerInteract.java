package org.windguest.manhunt.listener;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.menus.ModesMenu;
import org.windguest.manhunt.menus.PreferenceMenu;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Compass;
import org.windguest.manhunt.menus.JobsMenu;
import org.windguest.manhunt.menus.RulesMenu;
import org.windguest.manhunt.menus.TeleportMenu;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;

public class ListenerInteract implements Listener {
    private static final Main plugin = Main.getInstance();

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTitle().contains("共享背包")) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (item.getType() == Material.NETHER_STAR && event.getAction() == Action.RIGHT_CLICK_AIR) {
            PersistentDataContainer data = player.getPersistentDataContainer();
            NamespacedKey key = new NamespacedKey(plugin, "nether_star_effect_stage");
            int stage = data.getOrDefault(key, PersistentDataType.INTEGER, 0);
            switch (stage) {
                case 0: {
                    player.addPotionEffect(
                            new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1, true, false));
                    data.set(key, PersistentDataType.INTEGER, 1);
                    break;
                }
                case 1: {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1, true, false));
                    data.set(key, PersistentDataType.INTEGER, 2);
                    break;
                }
                case 2: {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 1, true, false));
                    data.set(key, PersistentDataType.INTEGER, 3);
                    break;
                }
                case 3: {
                    player.addPotionEffect(
                            new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));
                    data.set(key, PersistentDataType.INTEGER, 4);
                    break;
                }
                case 4: {
                    player.addPotionEffect(
                            new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 1, true, false));
                    data.set(key, PersistentDataType.INTEGER, 5);
                    break;
                }
                default: {
                    player.sendMessage("你已经获得所有效果！");
                    return;
                }
            }
            item.setAmount(item.getAmount() - 1);
        }
        if (item.getType() == Material.COMPASS && item.hasItemMeta()) {
            PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
            Action action = event.getAction();

            // Hub Compass
            if (container.has(new NamespacedKey(plugin, "hub_compass"))) {
                if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                    RulesMenu.open(player);
                } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                    if (Mode.getCurrentMode() == null) {
                        ModesMenu.open(player);
                    } else if (TeamsManager.isPrefVotingStarted()) {
                        PreferenceMenu.open(player);
                    }
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                event.setCancelled(true);
                return;
            }

            // Job Compass
            if (container.has(new NamespacedKey(plugin, "job_compass"))) {
                if (Game.getCurrentState() == Game.GameState.FROZEN) {
                    if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        JobsMenu.open(player);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                        event.setCancelled(true);
                    }
                }
                return;
            }

            // Game Compass
            if (container.has(new NamespacedKey(plugin, "game_compass"))) {
                if (Game.getCurrentState() == Game.GameState.RUNNING) {
                    if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                        // 左键打开传送菜单
                        if (!player.hasMetadata("teleporting")) {
                            Player nearestPlayer = Compass.findNearestEnemy(player);
                            if (nearestPlayer != null
                                    && player.getLocation().distance(nearestPlayer.getLocation()) < 50.0) {
                                player.sendMessage("§c[❌] 附近50格内有敌人，无法打开传送菜单！");
                                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                            } else {
                                TeleportMenu.open(player, 0);
                                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
                            }
                        }
                    } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                        // 右键打开共享背包
                        Player nearestPlayer = Compass.findNearestEnemy(player);
                        if (nearestPlayer != null
                                && player.getLocation().distance(nearestPlayer.getLocation()) < 50.0) {
                            player.sendMessage("§c[❌] 附近50格内有敌人，无法打开共享背包！");
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                        } else {
                            Team team = TeamsManager.getPlayerTeam(player);
                            if (team != null) {
                                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.3f, 1.0f);
                                player.openInventory(team.getSharedChest());
                            }
                        }
                    }
                }
            }
        }
    }
}
