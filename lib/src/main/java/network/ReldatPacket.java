package network;

import java.security.MessageDigest;

public class ReldatPacket {
    /** Flags */
    private boolean SYN, ACK, FIN;

    /** The size of the packet payload in bytes */
    private short size;

    /** The checksum to check for corrupted packets */
    private MessageDigest checksum;
}
