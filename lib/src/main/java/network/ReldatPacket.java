package network;

import java.io.*;
import java.security.MessageDigest;
import java.util.Objects;

public class ReldatPacket implements Serializable {
    /** Packet headers */
    private boolean SYN, ACK, FIN;
    private short size, windowSize;
    private MessageDigest checksum;
    private int seqNum, ackNum;

    // Initialize to empty array to getHeaderSize
    // This has the unfortunate side effect of making the header bigger
    // for header-only packets, but whatever...
    private byte[] data = {};


    /**
     * Constructor for packet without data (header-only packet)
     */
    public ReldatPacket(int windowSize, int seqNum) {
        this.windowSize = (short) windowSize;
        this.seqNum = (short) seqNum;
    }

    public ReldatPacket(byte[] data, int windowSize, int seqNum) {
        this(windowSize, seqNum);
        this.data = data;
        this.size = (short) data.length;
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

    public void calcChecksum() {
        // TODO
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

    public void setACK(int ackNum) {
        this.ACK = true;
        this.ackNum = ackNum;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public byte[] getBytes() throws IOException {
        try (ByteArrayOutputStream bStream = new ByteArrayOutputStream();
             ObjectOutputStream oStream = new ObjectOutputStream(bStream)) {

            oStream.writeObject(this);
            oStream.flush();
            return bStream.toByteArray();
        }
    }

    public static ReldatPacket fromBytes(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bStream = new ByteArrayInputStream(bytes);
            ObjectInputStream oStream = new ObjectInputStream(bStream)) {
            return (ReldatPacket) oStream.readObject();
        }
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
