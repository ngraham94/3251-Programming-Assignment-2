package network;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ReldatPacketTest {
    @Test
    void testSerialization() throws Exception {
        // Generate random data
        Random r = new Random();
        byte[] data = new byte[500];
        r.nextBytes(data);
        int windowSize = r.nextInt();
        int seqNum = r.nextInt();
        int ackNum = r.nextInt();

        ReldatPacket packet = new ReldatPacket(data, windowSize, seqNum, ackNum);

        byte[] serialized = packet.getBytes();

        ReldatPacket deserialized = ReldatPacket.fromBytes(serialized);

        // Assert packets are the same
        assertEquals(packet, deserialized);

        // Check if re-serializing deserialized object yields same result
        byte[] serialized2 = deserialized.getBytes();
        assertArrayEquals(serialized, serialized2);

        // Check if the data is equal (the assertEquals above should already
        // handle this but adding this to be sure)
        assertArrayEquals(data, deserialized.getData());
    }
}