package com.example.hostgame.dto;

import com.example.hostgame.domain.GameStage;
import java.time.Instant;

public record StageChangedView(
        GameStage stage,
        Instant stageEndsAt
) {
}
