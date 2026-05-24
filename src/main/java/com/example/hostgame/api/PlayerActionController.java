package com.example.hostgame.api;

import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.PlayerAction;
import com.example.hostgame.domain.Vote;
import com.example.hostgame.dto.ChatMessageView;
import com.example.hostgame.dto.PlayerActionRequest;
import com.example.hostgame.dto.PlayerActionResponse;
import com.example.hostgame.dto.VoteAcceptedView;
import com.example.hostgame.dto.AvailableActionsUpdatedView;
import com.example.hostgame.service.AvailableActionService;
import com.example.hostgame.service.PlayerActionService;
import com.example.hostgame.service.RoomService;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import com.example.hostgame.websocket.WsEventType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms/{roomId}/players/{playerId}/actions")
public class PlayerActionController {

    private final PlayerActionService playerActionService;
    private final RoomService roomService;
    private final AvailableActionService availableActionService;
    private final WebSocketBroadcaster webSocketBroadcaster;

    public PlayerActionController(
            PlayerActionService playerActionService,
            RoomService roomService,
            AvailableActionService availableActionService,
            WebSocketBroadcaster webSocketBroadcaster
    ) {
        this.playerActionService = playerActionService;
        this.roomService = roomService;
        this.availableActionService = availableActionService;
        this.webSocketBroadcaster = webSocketBroadcaster;
    }

    @GetMapping
    public AvailableActionsUpdatedView getAvailableActions(
            @PathVariable String roomId,
            @PathVariable String playerId
    ) {
        var room = roomService.getRoom(roomId);
        var player = room.getPlayers().stream()
                .filter(candidate -> candidate.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new com.example.hostgame.service.ActionRejectedException("Player is not in the room."));
        return new AvailableActionsUpdatedView(
                playerId,
                room.getCurrentStage(),
                availableActionService.getAvailableActions(room, player)
        );
    }

    @PostMapping
    public PlayerActionResponse submitAction(
            @PathVariable String roomId,
            @PathVariable String playerId,
            @RequestBody PlayerActionRequest request
    ) {
        PlayerAction action = new PlayerAction();
        action.setActionType(request.actionType());
        action.setPayload(request.safePayload());

        Object result = playerActionService.submitAction(roomId, playerId, action);
        Object responseResult = toResponseResult(roomId, result);
        return PlayerActionResponse.accepted(action.getActionType(), responseResult);
    }

    private Object toResponseResult(String roomId, Object result) {
        if (result instanceof ChatMessage message) {
            ChatMessageView messageView = ChatMessageView.from(message);
            webSocketBroadcaster.broadcastRoomEvent(
                    roomId,
                    WsEvent.of(WsEventType.MESSAGE_CREATED, messageView)
            );
            return messageView;
        }
        if (result instanceof Vote vote) {
            VoteAcceptedView voteView = VoteAcceptedView.from(vote);
            webSocketBroadcaster.sendPrivateEvent(
                    vote.getVoterPlayerId(),
                    WsEvent.of(WsEventType.ACTION_ACCEPTED, voteView)
            );
            return voteView;
        }
        return result;
    }
}
