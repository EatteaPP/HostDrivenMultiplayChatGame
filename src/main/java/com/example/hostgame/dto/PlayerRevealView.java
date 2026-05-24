package com.example.hostgame.dto;

import com.example.hostgame.domain.Faction;
import com.example.hostgame.domain.GameRole;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.PlayerStatus;

public record PlayerRevealView(
        String playerId,
        int playerNo,
        String color,
        PlayerStatus status,
        PlayerControllerType controllerType,
        GameRole role,
        Faction faction
) {

    public static PlayerRevealView from(Player player) {
        return new PlayerRevealView(
                player.getPlayerId(),
                player.getPlayerNo(),
                player.getColor(),
                player.getStatus(),
                player.getControllerType(),
                player.getRole(),
                player.getFaction()
        );
    }
}
