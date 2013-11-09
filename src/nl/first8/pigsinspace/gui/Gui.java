package nl.first8.pigsinspace.gui;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW;
import static org.lwjgl.opengl.GL11.GL_PROJECTION;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMatrixMode;
import static org.lwjgl.opengl.GL11.glOrtho;
import static org.lwjgl.opengl.GL11.glViewport;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.Main;
import nl.first8.pigsinspace.Tile;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.partition.Partition;
import com.hazelcast.partition.PartitionService;

public class Gui implements EntryListener<Integer, Tile> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Gui.class);
    private static long timerTicksPerSecond = Sys.getTimerResolution();
    private static final String WINDOW_TITLE = "Pigs in Space - First8";
    private static final float ZOOM_SPEED = 1.02f;
    private static final int SCROLL_SPEED = 10;
    private static final long KEY_DELAY = 500;
    
    private int width = 800;
    private int height = 600;
    private final boolean fullScreen;
    
    
    private static class Statistics {
        long lastLoopTime = getTime();
        long lastFpsTime;
        int fps;
        long delta;
        int nrOfTiles;
        int nrOfEntities;
        
        public void reset() {
            delta = getTime() - lastLoopTime;
            lastLoopTime = getTime();
            lastFpsTime += delta;
            fps++;
        }
        
        public void resetFps() {
            lastFpsTime = 0;
            fps = 0;
        }
    }
    
    private Statistics statistics = new Statistics();
    
    private volatile float zoom = 4.0f;
    private volatile int x = 0;
    private volatile int y = 0;
    private volatile DrawMode drawMode = DrawMode.NONE;

    private TileDisplayer tileDisplayer;

    private ConcurrentMap<Integer,Tile> localTiles = new ConcurrentHashMap<Integer,Tile>(); 
    
    private HazelcastInstance hzInstance;
    private long timeLastKeypress;
    

    public Gui(final boolean fullScreen) {
        try {
            this.fullScreen = fullScreen;
            initializeGraphics();
            initializeSprites();
            initializeNetwork();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeSprites() {
        tileDisplayer = new TileDisplayer();        
    }

    private void initializeNetwork() {
        if (Configuration.getInstance().isLiteMember()) {
            hzInstance = Hazelcast.newHazelcastInstance(Configuration.getInstance().getLiteMemberHazelcastConfig());
        } else {
            hzInstance = HazelcastClient.newHazelcastClient(Configuration.getInstance().getHazelcastClientConfig());
        }
        IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);
        localTiles.putAll(tiles);
        tiles.addEntryListener(this, true);
    }

    public void initializeGraphics() throws LWJGLException {
        // initialize the window beforehand
        setDisplayMode();
        Display.setTitle(WINDOW_TITLE);
        Display.setFullscreen(fullScreen);
        Display.create();

        // grab the mouse, dont want that hideous cursor when we're playing!
//        Mouse.setGrabbed(true);

        // enable textures since we're going to use these for our sprites
        glEnable(GL_TEXTURE_2D);
        glDisable(GL_DEPTH_TEST);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, Configuration.getInstance().getMapWidth(), Configuration.getInstance().getMapHeight(), 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
        glViewport(x, y, (int) (width*zoom +x), (int) (height*zoom + y));
    }

    /**
     * Run the main game loop. This method keeps rendering the scene and
     * requesting that the callback update its screen.
     */
    public void execute() {
        while (Main.isGameRunning()) {
            
            try {
                // clear screen
                glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                glMatrixMode(GL_MODELVIEW);
                glLoadIdentity();
                glViewport(x, y, (int) (width*zoom +x), (int) (height*zoom + y));
                frameRendering();
                Display.update();
            } catch(Exception e) {
                LOGGER.error("Unhandled exception in main loop: " + e.getMessage(), e);
            }
        }
        Display.destroy();
        hzInstance.getLifecycleService().shutdown();
    }

    /**
     * Notification that a frame is being rendered. Responsible for running game
     * logic and rendering the scene.
     */
    public void frameRendering() {
        Display.sync(60);

        // work out how long its been since the last update, this
        // will be used to calculate how far the entities should
        // move this loop
        
        statistics.reset();
        

        // update our FPS counter if a second has passed
        if (statistics.lastFpsTime >= 1000) {
            Display.setTitle(WINDOW_TITLE + " (FPS: " + statistics.fps + ")");
            LOGGER.debug("Nr of tiles drawn: " + statistics.nrOfTiles + ", nr of entities drawn: " + statistics.nrOfEntities + ", fps: " + statistics.fps + "\nx=" + x + ", y=" + y + ", zoom=" + zoom);
            statistics.resetFps();
        }

        drawTiles();
        handleKeys();
    }

    private void drawTiles() {
        PartitionService partitionService = hzInstance.getPartitionService();
        // cycle round drawing all the entities we have in the game
        
        // TODO cache serverId's and/or only lookup when necessary for rendering
        statistics.nrOfTiles = localTiles.size();
        statistics.nrOfEntities = 0;
        // render tiles
        for (Tile tile : localTiles.values()) {
            statistics.nrOfEntities  += tile.getEntities().size();
            
            Partition partition = partitionService.getPartition(tile.getId());
            Member ownerMember = partition.getOwner();
            String serverId = (Configuration.getInstance().isLocalhost()?ownerMember.getUuid():ownerMember.getInetSocketAddress().getAddress().getHostAddress());
            tileDisplayer.drawTile(tile, drawMode, serverId);
        }
        // render entities
        for (Tile tile : localTiles.values()) {
            Partition partition = partitionService.getPartition(tile.getId());
            Member ownerMember = partition.getOwner();
            String serverId = (Configuration.getInstance().isLocalhost()?ownerMember.getUuid():ownerMember.getInetSocketAddress().getAddress().getHostAddress());
            tileDisplayer.drawEntities(tile, drawMode, serverId);
        }
    }

    private void handleKeys() {
        
        if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            y-=SCROLL_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            y+=SCROLL_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT)) {
            x+=SCROLL_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT)) {
            x-=SCROLL_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_MINUS)) {
            zoom = zoom / ZOOM_SPEED;
//            x += SCROLL_SPEED;
//            y += SCROLL_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_EQUALS)) {
            zoom = zoom * ZOOM_SPEED;
//            x -= SCROLL_SPEED;
//            y -= SCROLL_SPEED;
        }
        
        
        
        if (x>0) {
            x = 0;
        }
        
        
        if (y>0) {
            y = 0;
        }
        if (zoom<1.0f) {
            zoom = 1.0f;
        }

        int minx = (int) (-(zoom-1)*(width/2));
        int miny = (int) (-(zoom-1)*(height/2));
        
        if (x<minx ) {
            x = minx;
        }
        
        if (y<miny) {
            y = miny;
        }
        
        
        // delayed keys here
        if (System.currentTimeMillis()-timeLastKeypress<KEY_DELAY) {
            return;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            drawMode = DrawMode.values()[ (drawMode.ordinal()+1) % DrawMode.values().length ];
            timeLastKeypress = System.currentTimeMillis();
        }

        // if escape has been pressed, stop the game
        if ((Display.isCloseRequested() || Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))) {
            Main.stopGame();
        }
    }

    private boolean setDisplayMode() {
        try {
            DisplayMode[] dm = org.lwjgl.util.Display.getAvailableDisplayModes(width, height, -1, -1, -1, -1, 60, 60);
            org.lwjgl.util.Display.setDisplayMode(dm, new String[] { "width=" + width, "height=" + height, "freq=" + 60, "bpp=" + org.lwjgl.opengl.Display.getDisplayMode().getBitsPerPixel() });
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to enter fullscreen, continuing in windowed mode: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the high resolution time in milliseconds
     * 
     * @return The high resolution time in milliseconds
     */
    public static long getTime() {
        // we get the "timer ticks" from the high resolution timer
        // multiply by 1000 so our end result is in milliseconds
        // then divide by the number of ticks in a second giving
        // us a nice clear time in milliseconds
        return (Sys.getTime() * 1000) / timerTicksPerSecond;
    }

    /**
     * Sleep for a fixed number of milliseconds.
     * 
     * @param duration
     *            The amount of time in milliseconds to sleep for
     */
    public static void sleep(long duration) {
        try {
            Thread.sleep((duration * timerTicksPerSecond) / 1000);
        } catch (InterruptedException inte) {
        }
    }

    @Override
    public void entryAdded(EntryEvent<Integer, Tile> e) {
        localTiles.put( e.getKey(), e.getValue());
    }

    @Override
    public void entryEvicted(EntryEvent<Integer, Tile> e) {
        localTiles.remove(e.getKey());
    }

    @Override
    public void entryRemoved(EntryEvent<Integer, Tile> e) {
        localTiles.remove(e.getKey());
    }

    @Override
    public void entryUpdated(EntryEvent<Integer, Tile> e) {
        localTiles.put( e.getKey(), e.getValue());
    }
}
