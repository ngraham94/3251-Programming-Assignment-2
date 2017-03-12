package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    public static final int MSS = 1000;

    /**
     * Timeout in ms
     */
    public static final int TIMEOUT = 5000;

    /** The window size in bytes */
    private final int windowSize;

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
        this.connections = new ConcurrentHashMap<>();
    }

    /**
     * Construct a ReldatSocket without a listening port
     *
     * @param windowSize
     * @throws IOException
     */
    public ReldatSocket(int windowSize) throws IOException {
        this(0, windowSize);
    }

    /**
     * Set callback for when new connection is accepted.
     *
     * @param callback
     */
    public void acceptConnection(Consumer<ReldatConnection> callback) {
        // TODO: implement this
    }

    /**
     * Connects to destination address:port and sets callback for new connection.
     *
     * @param address
     * @param callback
     */
    public void connect(SocketAddress address, Consumer<ReldatConnection> callback) {
        ReldatConnection conn = new ReldatConnection(address, windowSize);
        this.connections.put(address, conn);

        // TODO: implement three way handshake
        conn.synchronize();

        callback.accept(conn);
    }

    /**
     * Receive a ReldatPacket through the socket.
     * <p>
     * This should block until a packet is received.
     * This is an internal/unreliable operation and does not check
     * the packet for correctness (checksum, seqnum, etc).
     *
     * @return null if an error occurs or get the packet.
     */
    private ReldatPacket receive() {
        byte[] buffer = new byte[MSS];
        DatagramPacket udpPacket = new DatagramPacket(buffer, MSS);

        try {
            receive(udpPacket);
            return ReldatPacket.fromBytes(udpPacket.getData());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Send a ReldatPacket to the desired SocketAddress.
     * <p>
     * This is an internal/unreliable operation and does
     * not check for ACKS to verify it is received.
     *
     * @param packet
     * @param address
     * @throws IOException
     */
    private void send(ReldatPacket packet, SocketAddress address) throws IOException {
        byte[] data = packet.getBytes();
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, address);
        send(udpPacket);
    }
}
