package org.windguest.manhunt.listener;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.utils.Utils;

public class ListenerDamage implements Listener {

    @EventHandler
    private void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework && event.getDamager().hasMetadata("noDamage")) {
            event.setCancelled(true);
            return;
        }
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        double damage = event.getFinalDamage();
        if (damaged instanceof Player victim) {
            Player attacker = null;
            if (damager instanceof Player) {
                attacker = (Player) damager;
                Material attackerItem = attacker.getInventory().getItemInMainHand().getType();
                if (Utils.isAxe(attackerItem)) {
                    if (victim.isBlocking() && victim.isHandRaised() && damage == 0.0) {
                        attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                        return;
                    }
                }
            }
            if (damager instanceof Projectile projectile) {
                if (projectile instanceof Arrow) {
                    victim.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);
                }
                if (projectile.getShooter() instanceof Player) {
                    attacker = (Player) projectile.getShooter();
                }
            }
            if (victim.isBlocking() && victim.isHandRaised() && damage == 0.0) {
                victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                if (attacker != null) {
                    attacker.playSound(attacker.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
                }
            }
            if (attacker != null) {
                if (TeamsManager.areSameTeam(attacker, victim)) {
                    event.setCancelled(true);
                    return;
                }
            }
            // combat tag
//            CombatManager.tag(victim);
//            CombatManager.tryNotifyLowHealth(victim);
//            if (damage > 0) {
//                victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5,
//                        0.5, 0.0, Material.RED_WOOL.createBlockData());
//            }
        }
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damaged = event.getEntity();
        Entity damager = event.getDamager();
        double damage = event.getFinalDamage();
        if (damaged instanceof EnderDragon dragon) {
            if (dragon.getMaxHealth() < 500.0) {
                dragon.setMaxHealth(500.0);
                dragon.setHealth(500.0 - damage);
            }
            Player player = null;
            if (damager instanceof Player) {
                player = (Player) damager;
            }
            if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player) {
                    player = (Player) projectile.getShooter();
                }
            }
            if (player != null) {
                Team team = TeamsManager.getPlayerTeam(player);
                team.addDragonDamage(damage);
            }
        }
    }
}
