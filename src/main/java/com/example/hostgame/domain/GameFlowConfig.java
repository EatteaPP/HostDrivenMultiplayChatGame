package com.example.hostgame.domain;

public class GameFlowConfig {

    private int discussionSeconds = 300;
    private int votingSeconds = 60;
    private int messageCooldownSeconds = 15;

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
}
