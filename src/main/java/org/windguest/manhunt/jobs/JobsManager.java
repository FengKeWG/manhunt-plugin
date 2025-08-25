package org.windguest.manhunt.jobs;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
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

    public static boolean isGhost(Player p) {
        Job j = chosenJobs.get(p.getUniqueId());
        return j != null && j.getDisplayName().contains("幽灵");
    }

    public static Map<Integer, Job> getJobs() {
        return Collections.unmodifiableMap(jobs);
    }

    public static Job getJobFromSlot(int slot) {
        return jobs.get(slot);
    }

    public static void initializeJobs() {
        Job rangerJob = new Job(11, "§a游侠", Material.ENDER_PEARL, Arrays.asList("", "§7开局获得10分钟的速度Ⅰ", "§725分钟的急迫Ⅰ", "§750分钟的生命恢复Ⅰ", "§7开局获得4个末影珍珠"), player -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60 * 10, 0)); // 10 minutes
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 60 * 25, 0)); // 25 minutes
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60 * 50, 0)); // 50
            // minutes
            player.getInventory().addItem(new ItemStack(Material.ENDER_PEARL, 4));
            player.sendMessage("§a你选择了游侠职业！");
        });
        jobs.put(rangerJob.getMenuSlot(), rangerJob);
        Job archerJob = new Job(13, "§a弓箭手", Material.BOW, Arrays.asList("", "§7开局后获得力量3 冲击 火焰附件的弓", "§764 个燧石(可与羽毛+木棍合成箭)以及皮革头盔"), player -> {
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
            player.getInventory().addItem(new ItemStack(Material.FLINT, 64));
            player.getInventory().addItem(new ItemStack(Material.LEATHER_HELMET));
            player.sendMessage("§a你选择了弓箭手职业！");
        });
        jobs.put(archerJob.getMenuSlot(), archerJob);
        Job blacksmithJob = new Job(15, "§a铁匠", Material.IRON_INGOT, Arrays.asList("", "§7开局后获得16个铁、16个熟牛肉", "§7铁胸甲，铁镐"), player -> {
            player.getInventory().addItem(new ItemStack(Material.IRON_INGOT, 16));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
            player.getInventory().addItem(new ItemStack(Material.IRON_CHESTPLATE));
            ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
            pick.addEnchantment(Enchantment.EFFICIENCY, 3);
            player.getInventory().addItem(pick);
            player.sendMessage("§a你选择了铁匠职业！");
        });
        jobs.put(blacksmithJob.getMenuSlot(), blacksmithJob);

        // 战士
        Job warriorJob = new Job(20, "§a战士", Material.IRON_SWORD, Arrays.asList("", "§7获得锋利 I 铁剑和盾牌", "§716 个熟牛排与铁头盔", "§7开局 4 分钟力量 I"), player -> {
            ItemStack sword = new ItemStack(Material.IRON_SWORD);
            sword.addEnchantment(Enchantment.SHARPNESS, 1);
            sword.addEnchantment(Enchantment.VANISHING_CURSE, 1);
            player.getInventory().addItem(sword);
            player.getInventory().addItem(new ItemStack(Material.SHIELD));
            player.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
            player.getInventory().addItem(new ItemStack(Material.IRON_HELMET));
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 240, 0));
            player.sendMessage("§a你选择了战士职业！");
        });
        jobs.put(warriorJob.getMenuSlot(), warriorJob);

        // 附魔师
        Job enchanterJob = new Job(22, "§a附魔师", Material.ENCHANTING_TABLE, Arrays.asList("", "§7获得附魔台和 32 个青金石", "§75 个书架，16 个经验瓶"), player -> {
            player.getInventory().addItem(new ItemStack(Material.ENCHANTING_TABLE));
            player.getInventory().addItem(new ItemStack(Material.LAPIS_LAZULI, 32));
            player.getInventory().addItem(new ItemStack(Material.BOOKSHELF, 5));
            player.getInventory().addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
            player.sendMessage("§a你选择了附魔师职业！");
        });
        jobs.put(enchanterJob.getMenuSlot(), enchanterJob);

        // 酿药师
        Job alchemistJob = new Job(24, "§a酿药师", Material.BREWING_STAND, Arrays.asList("", "§7获得酿造台、地狱疣 32", "§7速度 I 与 抗火药水各 1 瓶"), player -> {
            player.getInventory().addItem(new ItemStack(Material.BREWING_STAND));
            player.getInventory().addItem(new ItemStack(Material.NETHER_WART, 32));
            player.getInventory().addItem(new ItemStack(Material.POTION));
            ItemStack speed = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) speed.getItemMeta();
            if (meta != null) {
                meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 180, 0), true);
                meta.setDisplayName("§f速度药水 I (3:00)");
                speed.setItemMeta(meta);
            }
            player.getInventory().addItem(speed);
            ItemStack fire = new ItemStack(Material.POTION);
            PotionMeta meta2 = (PotionMeta) fire.getItemMeta();
            if (meta2 != null) {
                meta2.addCustomEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 180, 0), true);
                meta2.setDisplayName("§f抗火药水 (3:00)");
                fire.setItemMeta(meta2);
            }
            player.getInventory().addItem(fire);
            player.sendMessage("§a你选择了酿药师职业！");
        });
        jobs.put(alchemistJob.getMenuSlot(), alchemistJob);
        Job ghostJob = new Job(
                29,
                "§a幽灵",
                Material.GHAST_TEAR,
                Arrays.asList(
                        "",
                        "§7永久隐身（有粒子）",
                        "§7获得成就不提示",
                        "§732格内定位不显示具体距离"),
                player -> {
                    PotionEffect invis = new PotionEffect(
                            PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0);
                    player.addPotionEffect(invis);
                    player.sendMessage("§a你选择了幽灵职业！");
                });
        jobs.put(ghostJob.getMenuSlot(), ghostJob);
        // --- 要添加新职业？只需要在这里添加一个新的 Job 实例！ ---
        // 例如，添加一个“矿工”职业
        /*
         * jobs.put("miner", new Job(
         * "miner",
         * "§b矿工",
         * Material.DIAMOND_PICKAXE,
         * Arrays.asList(
         * "",
         * "§7开局获得效率II的石镐",
         * "§710个火把和5个面包"
         * ),
         * 17, // 下一个可用slot
         * player -> {
         * ItemStack pickaxe = new ItemStack(Material.STONE_PICKAXE);
         * pickaxe.addEnchantment(Enchantment.DIG_SPEED, 2);
         * player.getInventory().addItem(pickaxe);
         * player.getInventory().addItem(new ItemStack(Material.TORCH, 10));
         * player.getInventory().addItem(new ItemStack(Material.BREAD, 5));
         * player.sendMessage("§b你选择了矿工职业！");
         * }
         * ));
         */
    }

}
