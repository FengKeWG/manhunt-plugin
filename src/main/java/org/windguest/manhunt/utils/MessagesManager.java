package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;

import java.util.List;

public class MessagesManager {
    private static final Main plugin = Main.getInstance();
    private static final List<String> messages = java.util.List.of(
            "§e[🔔] 默认聊天为队伍聊天，你可以使用 /s <内容> 进行喊话",
            "§e[🔔] 如果 50 格内有敌人，你不能打开共享背包或传送菜单",
            "§e[🔔] 游戏开始 30 分钟后，将不允许新玩家加入",
            "§e[🔔] 旁观者可以看到所有人的聊天，旁观者的聊天所有人也看得到",
            "§e[🔔] 服务器的游戏版本为 1.21.7",
            "§e[🔔] 游戏正在测试中！有问题请进群反馈！743361976",
            "§e[🔔] 建议进群下载汉化材质包游玩！743361976");
    private static int currentMessageIndex = 0;

    public static void startScheduledMessages() {
        long delay = 0L;
        long period = 3 * 60 * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (messages.isEmpty())
                return;
            String message = messages.get(currentMessageIndex);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            currentMessageIndex = (currentMessageIndex + 1) % messages.size();
        }, delay, period);
    }
}
