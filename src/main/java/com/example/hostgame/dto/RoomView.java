package com.example.hostgame.dto;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.RoomObjective;
import com.example.hostgame.domain.RoomStatus;
import java.time.Instant;
import java.util.List;

public record RoomView(
        String roomId,
        GameType gameType,
        RoomObjective objective,
        String objectiveHint,
        RoomStatus roomStatus,
        GameStage currentStage,
        List<PlayerPublicView> players,
        List<ChatMessageView> messages,
        Instant createdAt,
        Instant startedAt,
        Instant stageStartedAt,
        Instant stageEndsAt,
        Instant endedAt,
        int round,
        int discussionSeconds,
        int votingSeconds,
        int messageCooldownSeconds,
        int maxRounds,
        int minPlayersToStart,
        int endWhenAlivePlayersLE
) {

    public static RoomView from(GameRoom room) {
        return new RoomView(
                room.getRoomId(),
                room.getGameType(),
                room.getObjective(),
                room.getObjectiveHint(),
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
                room.getRound(),
                room.getFlowConfig().getDiscussionSeconds(),
                room.getFlowConfig().getVotingSeconds(),
                room.getFlowConfig().getMessageCooldownSeconds(),
                room.getFlowConfig().getMaxRounds(),
                room.getFlowConfig().getMinPlayersToStart(),
                room.getFlowConfig().getEndWhenAlivePlayersLE()
        );
    }
}
