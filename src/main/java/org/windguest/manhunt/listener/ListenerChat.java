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
        String playerIcon = team.getIcon();
        String message = event.getMessage();
        event.setFormat(playerIcon + " " + sender.getName() + "§f: " + message);
        Set<Player> recipients = new HashSet<>(Bukkit.getOnlinePlayers());
        recipients.removeAll(team.getOpponent().getPlayers());
        event.getRecipients().clear();
        event.getRecipients().addAll(recipients);
    }
}
