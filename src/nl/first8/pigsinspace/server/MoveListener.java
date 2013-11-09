package nl.first8.pigsinspace.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.Entity;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class MoveListener implements MessageListener<MoveEvent> {
    private Set<Integer> localTileIdSet;
    
    private Map<Integer, Set<Entity>> incoming = new HashMap<Integer,Set<Entity>>();
    private Map<Integer, Set<Entity>> outgoing = new HashMap<Integer,Set<Entity>>();
    

    public MoveListener(HazelcastInstance hzInstance) {
        ITopic<MoveEvent> topic = hzInstance.getTopic(Configuration.MOVE_TOPIC);
        topic.addMessageListener(this);
    }

    public synchronized void updateLocalTileIdSet(Set<Integer> localTileIdSet) {
        this.localTileIdSet = localTileIdSet;
        
    }

    public synchronized Set<Entity> popIncoming(int tileId) {
        return incoming.remove(tileId);
    }

    public synchronized Set<Entity> popOutgoing(int tileId) {
        return outgoing.remove(tileId);
    }

    @Override
    public synchronized void onMessage(Message<MoveEvent> event) {
        Entity entity = event.getMessageObject().getEntity();
        Integer newTile = event.getMessageObject().getNewTile();
        Integer oldTile = event.getMessageObject().getOriginalTile();
        
        // in a recovery scenario, this might be triggered before updateLocalTileIdSet(). Assume empty set then
        if (localTileIdSet==null) {
            localTileIdSet = new HashSet<Integer>();
        }
        if (localTileIdSet.contains(newTile)) {
            add(incoming, newTile, entity);
        }
        if (localTileIdSet.contains(oldTile)) {
            add(outgoing, oldTile, entity);
        }
    }

    private void add(Map<Integer, Set<Entity>> map, Integer tileId, Entity entity) {
        Set<Entity> set = map.get(tileId);
        if (set==null) {
            set = new HashSet<Entity>();
        }
        set.add(entity);
        map.put(tileId, set);
    }

}
