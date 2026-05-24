package com.example.hostgame.store;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.RoomStatus;
import java.util.List;
import java.util.Optional;

public interface GameStore {

    Optional<GameRoom> getRoom(String roomId);

    GameRoom saveRoom(GameRoom room);

    List<GameRoom> findRooms(RoomStatus status);

    List<GameRoom> getActiveRooms();
}
