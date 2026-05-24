package com.example.hostgame.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastRoomEvent(String roomId, WsEvent<?> event) {
        messagingTemplate.convertAndSend(roomTopic(roomId), event);
    }

    public void sendPrivateEvent(String user, WsEvent<?> event) {
        messagingTemplate.convertAndSendToUser(user, "/queue/events", event);
    }

    public String roomTopic(String roomId) {
        return "/topic/rooms/" + roomId;
    }
}
