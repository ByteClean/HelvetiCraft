package com.HelvetiCraft.quiz;

import java.util.List;

public class QuizQuestion {

    private final String question;
    private final List<String> answers;

    public QuizQuestion(String question, List<String> answers) {
        this.question = question;
        this.answers = answers;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getAnswers() {
        return answers;
    }
}
