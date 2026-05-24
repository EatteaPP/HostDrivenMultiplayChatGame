package com.example.hostgame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.store.InMemoryGameStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomServiceTest {

    private RoomService roomService;

    @BeforeEach
    void setUp() {
        roomService = new RoomService(new InMemoryGameStore());
    }

    @Test
    void createRoomCreatesWaitingRoomWithoutPlayers() {
        GameRoom room = roomService.createRoom(GameType.AI_CHAT_WEREWOLF);

        assertThat(room.getRoomId()).isNotBlank();
        assertThat(room.getRoomStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getCurrentStage()).isEqualTo(GameStage.WAITING);
        assertThat(room.getPlayers()).isEmpty();
    }

    @Test
    void joinRoomCreatesHumanPlayerWithPublicIdentityFields() {
        GameRoom room = roomService.createRoom(null);

        Player player = roomService.joinRoom(room.getRoomId());

        assertThat(player.getPlayerId()).isNotBlank();
        assertThat(player.getPlayerNo()).isEqualTo(1);
        assertThat(player.getColor()).isEqualTo("red");
        assertThat(player.getControllerType()).isEqualTo(PlayerControllerType.HUMAN);
        assertThat(roomService.getRoom(room.getRoomId()).getPlayers()).hasSize(1);
    }

    @Test
    void joinRoomRejectsNonWaitingRoom() {
        GameRoom room = roomService.createRoom(null);
        room.setRoomStatus(RoomStatus.IN_PROGRESS);

        assertThatThrownBy(() -> roomService.joinRoom(room.getRoomId()))
                .isInstanceOf(RoomJoinException.class);
    }

    @Test
    void getRoomRejectsUnknownRoomId() {
        assertThatThrownBy(() -> roomService.getRoom("missing-room"))
                .isInstanceOf(RoomNotFoundException.class);
    }
}
