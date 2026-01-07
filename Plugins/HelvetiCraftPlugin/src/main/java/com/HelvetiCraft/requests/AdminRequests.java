package com.HelvetiCraft.requests;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    public static void init(JavaPlugin plugin) {
        logger = plugin.getLogger();
        pluginRef = plugin;
    }

    public static void logUpgrade(UUID playerId, String playerName, String reason, long expiresAtMillis) {
        String expires = DF.format(Instant.ofEpochMilli(expiresAtMillis));
        logger.info("[AdminRequests] Upgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' expires=" + expires);

        if (pluginRef == null) return;

        // read config synchronously, then perform network call on executor to avoid Folia scheduler issues
        final String base = pluginRef.getConfig().getString("initiatives_api_base", "http://127.0.0.1:3000");
        final String endpoint = base.replaceAll("/+$", "") + "/discord-logging/upgrade-admin";
        final String json = buildJsonPayload(playerId, playerName, reason, expires, "Player", "Administrator", null);

        executor.submit(() -> {
            try {
                // build payload map and send with HttpClient (consistent with InitiativeRequests)
                var payload = new java.util.HashMap<String, Object>();
                payload.put("playername", playerName != null ? playerName : "Unknown");
                payload.put("playerId", playerId != null ? playerId.toString() : "");
                payload.put("username", playerName != null ? playerName + "#" + (playerId != null ? playerId.toString().substring(0, 4) : "0000") : "Unknown#0000");
                payload.put("minecraft_name", playerName != null ? playerName : "Unknown");
                payload.put("previous_role", "Player");
                payload.put("new_role", "Administrator");
                payload.put("expires", expires);
                payload.put("reason", reason != null ? reason : "");

                sendPostWithHttpClient(endpoint, payload, playerId);
            } catch (Exception e) {
                logger.warning("[AdminRequests] Failed to send upgrade notification to backend: " + e.getMessage());
            }
        });
    }

    public static void logDowngrade(UUID playerId, String playerName, String reason) {
        String now = DF.format(Instant.now());
        logger.info("[AdminRequests] Downgrade: player=" + playerName + " (" + playerId + ") reason='" + reason + "' at=" + now);

        if (pluginRef == null) return;

        final String base = pluginRef.getConfig().getString("initiatives_api_base", "http://127.0.0.1:3000");
        final String endpoint = base.replaceAll("/+$", "") + "/discord-logging/downgrade-admin";
        final String at = DF.format(Instant.now());
        final String json = buildJsonPayload(playerId, playerName, reason, null, "Administrator", "Player", at);

        executor.submit(() -> {
            try {
                var payload = new java.util.HashMap<String, Object>();
                payload.put("playername", playerName != null ? playerName : "Unknown");
                payload.put("playerId", playerId != null ? playerId.toString() : "");
                payload.put("username", playerName != null ? playerName + "#" + (playerId != null ? playerId.toString().substring(0, 4) : "0000") : "Unknown#0000");
                payload.put("minecraft_name", playerName != null ? playerName : "Unknown");
                payload.put("previous_role", "Administrator");
                payload.put("new_role", "Player");
                payload.put("at", at);
                payload.put("reason", reason != null ? reason : "");

                sendPostWithHttpClient(endpoint, payload, playerId);
            } catch (Exception e) {
                logger.warning("[AdminRequests] Failed to send downgrade notification to backend: " + e.getMessage());
            }
        });
    }

    private static String buildJsonPayload(UUID playerId, String playerName, String reason, String expiresOrNull, String previousRole, String newRole, String atOrNull) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        // include fields backend expects: playername and playerId
        sb.append("\"playername\":");
        sb.append('"').append(escapeJson(playerName != null ? playerName : "Unknown")).append('"').append(',');
        sb.append("\"playerId\":");
        sb.append('"').append(playerId != null ? escapeJson(playerId.toString()) : "").append('"').append(',');

        sb.append("\"username\":");
        sb.append('"').append(playerName != null ? escapeJson(playerName + "#" + playerId.toString().substring(0, 4)) : "Unknown#0000").append('"').append(',');

        sb.append("\"minecraft_name\":");
        sb.append('"').append(escapeJson(playerName != null ? playerName : "Unknown")).append('"').append(',');
        sb.append("\"previous_role\":");
        sb.append('"').append(escapeJson(previousRole != null ? previousRole : "")).append('"').append(',');
        sb.append("\"new_role\":");
        sb.append('"').append(escapeJson(newRole != null ? newRole : "")).append('"').append(',');

        // backend expects either `expires` or `at` fields; include whichever is provided
        if (expiresOrNull != null) {
            sb.append("\"expires\":");
            sb.append('"').append(escapeJson(expiresOrNull)).append('"').append(',');
        } else if (atOrNull != null) {
            sb.append("\"at\":");
            sb.append('"').append(escapeJson(atOrNull)).append('"').append(',');
        }

        sb.append("\"reason\":");
        sb.append('"').append(escapeJson(reason != null ? reason : "")).append('"');
        sb.append('}');
        return sb.toString();
    }

    private static void sendPost(String endpoint, String json, UUID playerId) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; utf-8");
        // Add auth headers used elsewhere in the plugin/backend
        try {
            if (pluginRef != null) {
                String apiKey = pluginRef.getConfig().getString("minecraft_api_key", "");
                if (apiKey != null && !apiKey.isEmpty()) conn.setRequestProperty("x-auth-key", apiKey);
            }
        } catch (Exception ignored) {}
        conn.setRequestProperty("x-auth-from", "minecraft");
        if (playerId != null) conn.setRequestProperty("x-uuid", playerId.toString());
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

    private static void sendPostWithHttpClient(String endpoint, java.util.Map<String, Object> payload, UUID playerId) {
        try {
            String json = GSON.toJson(payload);

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .header("x-auth-from", "minecraft")
                    .header("Content-Type", "application/json");

            try {
                if (pluginRef != null) {
                    String apiKey = pluginRef.getConfig().getString("minecraft_api_key", "");
                    if (apiKey != null && !apiKey.isEmpty()) builder.header("x-auth-key", apiKey);
                }
            } catch (Exception ignored) {}

            if (playerId != null) builder.header("x-uuid", playerId.toString());

            HttpRequest req = builder.build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            int status = res.statusCode();
            if (status < 200 || status >= 300) {
                logger.warning("[AdminRequests] Backend returned status " + status + " for endpoint " + endpoint + " body: " + res.body());
            }
        } catch (Exception e) {
            logger.warning("[AdminRequests] sendPostWithHttpClient failed: " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
