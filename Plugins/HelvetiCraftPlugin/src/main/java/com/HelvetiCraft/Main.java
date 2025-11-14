package com.HelvetiCraft;

import com.HelvetiCraft.Claims.*;
import com.HelvetiCraft.commands.*;
import com.HelvetiCraft.convert.ConvertManager;
import com.HelvetiCraft.expansions.FinanceExpansion;
import com.HelvetiCraft.expansions.InitiativeExpansion;
import com.HelvetiCraft.initiatives.InitiativeManager;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.finance.FinanceJoinListener;
import com.HelvetiCraft.economy.VaultEconomyBridge;
import com.HelvetiCraft.requests.*;
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
    private LandTaxManager landTaxManager;

    int intervalDays = TaxRequests.getLandSteuerIntervalDays();
    long intervalSeconds = intervalDays * 24L * 3600L;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // === Initialization ===
        AdminRequests.init(this);

        initiativeManager = new InitiativeManager(this);
        financeManager = new FinanceManager(this);
        claimManager = new ClaimManager(this, financeManager);
        landTaxManager = new LandTaxManager(this, financeManager);

        // === Placeholder Expansions ===
        new InitiativeExpansion().register();
        new FinanceExpansion(financeManager).register();
        getLogger().info("Placeholders registered!");

        // === Commands ===
        registerCommand("initiative", new InitiativeCommand(initiativeManager));
        registerCommand("verify", new VerifyCommand(this));
        registerCommand("status", new StatusCommand(this));
        registerCommand("helveticraft", new HelveticraftCommand(this));
        registerCommand("admin", new AdminCommand(this));
        registerCommand("finance", new FinanceCommand(financeManager));
        registerCommand("networth", new NetworthCommand(financeManager));
        registerCommand("pay", new PayCommand(financeManager));

        SellCommand sell = new SellCommand(this, financeManager);
        registerCommand("sell", sell);
        registerCommand("sellaccept", sell);
        registerCommand("selldecline", sell);
        registerCommand("save", new SaveCommand(financeManager));

        ConvertManager convertManager = new ConvertManager(this, financeManager);
        registerCommand("convert", new ConvertCommand(convertManager));

        // === Claim block trading ===
        registerCommand("buyclaimblock", new BuyClaimBlockCommand(claimManager));
        registerCommand("sellclaimblock", new SellClaimBlockCommand(claimManager));

        // === Land Tax Command ===
        if (getCommand("landtax") != null) {
            getCommand("landtax").setExecutor(new LandTaxCommand(landTaxManager));
            getLogger().info("Command /landtax registered successfully.");
        } else {
            getLogger().warning("Command 'landtax' could not be found! Check plugin.yml.");
        }

        // === Event Listeners ===
        getServer().getPluginManager().registerEvents(initiativeManager, this);
        getServer().getPluginManager().registerEvents(new FinanceJoinListener(financeManager), this);

        // === Vault Economy Bridge ===
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

        // === Periodic Taxes (Every 3 Days) ===
        getLogger().info("[HelvetiCraft] Landsteuer-Intervall: " + intervalDays + " Tage (" + intervalSeconds + " Sekunden)");

        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> {
            try {
                getLogger().info("[HelvetiCraft] Running periodic tax collection...");
                runVermoegensSteuerCollection();
                runEinkommenSteuerCollection();
                landTaxManager.collectLandTax();
                getLogger().info("[HelvetiCraft] Periodic tax cycle completed.");
            } catch (Exception e) {
                getLogger().severe("[HelvetiCraft] Error during periodic tax task: " + e.getMessage());
                e.printStackTrace();
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void onDisable() {
        getLogger().info("HelvetiCraft Plugin has been disabled!");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor) {
        if (getCommand(name) != null) {
            getCommand(name).setExecutor(executor);
        } else {
            getLogger().warning("Command '" + name + "' could not be found!");
        }
    }

    // === Tax System ===
    private void runVermoegensSteuerCollection() {
        long activeThreshold = System.currentTimeMillis() - 10L * 24 * 3600 * 1000;
        for (UUID id : financeManager.getKnownPlayers()) {
            if (id.equals(ClaimManager.GOVERNMENT_UUID)) continue;
            OfflinePlayer p = Bukkit.getOfflinePlayer(id);
            if (p.getLastLogin() < activeThreshold) continue;

            long wealth = financeManager.getMain(id) + financeManager.getSavings(id);
            long tax = TaxRequests.calculateVermoegensSteuer(wealth);
            if (tax <= 0) continue;

            if (financeManager.getMain(id) >= tax) {
                financeManager.addToMain(id, -tax);
                financeManager.addToMain(ClaimManager.GOVERNMENT_UUID, tax);
                Player online = p.getPlayer();
                if (online != null)
                    online.sendMessage("§cVermögenssteuer abgezogen: §f" + FinanceManager.formatCents(tax));
            } else {
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
                if (online != null)
                    online.sendMessage("§cEinkommensteuer abgezogen: §f" + FinanceManager.formatCents(tax));
                FinanceRequests.resetPeriodIncome(id);
            } else {
                getLogger().warning("Player " + p.getName() + " has insufficient funds for Einkommensteuer: " + tax);
            }
        }
    }
}
