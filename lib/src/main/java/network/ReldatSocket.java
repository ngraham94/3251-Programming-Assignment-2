package network;

import java.io.IOException;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.Random;

public class ReldatSocket extends DatagramSocket {
    /** The maximum segment size in bytes */
    private static final int MSS = 1000;

    /**
     * Timeout in ms
     */
    private static final int TIMEOUT = 1000;

    /**
     * The size of the receive window in bytes.
     */
    private final int windowSize;

    /**
     * The size of the send window in bytes.
     */
    private int sendWindow;

    /**
     * The current sequence number.
     * The next packet sent will be sent with this value.
     */
    private int seqNum;

    /**
     * The socket address this socket is connected to.
     */
    private SocketAddress remoteSocketAddress;
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
            syn = receivePacket();
        } while (syn == null || !syn.getSYN());

        // Create new socket for the new connection
        ReldatSocket conn = new ReldatSocket(windowSize);
        conn.remoteSocketAddress = syn.getSocketAddress();

        // Send SYNACK
        ReldatPacket synack = new ReldatPacket(windowSize, conn.seqNum);
        synack.setSYN();
        synack.setACK(syn.getSeqNum() + 1);
        conn.sendPacket(synack, syn.getSocketAddress());

        // Wait for ACK
        ReldatPacket ack;
        do {
            ack = conn.receivePacket();
        } while (ack == null || !ack.getACK());

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
        ReldatPacket syn = new ReldatPacket(windowSize, seqNum++);
        syn.setSYN();

        try {
            // Send SYN and wait for SYNACK
            ReldatPacket synack;
            do {
                sendPacket(syn, address);
                synack = receivePacket();
            } while (synack == null || !synack.getSYN() || !synack.getACK() ||
                    seqNum != synack.getAckNum());

            // Get the address of the newly opened socket on the server
            SocketAddress newAddress = synack.getSocketAddress();

            ReldatPacket ack = new ReldatPacket(windowSize, seqNum);
            ack.setACK(synack.getSeqNum() + 1);
            sendPacket(ack, newAddress);

            isConnected = true;
        } catch (IOException e) {
            throw new ConnectException(e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    /**
     * Send data through the established connection to the
     * remote SocketAddress
     *
     * @param data the data to send
     */
    public void send(byte[] data) {
        // TODO: Implement this
    }

    /**
     * Receive bytes from the connection
     *
     * @param length the length of the data to receive
     * @return an array of bytes containing the received data
     */
    public byte[] receive(int length) {
        // TODO: Implement this
        return null;
    }

    private ReldatPacket receivePacket() {
        return receivePacket(TIMEOUT);
    }

    /**
     * Receive a ReldatPacket through the socket.
     * <p>
     * This should block until a packet with valid checksum is received
     * or it times out. Receiving any packet, even if it fails the checksum,
     * will reset the timeout.
     *
     * @return the packet or null if an error occurs (timeout, other IOException, etc)
     */
    private ReldatPacket receivePacket(int timeout) {
        byte[] buffer = new byte[MSS];
        DatagramPacket udpPacket = new DatagramPacket(buffer, MSS);


        try {
            // Set the timeout on the underlying UDP socket
            this.setSoTimeout(timeout);

            // Receive the packet
            ReldatPacket packet;
            do {
                receive(udpPacket);
                packet = ReldatPacket.fromUDP(udpPacket);
            } while (packet == null || !packet.verifyChecksum());

            // Set the sendWindow to the other side's advertised receive window
            this.sendWindow = packet.getWindowSize();

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
     * @throws IOException when the packet fails to send or serialization
     *                     of the packet to bytes fails
     */
    private void sendPacket(ReldatPacket packet, SocketAddress address) throws IOException {
        byte[] data = packet.getBytes();
        DatagramPacket udpPacket = new DatagramPacket(data, data.length, address);
        send(udpPacket);
    }
}
