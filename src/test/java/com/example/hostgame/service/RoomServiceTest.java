package com.example.hostgame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.PlayerStatus;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.domain.RoomObjective;
import com.example.hostgame.dto.CreateRoomRequest;
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
        GameRoom room = roomService.createRoom(new CreateRoomRequest(
                null,
                RoomObjective.FIND_TRAITOR,
                "找出女性",
                120,
                45,
                12,
                10,
                4,
                3
        ));

        assertThat(room.getRoomId()).isNotBlank();
        assertThat(room.getRoomStatus()).isEqualTo(RoomStatus.WAITING);
        assertThat(room.getCurrentStage()).isEqualTo(GameStage.WAITING);
        assertThat(room.getObjective()).isEqualTo(RoomObjective.FIND_TRAITOR);
        assertThat(room.getObjectiveHint()).isEqualTo("找出女性");
        assertThat(room.getFlowConfig().getDiscussionSeconds()).isEqualTo(120);
        assertThat(room.getFlowConfig().getVotingSeconds()).isEqualTo(45);
        assertThat(room.getFlowConfig().getMessageCooldownSeconds()).isEqualTo(12);
        assertThat(room.getFlowConfig().getMaxRounds()).isEqualTo(10);
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

    @Test
    void createRoomWithoutRequestDefaultsToFindTraitorObjective() {
        GameRoom room = roomService.createRoom(null);

        assertThat(room.getObjective()).isEqualTo(RoomObjective.FIND_TRAITOR);
    }

    @Test
    void joinAiPlayerRejectedWhenObjectiveIsNotFindAi() {
        GameRoom room = roomService.createRoom(null);

        assertThatThrownBy(() -> roomService.joinAiPlayer(room.getRoomId()))
                .isInstanceOf(ActionRejectedException.class)
                .hasMessageContaining("FIND_AI");
    }

    @Test
    void disconnectPlayerMarksAlivePlayerAsEliminated() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());

        boolean changed = roomService.disconnectPlayer(room.getRoomId(), player.getPlayerId());

        assertThat(changed).isTrue();
        assertThat(player.getStatus()).isEqualTo(PlayerStatus.ELIMINATED);
    }
}
