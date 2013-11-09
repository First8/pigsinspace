package nl.first8.pigsinspace;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;

import com.hazelcast.nio.DataSerializable;

/**
 * A thingie that can be on the grid.
 */
public class Entity implements Serializable, DataSerializable {
    private static final long serialVersionUID = 1L;
    private static final Random RANDOM = new Random();
    private static final int CHANGE_DIRECTION = 100;
    private int id;
    private int x;
    private int y;
    private int previousX;
    private int previousY;
    private Direction direction;

    protected Entity(int x, int y) {
        this.x = x;
        this.y = y;
        this.id = RANDOM.nextInt();
        changeDirection();
    }
    
    public Entity() {
        this(RANDOM.nextInt(Configuration.getInstance().getMapWidth() - Configuration.ENTITY_WIDTH - 1), RANDOM.nextInt(Configuration.getInstance().getMapHeight() - Configuration.ENTITY_HEIGHT - 1));
    }

    public Entity(Tile tile) {
        this(TileUtil.getMapX(tile.getId()) + RANDOM.nextInt(Configuration.getInstance().getTileWidth() - Configuration.ENTITY_WIDTH), TileUtil.getMapY(tile.getId()) + RANDOM.nextInt(Configuration.getInstance().getTileHeight() - Configuration.ENTITY_HEIGHT));
    }
    
    protected void setX(int x) {
        this.x = x;
    }

    protected void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean collidesWith(Entity other) {
        if (equals(other)) {
            return false;
        }
       
        int tw = Configuration.ENTITY_WIDTH;
        int th = Configuration.ENTITY_HEIGHT;
        int rw = Configuration.ENTITY_WIDTH;
        int rh = Configuration.ENTITY_HEIGHT;
        int tx = this.x;
        int ty = this.y;
        int rx = other.x;
        int ry = other.y;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        //      overflow || intersect
        return ((rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Entity other = (Entity) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public void doSomething(float delta) {
        delta = 1.0f;
    
        if (shouldChangeDirection()) {
            changeDirection();
            // LOGGER.debug("Changing direction to " + direction);
        }
    
        previousX = getX();
        previousY = getY();
    
        int speed = Configuration.getInstance().getEntitySpeed();
        setX(getX() + direction.getDx() * speed);
        setY(getY() + direction.getDy() * speed);
    
        if (getX() <= speed) {
            direction = Direction.E;
        }
        if (getX() >= Configuration.getInstance().getMapWidth() - Configuration.ENTITY_WIDTH - speed) {
            direction = Direction.W;
        }
        if (getY() <= speed) {
            direction = Direction.S;
        }
        if (getY() >= Configuration.getInstance().getMapHeight() - Configuration.ENTITY_HEIGHT - speed) {
            direction = Direction.N;
        }
    
    }

    protected void changeDirection() {
        direction = Direction.values()[RANDOM.nextInt(Direction.values().length)];
    }

    private boolean shouldChangeDirection() {
        return RANDOM.nextInt(CHANGE_DIRECTION) == 0;
    }

    public Direction getDirection() {
        return direction;
    }

    public void bounce() {
        setX(previousX);
        setY(previousY);
        changeDirection();
    }
    
    public int getId() {
        return id;
    }

    public int getPreviousX() {
        return previousX;
    }

    public void setPreviousX(int previousX) {
        this.previousX = previousX;
    }

    public int getPreviousY() {
        return previousY;
    }

    public void setPreviousY(int previousY) {
        this.previousY = previousY;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    @Override
    public void readData(DataInput in) throws IOException {
        id = in.readInt();
        x = in.readInt();
        y = in.readInt();
        previousX = in.readInt();
        previousY = in.readInt();
        direction = Direction.values()[in.readInt()];
    }

    @Override
    public void writeData(DataOutput out) throws IOException {
        out.writeInt(id);
        out.writeInt(x);
        out.writeInt(y);
        out.writeInt(previousX);
        out.writeInt(previousY);
        out.writeInt(direction.ordinal());
    }

}
