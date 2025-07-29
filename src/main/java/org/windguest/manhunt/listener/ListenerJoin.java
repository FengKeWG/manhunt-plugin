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
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.game.Teleport;
import org.windguest.manhunt.menus.ManhuntJoinMenu;
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
        if (Game.getCurrentState() == Game.GameState.WAITING
                || Game.getCurrentState() == Game.GameState.COUNTDOWN_STARTED) {
            if (org.windguest.manhunt.world.ChunkyManager.isMaintenanceWindow()) {
                player.kickPlayer("服务器凌晨地图预生成中，请 07:00 后再加入！");
                return;
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> RulesMenu.open(player), 20L);
            World hub = Bukkit.getWorld("hub");
            if (hub != null) {
                Location hubLocation = new Location(hub, 0.5, 81.0, 0.5);
                player.teleport(hubLocation);
                player.setGameMode(GameMode.ADVENTURE);
                Compass.giveHubCompass(player);
                player.setInvulnerable(true);
            }
            if (Game.getCurrentState() == Game.GameState.WAITING && Bukkit.getOnlinePlayers().size() >= 2) {
                Game.startWaitingCountdown();
                Mode.startVoting();
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
                return;
            }

            // 新玩家首次加入，提供观战/加入菜单
            player.setInvisible(true);
            player.setInvulnerable(true);
            if (Mode.getCurrentMode() == Mode.GameMode.TEAM || Mode.getCurrentMode() == Mode.GameMode.END) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> PlaySelectionMenu.open(player), 5L);
            } else if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
                Team hunterTeam = TeamsManager.getTeamByName("猎杀者");
                Team runnerTeam = TeamsManager.getTeamByName("逃生者");
                if (hunterTeam != null && runnerTeam != null) {
                    int hunterCount = hunterTeam.getPlayerCount();
                    int runnerCount = runnerTeam.getPlayerCount();
                    if (runnerCount == 0) {
                        Bukkit.getScheduler().runTaskLater(plugin, () -> ManhuntJoinMenu.open(player, "逃生者"), 5L);
                    } else {
                        double ratio = (double) hunterCount / runnerCount;
                        String suggestion = (ratio > 2.0) ? "逃生者" : "猎杀者";
                        Bukkit.getScheduler().runTaskLater(plugin, () -> ManhuntJoinMenu.open(player, suggestion), 5L);
                    }
                }
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
            if (Game.getGameElapsedTime() > 30 * 60) {
                player.sendMessage("§7[🚫] 游戏已经进行了超过30分钟！你现在是旁观者");
                player.setGameMode(GameMode.SPECTATOR);
                Teleport.teleportToRandomTeamPlayer(player, null);
                return;
            }

            // 根据游戏模式处理
            Mode.GameMode currentMode = Mode.getCurrentMode();
            if (currentMode == Mode.GameMode.TEAM || currentMode == Mode.GameMode.END) {
                // 团队模式，打开通用选择菜单
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    PlaySelectionMenu.open(player);
                }, 5L);
            } else if (currentMode == Mode.GameMode.MANHUNT) {
                // 猎人模式，根据比例决定可加入的阵营
                Team hunterTeam = TeamsManager.getTeamByName("猎杀者");
                Team runnerTeam = TeamsManager.getTeamByName("逃生者");

                if (hunterTeam == null || runnerTeam == null)
                    return; // 安全检查

                int hunterCount = hunterTeam.getPlayerCount();
                int runnerCount = runnerTeam.getPlayerCount();

                // 防止除零
                if (runnerCount == 0) {
                    // 如果没有逃生者，必须加入逃生者
                    Bukkit.getScheduler().runTaskLater(plugin, () -> ManhuntJoinMenu.open(player, "逃生者"), 5L);
                    return;
                }

                double ratio = (double) hunterCount / runnerCount;
                String joinableTeam = (ratio > 2.0) ? "逃生者" : "猎人";

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setInvisible(true);
                    player.setInvulnerable(true);
                    ManhuntJoinMenu.open(player, joinableTeam);
                }, 5L);
            }
        }
    }
}
