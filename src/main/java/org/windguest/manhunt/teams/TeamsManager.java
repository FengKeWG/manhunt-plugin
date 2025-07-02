package org.windguest.manhunt.teams;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Mode;

import java.util.*;

public class TeamsManager {
    private static final Main plugin = Main.getInstance();
    private static final Map<UUID, Team> quit = new HashMap<>();
    private static final Map<UUID, Team> dead = new HashMap<>();
    private static final Set<Team> teams = new HashSet<>();
    private static final Map<Player, Team> teamPreferences = new HashMap<>();

    public static Set<Player> getAllGamingPlayers() {
        Set<Player> allPlayers = new HashSet<>();
        for (Team team : teams) {
            allPlayers.addAll(team.getPlayers());
        }
        return allPlayers;
    }

    public static void setQuit(Player player, Team team) {
        quit.put(player.getUniqueId(), team);
    }

    public static Team getQuitTeam(Player player) {
        return quit.get(player.getUniqueId());
    }

    public static void removeQuit(Player player) {
        quit.remove(player.getUniqueId());
    }

    public static void setDead(Player player, Team team) {
        dead.put(player.getUniqueId(), team);
    }

    public static Team getDeadTeam(Player player) {
        return dead.get(player.getUniqueId());
    }

    public static void removeDead(Player player) {
        dead.remove(player.getUniqueId());
    }

    public static Team getTopDamageTeam() {
        Team topTeam = null;
        double maxDamage = Double.MIN_VALUE;
        for (Team team : teams) {
            if (team.getDragonDamage() > maxDamage) {
                maxDamage = team.getDragonDamage();
                topTeam = team;
            }
        }
        return topTeam;
    }

    public static boolean areSameTeam(Player p1, Player p2) {
        Team team1 = getPlayerTeam(p1);
        Team team2 = getPlayerTeam(p2);
        return team1 != null && team1.equals(team2);
    }

    public static Team getPlayerTeam(Player player) {
        for (Team team : teams) {
            if (team.hasPlayer(player)) {
                return team;
            }
        }
        return null;
    }

    void initializeTeams() {
        if (Mode.getCurrentMode() == Mode.GameMode.TEAM || Mode.getCurrentMode() == Mode.GameMode.END) {
            Team redTeam = new Team("红队", "§c", "🏹", Color.RED);
            Team blueTeam = new Team("蓝队", "§9", "⚔", Color.BLUE);
            teams.add(redTeam);
            teams.add(blueTeam);
        }
        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
            Team runnerTeam = new Team("逃生者", "§a", "🐉", Color.GREEN);
            Team hunterTeam = new Team("猎杀者", "§c", "🏹", Color.RED);
            teams.add(runnerTeam);
            teams.add(hunterTeam);
        }
    }

    private void assignTeams() {
        List<Player> allPlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        allPlayers.sort((p1, p2) -> Double.compare(calculatePlayerScore(p2), calculatePlayerScore(p1)));
        double redScore = 0;
        double blueScore = 0;
        for (Player player : allPlayers) {
            double playerScore = calculatePlayerScore(player);
            if (red.size() < blue.size()) {
                red.add(player);
                redScore += playerScore;
            } else if (blue.size() < red.size()) {
                blue.add(player);
                blueScore += playerScore;
            } else {
                if (redScore <= blueScore) {
                    red.add(player);
                    redScore += playerScore;
                } else {
                    blue.add(player);
                    blueScore += playerScore;
                }
            }
        }
    }
}
