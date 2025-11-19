package com.HelvetiCraft.shop;

import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.util.TaxUtils;

import java.util.UUID;

public class ShopTaxManager {

    public static final UUID GOVERNMENT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private final FinanceManager financeManager;

    public ShopTaxManager(FinanceManager financeManager) {
        this.financeManager = financeManager;
        financeManager.ensureAccount(GOVERNMENT_UUID);
    }

    public long[] applyTaxes(long grossCents) {
        long mwst = TaxUtils.calculateMWST(grossCents);
        long shop = TaxUtils.calculateShopTax(grossCents);
        long net = grossCents - mwst - shop;
        return new long[]{mwst, shop, net};
    }
}