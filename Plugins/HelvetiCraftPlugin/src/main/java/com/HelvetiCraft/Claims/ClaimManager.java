package com.HelvetiCraft.Claims;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages buying/selling of bonus claim blocks and (best-effort) integration with GriefPrevention.
 *
 * Assumptions made:
 * - Price per claim-block when buying is BUY_PRICE_CENTS_PER_BLOCK (default 500 = 5.00).
 * - Sell price per claim-block is SELL_PRICE_CENTS_PER_BLOCK (default 300 = 3.00).
 * - Admin/government account uses UUID(0,0) as requested.
 *
 * This class keeps a small in-memory store of applied bonus blocks and will attempt to apply
 * the same change to GriefPrevention via reflection if the plugin is available. The reflection
 * integration is best-effort and logs if it cannot find compatible fields/methods.
 */
public class ClaimManager {

    private final Plugin plugin;
    private final FinanceManager finance;

    // simple in-memory store for bonus blocks when GP isn't available or as authoritative local cache
    private final Map<UUID, Integer> bonusBlocks = new ConcurrentHashMap<>();

    // Prices (cents)
    public static final long BUY_PRICE_CENTS_PER_BLOCK = 500L; // 5.00
    public static final long SELL_PRICE_CENTS_PER_BLOCK = 300L; // 3.00

    // Government/admin account UUID (all zeros)
    public static final UUID GOVERNMENT_UUID = new UUID(0L, 0L);

    public ClaimManager(Plugin plugin, FinanceManager finance) {
        this.plugin = plugin;
        this.finance = finance;
    }

    public int getLocalBonusBlocks(UUID player) {
        return bonusBlocks.getOrDefault(player, 0);
    }

    /**
     * Attempt to buy a number of claim blocks. Handles finance transfer from player -> government.
     * If successful, increments local bonus store and tries to apply to GriefPrevention.
     */
    public boolean buyClaimBlocks(UUID player, int amount) {
        if (amount <= 0) return false;
        long totalCost = Math.multiplyExact(amount, BUY_PRICE_CENTS_PER_BLOCK);

        finance.ensureAccount(player);
        finance.ensureAccount(GOVERNMENT_UUID);

        if (finance.getMain(player) < totalCost) return false;

        boolean ok = finance.transferMain(player, GOVERNMENT_UUID, totalCost);
        if (!ok) return false;

        bonusBlocks.merge(player, amount, Integer::sum);
        applyToGriefPrevention(player, amount); // best-effort
        finance.save();
        return true;
    }

    /**
     * Attempt to sell claim blocks from the player to the government (government pays player).
     * Removes blocks locally and (best-effort) from GriefPrevention.
     */
    public boolean sellClaimBlocks(UUID player, int amount) {
        if (amount <= 0) return false;

        int local = getLocalBonusBlocks(player);
        if (local < amount) return false;

        long payout = Math.multiplyExact(amount, SELL_PRICE_CENTS_PER_BLOCK);

        finance.ensureAccount(player);
        finance.ensureAccount(GOVERNMENT_UUID);

        // Move money from government -> player
        boolean paid = finance.transferMain(GOVERNMENT_UUID, player, payout);
        if (!paid) return false; // government might not have money in dummy backend

        bonusBlocks.computeIfPresent(player, (k, v) -> v - amount <= 0 ? null : v - amount);
        applyToGriefPrevention(player, -amount); // remove blocks in GP if possible
        finance.save();
        return true;
    }

    /**
     * Best-effort integration with GriefPrevention: tries common field/method names via reflection.
     * If it cannot find anything it simply logs and returns.
     */
    private void applyToGriefPrevention(UUID player, int delta) {
        // First try using the built-in GriefPrevention admin command '/acb <player> <delta>' as console.
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) {
                plugin.getLogger().info("GriefPrevention not present — keeping local claim blocks only.");
                return;
            }

