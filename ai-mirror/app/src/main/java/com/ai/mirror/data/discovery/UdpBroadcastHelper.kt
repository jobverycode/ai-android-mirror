package com.ai.mirror.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.data.protocol.MirrorProtocol
import com.ai.mirror.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketException

data class UdpBeaconPayload(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val role: String,
    val isStreaming: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class UdpBroadcastHelper(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String,
    private val role: DeviceRole,
    private val isStreaming: Boolean = false,
    private val streamPort: Int = MirrorProtocol.DEFAULT_STREAM_PORT,
    private val beaconPort: Int = MirrorProtocol.DEFAULT_BEACON_PORT,
    private val onDeviceDiscovered: (DiscoveredDevice) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()

    private var broadcastJob: Job? = null
    private var listenJob: Job? = null
    private var listenSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var isRunning = false

    @Synchronized
    fun start() {
        if (isRunning) return
        isRunning = true

        acquireMulticastLock()
        startListening()
        startBroadcasting()
    }

    @Synchronized
    fun stop() {
        isRunning = false
        broadcastJob?.cancel()
        broadcastJob = null
        listenJob?.cancel()
        listenJob = null

        try {
            listenSocket?.close()
            listenSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("AiMirrorMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseMulticastLock() {
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
            multicastLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startBroadcasting() {
        broadcastJob = scope.launch {
            while (isActive && isRunning) {
                try {
                    val localIp = NetworkUtils.getLocalIpAddress()
                    if (localIp != null) {
                        val broadcastAddr = NetworkUtils.getBroadcastAddress()
                        if (broadcastAddr != null) {
                            val payload = UdpBeaconPayload(
                                id = deviceId,
                                name = deviceName,
                                ip = localIp,
                                port = streamPort,
                                role = role.name,
                                isStreaming = isStreaming,
                                timestamp = System.currentTimeMillis()
                            )
                            val jsonBytes = gson.toJson(payload).toByteArray(Charsets.UTF_8)
                            val socket = DatagramSocket()
                            socket.broadcast = true
                            val packet = DatagramPacket(
                                jsonBytes,
                                jsonBytes.size,
                                broadcastAddr,
                                beaconPort
                            )
                            socket.send(packet)
                            socket.close()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
                delay(1200)
            }
        }
    }

    private fun startListening() {
        listenJob = scope.launch {
            try {
                listenSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(beaconPort))
                    broadcast = true
                }

                val buffer = ByteArray(2048)
                while (isActive && isRunning) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        listenSocket?.receive(packet)
                        val text = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        val beacon = gson.fromJson(text, UdpBeaconPayload::class.java)

                        // Avoid discovering self
                        if (beacon.id != deviceId) {
                            val senderIp = packet.address.hostAddress ?: beacon.ip
                            val deviceRole = try {
                                DeviceRole.valueOf(beacon.role)
                            } catch (e: Exception) {
                                DeviceRole.RECEIVER
                            }

                            val device = DiscoveredDevice(
                                id = beacon.id,
                                name = beacon.name,
                                ip = senderIp,
                                port = beacon.port,
                                role = deviceRole,
                                isStreaming = beacon.isStreaming,
                                lastSeenTimestamp = System.currentTimeMillis()
                            )
                            onDeviceDiscovered(device)
                        }
                    } catch (e: SocketException) {
                        break
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
