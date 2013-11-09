package nl.first8.pigsinspace;

import java.io.FileInputStream;
import java.util.Properties;

import com.hazelcast.client.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MemberGroupConfig;
import com.hazelcast.config.PartitionGroupConfig;
import com.hazelcast.config.PartitionGroupConfig.MemberGroupType;

/**
 * Global stuff for configuration.
 */
public class Configuration {
    public static final String TILES_MAP = "tiles";
    public static final String INITIALIZATION_LOCK = "initializationLock";
    public static final String ITERATION_DONE_LATCH = "iterationDone";
    public static final String START_COUNT_DOWN = "startCountDown";
    public static final String INCOMING_MAP = "incomingMap";
    public static final String OUTGOING_MAP = "outgoingMap";
    public static final String CONTROL_TOPIC = "controlTopic";
    public static final String MOVE_TOPIC = "moveTopic";
    public static final String EXECUTOR_SERVICE = "default";
    private static final boolean LITE_MEMBER_INSTEAD_OF_CLIENT = true;

    public static final int ENTITY_WIDTH = 50;
    public static final int ENTITY_HEIGHT = 50;

    private final boolean localhost;
    private final int nrOfHorizontalTiles;
    private final int nrOfVerticalTiles;
    private final int tileWidth;
    private final int tileHeight;
    private final int iterationDoneTimeout;
    private final int nrOfBackups;
    private final int nrOfAsyncBackups;
    private final int entitySpeed;
    private final int nrOfEntities;
    private final boolean readBackupData;
    private final boolean defaultSynchronizeLoop;
    private final boolean defaultLocalTileCollisionDetection;
    private final boolean defaultRemoteTileCollisionDetection;
    private final boolean defaultRemoteTileLocalCheck;
    private final boolean defaultRemoteTileListener;
    private final boolean defaultRemoteTileGroup;
    private final boolean defaultRemoteTileInParallel;
    private final boolean defaultTileLock;
    private final boolean defaultInOutTopic;


    private static Configuration instance = null;

    public static Configuration getInstance() {
        return instance;
    }

    public static void init(boolean localhost) {
        instance = new Configuration(localhost);
    }

    private Configuration(boolean localhost) {
        this.localhost = localhost;
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("pigsinspace.properties"));
            nrOfHorizontalTiles = Integer.valueOf(properties.getProperty("tiles.horizontal", "8"));
            nrOfVerticalTiles = Integer.valueOf(properties.getProperty("tiles.vertical", "8"));
            tileWidth = Integer.valueOf(properties.getProperty("tiles.width", "300"));
            tileHeight = Integer.valueOf(properties.getProperty("tiles.height", "300"));
            iterationDoneTimeout = Integer.valueOf(properties.getProperty("iteration.done.timeout", "1000"));
            nrOfBackups = Integer.valueOf(properties.getProperty("backups.sync", "0"));
            nrOfAsyncBackups = Integer.valueOf(properties.getProperty("backups.async", "1"));
            entitySpeed = Integer.valueOf(properties.getProperty("entity.speed", "5"));
            nrOfEntities = Integer.valueOf(properties.getProperty("entity.nr", "100"));
            readBackupData = Boolean.valueOf(properties.getProperty("backups.read.backup", "true"));

            defaultSynchronizeLoop = Boolean.valueOf(properties.getProperty("default.synchronize.loop", "true"));
            defaultLocalTileCollisionDetection = Boolean.valueOf(properties.getProperty("default.localtile.collisiondetection", "true"));
            defaultRemoteTileCollisionDetection = Boolean.valueOf(properties.getProperty("default.remotetile.collisiondetection", "true"));
            defaultRemoteTileLocalCheck = Boolean.valueOf(properties.getProperty("default.remotetile.localcheck", "true"));
            defaultRemoteTileListener = Boolean.valueOf(properties.getProperty("default.remotetile.listener", "true"));
            defaultRemoteTileGroup = Boolean.valueOf(properties.getProperty("default.remotetile.group", "true"));
            defaultRemoteTileInParallel = Boolean.valueOf(properties.getProperty("default.remotetile.parallel", "true"));
            defaultTileLock = Boolean.valueOf(properties.getProperty("default.tile.lock", "true"));
            defaultInOutTopic = Boolean.valueOf(properties.getProperty("default.inout.topic", "true"));
            
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Gets the hazelcast cluster config. For now, uses localhost since
     * multicast isn't enabled by default on a Mac.
     * 
     * @return the Hazelcast config to use
     */
    public Config getHazelcastConfig() {
        Config config = new Config();
        if (isLocalhost()) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
            config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
        } else {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
            PartitionGroupConfig partitionGroupConfig = config.getPartitionGroupConfig();
            partitionGroupConfig.setEnabled(true).setGroupType(MemberGroupType.CUSTOM);
            MemberGroupConfig groupLeft = new MemberGroupConfig();
            groupLeft.addInterface("10.200.1.122").addInterface("10.200.1.134").addInterface("10.200.1.139").addInterface("10.200.1.133");

            MemberGroupConfig groupRight = new MemberGroupConfig();
            groupRight.addInterface("10.200.1.149").addInterface("10.200.1.137").addInterface("10.200.1.138").addInterface("10.200.1.132");

            partitionGroupConfig.addMemberGroupConfig(groupLeft);
            partitionGroupConfig.addMemberGroupConfig(groupRight);
            config.setProperty("hazelcast.initial.min.cluster.size", "8");
            // config.setProperty("hazelcast.initial.wait.seconds", "30");
        }

