package network;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

public class ReldatSocket extends DatagramSocket {
    /**
     * The maximum segment size in bytes
     */
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
     * @param port       the port to bind on
     * @param windowSize the receiving window size
     * @throws IOException exception inherited from parent DatagramSocket
     */
    public ReldatSocket(int port, int windowSize) throws IOException {
        super(port);
        this.windowSize = windowSize;

        // Initialize random positive seq num
        this.seqNum = new Random().nextInt() & Integer.MAX_VALUE;

        // Set receive buffer size
        this.setReceiveBufferSize(windowSize * MSS);
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
     * <p>
     * The connection is just a new ReldatSocket for the connection
     *
     * @return a new socket for the newly accepted connection
     * @throws IOException error while receiving packet
     */
    public ReldatSocket accept() {
        // Blocks until a connection is accepted
        while (true) {
            try {
                // Blocks until receiving a SYN packet
                ReldatPacket syn;
                do {
                    syn = receivePacket(0);
                } while (syn == null || !syn.getSYN());

                // Create new socket for the new connection
                ReldatSocket conn = new ReldatSocket(windowSize);
                conn.remoteSocketAddress = syn.getSocketAddress();

                // Create SYNACK packet
                ReldatPacket synack = new ReldatPacket(windowSize, conn.seqNum++);
                synack.setSYN();
                synack.setACK(syn.getSeqNum() + 1);

                // Send SYNACK and wait for ACK
                // TODO: consider what will happen if final ACK is dropped
                ReldatPacket ack;
                do {
                    conn.sendPacket(synack, syn.getSocketAddress());
                    ack = conn.receivePacket(2 * TIMEOUT);
                } while (ack == null || !ack.getACK() || ack.getAckNum() != conn.seqNum);

                conn.isConnected = true;
                return conn;
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * Connects to destination address:port
     * <p>
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
                synack = receivePacket(2 * TIMEOUT);
            } while (synack == null || !synack.getSYN() || !synack.getACK() ||
                    seqNum != synack.getAckNum());

            // Get the address of the newly opened socket on the server
            SocketAddress newAddress = synack.getSocketAddress();

            ReldatPacket ack = new ReldatPacket(windowSize, seqNum++);
            ack.setACK(synack.getSeqNum() + 1);
            sendPacket(ack, newAddress);

            isConnected = true;
            remoteSocketAddress = newAddress;
        } catch (IOException e) {
            throw new ConnectException("Failed to establish connection: " +
                    e.getMessage());
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
        boolean fin = false;
        int startIndex = 0; //Start of the segment of data to be sent
        int endIndex = (data.length <= windowSize) ? (data.length - 1) : (windowSize - 1);
        while (!fin) {
            byte[] currentSegment = Arrays.copyOfRange(data, startIndex, endIndex);
            ReldatPacket outgoing = new ReldatPacket(currentSegment, sendWindow, seqNum++);
            outgoing.setSYN();
            if (endIndex == data.length - 1) {
                outgoing.setFIN();
                outgoing.setACK(endIndex); //Setting these based off the README
            }
            try {
                sendPacket(outgoing, remoteSocketAddress);
            } catch (IOException e) {
                e.printStackTrace();
            }
            startIndex = endIndex + 1;
            endIndex = ((startIndex + windowSize) <= data.length - 1) ? (startIndex + windowSize) : (data.length - 1);
        }
    }

    /**
     * Receive bytes from the connection
     *
     * @param length the length of the data to receive
     * @return an array of bytes containing the received data
     */
    public byte[] receive(int length) {
        byte[] buffer = new byte[2048]; //not sure what default buffer should be
        boolean fin = false;
        ReldatPacket incoming = null;
        while (!fin) {
            try {
                incoming = receivePacket();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
            }
            if (incoming.getACK()) {
                try {
                    this.setSendBufferSize(incoming.getWindowSize());
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private ReldatPacket receivePacket() throws SocketTimeoutException {
        return receivePacket(TIMEOUT);
    }

    /**
     * Receive a ReldatPacket through the socket.
     * <p>
     * This should block until a packet with valid checksum is received
     * or it times out. Receiving any packet, even if it fails the checksum,
     * will reset the timeout.
     * <p>
     * This also has the side effect of setting the send window to
     * the received packet's advertised window size.
     *
     * @param timeout the timeout in milliseconds. A timeout of 0 is an infinite timeout
     * @return the packet
     * @throws SocketTimeoutException if the timeout is reached
     */
    private ReldatPacket receivePacket(int timeout) throws SocketTimeoutException {
        // Block until a valid packet is received or socket times out
        while (true) {
            try {
                // Set the timeout on the underlying UDP socket
                this.setSoTimeout(timeout);

                // Receive the packet
                ReldatPacket packet;
                do {
                    byte[] buffer = new byte[MSS];
                    DatagramPacket udpPacket = new DatagramPacket(buffer, MSS);

                    receive(udpPacket);
                    packet = ReldatPacket.fromUDP(udpPacket);
                } while (packet == null || !packet.verifyChecksum());

                // Set the sendWindow to the other side's advertised receive window
                this.sendWindow = packet.getWindowSize();

                return packet;
            } catch (SocketTimeoutException e) {
                throw e;
            } catch (IOException e) {
            }
        }
    }

    /**
     * Send a ReldatPacket to the desired SocketAddress.
     * <p>
     * This is an internal/unreliable operation and does
     * not check for ACKS to verify it is received.
     *
     * @param packet  the ReldatPacket to send
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
