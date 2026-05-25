package com.example.hostgame.domain;

public class GameFlowConfig {

    private int discussionSeconds = 300;
    private int votingSeconds = 60;
    private int messageCooldownSeconds = 15;
    private int maxRounds = 10;
    private int minPlayersToStart = 4;
    private int endWhenAlivePlayersLE = 3;

    public int getDiscussionSeconds() {
        return discussionSeconds;
    }

    public void setDiscussionSeconds(int discussionSeconds) {
        this.discussionSeconds = discussionSeconds;
    }

    public int getVotingSeconds() {
        return votingSeconds;
    }

    public void setVotingSeconds(int votingSeconds) {
        this.votingSeconds = votingSeconds;
    }

    public int getMessageCooldownSeconds() {
        return messageCooldownSeconds;
    }

    public void setMessageCooldownSeconds(int messageCooldownSeconds) {
        this.messageCooldownSeconds = messageCooldownSeconds;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
    }

    public int getMinPlayersToStart() {
        return minPlayersToStart;
    }

    public void setMinPlayersToStart(int minPlayersToStart) {
        this.minPlayersToStart = minPlayersToStart;
    }

    public int getEndWhenAlivePlayersLE() {
        return endWhenAlivePlayersLE;
    }

    public void setEndWhenAlivePlayersLE(int endWhenAlivePlayersLE) {
        this.endWhenAlivePlayersLE = endWhenAlivePlayersLE;
    }
}
