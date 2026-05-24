package com.example.hostgame.service;

import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.MessageAudience;
import com.example.hostgame.domain.MessageType;
import com.example.hostgame.dto.ChatMessageView;
import com.example.hostgame.store.GameStore;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import com.example.hostgame.websocket.WsEventType;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class HostService {

    public static final String HOST_SPEAKER_NAME = "Host";

    private final RoomService roomService;
    private final GameStore gameStore;
    private final WebSocketBroadcaster webSocketBroadcaster;

    public HostService(
            RoomService roomService,
            GameStore gameStore,
            WebSocketBroadcaster webSocketBroadcaster
    ) {
        this.roomService = roomService;
        this.gameStore = gameStore;
        this.webSocketBroadcaster = webSocketBroadcaster;
    }

    public ChatMessage announcePublicMessage(String roomId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ActionRejectedException("Host message content is required.");
        }

        GameRoom room = roomService.getRoom(roomId);
        ChatMessage message = new ChatMessage();
        message.setRoomId(room.getRoomId());
        message.setMessageType(MessageType.HOST);
        message.setAudience(MessageAudience.publicAudience());
        message.setSpeakerName(HOST_SPEAKER_NAME);
        message.setContent(content.trim());
        message.setCreatedAt(Instant.now());

        room.getMessages().add(message);
        gameStore.saveRoom(room);

        webSocketBroadcaster.broadcastRoomEvent(
                roomId,
                WsEvent.of(WsEventType.MESSAGE_CREATED, ChatMessageView.from(message))
        );
        return message;
    }
}
