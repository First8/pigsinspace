package nl.first8.pigsinspace.server;

import java.io.Serializable;

import nl.first8.pigsinspace.Entity;

public class MoveEvent implements Serializable {
    private final Integer originalTile;
    private final Integer newTile;
    private final Entity entity;

    public MoveEvent(Entity entity, Integer originalTile, Integer newTile) {
        this.entity = entity;
        this.originalTile = originalTile;
        this.newTile = newTile;
    }

    public Integer getOriginalTile() {
        return originalTile;
    }

    public Integer getNewTile() {
        return newTile;
    }

    public Entity getEntity() {
        return entity;
    }
}
