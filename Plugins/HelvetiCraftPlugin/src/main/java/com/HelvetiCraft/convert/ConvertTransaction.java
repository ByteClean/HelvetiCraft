package com.HelvetiCraft.convert;

import org.bukkit.Material;

public class ConvertTransaction {
    private final Material material;
    private final int amount;
    private final long rateCents;

    public ConvertTransaction(Material material, int amount, long rateCents) {
        this.material = material;
        this.amount = amount;
        this.rateCents = rateCents;
    }

    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public long getRateCents() { return rateCents; }
    public long getTotalCents() { return rateCents * amount; }
}