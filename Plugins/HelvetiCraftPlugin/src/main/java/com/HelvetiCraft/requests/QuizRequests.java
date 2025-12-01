package com.HelvetiCraft.requests;

import com.HelvetiCraft.quiz.QuizQuestion;
import com.google.gson.Gson;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QuizRequests {

    private static final Gson GSON = new Gson();

    public static QuizQuestion fetchNextQuestion() {

        // Dummy list of JSON questions
        List<String> dummyJsonQuestions = Arrays.asList(
                "{ \"question\": \"Wer ist Admin?\", \"answers\": [\"A\", \"B\", \"C\"] }",
                "{ \"question\": \"Wie viele Planeten hat unser System?\", \"answers\": [\"8\", \"9\", \"7\"] }",
                "{ \"question\": \"Welche Sprache ist dieses Plugin geschrieben?\", \"answers\": [\"Java\", \"Kotlin\", \"Python\"] }"
        );

        String json = dummyJsonQuestions.get(new Random().nextInt(dummyJsonQuestions.size()));

        QuizJson parsed = GSON.fromJson(json, QuizJson.class);

        return new QuizQuestion(parsed.question, parsed.answers);
    }

    public static String sendRankingUpdate(String playerName, int rankPosition) {
        RankingResult result = new RankingResult(playerName, rankPosition, "ok");
        return GSON.toJson(result);
    }

    // JSON models
    private static class QuizJson {
        String question;
        List<String> answers;
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
