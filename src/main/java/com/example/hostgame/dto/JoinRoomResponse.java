package com.example.hostgame.dto;

public record JoinRoomResponse(
        RoomView room,
        PlayerPublicView player
) {
}
