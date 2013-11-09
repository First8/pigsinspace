package nl.first8.pigsinspace.gui;

import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.Direction;
import nl.first8.pigsinspace.Entity;

public class ShipDisplayer extends EntityDisplayer<Entity> {
    private static final String THEME = "swinetrek"; // "bug"
    private Sprite[] sprite = new Sprite[8];

    protected ShipDisplayer() {
        for (Direction d : Direction.values()) {
            sprite[d.ordinal()] = TextureLoader.getSprite(THEME + "-" + d.name() + ".png");
        }
    }

    /**
     * Draw the given entity
     * @param e the ship to draw
     * @param fs 
     */
    public void draw(Entity e, DrawMode mode, float[] color) {
        if (mode==DrawMode.ENTITY_COLORING) {
            DrawUtil.colored_rect( e.getX(),e.getY(), Configuration.ENTITY_WIDTH, Configuration.ENTITY_HEIGHT, color[0], color[1], color[2]);
        } else {
            DrawUtil.colored_rect( e.getX(),e.getY(), Configuration.ENTITY_WIDTH, Configuration.ENTITY_HEIGHT, 1.0f, 1.0f, 1.0f);
        }
        sprite[e.getDirection().ordinal()].draw(e.getX(), e.getY());
    }
    

    
}
