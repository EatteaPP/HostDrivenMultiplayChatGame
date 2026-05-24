package com.example.hostgame.service;

import com.example.hostgame.domain.ChatMessage;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.MessageAudience;
import com.example.hostgame.domain.MessageType;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerActionRecord;
import com.example.hostgame.domain.ActionType;
import com.example.hostgame.store.GameStore;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final GameStore gameStore;

    public ChatService(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    public ChatMessage sendMessage(GameRoom room, Player player, String content) {
        validateCanSendMessage(room, player, content);

        Instant now = Instant.now();
        ChatMessage message = new ChatMessage();
        message.setRoomId(room.getRoomId());
        message.setMessageType(MessageType.PLAYER);
        message.setAudience(MessageAudience.publicAudience());
        message.setPlayerId(player.getPlayerId());
        message.setPlayerNo(player.getPlayerNo());
        message.setSpeakerName("Player " + player.getPlayerNo());
        message.setContent(content.trim());
        message.setCreatedAt(now);

        PlayerActionRecord record = new PlayerActionRecord();
        record.setRoomId(room.getRoomId());
        record.setPlayerId(player.getPlayerId());
        record.setActionType(ActionType.SEND_MESSAGE);
        record.setPayload(Map.of("content", message.getContent()));
        record.setCreatedAt(now);

        player.setLastMessageAt(now);
        room.getMessages().add(message);
        room.getActionRecords().add(record);
        gameStore.saveRoom(room);
        return message;
    }

    private void validateCanSendMessage(GameRoom room, Player player, String content) {
        if (!player.isAlive()) {
            throw new ActionRejectedException("Eliminated players cannot send messages.");
        }
        if (room.getCurrentStage() != GameStage.DISCUSSION && room.getCurrentStage() != GameStage.VOTING) {
            throw new ActionRejectedException("Messages are not allowed in the current stage.");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new ActionRejectedException("Message content is required.");
        }
        if (content.trim().length() > MAX_MESSAGE_LENGTH) {
            throw new ActionRejectedException("Message content is too long.");
        }
        if (player.getLastMessageAt() != null) {
            long elapsedSeconds = Duration.between(player.getLastMessageAt(), Instant.now()).getSeconds();
            if (elapsedSeconds < room.getFlowConfig().getMessageCooldownSeconds()) {
                throw new ActionRejectedException("Message cooldown is still active.");
            }
        }
    }
}
