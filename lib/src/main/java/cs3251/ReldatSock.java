package cs3251;

import java.io.IOException;
import java.net.DatagramSocket;

public class ReldatSock {
    // Max segment size in bytes
    public static int MSS = 1000;

    private DatagramSocket sock;

    private int port, windowSize;

    /**
     * Constructor for a listening ReldatSock.
     *
     * @param port
     * @param windowSize
     * @throws IOException
     */
    public ReldatSock(int port, int windowSize) throws IOException {
        this.port = port;
        this.windowSize = windowSize;
        this.sock = new DatagramSocket(port);
    }

    /**
     * Construct a ReldatSock without a listening port
     *
     * @param windowSize
     * @throws IOException
     */
    public ReldatSock(int windowSize) throws IOException {
        this.windowSize = windowSize;
        this.sock = new DatagramSocket();
    }
}
