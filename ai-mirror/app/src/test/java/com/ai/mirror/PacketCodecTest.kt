package com.ai.mirror

import com.ai.mirror.data.protocol.MirrorProtocol
import com.ai.mirror.data.protocol.PacketCodec
import com.ai.mirror.data.protocol.StreamPacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer

class PacketCodecTest {

    @Test
    fun testEncodeAndDecodeFramePacket() {
        val payload = "test_jpeg_data_bytes_12345".toByteArray(Charsets.UTF_8)
        val originalPacket = StreamPacket(
            version = MirrorProtocol.PROTOCOL_VERSION,
            type = MirrorProtocol.TYPE_FRAME_DATA,
            flags = 1,
            sequenceNumber = 42L,
            timestamp = 1234567890L,
            width = 1280,
            height = 720,
            rotation = 90,
            payload = payload
        )

        val encoded = PacketCodec.encode(originalPacket)
        assertEquals(MirrorProtocol.HEADER_SIZE + payload.size, encoded.size)

        val decoded = PacketCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(originalPacket.version, decoded!!.version)
        assertEquals(originalPacket.type, decoded.type)
        assertEquals(originalPacket.flags, decoded.flags)
        assertEquals(originalPacket.sequenceNumber, decoded.sequenceNumber)
        assertEquals(originalPacket.timestamp, decoded.timestamp)
        assertEquals(originalPacket.width, decoded.width)
        assertEquals(originalPacket.height, decoded.height)
        assertEquals(originalPacket.rotation, decoded.rotation)
        assertArrayEquals(originalPacket.payload, decoded.payload)
    }

    @Test
    fun testEmptyPayloadPacket() {
        val originalPacket = StreamPacket(
            type = MirrorProtocol.TYPE_HEARTBEAT_PING,
            timestamp = System.currentTimeMillis()
        )

        val encoded = PacketCodec.encode(originalPacket)
        assertEquals(MirrorProtocol.HEADER_SIZE, encoded.size)

        val decoded = PacketCodec.decode(encoded)
        assertNotNull(decoded)
        assertEquals(MirrorProtocol.TYPE_HEARTBEAT_PING, decoded!!.type)
        assertEquals(0, decoded.payload.size)
    }

    @Test
    fun testDecodeInvalidMagicHeader() {
        val originalPacket = StreamPacket(
            type = MirrorProtocol.TYPE_FRAME_DATA,
            payload = byteArrayOf(1, 2, 3)
        )
        val encoded = PacketCodec.encode(originalPacket)
        // Corrupt magic header
        encoded[0] = 0x00

        val decoded = PacketCodec.decode(encoded)
        assertNull(decoded)
    }

    @Test
    fun testDecodeTruncatedBuffer() {
        val payload = ByteArray(100) { 1 }
        val originalPacket = StreamPacket(
            type = MirrorProtocol.TYPE_FRAME_DATA,
            payload = payload
        )
        val encoded = PacketCodec.encode(originalPacket)

        // Pass truncated data
        val truncated = encoded.copyOf(MirrorProtocol.HEADER_SIZE + 50)
        val decoded = PacketCodec.decode(truncated)
        assertNull(decoded)
    }

    @Test
    fun testDecodeByteBufferStream() {
        val payload1 = "frame1".toByteArray()
        val payload2 = "frame2_longer_content".toByteArray()

        val p1 = StreamPacket(type = MirrorProtocol.TYPE_FRAME_DATA, sequenceNumber = 1L, payload = payload1)
        val p2 = StreamPacket(type = MirrorProtocol.TYPE_FRAME_DATA, sequenceNumber = 2L, payload = payload2)

        val enc1 = PacketCodec.encode(p1)
        val enc2 = PacketCodec.encode(p2)

        val combinedBuffer = ByteBuffer.allocate(enc1.size + enc2.size)
        combinedBuffer.put(enc1)
        combinedBuffer.put(enc2)
        combinedBuffer.flip()

        val decoded1 = PacketCodec.decode(combinedBuffer)
        assertNotNull(decoded1)
        assertEquals(1L, decoded1!!.sequenceNumber)
        assertArrayEquals(payload1, decoded1.payload)

        val decoded2 = PacketCodec.decode(combinedBuffer)
        assertNotNull(decoded2)
        assertEquals(2L, decoded2!!.sequenceNumber)
        assertArrayEquals(payload2, decoded2.payload)
    }
}
