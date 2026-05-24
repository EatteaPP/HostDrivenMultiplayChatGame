package com.example.hostgame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.MessageType;
import com.example.hostgame.store.InMemoryGameStore;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HostServiceTest {

    private RoomService roomService;
    private HostService hostService;
    private WebSocketBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        InMemoryGameStore gameStore = new InMemoryGameStore();
        roomService = new RoomService(gameStore);
        broadcaster = org.mockito.Mockito.mock(WebSocketBroadcaster.class);
        hostService = new HostService(roomService, gameStore, broadcaster);
    }

    @Test
    void announcePublicMessageCreatesHostMessageWithoutAddingPlayer() {
        GameRoom room = roomService.createRoom(null);

        ChatMessage message = hostService.announcePublicMessage(room.getRoomId(), "Game starts soon.");

        assertThat(message.getMessageType()).isEqualTo(MessageType.HOST);
        assertThat(message.getSpeakerName()).isEqualTo(HostService.HOST_SPEAKER_NAME);
        assertThat(message.getPlayerId()).isNull();
        assertThat(message.getPlayerNo()).isNull();
        assertThat(roomService.getRoom(room.getRoomId()).getPlayers()).isEmpty();
        assertThat(roomService.getRoom(room.getRoomId()).getMessages()).containsExactly(message);
        verify(broadcaster).broadcastRoomEvent(eq(room.getRoomId()), org.mockito.ArgumentMatchers.<WsEvent<?>>any());
    }

    @Test
    void announcePublicMessageRejectsBlankContent() {
        GameRoom room = roomService.createRoom(null);

        assertThatThrownBy(() -> hostService.announcePublicMessage(room.getRoomId(), " "))
                .isInstanceOf(ActionRejectedException.class);
    }
}
