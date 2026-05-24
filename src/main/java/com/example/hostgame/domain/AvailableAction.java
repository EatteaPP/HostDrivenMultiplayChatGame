package com.example.hostgame.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AvailableAction {

    private ActionType actionType;
    private List<ActionTarget> targets = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    public AvailableAction() {
    }

    public AvailableAction(ActionType actionType) {
        this.actionType = actionType;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public List<ActionTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<ActionTarget> targets) {
        this.targets = targets;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
