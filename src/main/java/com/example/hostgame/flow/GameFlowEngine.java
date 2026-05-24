package com.example.hostgame.flow;

import com.example.hostgame.agent.AgentCoordinator;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.PlayerStatus;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.domain.Vote;
import com.example.hostgame.dto.AvailableActionsUpdatedView;
import com.example.hostgame.dto.GameEndedView;
import com.example.hostgame.dto.PlayerEliminatedView;
import com.example.hostgame.dto.PlayerRevealView;
import com.example.hostgame.dto.RoomView;
import com.example.hostgame.dto.StageChangedView;
import com.example.hostgame.service.ActionRejectedException;
import com.example.hostgame.service.AvailableActionService;
import com.example.hostgame.service.HostService;
import com.example.hostgame.service.RoomService;
import com.example.hostgame.store.GameStore;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import com.example.hostgame.websocket.WsEventType;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class GameFlowEngine {

    private final RoomService roomService;
    private final GameStore gameStore;
    private final HostService hostService;
    private final AvailableActionService availableActionService;
    private final WebSocketBroadcaster webSocketBroadcaster;
    private final AgentCoordinator agentCoordinator;

    public GameFlowEngine(
            RoomService roomService,
            GameStore gameStore,
            HostService hostService,
            AvailableActionService availableActionService,
            WebSocketBroadcaster webSocketBroadcaster,
            AgentCoordinator agentCoordinator
    ) {
        this.roomService = roomService;
        this.gameStore = gameStore;
        this.hostService = hostService;
        this.availableActionService = availableActionService;
        this.webSocketBroadcaster = webSocketBroadcaster;
        this.agentCoordinator = agentCoordinator;
    }

    public synchronized GameRoom startGame(String roomId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room.getRoomStatus() != RoomStatus.WAITING || room.getCurrentStage() != GameStage.WAITING) {
            throw new ActionRejectedException("Only waiting rooms can be started.");
        }
        if (room.getPlayers().isEmpty()) {
            throw new ActionRejectedException("Cannot start a room without players.");
        }

        Instant now = Instant.now();
        room.setRoomStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentStage(GameStage.DISCUSSION);
        room.setStartedAt(now);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now.plusSeconds(room.getFlowConfig().getDiscussionSeconds()));
        room.setRound(1);
        gameStore.saveRoom(room);

        hostService.announcePublicMessage(roomId, "Game started. Discussion phase begins.");
        broadcastStageChanged(room);
        broadcastAvailableActions(room);
        webSocketBroadcaster.broadcastRoomEvent(
                roomId,
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, RoomView.from(room))
        );
        return room;
    }

    public synchronized GameRoom handleStageTimeout(String roomId, Instant now) {
        GameRoom room = roomService.getRoom(roomId);
        if (room.getStageEndsAt() == null || now.isBefore(room.getStageEndsAt())) {
            return room;
        }
        if (room.getCurrentStage() == GameStage.DISCUSSION) {
            return enterVoting(room, now);
        }
        if (room.getCurrentStage() == GameStage.VOTING) {
            return enterElimination(room, now);
        }
        return room;
    }

    public synchronized GameRoom handleActionCompleted(String roomId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room.getCurrentStage() == GameStage.VOTING && allAlivePlayersVoted(room)) {
            return enterElimination(room, Instant.now());
        }
        return room;
    }

    private GameRoom enterVoting(GameRoom room, Instant now) {
        if (room.getCurrentStage() != GameStage.DISCUSSION) {
            return room;
        }
        room.setCurrentStage(GameStage.VOTING);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now.plusSeconds(room.getFlowConfig().getVotingSeconds()));
        gameStore.saveRoom(room);

        hostService.announcePublicMessage(room.getRoomId(), "Voting phase begins. Choose one alive player.");
        broadcastStageChanged(room);
        broadcastAvailableActions(room);
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, RoomView.from(room))
        );
        return room;
    }

    private GameRoom enterElimination(GameRoom room, Instant now) {
        if (room.getCurrentStage() != GameStage.VOTING) {
            return room;
        }
        room.setCurrentStage(GameStage.ELIMINATION);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now);
        gameStore.saveRoom(room);

        hostService.announcePublicMessage(room.getRoomId(), "Voting ended. Resolving votes.");
        broadcastStageChanged(room);

        Optional<Player> eliminatedPlayer = resolveElimination(room);
        eliminatedPlayer.ifPresent(player -> {
            player.setStatus(PlayerStatus.ELIMINATED);
            webSocketBroadcaster.broadcastRoomEvent(
                    room.getRoomId(),
                    WsEvent.of(
                            WsEventType.PLAYER_ELIMINATED,
                            new PlayerEliminatedView(player.getPlayerId(), player.getPlayerNo(), room.getRound())
                    )
            );
            hostService.announcePublicMessage(
                    room.getRoomId(),
                    "Player " + player.getPlayerNo() + " has been eliminated."
            );
        });
        if (eliminatedPlayer.isEmpty()) {
            hostService.announcePublicMessage(room.getRoomId(), "Vote tied. No player was eliminated.");
        }

        if (alivePlayerCount(room) <= 3) {
            return enterEnded(room, now);
        }
        return enterNextDiscussion(room, now);
    }

    private Optional<Player> resolveElimination(GameRoom room) {
        Map<String, Long> voteCounts = room.getVotes().stream()
                .filter(vote -> vote.getRound() == room.getRound())
                .collect(Collectors.groupingBy(Vote::getTargetPlayerId, Collectors.counting()));
        if (voteCounts.isEmpty()) {
            return Optional.empty();
        }

        long highestCount = voteCounts.values().stream()
                .max(Comparator.naturalOrder())
                .orElse(0L);
        long topTargetCount = voteCounts.values().stream()
                .filter(count -> count == highestCount)
                .count();
        if (topTargetCount != 1) {
            return Optional.empty();
        }

        String eliminatedPlayerId = voteCounts.entrySet().stream()
                .filter(entry -> entry.getValue() == highestCount)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow();
        return room.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(eliminatedPlayerId))
                .findFirst();
    }

    private GameRoom enterNextDiscussion(GameRoom room, Instant now) {
        room.setRound(room.getRound() + 1);
        room.setCurrentStage(GameStage.DISCUSSION);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now.plusSeconds(room.getFlowConfig().getDiscussionSeconds()));
        gameStore.saveRoom(room);

        hostService.announcePublicMessage(room.getRoomId(), "Discussion phase begins.");
        broadcastStageChanged(room);
        broadcastAvailableActions(room);
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, RoomView.from(room))
        );
        return room;
    }

    private boolean allAlivePlayersVoted(GameRoom room) {
        long alivePlayerCount = room.getPlayers().stream()
                .filter(Player::isAlive)
                .count();
        long votedAlivePlayerCount = room.getVotes().stream()
                .filter(vote -> vote.getRound() == room.getRound())
                .map(Vote::getVoterPlayerId)
                .distinct()
                .count();
        return alivePlayerCount > 0 && votedAlivePlayerCount >= alivePlayerCount;
    }

    private GameRoom enterEnded(GameRoom room, Instant now) {
        room.setRoomStatus(RoomStatus.ENDED);
        room.setCurrentStage(GameStage.ENDED);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now);
        room.setEndedAt(now);
        gameStore.saveRoom(room);

        hostService.announcePublicMessage(room.getRoomId(), "Game ended. Revealing final identities.");
        broadcastStageChanged(room);
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.GAME_ENDED, buildGameEndedView(room))
        );
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, RoomView.from(room))
        );
        broadcastAvailableActions(room);
        return room;
    }

    private GameEndedView buildGameEndedView(GameRoom room) {
        int aliveAiCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> player.getControllerType() == PlayerControllerType.AI)
                .count();
        int aliveHumanCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> player.getControllerType() == PlayerControllerType.HUMAN)
                .count();

        String resultCode;
        String resultMessage;
        if (aliveAiCount == 0) {
            resultCode = "HUMANS_WIN";
            resultMessage = "No AI players remain.";
        } else if (aliveAiCount == 1) {
            resultCode = "AI_SURVIVED";
            resultMessage = "One AI player survived to the final group.";
        } else {
            resultCode = "AI_DOMINATED";
            resultMessage = "Multiple AI players survived to the final group.";
        }

        return new GameEndedView(
                room.getRoomId(),
                resultCode,
                resultMessage,
                aliveHumanCount,
                aliveAiCount,
                room.getPlayers().stream()
                        .map(PlayerRevealView::from)
                        .toList(),
                room.getEndedAt()
        );
    }

    private long alivePlayerCount(GameRoom room) {
        return room.getPlayers().stream()
                .filter(Player::isAlive)
                .count();
    }

    private void broadcastStageChanged(GameRoom room) {
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(
                        WsEventType.STAGE_CHANGED,
                        new StageChangedView(room.getCurrentStage(), room.getStageEndsAt())
                )
        );
    }

    private void broadcastAvailableActions(GameRoom room) {
        room.getPlayers().forEach(player -> {
            var availableActions = availableActionService.getAvailableActions(room, player);
            webSocketBroadcaster.sendPrivateEvent(
                    player.getPlayerId(),
                    WsEvent.of(
                            WsEventType.AVAILABLE_ACTIONS_UPDATED,
                            new AvailableActionsUpdatedView(
                                    player.getPlayerId(),
                                    room.getCurrentStage(),
                                    availableActions
                            )
                    )
            );
            agentCoordinator.notifyAvailableActions(room, player, availableActions);
        });
    }
}
