package com.HelvetiCraft.requests;

import com.HelvetiCraft.initiatives.Initiative;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InitiativeRequests {

    private static final Map<String, Initiative> initiatives = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> playerVotesPhase1 = new ConcurrentHashMap<>();
    private static final Map<UUID, Map<String, Boolean>> playerVotesPhase2 = new ConcurrentHashMap<>();
    private static final Set<String> createdPhase1 = ConcurrentHashMap.newKeySet();

    private static int currentPhase = 2; // default to phase 1

    static {
        // --- Dummy initiatives ---
        createDummyInitiative("Umweltschutz", "Initiative für saubere Luft", "Alice");
        createDummyInitiative("Bildung", "Mehr Schulen und Lehrmittel", "Bob");
        createDummyInitiative("Verkehr", "ÖPNV ausbauen", "Charlie");
        createDummyInitiative("Digitalisierung", "Schnelles Internet für alle", "Diana");
        createDummyInitiative("Gesundheit", "Bessere Krankenhäuser", "Eve");
    }

    private static void createDummyInitiative(String title, String desc, String author) {
        Initiative i = new Initiative(title, desc, author);
        i.setPhase(1); // always start in phase 1
        initiatives.put(title, i);
        createdPhase1.add(author);
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
        createdPhase1.add(initiative.getAuthor());
        initiative.setPhase(1); // new initiatives start in phase 1
    }

    public static void deleteInitiative(String title) {
        Initiative i = initiatives.remove(title);
        if (i != null && i.getPhase() == 1) {
            createdPhase1.remove(i.getAuthor());
        }
    }

    // --- Phase Management ---
    public static int getCurrentPhase() {
        return currentPhase;
    }

    public static void setCurrentPhase(int phase) {
        currentPhase = phase;
    }

    public static boolean canCreateInitiative(UUID playerId, String playerName) {
        return currentPhase == 1 && !createdPhase1.contains(playerName);
    }

    // --- Voting Phase 1 (support votes) ---
    public static void votePhase1(UUID playerId, String title) {
        Initiative initiative = getInitiative(title);
        if (initiative == null) return;

        // prevent author from voting for own initiative
        if (initiative.getAuthor().equalsIgnoreCase(playerId.toString())) return;

        Set<String> votedInitiatives = playerVotesPhase1.computeIfAbsent(playerId, k -> new HashSet<>());
        if (votedInitiatives.contains(title)) {
            votedInitiatives.remove(title);
            initiative.decrementVotes();
        } else {
            votedInitiatives.add(title);
            initiative.incrementVotes();
        }
    }

    public static Set<String> getPlayerVotesPhase1(UUID playerId) {
        return playerVotesPhase1.computeIfAbsent(playerId, k -> new HashSet<>());
    }

    // --- Voting Phase 2 (for / against) ---
    public static void votePhase2(UUID playerId, String title, boolean voteFor) {
        Initiative initiative = getInitiative(title);
        if (initiative == null) return;

        Map<String, Boolean> votes = playerVotesPhase2.computeIfAbsent(playerId, k -> new HashMap<>());
        Boolean previousVote = votes.put(title, voteFor);

        // remove previous vote effect
        if (previousVote != null) {
            if (previousVote) initiative.decrementVoteFor();
            else initiative.decrementVoteAgainst();
        }

        // apply new vote
        if (voteFor) initiative.voteFor();
        else initiative.voteAgainst();
    }

    public static Map<String, Boolean> getPlayerVotesPhase2(UUID playerId) {
        return playerVotesPhase2.computeIfAbsent(playerId, k -> new HashMap<>());
    }
}
