package com.helveticraft.helveticraftplugin;

import org.bukkit.plugin.java.JavaPlugin;


public class Main extends JavaPlugin {

    @Override
    public void onEnable() {
        // Called when the plugin is enabled
        getLogger().info("HelvetiCraft Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Called when the plugin is disabled
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }
}
