package com.HelvetiCraft.requests;

import com.HelvetiCraft.initiatives.Initiative;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InitiativeRequests {

    // --- Initiative storage (simulated DB) ---
    private static final Map<String, Initiative> initiatives = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerVotes = new ConcurrentHashMap<>();

    // --- Phase tracking ---
    private static int currentPhase = 1;
    private static long phaseStartTime = System.currentTimeMillis();
    private static int phaseDurationTicks = 3 * 24000; // default: 3 Minecraft days
    private static int totalRounds = 5;

    // --- Static initializer with example initiatives ---
    static {
        createInitiative(new Initiative(
                "Mehr Bäume pflanzen",
                "Wir sollten mehr Bäume im Spawnbereich pflanzen, um die Umwelt zu verschönern.",
                "Admin", 0
        ));
        createInitiative(new Initiative(
                "Community-Event",
                "Ein wöchentliches PvP-Turnier für alle Spieler organisieren.",
                "Admin", 0
        ));
        createInitiative(new Initiative(
                "Neue Shops",
                "Erlaubt Spielern, eigene Shops auf dem Markt zu eröffnen.",
                "Moderator", 0
        ));
        createInitiative(new Initiative(
                "Farbige Häuser",
                "Führt bunte Blöcke ein, um die Bauwerke abwechslungsreicher zu gestalten.",
                "Moderator", 0
        ));
    }

    // --- CRUD ---
    public static Collection<Initiative> getAllInitiatives() {
        return initiatives.values();
    }

    public static Initiative getInitiative(String title) {
        return initiatives.get(title);
    }

    public static void createInitiative(Initiative initiative) {
        initiatives.put(initiative.getTitle(), initiative);
    }

    public static void updateInitiative(Initiative initiative) {
        initiatives.put(initiative.getTitle(), initiative);
    }

    public static void deleteInitiative(String title) {
        initiatives.remove(title);
    }

    public static Set<String> getPlayerVotes(UUID playerId) {
        return playerVotes.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    public static void setPlayerVotes(UUID playerId, Set<String> votes) {
        playerVotes.put(playerId, votes);
    }

    // --- Voting helpers ---
    public static void vote(UUID playerId, String title) {
        Set<String> votes = getPlayerVotes(playerId);
        Initiative initiative = getInitiative(title);
        if (initiative == null) return;

        if (votes.contains(title)) {
            votes.remove(title);
            initiative.decrementVotes();
        } else {
            votes.add(title);
            initiative.incrementVotes();
        }

        updateInitiative(initiative);
    }

    // --- Phase management ---
    public static void advancePhase() {
        long now = System.currentTimeMillis();
        long durationMs = phaseDurationTicks * 50L; // ticks to ms
        if (now - phaseStartTime >= durationMs && currentPhase < totalRounds) {
            currentPhase++;
            phaseStartTime = now;
        }
    }

    public static int getCurrentPhase() {
        return currentPhase;
    }

    public static int getPastPhases() {
        return currentPhase - 1;
    }

    public static long getPhaseEndTime() {
        return phaseStartTime + phaseDurationTicks * 50L;
    }

    public static void setPhaseConfig(int durationDays, int rounds) {
        phaseDurationTicks = durationDays * 24000;
        totalRounds = rounds;
    }
}
