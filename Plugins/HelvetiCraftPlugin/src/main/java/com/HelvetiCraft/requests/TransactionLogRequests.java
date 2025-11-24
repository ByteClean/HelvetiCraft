package com.HelvetiCraft.requests;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;

import java.util.UUID;

public class TransactionLogRequests {

    public static void prepareRequest(String transactionType, UUID fromUuid, UUID toUuid, long sumTotal) {

        // Build JSON object
        JsonObject json = new JsonObject();
        json.addProperty("transactionType", transactionType);

        // Convert UUIDs to string OR use "null"
        json.addProperty("from", fromUuid != null ? fromUuid.toString() : "null");
        json.addProperty("to", toUuid != null ? toUuid.toString() : "null");

        json.addProperty("sum", String.valueOf(sumTotal));

        // Log to MC console
        Bukkit.getLogger().info("[Finance] Dummy transaction log prepared:");
        Bukkit.getLogger().info(json.toString());
    }
}
