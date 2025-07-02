package org.windguest.manhunt.game;

import org.bukkit.entity.Player;
import org.windguest.manhunt.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Mode {
    private static final Main plugin = Main.getInstance();
    private static final Random rand = new Random();
    private static final Map<Player, GameMode> gamemodePreferences = new HashMap<>();
    private static GameMode currentMode = null;

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

    public enum GameMode {
        MANHUNT,
        TEAM,
        END,
    }
}
