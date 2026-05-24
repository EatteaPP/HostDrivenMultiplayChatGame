package com.example.hostgame.websocket;

import java.time.Instant;

public record WsEvent<T>(
        WsEventType type,
        T payload,
        Instant createdAt
) {

    public static <T> WsEvent<T> of(WsEventType type, T payload) {
        return new WsEvent<>(type, payload, Instant.now());
    }
}
