package com.example.hostgame.domain;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerActionRecord {

    private String actionRecordId = UUID.randomUUID().toString();
    private String roomId;
    private String playerId;
    private ActionType actionType;
    private Map<String, Object> payload = new HashMap<>();
    private Instant createdAt = Instant.now();

    public String getActionRecordId() {
        return actionRecordId;
    }

    public void setActionRecordId(String actionRecordId) {
        this.actionRecordId = actionRecordId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
