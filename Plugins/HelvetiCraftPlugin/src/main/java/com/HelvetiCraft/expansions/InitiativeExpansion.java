package com.HelvetiCraft.expansions;

import com.HelvetiCraft.requests.InitiativeRequests;
import com.HelvetiCraft.initiatives.Initiative;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

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

        switch (params.toLowerCase()) {
            case "phase":
                return "Phase " + phase;

            case "initiatives": {
                // Count initiatives authored by this player
                long count = InitiativeRequests.getAllInitiatives().stream()
                        .filter(i -> i.getAuthor().equalsIgnoreCase(player.getName()))
                        .count();
                return String.valueOf(count);
            }

            case "phase1_votes": {
                if (phase != 1) return "0";
                return String.valueOf(InitiativeRequests.getPlayerVotesPhase1(player.getUniqueId()).size());
            }

            case "phase2_votes_for": {
                if (phase != 2) return "0";
                return String.valueOf(
                        InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId()).values().stream()
                                .filter(v -> v)
                                .count()
                );
            }

            case "phase2_votes_against": {
                if (phase != 2) return "0";
                return String.valueOf(
                        InitiativeRequests.getPlayerVotesPhase2(player.getUniqueId()).values().stream()
                                .filter(v -> !v)
                                .count()
                );
            }

            default:
                return null;
        }
    }
}
