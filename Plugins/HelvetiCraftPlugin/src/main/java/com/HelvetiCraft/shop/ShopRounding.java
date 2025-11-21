package com.HelvetiCraft.shop;

public class ShopRounding {

    /**
     * Rundet auf den nächsten Rappen (0.05 CHF)
     */
    public static long roundToRappen(long cents) {
        long remainder = cents % 5;
        if (remainder == 0) return cents;
        if (remainder < 3) return cents - remainder; // abrunden
        return cents + (5 - remainder); // aufrunden
    }

    public static double roundToRappenDouble(double amount) {
        long cents = Math.round(amount * 100);
        return roundToRappen(cents) / 100.0;
    }
}
