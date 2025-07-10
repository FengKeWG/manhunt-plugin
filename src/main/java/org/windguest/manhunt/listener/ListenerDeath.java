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
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Teleport;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;

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
        String deadIcon = victimTeam.getIcon();
        if (killer != null) {
            Team killerTeam = TeamsManager.getPlayerTeam(killer);
            DataManager.updatePlayerData(killer, "kills");
            String killerIcon = killerTeam.getIcon();
            killer.sendTitle("", "§c§l🗡 §r" + deadIcon + " " + victim.getName(), 10, 70, 20);

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            String weaponDisplay;
            if (weapon.getType() == Material.AIR) {
                weaponDisplay = "§7用§e拳头§7";
            } else {
                weaponDisplay = "§7用§b[" + getItemName(weapon) + "§b]§7";
            }

            Bukkit.broadcastMessage(
                    "§f[☠] " + killerIcon + " " + killer.getName() + " " + weaponDisplay + " 击杀了 " + deadIcon + " "
                            + victim.getName());
        } else {
            String deathCause = getDeathCause(victim.getLastDamageCause().getCause());
            Bukkit.broadcastMessage("§f[☠] " + deadIcon + " " + victim.getName() + " §7因为" + deathCause + "死亡了");
        }
        DataManager.updatePlayerData(victim, "deaths");
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
        victim.setGameMode(GameMode.SPECTATOR);
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

        // Teleport spectator to a teammate or enemy
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Team targetTeam = victimTeam.isEmpty() ? victimTeam.getOpponent() : victimTeam;
            if (targetTeam != null && !targetTeam.isEmpty()) {
                Teleport.teleportToRandomTeamPlayer(victim, targetTeam);
                victim.sendMessage("§7你已死亡，正在传送至随机玩家身边...");
            }
        }, 20L); // Delay teleport slightly

        if (victimTeam.isEmpty()) {
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
