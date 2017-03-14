package network;

import java.io.*;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class ReldatPacket implements Serializable {
    /** Packet headers */
    private boolean SYN, ACK, FIN;
    private int size, windowSize;
    private int seqNum, ackNum;
    private byte[] checksum;

    /**
     * The SocketAddress the packet was sent from.
     * This is not included in the header since it is a part of the UDP header.
     */
    private transient SocketAddress from;

    // Initialize to empty array to getHeaderSize
    // This has the unfortunate side effect of making the header bigger
    // for header-only packets, but whatever...
    private byte[] data;


    /**
     * Constructor for packet without data (header-only packet)
     */
    public ReldatPacket(int windowSize, int seqNum) {
        this.windowSize = windowSize;
        this.seqNum = seqNum;
        this.checksum = calcChecksum();
    }

    public ReldatPacket(byte[] data, int windowSize, int seqNum) {
        this(windowSize, seqNum);
        this.data = data;
        this.size = data.length;
        this.checksum = calcChecksum();
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

    private byte[] calcChecksum() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Buffer for headers
            ByteBuffer buffer = ByteBuffer.allocate(50);
            buffer.putInt(SYN ? 1 : 0);
            buffer.putInt(ACK ? 1 : 0);
            buffer.putInt(FIN ? 1 : 0);
            buffer.putInt(size);
            buffer.putInt(windowSize);
            buffer.putInt(seqNum);
            buffer.putInt(ackNum);

            // Update and digest!
            md.update(buffer.array());
            if (data != null) md.update(data);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            // Should never happen since the algorithm is hardcoded
            return null;
        }
    }

    public boolean verifyChecksum() {
        return Arrays.equals(checksum, calcChecksum());
    }

    public void setFIN() {
        this.FIN = true;
        checksum = calcChecksum();
    }

    public void setSYN() {
        this.SYN = true;
        checksum = calcChecksum();
    }

    public boolean getSYN() {
        return SYN;
    }

    public void setACK(int ackNum) {
        this.ACK = true;
        this.ackNum = ackNum;
        checksum = calcChecksum();
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
        this.size = data.length;
        this.checksum = calcChecksum();
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
        try (ByteArrayOutputStream bstream = new ByteArrayOutputStream();
             DataOutputStream stream = new DataOutputStream(bstream)) {

            stream.writeBoolean(SYN);
            stream.writeBoolean(ACK);
            stream.writeBoolean(FIN);
            stream.writeInt(size);
            stream.writeInt(windowSize);
            stream.writeInt(seqNum);
            stream.writeInt(ackNum);
            stream.write(checksum);
            if (data != null) stream.write(data);

            stream.flush();
            return bstream.toByteArray();
        }
    }

    /**
     * Deserialize a bytes array into a ReldatPacket.
     *
     * @param bytes the array of bytes to deserialize
     * @return the packet
     * @throws IOException when deserialization fails due to corrupt data
     */
    public static ReldatPacket fromBytes(byte[] bytes) throws IOException {
        try (ByteArrayInputStream bstream = new ByteArrayInputStream(bytes);
             DataInputStream stream = new DataInputStream(bstream)) {

            boolean SYN = stream.readBoolean();
            boolean ACK = stream.readBoolean();
            boolean FIN = stream.readBoolean();

            int size = stream.readInt();
            int windowSize = stream.readInt();
            int seqNum = stream.readInt();
            int ackNum = stream.readInt();

            byte[] checksum = new byte[16];
            stream.read(checksum);

            ReldatPacket packet = new ReldatPacket(windowSize, seqNum);
            packet.SYN = SYN;
            packet.ACK = ACK;
            packet.FIN = FIN;
            packet.size = size;
            packet.windowSize = windowSize;
            packet.ackNum = ackNum;
            packet.checksum = checksum;

            if (size > 0) {
                byte[] data = new byte[size];
                stream.read(data);
                packet.data = data;
            }

            return packet;
        }
    }

    /**
     * Factory method for a ReldatPacket from an UDP datagram.
     *
     * @param datagram the UDP packet
     * @return the new ReldatPacket
     * @throws IOException when deserialization fails
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
                && Arrays.equals(checksum, p2.checksum)
                && seqNum == p2.seqNum
                && ackNum == p2.ackNum
                && Arrays.equals(data, p2.data);
    }
}
