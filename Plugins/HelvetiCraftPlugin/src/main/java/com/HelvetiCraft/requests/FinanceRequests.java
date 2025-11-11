package com.HelvetiCraft.requests;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.bukkit.Bukkit.getLogger;

/**
 * Dummy backend request handler for Finance data.
 * Later this will perform real HTTP requests to the Flask backend.
 */
public class FinanceRequests {

    // --- Temporary in-memory simulation (represents backend) ---
    private static final Map<UUID, Long> mainBalances = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> savingsBalances = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> periodIncome = new ConcurrentHashMap<>();

    public static boolean hasAccount(UUID id) {
        return mainBalances.containsKey(id);
    }

    public static void createAccount(UUID id, long starterCents) {
        mainBalances.put(id, starterCents);
        savingsBalances.put(id, 0L);
        periodIncome.put(id, 0L);
        getLogger().info("[FinanceRequests] Created dummy account for " + id + " with " + starterCents + " cents");
    }

    public static long getMain(UUID id) {
        return mainBalances.getOrDefault(id, 0L);
    }

    public static long getSavings(UUID id) {
        return savingsBalances.getOrDefault(id, 0L);
    }

    public static void setMain(UUID id, long cents) {
        mainBalances.put(id, Math.max(0, cents));
        getLogger().info("[FinanceRequests] (Dummy) Updated main balance for " + id + ": " + cents);
    }

    public static void setSavings(UUID id, long cents) {
        savingsBalances.put(id, Math.max(0, cents));
        getLogger().info("[FinanceRequests] (Dummy) Updated savings for " + id + ": " + cents);
    }

    public static void addToMain(UUID id, long cents) {
        long current = getMain(id);
        long newBalance = Math.max(0, current + cents);
        setMain(id, newBalance);
        if (cents > 0) {
            periodIncome.put(id, periodIncome.getOrDefault(id, 0L) + cents);
        }
    }

    public static boolean transferMain(UUID from, UUID to, long cents) {
        if (getMain(from) < cents) return false;
        addToMain(from, -cents);
        addToMain(to, cents);
        getLogger().info("[FinanceRequests] (Dummy) Transfer " + cents + " cents from " + from + " → " + to);
        return true;
    }

    public static long getTotalNetWorthCents() {
        return mainBalances.values().stream().mapToLong(Long::longValue).sum()
                + savingsBalances.values().stream().mapToLong(Long::longValue).sum();
    }

    public static Set<UUID> getKnownPlayers() {
        Set<UUID> all = new HashSet<>();
        all.addAll(mainBalances.keySet());
        all.addAll(savingsBalances.keySet());
        return all;
    }

    public static long getPeriodIncome(UUID id) {
        return periodIncome.getOrDefault(id, 0L);
    }

    public static void resetPeriodIncome(UUID id) {
        periodIncome.put(id, 0L);
    }
}