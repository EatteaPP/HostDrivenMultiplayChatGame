package com.example.hostgame.domain;

import java.util.HashSet;
import java.util.Set;

public class MessageAudience {

    private AudienceType type = AudienceType.PUBLIC;
    private Set<String> targetPlayerIds = new HashSet<>();
    private Faction faction;
    private GameRole role;

    public static MessageAudience publicAudience() {
        MessageAudience audience = new MessageAudience();
        audience.setType(AudienceType.PUBLIC);
        return audience;
    }

    public AudienceType getType() {
        return type;
    }

    public void setType(AudienceType type) {
        this.type = type;
    }

    public Set<String> getTargetPlayerIds() {
        return targetPlayerIds;
    }

    public void setTargetPlayerIds(Set<String> targetPlayerIds) {
        this.targetPlayerIds = targetPlayerIds;
    }

    public Faction getFaction() {
        return faction;
    }

    public void setFaction(Faction faction) {
        this.faction = faction;
    }

    public GameRole getRole() {
        return role;
    }

    public void setRole(GameRole role) {
        this.role = role;
    }
}
