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

    private byte[] data;

    public ReldatPacket(byte[] data, int windowSize, int seqNum) {
        this.data = data;
        this.size = (short) data.length;
        this.windowSize = (short) windowSize;
        this.seqNum = seqNum;
    }

    public ReldatPacket(byte[] data, int windowSize, int seqNum, int ackNum) {
        this(data, windowSize, seqNum);
        this.ACK = true;
        this.ackNum = ackNum;
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
