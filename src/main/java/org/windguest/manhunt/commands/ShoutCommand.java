package org.windguest.manhunt.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;

public class ShoutCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player player = (Player) sender;
        Team team = TeamsManager.getPlayerTeam(player);
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.sendMessage(ChatColor.RED + "只有游戏玩家才可以喊话！");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "使用方法: /s <消息内容>");
            return true;
        }
        String message = String.join(" ", args);
        String formattedMessage = ChatColor.YELLOW + "[喊话] " + team.getColorString() + team.getIcon() + " " + player.getName() + ChatColor.WHITE + ": " + message;
        Bukkit.broadcastMessage(formattedMessage);
        return true;
    }

}

