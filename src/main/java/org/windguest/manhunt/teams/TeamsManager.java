package org.windguest.manhunt.teams;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.game.Mode;

import java.util.*;

public class TeamsManager {
    private static final Main plugin = Main.getInstance();
    private static final Map<UUID, Team> quit = new HashMap<>();
    private static final Map<UUID, Team> dead = new HashMap<>();
    private static final Set<Team> teams = new HashSet<>();
    private static final Map<Player, TeamPreference> teamPreferences = new HashMap<>();
    private static boolean isPrefVotingStarted = false;

    public enum TeamPreference {
        RED, BLUE, RUNNER, HUNTER, NONE
    }

    public static Map<Player, TeamPreference> getTeamPreferences() {
        return teamPreferences;
    }

    public static void setTeamPreference(Player player, TeamPreference preference) {
        teamPreferences.put(player, preference);
    }

    public static boolean isPrefVotingStarted() {
        return isPrefVotingStarted;
    }

    public static void startPrefVoting() {
        isPrefVotingStarted = true;
        new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                if (time == 60 || time == 30 || time == 10 || time <= 5 && time > 0) {
                    Bukkit.broadcastMessage("§e[!] 队伍倾向选择还剩 " + time + " 秒！");
                    Bukkit.getOnlinePlayers()
                            .forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f));
                }
                if (time <= 0) {
                    this.cancel();
                    isPrefVotingStarted = false;
                    Bukkit.broadcastMessage("§a[✔] 倾向选择结束！正在分配队伍...");
                }
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public static Set<Team> getTeams() {
        return teams;
    }

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

    public static void initializeTeams() {
        if (Mode.getCurrentMode() == Mode.GameMode.TEAM || Mode.getCurrentMode() == Mode.GameMode.END) {
            Team redTeam = new Team("红队", "§c", "🏹", Color.RED);
            Team blueTeam = new Team("蓝队", "§9", "⚔", Color.BLUE);
            teams.add(redTeam);
            teams.add(blueTeam);
            redTeam.opponent = blueTeam;
            blueTeam.opponent = redTeam;
        }
        if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
            Team runnerTeam = new Team("逃生者", "§a", "🐉", Color.GREEN);
            Team hunterTeam = new Team("猎杀者", "§c", "🏹", Color.RED);
            teams.add(runnerTeam);
            teams.add(hunterTeam);
            runnerTeam.opponent = hunterTeam;
            hunterTeam.opponent = runnerTeam;
        }
    }

    public static void assignTeams() {
        teams.clear();
        initializeTeams();

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(players);

        Map<Player, TeamPreference> prefMap = getTeamPreferences();

        if (Mode.getCurrentMode() == Mode.GameMode.TEAM || Mode.getCurrentMode() == Mode.GameMode.END) {
            Team red = getTeamByName("红队");
            Team blue = getTeamByName("蓝队");
            List<Player> redPref = new ArrayList<>();
            List<Player> bluePref = new ArrayList<>();
            List<Player> none = new ArrayList<>();
            for (Player p : players) {
                TeamPreference pref = prefMap.getOrDefault(p, TeamPreference.NONE);
                if (pref == TeamPreference.RED)
                    redPref.add(p);
                else if (pref == TeamPreference.BLUE)
                    bluePref.add(p);
                else
                    none.add(p);
            }

            boolean toRed = true;
            while (!redPref.isEmpty() || !bluePref.isEmpty() || !none.isEmpty()) {
                if (toRed) {
                    Player p = !redPref.isEmpty() ? redPref.remove(0)
                            : !none.isEmpty() ? none.remove(0) : bluePref.remove(0);
                    red.addPlayer(p);
                } else {
                    Player p = !bluePref.isEmpty() ? bluePref.remove(0)
                            : !none.isEmpty() ? none.remove(0) : redPref.remove(0);
                    blue.addPlayer(p);
                }
                toRed = !toRed;
            }
        } else if (Mode.getCurrentMode() == Mode.GameMode.MANHUNT) {
            Team runner = getTeamByName("逃生者");
            Team hunter = getTeamByName("猎杀者");

            List<Player> runPref = new ArrayList<>();
            List<Player> huntPref = new ArrayList<>();
            List<Player> none = new ArrayList<>();
            for (Player p : players) {
                TeamPreference pref = prefMap.getOrDefault(p, TeamPreference.NONE);
                if (pref == TeamPreference.RUNNER)
                    runPref.add(p);
                else if (pref == TeamPreference.HUNTER)
                    huntPref.add(p);
                else
                    none.add(p);
            }

            int totalPlayers = players.size();
            int desiredRunners = Math.max(1, Math.round(totalPlayers / 4.0f));

            while (runner.getPlayerCount() < desiredRunners && (!runPref.isEmpty() || !none.isEmpty())) {
                Player p = !runPref.isEmpty() ? runPref.remove(0) : none.remove(0);
                runner.addPlayer(p);
            }

            // 剩余全部加入猎杀者
            for (Player p : runPref)
                hunter.addPlayer(p);
            for (Player p : huntPref)
                hunter.addPlayer(p);
            for (Player p : none)
                hunter.addPlayer(p);
        }
    }

    public static Team getTeamByName(String name) {
        for (Team t : teams) {
            if (t.getName().equals(name))
                return t;
        }
        return null;
    }
}
