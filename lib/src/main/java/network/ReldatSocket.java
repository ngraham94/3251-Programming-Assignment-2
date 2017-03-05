package network;

import java.io.IOException;
import java.net.DatagramSocket;

public class ReldatSocket {
    // Max segment size in bytes
    public static int MSS = 1000;

    private DatagramSocket sock;

    private int port, windowSize;

    /**
     * Constructor for a listening ReldatSocket.
     *
     * @param port
     * @param windowSize
     * @throws IOException
     */
    public ReldatSocket(int port, int windowSize) throws IOException {
        this.port = port;
        this.windowSize = windowSize;
        this.sock = new DatagramSocket(port);
    }

    /**
     * Construct a ReldatSocket without a listening port
     *
     * @param windowSize
     * @throws IOException
     */
    public ReldatSocket(int windowSize) throws IOException {
        this.windowSize = windowSize;
        this.sock = new DatagramSocket();
    }
}
