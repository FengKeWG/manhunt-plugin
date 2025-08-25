package org.windguest.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.windguest.manhunt.commands.ShoutCommand;
import org.windguest.manhunt.files.DataManager;
import org.windguest.manhunt.jobs.JobsManager;
import org.windguest.manhunt.listener.*;
import org.windguest.manhunt.placeholder.Placeholder;
import org.windguest.manhunt.utils.MessagesManager;
import org.windguest.manhunt.world.ChunkyManager;
import org.windguest.manhunt.world.StructureManager;
import org.windguest.manhunt.world.WorldManager;

public final class Main extends JavaPlugin {

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        DataManager.createUsersFolder();
        JobsManager.initializeJobs();
        ShoutCommand shoutCommand = new ShoutCommand();
        this.getCommand("s").setExecutor(shoutCommand);

        WorldManager worldManager = new WorldManager();
        worldManager.loadWorld();

        MessagesManager.startScheduledMessages();

        StructureManager.init();
        org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ListenerJoin(), this);
        pm.registerEvents(new ListenerQuit(), this);
        pm.registerEvents(new ListenerChat(), this);
        pm.registerEvents(new ListenerMove(), this);
        pm.registerEvents(new ListenerPlayers(), this);
        pm.registerEvents(new ListenerBlock(), this);
        pm.registerEvents(new ListenerDamage(), this);
        pm.registerEvents(new ListenerDeath(), this);
        pm.registerEvents(new ListenerInteract(), this);
        pm.registerEvents(new ListenerInventory(), this);
        pm.registerEvents(new ListenerPortal(), this);
        pm.registerEvents(new ListenerWorld(), this);
        // 初始化 ChunkyManager
        ChunkyManager.initialize();
        WorldManager.getNearestNonOceanBiomeLocation();
        /*
         * 以下Chunky预生成注释掉了
         * 需要的自己配置
         */
        // if (ChunkyManager.isMaintenanceWindow()) {
        // // 维护时段：不允许启动游戏，开始预生成
        // ChunkyManager.runStartCommand();
        // WorldManager.getNearestNonOceanBiomeLocation();
        // } else {
        // // 非维护时段：尝试加载已预生成地图
        // if (!ChunkyManager.swapInPreGeneratedWorldIfExists()) {
        // WorldManager.getNearestNonOceanBiomeLocation();
        // }
        // }
        new Placeholder().register();
    }

    @Override
    public void onDisable() {
    }
}
