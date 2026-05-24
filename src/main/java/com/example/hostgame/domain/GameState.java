package com.example.hostgame.domain;

import java.util.HashMap;
import java.util.Map;

public class GameState {

    private Map<String, Object> values = new HashMap<>();

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
}
