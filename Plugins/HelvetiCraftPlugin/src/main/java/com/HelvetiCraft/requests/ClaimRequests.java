package com.HelvetiCraft.requests;

import static org.bukkit.Bukkit.getLogger;

/**
 * Dummy request handler for claim block buy/sell rates.
 * Other classes should call these static methods to obtain the current rates (in cents).
 * Values are configurable here for tests and can later be wired to a real backend.
 */
public class ClaimRequests {

    // default rates (cents)
    private static long buyPriceCents = 500L; // 5.00
    private static long sellPriceCents = 300L; // 3.00

    public static long getBuyPriceCents() {
        return buyPriceCents;
    }

    public static long getSellPriceCents() {
        return sellPriceCents;
    }

    public static void setBuyPriceCents(long cents) {
        buyPriceCents = Math.max(0, cents);
        getLogger().info("[ClaimRequests] Set buy price to " + buyPriceCents + " cents");
    }

    public static void setSellPriceCents(long cents) {
        sellPriceCents = Math.max(0, cents);
        getLogger().info("[ClaimRequests] Set sell price to " + sellPriceCents + " cents");
    }
}
