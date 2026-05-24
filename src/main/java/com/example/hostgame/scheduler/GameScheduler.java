package com.example.hostgame.scheduler;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.flow.GameFlowEngine;
import com.example.hostgame.store.GameStore;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GameScheduler {

    private final GameStore gameStore;
    private final GameFlowEngine gameFlowEngine;

    public GameScheduler(GameStore gameStore, GameFlowEngine gameFlowEngine) {
        this.gameStore = gameStore;
        this.gameFlowEngine = gameFlowEngine;
    }

    @Scheduled(fixedDelay = 1000)
    public void advanceExpiredStages() {
        Instant now = Instant.now();
        for (GameRoom room : gameStore.getActiveRooms()) {
            gameFlowEngine.handleStageTimeout(room.getRoomId(), now);
        }
    }
}
