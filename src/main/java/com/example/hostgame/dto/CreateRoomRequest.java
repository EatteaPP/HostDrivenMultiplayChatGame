package com.example.hostgame.dto;

import com.example.hostgame.domain.GameType;

public record CreateRoomRequest(
        GameType gameType
) {
}
