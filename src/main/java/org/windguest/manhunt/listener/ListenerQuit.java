package org.windguest.manhunt.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
//import org.windguest.manhunt.utils.CombatManager;

public class ListenerQuit implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        event.quitMessage(
                Component.text("[-] ", NamedTextColor.RED).append(Component.text(playerName, NamedTextColor.RED)));
        Team team = TeamsManager.getPlayerTeam(player);

        if (team != null) {
            team.removePlayer(player);
            // 将玩家记入 quit 表，以便重新加入时自动回到原队伍
            TeamsManager.setQuit(player, team);
            if (team.isEmpty()) {
                Game.endGame(team.getOpponent());
            }
        }
    }
}
