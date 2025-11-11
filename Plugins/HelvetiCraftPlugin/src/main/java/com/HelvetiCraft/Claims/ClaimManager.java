package com.HelvetiCraft.Claims;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.ClaimRequests;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages buying/selling of bonus claim blocks using GriefPrevention's /acb command and PlaceholderAPI.
 *
 * Assumptions made:
 * - Price per claim-block when buying is provided by ClaimRequests (default 500 = 5.00).
 * - Sell price per claim-block is provided by ClaimRequests (default 300 = 3.00).
 * - Admin/government account uses UUID(0,0) as requested.
 * - GriefPrevention's /acb command is available for modifying claim blocks.
 * - PlaceholderAPI with %griefprevention_remainingclaims% is available for checking current claims.
 *
 * This class uses GriefPrevention's /acb command to modify claim blocks and PlaceholderAPI to
 * check available claim blocks before sales. No local storage is used - GriefPrevention is
 * the sole source of truth for claim block counts.
 */
public class ClaimManager {

    private final Plugin plugin;
    private final FinanceManager finance;

    // Note: prices are now provided by ClaimRequests (dummy backend). Constants retained as fallback.
    public static final long BUY_PRICE_CENTS_PER_BLOCK = 500L; // fallback 5.00
    public static final long SELL_PRICE_CENTS_PER_BLOCK = 300L; // fallback 3.00

    // Government/admin account UUID (all zeros)
    public static final UUID GOVERNMENT_UUID = new UUID(0L, 0L);

    public ClaimManager(Plugin plugin, FinanceManager finance) {
        this.plugin = plugin;
        this.finance = finance;
    }

    /**
     * Attempt to buy a number of claim blocks. Handles finance transfer from player -> government.
     * If successful, increments local bonus store and tries to apply to GriefPrevention.
     */
    public boolean buyClaimBlocks(UUID player, int amount) {
        if (amount <= 0) return false;
        long pricePer = ClaimRequests.getBuyPriceCents();
        long totalCost = Math.multiplyExact(amount, pricePer);

        finance.ensureAccount(player);
        finance.ensureAccount(GOVERNMENT_UUID);

        if (finance.getMain(player) < totalCost) return false;

        boolean ok = finance.transferMain(player, GOVERNMENT_UUID, totalCost);
        if (!ok) return false;

        // Apply change through GriefPrevention /acb command
        applyToGriefPrevention(player, amount);
        finance.save();
        return true;
    }

    /**
     * Attempt to sell claim blocks from the player to the government (government pays player).
     * Removes blocks locally and (best-effort) from GriefPrevention.
     */
    public boolean sellClaimBlocks(UUID player, int amount) {
        if (amount <= 0) return false;

        // Check remaining claims via PlaceholderAPI
        int remaining = getRemainingClaims(player);
        if (remaining < 0) return false; // Cannot determine available claims
        if (remaining < amount) return false;

        long sellPer = ClaimRequests.getSellPriceCents();
        long payout = Math.multiplyExact(amount, sellPer);

        finance.ensureAccount(player);
        finance.ensureAccount(GOVERNMENT_UUID);

        // Move money from government -> player
        boolean paid = finance.transferMain(GOVERNMENT_UUID, player, payout);
        if (!paid) return false; // government might not have money in dummy backend

        // Apply change through GriefPrevention /acb command (negative amount to remove)
        applyToGriefPrevention(player, -amount);
        finance.save();
        return true;
    }

