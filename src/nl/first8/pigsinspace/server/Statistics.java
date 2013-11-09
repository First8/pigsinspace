package nl.first8.pigsinspace.server;

class Statistics {
    public int loop;
    public long total;
    public long deltaInclSync;
    public long deltaExclSync;
    public int tiles;
    public int entities;
    public int tileChanges;
    public int remoteServerCollisionDetections;
    public int remoteServerCollisions;
    public int internalCollisionDetections;
    public int internalCollisions;
    private long startTime;
    public int localServerCollisionDetections;
    public int localServerCollisions;
    
    public void resetAll() {
        loop = 0;
        total = 0;
    }

    public void resetLoop(int size) {
        tiles = size;
        entities = 0;
        tileChanges = 0;
        remoteServerCollisionDetections = 0;
        remoteServerCollisions = 0;
        localServerCollisionDetections = 0;
        localServerCollisions = 0;
        internalCollisionDetections = 0;
        internalCollisions = 0;
        loop++;
        total += deltaInclSync;
    }

    public void startTime() {
        startTime = System.currentTimeMillis();
    }

    public void endTimeExclSync() {
        deltaExclSync = System.currentTimeMillis() - startTime;
    }

    public void endTimeInclSync() {
        deltaInclSync = System.currentTimeMillis() - startTime;
    }

    public void print() {
        System.out.println("Loop " + loop + ": " + (deltaExclSync) + "ms (+" + ((deltaInclSync-deltaExclSync)) + "ms sync). " + //
                "Tiles: " + tiles + ", Entities: " + entities + ", tileChanges: " + tileChanges + //
                ", internalCollisions: " + internalCollisions + "/" + internalCollisionDetections + //
                ", localServerCollisions: " + localServerCollisions + "/" + localServerCollisionDetections + //
                ", remoteServerCollisions: " + remoteServerCollisions + "/" + remoteServerCollisionDetections + //
                ", total: " + total + ", avg loop: " + getAvgTime() 
                );
    }

    private float getAvgTime() {
        if ( loop!=0) {
            return (float) total / loop;
        } else {
            return 0;
        }
    }

}