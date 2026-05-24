package com.example.hostgame.dto;

import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.MessageType;
import java.time.Instant;

public record ChatMessageView(
        String messageId,
        String roomId,
        MessageType messageType,
        String playerId,
        Integer playerNo,
        String speakerName,
        String content,
        Instant createdAt
) {

    public static ChatMessageView from(ChatMessage message) {
        return new ChatMessageView(
                message.getMessageId(),
                message.getRoomId(),
                message.getMessageType(),
                message.getPlayerId(),
                message.getPlayerNo(),
                message.getSpeakerName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
