package com.HelvetiCraft;

import com.HelvetiCraft.commands.*;
import com.HelvetiCraft.expansions.FinanceExpansion;
import com.HelvetiCraft.expansions.InitiativeExpansion;
import com.HelvetiCraft.initiatives.InitiativeManager;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.finance.FinanceJoinListener;
import com.HelvetiCraft.economy.VaultEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private InitiativeManager initiativeManager;
    private FinanceManager financeManager;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Initiative manager
        initiativeManager = new InitiativeManager(this);

        // Register PlaceholderAPI expansion
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new InitiativeExpansion().register(); // <-- no args needed
            getLogger().info("InitiativeExpansion placeholders registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Initiative placeholders will not work.");
        }

        // Register commands
        getCommand("initiative").setExecutor(new InitiativeCommand(initiativeManager));
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("status").setExecutor(new StatusCommand(this));
        getCommand("helveticraft").setExecutor(new HelveticraftCommand(this));

        // Finance manager
        financeManager = new FinanceManager(this);
        new FinanceExpansion(financeManager).register();
        getLogger().info("FinanceExpansion placeholders registered!");

        // Finance commands
        getCommand("finance").setExecutor(new FinanceCommand(financeManager));
        getCommand("networth").setExecutor(new NetworthCommand(financeManager));
        getCommand("pay").setExecutor(new PayCommand(financeManager));
        SellCommand sell = new SellCommand(this, financeManager);
        getCommand("sell").setExecutor(sell);
        getCommand("sellaccept").setExecutor(sell);
        getCommand("selldecline").setExecutor(sell);
        getCommand("save").setExecutor(new SaveCommand(financeManager));

        // Listeners
        getServer().getPluginManager().registerEvents(initiativeManager, this);
        getServer().getPluginManager().registerEvents(new FinanceJoinListener(financeManager), this);

        // Vault economy bridge
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Bukkit.getServicesManager().register(
                    net.milkbowl.vault.economy.Economy.class,
                    new VaultEconomyBridge(financeManager),
                    this,
                    ServicePriority.High
            );
            getLogger().info("Vault economy bridge registered!");
        } else {
            getLogger().warning("Vault not found! Economy plugins like ChestShop will not work.");
        }
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
