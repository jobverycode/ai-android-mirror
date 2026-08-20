package com.ai.mirror.data.streaming

import com.ai.mirror.data.protocol.MirrorProtocol
import com.ai.mirror.data.protocol.PacketCodec
import com.ai.mirror.data.protocol.PairRequestPayload
import com.ai.mirror.data.protocol.PairResponsePayload
import com.ai.mirror.data.protocol.StreamPacket
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class StreamServer(
    private val port: Int = MirrorProtocol.DEFAULT_STREAM_PORT,
    private val deviceId: String,
    private val deviceName: String,
    private val autoAcceptPairing: Boolean = true,
    private val onPairRequested: ((PairRequestPayload, (Boolean) -> Unit) -> Unit)? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var serverInstance: InternalWebSocketServer? = null

    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount: StateFlow<Int> = _connectedClientsCount.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    val stats = StreamStatsCalculator()
    private val sequenceCounter = AtomicLong(0L)

    private val pairedClients = ConcurrentHashMap<WebSocket, PairRequestPayload>()

    @Synchronized
    fun start() {
        if (_isServerRunning.value) return
        try {
            val address = InetSocketAddress(port)
            serverInstance = InternalWebSocketServer(address)
            serverInstance?.isReuseAddr = true
            serverInstance?.start()
            _isServerRunning.value = true
        } catch (e: Exception) {
            e.printStackTrace()
            _isServerRunning.value = false
        }
    }

    @Synchronized
    fun stop() {
        if (!_isServerRunning.value) return
        try {
            serverInstance?.stop(1000)
            serverInstance = null
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            pairedClients.clear()
            _connectedClientsCount.value = 0
            _isServerRunning.value = false
            stats.reset()
        }
    }

    fun broadcastFrame(
        jpegBytes: ByteArray,
        width: Int,
        height: Int,
        rotation: Int
    ) {
        if (!_isServerRunning.value || pairedClients.isEmpty()) return

        val seq = sequenceCounter.incrementAndGet()
        val packet = StreamPacket(
            type = MirrorProtocol.TYPE_FRAME_DATA,
            sequenceNumber = seq,
            timestamp = System.currentTimeMillis(),
            width = width,
            height = height,
            rotation = rotation,
            payload = jpegBytes
        )

        val encodedBytes = PacketCodec.encode(packet)
        stats.onFrameProcessed(encodedBytes.size, width, height)

        val clients = pairedClients.keys
        for (client in clients) {
            if (client.isOpen) {
                try {
                    client.send(encodedBytes)
                } catch (e: Exception) {
                    e.printStackTrace()
                    stats.onFrameDropped()
                }
            }
        }
    }

    private inner class InternalWebSocketServer(address: InetSocketAddress) : WebSocketServer(address) {

        override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
            // Wait for pair request
        }

        override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
            if (conn != null) {
                pairedClients.remove(conn)
                _connectedClientsCount.value = pairedClients.size
            }
        }

        override fun onMessage(conn: WebSocket?, message: String?) {
            // Text messages if any
        }

        override fun onMessage(conn: WebSocket?, bytes: ByteBuffer?) {
            if (conn == null || bytes == null) return
            val packet = PacketCodec.decode(bytes) ?: return

            when (packet.type) {
                MirrorProtocol.TYPE_PAIR_REQUEST -> {
                    try {
                        val payloadStr = String(packet.payload, Charsets.UTF_8)
                        val pairReq = gson.fromJson(payloadStr, PairRequestPayload::class.java)

                        if (autoAcceptPairing || onPairRequested == null) {
                            acceptPairing(conn, pairReq)
                        } else {
                            scope.launch {
                                onPairRequested.invoke(pairReq) { accepted ->
                                    if (accepted) {
                                        acceptPairing(conn, pairReq)
                                    } else {
                                        rejectPairing(conn, pairReq)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                MirrorProtocol.TYPE_HEARTBEAT_PING -> {
                    // Reply pong
                    val pongPacket = StreamPacket(
                        type = MirrorProtocol.TYPE_HEARTBEAT_PONG,
                        timestamp = System.currentTimeMillis()
                    )
                    conn.send(PacketCodec.encode(pongPacket))
                }
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception?) {
            ex?.printStackTrace()
        }

        override fun onStart() {
            _isServerRunning.value = true
        }

        private fun acceptPairing(conn: WebSocket, pairReq: PairRequestPayload) {
            pairedClients[conn] = pairReq
            _connectedClientsCount.value = pairedClients.size

            val responsePayload = PairResponsePayload(
                accepted = true,
                serverDeviceId = deviceId,
                serverDeviceName = deviceName,
                message = "Pairing successful"
            )
            val json = gson.toJson(responsePayload).toByteArray(Charsets.UTF_8)
            val packet = StreamPacket(
                type = MirrorProtocol.TYPE_PAIR_RESPONSE,
                payload = json
            )
            conn.send(PacketCodec.encode(packet))
        }

        private fun rejectPairing(conn: WebSocket, pairReq: PairRequestPayload) {
            val responsePayload = PairResponsePayload(
                accepted = false,
                serverDeviceId = deviceId,
                serverDeviceName = deviceName,
                message = "Pairing rejected by host"
            )
            val json = gson.toJson(responsePayload).toByteArray(Charsets.UTF_8)
            val packet = StreamPacket(
                type = MirrorProtocol.TYPE_PAIR_RESPONSE,
                payload = json
            )
            conn.send(PacketCodec.encode(packet))
            conn.close()
        }
    }
}
