package com.example.hostgame.domain;

public class ActionTarget {

    private String playerId;
    private Integer playerNo;

    public ActionTarget() {
    }

    public ActionTarget(String playerId, Integer playerNo) {
        this.playerId = playerId;
        this.playerNo = playerNo;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public Integer getPlayerNo() {
        return playerNo;
    }

    public void setPlayerNo(Integer playerNo) {
        this.playerNo = playerNo;
    }
}
