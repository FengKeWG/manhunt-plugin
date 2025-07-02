package org.windguest.manhunt.jobs;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.windguest.manhunt.Main;

import java.util.*;

public class JobsManager {
    private static final Main plugin = Main.getInstance();
    private static final Map<Integer, Job> jobs = new HashMap<>();
    private static Map<UUID, Job> chosenJobs = new HashMap<>();

    public static boolean hasChosenJob(Player player) {
        return chosenJobs.containsKey(player.getUniqueId());
    }

    public static void setChosenJob(Player player, Job job) {
        chosenJobs.put(player.getUniqueId(), job);
    }

    public static Map<Integer, Job> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    public static Job getJobFromSlot(int slot) {
        return jobs.get(slot);
    }

    private void initializeJobs() {
        Job rangerJob = new Job(
                11,
                "§a游侠",
                Material.ENDER_PEARL,
                Arrays.asList(
                        "",
                        "§7开局获得10分钟的速度Ⅰ",
                        "§725分钟的急迫Ⅰ",
                        "§750分钟的生命恢复Ⅰ",
                        "§7开局获得4个末影珍珠"
                ),
                player -> {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 0)); // 10 minutes
                    player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 60 * 25, 0)); // 25 minutes
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60 * 50, 0)); // 50 minutes
                    player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
                    player.sendMessage("§a你选择了游侠职业！");
                }
        );
        jobs.put(rangerJob.getMenuSlot(), rangerJob);
        Job archerJob = new Job(
                13,
                "§a弓箭手",
                Material.BOW,
                Arrays.asList(
                        "",
                        "§7开局后获得力量3 冲击 火焰附件的弓",
                        "§71组箭矢和皮革头盔"
                ),
                player -> {
                    ItemStack bow = new ItemStack(Material.BOW);
                    ItemMeta bowMeta = bow.getItemMeta();
                    if (bowMeta != null) {
                        bowMeta.addEnchant(Enchantment.POWER, 3, true);
                        bowMeta.addEnchant(Enchantment.PUNCH, 1, true);
                        bowMeta.addEnchant(Enchantment.FLAME, 1, true);
                        bowMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                        bow.setItemMeta(bowMeta);
                    }
                    player.getInventory().addItem(bow);
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 64));
                    player.getInventory().addItem(new ItemStack(Material.LEATHER_HELMET));
                    player.sendMessage("§a你选择了弓箭手职业！");
                }
        );
        jobs.put(archerJob.getMenuSlot(), archerJob);
        Job blacksmithJob = new Job(
                15,
                "§a铁匠",
                Material.IRON_INGOT,
                Arrays.asList(
                        "",
                        "§7开局后获得16个铁、16个熟牛肉",
                        "§7铁胸甲，铁镐"
                ),
                player -> {
                    player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 16));
                    player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
                    player.getInventory().addItem(new ItemStack(Material.IRON_CHESTPLATE));
                    player.getInventory().addItem(new ItemStack(Material.IRON_PICKAXE));
                    player.sendMessage("§a你选择了铁匠职业！");
                }
        );
        jobs.put(blacksmithJob.getMenuSlot(), blacksmithJob);
        // --- 要添加新职业？只需要在这里添加一个新的 Job 实例！ ---
        // 例如，添加一个“矿工”职业
        /*
        jobs.put("miner", new Job(
                "miner",
                "§b矿工",
                Material.DIAMOND_PICKAXE,
                Arrays.asList(
                        "",
                        "§7开局获得效率II的石镐",
                        "§710个火把和5个面包"
                ),
                17, // 下一个可用slot
                player -> {
                    ItemStack pickaxe = new ItemStack(Material.STONE_PICKAXE);
                    pickaxe.addEnchantment(Enchantment.DIG_SPEED, 2);
                    player.getInventory().addItem(pickaxe);
                    player.getInventory().addItem(new ItemStack(Material.TORCH, 10));
                    player.getInventory().addItem(new ItemStack(Material.BREAD, 5));
                    player.sendMessage("§b你选择了矿工职业！");
                }
        ));
        */
    }

}
