package nl.first8.pigsinspace;

public enum Direction {
    N(0,-1), S(0,1), E(1,0), W(-1,0),
    NE(1,-1), NW(-1,-1),
    SE(1,1), SW(-1,1);
    
    private final int dx;
    private final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }
}
