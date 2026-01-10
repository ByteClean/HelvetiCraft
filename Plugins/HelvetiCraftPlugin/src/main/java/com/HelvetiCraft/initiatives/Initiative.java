package com.HelvetiCraft.initiatives;

public class Initiative {
    private String id; // NEW FIELD
    private final String title;
    private String description;
    private final String author;
    private int phase; // 1 or 2

    // Phase 1 votes
    private int votes;

    // Phase 2 votes
    private int votesFor;
    private int votesAgainst;

    private String createdAt;

    public Initiative(String id, String title, String description, String author) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.phase = 1;
    }

    // Constructor used for new initiatives before backend assigns ID
    public Initiative(String title, String description, String author) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.phase = 1;
        this.id = null; // backend will assign
    }

    // --- ID getter/setter ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    // --- other getters/setters ---
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthor() { return author; }
    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }

    public int getVotes() { return votes; }
    public void setVotes(int votes) { this.votes = votes; }
    public void incrementVotes() { votes++; }
    public void decrementVotes() { if (votes > 0) votes--; }

    public int getVotesFor() { return votesFor; }
    public void setVotesFor(int votesFor) { this.votesFor = votesFor; }
    public int getVotesAgainst() { return votesAgainst; }
    public void setVotesAgainst(int votesAgainst) { this.votesAgainst = votesAgainst; }
    public void voteFor() { votesFor++; }
    public void voteAgainst() { votesAgainst++; }
    public void decrementVoteFor() { if (votesFor > 0) votesFor--; }
    public void decrementVoteAgainst() { if (votesAgainst > 0) votesAgainst--; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
