package org.windguest.manhunt.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.StructureType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.game.Game;
import org.windguest.manhunt.game.Mode;
import org.windguest.manhunt.teams.Team;
import org.windguest.manhunt.teams.TeamsManager;
import org.windguest.manhunt.world.StructureManager;

import java.text.DecimalFormat;

public class Placeholder extends PlaceholderExpansion {

    private static final Main plugin = Main.getInstance();
    private static final DecimalFormat df = new DecimalFormat("0.##");

    public @NotNull String getAuthor() {
        return "WindGuest";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ManHunt";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // General placeholders
        switch (params) {
            case "gamestate":
                return Game.getCurrentState().name();
            case "mode":
                Mode.GameMode cm = Mode.getCurrentMode();
                return cm != null ? Mode.getModeName(cm) : "§7未开始";
            case "countdown":
                if (Game.getCurrentState() == Game.GameState.COUNTDOWN_STARTED) {
                    return "将在 §a" + Game.getCountdown() + " §f秒后开始";
                }
                return "等待玩家";
            case "elapsed_time":
                long elapsedTime = Game.getGameElapsedTime();
                long minutes = elapsedTime / 60L;
                long seconds = elapsedTime % 60L;
                return String.format("%d分%d秒", minutes, seconds);
            case "team":
                Team team = TeamsManager.getPlayerTeam(player);
                return team != null ? team.getDisplayName() : "§7旁观者";
            case "icon":
                Team pTeam = TeamsManager.getPlayerTeam(player);
                return pTeam != null ? pTeam.getColorString() + pTeam.getIcon() : "§7🚫";
            case "score":
                return String.format("%.1f", DataManager.calculateTotalPlayerScore(player));
            case "total_wins":
                return String.valueOf(getTotalStat(player, "wins"));
            case "total_games":
                return String.valueOf(getTotalStat(player, "games"));
            case "total_kills":
                return String.valueOf(getTotalStat(player, "kills"));
            case "total_deaths":
                return String.valueOf(getTotalStat(player, "deaths"));
            case "total_wr":
                return df.format(DataManager.getTotalPlayerWR(player));
            case "total_kdr":
                return df.format(DataManager.getTotalPlayerKDR(player));
        }

        // Team specific placeholders: manhunt_team_players_alive_红队
        if (params.startsWith("team_players_alive_")) {
            String teamName = params.substring("team_players_alive_".length());
            Team team = TeamsManager.getTeamByName(teamName);
            return team != null ? String.valueOf(team.getPlayerCount()) : "0";
        }
        if (params.startsWith("team_dragon_damage_")) {
            String teamName = params.substring("team_dragon_damage_".length());
            Team team = TeamsManager.getTeamByName(teamName);
            return team != null ? df.format(team.getDragonDamage()) : "0";
        }

        // Structure placeholders: manhunt_structure_stronghold
        if (params.startsWith("structure_")) {
            String structureName = params.substring("structure_".length()).toUpperCase();
            StructureType type = getStructureTypeByName(structureName);
            if (type == null) {
                return "Invalid Structure";
            }
            Location loc = StructureManager.getNearestStructure(type, player);
            return loc != null ? loc.getBlockX() + ", " + loc.getBlockZ() : "未找到";
        }

        // Per-mode (可含角色) 统计占位符
        // 用法:
        // stats_<mode>_<stat> -> team模式等
        // stats_manhunt_<role>_<stat> -> manhunt.runner.wins 等
        if (params.startsWith("stats_")) {
            String[] parts = params.substring("stats_".length()).split("_");
            if (parts.length == 2) {
                // 普通模式
                try {
                    Mode.GameMode mode = Mode.GameMode.valueOf(parts[0].toUpperCase());
                    String stat = parts[1];
                    return String.valueOf(DataManager.getPlayerData(player, mode, stat));
                } catch (IllegalArgumentException e) {
                    return "Invalid Mode";
                }
            } else if (parts.length == 3 && parts[0].equalsIgnoreCase("manhunt")) {
                String role = parts[1]; // runner / hunter
                String stat = parts[2];
                return String.valueOf(DataManager.getPlayerData(player, Mode.GameMode.MANHUNT, role, stat));
            }
        }

        return null;
    }

    private int getTotalStat(Player player, String key) {
        int total = 0;
        for (Mode.GameMode mode : Mode.GameMode.values()) {
            total += DataManager.getPlayerData(player, mode, key);
        }
        return total;
    }

    private @Nullable StructureType getStructureTypeByName(String name) {
        try {
            for (java.lang.reflect.Field field : StructureType.class.getFields()) {
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                        && field.getType() == StructureType.class) {
                    if (field.getName().equalsIgnoreCase(name)) {
                        return (StructureType) field.get(null);
                    }
                }
            }
        } catch (IllegalAccessException ignored) {
        }
        return null;
    }
}
