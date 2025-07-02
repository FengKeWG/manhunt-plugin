package org.windguest.manhunt.listener;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.StructureType;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.windguest.manhunt.world.StructureManager;

import java.util.List;
import java.util.Random;

public class ListenerWorld implements Listener {

    private static final Random rand = new Random();

    @EventHandler
    public static void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        World.Environment environment = world.getEnvironment();
        switch (environment) {
            case NORMAL:
                StructureManager.handleStructureUpdate(player, StructureType.STRONGHOLD, 1500);
                break;
            case NETHER:
                StructureManager.handleStructureUpdate(player, StructureType.NETHER_FORTRESS, 200);
                StructureManager.handleStructureUpdate(player, StructureType.BASTION_REMNANT, 200);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        World world = event.getWorld();
        List<ItemStack> loot = event.getLoot();
        for (ItemStack item : loot) {
            int multiplier = rand.nextInt(3) + 1;
            item.setAmount(item.getAmount() * multiplier);
        }
        if (world.getEnvironment() == World.Environment.THE_END) {
            addRandomItems(loot);
        }
    }

    private void setRandomDurability(ItemStack item, int minPercent, int maxPercent) {
        int maxDurability = item.getType().getMaxDurability();
        int percent = minPercent + rand.nextInt(maxPercent - minPercent + 1);
        short durability = (short) ((percent / 100.0) * maxDurability);
        durability = (short) Math.min(durability, maxDurability);
        item.setDurability((short) (item.getType().getMaxDurability() - durability));
    }

    private void addRandomItems(List<ItemStack> loot) {
        if (rand.nextDouble() < 0.30) {
            ItemStack elytra = new ItemStack(Material.ELYTRA);
            setRandomDurability(elytra, 2, 20);
            loot.add(elytra);
        }
        if (rand.nextDouble() < 0.40) {
            int amount = 1 + rand.nextInt(8);
            ItemStack fireworks = new ItemStack(Material.FIREWORK_ROCKET, amount);
            loot.add(fireworks);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
            setRandomDurability(diamondSword, 20, 100);
            int sharpnessLevel = rand.nextInt(6);
            int knockbacklevel = rand.nextInt(3);
            if (sharpnessLevel > 0) {
                diamondSword.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpnessLevel);
            }
            if (knockbacklevel > 0) {
                diamondSword.addUnsafeEnchantment(Enchantment.KNOCKBACK, sharpnessLevel);
            }
            loot.add(diamondSword);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack diamondAxe = new ItemStack(Material.DIAMOND_AXE);
            setRandomDurability(diamondAxe, 20, 100);
            int sharpnessLevel = rand.nextInt(6);
            int efficiencyLevel = rand.nextInt(6);
            if (sharpnessLevel > 0) {
                diamondAxe.addUnsafeEnchantment(Enchantment.SHARPNESS, sharpnessLevel);
            }
            if (efficiencyLevel > 0) {
                diamondAxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, efficiencyLevel);
            }
            loot.add(diamondAxe);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack diamondPickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
            setRandomDurability(diamondPickaxe, 20, 100);
            int efficiencyLevel = rand.nextInt(6);
            if (efficiencyLevel > 0) {
                diamondPickaxe.addUnsafeEnchantment(Enchantment.EFFICIENCY, efficiencyLevel);
            }
            loot.add(diamondPickaxe);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
            setRandomDurability(helmet, 20, 100);
            int protectionLevel = rand.nextInt(5);
            if (protectionLevel > 0) {
                helmet.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
            }
            loot.add(helmet);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
            setRandomDurability(chestplate, 20, 100);
            int protectionLevel = rand.nextInt(5);
            if (protectionLevel > 0) {
                chestplate.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
            }
            loot.add(chestplate);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
            setRandomDurability(leggings, 20, 100);
            int protectionLevel = rand.nextInt(5);
            if (protectionLevel > 0) {
                leggings.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
            }
            loot.add(leggings);
        }
        if (rand.nextDouble() < 0.20) {
            ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
            setRandomDurability(boots, 20, 100);
            int protectionLevel = rand.nextInt(5);
            if (protectionLevel > 0) {
                boots.addUnsafeEnchantment(Enchantment.PROTECTION, protectionLevel);
            }
            loot.add(boots);
        }
    }

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        Random random = new Random();
        event.getOutcome().clear();
        int pearlCount = 2 + random.nextInt(3);
        ItemStack pearls = new ItemStack(Material.ENDER_PEARL, pearlCount);
        int obsidianCount = 1 + random.nextInt(2);
        ItemStack obsidian = new ItemStack(Material.OBSIDIAN, obsidianCount);
        ItemStack splashFireResist = new ItemStack(Material.SPLASH_POTION);
        PotionMeta splashMeta = (PotionMeta) splashFireResist.getItemMeta();
        splashMeta.clearCustomEffects();
        splashMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0), true);
        splashMeta.setDisplayName("§f喷溅型抗火药水");
        splashMeta.setColor(Color.fromRGB(225, 186, 128));
        splashFireResist.setItemMeta(splashMeta);
        ItemStack fireResist = new ItemStack(Material.POTION);
        PotionMeta fireMeta = (PotionMeta) fireResist.getItemMeta();
        fireMeta.clearCustomEffects();
        fireMeta.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 3600, 0), true);
        fireMeta.setDisplayName("§f抗火药水");
        splashMeta.setColor(Color.fromRGB(225, 186, 128));
        fireResist.setItemMeta(fireMeta);
        int spectralArrowCount = 6 + random.nextInt(7);
        ItemStack spectralArrows = new ItemStack(Material.SPECTRAL_ARROW, spectralArrowCount);
        int dropChoice = random.nextInt(5);
        switch (dropChoice) {
            case 0: {
                event.getOutcome().add(pearls);
                break;
            }
            case 1: {
                event.getOutcome().add(obsidian);
                break;
            }
            case 2: {
                event.getOutcome().add(splashFireResist);
                break;
            }
            case 3: {
                event.getOutcome().add(fireResist);
                break;
            }
            case 4: {
                event.getOutcome().add(spectralArrows);
            }
        }
    }
}
