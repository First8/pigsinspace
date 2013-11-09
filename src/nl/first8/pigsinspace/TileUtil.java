package nl.first8.pigsinspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.IMap;

public class TileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(TileUtil.class);

    public static int getTileId(Entity entity) {
        return getTileId(entity.getX(), entity.getY());
    }

    public static int getTileId(int x, int y) {
        int tileX = x / Configuration.getInstance().getTileWidth();
        int tileY = y / Configuration.getInstance().getTileHeight();
        return tileX + tileY * Configuration.getInstance().getNrOfHorizontalTiles();
    }

    public static int getTileX(int tileId) {
        return tileId % Configuration.getInstance().getNrOfHorizontalTiles();
    }

    public static int getTileY(int tileId) {
        return tileId / Configuration.getInstance().getNrOfHorizontalTiles();
    }

    public static int getMapX(int tileId) {
        return getTileX(tileId) * Configuration.getInstance().getTileWidth();
    }

    public static int getMapY(int tileId) {
        return getTileY(tileId) * Configuration.getInstance().getTileHeight();
    }

    public static List<Integer> getAllTiles(Entity entity) {
        List<Integer> allTiles = new ArrayList<Integer>(8);

        int tileId = getTileId(entity);
        allTiles.add(tileId);

        int tileWidth = Configuration.getInstance().getTileWidth();
        int tileHeight = Configuration.getInstance().getTileHeight();
        int nrOfHorizontalTiles = Configuration.getInstance().getNrOfHorizontalTiles();
        
        // check if tiles to the right or bottom are hit as well
        boolean right = ((entity.getX() + Configuration.ENTITY_WIDTH) / tileWidth) != (entity.getX() / tileWidth);
        boolean down = ((entity.getY() + Configuration.ENTITY_HEIGHT) / tileHeight) != (entity.getY() / tileHeight);
        // also check other edges: ships might overlap there
        boolean left = ((entity.getX() - Configuration.ENTITY_WIDTH) / tileWidth) != (entity.getX() / tileWidth);
        boolean up = ((entity.getY() - Configuration.ENTITY_HEIGHT) / tileHeight) != (entity.getY() / tileHeight);

        if (right) {
            allTiles.add(tileId + 1);
        }
        if (left) {
            allTiles.add(tileId - 1);
        }
        if (down) {
            allTiles.add(tileId + nrOfHorizontalTiles);
        }
        if (up) {
            allTiles.add(tileId - nrOfHorizontalTiles);
        }
        if (right && down) {
            allTiles.add(tileId + nrOfHorizontalTiles + 1);
        }
        if (left && down) {
            allTiles.add(tileId + nrOfHorizontalTiles - 1);
        }
        if (right && up) {
            allTiles.add(tileId - nrOfHorizontalTiles + 1);
        }
        if (left && up) {
            allTiles.add(tileId - nrOfHorizontalTiles - 1);
        }

        return allTiles;
    }

    @SuppressWarnings("unchecked")
    public static void addToMap(int tileId, Entity entity, IMap<Object, Object> map) {
        map.lock(tileId);
        try {
            Set<Entity> entities = (Set<Entity>) map.get(tileId);
            if (entities==null) {
                entities = new HashSet<Entity>();
            }
            entities.add(entity);
            map.put(tileId, entities);
        } finally {
            map.unlock(tileId);
        }
    }
    
    public static boolean collides(Tile tile, Entity entity) {
        try {
            boolean collides = false;
            for (Entity e : tile.getEntities()) {
                if (e.collidesWith(entity)) {
//                    se.bounce();
                    return true;
                }
            }
            return collides;
        } catch( Exception e) {
            LOGGER.error("Unable to detect collision on a tile, assuming collision: " + e.getMessage(), e);
            return true;
        }
    }

    public static Collection<Integer> getNeighbouringTiles(Integer tileId) {
        HashSet<Integer> neighbouringTiles = new HashSet<Integer>(8);
        
        int tileWidth = Configuration.getInstance().getTileWidth();
        int tileHeight = Configuration.getInstance().getTileHeight();
        int nrOfHorizontalTiles = Configuration.getInstance().getNrOfHorizontalTiles();

        
        boolean up = getTileY(tileId)>0;
        boolean down = getTileY(tileId)<tileHeight-1;
        boolean left = getTileX(tileId)>0;
        boolean right = getTileX(tileId)<tileWidth-1;
        
        if (right) {
            neighbouringTiles.add(tileId + 1);
        }
        if (left) {
            neighbouringTiles.add(tileId - 1);
        }
        if (down) {
            neighbouringTiles.add(tileId + nrOfHorizontalTiles);
        }
        if (up) {
            neighbouringTiles.add(tileId - nrOfHorizontalTiles);
        }
        if (right && down) {
            neighbouringTiles.add(tileId + nrOfHorizontalTiles + 1);
        }
        if (left && down) {
            neighbouringTiles.add(tileId + nrOfHorizontalTiles - 1);
        }
        if (right && up) {
            neighbouringTiles.add(tileId - nrOfHorizontalTiles + 1);
        }
        if (left && up) {
            neighbouringTiles.add(tileId - nrOfHorizontalTiles - 1);
        }
        
        LOGGER.debug("Tile " + tileId + " borders to: ");
        for (Integer t : neighbouringTiles) {
            LOGGER.debug(" * " + t);
        }
        return neighbouringTiles;
    }


}
