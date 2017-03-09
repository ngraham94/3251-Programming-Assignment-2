package network;

import java.security.MessageDigest;

public class ReldatPacket {
    /** Flags */
    private boolean SYN, ACK, FIN;

    private short size, windowSize;

    private MessageDigest checksum;

    private int seqNum, ackNum;

    private int sourcePort, destPort;
}
