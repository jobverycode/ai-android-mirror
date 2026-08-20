package com.ai.mirror.data.discovery

import android.content.Context
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.data.protocol.MirrorProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class DeviceDiscoveryManager(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String,
    private val role: DeviceRole,
    private val streamPort: Int = MirrorProtocol.DEFAULT_STREAM_PORT
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val deviceMap = ConcurrentHashMap<String, DiscoveredDevice>()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private var udpHelper: UdpBroadcastHelper? = null
    private var nsdHelper: NsdDiscoveryHelper? = null
    private var cleanupJob: Job? = null
    private var isScanning = false

    @Synchronized
    fun startDiscovery() {
        if (isScanning) return
        isScanning = true

        udpHelper = UdpBroadcastHelper(
            deviceId = deviceId,
            deviceName = deviceName,
            role = role,
            streamPort = streamPort,
            onDeviceDiscovered = ::onDeviceFound
        )
        udpHelper?.start()

        nsdHelper = NsdDiscoveryHelper(
            context = context,
            deviceId = deviceId,
            deviceName = deviceName,
            role = role,
            streamPort = streamPort,
            onDeviceDiscovered = ::onDeviceFound
        )
        nsdHelper?.start()

        startCleanupJob()
    }

    @Synchronized
    fun stopDiscovery() {
        isScanning = false
        cleanupJob?.cancel()
        cleanupJob = null

        udpHelper?.stop()
        udpHelper = null

        nsdHelper?.stop()
        nsdHelper = null

        deviceMap.clear()
        _discoveredDevices.value = emptyList()
    }

    private fun onDeviceFound(device: DiscoveredDevice) {
        if (device.id == deviceId) return
        val key = "${device.ip}:${device.port}"
        deviceMap[key] = device
        updateList()
    }

    private fun startCleanupJob() {
        cleanupJob = scope.launch {
            while (isActive && isScanning) {
                delay(2000)
                val now = System.currentTimeMillis()
                var changed = false
                val iterator = deviceMap.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    // Remove device if not seen in 6 seconds
                    if (now - entry.value.lastSeenTimestamp > 6000) {
                        iterator.remove()
                        changed = true
                    }
                }
                if (changed) {
                    updateList()
                }
            }
        }
    }

    private fun updateList() {
        _discoveredDevices.value = deviceMap.values.toList().sortedBy { it.name }
    }
}
