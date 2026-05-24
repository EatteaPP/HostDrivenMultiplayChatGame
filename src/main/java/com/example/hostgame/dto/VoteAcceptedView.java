package com.example.hostgame.dto;

import com.example.hostgame.domain.Vote;
import java.time.Instant;

public record VoteAcceptedView(
        String voteId,
        int round,
        String voterPlayerId,
        String targetPlayerId,
        Instant createdAt
) {

    public static VoteAcceptedView from(Vote vote) {
        return new VoteAcceptedView(
                vote.getVoteId(),
                vote.getRound(),
                vote.getVoterPlayerId(),
                vote.getTargetPlayerId(),
                vote.getCreatedAt()
        );
    }
}
