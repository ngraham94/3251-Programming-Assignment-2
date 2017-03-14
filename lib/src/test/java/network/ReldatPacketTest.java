package network;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ReldatPacketTest {
    private ReldatPacket genPacket(int dataSize) {
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

    @Test
    void testChecksum() {
        // Basic test
        ReldatPacket packet = genPacket(750);
        assertTrue(packet.verifyChecksum());

        // Test update operations update checksum
        packet.setSYN();
        assertTrue(packet.verifyChecksum());
        packet.setACK((int) (Math.random() * 500));
        assertTrue(packet.verifyChecksum());
        packet.setFIN();
        assertTrue(packet.verifyChecksum());
        byte[] data = new byte[500];
        new Random().nextBytes(data);
        packet.setData(data);
        assertTrue(packet.verifyChecksum());
    }

    @Test
    void testChecksumCorrupt() throws NoSuchFieldException, IllegalAccessException {
        Random rand = new Random();

        // SYN
        ReldatPacket packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        Field field = ReldatPacket.class.getDeclaredField("SYN");
        field.setAccessible(true);
        field.setBoolean(packet, true);
        assertFalse(packet.verifyChecksum());

        // ACK
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("ACK");
        field.setAccessible(true);
        field.setBoolean(packet, true);
        assertFalse(packet.verifyChecksum());

        // FIN
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("FIN");
        field.setAccessible(true);
        field.setBoolean(packet, true);
        assertFalse(packet.verifyChecksum());

        // size
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("size");
        field.setAccessible(true);
        field.setInt(packet, rand.nextInt());
        assertFalse(packet.verifyChecksum());

        // windowSize
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("windowSize");
        field.setAccessible(true);
        field.setInt(packet, rand.nextInt());
        assertFalse(packet.verifyChecksum());

        // seqNum
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("seqNum");
        field.setAccessible(true);
        field.setInt(packet, rand.nextInt());
        assertFalse(packet.verifyChecksum());

        // ackNum
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("ackNum");
        field.setAccessible(true);
        field.setInt(packet, rand.nextInt());
        assertFalse(packet.verifyChecksum());

        // data
        packet = genPacket(750);
        assertTrue(packet.verifyChecksum());  // Sanity check
        field = ReldatPacket.class.getDeclaredField("data");
        field.setAccessible(true);
        byte[] data = new byte[749];
        rand.nextBytes(data);
        field.set(packet, data);
        assertFalse(packet.verifyChecksum());
    }
}