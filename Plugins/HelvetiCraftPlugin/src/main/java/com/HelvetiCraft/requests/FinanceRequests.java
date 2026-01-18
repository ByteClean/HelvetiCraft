package com.HelvetiCraft.requests;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FinanceRequests {
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static String API_BASE = "http://helveticraft-backend:3000";
    private static String API_KEY = "";
    static UUID govUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static void init(String apiBase, String apiKey) {
        if (apiBase != null && !apiBase.isEmpty()) API_BASE = apiBase.replaceAll("/+$$", "");
        if (apiKey != null) API_KEY = apiKey;
    }

    // Call this from your Main.onEnable, passing the plugin instance
    public static void loadConfigFromPlugin(org.bukkit.plugin.java.JavaPlugin plugin) {
        String apiBase = plugin.getConfig().getString("initiatives_api_base");
        String apiKey = plugin.getConfig().getString("minecraft_api_key");
        if (apiBase == null || apiBase.isEmpty()) {
            plugin.getLogger().warning("[FinanceRequests] finances_api_base missing in config.yml!");
        }
        if (apiKey == null || apiKey.isEmpty()) {
            plugin.getLogger().warning("[FinanceRequests] minecraft_api_key missing in config.yml!");
        }
        init(apiBase, apiKey);
        plugin.getLogger().info("[FinanceRequests] Initialized with API base: " + apiBase);
    }

    public static boolean hasAccount(UUID id) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/exists"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("true");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void createAccount(UUID id, long starterCents) {
        try {
            String json = "{\"starterCents\":" + starterCents + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static long getMain(UUID id) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/balance"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                String body = res.body();
                // Try to parse JSON for "main" field
                int idx = body.indexOf("\"main\"");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    String[] parts = sub.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].replaceAll("[^0-9]", "");
                        if (!value.isEmpty()) return Long.parseLong(value);
                    }
                }
                // Fallback: try main_cents for legacy
                idx = body.indexOf("main_cents");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    String[] parts = sub.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].replaceAll("[^0-9]", "");
                        if (!value.isEmpty()) return Long.parseLong(value);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return 0L;
    }

    public static long getSavings(UUID id) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/balance"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                String body = res.body();
                // Try to parse JSON for "savings" field
                int idx = body.indexOf("\"savings\"");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    String[] parts = sub.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].replaceAll("[^0-9]", "");
                        if (!value.isEmpty()) return Long.parseLong(value);
                    }
                }
                // Fallback: try savings_cents for legacy
                idx = body.indexOf("savings_cents");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    String[] parts = sub.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].replaceAll("[^0-9]", "");
                        if (!value.isEmpty()) return Long.parseLong(value);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return 0L;
    }

    public static void setMain(UUID id, long cents) {
        try {
            String json = "{\"cents\":" + cents + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/setMain"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void setSavings(UUID id, long cents) {
        try {
            String json = "{\"cents\":" + cents + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/setSavings"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static void addToMain(UUID id, long cents) {
        try {
            String json = "{\"cents\":" + cents + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/addToMain"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", id.toString())
                    .header("Content-Type", "application/json")
                    .build();
            CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static boolean transferMain(UUID from, UUID to, long cents) {
        try {
            String json = String.format("{\"from\":\"%s\",\"to\":\"%s\",\"cents\":%d}", from, to, cents);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/transfer"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", from.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("true");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static long getTotalNetWorthCents() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/totalNetWorth"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", govUUID.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("totalNetWorth")) {
                String body = res.body();
                int idx = body.indexOf("totalNetWorth");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    String[] parts = sub.split(":");
                    if (parts.length > 1) {
                        String value = parts[1].replaceAll("[^0-9]", "");
                        if (!value.isEmpty()) return Long.parseLong(value);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return 0L;
    }

    public static Set<UUID> getKnownPlayers() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/knownPlayers"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", govUUID.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("players")) {
                Set<UUID> players = new HashSet<>();
                String body = res.body();
                // Parse JSON array: "players":["uuid1","uuid2",...]
                int startIdx = body.indexOf("\"players\"");
                if (startIdx != -1) {
                    int arrStart = body.indexOf("[", startIdx);
                    int arrEnd = body.indexOf("]", arrStart);
                    if (arrStart != -1 && arrEnd != -1) {
                        String arrayContent = body.substring(arrStart + 1, arrEnd);
                        String[] uuidStrings = arrayContent.split(",");
                        for (String uuidStr : uuidStrings) {
                            String cleaned = uuidStr.replaceAll("[\"\\s]", "");
                            if (!cleaned.isEmpty()) {
                                try {
                                    players.add(UUID.fromString(cleaned));
                                } catch (IllegalArgumentException e) {
                                    // Skip invalid UUIDs
                                }
                            }
                        }
                    }
                }
                return players;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return new HashSet<>();
    }

    public static long getPeriodIncome(UUID id) {
        // Not supported by backend
        throw new UnsupportedOperationException("getPeriodIncome not supported by backend");
    }

    public static void resetPeriodIncome(UUID id) {
        // Not supported by backend
        throw new UnsupportedOperationException("resetPeriodIncome not supported by backend");
    }

    public static String getUsername(UUID id) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/finances/" + id + "/username"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", govUUID.toString())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("username")) {
                String body = res.body();
                int idx = body.indexOf("\"username\"");
                if (idx != -1) {
                    String sub = body.substring(idx);
                    int colonIdx = sub.indexOf(":");
                    int quoteStart = sub.indexOf("\"", colonIdx);
                    int quoteEnd = sub.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        return sub.substring(quoteStart + 1, quoteEnd);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return null;
    }
}