    /**
     * Best-effort integration with GriefPrevention: tries common field/method names via reflection.
     * If it cannot find anything it simply logs and returns.
     */
    private void applyToGriefPrevention(UUID player, int delta) {
        // Try using the /acb command as console first.
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
                try {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            boolean dispatched = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            plugin.getLogger().info("Dispatched GriefPrevention command: '" + cmd + "' result=" + dispatched);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("Error dispatching GP command on main thread: " + ex.getMessage());
                        }
                    });
                    return;
                } catch (IllegalStateException ise) {
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
            Class<?> gpClass = null;
            try {
                gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            } catch (ClassNotFoundException e) {
                try {
                    gpClass = Class.forName("com.griefprevention.GriefPrevention");
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (gpClass == null) {
                plugin.getLogger().info("GriefPrevention class not found via reflection — skipping GP sync.");
                return;
            }

            Object gpInstance = null;
            try {
                Field inst = gpClass.getDeclaredField("instance");
                inst.setAccessible(true);
                gpInstance = inst.get(null);
            } catch (NoSuchFieldException ignored) {
            }

            if (gpInstance == null) {
                plugin.getLogger().info("Could not obtain GriefPrevention instance — skipping GP sync.");
                return;
            }

            Field dataStoreField = gpClass.getDeclaredField("dataStore");
            dataStoreField.setAccessible(true);
            Object dataStore = dataStoreField.get(gpInstance);

            if (dataStore == null) {
                plugin.getLogger().info("GriefPrevention dataStore not found — skipping GP sync.");
                return;
            }

            Object playerData = null;
            try {
                Method getPlayerData = dataStore.getClass().getMethod("getPlayerData", UUID.class);
                playerData = getPlayerData.invoke(dataStore, player);
            } catch (NoSuchMethodException ignored) {
                try {
                    Method getPlayerData2 = dataStore.getClass().getMethod("getPlayerData", org.bukkit.World.class, UUID.class);
                    playerData = getPlayerData2.invoke(dataStore, Bukkit.getWorlds().get(0), player);
                } catch (NoSuchMethodException ignored2) {
                }
            }

            if (playerData == null) {
                plugin.getLogger().info("Could not obtain GriefPrevention PlayerData — skipping GP sync.");
                return;
            }

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
                    } catch (NumberFormatException ignored) {
                    }
                }

                int updated = current + delta;
                Class<?> ftype = bonusField.getType();

                if (ftype == int.class) bonusField.setInt(playerData, updated);
                else if (ftype == long.class) bonusField.setLong(playerData, updated);
                else bonusField.set(playerData, updated);

                plugin.getLogger().info("Applied delta " + delta + " to GP bonusClaimBlocks for " + player);
                return;
            } catch (NoSuchFieldException ignored) {
                // Try method addBonusClaimBlocks(int)
                try {
                    Method addMethod = playerData.getClass().getMethod("addBonusClaimBlocks", int.class);
                    addMethod.invoke(playerData, delta);
                    plugin.getLogger().info("Invoked addBonusClaimBlocks(" + delta + ") on GP PlayerData for " + player);
                    return;
                } catch (NoSuchMethodException ignored2) {
                }
            }

            plugin.getLogger().info("No compatible field/method found on GP PlayerData — GP sync skipped.");

        } catch (Exception e) {
            plugin.getLogger().warning("Error while attempting GriefPrevention integration: " + e.getMessage());
        }
    }

    /**
     * Query current remaining (unused) claim blocks for the player using PlaceholderAPI.
     * Returns -1 if the value cannot be determined.
     */
    public int getRemainingClaims(UUID player) {
        return getClaimsFromPAPI(player, "%griefprevention_remainingclaims%");
    }

    /**
     * Query used claim blocks for the player using PlaceholderAPI.
     * Returns -1 if the value cannot be determined.
     */
    public int getUsedClaims(UUID player) {
        return getClaimsFromPAPI(player, "%griefprevention_claimsused%");
    }

    private int getClaimsFromPAPI(UUID player, String placeholder) {
        try {
            if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                plugin.getLogger().info("PlaceholderAPI not present — cannot query claims.");
                return -1;
            }

            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method setPlaceholders = papi.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            OfflinePlayer op = Bukkit.getOfflinePlayer(player);
            Object res = setPlaceholders.invoke(null, op, placeholder);
            if (res == null) return -1;

            String s = res.toString().trim();
            if (s.isEmpty()) return -1;

            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException nfe) {
                plugin.getLogger().info("Placeholder " + placeholder + " returned non-integer: '" + s + "'");
                return -1;
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error while invoking PlaceholderAPI: " + e.getMessage());
            return -1;
        }
    }
}