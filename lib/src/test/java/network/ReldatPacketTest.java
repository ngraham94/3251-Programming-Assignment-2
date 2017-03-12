package network;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ReldatPacketTest {
    ReldatPacket genPacket(int dataSize) {
        // Generate random data
        Random r = new Random();
        byte[] data = new byte[dataSize];
        r.nextBytes(data);
        int windowSize = r.nextInt();
        int seqNum = r.nextInt();

        return new ReldatPacket(data, windowSize, seqNum);
    }

    @Test
    void getHeaderSize() throws Exception {
        int headerSize = ReldatPacket.getHeaderSize();

        assertNotEquals(-1, headerSize);

        // Test on empty packet
        ReldatPacket noData = new ReldatPacket(50, 50);
        assertEquals(headerSize, noData.getBytes().length);

        // Packet size = 500 bytes
        ReldatPacket packet = genPacket(500);
        assertEquals(headerSize + 500, packet.getBytes().length);

        // Packet size = 1 byte
        packet = genPacket(1);
        assertEquals(headerSize + 1, packet.getBytes().length);
    }

    @Test
    void testSerialization() throws Exception {
        ReldatPacket packet = genPacket(500);

        byte[] serialized = packet.getBytes();

        ReldatPacket deserialized = ReldatPacket.fromBytes(serialized);

        // Assert packets are the same
        assertEquals(packet, deserialized);

        // Check if re-serializing deserialized object yields same result
        byte[] serialized2 = deserialized.getBytes();
        assertArrayEquals(serialized, serialized2);

        // Check if the data is equal (the assertEquals above should already
        // handle this but adding this to be sure)
        assertArrayEquals(packet.getData(), deserialized.getData());
    }
}