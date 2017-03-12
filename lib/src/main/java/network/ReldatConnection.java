package network;

import java.net.SocketAddress;
import java.util.Random;

public class ReldatConnection {
    private final SocketAddress address;
    private final int windowSize;

    private int seqNum;

    private ConnectionState state;

    public ReldatConnection(SocketAddress address, int windowSize) {
        this.address = address;
        this.windowSize = windowSize;

        // Initialize random sequence number
        this.seqNum = new Random().nextInt(windowSize);

        this.state = ConnectionState.NEW;
    }

    /**
     * Send a SYN packet.
     */
    public void synchronize() {
        // TODO: implement this
    }
}
