package com.helveticraft.helveticraftplugin;

import com.HelvetiCraft.commands.*;
import com.HelvetiCraft.initiatives.InitiativeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private InitiativeManager initiativeManager;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Initialize initiative manager
        initiativeManager = new InitiativeManager(this);

        // Register commands with executors
        getCommand("initiative").setExecutor(new InitiativeCommand(initiativeManager));
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("status").setExecutor(new StatusCommand());
        getCommand("helveticraft").setExecutor(new HelveticraftCommand());

        // Register listeners (GUI handling lives inside InitiativeManager)
        getServer().getPluginManager().registerEvents(initiativeManager, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }

    public InitiativeManager getInitiativeManager() {
        return initiativeManager;
    }
}
