package com.helveticraft.helveticraftplugin;

import com.HelvetiCraft.commands.*;
import com.HelvetiCraft.expansions.FinanceExpansion;
import com.HelvetiCraft.initiatives.InitiativeManager;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.finance.FinanceJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private InitiativeManager initiativeManager;
    private FinanceManager financeManager;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Initialize initiative manager
        initiativeManager = new InitiativeManager(this);

        // Register commands with executors
        getCommand("initiative").setExecutor(new InitiativeCommand(initiativeManager));
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("status").setExecutor(new StatusCommand(this));
        getCommand("helveticraft").setExecutor(new HelveticraftCommand(this));

        // Finance manager (initialize before registering finance commands/listeners)
        financeManager = new FinanceManager(this);
        new FinanceExpansion(financeManager).register();
        getLogger().info("FinanceExpansion placeholders registered!");

        // Finance commands
        getCommand("finance").setExecutor(new FinanceCommand(financeManager));
        getCommand("networth").setExecutor(new NetworthCommand(financeManager));
        getCommand("pay").setExecutor(new PayCommand(financeManager));
        getCommand("sell").setExecutor(new SellCommand(this, financeManager));


        // Register listeners (GUI handling lives inside InitiativeManager)
        getServer().getPluginManager().registerEvents(initiativeManager, this);
    getServer().getPluginManager().registerEvents(new FinanceJoinListener(financeManager), this);

    }

    @Override
    public void onDisable() {
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }

    public InitiativeManager getInitiativeManager() {
        return initiativeManager;
    }

    public FinanceManager getFinanceManager() {
        return financeManager;
    }

}
