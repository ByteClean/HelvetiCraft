package com.HelvetiCraft.quiz;

import com.HelvetiCraft.Main;
import com.HelvetiCraft.requests.QuizRequests;
import com.HelvetiCraft.util.SafeScheduler;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.*;

public class QuizManager {

    private final Main plugin;
    private final Permission perms;
    private final Economy economy;

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

        RegisteredServiceProvider<Economy> economyProvider =
                Bukkit.getServicesManager().getRegistration(Economy.class);

        this.economy = economyProvider != null ? economyProvider.getProvider() : null;

        if (economy == null) {
            Bukkit.getLogger().severe("[Quiz] ERROR: Vault Economy provider missing!");
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
                ranking.remove(player.getName());
                ranking.add(player.getName());

                Bukkit.broadcastMessage(ChatColor.GREEN
                        + "[Quiz] Richtige Antwort von " + player.getName() + "!");

                // Award 200 CHF
                if (economy != null) {
                    economy.depositPlayer(player, 200.0);
                    player.sendMessage(ChatColor.YELLOW + "Du hast " + ChatColor.GOLD + "200 CHF" 
                            + ChatColor.YELLOW + " gewonnen!");
                    Bukkit.getLogger().info("[Quiz] Awarded 200 CHF to " + player.getName());
                } else {
                    Bukkit.getLogger().warning("[Quiz] Economy not available, could not award 200 CHF");
                }

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

        // Remove all quiz-related groups from all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String group : RANK_GROUPS) {
                perms.playerRemoveGroup("world", p.getName(), group);
            }
        }

        // Assign each player ONLY their correct rank
        for (int i = 0; i < ranking.size() && i < RANK_GROUPS.size(); i++) {

            String playerName = ranking.get(i);
            String newGroup = RANK_GROUPS.get(i);

            // Assign correct rank
            perms.playerAddGroup("world", playerName, newGroup);
            Bukkit.getLogger().info("[Quiz] Assigned group '" + newGroup + "' to player '" + playerName + "'");

            Player p = Bukkit.getPlayer(playerName);
            if (p != null && p.isOnline()) {
                p.sendMessage(ChatColor.YELLOW + "Du hast jetzt den Rang "
                        + ChatColor.AQUA + "[" + newGroup + "]"
                        + ChatColor.YELLOW + "!");
            }
        }
    }
}
