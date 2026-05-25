package com.example.hostgame.flow;

import com.example.hostgame.agent.AgentCoordinator;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameRole;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerStatus;
import com.example.hostgame.domain.RoomObjective;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.domain.Vote;
import com.example.hostgame.domain.Faction;
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
import java.util.LinkedHashMap;
import java.util.List;
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
        if (room.getPlayers().size() < room.getFlowConfig().getMinPlayersToStart()) {
            throw new ActionRejectedException(
                    "At least " + room.getFlowConfig().getMinPlayersToStart() + " players are required to start."
            );
        }

        Instant now = Instant.now();
        room.setRoomStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentStage(GameStage.DISCUSSION);
        room.setStartedAt(now);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now.plusSeconds(room.getFlowConfig().getDiscussionSeconds()));
        room.setRound(1);
        assignHiddenIdentities(room);
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

    public synchronized GameRoom handlePlayerAvailabilityChanged(String roomId) {
        GameRoom room = roomService.getRoom(roomId);
        if (room.getRoomStatus() != RoomStatus.IN_PROGRESS || room.getCurrentStage() == GameStage.ENDED) {
            return room;
        }
        Instant now = Instant.now();
        if (room.getCurrentStage() == GameStage.VOTING && allAlivePlayersVoted(room)) {
            return enterElimination(room, now);
        }
        if (alivePlayerCount(room) <= room.getFlowConfig().getEndWhenAlivePlayersLE()) {
            return enterEnded(room, now, "FINAL_GROUP");
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
        hostService.announcePublicMessage(room.getRoomId(), buildVoteResultMessage(room));
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

        if (alivePlayerCount(room) <= room.getFlowConfig().getEndWhenAlivePlayersLE()) {
            return enterEnded(room, now, "FINAL_GROUP");
        }
        if (room.getRound() >= room.getFlowConfig().getMaxRounds()) {
            return enterEnded(room, now, "MAX_ROUNDS_REACHED");
        }
        return enterNextDiscussion(room, now);
    }

    private Optional<Player> resolveElimination(GameRoom room) {
        Map<String, Long> voteCounts = buildVoteCountsByTargetPlayerId(room);
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

    private Map<String, Long> buildVoteCountsByTargetPlayerId(GameRoom room) {
        return room.getVotes().stream()
                .filter(vote -> vote.getRound() == room.getRound())
                .collect(Collectors.groupingBy(Vote::getTargetPlayerId, Collectors.counting()));
    }

    private String buildVoteResultMessage(GameRoom room) {
        Map<String, Long> voteCounts = buildVoteCountsByTargetPlayerId(room);
        if (voteCounts.isEmpty()) {
            return "Vote result: no votes.";
        }

        Map<Integer, Long> voteByPlayerNo = voteCounts.entrySet().stream()
                .map(entry -> Map.entry(resolvePlayerNo(room, entry.getKey()), entry.getValue()))
                .filter(entry -> entry.getKey() != null && entry.getValue() > 0)
                .sorted((a, b) -> {
                    int byVoteDesc = Long.compare(b.getValue(), a.getValue());
                    if (byVoteDesc != 0) {
                        return byVoteDesc;
                    }
                    return Integer.compare(a.getKey(), b.getKey());
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (voteByPlayerNo.isEmpty()) {
            return "Vote result: no votes.";
        }

        String summary = voteByPlayerNo.entrySet().stream()
                .map(entry -> "Player " + entry.getKey() + " : " + entry.getValue() + " vote")
                .collect(Collectors.joining(" / "));
        return "Vote result: " + summary;
    }

    private Integer resolvePlayerNo(GameRoom room, String playerId) {
        return room.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .map(Player::getPlayerNo)
                .findFirst()
                .orElse(null);
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

    private GameRoom enterEnded(GameRoom room, Instant now, String reasonCode) {
        room.setRoomStatus(RoomStatus.ENDED);
        room.setCurrentStage(GameStage.ENDED);
        room.setStageStartedAt(now);
        room.setStageEndsAt(now);
        room.setEndedAt(now);
        gameStore.saveRoom(room);

        if ("MAX_ROUNDS_REACHED".equals(reasonCode)) {
            hostService.announcePublicMessage(
                    room.getRoomId(),
                    "Game ended after reaching max rounds. Revealing final identities."
            );
        } else {
            hostService.announcePublicMessage(room.getRoomId(), "Game ended. Revealing final identities.");
        }
        broadcastStageChanged(room);
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.GAME_ENDED, buildGameEndedView(room, reasonCode))
        );
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, RoomView.from(room))
        );
        hostService.announcePublicMessage(room.getRoomId(), buildFinalSummaryMessage(room));
        broadcastAvailableActions(room);
        return room;
    }

    private GameEndedView buildGameEndedView(GameRoom room, String reasonCode) {
        int aliveAiCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> player.getControllerType() == PlayerControllerType.AI)
                .count();
        int aliveHumanCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> player.getControllerType() == PlayerControllerType.HUMAN)
                .count();
        int aliveTraitorCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(this::isTraitor)
                .count();
        int aliveCrewCount = (int) room.getPlayers().stream()
                .filter(Player::isAlive)
                .filter(player -> !isTraitor(player))
                .count();

        String resultCode;
        String resultMessage;
        if (aliveTraitorCount > 0) {
            resultCode = room.getObjective() == RoomObjective.FIND_AI ? "AI_WIN" : "TRAITOR_WIN";
            if (room.getObjective() == RoomObjective.FIND_AI) {
                resultMessage = "At least one AI survived to the final verdict.";
            } else {
                resultMessage = "At least one traitor survived to the final verdict.";
            }
        } else {
            resultCode = room.getObjective() == RoomObjective.FIND_AI ? "HUMAN_WIN" : "CREW_WIN";
            if (room.getObjective() == RoomObjective.FIND_AI) {
                resultMessage = "All AI players were eliminated.";
            } else {
                resultMessage = "All traitors were eliminated.";
            }
        }
        if ("MAX_ROUNDS_REACHED".equals(reasonCode)) {
            resultMessage = resultMessage + " (Max rounds reached.)";
        }

        return new GameEndedView(
                room.getRoomId(),
                room.getObjective(),
                reasonCode,
                resultCode,
                resultMessage,
                aliveCrewCount,
                aliveTraitorCount,
                aliveHumanCount,
                aliveAiCount,
                room.getPlayers().stream()
                        .map(PlayerRevealView::from)
                        .toList(),
                room.getEndedAt()
        );
    }

    private void assignHiddenIdentities(GameRoom room) {
        if (room.getObjective() == RoomObjective.FIND_TRAITOR) {
            Player traitor = room.getPlayers().stream()
                    .filter(player -> player.getControllerType() == PlayerControllerType.HUMAN)
                    .findFirst()
                    .orElse(room.getPlayers().get(0));
            room.getPlayers().forEach(player -> {
                if (player.getPlayerId().equals(traitor.getPlayerId())) {
                    player.setFaction(Faction.WEREWOLF);
                    player.setRole(GameRole.WEREWOLF);
                } else {
                    player.setFaction(Faction.VILLAGE);
                    player.setRole(GameRole.VILLAGER);
                }
            });
            return;
        }

        room.getPlayers().forEach(player -> {
            if (player.getControllerType() == PlayerControllerType.AI) {
                player.setFaction(Faction.WEREWOLF);
                player.setRole(GameRole.WEREWOLF);
            } else {
                player.setFaction(Faction.VILLAGE);
                player.setRole(GameRole.VILLAGER);
            }
        });
    }

    private boolean isTraitor(Player player) {
        return player.getFaction() == Faction.WEREWOLF;
    }

    private String buildFinalSummaryMessage(GameRoom room) {
        List<Player> traitors = room.getPlayers().stream()
                .filter(this::isTraitor)
                .toList();
        String traitorList = traitors.isEmpty()
                ? "none"
                : traitors.stream()
                        .map(player -> "Player " + player.getPlayerNo())
                        .collect(Collectors.joining(", "));
        boolean traitorWin = room.getPlayers().stream()
                .filter(Player::isAlive)
                .anyMatch(this::isTraitor);
        String winnerText = traitorWin ? "Traitor side wins." : "Crew side wins.";
        return "Final result: " + winnerText + " Traitor(s): " + traitorList + ".";
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
