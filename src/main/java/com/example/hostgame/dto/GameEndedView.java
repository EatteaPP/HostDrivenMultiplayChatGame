package com.example.hostgame.dto;

import com.example.hostgame.domain.RoomObjective;
import java.time.Instant;
import java.util.List;

public record GameEndedView(
        String roomId,
        RoomObjective objective,
        String endReasonCode,
        String resultCode,
        String resultMessage,
        int aliveCrewCount,
        int aliveTraitorCount,
        int aliveHumanCount,
        int aliveAiCount,
        List<PlayerRevealView> players,
        Instant endedAt
) {
}
