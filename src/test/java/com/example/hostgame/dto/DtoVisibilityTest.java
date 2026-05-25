package com.example.hostgame.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerControllerType;
import com.example.hostgame.domain.RoomObjective;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class DtoVisibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void roomViewDoesNotExposeControllerTypeBeforeGameEnds() throws Exception {
        GameRoom room = new GameRoom();
        Player ai = new Player();
        ai.setPlayerNo(1);
        ai.setControllerType(PlayerControllerType.AI);
        room.getPlayers().add(ai);

        String json = objectMapper.writeValueAsString(RoomView.from(room));

        assertThat(json).doesNotContain("controllerType");
        assertThat(json).doesNotContain("\"controllerType\":\"AI\"");
        assertThat(json).doesNotContain("\"controllerType\":\"HUMAN\"");
    }

    @Test
    void gameEndedViewCanExposeRevealView() throws Exception {
        GameRoom room = new GameRoom();
        room.setEndedAt(Instant.now());
        Player ai = new Player();
        ai.setPlayerNo(1);
        ai.setControllerType(PlayerControllerType.AI);
        room.getPlayers().add(ai);

        GameEndedView view = new GameEndedView(
                room.getRoomId(),
                RoomObjective.FIND_AI,
                "FINAL_GROUP",
                "AI_SURVIVED",
                "One AI player survived to the final group.",
                0,
                1,
                0,
                1,
                room.getPlayers().stream().map(PlayerRevealView::from).toList(),
                room.getEndedAt()
        );
        String json = objectMapper.writeValueAsString(view);

        assertThat(json).contains("controllerType");
        assertThat(json).contains("AI");
    }
}
