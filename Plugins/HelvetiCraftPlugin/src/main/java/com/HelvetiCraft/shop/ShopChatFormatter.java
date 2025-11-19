package com.HelvetiCraft.shop;

import com.HelvetiCraft.finance.FinanceManager;
import org.bukkit.entity.Player;

public class ShopChatFormatter {

    private final FinanceManager financeManager;

    public ShopChatFormatter(FinanceManager financeManager) {
        this.financeManager = financeManager;
    }

    // KAUF bei Spieler-Shop
    public void sendBuyMessage(Player buyer, Player seller, int amount, String itemName,
                               long gross, long mwst, long shopTax, long net) {
        String pretty = formatItemName(itemName);
        long totalTax = mwst + shopTax;

        buyer.sendMessage("§aKauf ▸ §7Du hast §e" + amount + "× §b" + pretty +
                " §7für §a" + FinanceManager.formatCents(gross) + " CHF §7gekauft " +
                "§8(inkl. §6" + FinanceManager.formatCents(mwst) + " CHF MwSt§8)");

        if (seller != null) {
            seller.sendMessage("§eVerkauf ▸ §7Du hast §a" + FinanceManager.formatCents(net) +
                    " CHF netto §7erhalten §8(§6-" + FinanceManager.formatCents(totalTax) +
                    " CHF §7Steuern abgezogen§8)");
        }
    }

    // VERKAUF an Spieler-Shop
    public void sendSellMessage(Player seller, Player buyer, int amount, String itemName,
                                long gross, long mwst, long shopTax, long net) {
        String pretty = formatItemName(itemName);
        long totalTax = mwst + shopTax;

        seller.sendMessage("§aVerkauf ▸ §7Du hast §e" + amount + "× §b" + pretty +
                " §7für §a" + FinanceManager.formatCents(net) + " CHF netto §7verkauft " +
                "§8(§6-" + FinanceManager.formatCents(totalTax) + " CHF §7Steuern§8)");

        if (buyer != null) {
            buyer.sendMessage("§aKauf ▸ §7Du hast §e" + amount + "× §b" + pretty +
                    " §7für §a" + FinanceManager.formatCents(gross) + " CHF §7gekauft " +
                    "§8(inkl. §6" + FinanceManager.formatCents(mwst) + " CHF MwSt§8)");
        }
    }

    // VERKAUF an Admin-Shop (keine Steuer)
    public void sendSellToAdminMessage(Player seller, int amount, String itemName, long gross) {
        String pretty = formatItemName(itemName);

        seller.sendMessage("§aVerkauf an Admin-Shop ▸ §7Du hast §e" + amount + "× §b" + pretty +
                " §7für §a" + FinanceManager.formatCents(gross) + " CHF §7verkauft §8(keine Steuer)§8)");
    }

    private String formatItemName(String materialName) {
        String[] parts = materialName.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}