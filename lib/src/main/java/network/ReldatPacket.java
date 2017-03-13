package network;

import java.io.*;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.security.MessageDigest;
import java.util.Objects;

public class ReldatPacket implements Serializable {
    /** Packet headers */
    private boolean SYN, ACK, FIN;
    private int size, windowSize;
    private MessageDigest checksum;
    private int seqNum, ackNum;


    /**
     * The SocketAddress the packet was sent from.
     * This is not included in the header since it is a part of the UDP header.
     */
    private transient SocketAddress from;

    // Initialize to empty array to getHeaderSize
    // This has the unfortunate side effect of making the header bigger
    // for header-only packets, but whatever...
    private byte[] data = {};


    /**
     * Constructor for packet without data (header-only packet)
     */
    public ReldatPacket(int windowSize, int seqNum) {
        this.windowSize = windowSize;
        this.seqNum = seqNum;
    }

    public ReldatPacket(byte[] data, int windowSize, int seqNum) {
        this(windowSize, seqNum);
        this.data = data;
        this.size = data.length;
    }

    /**
     * Calculates the size of the header.
     * @return the size of the header in bytes or -1 if an error occurs
     */
    public static int getHeaderSize() {
        try {
            return new ReldatPacket(0, 0).getBytes().length;
        } catch (IOException e) {
            return -1;
        }
    }

    public MessageDigest calcChecksum() {
        // TODO
        return null;
    }

    public boolean verifyChecksum() {
        // TODO
        return false;
    }

    public void setFIN() {
        this.FIN = true;
    }

    public void setSYN() {
        this.SYN = true;
    }

    public boolean getSYN() {
        return SYN;
    }

    public void setACK(int ackNum) {
        this.ACK = true;
        this.ackNum = ackNum;
    }

    public boolean getACK() {
        return ACK;
    }

    public int getAckNum() {
        return ackNum;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * Get the SocketAddress the packet was sent from.
     *
     * @return the SocketAddress of the sender.
     */
    public SocketAddress getSocketAddress() {
        return from;
    }

    public byte[] getBytes() throws IOException {
        try (ByteArrayOutputStream bStream = new ByteArrayOutputStream();
             ObjectOutputStream oStream = new ObjectOutputStream(bStream)) {

            oStream.writeObject(this);
            oStream.flush();
            return bStream.toByteArray();
        }
    }

    public static ReldatPacket fromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bStream = new ByteArrayInputStream(bytes);
             ObjectInputStream oStream = new ObjectInputStream(bStream)) {
            return (ReldatPacket) oStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Factory method for a ReldatPacket from an UDP datagram.
     *
     * @param datagram the UDP packet
     * @return the new ReldatPacket
     */
    public static ReldatPacket fromUDP(DatagramPacket datagram) throws IOException {
        ReldatPacket packet = fromBytes(datagram.getData());
        packet.from = datagram.getSocketAddress();
        return packet;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ReldatPacket)) return false;

        ReldatPacket p2 = (ReldatPacket) other;

        return SYN == p2.SYN
                && ACK == p2.ACK
                && FIN == p2.FIN
                && size == p2.size
                && windowSize == p2.windowSize
                && Objects.equals(checksum, p2.checksum)
                && seqNum == p2.seqNum
                && ackNum == p2.ackNum
                && Objects.deepEquals(data, p2.data);
    }
}
