package com.HelvetiCraft.requests;

import com.HelvetiCraft.quiz.QuizQuestion;
import com.google.gson.Gson;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class QuizRequests {

    private static final Gson GSON = new Gson();


    // Backend API base URL (adjust if needed)
    private static final String BACKEND_API_URL = "http://localhost:3000/quiz";

    public static QuizQuestion fetchNextQuestion() {
        try {
            URL url = new URL(BACKEND_API_URL + "/question");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sb.append(output);
            }
            conn.disconnect();

            QuizJson parsed = GSON.fromJson(sb.toString(), QuizJson.class);
            return new QuizQuestion(parsed.question, parsed.answers);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String sendRankingUpdate(String playerName, int rankPosition) {
        try {
            URL url = new URL(BACKEND_API_URL + "/ranking");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String jsonInput = GSON.toJson(new RankingRequest(playerName, rankPosition));
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
            conn.disconnect();
            return response.toString();
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
