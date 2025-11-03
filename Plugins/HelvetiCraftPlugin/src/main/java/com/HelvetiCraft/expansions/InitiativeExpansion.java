package com.HelvetiCraft.expansions;

import com.HelvetiCraft.requests.InitiativeRequests;
import com.HelvetiCraft.initiatives.Initiative;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InitiativeExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "hcinit"; // UNIQUE for this expansion
    }

    @Override
    public @NotNull String getAuthor() {
        return "HelvetiCraft";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1"; // Updated version
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion loaded across reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
            if (player == null) return "";

            int phase = InitiativeRequests.getCurrentPhase();
            String param = params.toLowerCase();

            switch (param) {
                case "phase":
                    return "Phase " + phase;

                case "initiatives": {
                    // Count initiatives authored by this player (author is stored as name)
                    String playerName = player.getName();
                    if (playerName == null) return "0";
                    long count = InitiativeRequests.getAllInitiatives().stream()
                            .filter(i -> i.getAuthor().equalsIgnoreCase(playerName))
                            .count();
                    return String.valueOf(count);
                }

                case "phase1_votes": {
                    if (phase != 1) return "0";
                    Set<String> votes = InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId());
                    return String.valueOf(votes != null ? votes.size() : 0);
                }

                case "phase2_votes_for": {
                    if (phase != 2) return "0";
                    Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                    long count = votes != null ? votes.values().stream().filter(Boolean::booleanValue).count() : 0;
                    return String.valueOf(count);
                }

                case "phase2_votes_against": {
                    if (phase != 2) return "0";
                    Map<String, Boolean> votes = InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId());
                    long count = votes != null ? votes.values().stream().filter(v -> !v).count() : 0;
                    return String.valueOf(count);
                }

                default:
                    return "";
            }
        }
}
