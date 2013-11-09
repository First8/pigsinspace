package nl.first8.pigsinspace;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.DistributedTask;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;

public class ShipControl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShipControl.class);
    private static final Random RANDOM = new Random();
    
    public static void addShips(HazelcastInstance hzInstance, int nr, Integer givenTileId) {
        Map<Integer, Integer> countsPerTile = new HashMap<Integer, Integer>();
        for (int i = 0; i < nr; i++) {
            
            int tileId = (givenTileId==null?RANDOM.nextInt(Configuration.getInstance().getNrOfTiles()):givenTileId);
            Integer count = countsPerTile.get(tileId);
            if (count == null) {
                count = 0;
            }
            count++;
            countsPerTile.put(tileId, count);
        }
        System.out.println("Adding " + nr + " ships to " + countsPerTile.size() + " tiles");

        // Hazelcast 3.0
//        IExecutorService executorService = hzInstance.getExecutorService(Configuration.EXECUTOR_SERVICE);
//        for (Entry<Integer, Integer> entry : countsPerTile.entrySet()) {
//            System.out.println("  Adding " + entry.getValue() + " ships to tile " + entry.getKey());
//            Future<Integer> result = executorService.submitToKeyOwner(new AddShips(entry.getKey(), entry.getValue()), entry.getKey());
//            try {
//                System.out.println(" Done, added " + result.get() + " ships");
//            } catch (InterruptedException | ExecutionException e) {
//                LOGGER.error("unable to add all ships: " + e.getMessage(), e);
//            }
//        }

        ExecutorService executorService = hzInstance.getExecutorService();
        for (Entry<Integer, Integer> entry : countsPerTile.entrySet()) {
            System.out.println("  Adding " + entry.getValue() + " ships to tile " + entry.getKey());
            FutureTask<Integer> task = new DistributedTask<Integer>(new AddShips(entry.getKey(), entry.getValue()), entry.getKey());
            executorService.execute(task);
            try {
                System.out.println(" Done, added " + task.get() + " ships");
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("unable to add all ships: " + e.getMessage(), e);
            }
        }

        
        
    }

    private static class AddShips implements Callable<Integer>, HazelcastInstanceAware, Serializable {
        private static final long serialVersionUID = 1L;
        private Integer tileId;
        private Integer nr;
        private HazelcastInstance hzInstance;

        public AddShips(Integer tileId, Integer nr) {
            this.tileId = tileId;
            this.nr = nr;
        }

        @Override
        public Integer call() throws Exception {
            IMap<Integer, Tile> tiles = hzInstance.getMap(Configuration.TILES_MAP);

            System.out.println("Adding " + nr + " ships to tile " + tileId);

            tiles.lock(tileId);
            int count = 0;
            try {
                Tile tile = tiles.get(tileId);
                while (count < nr) {
                    Entity e = new Entity(tile);
                    if (!TileUtil.collides(tile, e)) {
                        System.out.println(" added " + e.getId());
                        tile.addEntity(e);
                        count++;
                    }
                }
                tiles.put(tileId, tile);
            } finally {
                tiles.unlock(tileId);
            }

            return count;
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance hzInstance) {
            this.hzInstance = hzInstance;
        }

    }

}
