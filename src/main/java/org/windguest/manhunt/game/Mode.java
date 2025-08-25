package org.windguest.manhunt.game;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.windguest.manhunt.Main;
import org.windguest.manhunt.teams.TeamsManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class Mode {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static final Map<Player, GameMode> gamemodePreferences = new HashMap<>();
    private static GameMode currentMode = null;
    private static boolean isVotingStarted = false;

    public static GameMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(GameMode currentMode) {
        Mode.currentMode = currentMode;
    }

    public static Map<Player, GameMode> getPreferences() {
        return gamemodePreferences;
    }

    public static void setPreference(Player player, GameMode mode) {
        gamemodePreferences.put(player, mode);
    }

    public static void startVoting() {
        if (isVotingStarted) {
            return;
        }
        isVotingStarted = true;
        new BukkitRunnable() {
            int time = 60;

            @Override
            public void run() {
                if (time == 60 || time == 30 || time == 10 || time <= 5 && time > 0) {
                    Bukkit.broadcastMessage("§e[!] 游戏模式投票还剩 " + time + " 秒！");
                    Bukkit.getOnlinePlayers()
                            .forEach(p -> p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f));
                }
                if (time <= 0) {
                    this.cancel();
                    calculateWinner();
                    isVotingStarted = false;
                }
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static void calculateWinner() {
        if (gamemodePreferences.isEmpty()) {
            currentMode = GameMode.values()[rand.nextInt(GameMode.values().length)];
            if (currentMode == GameMode.END) {
                currentMode = GameMode.TEAM;
            }
            Bukkit.broadcastMessage("§e[⚠] 没有玩家投票，随机选择模式：" + getModeName(currentMode));
        } else {
            Map<GameMode, Long> votes = gamemodePreferences.values().stream()
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            long maxVotes = 0;
            for (Long voteCount : votes.values()) {
                if (voteCount > maxVotes) {
                    maxVotes = voteCount;
                }
            }
            long finalMaxVotes = maxVotes;
            java.util.List<GameMode> winners = votes.entrySet().stream()
                    .filter(entry -> entry.getValue() == finalMaxVotes).map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            currentMode = winners.get(rand.nextInt(winners.size()));
            Bukkit.broadcastMessage("§a[✔] 投票结束！最终模式为：" + getModeName(currentMode));
        }

        TeamsManager.startPrefVoting();
    }

    public static String getModeName(GameMode mode) {
        switch (mode) {
            case MANHUNT:
                return "§a追杀模式";
            case TEAM:
                return "§b团队模式";
            case END:
                return "§d浑沌末地";
            default:
                return "§7未开始";
        }
    }

    public enum GameMode {
        MANHUNT, TEAM, END,
    }
}
