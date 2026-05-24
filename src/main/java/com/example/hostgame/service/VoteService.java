package com.example.hostgame.service;

import com.example.hostgame.domain.ActionType;
import com.example.hostgame.domain.GameRoom;
import com.example.hostgame.domain.GameStage;
import com.example.hostgame.domain.Player;
import com.example.hostgame.domain.PlayerActionRecord;
import com.example.hostgame.domain.Vote;
import com.example.hostgame.store.GameStore;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class VoteService {

    private final GameStore gameStore;

    public VoteService(GameStore gameStore) {
        this.gameStore = gameStore;
    }

    public Vote submitVote(GameRoom room, Player voter, String targetPlayerId) {
        Player target = findPlayer(room, targetPlayerId);
        validateVote(room, voter, target);

        Instant now = Instant.now();
        Vote vote = new Vote();
        vote.setRound(room.getRound());
        vote.setVoterPlayerId(voter.getPlayerId());
        vote.setTargetPlayerId(target.getPlayerId());
        vote.setCreatedAt(now);

        PlayerActionRecord record = new PlayerActionRecord();
        record.setRoomId(room.getRoomId());
        record.setPlayerId(voter.getPlayerId());
        record.setActionType(ActionType.SUBMIT_VOTE);
        record.setPayload(Map.of("targetPlayerId", target.getPlayerId()));
        record.setCreatedAt(now);

        room.getVotes().add(vote);
        room.getActionRecords().add(record);
        gameStore.saveRoom(room);
        return vote;
    }

    private void validateVote(GameRoom room, Player voter, Player target) {
        if (room.getCurrentStage() != GameStage.VOTING) {
            throw new ActionRejectedException("Votes are only allowed during voting stage.");
        }
        if (!voter.isAlive()) {
            throw new ActionRejectedException("Eliminated players cannot vote.");
        }
        if (!target.isAlive()) {
            throw new ActionRejectedException("Cannot vote for an eliminated player.");
        }
        if (voter.getPlayerId().equals(target.getPlayerId())) {
            throw new ActionRejectedException("Players cannot vote for themselves.");
        }
        boolean alreadyVoted = room.getVotes().stream()
                .anyMatch(vote -> vote.getRound() == room.getRound()
                        && vote.getVoterPlayerId().equals(voter.getPlayerId()));
        if (alreadyVoted) {
            throw new ActionRejectedException("Player has already voted.");
        }
    }

    private Player findPlayer(GameRoom room, String playerId) {
        return room.getPlayers().stream()
                .filter(player -> player.getPlayerId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ActionRejectedException("Vote target is not in the room."));
    }
}
