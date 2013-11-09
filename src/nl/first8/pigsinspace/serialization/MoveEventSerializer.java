package nl.first8.pigsinspace.serialization;


// Hazelcast 3.0
public class MoveEventSerializer {} /* implements StreamSerializer<MoveEvent> {
    private static final int TYPE_ID = 10;
    
    public int getTypeId() {
        return TYPE_ID;
    }

    public void write(ObjectDataOutput out, MoveEvent object) throws IOException {
        KryoWrapper.getInstance().writeObject(out, object);
    }

    public MoveEvent read(ObjectDataInput in) throws IOException {
        return (MoveEvent) KryoWrapper.getInstance().readObject(in, MoveEvent.class);
    }

    public void destroy() {
    }
}
*/