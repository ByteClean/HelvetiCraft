package com.HelvetiCraft.requests;

import com.HelvetiCraft.initiatives.Initiative;
import com.HelvetiCraft.initiatives.PhaseFileManager;
import com.HelvetiCraft.initiatives.PhaseSchedule;
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

    private static PhaseSchedule cachedSchedule = null;

    private static final Map<UUID, Set<String>> playerVotesPhase1 = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> playerVotesPhase2 = new ConcurrentHashMap<>();
    private static final Map<UUID, List<Initiative>> cachedInitiatives = new ConcurrentHashMap<>();

    private static class VoteResponse {
        int initiative_id;
        int normal_votes;
        int votes_for;     // Added
        int votes_against; // Added
        List<VoteEntry> votes;
    }

    private static class VoteEntry {
        int vote_id;
        String created_at;
        int user_id;
        String username;
        boolean vote;
    }

    // --- CRUD (now backed by HTTP) ---

    public static Collection<Initiative> getAllInitiatives(UUID playerId) {
        if (cachedInitiatives.containsKey(playerId)) {
            return cachedInitiatives.get(playerId);
        }

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/all"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] GET /initiatives/all returned status: " + res.statusCode());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                System.out.println("[HelvetiCraft] Response: " + res.body());
                Type listType = new TypeToken<List<Initiative>>() {}.getType();
                List<Initiative> list = GSON.fromJson(res.body(), listType);
                if (list != null) {
                    cachedInitiatives.put(playerId, list);
                    return list;
                }
                return Collections.emptyList();
            } else {
                System.out.println("[HelvetiCraft] Error body: " + res.body());
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
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] GET /initiatives/" + id + " returned status: " + res.statusCode());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                System.out.println("[HelvetiCraft] Response: " + res.body());
                return GSON.fromJson(res.body(), Initiative.class);
            } else {
                System.out.println("[HelvetiCraft] Error body: " + res.body());
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
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] POST /initiatives/create returned status: " + res.statusCode());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // Success
            } else {
                System.out.println("[HelvetiCraft] Error: POST /initiatives/create failed");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
                    .header("x-auth-key", API_KEY)
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

        // Load from cache or file
        // Caching logic: Only fetch from backend if not cached or expired, otherwise use cached schedule.
        if (cachedSchedule == null) {
            cachedSchedule = PhaseFileManager.loadPhaseSchedule();
            if (cachedSchedule == null) {
                cachedSchedule = fetchPhaseScheduleFromBackend(playerId);
                if (cachedSchedule != null) PhaseFileManager.savePhaseSchedule(cachedSchedule);
            }
        }

        // If the phase has reached "abschluss", refresh from backend
        if (cachedSchedule == null || cachedSchedule.isExpired()) {
            System.out.println("[HelvetiCraft] Phase expired → fetching new schedule.");
            cachedSchedule = fetchPhaseScheduleFromBackend(playerId);
            if (cachedSchedule != null) PhaseFileManager.savePhaseSchedule(cachedSchedule);
        }

        // Always return fresh computed phase
        return cachedSchedule != null ? cachedSchedule.getCurrentPhase() : 0;
    }

    /**
     * Fetches the phase schedule from the backend /phases/current endpoint.
     * Maps the backend response to PhaseSchedule fields.
     */
    private static PhaseSchedule fetchPhaseScheduleFromBackend(UUID playerId) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/phases/current"))
                    .GET()
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", playerId != null ? playerId.toString() : "")
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] GET /phases/current returned status: " + res.statusCode());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                System.out.println("[HelvetiCraft] Response: " + res.body());
                // Parse JSON and map to PhaseSchedule
                Map<String, Object> map = GSON.fromJson(res.body(), Map.class);
                PhaseSchedule schedule = new PhaseSchedule();
                // The backend returns ISO strings for start_phase0, start_phase1, ...
                // We'll use start_phase1, start_phase2, start_phase3, and start_phase0+duration_phase0 as abschluss
                String s1 = (String) map.get("start_phase1");
                String s2 = (String) map.get("start_phase2");
                String s3 = (String) map.get("start_phase3");
                String s0 = (String) map.get("start_phase0");
                Double d0 = map.get("duration_phase0") instanceof Number ? ((Number) map.get("duration_phase0")).doubleValue() : null;
                Double d1 = map.get("duration_phase1") instanceof Number ? ((Number) map.get("duration_phase1")).doubleValue() : null;
                Double d2 = map.get("duration_phase2") instanceof Number ? ((Number) map.get("duration_phase2")).doubleValue() : null;
                Double d3 = map.get("duration_phase3") instanceof Number ? ((Number) map.get("duration_phase3")).doubleValue() : null;
                // Parse instants
                schedule.setStart1(Instant.parse(s1));
                schedule.setStart2(Instant.parse(s2));
                schedule.setStart3(Instant.parse(s3));
                // Abschluss = start_phase3 + duration_phase3 (all durations in seconds)
                Instant abschluss = Instant.parse(s3);
                if (d3 != null) abschluss = abschluss.plusSeconds(d3.longValue());
                schedule.setAbschluss(abschluss);
                return schedule;
            } else {
                System.out.println("[HelvetiCraft] Error body: " + res.body());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        Set<String> votedInitiatives = getPlayerVotesPhase1(playerId);
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
                    "phase", 0,
                    "vote", !isRemoving // true to add support, false if removing
            );
            String json = GSON.toJson(votePayload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/vote/" + encoded))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
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


    public static void refreshVotes(UUID playerId) {
        cachedInitiatives.remove(playerId); // Invalidate cache on refresh
        int phase = getCurrentPhase(playerId);
        if (phase == 1) {
            getPlayerVotesPhase1(playerId, true);
        } else if (phase == 2) {
            getPlayerVotesPhase2(playerId, true);
        }
    }

    public static Set<String> getPlayerVotesPhase1(UUID playerId) {
        return getPlayerVotesPhase1(playerId, false);
    }

    public static Set<String> getPlayerVotesPhase1(UUID playerId, boolean forceRefresh) {
        if (!forceRefresh && playerVotesPhase1.containsKey(playerId)) {
            return playerVotesPhase1.get(playerId);
        }

        Set<String> votedTitles = new HashSet<>();
        Collection<Initiative> all = getAllInitiatives(playerId);
        
        String playerName = org.bukkit.Bukkit.getOfflinePlayer(playerId).getName();
        if (playerName == null) return votedTitles;

        for (Initiative initiative : all) {
            if (initiative.getId() == null) continue;
            
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/initiatives/" + initiative.getId() + "/votes"))
                        .GET()
                        .header("x-auth-from", "minecraft")
                        .header("x-auth-key", API_KEY)
                        .header("x-uuid", playerId.toString())
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[HelvetiCraft] GET /initiatives/" + initiative.getId() + "/votes returned status: " + res.statusCode());
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    System.out.println("[HelvetiCraft] Response: " + res.body());
                    VoteResponse vr = GSON.fromJson(res.body(), VoteResponse.class);
                    if (vr != null) {
                        // Update global votes count on the initiative object
                        initiative.setVotes(vr.normal_votes);

                        if (vr.votes != null) {
                            for (VoteEntry entry : vr.votes) {
                                if (playerName.equalsIgnoreCase(entry.username)) {
                                    votedTitles.add(initiative.getTitle());
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("[HelvetiCraft] Error body: " + res.body());
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        playerVotesPhase1.put(playerId, votedTitles);
        return votedTitles;
    }

    // --- Voting Phase 2 (for / against) ---
    public static void votePhase2(UUID playerId, String title, boolean voteFor) {
        // Get initiative ID from title
        String id = getInitiativeIdByTitle(title, playerId);
        if (id == null) return;

        Initiative initiative = getInitiative(title, playerId); // or use proper type if id is String
        if (initiative == null) return;

        Map<String, Boolean> votes = getPlayerVotesPhase2(playerId);
        Boolean previousVote = votes.put(title, voteFor);

        // Local counters updates
        if (previousVote != null) {
            if (previousVote) initiative.decrementVoteFor();
            else initiative.decrementVoteAgainst();
        }

        if (voteFor) initiative.voteFor();
        else initiative.voteAgainst();

        // send POST /initiatives/finalvote/:id
        try {
            String encoded = URLEncoder.encode(id, StandardCharsets.UTF_8);
            Map<String, Object> votePayload = Map.of(
                    "vote", voteFor
            );
            String json = GSON.toJson(votePayload);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/initiatives/finalvote/" + encoded))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", playerId.toString())
                    .header("Content-Type", "application/json")
                    .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.println("[HelvetiCraft] POST /initiatives/finalvote/" + id + " (Phase 2) returned status: " + res.statusCode());
            if (res.statusCode() >= 200 && res.statusCode() < 300) {
                // Success
            } else {
                System.out.println("[HelvetiCraft] Error: POST /initiatives/finalvote/" + id + " failed");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    public static Map<String, Boolean> getPlayerVotesPhase2(UUID playerId) {
        return getPlayerVotesPhase2(playerId, false);
    }

    public static Map<String, Boolean> getPlayerVotesPhase2(UUID playerId, boolean forceRefresh) {
        if (!forceRefresh && playerVotesPhase2.containsKey(playerId)) {
            return playerVotesPhase2.get(playerId);
        }

        Map<String, Boolean> votedTitles = new HashMap<>();
        Collection<Initiative> all = getAllInitiatives(playerId);

        String playerName = org.bukkit.Bukkit.getOfflinePlayer(playerId).getName();
        if (playerName == null) return votedTitles;

        for (Initiative initiative : all) {
            if (initiative.getId() == null) continue;

            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(API_BASE + "/initiatives/" + initiative.getId() + "/votes"))
                        .GET()
                        .header("x-auth-from", "minecraft")
                        .header("x-auth-key", API_KEY)
                        .header("x-uuid", playerId.toString())
                        .header("Content-Type", "application/json")
                        .build();

                HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                System.out.println("[HelvetiCraft] GET /initiatives/" + initiative.getId() + "/votes (Phase 2) returned status: " + res.statusCode());
                if (res.statusCode() >= 200 && res.statusCode() < 300) {
                    System.out.println("[HelvetiCraft] Response: " + res.body());
                    VoteResponse vr = GSON.fromJson(res.body(), VoteResponse.class);
                    if (vr != null) {
                        // Update global votes count
                        initiative.setVotesFor(vr.votes_for);
                        initiative.setVotesAgainst(vr.votes_against);

                        if (vr.votes != null) {
                            for (VoteEntry entry : vr.votes) {
                                if (playerName.equalsIgnoreCase(entry.username)) {
                                    votedTitles.put(initiative.getTitle(), entry.vote);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("[HelvetiCraft] Error body: " + res.body());
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        playerVotesPhase2.put(playerId, votedTitles);
        return votedTitles;
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
