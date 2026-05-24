package com.example.hostgame.store;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.RoomStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryGameStore implements GameStore {

    private final ConcurrentMap<String, GameRoom> rooms = new ConcurrentHashMap<>();

    @Override
    public Optional<GameRoom> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    @Override
    public GameRoom saveRoom(GameRoom room) {
        rooms.put(room.getRoomId(), room);
        return room;
    }

    @Override
    public List<GameRoom> findRooms(RoomStatus status) {
        return rooms.values().stream()
                .filter(room -> room.getRoomStatus() == status)
                .toList();
    }

    @Override
    public List<GameRoom> getActiveRooms() {
        return rooms.values().stream()
                .filter(room -> room.getRoomStatus() != RoomStatus.ENDED)
                .toList();
    }

    public List<GameRoom> getRooms() {
        return new ArrayList<>(rooms.values());
    }
}
