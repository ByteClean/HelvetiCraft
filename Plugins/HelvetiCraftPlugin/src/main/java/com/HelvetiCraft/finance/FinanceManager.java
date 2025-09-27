package com.HelvetiCraft.finance;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FinanceManager {

    // We store currency as "cents" (long) to avoid floating point errors
    // 100 = 1.00 in-game currency
    private final Plugin plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private final Map<UUID, Account> cache = new ConcurrentHashMap<>();

    public static class Account {
        public long main;     // in cents
        public long savings;  // in cents
        Account(long main, long savings) { this.main = main; this.savings = savings; }
    }

    public FinanceManager(Plugin plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "data");
        if (!dir.exists()) dir.mkdirs();
        this.dataFile = new File(dir, "finance.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private void loadAll() {
        if (!data.isConfigurationSection("players")) return;
        for (String key : data.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                long main = data.getLong("players." + key + ".main", 0L);
                long savings = data.getLong("players." + key + ".savings", 0L);
                cache.put(id, new Account(main, savings));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public synchronized void save() {
        for (Map.Entry<UUID, Account> e : cache.entrySet()) {
            String base = "players." + e.getKey();
            data.set(base + ".main", e.getValue().main);
            data.set(base + ".savings", e.getValue().savings);
        }
        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save finance.yml: " + ex.getMessage());
        }
    }

    public void ensureAccount(UUID id) {
        cache.computeIfAbsent(id, k -> new Account(0L, 0L));
    }

    /**
     * Returns true if the player already has a stored account (in data file or cache).
     * This helps distinguish first-time joins from returning players.
     */
    public boolean hasAccount(UUID id) {
        // Check persisted data first
        if (data.isSet("players." + id.toString())) return true;
        // Then check in-memory cache
        return cache.containsKey(id);
    }

    public boolean moveMainToSavings(UUID id, long cents) {
        if (cents <= 0) return false;
        Account acc = getAccount(id);
        if (acc.main < cents) return false;
        acc.main -= cents;
        acc.savings += cents;
        return true;
    }

    public boolean moveSavingsToMain(UUID id, long cents) {
        if (cents <= 0) return false;
        Account acc = getAccount(id);
        if (acc.savings < cents) return false;
        acc.savings -= cents;
        acc.main += cents;
        return true;
    }

    public Account getAccount(UUID id) {
        ensureAccount(id);
        return cache.get(id);
    }

    public long getMain(UUID id) {
        return getAccount(id).main;
    }

    public long getSavings(UUID id) {
        return getAccount(id).savings;
    }

    public void setMain(UUID id, long cents) {
        getAccount(id).main = Math.max(0, cents);
    }

    public void setSavings(UUID id, long cents) {
        getAccount(id).savings = Math.max(0, cents);
    }

    public boolean transferMain(UUID from, UUID to, long cents) {
        if (cents <= 0) return false;
        Account a = getAccount(from);
        Account b = getAccount(to);
        if (a.main < cents) return false;
        a.main -= cents;
        b.main += cents;
        return true;
    }

    public Set<UUID> getKnownPlayers() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    public long getTotalNetWorthCents() {
        long total = 0L;
        for (Account acc : cache.values()) {
            total += acc.main + acc.savings;
        }
        return total;
    }

    public static long parseAmountToCents(String input) throws IllegalArgumentException {
        String cleaned = input.replace(",", ".").trim();

        // allow optional leading "-"
        if (!cleaned.matches("-?\\d+(\\.\\d{1,2})?"))
            throw new IllegalArgumentException("Invalid amount");

        boolean negative = cleaned.startsWith("-");
        if (negative) {
            cleaned = cleaned.substring(1); // strip "-" for parsing
        }

        int dot = cleaned.indexOf('.');
        long result;
        if (dot < 0) {
            result = Math.multiplyExact(Long.parseLong(cleaned), 100L);
        } else {
            String whole = cleaned.substring(0, dot);
            String frac = cleaned.substring(dot + 1);
            if (frac.length() == 1) frac = frac + "0";
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
        String formatted = NumberFormat.getInstance(Locale.GERMANY).format(whole) + "," + (frac < 10 ? "0" : "") + frac;
        return (negative ? "-" : "") + formatted;
    }

    public String getPlayerNameOrUuid(UUID id) {
        OfflinePlayer p = plugin.getServer().getOfflinePlayer(id);
        String name = p.getName();
        return name != null ? name : id.toString();
    }
}
