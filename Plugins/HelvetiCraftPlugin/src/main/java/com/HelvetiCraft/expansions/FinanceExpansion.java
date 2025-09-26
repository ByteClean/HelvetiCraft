package com.HelvetiCraft.expansions;

import com.HelvetiCraft.finance.FinanceManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class FinanceExpansion extends PlaceholderExpansion {

    private final FinanceManager finance;

    public FinanceExpansion(FinanceManager finance) {
        this.finance = finance;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "helveticraft"; // => %helveticraft_...%
    }

    @Override
    public @NotNull String getAuthor() {
        return "HelvetiCraft";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Bleibt nach /papi reload erhalten
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        UUID id = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "main":
                return FinanceManager.formatCents(finance.getMain(id));
            case "savings":
                return FinanceManager.formatCents(finance.getSavings(id));
            case "networth":
                long worth = finance.getMain(id) + finance.getSavings(id);
                return FinanceManager.formatCents(worth);
            case "totalnetworth":
                return FinanceManager.formatCents(finance.getTotalNetWorthCents());
            default:
                return null; // unbekannter Platzhalter
        }
    }
}
