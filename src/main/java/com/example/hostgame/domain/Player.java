package com.example.hostgame.domain;

import java.time.Instant;
import java.util.UUID;

public class Player {

    private String playerId = UUID.randomUUID().toString();
    private int playerNo;
    private String color;
    private PlayerControllerType controllerType = PlayerControllerType.HUMAN;
    private PlayerStatus status = PlayerStatus.ALIVE;
    private Instant joinedAt = Instant.now();
    private Instant lastMessageAt;
    private GameRole role;
    private Faction faction;

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public int getPlayerNo() {
        return playerNo;
    }

    public void setPlayerNo(int playerNo) {
        this.playerNo = playerNo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public PlayerControllerType getControllerType() {
        return controllerType;
    }

    public void setControllerType(PlayerControllerType controllerType) {
        this.controllerType = controllerType;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public GameRole getRole() {
        return role;
    }

    public void setRole(GameRole role) {
        this.role = role;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public boolean isAlive() {
        return status == PlayerStatus.ALIVE;
    }
}
