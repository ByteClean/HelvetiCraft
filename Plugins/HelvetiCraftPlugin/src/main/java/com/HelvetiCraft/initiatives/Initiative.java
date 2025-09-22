package com.HelvetiCraft.initiatives;

public class Initiative {
    private final String title;
    private String description;
    private final String author;
    private int votes;

    public Initiative(String title, String description, String author, int votes) {
        this.title = title;
        this.description = description;
        this.author = author;
        this.votes = votes;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getAuthor() { return author; }
    public int getVotes() { return votes; }

    public void incrementVotes() { votes++; }
    public void decrementVotes() { if (votes > 0) votes--; }
}
