package com.example.hostgame.websocket;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WebSocketBroadcasterTest {

    private final SimpMessagingTemplate messagingTemplate = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
    private final WebSocketBroadcaster broadcaster = new WebSocketBroadcaster(messagingTemplate);

    @Test
    void broadcastRoomEventSendsToRoomTopic() {
        WsEvent<String> event = WsEvent.of(WsEventType.ROOM_STATE_UPDATED, "payload");

        broadcaster.broadcastRoomEvent("room-1", event);

        verify(messagingTemplate).convertAndSend("/topic/rooms/room-1", event);
    }

    @Test
    void privateEventUsesUserQueue() {
        WsEvent<String> event = WsEvent.of(WsEventType.ACTION_REJECTED, "payload");

        broadcaster.sendPrivateEvent("player-1", event);

        verify(messagingTemplate).convertAndSendToUser("player-1", "/queue/events", event);
    }
}
