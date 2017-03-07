package network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.function.Consumer;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    public static int MSS = 1000;

    /** The window size in bytes */
    private int windowSize;

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
    }

    /**
     * Set callback for when new connection is opened.
     */
    public void acceptConnection(Consumer<ReldatConnection> consumer) {
        // TODO: implement this
    }

    /**
     * Connects and sets callback for new connection.
     */
    public void connect(Consumer<ReldatConnection> consumer) {
        // TODO: implement this
    }
}
