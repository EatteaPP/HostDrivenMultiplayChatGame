package com.example.hostgame.dto;

import java.time.Instant;
import java.util.List;

public record GameEndedView(
        String roomId,
        String resultCode,
        String resultMessage,
        int aliveHumanCount,
        int aliveAiCount,
        List<PlayerRevealView> players,
        Instant endedAt
) {
}
