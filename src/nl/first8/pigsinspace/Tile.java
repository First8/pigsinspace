package nl.first8.pigsinspace;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.lwjgl.util.Point;

import com.hazelcast.nio.DataSerializable;

public class Tile implements Serializable, DataSerializable {
    private static final long serialVersionUID = 1L;
    private static final Random RANDOM = new Random();
    private List<Entity> entities;
    private int tileId;
    private int colorId;

    public Tile() {
        // for deserialization only
        entities = new ArrayList<Entity>();
    }

    public Tile(int tileId) {
        this.tileId = tileId;
        entities = new ArrayList<Entity>();
        colorId = RANDOM.nextInt(3);
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void addEntity(Entity e) {
        entities.add(e);
    }

    @Override
    public String toString() {
        int tileX = TileUtil.getTileX(tileId);
        int tileY = TileUtil.getTileY(tileId);
        return "Tile" + tileId + " (" + tileX + "," + tileY + ")";
    }

    public int getId() {
        return tileId;
    }

    private int getMapX() {
        return TileUtil.getTileX(tileId) * Configuration.getInstance().getTileWidth();
    }

    private int getMapY() {
        return TileUtil.getTileY(tileId) * Configuration.getInstance().getTileHeight();
    }

    public Point getTopLeft() {
        return new Point(getMapX(), getMapY());
    }

    public Point getTopRight() {
        return new Point(getMapX() + Configuration.getInstance().getTileWidth() - 1, getMapY());
    }

    public Point getBottomLeft() {
        return new Point(getMapX(), getMapY() + Configuration.getInstance().getTileHeight() - 1);
    }

    public Point getBottomRight() {
        return new Point(getMapX() + Configuration.getInstance().getTileWidth(), getMapY() + Configuration.getInstance().getTileHeight() - 1);
    }

    public int getColorId() {
        return colorId;
    }

    @Override
    public void readData(DataInput in) throws IOException {
        tileId = in.readInt();
        colorId = in.readInt();
        int nr = in.readInt();
        entities = new ArrayList<Entity>(nr);
        for (int i = 0; i < nr; i++) {
            Entity entity = new Entity();
            entity.readData(in);
            entities.add(entity);
        }
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeInt(tileId);
        out.writeInt(colorId);
        out.writeInt(entities.size());
        for (Entity entity : entities) {
            entity.writeData(out);
        }

    }
}
