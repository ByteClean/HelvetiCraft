package com.HelvetiCraft.requests;

import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.logging.Logger;

public class ShopTransactionRequests {
    
    private static String API_BASE;
    private static String API_KEY;
    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static Logger logger;
    static UUID govUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void loadConfigFromPlugin(Plugin plugin) {
        API_BASE = plugin.getConfig().getString("initiatives_api_base");
        API_KEY = plugin.getConfig().getString("minecraft_api_key");
        logger = plugin.getLogger();
    }

    /**
     * Logs a shop transaction asynchronously (Folia-compatible).
     * 
     * @param itemType The material/item type (e.g., "DIAMOND")
     * @param quantity Number of items traded
     * @param transactionType "BUY" or "SELL"
     * @param priceCents Price in cents
     * @param buyerUuid UUID of the buyer
     * @param sellerUuid UUID of the seller
     */
    public static void logShopTransaction(
            String itemType,
            int quantity,
            String transactionType,
            long priceCents,
            UUID buyerUuid,
            UUID sellerUuid
    ) {
        String url = API_BASE + "/finances/shop-transactions/log";

        String buyerUuidStr = (buyerUuid != null) ? buyerUuid.toString() : "null";
        String sellerUuidStr = (sellerUuid != null) ? sellerUuid.toString() : "null";

        String jsonBody = String.format(
                "{\"itemType\":\"%s\",\"quantity\":%d,\"transactionType\":\"%s\",\"priceCents\":%d,\"buyerUuid\":\"%s\",\"sellerUuid\":\"%s\"}",
                itemType, quantity, transactionType, priceCents, buyerUuidStr, sellerUuidStr
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("x-auth-key", API_KEY)
                .header("x-auth-from", "minecraft")
                .header("x-uuid", govUUID.toString())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        // Async request (Folia-compatible - no scheduler needed)
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        logger.info("[ShopTransaction] Logged: " + itemType + " x" + quantity + " (" + transactionType + ") for " + priceCents + " cents");
                    } else {
                        logger.warning("[ShopTransaction] Failed to log: " + response.statusCode() + " - " + response.body());
                    }
                })
                .exceptionally(e -> {
                    logger.severe("[ShopTransaction] Error logging transaction: " + e.getMessage());
                    return null;
                });
    }
}
