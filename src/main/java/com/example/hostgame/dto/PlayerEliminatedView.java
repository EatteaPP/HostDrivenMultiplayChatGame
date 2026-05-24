package com.example.hostgame.dto;

public record PlayerEliminatedView(
        String playerId,
        int playerNo,
        int round
) {
}
