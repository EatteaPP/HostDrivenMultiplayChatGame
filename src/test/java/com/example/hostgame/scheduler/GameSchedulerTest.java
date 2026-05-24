package com.example.hostgame.scheduler;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.flow.GameFlowEngine;
import com.example.hostgame.store.GameStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GameSchedulerTest {

    private final GameStore gameStore = org.mockito.Mockito.mock(GameStore.class);
    private final GameFlowEngine gameFlowEngine = org.mockito.Mockito.mock(GameFlowEngine.class);
    private final GameScheduler scheduler = new GameScheduler(gameStore, gameFlowEngine);

    @Test
    void schedulerDelegatesActiveRoomsToGameFlowEngine() {
        GameRoom room = new GameRoom();
        when(gameStore.getActiveRooms()).thenReturn(List.of(room));

        scheduler.advanceExpiredStages();

        verify(gameFlowEngine).handleStageTimeout(eq(room.getRoomId()), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void schedulerDoesNothingWhenNoActiveRoomsExist() {
        when(gameStore.getActiveRooms()).thenReturn(List.of());

        scheduler.advanceExpiredStages();

        verifyNoInteractions(gameFlowEngine);
    }
}
