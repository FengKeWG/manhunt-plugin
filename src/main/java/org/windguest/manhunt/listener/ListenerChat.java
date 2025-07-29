package org.windguest.manhunt.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;

import java.util.HashSet;
import java.util.Set;

public class ListenerChat implements Listener {

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        Team team = TeamsManager.getPlayerTeam(sender);
        String message = event.getMessage();

        String prefix;
        if (team == null) { // 旁观者
            prefix = "§7🚫";
            event.setFormat(prefix + " " + sender.getName() + "§f: " + message);
            // 旁观者消息全员可见
            return;
        }

        prefix = team.getColorString() + team.getIcon();
        event.setFormat(prefix + " " + sender.getName() + "§f: " + message);

        // 队伍聊天：屏蔽敌对队伍
        Set<Player> recipients = new HashSet<>(Bukkit.getOnlinePlayers());
        recipients.removeAll(team.getOpponent().getPlayers());
        event.getRecipients().clear();
        event.getRecipients().addAll(recipients);
    }
}
