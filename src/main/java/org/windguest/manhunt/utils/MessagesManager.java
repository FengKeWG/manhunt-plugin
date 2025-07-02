package org.windguest.manhunt.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;

import java.util.List;

public class MessagesManager {
    private static final Main plugin = Main.getInstance();
    private static List<String> messages;
    private static int currentMessageIndex = 0;

    public static void startScheduledMessages() {
        long delay = 0L;
        long period = 3 * 60 * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            if (messages.isEmpty()) return;
            String message = messages.get(currentMessageIndex);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
            currentMessageIndex = (currentMessageIndex + 1) % messages.size();
        }, delay, period);
    }
}
