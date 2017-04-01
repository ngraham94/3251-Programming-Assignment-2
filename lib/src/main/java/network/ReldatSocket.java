package network;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

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
    private int sendWindowSize;

    /**
     * The current sequence number.
     * The next packet sent will be sent with this value.
     */
    private int seqNum;

    /**
     * The sequence number of the last received packet in order
     */
    private int lastReceived;

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

        // this.seqNum = new Random().nextInt() & Integer.MAX_VALUE;
        seqNum = 0;

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
                ReldatPacket synack = new ReldatPacket(windowSize, conn.seqNum);
                synack.setSYN();
                synack.setACK(calcAck(syn));
                conn.updateSeqNum(0);

                // Send SYNACK and wait for ACK
                // TODO: consider what will happen if final ACK is dropped
                ReldatPacket ack;
                do {
                    conn.sendPacket(synack, syn.getSocketAddress());
                    ack = conn.receivePacket(2 * TIMEOUT);
                } while (ack == null || !ack.getACK() || ack.getAckNum() != conn.seqNum);

                conn.lastReceived = ack.getSeqNum();
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
        ReldatPacket syn = new ReldatPacket(windowSize, seqNum);
        syn.setSYN();
        updateSeqNum(0);

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

            ReldatPacket ack = new ReldatPacket(windowSize, seqNum);
            ack.setACK(calcAck(synack));
            sendPacket(ack, newAddress);
            updateSeqNum(0);

            lastReceived = synack.getSeqNum();
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
     * @throws IOException if the any packet fails to send
     */
    public void send(byte[] data) throws IOException {
        ByteBuffer dataBuffer = ByteBuffer.wrap(data);
        Queue<ReldatPacket> sendWindow = new ArrayBlockingQueue<>(sendWindowSize);

        while (dataBuffer.hasRemaining() || !sendWindow.isEmpty()) {
            // Convert the next bytes to packets and populate the send window
            if (sendWindow.size() < sendWindowSize && dataBuffer.hasRemaining()) {
                int length = Math.min(MSS - ReldatPacket.getHeaderSize(), dataBuffer.remaining());
                byte[] bytes = new byte[length];
                dataBuffer.get(bytes);

                ReldatPacket packet = new ReldatPacket(bytes, windowSize, seqNum);
                updateSeqNum(packet.getSize());
                sendWindow.offer(packet);
            } else {
                // Send everything in send window
                for (int i = 0; i < sendWindow.size(); ++i) {
                    // Send the next packet in the window
                    // and requeue in case it needs to be resent
                    ReldatPacket packet = sendWindow.poll();
                    sendPacket(packet, remoteSocketAddress);
                    sendWindow.offer(packet);
                }

                // Attempt to receive sendWindow.size() ACKs over the timeout period
                for (int i = 0; i < sendWindow.size(); ++i) {
                    try {
                        // Get an ACK
                        ReldatPacket ack = receivePacket(Math.max(1, TIMEOUT / sendWindow.size()));
                        if (ack.getACK()) {
                            // Remove all acknowledged packets from the window
                            while (!sendWindow.isEmpty() &&
                                    calcAck(sendWindow.peek()) <= ack.getAckNum()) {
                                sendWindow.remove();
                            }
                            lastReceived = Math.max(lastReceived, ack.getSeqNum());
                        }
                    } catch (SocketTimeoutException e) {
                    }
                }
            }
        }
    }

    /**
     * Receive bytes from the connection
     *
     * @param length the length of the data to receive
     * @return an array of bytes containing the received data
     */
    public byte[] receive(int length) {
        // The total amount of bytes received so far
        int received = 0;

        // The buffer
        ByteBuffer buffer = ByteBuffer.allocate(length);

        // The receive window
        TreeSet<ReldatPacket> window = new TreeSet<>(Comparator.comparingInt(ReldatPacket::getSeqNum));

        while (received < length) {
            // Attempt to receive up to windowSize packets
            for (int i = 0; window.size() < windowSize && i < windowSize; ++i) {
                try {
                    ReldatPacket packet = receivePacket(Math.max(1, TIMEOUT / windowSize));
                    window.add(packet);
                } catch (IOException e) {
                }
            }

            // Process packets in the window
            for (Iterator<ReldatPacket> i = window.iterator(); i.hasNext(); ) {
                ReldatPacket packet = i.next();

                // Handle the next packet received in order
                if (packet.getSeqNum() <= lastReceived + MSS - ReldatPacket.getHeaderSize() + 1) {
                    // Ack the packet
                    ReldatPacket ack = new ReldatPacket(windowSize, seqNum);
                    updateSeqNum(0);
                    ack.setACK(calcAck(packet));
                    try {
                        sendPacket(ack, remoteSocketAddress);
                    } catch (IOException e) {
                        break;
                    }

                    // Update some metadata
                    lastReceived = packet.getSeqNum();
                    received += packet.getSize();

                    // Add data to buffer
                    buffer.put(packet.getData(), 0,
                            Math.min(packet.getSize(), buffer.remaining()));

                    // Remove from the receive window
                    i.remove();
                } else {
                    break;
                }
            }
        }

        return buffer.array();
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

                // Set the sendWindowSize to the other side's advertised receive window
                this.sendWindowSize = packet.getWindowSize();

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

    /**
     * Helper function to update the sequence number by the size of the packet.
     * <p>
     * If the value overflows an int, it wraps around from 0.
     *
     * @param size the size of the packet in bytes
     */
    private void updateSeqNum(int size) {
        seqNum = (seqNum + size + 1) & Integer.MAX_VALUE;
    }

    /**
     * Calculate the ACK number to use to acknowledge a packet
     *
     * @param packet the packet to acknowledge
     * @return the ACK number
     */
    private int calcAck(ReldatPacket packet) {
        return (packet.getSeqNum() + packet.getSize() + 1) & Integer.MAX_VALUE;
    }
}
