package network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Random;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    public static final int MSS = 1000;

    /**
     * Timeout in ms
     */
    public static final int TIMEOUT = 10000;

    /** The window size in bytes */
    private final int windowSize;

    /**
     * The current sequence number.
     * The next packet sent will be sent with this value.
     */
    private int seqNum;

    private boolean isConnected;

    /**
     * Construct a ReldatSocket bound on a specific port.
     *
     * @param port the port to bind on
     * @param windowSize the receiving window size
     * @throws IOException
     */
    public ReldatSocket(int port, int windowSize) throws IOException {
        super(port);
        this.windowSize = windowSize;

        // Initialize random positive seq num
        this.seqNum = new Random().nextInt() & Integer.MAX_VALUE;
    }

    /**
     * Construct a ReldatSocket bound on a random port.
     *
     * @param windowSize the receiving window size
     * @throws IOException
     */
    public ReldatSocket(int windowSize) throws IOException {
        this(0, windowSize);
    }

    /**
     * Blocks until a new connection is accepted
     *
     * @return a new socket for the newly accepted connection
     */
    public ReldatSocket accept() throws IOException {
        // Receive a SYN packet
        // TODO: verify packet integrity
        ReldatPacket syn = receive();
        while (!syn.getSYN()) {
            syn = receive();
        }
        // Create new socket for the new connection
        ReldatSocket conn = new ReldatSocket(windowSize);

        // Send SYNACK
        ReldatPacket synack = new ReldatPacket(windowSize, conn.seqNum);
        synack.setSYN();
        synack.setACK(syn.getAckNum() + 1);
        conn.send(synack, syn.getSocketAddress());

        // TODO: wait for ACK

        conn.isConnected = true;
        return conn;
    }

    /**
     * Connects to destination address:port
     *
     * This operation blocks until the connection is established.
     *
     * @param address the address to connect to
     */
    public void connect(SocketAddress address) throws ConnectException {
        ReldatPacket syn = new ReldatPacket(windowSize, seqNum);
        syn.setSYN();

        try {
            send(syn, address);

            ReldatPacket synack = receive();
            // TODO: verify packet integrity
            while (!synack.getSYN() || !synack.getACK()) {
                synack = receive();
            }

            System.out.println(synack.getSocketAddress());
            // TODO: send ACK

            // TODO: set isConnected to true
        } catch (IOException e) {
            throw new ConnectException(e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
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
            return ReldatPacket.fromUDP(udpPacket);
        } catch (IOException e) {
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
