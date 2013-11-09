package nl.first8.pigsinspace.control;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import nl.first8.pigsinspace.ShipControl;
import nl.first8.pigsinspace.Configuration;
import nl.first8.pigsinspace.ControlCommand;
import nl.first8.pigsinspace.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;

public class Control {
    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    private HazelcastInstance hzInstance;

    public Control() {
        if (Configuration.getInstance().isLiteMember()) {
            hzInstance = Hazelcast.newHazelcastInstance(Configuration.getInstance().getLiteMemberHazelcastConfig());
        } else {
            hzInstance = HazelcastClient.newHazelcastClient(Configuration.getInstance().getHazelcastClientConfig());
        }
    }

    public void execute() {
        try {
            for (;;) {
                printHelp();

                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                String line = "";

                while (!line.equalsIgnoreCase("quit")) {
                    line = in.readLine();
                    if (line.startsWith("+")) {
                        if (line.contains(",")) {
                            String[] l = line.substring(1).split(",");
                            ShipControl.addShips(hzInstance, Integer.valueOf(l[0]), Integer.valueOf(l[1]));
                        } else {
                            ShipControl.addShips(hzInstance, Integer.valueOf(line.substring(1)), null);
                        }
                    } else if (line.startsWith("help")) {
                        printHelp();
                    } else if (line.startsWith("sync ")) {
                        changeSync(line.equalsIgnoreCase("sync on"));
                    } else if (line.startsWith("remotetile ")) {
                        changeRemoteTileCollisionDetection(line.equalsIgnoreCase("remotetile on"));
                    } else if (line.startsWith("localtile ")) {
                        changeLocalTileCollisionDetection(line.equalsIgnoreCase("localtile on"));
                    } else if (line.startsWith("tilelock ")) {
                        changeTileLock(line.equalsIgnoreCase("tilelock on"));
                    } else if (line.startsWith("remotetilelocalcheck ")) {
                        changeRemoteTileLocalCheck(line.equalsIgnoreCase("remotetilelocalcheck on"));
                    } else if (line.startsWith("groupremotetiles ")) {
                        changeGroupRemoteTiles(line.equalsIgnoreCase("groupremotetiles on"));
                    } else if (line.startsWith("remotetilelistener ")) {
                        changeRemoteTileListener(line.equalsIgnoreCase("remotetilelistener on"));
                    } else if (line.startsWith("remotetileparallel ")) {
                        changeRemoteTileParallel(line.equalsIgnoreCase("remotetileparallel on"));
                    } else {
                        System.err.println("Unrecognized command: " + line );
                        printHelp();
                    }
                }

                in.close();
                quitAll();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to parse controls: " + e.getMessage(), e);
            System.exit(-1);
        }
    }


    private void printHelp() {
        System.out.println("Controller for PigsInSpace.");
        System.out.println("");
        System.out.println("The following commands are available:");
        System.out.println("");
        System.out.println("help                    : show this help information");
        System.out.println("+N                      : add more ships");
        System.out.println("+N,tileId               : add more ships on tile ");
        System.out.println("sync on                 : enable sync loop between servers");
        System.out.println("sync off                : disable sync loop between servers");
        System.out.println("remotetile on           : enable collision detection on remote tiles");
        System.out.println("remotetile off          : disable collision detection on remote tiles");
        System.out.println("remotetilelocalcheck on : remote tiles are copied local and checked local");
        System.out.println("remotetilelocalcheck off: remote tiles are remotely checked");
        System.out.println("localtile on            : enable collision detection on local tiles");
        System.out.println("localtile off           : disable collision detection on local tiles");
        System.out.println("tilelock on             : enable locking of tiles when server updates it");
        System.out.println("tilelock off            : disable locking of tiles when server updates it");
        System.out.println("groupremotetiles on     : enable grouping of entities per remote tile");
        System.out.println("groupremotetiles off    : disable grouping of entities per remote tile");
        System.out.println("remotetilelistener on   : listens to tile changes instead of pulling the tile");
        System.out.println("remotetilelistener off  : pull the tile instead of listening to changes");
        System.out.println("remotetileparallel on   : performs remote tile detection in parallel");
        System.out.println("remotetileparallel off  : performs remote tile detection sequentially");
        System.out.println("quit                    : quit controller");
    }

    private void changeRemoteTileLocalCheck(boolean localCheck) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (localCheck) {
            controlTopic.publish(ControlCommand.ENABLE_REMOTE_TILE_LOCAL_CHECK);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_REMOTE_TILE_LOCAL_CHECK);
        }
    }

    private void changeGroupRemoteTiles(boolean groupRemoteTiles) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (groupRemoteTiles) {
            controlTopic.publish(ControlCommand.ENABLE_GROUP_REMOTE_TILES);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_GROUP_REMOTE_TILES);
        }
    }

    private void changeRemoteTileListener(boolean remoteTileListener) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (remoteTileListener) {
            controlTopic.publish(ControlCommand.ENABLE_REMOTE_TILE_LISTENER);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_REMOTE_TILE_LISTENER);
        }
    }

    private void changeRemoteTileParallel(boolean parallel) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (parallel) {
            controlTopic.publish(ControlCommand.ENABLE_REMOTE_TILE_PARALLEL);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_REMOTE_TILE_PARALLEL);
        }
    }

    private void changeSync(boolean sync) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (sync) {
            controlTopic.publish(ControlCommand.ENABLE_SYNC);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_SYNC);
        }
    }

    private void changeRemoteTileCollisionDetection(boolean remote) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (remote) {
            controlTopic.publish(ControlCommand.ENABLE_REMOTE_TILE_COLLISION_DETECTION);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_REMOTE_TILE_COLLISION_DETECTION);
        }
    }

    private void changeLocalTileCollisionDetection(boolean remote) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (remote) {
            controlTopic.publish(ControlCommand.ENABLE_LOCAL_TILE_COLLISION_DETECTION);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_LOCAL_TILE_COLLISION_DETECTION);
        }
    }

    private void changeTileLock(boolean lock) {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        if (lock) {
            controlTopic.publish(ControlCommand.ENABLE_TILE_LOCK);
        } else {
            controlTopic.publish(ControlCommand.DISABLE_TILE_LOCK);
        }
    }

    private void quitAll() {
        ITopic<ControlCommand> controlTopic = hzInstance.getTopic(Configuration.CONTROL_TOPIC);
        controlTopic.publish(ControlCommand.QUIT);
    }
    

}
