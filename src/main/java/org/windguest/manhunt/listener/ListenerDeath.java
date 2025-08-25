package org.windguest.manhunt.listener;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.game.Teleport;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;
import org.windguest.manhunt.game.Compass;

import java.util.Random;

public class ListenerDeath implements Listener {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();

    private static String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return switch (item.getType()) {
            case WOODEN_SWORD -> "木剑";
            case STONE_SWORD -> "石剑";
            case IRON_SWORD -> "铁剑";
            case GOLDEN_SWORD -> "金剑";
            case DIAMOND_SWORD -> "钻石剑";
            case NETHERITE_SWORD -> "下界合金剑";
            case BOW -> "弓";
            case CROSSBOW -> "弩";
            case TRIDENT -> "三叉戟";
            case MACE -> "重锤";
            case WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE -> "斧";
            default -> item.getType().name().replace('_', ' ').toLowerCase();
        };
    }

    private static String getDeathCause(EntityDamageEvent.DamageCause cause) {
        return switch (cause) {
            case KILL -> "被玩家击杀";
            case WORLD_BORDER -> "世界边界";
            case CONTACT -> "实体接触";
            case ENTITY_ATTACK -> "实体攻击";
            case ENTITY_SWEEP_ATTACK -> "实体横扫攻击";
            case PROJECTILE -> "投射物";
            case SUFFOCATION -> "窒息";
            case FALL -> "摔落";
            case FIRE -> "火焰";
            case FIRE_TICK -> "火焰灼烧";
            case MELTING -> "融化";
            case LAVA -> "熔岩";
            case DROWNING -> "溺水";
            case BLOCK_EXPLOSION -> "方块爆炸";
            case ENTITY_EXPLOSION -> "实体爆炸";
            case VOID -> "虚空";
            case LIGHTNING -> "闪电";
            case SUICIDE -> "自杀";
            case STARVATION -> "饥饿";
            case POISON -> "中毒";
            case MAGIC -> "魔法";
            case WITHER -> "凋零";
            case FALLING_BLOCK -> "掉落的方块";
            case THORNS -> "荆棘";
            case DRAGON_BREATH -> "龙息";
            case FLY_INTO_WALL -> "撞墙";
            case HOT_FLOOR -> "岩浆块";
            case CAMPFIRE -> "营火";
            case CRAMMING -> "挤压";
            case DRYOUT -> "干涸";
            case FREEZE -> "冻结";
            case SONIC_BOOM -> "监守者声波";
            case CUSTOM -> "自定义原因";
        };
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (Game.getCurrentState() != Game.GameState.RUNNING) {
            return;
        }
        event.setDeathMessage(null);
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        Team victimTeam = TeamsManager.getPlayerTeam(victim);
        if (victimTeam == null) {
            return;
        }
        if (killer != null) {
            Team killerTeam = TeamsManager.getPlayerTeam(killer);
            if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                String role = TeamsManager.getPlayerTeam(killer).getName().equals("逃生者") ? "runner" : "hunter";
                DataManager.incrementPlayerData(killer, Mode.getCurrentMode(), role, "kills", 1);
            } else {
                DataManager.incrementPlayerData(killer, Mode.getCurrentMode(), "kills", 1);
            }
            killer.sendTitle("",
                    "§c§l🗡 §r" + victimTeam.getColorString() + victimTeam.getIcon() + " " + victim.getName(), 10, 70,
                    20);

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            String weaponDisplay;
            if (weapon.getType() == Material.AIR) {
                weaponDisplay = "§7用§e拳头§7";
            } else {
                weaponDisplay = "§7用 §b[" + getItemName(weapon) + "§b]§7";
            }

            Bukkit.broadcastMessage("§f[☠] " + killerTeam.getColorString() + killerTeam.getIcon() + " "
                    + killer.getName() + " " + weaponDisplay + " 击杀了 " + victimTeam.getColorString()
                    + victimTeam.getIcon() + " " + victim.getName());
        } else {
            String deathCause = getDeathCause(victim.getLastDamageCause().getCause());
            Bukkit.broadcastMessage("§f[☠] " + victimTeam.getColorString() + victimTeam.getIcon() + " "
                    + victim.getName() + " §7因为" + deathCause + "死亡了");
        }
        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
            String roleV = TeamsManager.getPlayerTeam(victim) != null
                    && TeamsManager.getPlayerTeam(victim).getName().equals("逃生者") ? "runner" : "hunter";
            DataManager.incrementPlayerData(victim, Mode.getCurrentMode(), roleV, "deaths", 1);
        } else {
            DataManager.incrementPlayerData(victim, Mode.getCurrentMode(), "deaths", 1);
        }
        Location deathLocation = victim.getLocation();
        World world = victim.getWorld();
        for (ItemStack item : victim.getInventory().getContents()) {
            if (item != null
                    && item.getType() != Material.AIR
                    && item.getType() != Material.COMPASS
                    && !item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                world.dropItemNaturally(deathLocation, item);
            }
        }
        for (ItemStack armor : victim.getInventory().getArmorContents()) {
            if (armor != null
                    && armor.getType() != Material.AIR
                    && armor.getType() != Material.COMPASS
                    && !armor.containsEnchantment(Enchantment.VANISHING_CURSE)) {
                world.dropItemNaturally(deathLocation, armor);
            }
        }

        // 清空背包与护甲栏，防止掉落后仍然留存
        victim.getInventory().clear();
        victim.getInventory().setArmorContents(new org.bukkit.inventory.ItemStack[4]);
        // 如果是追杀模式且死者为猎杀者，实现 5 秒复活，不从队伍移除
        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT && "猎杀者".equals(victimTeam.getName())) {
            victim.setGameMode(GameMode.SPECTATOR);

            // 5 秒复活计时
            new BukkitRunnable() {
                int time = 5;

                @Override
                public void run() {
                    if (!victim.isOnline()) {
                        cancel();
                        return;
                    }
                    if (time <= 0) {
                        // 复活
                        Location respawnLoc = victim.getBedSpawnLocation();
                        if (respawnLoc == null) {
                            World world = Bukkit.getWorld("world");
                            respawnLoc = world != null ? world.getSpawnLocation() : victim.getLocation();
                        }
                        victim.teleport(respawnLoc);
                        victim.setGameMode(GameMode.SURVIVAL);
                        Compass.giveGameCompass(victim);
                        victim.setHealth(20.0);
                        victim.setFoodLevel(20);
                        victim.setFireTicks(0);
                        victim.setFallDistance(0F);
                        victim.sendTitle("", "§a你已复活！", 10, 40, 10);
                        victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                        cancel();
                        return;
                    }
                    victim.sendTitle("§c你已死亡", "§e等待 " + time + " 秒复活", 0, 25, 0);
                    time--;
                }
            }.runTaskTimer(plugin, 0L, 20L);

            // 掉落物保持、火花等效果
            Utils.spawnFireworkAtPlayer(victim, victimTeam.getColor());
            event.setCancelled(true);
            return;
        }

        victim.setGameMode(GameMode.SPECTATOR);
        // 逃生者或其他模式：从队伍移除并标记死亡
        victimTeam.removePlayer(victim);
        TeamsManager.setDead(victim, victimTeam);
        Utils.spawnFireworkAtPlayer(victim, victimTeam.getColor());
        event.setCancelled(true);
        victim.setHealth(20.0);
        victim.setFoodLevel(20);
        victim.setFireTicks(0);
        victim.setFallDistance(0.0f);
        for (PotionEffect effect : victim.getActivePotionEffects()) {
            victim.removePotionEffect(effect.getType());
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Team targetTeam = victimTeam.isEmpty() ? victimTeam.getOpponent() : victimTeam;
            if (targetTeam != null && !targetTeam.isEmpty()) {
                Teleport.teleportToRandomTeamPlayer(victim, targetTeam);
            }
        }, 20L);

        if (victimTeam.isEmpty()) {
            Game.setEndLocation(deathLocation.clone());
            Game.endGame(victimTeam.getOpponent());
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        EntityType entityType = event.getEntityType();
        if (entityType == EntityType.WITHER) {
            event.getDrops().clear();
            ItemStack customNetherStar = new ItemStack(Material.NETHER_STAR);
            ItemMeta meta = customNetherStar.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b随身信标§7（右键点击获得永久BUFF）");
                customNetherStar.setItemMeta(meta);
            }
            event.getDrops().add(customNetherStar);
        } else if (entityType == EntityType.ENDER_DRAGON) {
            // 记录末影龙死亡地点，供 Game.endGame() 传送使用
            Game.setEndLocation(event.getEntity().getLocation());

            if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                Team runnerTeam = TeamsManager.getTeamByName("逃生者");
                if (runnerTeam != null) {
                    Game.endGame(runnerTeam);
                }
            } else {
                Game.endGame(TeamsManager.getTopDamageTeam());
            }
        } else if (entityType == EntityType.ENDERMAN) {
            event.getDrops().clear();
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL, rand.nextInt(2) + 1));
        } else if (entityType == EntityType.BLAZE) {
            event.getDrops().clear();
            event.getDrops().add(new ItemStack(Material.BLAZE_ROD, rand.nextInt(2) + 1));
        } else if (entityType == EntityType.WITHER_SKELETON) {
            event.getDrops().clear();
            event.getDrops().add(new ItemStack(Material.WITHER_SKELETON_SKULL, 1));
        }
    }
}
