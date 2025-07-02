package org.windguest.manhunt.menus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class RulesMenu {

    public static void open(Player player) {
        Inventory menu = Bukkit.createInventory(null, 36, "游戏规则");
        ItemStack hunterItem = new ItemStack(Material.DRAGON_EGG);
        ItemMeta hunterMeta = hunterItem.getItemMeta();
        if (hunterMeta != null) {
            hunterMeta.setDisplayName("§a获胜条件");
            ArrayList<String> hunterLore = new ArrayList<>();
            hunterLore.add("§7方式① 击杀全部敌方玩家即可获胜");
            hunterLore.add("§7方式② 末影龙死亡，并且你的队伍对末影龙造");
            hunterLore.add("§7造成的伤害 > 另一个队伍");
            hunterMeta.setLore(hunterLore);
            hunterItem.setItemMeta(hunterMeta);
        }
        menu.setItem(11, hunterItem);
        ItemStack runnerItem = new ItemStack(Material.BOW);
        ItemMeta runnerMeta = runnerItem.getItemMeta();
        if (runnerMeta != null) {
            runnerMeta.setDisplayName("§a规则");
            ArrayList<String> runnerLore = new ArrayList<>();
            runnerLore.add("§7游戏开始后同队伍玩家出生在一起");
            runnerLore.add("§7两个队伍初始相隔300格左右");
            runnerLore.add("§7玩家死亡后不可以复活，变为旁观者");
            runnerMeta.setLore(runnerLore);
            runnerItem.setItemMeta(runnerMeta);
        }
        menu.setItem(12, runnerItem);
        ItemStack teleportItem = new ItemStack(Material.COMPASS);
        ItemMeta teleportMeta = teleportItem.getItemMeta();
        if (teleportMeta != null) {
            teleportMeta.setDisplayName("§a传送");
            ArrayList<String> teleportLore = new ArrayList<>();
            teleportLore.add("§7左键指南针打开传送菜单");
            teleportLore.add("§7消耗19点血量进行传送");
            teleportLore.add("§7传送后的玩家短暂获得DEBUFF");
            teleportLore.add("§7若有队友进入过末地，则可以直接传送至末地");
            teleportMeta.setLore(teleportLore);
            teleportItem.setItemMeta(teleportMeta);
        }
        menu.setItem(13, teleportItem);
        ItemStack sharedChestItem = new ItemStack(Material.ENDER_CHEST);
        ItemMeta sharedChestMeta = sharedChestItem.getItemMeta();
        if (sharedChestMeta != null) {
            sharedChestMeta.setDisplayName("§a共享背包");
            ArrayList<String> sharedChestLore = new ArrayList<>();
            sharedChestLore.add("§7右键指南针打开共享背包");
            sharedChestLore.add("§7同队伍的玩家共用一个共享背包");
            sharedChestLore.add("§7若附近50格有玩家不能打开");
            sharedChestMeta.setLore(sharedChestLore);
            sharedChestItem.setItemMeta(sharedChestMeta);
        }
        menu.setItem(14, sharedChestItem);
        ItemStack netherStarItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta netherStarMeta = netherStarItem.getItemMeta();
        if (netherStarMeta != null) {
            netherStarMeta.setDisplayName("§a末地和凋灵");
            ArrayList<String> netherStarLore = new ArrayList<>();
            netherStarLore.add("§7玩家进入末地出生在外岛");
            netherStarLore.add("§7并获得低耐久鞘翅和烟花火箭");
            netherStarLore.add("§7右键下界之星可以获得永久药效");
            netherStarLore.add("§7依次为：生命恢复Ⅱ,速度Ⅱ,急迫Ⅱ,抗性提升Ⅰ,力量Ⅱ");
            netherStarMeta.setLore(netherStarLore);
            netherStarItem.setItemMeta(netherStarMeta);
        }
        menu.setItem(15, netherStarItem);
        ItemStack datapackItem = new ItemStack(Material.OAK_SAPLING);
        ItemMeta datapackMeta = datapackItem.getItemMeta();
        if (datapackMeta != null) {
            datapackMeta.setDisplayName("§a数据包");
            ArrayList<String> datapackLore = new ArrayList<>();
            datapackLore.add("§7服务器添加了大量自定义数据包");
            datapackLore.add("§7修改了地形的生成和结构");
            datapackLore.add("§7新增了大约500种新结构");
            datapackMeta.setLore(datapackLore);
            datapackItem.setItemMeta(datapackMeta);
        }
        menu.setItem(16, datapackItem);
        ItemStack dataModItem = new ItemStack(Material.BOOK);
        ItemMeta dataModMeta = dataModItem.getItemMeta();
        if (dataModMeta != null) {
            dataModMeta.setDisplayName("§a数据修改");
            ArrayList<String> dataModLore = new ArrayList<>();
            dataModLore.add("§71. 末影龙血量修改为 500");
            dataModLore.add("§72. 猪灵交易仅可以获得黑曜石、抗火药水和末影珍珠");
            dataModLore.add("§73. 击杀凋零骷髅 100% 掉落头颅");
            dataModLore.add("§74. 击杀烈焰人 100% 掉落烈焰棒");
            dataModLore.add("§75. 击杀末影人 100% 掉落末影珍珠");
            dataModLore.add("§76. 结构中的宝箱物品爆率大幅增大");
            dataModLore.add("§77. 玩家不允许在特定世界使用床、末影水晶和重生锚");
            dataModMeta.setLore(dataModLore);
            dataModItem.setItemMeta(dataModMeta);
        }
        menu.setItem(10, dataModItem);
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName("§c关闭");
            closeItem.setItemMeta(closeMeta);
        }
        menu.setItem(31, closeItem);
        player.openInventory(menu);
    }
}
