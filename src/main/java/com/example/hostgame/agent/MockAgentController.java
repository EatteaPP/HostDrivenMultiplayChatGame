package com.example.hostgame.agent;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerAction;
import com.example.hostgame.service.ActionRejectedException;
import com.example.hostgame.service.PlayerActionService;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class MockAgentController implements AgentController {

    private final ObjectProvider<PlayerActionService> playerActionServiceProvider;
    private final Set<String> completedActionKeys = new HashSet<>();

    public MockAgentController(ObjectProvider<PlayerActionService> playerActionServiceProvider) {
        this.playerActionServiceProvider = playerActionServiceProvider;
    }

    @Override
    public void onAvailableActionsUpdated(GameRoom room, Player player, List<AvailableAction> availableActions) {
        Optional<AvailableAction> voteAction = findAction(availableActions, ActionType.SUBMIT_VOTE);
        if (voteAction.isPresent() && !voteAction.get().getTargets().isEmpty()) {
            submitOnce(
                    room,
                    player,
                    ActionType.SUBMIT_VOTE,
                    Map.of("targetPlayerId", voteAction.get().getTargets().get(0).getPlayerId())
            );
            return;
        }

        Optional<AvailableAction> messageAction = findAction(availableActions, ActionType.SEND_MESSAGE);
        messageAction.ifPresent(action -> submitOnce(
                room,
                player,
                ActionType.SEND_MESSAGE,
                Map.of("content", "I am thinking this through.")
        ));
    }

    private Optional<AvailableAction> findAction(List<AvailableAction> availableActions, ActionType actionType) {
        return availableActions.stream()
                .filter(action -> action.getActionType() == actionType)
                .findFirst();
    }

    private void submitOnce(GameRoom room, Player player, ActionType actionType, Map<String, Object> payload) {
        String key = room.getRoomId() + ":" + room.getRound() + ":" + room.getCurrentStage() + ":"
                + player.getPlayerId() + ":" + actionType;
        if (!completedActionKeys.add(key)) {
            return;
        }

        PlayerAction action = new PlayerAction();
        action.setActionType(actionType);
        action.setPayload(payload);
        try {
            playerActionServiceProvider.getObject().submitAction(room.getRoomId(), player.getPlayerId(), action);
        } catch (ActionRejectedException ignored) {
            completedActionKeys.remove(key);
        }
    }
}
