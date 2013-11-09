Hi all!

As promised at JFall 2013, I'v made the source of the PigsInSpace demo available at GitHub.

For those who were not at JFall (why not?!), I gave a presentation called 'Designing distributed programs on a Raspberry Pi cluster'. The demo app I used, called <em>Pigs in Space</em>, shows a huge map with spaceships flying around. The goal is to have as large a map and as many spaceships as possible, while doing collision detection. 

The code is available at https://github.com/First8/pigsinspace

To build it, you'll need to add some stuff to your build path in your favorite IDE:
* the _src_ folder
* the _resources_ folder 
* the hazelcast-2.6.3.jar, hazelcast-client-2.6.3.jar, lwjgl_util.jar, lwjgl.jar, slf4j-api-1.7.5.jar, slf4j-simple-1.7.5.jar jars found in _libs_

(It also contains some preparations for hazelcast 3.0 but that still had some rough edges.)

The application consists of three runnable parts:
* server
* gui
* control

The servers are the main thing: they do all the work and are meant to run at different, well, servers. 
The gui displays the map and the control allows you to sent instructions to all connected servers, e.g. changing strategies or adding more ships.
All applications can either run in multicast mode or on localhost. To run a server, you can e.g. do something like this:
```
java -jar PigsInSpace server -multicast
```

For the control, something similar:
```
java -jar PigsInSpace control -multicast
```

For the gui, you'll need to specify a platform library for LWJGL:
```
java -Djava.library.path=lib/native/macosx -jar PigsInSpace control -multicast
```

Have fun!
