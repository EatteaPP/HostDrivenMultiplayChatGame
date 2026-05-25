package com.example.hostgame.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.RoomObjective;
import com.example.hostgame.dto.CreateRoomRequest;
import com.example.hostgame.service.AvailableActionService;
import com.example.hostgame.service.ChatService;
import com.example.hostgame.service.PlayerActionService;
import com.example.hostgame.service.RoomService;
import com.example.hostgame.service.VoteService;
import com.example.hostgame.store.InMemoryGameStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class MockAgentControllerTest {

    private RoomService roomService;
    private AvailableActionService availableActionService;
    private MockAgentController mockAgentController;

    @BeforeEach
    void setUp() {
        InMemoryGameStore gameStore = new InMemoryGameStore();
        roomService = new RoomService(gameStore);
        availableActionService = new AvailableActionService();
        PlayerActionService playerActionService = new PlayerActionService(
                roomService,
                new ChatService(gameStore),
                new VoteService(gameStore),
                org.mockito.Mockito.mock(com.example.hostgame.flow.GameFlowEngine.class)
        );
        mockAgentController = new MockAgentController(new FixedObjectProvider<>(playerActionService));
    }

    @Test
    void mockAgentSendsMessageThroughPlayerActionService() {
        GameRoom room = roomService.createRoom(new CreateRoomRequest(
                null,
                RoomObjective.FIND_AI,
                null,
                null,
                null,
                null,
                null
        ));
        Player ai = roomService.joinAiPlayer(room.getRoomId());
        room.setCurrentStage(GameStage.DISCUSSION);
        room.setRound(1);

        mockAgentController.onAvailableActionsUpdated(room, ai, availableActionService.getAvailableActions(room, ai));

        assertThat(room.getMessages()).hasSize(1);
        assertThat(room.getMessages().get(0).getPlayerId()).isEqualTo(ai.getPlayerId());
        assertThat(room.getActionRecords()).hasSize(1);
    }

    @Test
    void mockAgentVotesThroughPlayerActionService() {
        GameRoom room = roomService.createRoom(new CreateRoomRequest(
                null,
                RoomObjective.FIND_AI,
                null,
                null,
                null,
                null,
                null
        ));
        Player ai = roomService.joinAiPlayer(room.getRoomId());
        Player human = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);
        room.setRound(1);
        ai.setControllerType(PlayerControllerType.AI);

        mockAgentController.onAvailableActionsUpdated(room, ai, availableActionService.getAvailableActions(room, ai));

        assertThat(room.getVotes()).hasSize(1);
        assertThat(room.getVotes().get(0).getVoterPlayerId()).isEqualTo(ai.getPlayerId());
        assertThat(room.getVotes().get(0).getTargetPlayerId()).isEqualTo(human.getPlayerId());
    }

    private record FixedObjectProvider<T>(T value) implements ObjectProvider<T> {
        @Override
        public T getObject(Object... args) {
            return value;
        }

        @Override
        public T getIfAvailable() {
            return value;
        }

        @Override
        public T getIfUnique() {
            return value;
        }

        @Override
        public T getObject() {
            return value;
        }
    }
}
