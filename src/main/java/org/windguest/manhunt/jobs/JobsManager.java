package org.windguest.manhunt.jobs;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
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

        // 创建附魔盔甲的辅助方法
        private static ItemStack createEnchantedArmor(Material material, String displayName) {
            ItemStack armor = new ItemStack(material);
            ItemMeta meta = armor.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName("§6巨人" + displayName);
                
                // 1. 基础原版附魔：所有装备都有的
                try {
                    // 耐久X
                    Enchantment unbreaking = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                    if (unbreaking != null) {
                        meta.addEnchant(unbreaking, 10, true);
                    }
                    
                    // 绑定诅咒 - 胸甲除外
                    if (material != Material.NETHERITE_CHESTPLATE) {
                        Enchantment bindingCurse = Enchantment.getByKey(NamespacedKey.minecraft("binding_curse"));
                        if (bindingCurse != null) {
                            meta.addEnchant(bindingCurse, 1, true);
                        }
                    }
        
                    Enchantment vanishingCurse = Enchantment.getByKey(NamespacedKey.minecraft("vanishing_curse"));
                    if (vanishingCurse != null) {
                        meta.addEnchant(vanishingCurse, 1, true);
                    }        
        
                    // 火焰保护5 - 全套装备都有
                    Enchantment fireProtection = Enchantment.getByKey(NamespacedKey.minecraft("fire_protection"));
                    if (fireProtection != null) {
                        meta.addEnchant(fireProtection, 5, true);
                    }
                    
                    Enchantment projectileProtection = Enchantment.getByKey(NamespacedKey.minecraft("projectile_protection"));
                    if (projectileProtection != null) {
                        meta.addEnchant(projectileProtection, 5, true);
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("添加基础原版附魔时出错: " + e.getMessage());
                }
                
                // 2. 自定义附魔：Curse of Enchant
                try {
                    Enchantment curseOfEnchant = Enchantment.getByKey(NamespacedKey.fromString("enchantplus:durability/curse_of_enchant"));
                    if (curseOfEnchant != null) {
                        meta.addEnchant(curseOfEnchant, 1, true);
                    }
                } catch (Exception e) {
                    // 忽略错误
                }
                
                // 3. 根据装备部位添加特定的附魔
                try {
                    if (material == Material.NETHERITE_HELMET) {
                        // 头盔附魔
                        addCustomEnchant(meta, "enchantplus:helmet/voidless", 1, "Voidless");
                        
                        // 原版水下呼吸3
                        Enchantment respiration = Enchantment.getByKey(NamespacedKey.minecraft("respiration"));
                        if (respiration != null) {
                            meta.addEnchant(respiration, 3, true);
                        }
                        
                        // 原版水下速掘
                        Enchantment aquaAffinity = Enchantment.getByKey(NamespacedKey.minecraft("aqua_affinity"));
                        if (aquaAffinity != null) {
                            meta.addEnchant(aquaAffinity, 1, true);
                        }
                        
                    } else if (material == Material.NETHERITE_CHESTPLATE) {
                        // 胸甲附魔
                        addCustomEnchant(meta, "enchantplus:chestplate/builder_arm", 3, "Builder Arm");
                        addCustomEnchant(meta, "enchantplus:armor/venom_protection", 1, "Venom Protection");
                        addCustomEnchant(meta, "enchantplus:armor/lifeplus", 5, "Lifeplus");
                        
                    } else if (material == Material.NETHERITE_LEGGINGS) {
                        // 护腿附魔
                        addCustomEnchant(meta, "enchantplus:leggings/leaping", 3, "Leaping");
                        addCustomEnchant(meta, "enchantplus:armor/lifeplus", 5, "Lifeplus");
                        addCustomEnchant(meta, "enchantplus:leggings/oversize", 4, "Oversize");
                        Enchantment swiftSneak = Enchantment.getByKey(NamespacedKey.minecraft("swift_sneak"));
if (swiftSneak != null) {
    meta.addEnchant(swiftSneak, 7, true);
    Bukkit.getLogger().info("成功添加迅捷潜行7到巨人护腿");
}

                    } else if (material == Material.NETHERITE_BOOTS) {
                        // 靴子附魔
                        addCustomEnchant(meta, "enchantplus:boots/step_assist", 3, "Step Assist");
                        addCustomEnchant(meta, "enchantplus:boots/lava_walker", 3, "Lava Walker");
                        
                        // 原版深海探索者1
                        Enchantment depthStrider = Enchantment.getByKey(NamespacedKey.minecraft("depth_strider"));
                        if (depthStrider != null) {
                            meta.addEnchant(depthStrider, 1, true);
                        }
                        
                        // 原版摔落保护10
                        Enchantment featherFalling = Enchantment.getByKey(NamespacedKey.minecraft("feather_falling"));
                        if (featherFalling != null) {
                            meta.addEnchant(featherFalling, 10, true);
                        }
                        Enchantment soulSpeed = Enchantment.getByKey(NamespacedKey.minecraft("soul_speed"));
if (soulSpeed != null) {
    meta.addEnchant(soulSpeed, 7, true);
    Bukkit.getLogger().info("成功添加灵魂疾行7到巨人靴子");
}
                    }
                } catch (Exception e) {
                    Bukkit.getLogger().warning("添加特定部位附魔时出错: " + e.getMessage());
                }
                
                // 设置不可破坏
                meta.setUnbreakable(true);
                
                armor.setItemMeta(meta);
            }
            

            
            return armor;
        }
        
        // 辅助方法：添加自定义附魔
        private static void addCustomEnchant(ItemMeta meta, String key, int level, String enchantName) {
            try {
                Enchantment enchant = Enchantment.getByKey(NamespacedKey.fromString(key));
                if (enchant != null) {
                    meta.addEnchant(enchant, level, true);
                    Bukkit.getLogger().info("成功添加自定义附魔 " + enchantName + " 等级 " + level);
                } else {
                    Bukkit.getLogger().warning("无法找到自定义附魔: " + key);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("添加自定义附魔 " + enchantName + " 时出错: " + e.getMessage());
            }
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

        // 添加狙击手职业
        Job sniperJob = new Job(
                31, // 选择一个未被使用的slot，比如31
                "§6狙击手",
                Material.CROSSBOW,
                Arrays.asList(
                        "",
                        "§7- 永久夜视",
                        "§7- 快速装填V穿透III耐久III弩",
                        "§7- 16个面包",
                        "§7- 1个望远镜"),
                player -> {
                    // 添加夜视效果 (180分钟 = 180 * 60 * 20 ticks)
                    PotionEffect nightVision = new PotionEffect(
                            PotionEffectType.NIGHT_VISION, 180 * 60 * 20, 0);
                    player.addPotionEffect(nightVision);
                    
                    // 创建附魔弩
                    ItemStack crossbow = new ItemStack(Material.CROSSBOW);
                    ItemMeta crossbowMeta = crossbow.getItemMeta();
                    crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 5, true);
                    crossbowMeta.addEnchant(Enchantment.PIERCING, 3, true);
                    crossbowMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
                    crossbowMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
                    crossbow.setItemMeta(crossbowMeta);
                    
                    // 给予物品
                    player.getInventory().addItem(crossbow);
                    player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
                    player.getInventory().addItem(new ItemStack(Material.SPYGLASS, 1));
                    
                    player.sendMessage("§6你选择了狙击手职业！");
                });
        jobs.put(sniperJob.getMenuSlot(), sniperJob);

        Job giantJob = new Job(
            33, // slot 33
            "§c巨人", 
            Material.NETHERITE_CHESTPLATE,
            Arrays.asList(
                    "",
                    "§7开局获得：",
                    "§7- 缓慢II效果(120秒)",
                    "§7- 永久火焰保护效果",
                    "§7- 火焰保护5耐久X下界合金套",
                    "§7- 头盔：水下呼吸3，水下速掘",
                    "§7- 护腿：巨人化IV，跳跃提升III",
                    "§7- 靴子：深海探索者1，摔落保护X"),
            player -> {
                // 添加缓慢II效果 (120秒 = 120 * 20 ticks)
                PotionEffect slowness = new PotionEffect(
                        PotionEffectType.SLOWNESS, 120 * 20, 1);
                player.addPotionEffect(slowness);
                
                // 添加火焰保护效果 (120分钟 = 120 * 60 * 20 ticks)
                PotionEffect fireResistance = new PotionEffect(
                        PotionEffectType.FIRE_RESISTANCE, 120 * 60 * 20, 0);
                player.addPotionEffect(fireResistance);
                
                // 创建下界合金套装
                ItemStack[] armorPieces = {
                        createEnchantedArmor(Material.NETHERITE_HELMET, "头盔"),
                        createEnchantedArmor(Material.NETHERITE_CHESTPLATE, "胸甲"), 
                        createEnchantedArmor(Material.NETHERITE_LEGGINGS, "护腿"),
                        createEnchantedArmor(Material.NETHERITE_BOOTS, "靴子")
                };
                
                // 给玩家穿戴装备
                PlayerInventory inventory = player.getInventory();
                inventory.setHelmet(armorPieces[0]);
                inventory.setChestplate(armorPieces[1]);
                inventory.setLeggings(armorPieces[2]);
                inventory.setBoots(armorPieces[3]);
                
                player.sendMessage("§c你选择了巨人职业！");
            });
            
    jobs.put(giantJob.getMenuSlot(), giantJob);
        // 添加午马骑士职业
Job horseKnightJob = new Job(
    34, // 使用 slot 34
    "§6午马骑士", 
    Material.NETHERITE_HORSE_ARMOR,
    Arrays.asList(
            "",
            "§7开局获得：",
            "§7- 10分钟速度Ⅰ效果",
            "§7- 突进Ⅲ铁长矛",
            "§7- 鞍 ×1",
            "§7- 苹果×6，面包×16，河豚×6",
            "§7- 下界合金马铠×1",
            "§7- 下界合金鹦鹉螺铠×1"),
    player -> {
        // 添加速度Ⅰ效果 (10分钟)
        PotionEffect speed = new PotionEffect(
                PotionEffectType.SPEED, 10 * 60 * 20, 0);
        player.addPotionEffect(speed);
        
        // 创建突进Ⅲ铁长矛
        ItemStack ironSpear = new ItemStack(Material.IRON_SPEAR);
        ItemMeta spearMeta = ironSpear.getItemMeta();
        if (spearMeta != null) {
            // 添加突进Ⅲ附魔
            Enchantment lungeEnchant = Enchantment.getByKey(NamespacedKey.minecraft("lunge"));
            if (lungeEnchant != null) {
                spearMeta.addEnchant(lungeEnchant, 3, true);
            }
            spearMeta.setDisplayName("§6骑士长矛");
            ironSpear.setItemMeta(spearMeta);
        }
        
        // 给予所有物品
        player.getInventory().addItem(ironSpear);
        player.getInventory().addItem(new ItemStack(Material.SADDLE, 1));
        player.getInventory().addItem(new ItemStack(Material.APPLE, 6));
        player.getInventory().addItem(new ItemStack(Material.BREAD, 16));
        player.getInventory().addItem(new ItemStack(Material.PUFFERFISH, 6));
        player.getInventory().addItem(new ItemStack(Material.NETHERITE_HORSE_ARMOR, 1));
        player.getInventory().addItem(new ItemStack(Material.NETHERITE_NAUTILUS_ARMOR, 1));
        
        player.sendMessage("§6你选择了午马骑士职业！");
    });
jobs.put(horseKnightJob.getMenuSlot(), horseKnightJob);

        // --- 要添加新职业？只需要在这里添加一个新的 Job 实例！ ---
        // 例如，添加一个"矿工"职业
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