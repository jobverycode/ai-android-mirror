package com.ai.mirror.data.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PacketCodec {

    fun encode(packet: StreamPacket): ByteArray {
        val totalSize = MirrorProtocol.HEADER_SIZE + packet.payload.size
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.BIG_ENDIAN)

        // Magic Header (4 bytes)
        buffer.put(MirrorProtocol.MAGIC_HEADER)
        // Version (1 byte)
        buffer.put(packet.version)
        // Type (1 byte)
        buffer.put(packet.type)
        // Flags (2 bytes)
        buffer.putShort(packet.flags)
        // Sequence Number (8 bytes)
        buffer.putLong(packet.sequenceNumber)
        // Timestamp (8 bytes)
        buffer.putLong(packet.timestamp)
        // Width (4 bytes)
        buffer.putInt(packet.width)
        // Height (4 bytes)
        buffer.putInt(packet.height)
        // Rotation (4 bytes)
        buffer.putInt(packet.rotation)
        // Payload Length (4 bytes)
        buffer.putInt(packet.payload.size)
        // Payload
        if (packet.payload.isNotEmpty()) {
            buffer.put(packet.payload)
        }

        return buffer.array()
    }

    fun decode(data: ByteArray, offset: Int = 0, length: Int = data.size): StreamPacket? {
        if (length < MirrorProtocol.HEADER_SIZE) {
            return null
        }

        val buffer = ByteBuffer.wrap(data, offset, length).order(ByteOrder.BIG_ENDIAN)

        // Check Magic Header
        for (i in 0 until 4) {
            if (buffer.get() != MirrorProtocol.MAGIC_HEADER[i]) {
                return null
            }
        }

        val version = buffer.get()
        val type = buffer.get()
        val flags = buffer.short
        val sequenceNumber = buffer.long
        val timestamp = buffer.long
        val width = buffer.int
        val height = buffer.int
        val rotation = buffer.int
        val payloadLength = buffer.int

        if (payloadLength < 0 || payloadLength > (length - MirrorProtocol.HEADER_SIZE)) {
            return null
        }

        val payload = ByteArray(payloadLength)
        if (payloadLength > 0) {
            buffer.get(payload)
        }

        return StreamPacket(
            version = version,
            type = type,
            flags = flags,
            sequenceNumber = sequenceNumber,
            timestamp = timestamp,
            width = width,
            height = height,
            rotation = rotation,
            payload = payload
        )
    }

    fun decode(buffer: ByteBuffer): StreamPacket? {
        if (buffer.remaining() < MirrorProtocol.HEADER_SIZE) {
            return null
        }

        val markPosition = buffer.position()
        val originalOrder = buffer.order()
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Check Magic
        val b0 = buffer.get()
        val b1 = buffer.get()
        val b2 = buffer.get()
        val b3 = buffer.get()
        if (b0 != MirrorProtocol.MAGIC_HEADER[0] ||
            b1 != MirrorProtocol.MAGIC_HEADER[1] ||
            b2 != MirrorProtocol.MAGIC_HEADER[2] ||
            b3 != MirrorProtocol.MAGIC_HEADER[3]
        ) {
            buffer.position(markPosition)
            buffer.order(originalOrder)
            return null
        }

        val version = buffer.get()
        val type = buffer.get()
        val flags = buffer.short
        val sequenceNumber = buffer.long
        val timestamp = buffer.long
        val width = buffer.int
        val height = buffer.int
        val rotation = buffer.int
        val payloadLength = buffer.int

        if (payloadLength < 0 || buffer.remaining() < payloadLength) {
            buffer.position(markPosition)
            buffer.order(originalOrder)
            return null
        }

        val payload = ByteArray(payloadLength)
        if (payloadLength > 0) {
            buffer.get(payload)
        }

        buffer.order(originalOrder)
        return StreamPacket(
            version = version,
            type = type,
            flags = flags,
            sequenceNumber = sequenceNumber,
            timestamp = timestamp,
            width = width,
            height = height,
            rotation = rotation,
            payload = payload
        )
    }
}
