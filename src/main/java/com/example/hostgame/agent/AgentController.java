package com.example.hostgame.agent;

import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import java.util.List;

public interface AgentController {

    void onAvailableActionsUpdated(GameRoom room, Player player, List<AvailableAction> availableActions);
}
