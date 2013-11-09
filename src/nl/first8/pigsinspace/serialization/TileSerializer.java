package nl.first8.pigsinspace.serialization;



// Hazelcast 3.0
public class TileSerializer {} /*implements StreamSerializer<Tile> {
    private static final int TYPE_ID = 12;

    public int getTypeId() {
        return TYPE_ID;
    }

    public void write(ObjectDataOutput out, Tile object) throws IOException {
        KryoWrapper.getInstance().writeObject(out, object);
    }

    public Tile read(ObjectDataInput in) throws IOException {
        return (Tile) KryoWrapper.getInstance().readObject(in, Tile.class);
    }

    public void destroy() {
    }
} */