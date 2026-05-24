package com.example.hostgame.agent;

import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import java.util.List;

public class NoopAgentController implements AgentController {

    @Override
    public void onAvailableActionsUpdated(GameRoom room, Player player, List<AvailableAction> availableActions) {
        // Intentionally empty for tests or disabled AI.
    }
}