        config.setProperty("hazelcast.max.no.heartbeat.seconds", "10");
        config.setProperty("hazelcast.serializer.gzip.enabled", "true");
        config.setProperty("hazelcast.map.partition.count", "8");

        // Hazelcast 3.0
        // configureSerializers(config.getSerializationConfig());

        MapConfig tilesMapConfig = config.getMapConfig(TILES_MAP);
        tilesMapConfig.setAsyncBackupCount(getNrOfAsyncBackups());
        tilesMapConfig.setBackupCount(getNrOfBackups());
        tilesMapConfig.setReadBackupData(isReadBackupData());

        return config;
    }

    /**
     * Gets the hazelcast cluster config for a lite member (non-data).
     * 
     * @return the Hazelcast config to use
     */
    public Config getLiteMemberHazelcastConfig() {
        Config config = getHazelcastConfig();
        config.setLiteMember(true);
        return config;
    }

    public ClientConfig getHazelcastClientConfig() {
        ClientConfig clientConfig = new ClientConfig();
        if (isLocalhost()) {
            clientConfig.addAddress("127.0.0.1");
        } else {
            clientConfig.addAddress("10.200.1.122");
            clientConfig.addAddress("10.200.1.134");
            clientConfig.addAddress("10.200.1.139");
            clientConfig.addAddress("10.200.1.133");
            clientConfig.addAddress("10.200.1.149");
            clientConfig.addAddress("10.200.1.137");
            clientConfig.addAddress("10.200.1.138");
            clientConfig.addAddress("10.200.1.132");
        }

        // Hazelcast 3.0
        // configureSerializers(clientConfig.getSerializationConfig());
        return clientConfig;
    }

    // Hazelcast 3.0
    // private void configureSerializers(SerializationConfig
    // serializationConfig) {
    // serializationConfig.addSerializerConfig(new
    // SerializerConfig().setTypeClass(Tile.class).setImplementation(new
    // TileSerializer()));
    // serializationConfig.addSerializerConfig(new
    // SerializerConfig().setTypeClass(Entity.class).setImplementation(new
    // EntitySerializer()));
    // serializationConfig.addSerializerConfig(new
    // SerializerConfig().setTypeClass(MoveEvent.class).setImplementation(new
    // MoveEventSerializer()));
    // }

    public boolean isLiteMember() {
        return LITE_MEMBER_INSTEAD_OF_CLIENT;
    }

    public boolean isLocalhost() {
        return localhost;
    }

    public int getNrOfHorizontalTiles() {
        return nrOfHorizontalTiles;
    }

    public int getNrOfVerticalTiles() {
        return nrOfVerticalTiles;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getIterationDoneTimeout() {
        return iterationDoneTimeout;
    }

    public int getNrOfBackups() {
        return nrOfBackups;
    }

    public int getNrOfAsyncBackups() {
        return nrOfAsyncBackups;
    }

    public boolean isReadBackupData() {
        return readBackupData;
    }

    public int getMapWidth() {
        return getTileWidth() * getNrOfHorizontalTiles();
    }

    public int getMapHeight() {
        return getTileHeight() * getNrOfVerticalTiles();
    }

    public int getEntitySpeed() {
        return entitySpeed;
    }

    public int getNrOfTiles() {
        return getNrOfHorizontalTiles() * getNrOfVerticalTiles();
    }

    public int getNrOfEntities() {
        return nrOfEntities;
    }
    public boolean isDefaultSynchronizeLoop() {
        return defaultSynchronizeLoop;
    }

    public boolean isDefaultLocalTileCollisionDetection() {
        return defaultLocalTileCollisionDetection;
    }

    public boolean isDefaultRemoteTileCollisionDetection() {
        return defaultRemoteTileCollisionDetection;
    }

    public boolean isDefaultRemoteTileLocalCheck() {
        return defaultRemoteTileLocalCheck;
    }

    public boolean isDefaultRemoteTileListener() {
        return defaultRemoteTileListener;
    }

    public boolean isDefaultRemoteTileGroup() {
        return defaultRemoteTileGroup;
    }

    public boolean isDefaultTileLock() {
        return defaultTileLock;
    }

    public boolean isDefaultInOutTopic() {
        return defaultInOutTopic;
    }

    public boolean isDefaultRemoteTileInParallel() {
        return defaultRemoteTileInParallel;
    }

}
