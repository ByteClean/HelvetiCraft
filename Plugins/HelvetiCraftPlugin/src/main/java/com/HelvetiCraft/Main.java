package com.HelvetiCraft;

import com.HelvetiCraft.commands.*;
import com.HelvetiCraft.convert.ConvertManager;
import com.HelvetiCraft.expansions.FinanceExpansion;
import com.HelvetiCraft.expansions.InitiativeExpansion;
import com.HelvetiCraft.initiatives.InitiativeManager;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.Claims.ClaimManager;
import com.HelvetiCraft.commands.BuyClaimBlockCommand;
import com.HelvetiCraft.commands.SellClaimBlockCommand;
import com.HelvetiCraft.finance.FinanceJoinListener;
import com.HelvetiCraft.economy.VaultEconomyBridge;
import com.HelvetiCraft.requests.AdminRequests;
import com.HelvetiCraft.requests.FinanceRequests;
import com.HelvetiCraft.requests.TaxRequests;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {

    private InitiativeManager initiativeManager;
    private FinanceManager financeManager;
    private ClaimManager claimManager;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Initialize admin requests logger
        AdminRequests.init(this);

        // Initiative manager
        initiativeManager = new InitiativeManager(this);

        // Register InitiativeExpansion unconditionally
        new InitiativeExpansion().register();
        getLogger().info("InitiativeExpansion placeholders registered!");

        // Register commands
        getCommand("initiative").setExecutor(new InitiativeCommand(initiativeManager));
        getCommand("verify").setExecutor(new VerifyCommand(this));
        getCommand("status").setExecutor(new StatusCommand(this));
        getCommand("helveticraft").setExecutor(new HelveticraftCommand(this));
        // Admin command (uses Vault/LuckPerms for group management)
        getCommand("admin").setExecutor(new AdminCommand(this));

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

        // Convert Command
        ConvertManager convertManager = new ConvertManager(this, financeManager);
        getCommand("convert").setExecutor(new ConvertCommand(convertManager));

        // Claim block manager & commands
        claimManager = new ClaimManager(this, financeManager);
        getCommand("buyclaimblock").setExecutor(new BuyClaimBlockCommand(claimManager));
        getCommand("sellclaimblock").setExecutor(new SellClaimBlockCommand(claimManager));

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

        // Schedule periodic taxes (async for long periods)
        // 3 days for vermoegens and land
        long threeDaysSeconds = 3L * 24 * 3600;
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            runVermoegensSteuerCollection();
            runLandSteuerCollection();
        }, threeDaysSeconds, threeDaysSeconds, TimeUnit.SECONDS); // Start after 3 days, repeat every 3 days

        // 7 days for einkommen
        long sevenDaysSeconds = 7L * 24 * 3600;
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            runEinkommenSteuerCollection();
        }, sevenDaysSeconds, sevenDaysSeconds, TimeUnit.SECONDS);
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

    private void runVermoegensSteuerCollection() {
        long activeThreshold = System.currentTimeMillis() - 10L * 24 * 3600 * 1000; // 10 days ago
        for (UUID id : financeManager.getKnownPlayers()) {
            if (id.equals(ClaimManager.GOVERNMENT_UUID)) continue;
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            if (p.getLastLogin() < activeThreshold) continue; // Not active

            long wealth = financeManager.getMain(id) + financeManager.getSavings(id);
            long tax = TaxRequests.calculateVermoegensSteuer(wealth);
            if (tax <= 0) continue;

            if (financeManager.getMain(id) >= tax) {
                financeManager.addToMain(id, -tax);
                financeManager.addToMain(ClaimManager.GOVERNMENT_UUID, tax);
                Player online = p.getPlayer();
                if (online != null) {
                    online.sendMessage("§cVermögenssteuer abgezogen: §f" + FinanceManager.formatCents(tax));
                }
            } else {
                // Handle insufficient funds, e.g., set to debt or notify admin
                getLogger().warning("Player " + p.getName() + " has insufficient funds for Vermögenssteuer: " + tax);
            }
        }
    }

    private void runEinkommenSteuerCollection() {
        for (UUID id : financeManager.getKnownPlayers()) {
            if (id.equals(ClaimManager.GOVERNMENT_UUID)) continue;
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);

            long income = FinanceRequests.getPeriodIncome(id);
            long tax = TaxRequests.calculateEinkommenSteuer(income);
            if (tax <= 0) {
                FinanceRequests.resetPeriodIncome(id);
                continue;
            }

            if (financeManager.getMain(id) >= tax) {
                financeManager.addToMain(id, -tax);
                financeManager.addToMain(ClaimManager.GOVERNMENT_UUID, tax);
                Player online = p.getPlayer();
                if (online != null) {
                    online.sendMessage("§cEinkommensteuer abgezogen: §f" + FinanceManager.formatCents(tax));
                }
                FinanceRequests.resetPeriodIncome(id);
            } else {
                // Handle insufficient, perhaps carry over
                getLogger().warning("Player " + p.getName() + " has insufficient funds for Einkommensteuer: " + tax);
            }
        }
    }

    private void runLandSteuerCollection() {
        long activeThreshold = System.currentTimeMillis() - 10L * 24 * 3600 * 1000; // 10 days ago
        for (UUID id : financeManager.getKnownPlayers()) {
            if (id.equals(ClaimManager.GOVERNMENT_UUID)) continue;
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            if (p.getLastLogin() < activeThreshold) continue; // Not active

            int blocks = claimManager.getUsedClaims(id);
            if (blocks <= 0) continue;

            long tax = (long) (blocks * TaxRequests.getLandSteuerBasisPerBlock());
            if (tax <= 0) continue;

            if (financeManager.getMain(id) >= tax) {
                financeManager.addToMain(id, -tax);
                financeManager.addToMain(ClaimManager.GOVERNMENT_UUID, tax);
                Player online = p.getPlayer();
                if (online != null) {
                    online.sendMessage("§cLandsteuer abgezogen: §f" + FinanceManager.formatCents(tax) + " (§7" + blocks + " Blöcke§c)");
                }
            } else {
                getLogger().warning("Player " + p.getName() + " has insufficient funds for Landsteuer: " + tax);
            }
        }
    }
}