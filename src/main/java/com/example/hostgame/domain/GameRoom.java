package com.example.hostgame.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameRoom {

    private String roomId = UUID.randomUUID().toString();
    private GameType gameType = GameType.AI_CHAT_WEREWOLF;
    private RoomStatus roomStatus = RoomStatus.WAITING;
    private GameStage currentStage = GameStage.WAITING;
    private List<Player> players = new ArrayList<>();
    private List<ChatMessage> messages = new ArrayList<>();
    private List<Vote> votes = new ArrayList<>();
    private List<PlayerActionRecord> actionRecords = new ArrayList<>();
    private Instant createdAt = Instant.now();
    private Instant startedAt;
    private Instant stageStartedAt;
    private Instant stageEndsAt;
    private Instant endedAt;
    private int round;
    private GameState gameState = new GameState();
    private GameFlowConfig flowConfig = new GameFlowConfig();

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public RoomStatus getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(RoomStatus roomStatus) {
        this.roomStatus = roomStatus;
    }

    public GameStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(GameStage currentStage) {
        this.currentStage = currentStage;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public List<Vote> getVotes() {
        return votes;
    }

    public void setVotes(List<Vote> votes) {
        this.votes = votes;
    }

    public List<PlayerActionRecord> getActionRecords() {
        return actionRecords;
    }

    public void setActionRecords(List<PlayerActionRecord> actionRecords) {
        this.actionRecords = actionRecords;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getStageStartedAt() {
        return stageStartedAt;
    }

    public void setStageStartedAt(Instant stageStartedAt) {
        this.stageStartedAt = stageStartedAt;
    }

    public Instant getStageEndsAt() {
        return stageEndsAt;
    }

    public void setStageEndsAt(Instant stageEndsAt) {
        this.stageEndsAt = stageEndsAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public GameFlowConfig getFlowConfig() {
        return flowConfig;
    }

    public void setFlowConfig(GameFlowConfig flowConfig) {
        this.flowConfig = flowConfig;
    }
}
