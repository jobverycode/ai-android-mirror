package com.ai.mirror.data.streaming

import com.ai.mirror.data.protocol.MirrorProtocol
import com.ai.mirror.data.protocol.PacketCodec
import com.ai.mirror.data.protocol.PairRequestPayload
import com.ai.mirror.data.protocol.PairResponsePayload
import com.ai.mirror.data.protocol.StreamPacket
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.nio.ByteBuffer

enum class ConnectionState {
    IDLE,
    CONNECTING,
    PAIRED,
    REJECTED,
    DISCONNECTED,
    ERROR
}

data class ReceivedFrame(
    val jpegData: ByteArray,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val timestamp: Long,
    val latencyMs: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceivedFrame

        if (!jpegData.contentEquals(other.jpegData)) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (rotation != other.rotation) return false
        if (timestamp != other.timestamp) return false
        if (latencyMs != other.latencyMs) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jpegData.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + rotation
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + latencyMs.hashCode()
        return result
    }
}

class StreamClient(
    private val deviceId: String,
    private val deviceName: String
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var clientInstance: InternalWebSocketClient? = null
    private var heartbeatJob: Job? = null

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _latestFrame = MutableStateFlow<ReceivedFrame?>(null)
    val latestFrame: StateFlow<ReceivedFrame?> = _latestFrame.asStateFlow()

    val stats = StreamStatsCalculator()

    fun connect(targetIp: String, targetPort: Int) {
        disconnect()
        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value = "正在连接 $targetIp:$targetPort …"

        scope.launch(Dispatchers.IO) {
            try {
                val uri = URI("ws://$targetIp:$targetPort")
                clientInstance = InternalWebSocketClient(uri).apply {
                    connectionLostTimeout = 10
                    setTcpNoDelay(true)
                }
                clientInstance?.connect()
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.ERROR
                _statusMessage.value = formatErrorMessage(e)
            }
        }
    }

    fun disconnect() {
        heartbeatJob?.cancel()
        heartbeatJob = null
        try {
            clientInstance?.close()
            clientInstance = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _connectionState.value = ConnectionState.DISCONNECTED
            _statusMessage.value = "已断开连接"
            _latestFrame.value = null
            stats.reset()
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.PAIRED) {
                delay(2500)
                try {
                    val pingPacket = StreamPacket(
                        type = MirrorProtocol.TYPE_HEARTBEAT_PING,
                        timestamp = System.currentTimeMillis()
                    )
                    clientInstance?.send(PacketCodec.encode(pingPacket))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun formatErrorMessage(ex: Throwable?): String {
        if (ex == null) return "未知连接异常"
        val msg = ex.message ?: ""
        return when {
            ex is ConnectException || msg.contains("refused", ignoreCase = true) || msg.contains("ECONNREFUSED", ignoreCase = true) ->
                "连接被拒绝：请确保另一台手机已点击【发送端】打开摄像头推流"
            ex is SocketTimeoutException || msg.contains("timed out", ignoreCase = true) ->
                "连接超时：请确认两台手机处于同一 Wi-Fi 局域网且 IP 正确"
            msg.contains("ENETUNREACH", ignoreCase = true) || msg.contains("No route to host", ignoreCase = true) ->
                "网络不可达：请检查 Wi-Fi 连接状态"
            else ->
                "连接失败: ${ex.localizedMessage ?: msg}"
        }
    }

    private inner class InternalWebSocketClient(uri: URI) : WebSocketClient(uri) {

        override fun onOpen(handshakedata: ServerHandshake?) {
            _statusMessage.value = "正在握手配对…"
            val pairPayload = PairRequestPayload(
                deviceId = deviceId,
                deviceName = deviceName,
                role = "RECEIVER",
                clientTimestamp = System.currentTimeMillis()
            )
            val json = gson.toJson(pairPayload).toByteArray(Charsets.UTF_8)
            val packet = StreamPacket(
                type = MirrorProtocol.TYPE_PAIR_REQUEST,
                payload = json
            )
            send(PacketCodec.encode(packet))
        }

        override fun onMessage(message: String?) {
            // Unused
        }

        override fun onMessage(bytes: ByteBuffer?) {
            if (bytes == null) return
            val packet = PacketCodec.decode(bytes) ?: return

            when (packet.type) {
                MirrorProtocol.TYPE_PAIR_RESPONSE -> {
                    try {
                        val payloadStr = String(packet.payload, Charsets.UTF_8)
                        val pairResponse = gson.fromJson(payloadStr, PairResponsePayload::class.java)
                        if (pairResponse.accepted) {
                            _connectionState.value = ConnectionState.PAIRED
                            _statusMessage.value = "已成功配对：${pairResponse.serverDeviceName}"
                            startHeartbeat()
                        } else {
                            _connectionState.value = ConnectionState.REJECTED
                            _statusMessage.value = "配对请求被对方拒绝: ${pairResponse.message}"
                            close()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                MirrorProtocol.TYPE_FRAME_DATA -> {
                    val now = System.currentTimeMillis()
                    val latency = (now - packet.timestamp).coerceAtLeast(0)

                    stats.onFrameProcessed(
                        bytes = packet.payload.size,
                        width = packet.width,
                        height = packet.height,
                        latencyMs = latency
                    )

                    _latestFrame.value = ReceivedFrame(
                        jpegData = packet.payload,
                        width = packet.width,
                        height = packet.height,
                        rotation = packet.rotation,
                        timestamp = packet.timestamp,
                        latencyMs = latency
                    )
                }
                MirrorProtocol.TYPE_HEARTBEAT_PONG -> {
                    // Pong received
                }
            }
        }

        override fun onClose(code: Int, reason: String?, remote: Boolean) {
            if (_connectionState.value == ConnectionState.PAIRED) {
                _connectionState.value = ConnectionState.DISCONNECTED
                _statusMessage.value = "连接已中断 ($reason)"
            }
        }

        override fun onError(ex: Exception?) {
            ex?.printStackTrace()
            _connectionState.value = ConnectionState.ERROR
            _statusMessage.value = formatErrorMessage(ex)
        }
    }
}
