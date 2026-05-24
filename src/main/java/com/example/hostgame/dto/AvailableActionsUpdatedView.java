package com.example.hostgame.dto;

import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameStage;
import java.util.List;

public record AvailableActionsUpdatedView(
        String playerId,
        GameStage stage,
        List<AvailableAction> availableActions
) {
}
