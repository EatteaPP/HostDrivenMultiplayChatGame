package com.example.hostgame.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.MessageType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerAction;
import com.example.hostgame.domain.PlayerStatus;
import com.example.hostgame.flow.GameFlowEngine;
import com.example.hostgame.store.InMemoryGameStore;
import java.time.Instant;
import java.util.Map;
import org.mockito.Mockito;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerActionServiceTest {

    private RoomService roomService;
    private PlayerActionService playerActionService;
    private GameFlowEngine gameFlowEngine;

    @BeforeEach
    void setUp() {
        InMemoryGameStore gameStore = new InMemoryGameStore();
        roomService = new RoomService(gameStore);
        gameFlowEngine = Mockito.mock(GameFlowEngine.class);
        playerActionService = new PlayerActionService(
                roomService,
                new ChatService(gameStore),
                new VoteService(gameStore),
                gameFlowEngine
        );
    }

    @Test
    void sendMessageCreatesPublicPlayerMessageThroughActionService() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.DISCUSSION);

        ChatMessage message = (ChatMessage) playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "hello"))
        );

        assertThat(message.getMessageType()).isEqualTo(MessageType.PLAYER);
        assertThat(message.getPlayerId()).isEqualTo(player.getPlayerId());
        assertThat(message.getContent()).isEqualTo("hello");
        assertThat(room.getMessages()).containsExactly(message);
        assertThat(room.getActionRecords()).hasSize(1);
        assertThat(player.getLastMessageAt()).isNotNull();
    }

    @Test
    void sendMessageRejectsWaitingStage() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "hello"))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void sendMessageRejectsEndedStage() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.ENDED);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "after end"))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void sendMessageRejectsEliminatedPlayer() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.DISCUSSION);
        player.setStatus(PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "hello"))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void submitActionRejectsPlayerOutsideRoom() {
        GameRoom room = roomService.createRoom(null);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                "outside-player",
                action(ActionType.SEND_MESSAGE, Map.of("content", "hello"))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void sendMessageRejectsActiveCooldown() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.DISCUSSION);
        player.setLastMessageAt(Instant.now());

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "too soon"))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void sendMessageAllowsExpiredCooldown() {
        GameRoom room = roomService.createRoom(null);
        Player player = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.DISCUSSION);
        player.setLastMessageAt(Instant.now().minusSeconds(room.getFlowConfig().getMessageCooldownSeconds() + 1L));

        ChatMessage message = (ChatMessage) playerActionService.submitAction(
                room.getRoomId(),
                player.getPlayerId(),
                action(ActionType.SEND_MESSAGE, Map.of("content", "after cooldown"))
        );

        assertThat(message.getContent()).isEqualTo("after cooldown");
    }

    @Test
    void submitVoteRecordsVoteDuringVotingStage() {
        GameRoom room = roomService.createRoom(null);
        Player voter = roomService.joinRoom(room.getRoomId());
        Player target = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);

        Object result = playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", target.getPlayerId()))
        );

        assertThat(result).isInstanceOf(com.example.hostgame.domain.Vote.class);
        assertThat(room.getVotes()).hasSize(1);
        assertThat(room.getVotes().get(0).getVoterPlayerId()).isEqualTo(voter.getPlayerId());
        assertThat(room.getVotes().get(0).getTargetPlayerId()).isEqualTo(target.getPlayerId());
        assertThat(room.getActionRecords()).hasSize(1);
        Mockito.verify(gameFlowEngine).handleActionCompleted(room.getRoomId());
    }

    @Test
    void submitVoteRejectsSelfVote() {
        GameRoom room = roomService.createRoom(null);
        Player voter = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", voter.getPlayerId()))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void submitVoteRejectsEliminatedVoter() {
        GameRoom room = roomService.createRoom(null);
        Player voter = roomService.joinRoom(room.getRoomId());
        Player target = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);
        voter.setStatus(PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", target.getPlayerId()))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void submitVoteRejectsEliminatedTarget() {
        GameRoom room = roomService.createRoom(null);
        Player voter = roomService.joinRoom(room.getRoomId());
        Player target = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);
        target.setStatus(PlayerStatus.ELIMINATED);

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", target.getPlayerId()))
        )).isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void submitVoteRejectsDuplicateVote() {
        GameRoom room = roomService.createRoom(null);
        Player voter = roomService.joinRoom(room.getRoomId());
        Player target = roomService.joinRoom(room.getRoomId());
        room.setCurrentStage(GameStage.VOTING);

        playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", target.getPlayerId()))
        );

        assertThatThrownBy(() -> playerActionService.submitAction(
                room.getRoomId(),
                voter.getPlayerId(),
                action(ActionType.SUBMIT_VOTE, Map.of("targetPlayerId", target.getPlayerId()))
        )).isInstanceOf(ActionRejectedException.class);
    }

    private PlayerAction action(ActionType actionType, Map<String, Object> payload) {
        PlayerAction action = new PlayerAction();
        action.setActionType(actionType);
        action.setPayload(payload);
        return action;
    }
}
