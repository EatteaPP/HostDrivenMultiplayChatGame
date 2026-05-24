package com.example.hostgame.service;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.ActionTarget;
import com.example.hostgame.domain.AvailableAction;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AvailableActionService {

    public List<AvailableAction> getAvailableActions(GameRoom room, Player player) {
        if (!player.isAlive()) {
            return List.of();
        }
        if (room.getCurrentStage() == GameStage.DISCUSSION || room.getCurrentStage() == GameStage.VOTING) {
            AvailableAction sendMessage = new AvailableAction(ActionType.SEND_MESSAGE);
            if (room.getCurrentStage() == GameStage.VOTING) {
                AvailableAction submitVote = new AvailableAction(ActionType.SUBMIT_VOTE);
                submitVote.setTargets(room.getPlayers().stream()
                        .filter(Player::isAlive)
                        .filter(target -> !target.getPlayerId().equals(player.getPlayerId()))
                        .map(target -> new ActionTarget(target.getPlayerId(), target.getPlayerNo()))
                        .toList());
                return List.of(sendMessage, submitVote);
            }
            return List.of(sendMessage);
        }
        return List.of();
    }
}
