package com.example.hostgame.api;

import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.RoomStatus;
import com.example.hostgame.dto.CreateRoomRequest;
import com.example.hostgame.dto.JoinRoomRequest;
import com.example.hostgame.dto.JoinRoomResponse;
import com.example.hostgame.dto.PlayerPublicView;
import com.example.hostgame.dto.RoomView;
import com.example.hostgame.flow.GameFlowEngine;
import com.example.hostgame.service.RoomService;
import com.example.hostgame.websocket.WebSocketBroadcaster;
import com.example.hostgame.websocket.WsEvent;
import com.example.hostgame.websocket.WsEventType;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final GameFlowEngine gameFlowEngine;
    private final WebSocketBroadcaster webSocketBroadcaster;

    public RoomController(
            RoomService roomService,
            GameFlowEngine gameFlowEngine,
            WebSocketBroadcaster webSocketBroadcaster
    ) {
        this.roomService = roomService;
        this.gameFlowEngine = gameFlowEngine;
        this.webSocketBroadcaster = webSocketBroadcaster;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomView createRoom(@RequestBody(required = false) CreateRoomRequest request) {
        GameRoom room = roomService.createRoom(request == null ? null : request.gameType());
        RoomView roomView = RoomView.from(room);
        webSocketBroadcaster.broadcastRoomEvent(
                room.getRoomId(),
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, roomView)
        );
        return roomView;
    }

    @GetMapping
    public List<RoomView> listRooms(@RequestParam(required = false) RoomStatus status) {
        return roomService.findRooms(status).stream()
                .map(RoomView::from)
                .toList();
    }

    @PostMapping("/{roomId}/join")
    public JoinRoomResponse joinRoom(
            @PathVariable String roomId,
            @RequestBody(required = false) JoinRoomRequest request
    ) {
        PlayerPublicView player = PlayerPublicView.from(roomService.joinRoom(roomId));
        RoomView roomView = RoomView.from(roomService.getRoom(roomId));
        webSocketBroadcaster.broadcastRoomEvent(
                roomId,
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, roomView)
        );
        return new JoinRoomResponse(roomView, player);
    }

    @PostMapping("/{roomId}/ai-players")
    public RoomView addAiPlayer(@PathVariable String roomId) {
        roomService.joinAiPlayer(roomId);
        RoomView roomView = RoomView.from(roomService.getRoom(roomId));
        webSocketBroadcaster.broadcastRoomEvent(
                roomId,
                WsEvent.of(WsEventType.ROOM_STATE_UPDATED, roomView)
        );
        return roomView;
    }

    @GetMapping("/{roomId}")
    public RoomView getRoom(@PathVariable String roomId) {
        return RoomView.from(roomService.getRoom(roomId));
    }

    @PostMapping("/{roomId}/start")
    public RoomView startRoom(@PathVariable String roomId) {
        return RoomView.from(gameFlowEngine.startGame(roomId));
    }
}
