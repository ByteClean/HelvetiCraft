package com.HelvetiCraft.util;

import com.HelvetiCraft.requests.TaxRequests;
import com.HelvetiCraft.shop.ShopRounding;

public class TaxUtils {

    public static long calculateMWST(long grossCents) {
        double mwstRate = TaxRequests.getMWST() / 100.0;
        return ShopRounding.roundToRappen(Math.round(grossCents * mwstRate));
    }

    public static long calculateShopTax(long grossCents) {
        double shopRate = TaxRequests.getShopSteuer() / 100.0;
        return ShopRounding.roundToRappen(Math.round(grossCents * shopRate));
    }

    public static long calculateNetAmount(long grossCents) {
        long mwst = calculateMWST(grossCents);
        long shop = calculateShopTax(grossCents);
        return grossCents - mwst - shop;
    }
}
