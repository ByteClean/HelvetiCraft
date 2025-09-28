package com.HelvetiCraft.initiatives;

public class Initiative {
    private final String title;
    private String description;
    private final String author;
    private int votes;

    private int phase;           // current phase index
    private long phaseEndTime;   // when current phase ends (millis)

    public Initiative(String title, String description, String author, int votes) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.votes = votes;
        this.phase = 1;
        this.phaseEndTime = System.currentTimeMillis(); // will be updated by manager
    }

    // getters/setters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public void setDescription(String desc) { this.description = desc; }
    public String getAuthor() { return author; }
    public int getVotes() { return votes; }
    public void incrementVotes() { votes++; }
    public void decrementVotes() { votes--; }

    public int getPhase() { return phase; }
    public void setPhase(int phase) { this.phase = phase; }

    public long getPhaseEndTime() { return phaseEndTime; }
    public void setPhaseEndTime(long endTime) { this.phaseEndTime = endTime; }
}
