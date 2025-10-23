package com.HelvetiCraft.initiatives;

public class Initiative {
    private final String title;
    private String description;
    private final String author;
    private int phase; // 1 or 2

    // Phase 1 votes
    private int votes;

    // Phase 2 votes
    private int votesFor;
    private int votesAgainst;

    public Initiative(String title, String description, String author) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.phase = 1;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAuthor() { return author; }

    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }

    public int getVotes() { return votes; }
    public void incrementVotes() { votes++; }
    public void decrementVotes() { if (votes > 0) votes--; }

    public int getVotesFor() { return votesFor; }
    public int getVotesAgainst() { return votesAgainst; }
    public void voteFor() { votesFor++; }
    public void voteAgainst() { votesAgainst++; }
    public void decrementVoteFor() { if (votesFor > 0) votesFor--; }
    public void decrementVoteAgainst() { if (votesAgainst > 0) votesAgainst--; }
}
