package com.example.hostgame.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.example.hostgame.agent.AgentCoordinator;
import com.example.hostgame.agent.NoopAgentController;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.service.ActionRejectedException;
import com.example.hostgame.service.AvailableActionService;
import com.example.hostgame.service.HostService;
import com.example.hostgame.service.RoomService;
import com.example.hostgame.service.VoteService;
import com.example.hostgame.store.InMemoryGameStore;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GameFlowEngineTest {

    private RoomService roomService;
    private GameFlowEngine gameFlowEngine;
    private WebSocketBroadcaster broadcaster;
    private VoteService voteService;

    @BeforeEach
    void setUp() {
        InMemoryGameStore gameStore = new InMemoryGameStore();
        roomService = new RoomService(gameStore);
        voteService = new VoteService(gameStore);
        broadcaster = org.mockito.Mockito.mock(WebSocketBroadcaster.class);
        HostService hostService = new HostService(roomService, gameStore, broadcaster);
        gameFlowEngine = new GameFlowEngine(
                roomService,
                gameStore,
                hostService,
                new AvailableActionService(),
                broadcaster,
                new AgentCoordinator(new NoopAgentController())
        );
    }

    @Test
    void startGameMovesWaitingRoomToDiscussionAndBroadcastsEvents() {
        GameRoom room = roomService.createRoom(null);
        String playerId = roomService.joinRoom(room.getRoomId()).getPlayerId();
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());

        GameRoom startedRoom = gameFlowEngine.startGame(room.getRoomId());

        assertThat(startedRoom.getRoomStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
        assertThat(startedRoom.getCurrentStage()).isEqualTo(GameStage.DISCUSSION);
        assertThat(startedRoom.getStartedAt()).isNotNull();
        assertThat(startedRoom.getStageStartedAt()).isNotNull();
        assertThat(startedRoom.getStageEndsAt()).isNotNull();
        assertThat(startedRoom.getRound()).isEqualTo(1);
        assertThat(startedRoom.getMessages()).hasSize(1);
        verify(broadcaster, atLeastOnce()).broadcastRoomEvent(eq(room.getRoomId()), org.mockito.ArgumentMatchers.<WsEvent<?>>any());
        verify(broadcaster).sendPrivateEvent(eq(playerId), org.mockito.ArgumentMatchers.<WsEvent<?>>any());
    }

    @Test
    void startGameRejectsEmptyRoom() {
        GameRoom room = roomService.createRoom(null);

        assertThatThrownBy(() -> gameFlowEngine.startGame(room.getRoomId()))
                .isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void startGameRejectsAlreadyStartedRoom() {
        GameRoom room = roomService.createRoom(null);
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        gameFlowEngine.startGame(room.getRoomId());

        assertThatThrownBy(() -> gameFlowEngine.startGame(room.getRoomId()))
                .isInstanceOf(ActionRejectedException.class);
    }

    @Test
    void discussionTimeoutMovesRoomToVoting() {
        GameRoom room = roomService.createRoom(null);
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        roomService.joinRoom(room.getRoomId());
        GameRoom startedRoom = gameFlowEngine.startGame(room.getRoomId());
        Instant timeout = startedRoom.getStageEndsAt();

        GameRoom votingRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), timeout);

        assertThat(votingRoom.getCurrentStage()).isEqualTo(GameStage.VOTING);
        assertThat(votingRoom.getStageStartedAt()).isEqualTo(timeout);
        assertThat(votingRoom.getStageEndsAt()).isEqualTo(timeout.plusSeconds(votingRoom.getFlowConfig().getVotingSeconds()));
        assertThat(votingRoom.getMessages()).hasSize(2);
    }

    @Test
    void votingTimeoutResolvesVoteAndReturnsToDiscussion() {
        GameRoom room = roomService.createRoom(null);
        var player1 = roomService.joinRoom(room.getRoomId());
        var player2 = roomService.joinRoom(room.getRoomId());
        var player3 = roomService.joinRoom(room.getRoomId());
        var player4 = roomService.joinRoom(room.getRoomId());
        var player5 = roomService.joinRoom(room.getRoomId());
        GameRoom startedRoom = gameFlowEngine.startGame(room.getRoomId());
        GameRoom votingRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), startedRoom.getStageEndsAt());
        voteService.submitVote(votingRoom, player1, player2.getPlayerId());
        voteService.submitVote(votingRoom, player3, player2.getPlayerId());
        voteService.submitVote(votingRoom, player4, player2.getPlayerId());
        voteService.submitVote(votingRoom, player5, player2.getPlayerId());

        GameRoom nextDiscussionRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), votingRoom.getStageEndsAt());

        assertThat(nextDiscussionRoom.getCurrentStage()).isEqualTo(GameStage.DISCUSSION);
        assertThat(nextDiscussionRoom.getRound()).isEqualTo(2);
        assertThat(player2.isAlive()).isFalse();
        assertThat(nextDiscussionRoom.getMessages()).anyMatch(message ->
                message.getContent().contains("Vote result: Player 2 : 4 vote")
        );
        verify(broadcaster, atLeastOnce()).broadcastRoomEvent(eq(room.getRoomId()), org.mockito.ArgumentMatchers.<WsEvent<?>>any());
    }

    @Test
    void tiedVoteEliminatesNoPlayer() {
        GameRoom room = roomService.createRoom(null);
        var player1 = roomService.joinRoom(room.getRoomId());
        var player2 = roomService.joinRoom(room.getRoomId());
        var player3 = roomService.joinRoom(room.getRoomId());
        var player4 = roomService.joinRoom(room.getRoomId());
        GameRoom startedRoom = gameFlowEngine.startGame(room.getRoomId());
        GameRoom votingRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), startedRoom.getStageEndsAt());
        voteService.submitVote(votingRoom, player1, player2.getPlayerId());
        voteService.submitVote(votingRoom, player2, player1.getPlayerId());
        voteService.submitVote(votingRoom, player3, player4.getPlayerId());
        voteService.submitVote(votingRoom, player4, player3.getPlayerId());

        GameRoom nextDiscussionRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), votingRoom.getStageEndsAt());

        assertThat(nextDiscussionRoom.getCurrentStage()).isEqualTo(GameStage.DISCUSSION);
        assertThat(player1.isAlive()).isTrue();
        assertThat(player2.isAlive()).isTrue();
        assertThat(nextDiscussionRoom.getMessages()).anyMatch(message ->
                message.getContent().contains("Vote result:")
        );
    }

    @Test
    void eliminationEndsGameWhenThreePlayersRemain() {
        GameRoom room = roomService.createRoom(null);
        var player1 = roomService.joinRoom(room.getRoomId());
        var player2 = roomService.joinRoom(room.getRoomId());
        var player3 = roomService.joinRoom(room.getRoomId());
        var player4 = roomService.joinRoom(room.getRoomId());
        player3.setControllerType(PlayerControllerType.AI);
        GameRoom startedRoom = gameFlowEngine.startGame(room.getRoomId());
        GameRoom votingRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), startedRoom.getStageEndsAt());
        voteService.submitVote(votingRoom, player1, player2.getPlayerId());
        voteService.submitVote(votingRoom, player3, player2.getPlayerId());
        voteService.submitVote(votingRoom, player4, player2.getPlayerId());

        GameRoom endedRoom = gameFlowEngine.handleStageTimeout(room.getRoomId(), votingRoom.getStageEndsAt());

        assertThat(endedRoom.getRoomStatus()).isEqualTo(RoomStatus.ENDED);
        assertThat(endedRoom.getCurrentStage()).isEqualTo(GameStage.ENDED);
        assertThat(endedRoom.getEndedAt()).isNotNull();
        assertThat(player2.isAlive()).isFalse();
        assertThat(endedRoom.getMessages()).anyMatch(message ->
                message.getContent().startsWith("Final result:")
        );
        verify(broadcaster, atLeastOnce()).broadcastRoomEvent(eq(room.getRoomId()), org.mockito.ArgumentMatchers.<WsEvent<?>>any());
    }
}
