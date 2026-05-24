package com.example.hostgame.agent;

import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AgentCoordinator {

    private final AgentController agentController;

    public AgentCoordinator(AgentController agentController) {
        this.agentController = agentController;
    }

    public void notifyAvailableActions(GameRoom room, Player player, List<AvailableAction> availableActions) {
        if (player.getControllerType() != PlayerControllerType.AI || availableActions.isEmpty()) {
            return;
        }
        agentController.onAvailableActionsUpdated(room, player, availableActions);
    }
}
