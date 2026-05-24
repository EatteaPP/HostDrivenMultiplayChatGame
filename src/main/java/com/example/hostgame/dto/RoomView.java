package com.example.hostgame.dto;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.RoomStatus;
import java.time.Instant;
import java.util.List;

public record RoomView(
        String roomId,
        GameType gameType,
        RoomStatus roomStatus,
        GameStage currentStage,
        List<PlayerPublicView> players,
        List<ChatMessageView> messages,
        Instant createdAt,
        Instant startedAt,
        Instant stageStartedAt,
        Instant stageEndsAt,
        Instant endedAt,
        int round
) {

    public static RoomView from(GameRoom room) {
        return new RoomView(
                room.getRoomId(),
                room.getGameType(),
                room.getRoomStatus(),
                room.getCurrentStage(),
                room.getPlayers().stream()
                        .map(PlayerPublicView::from)
                        .toList(),
                room.getMessages().stream()
                        .map(ChatMessageView::from)
                        .toList(),
                room.getCreatedAt(),
                room.getStartedAt(),
                room.getStageStartedAt(),
                room.getStageEndsAt(),
                room.getEndedAt(),
                room.getRound()
        );
    }
}
