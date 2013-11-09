package nl.first8.pigsinspace.serialization;


// Hazelcast 3.0
public class EntitySerializer{} /* implements StreamSerializer<Entity> {
    private static final int TYPE_ID = 11;
    
    public int getTypeId() {
        return TYPE_ID;
    }

    public void write(ObjectDataOutput out, Entity object) throws IOException {
        KryoWrapper.getInstance().writeObject(out, object);
    }

    public Entity read(ObjectDataInput in) throws IOException {
        return (Entity) KryoWrapper.getInstance().readObject(in, Entity.class);
    }

    public void destroy() {
    }
}
*/