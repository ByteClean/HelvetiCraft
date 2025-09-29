package com.HelvetiCraft.economy;

import com.HelvetiCraft.finance.FinanceManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VaultEconomyBridge implements Economy {

    private final FinanceManager finance;

    public VaultEconomyBridge(FinanceManager finance) {
        this.finance = finance;
    }

    // --- Required by Vault ---

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("HelvetiCraft");
    }

    @Override
    public String getName() {
        return "HelvetiCraftEconomy";
    }

    @Override
    public boolean hasBankSupport() {
        return false; // No banks for now
    }

    @Override
    public int fractionalDigits() {
        return 2; // cents
    }

    @Override
    public String format(double amount) {
        return FinanceManager.formatCents((long) Math.round(amount * 100));
    }

    @Override
    public String currencyNamePlural() {
        return "Credits";
    }

    @Override
    public String currencyNameSingular() {
        return "Credit";
    }

    // --- Accounts ---

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        return true; // accounts exist implicitly
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String worldName) {
        return hasAccount(player); // ignore world
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        return finance.getMain(player.getUniqueId()) / 100.0;
    }

    @Override
    public double getBalance(OfflinePlayer player, String worldName) {
        return getBalance(player); // ignore world
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String worldName, double amount) {
        return has(player, amount); // ignore world
    }

    // --- Transactions ---

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        UUID id = player.getUniqueId();
        long cents = (long) Math.round(amount * 100);
        long balance = finance.getMain(id);
        if (balance < cents) {
            return new EconomyResponse(amount, balance / 100.0,
                    EconomyResponse.ResponseType.FAILURE, "Insufficient funds");
        }
        finance.setMain(id, balance - cents);
        finance.save();
        return new EconomyResponse(amount, getBalance(player),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
        return withdrawPlayer(player, amount); // ignore world
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        UUID id = player.getUniqueId();
        long cents = (long) Math.round(amount * 100);
        finance.setMain(id, finance.getMain(id) + cents);
        finance.save();
        return new EconomyResponse(amount, getBalance(player),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
        return depositPlayer(player, amount); // ignore world
    }

    @Override
    public EconomyResponse createBank(String name, String player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankHas(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
    }

    // --- Player account creation ---

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return true; // nothing to do, handled implicitly
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
        return createPlayerAccount(player); // ignore world
    }

    // --- Legacy overloads (for plugins using names) ---

    @Override public boolean hasAccount(String playerName) { return true; }
    @Override public boolean hasAccount(String playerName, String worldName) { return true; }
    @Override public double getBalance(String playerName) { return getBalance(Bukkit.getOfflinePlayer(playerName)); }
    @Override public double getBalance(String playerName, String worldName) { return getBalance(playerName); }
    @Override public boolean has(String playerName, double amount) { return has(Bukkit.getOfflinePlayer(playerName), amount); }
    @Override public boolean has(String playerName, String worldName, double amount) { return has(playerName, amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, double amount) { return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
    @Override public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) { return withdrawPlayer(playerName, amount); }
    @Override public EconomyResponse depositPlayer(String playerName, double amount) { return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount); }
    @Override public EconomyResponse depositPlayer(String playerName, String worldName, double amount) { return depositPlayer(playerName, amount); }
    @Override public boolean createPlayerAccount(String playerName) { return true; }
    @Override public boolean createPlayerAccount(String playerName, String worldName) { return true; }

    // --- Banks (not used) ---

    @Override public List<String> getBanks() { return Collections.emptyList(); }
}