            OfflinePlayer op = Bukkit.getOfflinePlayer(player);
            String name = op.getName();
            if (name == null || name.isEmpty()) {
                plugin.getLogger().info("Player name unavailable for " + player + " — cannot run /acb. Skipping GP sync.");
            } else {
                String cmd = "acb " + name + " " + delta;
                // Ensure dispatch runs on the main server thread to avoid 'Dispatching command async' warnings.
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            boolean dispatched = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            plugin.getLogger().info("Dispatched GriefPrevention command: '" + cmd + "' result=" + dispatched);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error dispatching GP command on main thread: " + ex.getMessage());
                        }
                    });
                    // Scheduled the command for main thread execution — treat as handled.
                    return;
                } catch (IllegalStateException ise) {
                    // If scheduler unavailable for some reason, fall back to direct dispatch (best-effort).
                    boolean dispatched = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    plugin.getLogger().info("Fallback dispatched GriefPrevention command: '" + cmd + "' result=" + dispatched);
                    if (dispatched) return;
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while attempting GriefPrevention /acb command: " + e.getMessage());
        }

        // Fallback: reflection-based attempt for older/newer GP builds (best-effort).
        try {
            // Try common GriefPrevention main class names (different builds use different packages)
            Class<?> gpClass = null;
            try {
                gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            } catch (ClassNotFoundException e) {
                try {
                    gpClass = Class.forName("com.griefprevention.GriefPrevention");
                } catch (ClassNotFoundException ex) {
                    // not found
                }
            }

            if (gpClass == null) {
                plugin.getLogger().info("GriefPrevention class not found via reflection — skipping GP sync.");
                return;
            }

            // Try to get the data store or instance field
            Object gpInstance = null;
            try {
                Field inst = gpClass.getDeclaredField("instance");
                inst.setAccessible(true);
                gpInstance = inst.get(null);
            } catch (NoSuchFieldException nsf) {
                try {
                    Method getInstance = gpClass.getDeclaredMethod("getInstance");
                    gpInstance = getInstance.invoke(null);
                } catch (NoSuchMethodException ignored) {
                    // fallthrough
                }
            }

            if (gpInstance == null) {
                plugin.getLogger().info("Could not obtain GriefPrevention instance — skipping GP sync.");
                return;
            }

            // Many GP builds have a 'dataStore' field with getPlayerData/PlayerData objects.
            Field dataStoreField = null;
            try {
                dataStoreField = gpClass.getDeclaredField("dataStore");
                dataStoreField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                // ignore
            }

            Object dataStore = dataStoreField != null ? dataStoreField.get(gpInstance) : null;
            if (dataStore == null) {
                plugin.getLogger().info("GriefPrevention dataStore not found — skipping GP sync.");
                return;
            }

            // try getPlayerData world-aware (signature varies). We'll try two common variants.
            Object playerData = null;
            try {
                Method getPlayerData = dataStore.getClass().getMethod("getPlayerData", java.util.UUID.class);
                playerData = getPlayerData.invoke(dataStore, player);
            } catch (NoSuchMethodException ignored) {
                try {
                    Method getPlayerData2 = dataStore.getClass().getMethod("getPlayerData", org.bukkit.World.class, java.util.UUID.class);
                    playerData = getPlayerData2.invoke(dataStore, Bukkit.getWorlds().get(0), player);
                } catch (NoSuchMethodException ignored2) {
                    // can't find
                }
            }

            if (playerData == null) {
                plugin.getLogger().info("Could not obtain GriefPrevention PlayerData — skipping GP sync.");
                return;
            }

            // Try to find a field named 'bonusClaimBlocks' or methods to modify accrued/bonus blocks
            try {
                Field bonusField = playerData.getClass().getDeclaredField("bonusClaimBlocks");
                bonusField.setAccessible(true);
                Object currentObj = bonusField.get(playerData);
                int current = 0;
                if (currentObj instanceof Number) {
                    current = ((Number) currentObj).intValue();
                } else if (currentObj != null) {
                    try {
                        current = Integer.parseInt(currentObj.toString());
                    } catch (NumberFormatException nfe) {
                        current = 0;
                    }
                }
                int updated = current + delta;
                Class<?> ftype = bonusField.getType();
                if (ftype.isPrimitive()) {
                    if (ftype == int.class) {
                        bonusField.setInt(playerData, updated);
                    } else if (ftype == long.class) {
                        bonusField.setLong(playerData, updated);
                    } else {
                        bonusField.set(playerData, updated);
                    }
                } else {
                    // Wrapper types
                    if (ftype == Integer.class) bonusField.set(playerData, Integer.valueOf(updated));
                    else if (ftype == Long.class) bonusField.set(playerData, Long.valueOf(updated));
                    else bonusField.set(playerData, updated);
                }
                plugin.getLogger().info("Applied delta " + delta + " to GP bonusClaimBlocks for " + player);
                return;
            } catch (NoSuchFieldException ignored) {
                // try methods
            }

            // Try method addBonusClaimBlocks(int)
            try {
                Method addMethod = playerData.getClass().getMethod("addBonusClaimBlocks", int.class);
                addMethod.invoke(playerData, delta);
                plugin.getLogger().info("Invoked addBonusClaimBlocks(" + delta + ") on GP PlayerData for " + player);
                return;
            } catch (NoSuchMethodException ignored) {
                // nothing
            }

            plugin.getLogger().info("No compatible field/method found on GP PlayerData — GP sync skipped.");

        } catch (Exception e) {
            plugin.getLogger().warning("Error while attempting GriefPrevention integration: " + e.getMessage());
        }
    }
}
