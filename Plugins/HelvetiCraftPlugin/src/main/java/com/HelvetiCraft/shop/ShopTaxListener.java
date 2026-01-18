package com.HelvetiCraft.shop;

import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.HelvetiCraft.finance.FinanceManager;
import com.HelvetiCraft.requests.ShopTransactionRequests;
import com.HelvetiCraft.requests.TransactionLogRequests;
import com.HelvetiCraft.util.FinanceTransactionLogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.math.BigDecimal;
import java.util.UUID;

public class ShopTaxListener implements Listener {

    private final ShopTaxManager taxManager;
    private final ShopChatFormatter chatFormatter;
    private final FinanceManager financeManager;

    public ShopTaxListener(FinanceManager financeManager) {
        this.financeManager = financeManager;
        this.taxManager = new ShopTaxManager(financeManager);
        this.chatFormatter = new ShopChatFormatter(financeManager);
    }

    @EventHandler
    public void onShopTransaction(TransactionEvent event) {
        if (event.isCancelled()) return;

        Player client = event.getClient();                    // Wer klickt
        OfflinePlayer owner = event.getOwner();                // Shop-Besitzer
        BigDecimal priceBig = event.getExactPrice();
        long grossCents = Math.round(priceBig.doubleValue() * 100);

        int amount = event.getStock().length > 0 ? event.getStock()[0].getAmount() : 1;
        String itemName = event.getStock().length > 0 ? event.getStock()[0].getType().name() : "Item";

        long[] taxes = taxManager.applyTaxes(grossCents);
        long mwst = taxes[0];
        long shopTax = taxes[1];
        long totalTax = mwst + shopTax;
        long net = grossCents - totalTax;

        Player ownerOnline = (owner != null && owner.isOnline()) ? owner.getPlayer() : null;

        // ================================================================
        // 1. SPIELER KAUFT BEI SPIELER-SHOP → Verkäufer = Shop-Besitzer
        // ================================================================
        if (event.getTransactionType() == TransactionEvent.TransactionType.BUY) {

            // Nur bei echtem Spieler-Shop Steuern abziehen
            if (owner == null || "Admin Shop".equals(owner.getName()) ||
                    owner.getUniqueId().equals(ShopTaxManager.GOVERNMENT_UUID)) {
                return; // Admin-Shop → keine Steuer
            }

            UUID sellerUUID = owner.getUniqueId();

            // Log shop transaction for supply/demand analytics
            ShopTransactionRequests.logShopTransaction(
                itemName,
                amount,
                "BUY",
                grossCents,
                client.getUniqueId(),  // buyer
                sellerUUID              // seller
            );

            TransactionLogRequests.prepareRequest("Shop-Buy-Transaction", client.getUniqueId(), sellerUUID, grossCents);
            // STEUERN VOM VERKÄUFER ABZIEHEN
            FinanceTransactionLogger logger = new FinanceTransactionLogger(financeManager);
            logger.logTransaction("Shop-Tax", sellerUUID, ShopTaxManager.GOVERNMENT_UUID, totalTax);

            //financeManager.addToMain(sellerUUID, -totalTax);
            //financeManager.addToMain(ShopTaxManager.GOVERNMENT_UUID, totalTax);

            chatFormatter.sendBuyMessage(client, ownerOnline, amount, itemName, grossCents, mwst, shopTax, net);
        }

        // ================================================================
        // 2. SPIELER VERKAUFT AN SHOP → Verkäufer = Spieler (client)
        // ================================================================
        else if (event.getTransactionType() == TransactionEvent.TransactionType.SELL) {

            UUID sellerUUID = client.getUniqueId();
            UUID buyerUUID = (owner != null) ? owner.getUniqueId() : ShopTaxManager.GOVERNMENT_UUID;

            // Log shop transaction for supply/demand analytics
            ShopTransactionRequests.logShopTransaction(
                itemName,
                amount,
                "SELL",
                grossCents,
                buyerUUID,   // shop owner/buyer
                sellerUUID   // player selling to shop
            );

            // Nur wenn es KEIN Admin-Shop ist → Spieler zahlt Steuer
            if (owner == null || "Admin Shop".equals(owner.getName()) ||
                    owner.getUniqueId().equals(ShopTaxManager.GOVERNMENT_UUID)) {
                // Verkauf an Admin-Shop → keine Steuer
                chatFormatter.sendSellToAdminMessage(client, amount, itemName, grossCents);
                return;
            }

            TransactionLogRequests.prepareRequest("Shop-Sell-Transaction", owner.getUniqueId(), sellerUUID, grossCents);
            // STEUERN VOM SPIELER (Verkäufer) ABZIEHEN
            FinanceTransactionLogger logger = new FinanceTransactionLogger(financeManager);
            logger.logTransaction("Shop-Tax", sellerUUID, ShopTaxManager.GOVERNMENT_UUID, totalTax);

            //financeManager.addToMain(sellerUUID, -totalTax);
            //financeManager.addToMain(ShopTaxManager.GOVERNMENT_UUID, totalTax);

            chatFormatter.sendSellMessage(client, ownerOnline, amount, itemName, grossCents, mwst, shopTax, net);
        }
    }
}