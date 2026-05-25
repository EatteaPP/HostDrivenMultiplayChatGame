package com.example.hostgame.service;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.PlayerStatus;
import com.example.hostgame.domain.RoomObjective;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.dto.CreateRoomRequest;
import com.example.hostgame.store.GameStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private static final List<String> PLAYER_COLORS = List.of(
            "red",
            "blue",
            "green",
            "yellow",
            "purple",
            "cyan",
            "pink",
            "gray"
    );

    private final GameStore gameStore;

    public RoomService(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    public GameRoom createRoom(CreateRoomRequest request) {
        GameRoom room = new GameRoom();
        GameType requestedGameType = request == null ? null : request.gameType();
        room.setGameType(requestedGameType == null ? GameType.AI_CHAT_WEREWOLF : requestedGameType);
        RoomObjective requestedObjective = request == null ? null : request.objective();
        room.setObjective(requestedObjective == null ? RoomObjective.FIND_TRAITOR : requestedObjective);
        room.setObjectiveHint(normalizeObjectiveHint(request == null ? null : request.objectiveHint()));
        applyFlowConfig(room, request);
        return gameStore.saveRoom(room);
    }

    private String normalizeObjectiveHint(String objectiveHint) {
        if (objectiveHint == null) {
            return null;
        }
        String trimmed = objectiveHint.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 120) {
            throw new ActionRejectedException("objectiveHint must be 120 characters or fewer.");
        }
        return trimmed;
    }

    private void applyFlowConfig(GameRoom room, CreateRoomRequest request) {
        if (request == null) {
            return;
        }
        if (request.discussionSeconds() != null) {
            room.getFlowConfig().setDiscussionSeconds(requirePositive(request.discussionSeconds(), "discussionSeconds"));
        }
        if (request.votingSeconds() != null) {
            room.getFlowConfig().setVotingSeconds(requirePositive(request.votingSeconds(), "votingSeconds"));
        }
        if (request.messageCooldownSeconds() != null) {
            room.getFlowConfig().setMessageCooldownSeconds(
                    requirePositive(request.messageCooldownSeconds(), "messageCooldownSeconds")
            );
        }
        if (request.maxRounds() != null) {
            room.getFlowConfig().setMaxRounds(requirePositive(request.maxRounds(), "maxRounds"));
        }
        if (request.minPlayersToStart() != null) {
            room.getFlowConfig().setMinPlayersToStart(
                    requireAtLeast(request.minPlayersToStart(), 2, "minPlayersToStart")
            );
        }
        if (request.endWhenAlivePlayersLE() != null) {
            room.getFlowConfig().setEndWhenAlivePlayersLE(
                    requireAtLeast(request.endWhenAlivePlayersLE(), 1, "endWhenAlivePlayersLE")
            );
        }
        validateFlowThresholds(room);
    }

    private int requirePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new ActionRejectedException(fieldName + " must be greater than 0.");
        }
        return value;
    }

    private int requireAtLeast(int value, int min, String fieldName) {
        if (value < min) {
            throw new ActionRejectedException(fieldName + " must be at least " + min + ".");
        }
        return value;
    }

    private void validateFlowThresholds(GameRoom room) {
        int minPlayersToStart = room.getFlowConfig().getMinPlayersToStart();
        int endWhenAlivePlayersLE = room.getFlowConfig().getEndWhenAlivePlayersLE();
        if (endWhenAlivePlayersLE >= minPlayersToStart) {
            throw new ActionRejectedException("endWhenAlivePlayersLE must be less than minPlayersToStart.");
        }
    }

    public List<GameRoom> findRooms(RoomStatus status) {
        if (status == null) {
            return gameStore.getActiveRooms();
        }
        return gameStore.findRooms(status);
    }

    public GameRoom getRoom(String roomId) {
        return gameStore.getRoom(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));
    }

    public Player joinRoom(String roomId) {
        return joinRoom(roomId, PlayerControllerType.HUMAN);
    }

    public Player joinAiPlayer(String roomId) {
        GameRoom room = getRoom(roomId);
        if (room.getObjective() != RoomObjective.FIND_AI) {
            throw new ActionRejectedException("AI players are only allowed when objective is FIND_AI.");
        }
        return joinRoom(roomId, PlayerControllerType.AI);
    }

    public boolean disconnectPlayer(String roomId, String playerId) {
        GameRoom room = getRoom(roomId);
        Player player = room.getPlayers().stream()
                .filter(candidate -> candidate.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ActionRejectedException("Player is not in the room."));
        if (!player.isAlive()) {
            return false;
        }
        player.setStatus(PlayerStatus.ELIMINATED);
        gameStore.saveRoom(room);
        return true;
    }

    private Player joinRoom(String roomId, PlayerControllerType controllerType) {
        GameRoom room = getRoom(roomId);
        if (room.getRoomStatus() != RoomStatus.WAITING) {
            throw new RoomJoinException("Only waiting rooms can be joined.");
        }

        Player player = new Player();
        player.setPlayerNo(room.getPlayers().size() + 1);
        player.setColor(resolveColor(room.getPlayers().size()));
        player.setControllerType(controllerType);
        room.getPlayers().add(player);
        gameStore.saveRoom(room);
        return player;
    }

    private String resolveColor(int index) {
        if (index < PLAYER_COLORS.size()) {
            return PLAYER_COLORS.get(index);
        }
        return "player-" + (index + 1);
    }
}
