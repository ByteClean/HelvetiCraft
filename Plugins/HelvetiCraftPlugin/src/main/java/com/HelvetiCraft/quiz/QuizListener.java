package com.HelvetiCraft.quiz;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class QuizListener implements Listener {

    private final QuizManager quizManager;

    public QuizListener(QuizManager quizManager) {
        this.quizManager = quizManager;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        quizManager.handleAnswer(event.getPlayer(), event.getMessage());
    }
}
