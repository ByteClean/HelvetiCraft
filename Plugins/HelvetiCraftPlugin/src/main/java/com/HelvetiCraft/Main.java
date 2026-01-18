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
import com.HelvetiCraft.initiatives.PhaseFileManager;
import com.HelvetiCraft.quiz.QuizListener;
import com.HelvetiCraft.quiz.QuizManager;
import com.HelvetiCraft.requests.*;
import com.HelvetiCraft.shop.ShopTaxListener;
import com.HelvetiCraft.taxes.LandTaxManager;
import com.HelvetiCraft.taxes.VermoegensSteuerManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.rowset.spi.SyncFactoryException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Main extends JavaPlugin {

    private InitiativeManager initiativeManager;
    private FinanceManager financeManager;
    private ClaimManager claimManager;
    private LandTaxManager landTaxManager;
    private VermoegensSteuerManager vermoegensSteuerManager;

    private QuizManager quizManager;

    int intervalDays = TaxRequests.getLandSteuerIntervalDays();
    long intervalSeconds = intervalDays * 24L * 3600L;

    @Override
    public void onEnable() {
        getLogger().info("HelvetiCraft Plugin has been enabled!");
        saveDefaultConfig();

        // Read from config.yml
        String apiBase = getConfig().getString("initiatives_api_base");
        String apiKey = getConfig().getString("minecraft_api_key");
        getLogger().info("API Base: " + apiBase);

        InitiativeRequests.init(apiBase, apiKey);
        QuizRequests.init(apiBase, apiKey, null);
        FinanceRequests.loadConfigFromPlugin(this);
        TransactionLogRequests.loadConfigFromPlugin(this);
        ShopTransactionRequests.loadConfigFromPlugin(this);

        PhaseFileManager.init(getDataFolder());

        // === Initialization ===
        AdminRequests.init(this);

        initiativeManager = new InitiativeManager(this);
        financeManager = new FinanceManager(this);
        claimManager = new ClaimManager(this, financeManager);
        landTaxManager = new LandTaxManager(this, financeManager);
        vermoegensSteuerManager = new VermoegensSteuerManager(this, financeManager);

        // === QUIZ SYSTEM ===
        quizManager = new QuizManager(this);
        quizManager.start();
        getLogger().info("[Quiz] QuizManager started and first question asked.");

        // Register answer listener
        getServer().getPluginManager().registerEvents(new QuizListener(quizManager), this);
        getLogger().info("[Quiz] QuizListener registered.");

        // === Placeholder Expansions ===
        new InitiativeExpansion().register();
        new FinanceExpansion(financeManager).register();
        getLogger().info("Placeholders registered!");

        ShopTaxListener listener = new ShopTaxListener(financeManager);
        getServer().getPluginManager().registerEvents(listener, this);
        getLogger().info("Shop tax listener registered for ChestShop!");

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
        
        // === Tax Test Command ===
        registerCommand("taxtest", new TaxTestCommand(landTaxManager, vermoegensSteuerManager));

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
                vermoegensSteuerManager.collectVermoegensSteuer();
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
}
