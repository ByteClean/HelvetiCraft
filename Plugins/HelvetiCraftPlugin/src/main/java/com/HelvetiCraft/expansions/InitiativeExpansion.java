package com.HelvetiCraft.expansions;

import com.HelvetiCraft.initiatives.Initiative;
import com.HelvetiCraft.requests.InitiativeRequests;
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
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; // Keep expansion loaded across reloads
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "initiatives": {
                // Count initiatives authored by this player
                long count = InitiativeRequests.getAllInitiatives().stream()
                        .filter(i -> i.getAuthor().equalsIgnoreCase(player.getName()))
                        .count();
                return String.valueOf(count);
            }
            case "phase": {
                // Get the current phase – assuming InitiativeRequests tracks it
                // Otherwise, you may need a separate PhaseManager or InitiativeManager instance
                return "Phase " + InitiativeRequests.getCurrentPhase();
            }
            case "pastphases": {
                return String.valueOf(InitiativeRequests.getPastPhases());
            }
            case "timeleft": {
                long now = System.currentTimeMillis();
                long diff = InitiativeRequests.getPhaseEndTime() - now;
                if (diff <= 0) return "Abgelaufen";
                long minutes = diff / 60000;
                long seconds = (diff / 1000) % 60;
                return minutes + "m " + seconds + "s";
            }
            default:
                return null;
        }
    }
}
