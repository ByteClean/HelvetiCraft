package com.HelvetiCraft.quiz;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.QuizRequests;
import com.HelvetiCraft.util.SafeScheduler;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class QuizManager {

    private final Main plugin;
    private final Permission perms;

    private QuizQuestion currentQuestion;
    private boolean hasWinner = false;

    // Stores winners in order
    private final List<String> ranking = new ArrayList<>();

    private static final List<String> RANK_GROUPS = Arrays.asList(
            "quizmeister",
            "raetselkoenig",
            "denkmeister",
            "schlaumeier"
    );

    public QuizManager(Main plugin) {
        this.plugin = plugin;

        RegisteredServiceProvider<Permission> rsp =
                Bukkit.getServicesManager().getRegistration(Permission.class);

        this.perms = rsp != null ? rsp.getProvider() : null;

        if (perms == null) {
            Bukkit.getLogger().severe("[Quiz] ERROR: Vault Permission provider missing!");
        }
    }

    // Start quiz cycle
    public void start() {
        askNewQuestion();

        // Schedule every 10 minutes
        SafeScheduler.runRepeating(plugin, this::askNewQuestion, 20L * 600, 20L * 600);
    }

    private void askNewQuestion() {
        currentQuestion = QuizRequests.fetchNextQuestion();
        hasWinner = false;

        Bukkit.broadcastMessage(ChatColor.GOLD + "[Quiz] Neue Frage:");
        Bukkit.broadcastMessage(ChatColor.AQUA + currentQuestion.getQuestion());

        // Logging
        Bukkit.getLogger().info("[Quiz] New quiz question asked: " + currentQuestion.getQuestion());
        Bukkit.getLogger().info("[Quiz] Possible answers: " + String.join(", ", currentQuestion.getAnswers()));
    }

    public void handleAnswer(Player player, String message) {
        if (currentQuestion == null) return;
        if (hasWinner) return;

        // Check correct answer
        for (String ans : currentQuestion.getAnswers()) {
            if (message.equalsIgnoreCase(ans)) {

                hasWinner = true;
                ranking.add(player.getName());

                Bukkit.broadcastMessage(ChatColor.GREEN
                        + "[Quiz] Richtige Antwort von " + player.getName() + "!");

                // Logging
                Bukkit.getLogger().info("[Quiz] Player '" + player.getName() + "' answered correctly.");

                updateRanks();

                int rankPos = ranking.indexOf(player.getName()) + 1;
                QuizRequests.sendRankingUpdate(player.getName(), rankPos);

                Bukkit.getLogger().info("[Quiz] Ranking update sent for " +
                        player.getName() + " (Rank " + rankPos + ")");

                return;
            }
        }
    }

    private void updateRanks() {
        if (perms == null) return;

        // Remove all quiz groups first
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String group : RANK_GROUPS) {
                perms.playerRemoveGroup("world", p.getName(), group);
            }
        }

        // Assign top 4
        for (int i = 0; i < ranking.size() && i < RANK_GROUPS.size(); i++) {

            String playerName = ranking.get(i);
            String group = RANK_GROUPS.get(i);

            perms.playerAddGroup("world", playerName, group);

            Player p = Bukkit.getPlayer(playerName);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.YELLOW + "Du hast den Rang "
                        + ChatColor.AQUA + "[" + group + "] "
                        + ChatColor.YELLOW + "erhalten!");
            }

            // Logging rank assignment
            Bukkit.getLogger().info("[Quiz] Assigned group '" + group +
                    "' to player '" + playerName + "'");
        }
    }
}
