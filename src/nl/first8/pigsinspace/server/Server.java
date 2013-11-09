package nl.first8.pigsinspace.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import nl.first8.pigsinspace.ShipControl;
import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.ControlCommand;
import nl.first8.pigsinspace.Entity;
import nl.first8.pigsinspace.Tile;
import nl.first8.pigsinspace.TileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.ICountDownLatch;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class Server implements MessageListener<ControlCommand> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private HazelcastInstance hzInstance;
    private boolean synchronizeLoop;
    private boolean remoteTileCollisionDetection;
    private boolean remoteTileListener;
    private boolean remoteTileInParallel;
    private boolean remoteTileLocalCheck;
    private boolean localTileCollisionDetection;
    private boolean tileLock;
    private boolean inOutTopic;
    private boolean groupRemoteTiles;
    
    private final Object configLock = new Object();
    private static final Statistics statistics = new Statistics();
    private MoveListener moveListener;
    private RemoteTileCache remoteTileCache;
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public Server() {
        hzInstance = Hazelcast.newHazelcastInstance(Configuration.getInstance().getHazelcastConfig());
        initialize();
    }

    private void initialize() {
        initializeConfig();
        initializeTiles();
        initializeControl();
        initializeListeners();
    }

    private void initializeConfig() {
        synchronizeLoop = Configuration.getInstance().isDefaultSynchronizeLoop();
        remoteTileCollisionDetection = Configuration.getInstance().isDefaultRemoteTileCollisionDetection();
        localTileCollisionDetection = Configuration.getInstance().isDefaultLocalTileCollisionDetection();
        tileLock = Configuration.getInstance().isDefaultTileLock();
        remoteTileLocalCheck = Configuration.getInstance().isDefaultRemoteTileLocalCheck();
        inOutTopic = Configuration.getInstance().isDefaultInOutTopic();
        groupRemoteTiles = Configuration.getInstance().isDefaultRemoteTileGroup();
        remoteTileListener = Configuration.getInstance().isDefaultRemoteTileListener();
        remoteTileInParallel = Configuration.getInstance().isDefaultRemoteTileInParallel();
    }

    private void initializeListeners() {
        if (inOutTopic) {
            moveListener = new MoveListener(hzInstance);
        }
        remoteTileCache = new RemoteTileCache(hzInstance);
    }

    private void initializeControl() {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        controlTopic.addMessageListener(this);
    }

    private void initializeTiles() {
        ILock initializationLock = hzInstance.getLock(Configuration.INITIALIZATION_LOCK);
        if (initializationLock.tryLock()) {
            try {
                IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);
                
                if (tiles == null || tiles.size() == 0) {
                    LOGGER.debug("Tiles not yet initialized, creating them");
                    for (int i = 0; i < Configuration.getInstance().getNrOfTiles(); i++) {
                        tiles.put(i, new Tile(i));
                    }
                    ShipControl.addShips(hzInstance, Configuration.getInstance().getNrOfEntities(), null);
                } else {
                    LOGGER.debug("Tiles already initialized");
                }
            } finally {
                initializationLock.unlock();
            }
        }
    }

    public void execute() {

        for (;;) {
            try {
                statistics.startTime();
                IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);
                Set<Integer> localTileIdSet = tiles.localKeySet();
                if (inOutTopic) {
                    moveListener.updateLocalTileIdSet(localTileIdSet);
                }
                
                handleLocalTiles(statistics.deltaInclSync, tiles, localTileIdSet);
    
                statistics.endTimeExclSync();
                waitTillAllDone();
                statistics.endTimeInclSync();
    
                statistics.print();
            } catch(Exception e ) {
                LOGGER.error("Exception in main loop, continuing: " + e.getMessage(), e);
            }
        }
    }

    private void waitTillAllDone() {
        if (isSynchronizeLoop()) {
            try {
                ILock lock = hzInstance.getLock(Configuration.START_COUNT_DOWN);
                try {
                    lock.lock();
                    ICountDownLatch latch = hzInstance.getCountDownLatch(Configuration.ITERATION_DONE_LATCH);
                    // Hazelcasr 3.0
//                    if (latch == null || latch.getCount()==0) {
//                        int size = getClusterSize();
//                        latch.trySetCount(size);
//                    }
                    if (latch == null || latch.hasCount()) {
                        int size = getClusterSize();
                        latch.setCount(size);
                    }
                } finally {
                    lock.unlock();
                }

                ICountDownLatch latch = hzInstance.getCountDownLatch(Configuration.ITERATION_DONE_LATCH);

                // LOGGER.debug("Instance " + hzInstance.getName() + " done.");
                // Thread.sleep(1000);
                latch.countDown();
                latch.await(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.error("Waited more than a second for iteration done, continuing...:" + e.getMessage());
            }
        }
    }

    private int getClusterSize() {
        return hzInstance.getCluster().getMembers().size();
    }

    private void inTileCollisions(Tile tile) {
        List<Entity> entities = tile.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            statistics.internalCollisionDetections++;
            for (int j = i + 1; j < entities.size(); j++) {
                Entity ei = entities.get(i);
                Entity ej = entities.get(j);
                if (ei.collidesWith(ej)) {
                    statistics.internalCollisions++;
                    ei.bounce();
                    ej.bounce();
                }
            }
        }
    }

    private void handleLocalTiles(long delta, IMap<Integer, Tile> tiles, Set<Integer> localTileIdSet) {
        // move all entities
        statistics.resetLoop(localTileIdSet.size());
        for (Integer tileId : localTileIdSet) {
            try {
                if (isTileLock()) {
                    tiles.lock(tileId);
                }
                
                Tile tile = tiles.get(tileId);
                try {
                    handleTile(delta, tile);
                } finally {
                    if (isTileLock()) {
                        tiles.unlock(tileId);
                    }
                }
                // don't lock here since that might cause deadlocks
                betweenTileCollisions(tile, localTileIdSet, tiles);
                tiles.put(tileId, tile);
                
                // no lock needed since we only read
                changeOfTile(tile, tiles);
            } catch( Exception e) {
                LOGGER.error("Unable to handle tile " + tileId + ", continuing with the rest: " + e.getMessage(), e);
            }

        }
    }

    private void handleTile(long delta, Tile tile) {
        if (inOutTopic) {
            handleMove(tile);
        } else {
            handleOutgoing(tile);
            handleIncoming(tile);
        }
        doSomething(delta, tile);
        inTileCollisions(tile);
    }

    private void handleMove(Tile tile) {
        removeAll( tile, moveListener.popOutgoing(tile.getId()));
        addAll( tile, moveListener.popIncoming(tile.getId()));
    }

    private void handleOutgoing(Tile tile) {
        IMap<Object, Object> outgoing = hzInstance.getMap(Configuration.OUTGOING_MAP);

        @SuppressWarnings("unchecked")
        Set<Entity> entities = (Set<Entity>) outgoing.remove(tile.getId());
        removeAll(tile, entities);
    }

    private void handleIncoming(Tile tile) {
        IMap<Object, Object> incoming = hzInstance.getMap(Configuration.INCOMING_MAP);
        @SuppressWarnings("unchecked")
        Set<Entity> entities = (Set<Entity>) incoming.remove(tile.getId());
        addAll(tile, entities);
    }

    private void addAll(Tile tile, Collection<Entity> entities) {
        if (entities != null) {
//            LOGGER.debug("Removing " + entities.size() + " entities from tile " + tile.getId());
            for (Entity entity : entities) {
                tile.addEntity(entity);
            }
        }
    }

    private void removeAll(Tile tile, Collection<Entity> entities) {
        if (entities != null) {
//            LOGGER.debug("Adding " + entities.size() + " entities to tile " + tile.getId());
            for (Entity entity : entities) {
                tile.getEntities().remove(entity);
            }
        }
    }

    private void betweenTileCollisions(final Tile tile, final Set<Integer> localTileIdSet, final IMap<Integer, Tile> tiles) {
        if (isRemoteTileListener()) {
            remoteTileCache.setNeighbouringTiles(localTileIdSet);
        }
        
        if (isGroupRemoteTiles()) {
            // group entities per external tile first
            final Map<Integer,Collection<Entity>> entitiesPerExternalTile =  groupTiles(tile);
            
            if (isRemoteTileInParallel()) {
                Collection<Callable<Void>> checks = new ArrayList<Callable<Void>>();
                for (final Integer externalTileId : entitiesPerExternalTile.keySet()) {
                    checks.add(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            checkExternalTile(localTileIdSet, tiles, entitiesPerExternalTile, externalTileId);
                            return null;
                        }});
                }
                try {
                    executor.invokeAll(checks);
                } catch (InterruptedException e) {
                    LOGGER.error("Executor interrupted while doing external tile checks");
                }
                
            } else {
                for (Integer externalTileId : entitiesPerExternalTile.keySet()) {
                    checkExternalTile(localTileIdSet, tiles, entitiesPerExternalTile, externalTileId);
                }
            }
        } else {
            for (Iterator<Entity> i = tile.getEntities().iterator(); i.hasNext();) {
                Entity entity = i.next();
                List<Integer> tilesOfEntity = TileUtil.getAllTiles(entity);
                // already checked this current tile, autoboxing @(*#^@(#
                tilesOfEntity.remove(Integer.valueOf(tile.getId()));
                for (Integer externalTileId : tilesOfEntity) {
                    if (externalTileId>=0 && externalTileId<Configuration.getInstance().getNrOfTiles()) {
                        Collection<Entity> entities = new ArrayList<Entity>(1);
                        entities.add(entity);
                        if (localTileIdSet.contains(externalTileId)) {
                            betweenTileCollisionLocalServer(entities, externalTileId, tiles);
                        } else {
                            betweenTileCollisionRemoveServer(entities, externalTileId, tiles);
                        }
                    }
                }
            }
            
            
            
        }
    }

    private void checkExternalTile(Set<Integer> localTileIdSet, IMap<Integer, Tile> tiles, Map<Integer, Collection<Entity>> entitiesPerExternalTile, Integer externalTileId) {
        if (externalTileId>=0 && externalTileId<Configuration.getInstance().getNrOfTiles()) {
            Collection<Entity> entities = entitiesPerExternalTile.get(externalTileId);
            if (localTileIdSet.contains(externalTileId)) {
                betweenTileCollisionLocalServer(entities, externalTileId, tiles);
            } else {
                betweenTileCollisionRemoveServer(entities, externalTileId, tiles);
            }
        }
    }

    private  Map<Integer,Collection<Entity>> groupTiles(Tile tile) {
        Map<Integer,Collection<Entity>> entitiesPerExternalTile = new HashMap<Integer,Collection<Entity>>(8);
        for (Iterator<Entity> i = tile.getEntities().iterator(); i.hasNext();) {
            Entity entity = i.next();
            List<Integer> tilesOfEntity = TileUtil.getAllTiles(entity);
            // already checked this current tile, autoboxing @(*#^@(#
            tilesOfEntity.remove(Integer.valueOf(tile.getId()));
            add(entitiesPerExternalTile, entity, tilesOfEntity);
        }
        return entitiesPerExternalTile;
    }

    /**
     * Adds the entity to all external tiles it requires.
     * @param entitiesPerExternalTile the map containing external tile ids and entities to check
     * @param entity the entity to add
     * @param tilesOfEntity the external tile for that entity
     */
    private void add(Map<Integer, Collection<Entity>> entitiesPerExternalTile, Entity entity, Collection<Integer> tilesOfEntity) {
        for (Integer externalTileId : tilesOfEntity) {
            Collection<Entity> entities = entitiesPerExternalTile.get(externalTileId);
            if (entities ==null) {
                entities = new ArrayList<Entity>(100);
                entitiesPerExternalTile.put(externalTileId, entities);
            }
            entities.add(entity);
        }
    }

    private void betweenTileCollisionLocalServer(Collection<Entity> entities, Integer externalTileId, IMap<Integer, Tile> tiles) {
        if (isLocalTileCollisionDetection()) {
            statistics.localServerCollisionDetections++;
            Tile tile = tiles.get(externalTileId);
            for (Entity entity : entities) {
                if (TileUtil.collides(tile, entity)) {
                    statistics.localServerCollisions++;
                    entity.bounce();
                }
            }
        }
    }

    private void betweenTileCollisionRemoveServer(Collection<Entity> entities, Integer externalTileId, IMap<Integer, Tile> tiles) {
        if (isRemoteTileCollisionDetection()) {
            if (isRemoteTileLocalCheck()) {
                remoteTileLocalCheck(entities, externalTileId, tiles);
            } else {
                remoteTileRemoteCheck(entities, externalTileId);
            }
        }
    }

    // Hazelcast 3.0
