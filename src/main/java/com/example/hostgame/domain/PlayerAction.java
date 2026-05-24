package com.example.hostgame.domain;

import java.util.HashMap;
import java.util.Map;

public class PlayerAction {

    private ActionType actionType;
    private Map<String, Object> payload = new HashMap<>();

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
}
