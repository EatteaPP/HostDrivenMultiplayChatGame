package com.example.hostgame.dto;

import com.example.hostgame.domain.ActionType;
import java.util.HashMap;
import java.util.Map;

public record PlayerActionRequest(
        ActionType actionType,
        Map<String, Object> payload
) {

    public Map<String, Object> safePayload() {
        return payload == null ? new HashMap<>() : payload;
    }
}
