package com.example.hostgame.service;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerAction;
import com.example.hostgame.domain.Vote;
import com.example.hostgame.flow.GameFlowEngine;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PlayerActionService {

    private final RoomService roomService;
    private final ChatService chatService;
    private final VoteService voteService;
    private final GameFlowEngine gameFlowEngine;

    public PlayerActionService(
            RoomService roomService,
            ChatService chatService,
            VoteService voteService,
            GameFlowEngine gameFlowEngine
    ) {
        this.roomService = roomService;
        this.chatService = chatService;
        this.voteService = voteService;
        this.gameFlowEngine = gameFlowEngine;
    }

    public Object submitAction(String roomId, String playerId, PlayerAction action) {
        if (action == null || action.getActionType() == null) {
            throw new ActionRejectedException("Action type is required.");
        }

        GameRoom room = roomService.getRoom(roomId);
        Player player = findPlayer(room, playerId);

        if (action.getActionType() == ActionType.SEND_MESSAGE) {
            return submitSendMessage(room, player, action.getPayload());
        }
        if (action.getActionType() == ActionType.SUBMIT_VOTE) {
            return submitVote(room, player, action.getPayload());
        }

        throw new ActionRejectedException("Unsupported action type: " + action.getActionType());
    }

    private ChatMessage submitSendMessage(GameRoom room, Player player, Map<String, Object> payload) {
        Object content = payload == null ? null : payload.get("content");
        if (!(content instanceof String text)) {
            throw new ActionRejectedException("SEND_MESSAGE requires string payload field: content.");
        }
        return chatService.sendMessage(room, player, text);
    }

    private Vote submitVote(GameRoom room, Player player, Map<String, Object> payload) {
        Object targetPlayerId = payload == null ? null : payload.get("targetPlayerId");
        if (!(targetPlayerId instanceof String text)) {
            throw new ActionRejectedException("SUBMIT_VOTE requires string payload field: targetPlayerId.");
        }
        Vote vote = voteService.submitVote(room, player, text);
        gameFlowEngine.handleActionCompleted(room.getRoomId());
        return vote;
    }

    private Player findPlayer(GameRoom room, String playerId) {
        return room.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ActionRejectedException("Player is not in the room."));
    }
}
