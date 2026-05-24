package com.example.hostgame.service;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String roomId) {
        super("Room not found: " + roomId);
    }
}
