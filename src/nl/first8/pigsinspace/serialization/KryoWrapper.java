package nl.first8.pigsinspace.serialization;


// Hazelcast 3.0
public class KryoWrapper {} /* {
    private static final KryoWrapper INSTANCE = new KryoWrapper();
    private final Kryo kryo = new Kryo();
    private FieldSerializer<Tile> tileSerializer;
    private FieldSerializer<Entity> entitySerializer;
    
    private KryoWrapper() {
        kryo.setRegistrationRequired(true);
        
        // basic types
        kryo.register(java.util.ArrayList.class);
        kryo.register(Direction.class);
        kryo.register(MoveEvent.class);
        kryo.register(Tile.class);
        kryo.register(Entity.class);
        
        entitySerializer = new FieldSerializer<Entity>(kryo, Entity.class);
        kryo.register(Entity.class, entitySerializer);
        
        tileSerializer = new FieldSerializer<Tile>(kryo, Tile.class);
        CollectionSerializer entityListSerializer = new CollectionSerializer();
        entityListSerializer.setElementClass(Entity.class, entitySerializer);
        entityListSerializer.setElementsCanBeNull(false);
        tileSerializer.getField("entities").setSerializer(entityListSerializer);
        
        kryo.register(Tile.class, tileSerializer);
    }
    
    public static KryoWrapper getInstance() {
        return INSTANCE;
    }

    public void writeObject(ObjectDataOutput out, Object object) {
        Log.TRACE();
        Output output = new Output((OutputStream) out);
        kryo.writeObject(output, object);
        output.flush();
    }

    public Object readObject(ObjectDataInput in, Class clazz) {
        Log.TRACE();
        Input input = new Input((InputStream) in);
        return kryo.readObject(input, clazz);
    }
}*/
