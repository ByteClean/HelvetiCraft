package com.HelvetiCraft.requests;

import com.HelvetiCraft.quiz.QuizQuestion;
import com.google.gson.Gson;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;



import java.util.UUID;
import java.io.FileInputStream;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import static org.bukkit.Bukkit.getLogger;

public class QuizRequests {


    private static final Gson GSON = new Gson();
    private static String BACKEND_API_URL = "http://helveticraft-backend:3000/quiz";
    private static final HttpClient CLIENT = HttpClient.newBuilder().build();
    private static String API_KEY = "";
    private static UUID PLAYER_UUID = null;

    /**
     * Initialize QuizRequests using config file and parameters.
     * If apiBase is null or empty, will try to read from config.yml (initiatives_api_base).
     */
    public static void init(String apiBase, String apiKey, UUID playerUuid) {
        String base = apiBase;
        if (base == null || base.isEmpty()) {
            // Try to read from config.yml
            try (InputStream input = new FileInputStream("src/main/resources/config.yml")) {
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(input);
                Object fromConfig = config.get("initiatives_api_base");
                if (fromConfig != null) base = fromConfig.toString();
            } catch (Exception e) {
                System.err.println("[QuizRequests] Could not read initiatives_api_base from config.yml: " + e.getMessage());
            }
        }
        if (base != null && !base.isEmpty()) BACKEND_API_URL = base.replaceAll("/+$", "") + "/quiz";
        if (apiKey != null) API_KEY = apiKey;
        getLogger().info("CONNECTION TO BACKEND FOR QUIZ" + API_KEY + "and BACKEND API URL" + BACKEND_API_URL);
        if (playerUuid != null) PLAYER_UUID = playerUuid;
    }

    public static QuizQuestion fetchNextQuestion() {
        int maxRetries = 5;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_API_URL + "/quiz/question"))
                    .GET()
                    .header("Accept", "application/json")
                    .header("x-auth-from", "minecraft")
                    .header("x-auth-key", API_KEY)
                    .header("x-uuid", PLAYER_UUID != null ? PLAYER_UUID.toString() : "")
                    .build();

                HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() < 200 || res.statusCode() >= 300) {
                    throw new RuntimeException("Failed : HTTP error code : " + res.statusCode());
                }
                QuizJson parsed = GSON.fromJson(res.body(), QuizJson.class);

                // Check if question is null and retry if so
                if (parsed == null || parsed.question == null) {
                    retryCount++;
                    System.err.println("[QuizRequests] Question is null, retry attempt " + retryCount + "/" + maxRetries);
                    if (retryCount < maxRetries) {
                        Thread.sleep(500); // Wait 500ms before retrying
                        continue;
                    }
                    throw new RuntimeException("Question is null after " + maxRetries + " retries");
                }

                return new QuizQuestion(parsed.question, parsed.answers);
            } catch (Exception e) {
                retryCount++;
                System.err.println("[QuizRequests] Error fetching question (attempt " + retryCount + "/" + maxRetries + "): " + e.getMessage());
                if (retryCount >= maxRetries) {
                    e.printStackTrace();
                    System.err.println("[QuizRequests] Failed to fetch question after " + maxRetries + " attempts. Returning null to prevent plugin crash.");
                    return null;
                }
                try {
                    Thread.sleep(500); // Wait 500ms before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }


    public static String sendRankingUpdate(String playerName, int rankPosition) {
        try {
            String jsonInput = GSON.toJson(new RankingRequest(playerName, rankPosition));
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BACKEND_API_URL + "/quiz/ranking"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonInput))
                .header("Content-Type", "application/json")
                .header("x-auth-from", "minecraft")
                .header("x-auth-key", API_KEY)
                .header("x-uuid", PLAYER_UUID != null ? PLAYER_UUID.toString() : "")
                .build();

            HttpResponse<String> res = CLIENT.send(req, HttpResponse.BodyHandlers.ofString());
            return res.body();
        } catch (Exception e) {
            e.printStackTrace();
            return GSON.toJson(new RankingResult(playerName, rankPosition, "error"));
        }
    }


    // JSON models
    private static class QuizJson {
        String question;
        List<String> answers;
    }

    private static class RankingRequest {
        String player;
        int rank;
        RankingRequest(String player, int rank) {
            this.player = player;
            this.rank = rank;
        }
    }

    private static class RankingResult {
        String player;
        int rank;
        String status;

        RankingResult(String player, int rank, String status) {
            this.player = player;
            this.rank = rank;
            this.status = status;
        }
    }
}
