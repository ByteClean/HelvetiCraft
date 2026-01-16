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

    public static void init(String apiBase, String apiKey) {
        if (apiBase != null && !apiBase.isEmpty()) API_BASE = apiBase.replaceAll("/+$$", "");
        if (apiKey != null) API_KEY = apiKey;
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
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("main_cents")) {
                String body = res.body();
                int idx = body.indexOf("main_cents");
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
            if (res.statusCode() >= 200 && res.statusCode() < 300 && res.body().contains("savings_cents")) {
                String body = res.body();
                int idx = body.indexOf("savings_cents");
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
        // Not supported by backend for all users at once
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
}