//    private void remoteTileRemoteCheck(Collection<Entity> entities, Integer externalTileId) {
//        IExecutorService executorService = hzInstance.getExecutorService(Configuration.EXECUTOR_SERVICE);
//        Future<Collection<Entity>> futureResult = executorService.submitToKeyOwner(new CollisionDetect(entities, externalTileId),externalTileId);
//        try {
//            Collection<Entity> result = futureResult.get();
//            for (Entity entity : entities) {
//                if (result.contains(entity)) {
//                    statistics.remoteServerCollisions++;
//                    entity.bounce();
//                }
//            }
//        } catch (Exception e) {
//            LOGGER.error("Unable to collision detect remotely, so assuming a collision: " + e.getMessage(), e);
//            for (Entity entity : entities) {
//                entity.bounce();
//            }
//        }
//    }
    
    private void remoteTileRemoteCheck(Collection<Entity> entities, Integer externalTileId) {
        FutureTask<Collection<Entity>> task = new DistributedTask<Collection<Entity>>(new CollisionDetect(entities, externalTileId), externalTileId);
        ExecutorService executorService = hzInstance.getExecutorService();
        executorService.execute(task);
        try {
            Collection<Entity> result = task.get();
            for (Entity entity : entities) {
                if (result.contains(entity)) {
                    statistics.remoteServerCollisions++;
                    entity.bounce();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Unable to collision detect remotely, so assuming a collision: " + e.getMessage(), e);
            for (Entity entity : entities) {
                entity.bounce();
            }
        }
    }
    

    private void remoteTileLocalCheck(Collection<Entity> entities, Integer externalTileId, IMap<Integer, Tile> tiles) {
        statistics.remoteServerCollisionDetections++;
        Tile tile = (isRemoteTileListener()?remoteTileCache.get(externalTileId):tiles.get(externalTileId));
        if (tile==null) {
            if (isRemoteTileListener() ) {
                LOGGER.debug("Tile not yet cached, assuming no collision");
                remoteTileCache.fetch(externalTileId);
            } else {
                LOGGER.error("Tile is null while getting it from distributed map");
            }
        } else {
            for (Entity entity:entities) {
                if (TileUtil.collides(tile, entity)) {
                    statistics.remoteServerCollisionDetections++;
                    entity.bounce();
                }
            }
        }
    }

    private static class CollisionDetect implements Callable<Collection<Entity>>, HazelcastInstanceAware, Serializable {
        
        private static final long serialVersionUID = 1L;
        private final Collection<Entity> entities;
        private HazelcastInstance hzInstance;
        private final int tileId;

        private CollisionDetect(final Collection<Entity> entities, final int tileId) {
            this.entities = entities;
            this.tileId = tileId;
        }

        @Override
        public Collection<Entity> call() throws Exception {
            // LOGGER.debug("Checking collision on external tile " + tileId +
            // " from entity on tile " + TileUtil.getTileId(entity));
            IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);
            
            tiles.lock(tileId);
            try {
                Tile tile = tiles.get(tileId);
                // tile.getEntities().size() + " entities");
                Collection<Entity> result = new ArrayList<Entity>();
                for (Entity entity:entities) {
                    if ( TileUtil.collides(tile, entity)) {
                        result.add(entity);
                    }
                }
                return result;
            } finally {
                tiles.unlock(tileId);
            }
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hzInstance) {
            this.hzInstance = hzInstance;
        }

    }

    private void changeOfTile(Tile tile, IMap<Integer, Tile> tiles) {
        List<Entity> entities = tile.getEntities();
        // LOGGER.debug("Tile " + tile.getId() + " has " + entities.size());
        for (Iterator<Entity> i = entities.iterator(); i.hasNext();) {
            Entity entity = i.next();
            if (TileUtil.getTileId(entity) != tile.getId()) {
                statistics.tileChanges++;
                if (inOutTopic) {
                    MoveEvent moveEvent = new MoveEvent(entity, tile.getId(), TileUtil.getTileId(entity));
                    hzInstance.getTopic(Configuration.MOVE_TOPIC).publish(moveEvent);
                } else {
                    TileUtil.addToMap(tile.getId(), entity, hzInstance.getMap(Configuration.OUTGOING_MAP));
                    TileUtil.addToMap(TileUtil.getTileId(entity), entity, hzInstance.getMap(Configuration.INCOMING_MAP));
                }
            }
        }
    }


    private void doSomething(long delta, Tile tile) {
        for (Entity entity : tile.getEntities()) {
            entity.doSomething(delta);
            statistics.entities++;
        }
    }

    @Override
    public void onMessage(Message<ControlCommand> message) {
        LOGGER.debug("Control message received: " + message.getMessageObject());
        statistics.resetAll();
        synchronized(configLock) {
            switch(message.getMessageObject()) {
            case ENABLE_SYNC: synchronizeLoop = true; break;
            case DISABLE_SYNC: synchronizeLoop = false; break;
            case DISABLE_LOCAL_TILE_COLLISION_DETECTION: localTileCollisionDetection = false; break; 
            case ENABLE_LOCAL_TILE_COLLISION_DETECTION: localTileCollisionDetection = true; break; 
            case DISABLE_REMOTE_TILE_COLLISION_DETECTION: remoteTileCollisionDetection = false; break; 
            case ENABLE_REMOTE_TILE_COLLISION_DETECTION: remoteTileCollisionDetection = true; break;
            case DISABLE_TILE_LOCK: tileLock = false; break;
            case ENABLE_TILE_LOCK: tileLock = true; break;
            case DISABLE_REMOTE_TILE_LOCAL_CHECK: remoteTileLocalCheck = false; break;
            case ENABLE_REMOTE_TILE_LOCAL_CHECK: remoteTileLocalCheck = true; break;
            case ENABLE_GROUP_REMOTE_TILES: groupRemoteTiles = true; break;
            case DISABLE_GROUP_REMOTE_TILES: groupRemoteTiles = false; break;
            case ENABLE_REMOTE_TILE_LISTENER: remoteTileListener = true; break;
            case DISABLE_REMOTE_TILE_LISTENER: remoteTileListener = false; break;
            case ENABLE_REMOTE_TILE_PARALLEL: remoteTileInParallel = true; break;
            case DISABLE_REMOTE_TILE_PARALLEL: remoteTileInParallel = false; break;
            case QUIT: quit(); break;
            default: LOGGER.error("Unknown control message, ignoring it: " + message.getMessageObject());
            }
        }
    }
    
    private void quit() {
        hzInstance.getLifecycleService().shutdown();
        System.exit(1);
    }

    private boolean isSynchronizeLoop() {
        synchronized(configLock ) {
            return synchronizeLoop;
        }
    }
    private boolean isLocalTileCollisionDetection() {
        synchronized(configLock ) {
            return localTileCollisionDetection;
        }
    }
    private boolean isRemoteTileCollisionDetection() {
        synchronized(configLock ) {
            return remoteTileCollisionDetection;
        }
    }

    private boolean isTileLock() {
        synchronized(configLock ) {
            return tileLock;
        }
    }

    private boolean isRemoteTileLocalCheck() {
        synchronized(configLock ) {
            return remoteTileLocalCheck ;
        }
    }

    private boolean isRemoteTileListener() {
        synchronized(configLock ) {
            return remoteTileListener ;
        }
    }

    private boolean isGroupRemoteTiles() {
        synchronized(configLock ) {
            return groupRemoteTiles ;
        }
    }

    private boolean isRemoteTileInParallel() {
        synchronized(configLock ) {
            return remoteTileInParallel ;
        }
    }

}
