package com.example.hostgame.dto;

import com.example.hostgame.domain.ActionType;

public record PlayerActionResponse(
        boolean accepted,
        ActionType actionType,
        Object result
) {

    public static PlayerActionResponse accepted(ActionType actionType, Object result) {
        return new PlayerActionResponse(true, actionType, result);
    }
}
