# Java-NIO-Reactive-Echo-Server
An Echo server implemented using Java NIO which means that the echo server is implemented directly on the top of tcp layer.
The reactive bahaviour here means that the server behaves non-blocking and do not block CPU cycles.
This server support huge number of clients by assigning a thread to each event instead of assigning a thread to each client request.

This approach could be used to implement high performance application layer load balancers. Also Gateways in microservice world could benefit from this appraoch
