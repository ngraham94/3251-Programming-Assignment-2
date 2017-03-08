package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    public static int MSS = 1000;

    /** The window size in bytes */
    private int windowSize;

    /** Set of all connections */
    private Map<SocketAddress, ReldatConnection> connections;

    /**
     * Constructor for a listening ReldatSocket.
     *
     * @param port
     * @param windowSize
     * @throws IOException
     */
    public ReldatSocket(int port, int windowSize) throws IOException {
        super(port);
        this.windowSize = windowSize;
        this.connections = new HashMap<>();
    }

    /**
     * Construct a ReldatSocket without a listening port
     *
     * @param windowSize
     * @throws IOException
     */
    public ReldatSocket(int windowSize) throws IOException {
        super();
        this.windowSize = windowSize;
        this.connections = new HashMap<>();
    }

    /**
     * Set callback for when new connection is accepted.
     */
    public void acceptConnection(Consumer<ReldatConnection> callback) {
        // TODO: implement this
    }

    /**
     * Connects to destination address:port and sets callback for new connection.
     */
    public void connect(InetAddress address, int port, Consumer<ReldatConnection> callback) {
        // TODO: implement this
    }
}
