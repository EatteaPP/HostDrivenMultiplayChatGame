package com.example.hostgame.domain;

import java.time.Instant;
import java.util.UUID;

public class ChatMessage {

    private String messageId = UUID.randomUUID().toString();
    private String roomId;
    private MessageType messageType;
    private MessageAudience audience = MessageAudience.publicAudience();
    private String playerId;
    private Integer playerNo;
    private String speakerName;
    private String content;
    private Instant createdAt = Instant.now();

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public MessageAudience getAudience() {
        return audience;
    }

    public void setAudience(MessageAudience audience) {
        this.audience = audience;
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

    public String getSpeakerName() {
        return speakerName;
    }

    public void setSpeakerName(String speakerName) {
        this.speakerName = speakerName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
