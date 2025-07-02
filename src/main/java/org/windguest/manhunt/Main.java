package org.windguest.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.windguest.manhunt.commands.ShoutCommand;
import org.windguest.manhunt.listener.ListenerJoin;
import org.windguest.manhunt.listener.ListenerWorld;

public final class Main extends JavaPlugin {

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        ShoutCommand shoutCommand = new ShoutCommand();
        this.getCommand("s").setExecutor(shoutCommand);
        Bukkit.getPluginManager().registerEvents(new ListenerJoin(), this);
        Bukkit.getPluginManager().registerEvents(new ListenerWorld(), this);
    }

    @Override
    public void onDisable() {
    }
}
