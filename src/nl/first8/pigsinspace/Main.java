package nl.first8.pigsinspace;

import nl.first8.pigsinspace.control.Control;
import nl.first8.pigsinspace.gui.Gui;
import nl.first8.pigsinspace.server.Server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static boolean gameRunning = true;

    private enum RunType {
        GUI, SERVER, CONTROL
    }

    private static RunType runType;

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            printUsage();
            System.exit(-1);
        }

        runType = RunType.valueOf(args[0].toUpperCase());
        boolean fullScreen = args.length > 2 && args[2].equalsIgnoreCase("-fullscreen");
        boolean localhost = args[1].equalsIgnoreCase("-localhost");
        
        Configuration.init(localhost);
        
        switch (runType) {
        case GUI:
            LOGGER.info("Starting GUI with fullscreen: " + fullScreen);
            new Gui(fullScreen).execute();
            break;
        case SERVER:
            LOGGER.info("Starting Server ");
            new Server().execute();
            break;
        case CONTROL:
            LOGGER.info("Starting Control ");
            new Control().execute();
            break;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -Djava.library.path=.../lib/native/macosx nl.first8.pigsinspace.gui.Main type {-localhost/-multicast} [-fullscreen]");
        System.out.println("  type        : either 'gui', 'server' or 'control'");
        System.out.println("  -localhost  : join the cluster on localhost only");
        System.out.println("  -multicast  : join the cluster using multicast");
        System.out.println("  -fullscreen : if type==gui, indicates that it should run full screen");
    }

    public static synchronized boolean isGameRunning() {
        return gameRunning;
    }

    public static synchronized void stopGame() {
        gameRunning = false;
    }

}
