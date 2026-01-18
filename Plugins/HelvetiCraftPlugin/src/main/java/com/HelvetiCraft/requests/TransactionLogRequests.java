package com.HelvetiCraft.requests;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

public class TransactionLogRequests {

    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static String API_BASE = "http://helveticraft-backend:3000";
    private static String API_KEY = "";

    public static void init(String apiBase, String apiKey) {
        if (apiBase != null && !apiBase.isEmpty()) {
            API_BASE = apiBase.replaceAll("/+$", "");
        }
        if (apiKey != null) {
            API_KEY = apiKey;
        }
    }

    public static void loadConfigFromPlugin(org.bukkit.plugin.java.JavaPlugin plugin) {
        String apiBase = plugin.getConfig().getString("initiatives_api_base");
        String apiKey = plugin.getConfig().getString("minecraft_api_key");
        init(apiBase, apiKey);
        plugin.getLogger().info("[TransactionLogRequests] Initialized with API base: " + apiBase);
    }

    public static void prepareRequest(String transactionType, UUID fromUuid, UUID toUuid, long sumTotal) {
        // Build JSON object
        JsonObject json = new JsonObject();
        json.addProperty("transactionType", transactionType);
        json.addProperty("from", fromUuid != null ? fromUuid.toString() : "null");
        json.addProperty("to", toUuid != null ? toUuid.toString() : "null");
        json.addProperty("cents", sumTotal);

        // Send to backend asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(
            Bukkit.getPluginManager().getPlugin("HelvetiCraftPlugin"),
            () -> sendTransactionLog(json.toString())
        );
    }

    private static void sendTransactionLog(String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/transactions/log"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", "00000000-0000-0000-0000-000000000000") // Use government UUID for system transactions
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                Bukkit.getLogger().info("[TransactionLog] Transaction logged successfully");
            } else {
                Bukkit.getLogger().warning("[TransactionLog] Failed to log transaction: " + res.statusCode() + " - " + res.body());
            }
        } catch (IOException | InterruptedException e) {
            Bukkit.getLogger().severe("[TransactionLog] Error sending transaction log: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
