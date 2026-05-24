package com.example.hostgame.service;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.RoomStatus;
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

    public GameRoom createRoom(GameType requestedGameType) {
        GameRoom room = new GameRoom();
        room.setGameType(requestedGameType == null ? GameType.AI_CHAT_WEREWOLF : requestedGameType);
        return gameStore.saveRoom(room);
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
        return joinRoom(roomId, PlayerControllerType.AI);
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
