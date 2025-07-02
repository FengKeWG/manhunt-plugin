package org.windguest.manhunt.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.game.Compass;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Teleport;
import org.windguest.manhunt.menus.PlaySelectionMenu;
import org.windguest.manhunt.menus.RulesMenu;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;

public class ListenerJoin implements Listener {
    private static final Main plugin = Main.getInstance();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        DataManager.createPlayerFileIfNotExists(player);
        event.joinMessage(Component.text("[+] ", NamedTextColor.GREEN)
                .append(Component.text(playerName, NamedTextColor.GREEN)));
        if (Game.getCurrentState() == Game.GameState.WAITING) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> RulesMenu.open(player), 20L);
            World hub = Bukkit.getWorld("hub");
            if (hub != null) {
                Location hubLocation = new Location(hub, 0.5, 81.0, 0.5);
                player.teleport(hubLocation);
                player.setGameMode(GameMode.ADVENTURE);
                Compass.giveHubCompass(player);
                player.setInvulnerable(true);
            }
            if (Bukkit.getOnlinePlayers().size() >= 2) {
                Game.startWaitingCountdown();
            }
        } else if (Game.getCurrentState() == Game.GameState.FROZEN) {
            Team quitTeam = TeamsManager.getQuitTeam(player);
            if (quitTeam != null) {
                quitTeam.addPlayer(player);
                quitTeam.sendBackMessage(player);
                TeamsManager.removeQuit(player);
                player.setGameMode(GameMode.ADVENTURE);
                player.setInvulnerable(true);
                Teleport.teleportToRandomTeamPlayer(player, quitTeam);
            }
        } else if (Game.getCurrentState() == Game.GameState.RUNNING) {
            Team quitTeam = TeamsManager.getQuitTeam(player);
            if (quitTeam != null) {
                quitTeam.addPlayer(player);
                quitTeam.sendBackMessage(player);
                TeamsManager.removeQuit(player);
                player.setGameMode(GameMode.SURVIVAL);
                Compass.giveGameCompass(player);
                player.setInvulnerable(false);
                return;
            }
            Team diedTeam = TeamsManager.getDeadTeam(player);
            if (diedTeam != null) {
                player.sendMessage("§7[🚫] 你已经死亡！你现在是旁观者");
                player.setInvisible(false);
                player.setInvulnerable(false);
                player.setGameMode(GameMode.SPECTATOR);
                Teleport.teleportToRandomTeamPlayer(player, null);
                return;
            }
            if (Game.getGameElapsedTime() <= 20 * 60 * 30) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    PlaySelectionMenu.open(player);
                }, 5L);
            } else {
                player.sendMessage("§7[🚫] 游戏已经进行了超过30分钟！你现在是旁观者");
                player.setInvisible(false);
                player.setInvulnerable(false);
                player.setGameMode(GameMode.SPECTATOR);
                Teleport.teleportToRandomTeamPlayer(player, null);
            }
        }
    }
}
