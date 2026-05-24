package com.example.hostgame.domain;

import java.time.Instant;
import java.util.UUID;

public class Vote {

    private String voteId = UUID.randomUUID().toString();
    private int round;
    private String voterPlayerId;
    private String targetPlayerId;
    private Instant createdAt = Instant.now();

    public String getVoteId() {
        return voteId;
    }

    public void setVoteId(String voteId) {
        this.voteId = voteId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public String getVoterPlayerId() {
        return voterPlayerId;
    }

    public void setVoterPlayerId(String voterPlayerId) {
        this.voterPlayerId = voterPlayerId;
    }

    public String getTargetPlayerId() {
        return targetPlayerId;
    }

    public void setTargetPlayerId(String targetPlayerId) {
        this.targetPlayerId = targetPlayerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
