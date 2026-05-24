package com.example.hostgame.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerStatus;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailableActionServiceTest {

    private final AvailableActionService service = new AvailableActionService();

    @Test
    void alivePlayerCanSendMessageDuringDiscussion() {
        GameRoom room = new GameRoom();
        room.setCurrentStage(GameStage.DISCUSSION);
        Player player = new Player();

        assertThat(service.getAvailableActions(room, player))
                .extracting("actionType")
                .containsExactly(ActionType.SEND_MESSAGE);
    }

    @Test
    void eliminatedPlayerHasNoActions() {
        GameRoom room = new GameRoom();
        room.setCurrentStage(GameStage.DISCUSSION);
        Player player = new Player();
        player.setStatus(PlayerStatus.ELIMINATED);

        assertThat(service.getAvailableActions(room, player)).isEmpty();
    }

    @Test
    void votingActionsIncludeVoteTargetsExceptSelf() {
        GameRoom room = new GameRoom();
        room.setCurrentStage(GameStage.VOTING);
        Player voter = new Player();
        voter.setPlayerId("voter");
        voter.setPlayerNo(1);
        Player target = new Player();
        target.setPlayerId("target");
        target.setPlayerNo(2);
        room.setPlayers(List.of(voter, target));

        assertThat(service.getAvailableActions(room, voter))
                .extracting("actionType")
                .containsExactly(ActionType.SEND_MESSAGE, ActionType.SUBMIT_VOTE);
        assertThat(service.getAvailableActions(room, voter).get(1).getTargets())
                .extracting("playerId")
                .containsExactly("target");
    }

    @Test
    void endedStageHasNoActions() {
        GameRoom room = new GameRoom();
        room.setCurrentStage(GameStage.ENDED);
        Player player = new Player();

        assertThat(service.getAvailableActions(room, player)).isEmpty();
    }
}
