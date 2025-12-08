package com.HelvetiCraft.requests;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

/**
 * Backend notifier for admin upgrades/downgrades.
 * Keeps the logger.info calls and additionally posts a JSON payload to the backend
 * so the backend can forward it to the Discord bot.
 */
public class AdminRequests {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static Logger logger;
    private static JavaPlugin pluginRef;

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        pluginRef = plugin;
    }

    public static void logUpgrade(UUID playerId, String playerName, String reason, long expiresAtMillis) {
        String expires = DF.format(Instant.ofEpochMilli(expiresAtMillis));
        logger.info("[AdminRequests] Upgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' expires=" + expires);

        if (pluginRef == null) return;

        // perform network call asynchronously to avoid blocking main thread
        pluginRef.getServer().getScheduler().runTaskAsynchronously(pluginRef, () -> {
            try {
                String base = pluginRef.getConfig().getString("initiatives_api_base", "http://127.0.0.1:3000");
                String endpoint = base.replaceAll("/+$", "") + "/discord-logging/upgrade-admin";

                String json = buildJsonPayload(playerId, playerName, reason, expires, "Player", "Administrator", null);
                sendPost(endpoint, json);
            } catch (Exception e) {
                logger.warning("[AdminRequests] Failed to send upgrade notification to backend: " + e.getMessage());
            }
        });
    }

    public static void logDowngrade(UUID playerId, String playerName, String reason) {
        String now = DF.format(Instant.now());
        logger.info("[AdminRequests] Downgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' at=" + now);

        if (pluginRef == null) return;

        pluginRef.getServer().getScheduler().runTaskAsynchronously(pluginRef, () -> {
            try {
                String base = pluginRef.getConfig().getString("initiatives_api_base", "http://127.0.0.1:3000");
                String endpoint = base.replaceAll("/+$", "") + "/discord-logging/downgrade-admin";

                // for downgrade we include the current time as "at" field
                String at = DF.format(Instant.now());
                String json = buildJsonPayload(playerId, playerName, reason, null, "Administrator", "Player", at);
                sendPost(endpoint, json);
            } catch (Exception e) {
                logger.warning("[AdminRequests] Failed to send downgrade notification to backend: " + e.getMessage());
            }
        });
    }

    private static String buildJsonPayload(UUID playerId, String playerName, String reason, String expiresOrNull, String previousRole, String newRole, String atOrNull) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"username\":");
        sb.append('"').append(playerName != null ? escapeJson(playerName + "#" + playerId.toString().substring(0, 4)) : "Unknown#0000").append('"').append(',');
        sb.append("\"minecraft_name\":");
        sb.append('"').append(escapeJson(playerName != null ? playerName : "Unknown")).append('"').append(',');
        sb.append("\"previous_role\":");
        sb.append('"').append(escapeJson(previousRole != null ? previousRole : "")).append('"').append(',');
        sb.append("\"new_role\":");
        sb.append('"').append(escapeJson(newRole != null ? newRole : "")).append('"').append(',');
        if (expiresOrNull != null) {
            sb.append("\"duration\":");
            sb.append('"').append(escapeJson(expiresOrNull)).append('"').append(',');
        } else if (atOrNull != null) {
            sb.append("\"duration\":");
            sb.append('"').append(escapeJson(atOrNull)).append('"').append(',');
        } else {
            sb.append("\"duration\":\"\"").append(',');
        }
        sb.append("\"reason\":");
        sb.append('"').append(escapeJson(reason != null ? reason : "")).append('"');
        sb.append('}');
        return sb.toString();
    }

    private static void sendPost(String endpoint, String json) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        conn.setDoOutput(true);

        byte[] out = json.getBytes("utf-8");
        conn.setFixedLengthStreamingMode(out.length);
        conn.connect();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(out);
        }

        int status = conn.getResponseCode();
        if (status < 200 || status >= 300) {
            logger.warning("[AdminRequests] Backend returned status " + status + " for endpoint " + endpoint);
        }
        conn.disconnect();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
