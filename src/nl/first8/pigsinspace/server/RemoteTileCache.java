package nl.first8.pigsinspace.server;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.Tile;
import nl.first8.pigsinspace.TileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.partition.MigrationEvent;
import com.hazelcast.partition.MigrationListener;

public class RemoteTileCache implements EntryListener<Integer, Tile>, MigrationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteTileCache.class);

    private ConcurrentMap<Integer,Tile> cachedTiles = new ConcurrentHashMap<Integer,Tile>();
    private HazelcastInstance hzInstance; 
    private Collection<Integer> neighbouringTiles = null;
    private boolean partitioningChanged = false;
    private Map<Integer,String> listenerIds = new HashMap<Integer,String>();

    public RemoteTileCache(HazelcastInstance hzInstance) {
        this.hzInstance = hzInstance;
        hzInstance.getPartitionService().addMigrationListener(this);
    }

    public void setNeighbouringTiles(Set<Integer> localTileIds) {
        if (neighbouringTiles==null || partitioningChanged ) {
            LOGGER.info("Partitioning changed, reregistering tile listener");
            partitioningChanged = false;
            
            if (neighbouringTiles==null) {
                neighbouringTiles = new HashSet<Integer>();
            }
            
            Collection<Integer> newNeighbouringTiles = findNeighbouringTiles(localTileIds);
            LOGGER.debug("Old neighbouring tiles: " );
            for (Integer t : neighbouringTiles) {
                LOGGER.debug(" * " + t);
            }
            
            LOGGER.debug("New neighbouring tiles: " );
            for (Integer t : newNeighbouringTiles) {
                LOGGER.debug(" * " + t);
            }

            IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);
            // remove non-neighbouring tiles
            for (Integer tileId : neighbouringTiles) {
                if (!newNeighbouringTiles.contains(tileId)) {
                    LOGGER.debug("No longer listening to tile " + tileId);
                    // Hazelcast 3.0
//                    tiles.removeEntryListener(listenerIds.get(tileId));
                    tiles.removeEntryListener(this);
                } else {
                    LOGGER.debug("Still not listening to tile " + tileId);
                }
            }
            // add new neighbouring tiles
            for (Integer tileId : newNeighbouringTiles) {
                if (!neighbouringTiles.contains(tileId)) {
                    LOGGER.debug("Now listening to tile " + tileId);
                    
                    // Hazelcast 3.0
//                    String listenerId = 
                    tiles.addEntryListener(this, tileId, true);
//                    listenerIds.put(tileId, listenerId);
                } else {
                    LOGGER.debug("Continue listening to tile " + tileId);
                }
            }
            neighbouringTiles = newNeighbouringTiles;
        }
    }

    private Collection<Integer> findNeighbouringTiles(Collection<Integer> localTileIds) {
        HashSet<Integer> neighbouringTiles = new HashSet<Integer>();
        
        for (Integer tileId:localTileIds) {
            neighbouringTiles.addAll(TileUtil.getNeighbouringTiles(tileId));
        }
        
        neighbouringTiles.removeAll(localTileIds);
        return neighbouringTiles;
    }

    public Tile get(Integer externalTileId) {
        return cachedTiles.get(externalTileId);
    }

    @Override
    public void entryAdded(EntryEvent<Integer, Tile> e) {
        cachedTiles.put( e.getKey(), e.getValue());
    }

    @Override
    public void entryEvicted(EntryEvent<Integer, Tile> e) {
        cachedTiles.remove(e.getKey());
    }

    @Override
    public void entryRemoved(EntryEvent<Integer, Tile> e) {
        cachedTiles.remove(e.getKey());
    }

    @Override
    public void entryUpdated(EntryEvent<Integer, Tile> e) {
        cachedTiles.put( e.getKey(), e.getValue());  
    }

    @Override
    public void migrationCompleted(MigrationEvent arg0) {
        partitioningChanged = true;
    }

    @Override
    public void migrationFailed(MigrationEvent arg0) {
        partitioningChanged = true;
    }

    @Override
    public void migrationStarted(MigrationEvent arg0) {
        
    }

    public void fetch(Integer externalTileId) {
        if (neighbouringTiles==null) {
            partitioningChanged = true;
        }
        if (!neighbouringTiles.contains(externalTileId)) {
            LOGGER.error("Neighbouring tiles is missing " + externalTileId );
            neighbouringTiles.add(externalTileId);
            partitioningChanged = true;
        }
    }

}
