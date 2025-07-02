package org.windguest.manhunt.listener;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class ListenerBlock implements Listener {

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material block = event.getBlock().getType();
        World.Environment environment = player.getWorld().getEnvironment();
        if (block == Material.END_CRYSTAL) {
            event.setCancelled(true);
            player.sendMessage("§c[❌] 你不能放置末影水晶！");
            return;
        }
        if (block.toString().endsWith("_BED") && environment != World.Environment.NORMAL) {
            event.setCancelled(true);
            player.sendMessage("§c[❌] 你不能在非主世界放置床！");
            return;
        }
        if (block == Material.RESPAWN_ANCHOR && environment != World.Environment.NETHER) {
            event.setCancelled(true);
            player.sendMessage("§c[❌] 你不能在非下界放置重生锚！");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        ItemStack tool = player.getInventory().getItemInMainHand();
        int fortuneLevel = tool.getEnchantmentLevel(Enchantment.FORTUNE);

        ItemStack drop;
        Random random = new Random();
        switch (blockType) {
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.IRON_INGOT, amount);
                break;
            }
            case RAW_IRON_BLOCK: {
                drop = new ItemStack(Material.IRON_BLOCK, 1);
                break;
            }
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.GOLD_INGOT, amount);
                break;
            }
            case RAW_GOLD_BLOCK: {
                drop = new ItemStack(Material.GOLD_BLOCK, 1);
                break;
            }
            case ANCIENT_DEBRIS: {
                int amount = 1 + random.nextInt(3);
                drop = new ItemStack(Material.NETHERITE_SCRAP, amount);
                break;
            }
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.COPPER_INGOT, amount);
                break;
            }
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.DIAMOND, amount);
                break;
            }
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.EMERALD, amount);
                break;
            }
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE: {
                int amount = 4 + random.nextInt(8);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.LAPIS_LAZULI, amount);
                break;
            }
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE: {
                int amount = 4 + random.nextInt(8);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.REDSTONE, amount);
                break;
            }
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE: {
                int amount = 1 + random.nextInt(3);
                amount = applyFortune(amount, fortuneLevel, random);
                drop = new ItemStack(Material.COAL, amount);
                break;
            }
            default: {
                return;
            }
        }
        event.setDropItems(false);
        event.getBlock().getWorld().dropItem(event.getBlock().getLocation().add(0.5, 0.2, 0.5), drop);
    }

    private int applyFortune(int baseAmount, int fortuneLevel, Random random) {
        if (fortuneLevel > 0) {
            switch (fortuneLevel) {
                case 1: {
                    if (!(random.nextDouble() < 0.33)) break;
                    return baseAmount * 2;
                }
                case 2: {
                    double chance = random.nextDouble();
                    if (chance < 0.25) {
                        return baseAmount * 2;
                    }
                    if (!(chance < 0.5)) break;
                    return baseAmount * 3;
                }
                case 3: {
                    double chance = random.nextDouble();
                    if (chance < 0.2) {
                        return baseAmount * 2;
                    }
                    if (chance < 0.4) {
                        return baseAmount * 3;
                    }
                    if (!(chance < 0.6)) break;
                    return baseAmount * 4;
                }
            }
        }
        return baseAmount;
    }
}
