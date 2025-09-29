package com.HelvetiCraft.expansions;

import com.HelvetiCraft.initiatives.Initiative;
import com.HelvetiCraft.initiatives.InitiativeManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InitiativeExpansion extends PlaceholderExpansion {

    private final InitiativeManager initiatives;

    public InitiativeExpansion(InitiativeManager initiatives) {
        this.initiatives = initiatives;
    }

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
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        UUID id = player.getUniqueId();

        switch (params.toLowerCase()) {
            case "initiatives": {
                long count = initiatives.getInitiatives().values().stream()
                        .filter(i -> i.getAuthor().equalsIgnoreCase(player.getName()))
                        .count();
                return String.valueOf(count);
            }
            case "phase": {
                return "Phase " + initiatives.getCurrentPhase();
            }
            case "pastphases": {
                return String.valueOf(initiatives.getPastPhases());
            }
            case "timeleft": {
                long now = System.currentTimeMillis();
                long diff = initiatives.getPhaseEndTime() - now;
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
