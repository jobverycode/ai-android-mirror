package com.ai.mirror.data.protocol

object MirrorProtocol {
    val MAGIC_HEADER = byteArrayOf(0x41, 0x49, 0x4D, 0x52) // "AIMR"
    const val PROTOCOL_VERSION: Byte = 0x01

    const val TYPE_FRAME_DATA: Byte = 0x01
    const val TYPE_PAIR_REQUEST: Byte = 0x02
    const val TYPE_PAIR_RESPONSE: Byte = 0x03
    const val TYPE_HEARTBEAT_PING: Byte = 0x04
    const val TYPE_HEARTBEAT_PONG: Byte = 0x05
    const val TYPE_CONTROL_CONFIG: Byte = 0x06

    const val HEADER_SIZE = 40
    const val DEFAULT_STREAM_PORT = 8888
    const val DEFAULT_BEACON_PORT = 8889
    const val NSD_SERVICE_TYPE = "_aimirror._tcp."
}

data class StreamPacket(
    val version: Byte = MirrorProtocol.PROTOCOL_VERSION,
    val type: Byte,
    val flags: Short = 0,
    val sequenceNumber: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val payload: ByteArray = ByteArray(0)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StreamPacket

        if (version != other.version) return false
        if (type != other.type) return false
        if (flags != other.flags) return false
        if (sequenceNumber != other.sequenceNumber) return false
        if (timestamp != other.timestamp) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (rotation != other.rotation) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.toInt()
        result = 31 * result + type
        result = 31 * result + flags
        result = 31 * result + sequenceNumber.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rotation
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

data class PairRequestPayload(
    val deviceId: String,
    val deviceName: String,
    val role: String,
    val clientTimestamp: Long = System.currentTimeMillis()
)

data class PairResponsePayload(
    val accepted: Boolean,
    val serverDeviceId: String,
    val serverDeviceName: String,
    val message: String = ""
)
