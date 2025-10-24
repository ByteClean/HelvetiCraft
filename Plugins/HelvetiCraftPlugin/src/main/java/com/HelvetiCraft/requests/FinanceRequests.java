package com.HelvetiCraft.requests;

import java.util.*;
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

    public static boolean hasAccount(UUID id) {
        return mainBalances.containsKey(id);
    }

    public static void createAccount(UUID id, long starterCents) {
        mainBalances.put(id, starterCents);
        savingsBalances.put(id, 0L);
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

    public static boolean transferMain(UUID from, UUID to, long cents) {
        if (getMain(from) < cents) return false;
        setMain(from, getMain(from) - cents);
        setMain(to, getMain(to) + cents);
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
}
