package network;

import java.io.IOException;
import java.net.*;
import java.util.Random;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    private static final int MSS = 1000;

    /**
     * Timeout in ms
     */
    private static final int TIMEOUT = 5000;

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
     * @throws IOException exception inherited from parent DatagramSocket
     */
    public ReldatSocket(int port, int windowSize) throws IOException {
        super(port);
        this.windowSize = windowSize;

        // Initialize random positive seq num
        this.seqNum = new Random().nextInt() & Integer.MAX_VALUE;

        // Set the timeout
        this.setSoTimeout(TIMEOUT);

        // Set receive buffer size
        this.setReceiveBufferSize(windowSize*MSS);
    }

    /**
     * Construct a ReldatSocket bound on a random port.
     *
     * @param windowSize the receiving window size
     * @throws IOException exception inherited from parent DatagramSocket
     */
    public ReldatSocket(int windowSize) throws IOException {
        this(0, windowSize);
    }

    /**
     * Blocks until a new connection is accepted
     *
     * @return a new socket for the newly accepted connection
     * @throws IOException error while receiving packet
     */
    public ReldatSocket accept() throws IOException {
        // Blocks until receiving a SYN packet
        ReldatPacket syn;
        do {
            System.out.println("Waiting for SYN");
            syn = receive();
        } while (syn == null || !syn.getSYN());

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
     * @throws ConnectException when connection fails
     */
    public void connect(SocketAddress address) throws ConnectException {
        ReldatPacket syn = new ReldatPacket(windowSize, seqNum);
        syn.setSYN();

        try {
            // Send SYN and wait for SYNACK
            ReldatPacket synack;
            do {
                System.out.println("SYN sent, waiting for synack...");
                send(syn, address);
                synack = receive();
            } while (synack == null || !synack.getSYN() || !synack.getACK());

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
     * This should block until a packet with valid checksum is received
     * or it times out. Receiving a packet that fails the checksum test resets
     * the timeout.
     *
     * @return the packet or null if an error occurs (timeout, other IOException, etc)
     */
    private ReldatPacket receive() {
        byte[] buffer = new byte[MSS];
        DatagramPacket udpPacket = new DatagramPacket(buffer, MSS);

        try {
            ReldatPacket packet;
            do {
                receive(udpPacket);
                packet = ReldatPacket.fromUDP(udpPacket);
            } while (packet == null || !packet.verifyChecksum());
            return packet;
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
     * @param packet the ReldatPacket to send
     * @param address the SocketAddress to send it to
     */
    private void send(ReldatPacket packet, SocketAddress address) throws IOException {
        byte[] data = packet.getBytes();
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, address);
        send(udpPacket);
    }
}
