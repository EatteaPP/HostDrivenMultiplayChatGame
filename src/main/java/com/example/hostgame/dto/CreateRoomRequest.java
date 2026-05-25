package com.example.hostgame.dto;

import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.RoomObjective;

public record CreateRoomRequest(
        GameType gameType,
        RoomObjective objective,
        String objectiveHint,
        Integer discussionSeconds,
        Integer votingSeconds,
        Integer messageCooldownSeconds,
        Integer maxRounds,
        Integer minPlayersToStart,
        Integer endWhenAlivePlayersLE
) {
}
