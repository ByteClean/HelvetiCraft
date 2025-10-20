package com.HelvetiCraft.finance;

import com.HelvetiCraft.requests.FinanceRequests;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class FinanceManager {

    private final Plugin plugin;

    public FinanceManager(Plugin plugin) {
        this.plugin = plugin;
    }

    // --- Core Methods (redirected to FinanceRequests) ---

    public void ensureAccount(UUID id) {
        if (!FinanceRequests.hasAccount(id)) {
            FinanceRequests.createAccount(id, 25000L); // default starting balance
        }
    }

    public boolean hasAccount(UUID id) {
        return FinanceRequests.hasAccount(id);
    }

    public long getMain(UUID id) {
        return FinanceRequests.getMain(id);
    }

    public long getSavings(UUID id) {
        return FinanceRequests.getSavings(id);
    }

    public void setMain(UUID id, long cents) {
        FinanceRequests.setMain(id, cents);
    }

    public void setSavings(UUID id, long cents) {
        FinanceRequests.setSavings(id, cents);
    }

    public boolean transferMain(UUID from, UUID to, long cents) {
        return FinanceRequests.transferMain(from, to, cents);
    }

    public long getTotalNetWorthCents() {
        return FinanceRequests.getTotalNetWorthCents();
    }

    public Set<UUID> getKnownPlayers() {
        return FinanceRequests.getKnownPlayers();
    }

    // --- Account Movement Helpers (used by /save command) ---

    public boolean moveMainToSavings(UUID id, long cents) {
        long main = getMain(id);
        if (main < cents) return false;
        setMain(id, main - cents);
        setSavings(id, getSavings(id) + cents);
        return true;
    }

    public boolean moveSavingsToMain(UUID id, long cents) {
        long savings = getSavings(id);
        if (savings < cents) return false;
        setSavings(id, savings - cents);
        setMain(id, getMain(id) + cents);
        return true;
    }

    // --- Placeholder save (used by PayCommand, SellCommand, etc.) ---

    /**
     * Dummy save() for compatibility.
     * Currently does nothing, since FinanceRequests stores data in memory.
     * Later this can push all balances to the real backend.
     */
    public void save() {
        System.out.println("[FinanceManager] Dummy save() called – no persistent storage yet.");
    }

    // --- Utility Methods (unchanged) ---

    public static long parseAmountToCents(String input) throws IllegalArgumentException {
        String cleaned = input.replace(",", ".").trim();
        if (!cleaned.matches("-?\\d+(\\.\\d{1,2})?"))
            throw new IllegalArgumentException("Invalid amount");

        boolean negative = cleaned.startsWith("-");
        if (negative) cleaned = cleaned.substring(1);

        int dot = cleaned.indexOf('.');
        long result;
        if (dot < 0) {
            result = Math.multiplyExact(Long.parseLong(cleaned), 100L);
        } else {
            String whole = cleaned.substring(0, dot);
            String frac = cleaned.substring(dot + 1);
            if (frac.length() == 1) frac += "0";
            if (frac.length() > 2) frac = frac.substring(0, 2);
            long wholePart = Long.parseLong(whole);
            long fracPart = Long.parseLong(frac);
            result = Math.addExact(Math.multiplyExact(wholePart, 100L), fracPart);
        }
        return negative ? -result : result;
    }

    public static String formatCents(long cents) {
        boolean negative = cents < 0;
        long abs = Math.abs(cents);
        long whole = abs / 100;
        long frac = abs % 100;
        String formatted = NumberFormat.getInstance(Locale.GERMANY)
                .format(whole) + "," + (frac < 10 ? "0" : "") + frac;
        return (negative ? "-" : "") + formatted;
    }

    public String getPlayerNameOrUuid(UUID id) {
        OfflinePlayer p = plugin.getServer().getOfflinePlayer(id);
        String name = p.getName();
        return name != null ? name : id.toString();
    }
}
