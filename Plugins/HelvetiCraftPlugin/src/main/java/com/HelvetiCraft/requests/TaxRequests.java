package com.HelvetiCraft.requests;

import static org.bukkit.Bukkit.getLogger;

/**
 * Dummy request handler for tax rates and ore conversion rates.
 * Values are configurable here for tests and can later be wired to a real backend.
 */
public class TaxRequests {

    // Tax rates in percent
    public static double MWST = 7.7; // Mehrwertsteuer
    public static double VERKAUFS_STEUER_1ZU1 = 26.0; // 1-zu-1-Verkaufssteuer
    public static double SHOP_STEUER = 2.0; // Shopsteuer

    // Landsteuer basis in cents per block per period
    public static double LAND_STEUER_BASIS_PER_BLOCK = 1.0;

    // Progressive Einkommensteuer brackets: {threshold in cents, rate in percent}
    public static final double[][] EINKOMMEN_STEUER_BRACKETS = {
            {0, 5},
            {1000000, 12}, // 10.00 CHF
            {5000000, 20},
            {10000000, 30}
    };

    // Progressive Vermögenssteuer brackets: {threshold in cents, rate in percent}
    public static final double[][] VERMOEGENS_STEUER_BRACKETS = {
            {0, 0.2},
            {5000000, 0.5},
            {25000000, 1.0}
    };

    public static long ORE_CONVERT_TAX = 500; // 5.00 CHF pro Transaktion
    // Ore conversion rates in cents per ore item
    public static long COAL_ORE_CONVERSION = 50;
    public static long IRON_ORE_CONVERSION = 100;
    public static long COPPER_ORE_CONVERSION = 80;
    public static long GOLD_ORE_CONVERSION = 300;
    public static long REDSTONE_ORE_CONVERSION = 150;
    public static long LAPIS_ORE_CONVERSION = 200;
    public static long DIAMOND_ORE_CONVERSION = 1000;
    public static long EMERALD_ORE_CONVERSION = 1500;
    public static long QUARTZ_ORE_CONVERSION = 120;
    public static long ANCIENT_DEBRIS_CONVERSION = 2000; // For netherite

    // Getters for tax rates
    public static double getMWST() {
        return MWST;
    }

    public static double getVerkaufsSteuer1zu1() {
        return VERKAUFS_STEUER_1ZU1;
    }

    public static double getShopSteuer() {
        return SHOP_STEUER;
    }

    public static double getLandSteuerBasisPerBlock() {
        return LAND_STEUER_BASIS_PER_BLOCK;
    }

    // Calculate progressive taxes
    public static long calculateEinkommenSteuer(long incomeCents) {
        long tax = 0;
        double prevThreshold = 0;
        for (double[] bracket : EINKOMMEN_STEUER_BRACKETS) {
            double threshold = bracket[0];
            double rate = bracket[1] / 100.0;
            if (incomeCents > threshold) {
                tax += (long) ((threshold - prevThreshold) * rate);
                prevThreshold = threshold;
            } else {
                tax += (long) ((incomeCents - prevThreshold) * rate);
                return tax;
            }
        }
        // Highest bracket
        double rate = EINKOMMEN_STEUER_BRACKETS[EINKOMMEN_STEUER_BRACKETS.length - 1][1] / 100.0;
        tax += (long) ((incomeCents - prevThreshold) * rate);
        return tax;
    }

    public static long calculateVermoegensSteuer(long wealthCents) {
        long tax = 0;
        double prevThreshold = 0;
        for (double[] bracket : VERMOEGENS_STEUER_BRACKETS) {
            double threshold = bracket[0];
            double rate = bracket[1] / 100.0;
            if (wealthCents > threshold) {
                tax += (long) ((threshold - prevThreshold) * rate);
                prevThreshold = threshold;
            } else {
                tax += (long) ((wealthCents - prevThreshold) * rate);
                return tax;
            }
        }
        // Highest bracket
        double rate = VERMOEGENS_STEUER_BRACKETS[VERMOEGENS_STEUER_BRACKETS.length - 1][1] / 100.0;
        tax += (long) ((wealthCents - prevThreshold) * rate);
        return tax;
    }

    // Setters for dynamic adjustments (e.g., via admin commands)
    public static void setMWST(double rate) {
        MWST = Math.max(0, rate);
        getLogger().info("[TaxRequests] Set MWST to " + rate + "%");
    }

    public static long getOreConvertTax() {
        return ORE_CONVERT_TAX;
    }

    public static void setOreConvertTax(long tax) {
        ORE_CONVERT_TAX = Math.max(0, tax);
        getLogger().info("[TaxRequests] ORE_CONVERT_TAX auf " + tax + " Cents gesetzt.");
    }

    // Similar setters for others...
}