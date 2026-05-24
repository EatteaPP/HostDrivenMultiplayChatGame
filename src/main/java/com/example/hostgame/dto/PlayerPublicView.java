package com.example.hostgame.dto;

import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerStatus;

public record PlayerPublicView(
        String playerId,
        int playerNo,
        String color,
        PlayerStatus status
) {

    public static PlayerPublicView from(Player player) {
        return new PlayerPublicView(
                player.getPlayerId(),
                player.getPlayerNo(),
                player.getColor(),
                player.getStatus()
        );
    }
}
