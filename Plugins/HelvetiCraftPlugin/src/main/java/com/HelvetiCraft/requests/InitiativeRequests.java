package com.HelvetiCraft.requests;

import com.HelvetiCraft.initiatives.Initiative;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InitiativeRequests {

    private static final Gson GSON = new Gson();
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();

    private static String API_BASE = "http://localhost:8080";
    private static String API_KEY = "";

    public static void init(String apiBase, String apiKey) {
        if (apiBase != null && !apiBase.isEmpty()) API_BASE = apiBase.replaceAll("/+$", "");
        if (apiKey != null) API_KEY = apiKey;
    }

    private static final Map<UUID, Set<String>> playerVotesPhase1 = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> playerVotesPhase2 = new ConcurrentHashMap<>();

    // --- CRUD (now backed by HTTP) ---

    public static Collection<Initiative> getAllInitiatives(UUID playerId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/all"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                Type listType = new TypeToken<List<Initiative>>() {}.getType();
                List<Initiative> list = GSON.fromJson(res.body(), listType);
                return list != null ? list : Collections.emptyList();
            } else {
                // Non-2xx - return empty list
                return Collections.emptyList();
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
    }

    public static Initiative getInitiative(String title, UUID playerId) {
        // Get the initiative ID using the helper
        String id = getInitiativeIdByTitle(title, playerId);
        if (id == null) return null;

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/" + URLEncoder.encode(id, StandardCharsets.UTF_8)))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                return GSON.fromJson(res.body(), Initiative.class);
            } else {
                System.out.println("[HelvetiCraft] GET /initiatives/" + id + " failed with status " + res.statusCode());
                return null;
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public static void createInitiative(Initiative initiative, UUID playerId) {
        try {
            // ensure phase and created_at present
            if (initiative.getPhase() == 0) initiative.setPhase(1);
            if (initiative.getCreatedAt() == null || initiative.getCreatedAt().trim().isEmpty()) {
                initiative.setCreatedAt(Instant.now().toString());
            }

            String json = GSON.toJson(Map.of(
                    "author", initiative.getAuthor(),
                    "title", initiative.getTitle(),
                    "description", initiative.getDescription()
            ));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/create"))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void deleteInitiative(String title, UUID playerId) {
        try {
            // Get correct ID using raw title
            String initiativeId = getInitiativeIdByTitle(title, playerId);
            if (initiativeId == null) {
                System.out.println("[HelvetiCraft] Could not find initiative ID for title: " + title);
                return;
            }

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/del/" + initiativeId))
                    .DELETE()
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            System.out.println("[HelvetiCraft] DELETE /initiatives/del/" + initiativeId +
                    " returned " + res.statusCode());
            System.out.println("[HelvetiCraft] Response body: " + res.body());

        } catch (Exception e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }



    // --- Phase Management (status endpoint) ---

    public static int getCurrentPhase(UUID playerId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiative/status"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // Expect the body to be a number or a small JSON like {"phase":2}
                String body = res.body().trim();
                try {
                    return Integer.parseInt(body);
                } catch (NumberFormatException nfe) {
                    try {
                        Map<?,?> map = GSON.fromJson(body, Map.class);
                        Object p = map.get("phase");
                        if (p instanceof Number) return ((Number) p).intValue();
                        if (p instanceof String) return Integer.parseInt((String) p);
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // fallback to phase 1 if unknown
        return 1;
    }

    public static boolean canCreateInitiative(UUID playerId, String playerName) {
        int phase = getCurrentPhase(playerId);
        if (phase != 1) return false;

        // Check whether player already created in phase 1
        Collection<Initiative> all = getAllInitiatives(playerId);
        for (Initiative i : all) {
            if (i.getAuthor() != null && i.getAuthor().equalsIgnoreCase(playerName) && i.getPhase() == 1)
                return false;
        }
        return true;
    }

    // --- Voting Phase 1 (support votes) ---
    public static void votePhase1(UUID playerId, String title) {
        // Find the initiative with the given title
        Collection<Initiative> all = getAllInitiatives(playerId);
        Initiative initiative = all.stream()
                .filter(i -> i.getTitle().equalsIgnoreCase(title))
                .findFirst()
                .orElse(null);
        if (initiative == null) return;

        String id = initiative.getId(); // get the ID for HTTP calls

        // prevent author from voting for own initiative
        if (initiative.getAuthor() != null && initiative.getAuthor().equalsIgnoreCase(playerId.toString())) return;

        // toggle local memorized votes for UI immediacy (this exists only locally)
        Set<String> votedInitiatives = playerVotesPhase1.computeIfAbsent(playerId, k -> new HashSet<>());
        boolean isRemoving = votedInitiatives.contains(title);
        if (isRemoving) {
            votedInitiatives.remove(title);
        } else {
            votedInitiatives.add(title);
        }

        // send POST /initiatives/vote/:id with payload { playerId, phase:1, vote: true/false }
        try {
            String encoded = URLEncoder.encode(id, StandardCharsets.UTF_8);
            Map<String, Object> votePayload = Map.of(
                    "playerId", playerId.toString(),
                    "phase", 1,
                    "vote", !isRemoving // true to add support, false if removing
            );
            String json = GSON.toJson(votePayload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/vote/" + encoded))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] Vote Phase 1 sent for initiative ID " + id + ", status: " + res.statusCode());
            System.out.println("[HelvetiCraft] Response body: " + res.body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }


    public static Set<String> getPlayerVotesPhase1(UUID playerId) {
        return playerVotesPhase1.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    // --- Voting Phase 2 (for / against) ---
    public static void votePhase2(UUID playerId, String title, boolean voteFor) {
        // Get initiative ID from title
        String id = getInitiativeIdByTitle(title, playerId);
        if (id == null) return;

        Initiative initiative = getInitiative(title, playerId); // or use proper type if id is String
        if (initiative == null) return;

        Map<String, Boolean> votes = playerVotesPhase2.computeIfAbsent(playerId, k -> new HashMap<>());
        Boolean previousVote = votes.put(title, voteFor);

        // Local counters updates
        if (previousVote != null) {
            if (previousVote) initiative.decrementVoteFor();
            else initiative.decrementVoteAgainst();
        }

        if (voteFor) initiative.voteFor();
        else initiative.voteAgainst();

        // send POST /initiatives/vote/:id
        try {
            String encoded = URLEncoder.encode(id, StandardCharsets.UTF_8);
            Map<String, Object> votePayload = Map.of(
                    "playerId", playerId.toString(),
                    "phase", 2,
                    "vote", voteFor
            );
            String json = GSON.toJson(votePayload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/vote/" + encoded))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("MINECRAFT_API_KEY", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<Void> res = CLIENT.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static Map<String, Boolean> getPlayerVotesPhase2(UUID playerId) {
        return playerVotesPhase2.computeIfAbsent(playerId, k -> new HashMap<>());
    }

    // --- Helper method ---
    private static String getInitiativeIdByTitle(String title, UUID playerId) {
        return getAllInitiatives(playerId).stream()
                .filter(i -> i.getTitle().equalsIgnoreCase(title))
                .map(Initiative::getId)
                .findFirst()
                .orElse(null);
    }
}
