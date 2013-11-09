package nl.first8.pigsinspace.gui;

import java.util.HashMap;
import java.util.Map;

import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.Entity;
import nl.first8.pigsinspace.Tile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TileDisplayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TileDisplayer.class);
    private ShipDisplayer shipDisplayer;

    private static final int OVERLAP = 1;
    // private Sprite cross;

    private static int nextColorAssignment = 0;

    private final static float colors[][] = new float[][] { 
            { 255f / 255f, 123f / 255f, 126f / 255f }, // red
            { 193f / 255f, 252f / 255f, 220f / 255f }, // green
            { 236f / 255f, 164f / 255f, 84f / 255f }, // orange
            { 166f / 255f, 127f / 255f, 199f / 255f }, // purple
            { 198f / 255f, 117f / 255f, 159f / 255f }, // dark pink
            { 237f / 255f, 234f / 255f, 165f / 255f }, // yellow
            { 126f / 255f, 180f / 255f, 231f / 255f }, // blue
            { 238f / 255f, 161f / 255f, 193f / 255f }, // pink

            // {1.0f,0f,0f}, // red
            // {0f,1f, 0f}, // green
            // {1f, 0.5f, 0f}, // orange
            // {0.5f, 0, 1f}, //purple
            // {1f,0f,0.9f}, // dark pink
            // {1f,1f,0f}, // yellow
            // {0f,0f,1f}, // blue
            // {1f,0.8f,0.9f}, // pink
            { 1f, 1f, 1f } // white
    };
    private final static Map<String, Integer> colorAssignments = new HashMap<String, Integer>();
    static {
        colorAssignments.put("10.200.1.122", 0); // red
        colorAssignments.put("10.200.1.149", 1); // green
        colorAssignments.put("10.200.1.134", 2); // orange
        colorAssignments.put("10.200.1.137", 3); // purple
        colorAssignments.put("10.200.1.139", 4); // darkpink
        colorAssignments.put("10.200.1.138", 5); // yellow
        colorAssignments.put("10.200.1.133", 6); // blue
        colorAssignments.put("10.200.1.100", 7); // pink
    }

    int colorIndex = 0;
    private Sprite background;

    public TileDisplayer() {
        shipDisplayer = new ShipDisplayer();
        background = TextureLoader.getSprite("space.jpg");
    }

    public void drawTile(Tile tile, DrawMode mode, String ip) {
        if (mode == DrawMode.TILE_COLORING) {
            float color[] = getColorAssignment(ip);
            DrawUtil.colored_rect(tile.getTopLeft().getX() - OVERLAP, tile.getTopLeft().getY() - OVERLAP, Configuration.getInstance().getTileWidth() + OVERLAP * 2, Configuration.getInstance().getTileHeight() + OVERLAP * 2, color[0], color[1], color[2]);
        } else {
            DrawUtil.colored_rect(tile.getTopLeft().getX() - OVERLAP, tile.getTopLeft().getY() - OVERLAP, Configuration.getInstance().getTileWidth() + OVERLAP * 2, Configuration.getInstance().getTileHeight() + OVERLAP * 2, 1.0f, 1.0f, 1.0f);
        }
        background.draw(tile.getTopLeft().getX() - OVERLAP, tile.getTopLeft().getY() - OVERLAP, Configuration.getInstance().getTileWidth() + OVERLAP * 2, Configuration.getInstance().getTileHeight() + OVERLAP * 2, true);
    }

    public void drawEntities(Tile tile, DrawMode mode, String ip) {
        for (Entity entity : tile.getEntities()) {
            Entity e = (Entity) entity;
            shipDisplayer.draw(e, mode, getColorAssignment(ip));
        }
    }

    private float[] getColorAssignment(String ip) {
        Integer colorAssignment = colorAssignments.get(ip);
        if (colorAssignment == null) {
            colorAssignment = nextColorAssignment;
            nextColorAssignment = (nextColorAssignment + 1) % colors.length;

            colorAssignments.put(ip, colorAssignment);
            LOGGER.debug("Member " + ip + " got color " + colorIndex);
        }
        return colors[colorAssignment];
    }

}
