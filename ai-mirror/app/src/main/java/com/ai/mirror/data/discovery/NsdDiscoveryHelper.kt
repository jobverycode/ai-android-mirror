package com.ai.mirror.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.DiscoveredDevice
import com.ai.mirror.data.protocol.MirrorProtocol

class NsdDiscoveryHelper(
    private val context: Context,
    private val deviceId: String,
    private val deviceName: String,
    private val role: DeviceRole,
    private val streamPort: Int = MirrorProtocol.DEFAULT_STREAM_PORT,
    private val onDeviceDiscovered: (DiscoveredDevice) -> Unit
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var isRegistered = false
    private var isDiscovering = false

    @Synchronized
    fun start() {
        registerService()
        discoverServices()
    }

    @Synchronized
    fun stop() {
        unregisterService()
        stopDiscovery()
    }

    private fun registerService() {
        if (nsdManager == null || isRegistered) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "AIMirror-${role.name}-$deviceName-$deviceId"
            serviceType = MirrorProtocol.NSD_SERVICE_TYPE
            port = streamPort
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo?) {
                isRegistered = true
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                isRegistered = false
            }

            override fun onServiceUnregistered(arg0: NsdServiceInfo?) {
                isRegistered = false
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                isRegistered = false
            }
        }

        try {
            nsdManager.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterService() {
        if (nsdManager != null && isRegistered && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isRegistered = false
                registrationListener = null
            }
        }
    }

    private fun discoverServices() {
        if (nsdManager == null || isDiscovering) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String?) {
                isDiscovering = true
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return
                if (serviceInfo.serviceType != MirrorProtocol.NSD_SERVICE_TYPE &&
                    !serviceInfo.serviceType.contains("aimirror")
                ) return

                try {
                    nsdManager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                            }

                            override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                                if (resolvedInfo == null) return
                                val host = resolvedInfo.host?.hostAddress ?: return
                                val name = resolvedInfo.serviceName ?: return
                                val parsedRole = if (name.contains("SENDER")) {
                                    DeviceRole.SENDER
                                } else {
                                    DeviceRole.RECEIVER
                                }

                                val device = DiscoveredDevice(
                                    id = name,
                                    name = name,
                                    ip = host,
                                    port = resolvedInfo.port,
                                    role = parsedRole,
                                    lastSeenTimestamp = System.currentTimeMillis()
                                )
                                onDeviceDiscovered(device)
                            }
                        }
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onServiceLost(service: NsdServiceInfo?) {
            }

            override fun onDiscoveryStopped(serviceType: String?) {
                isDiscovering = false
            }

            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                isDiscovering = false
            }
        }

        try {
            nsdManager.discoverServices(
                MirrorProtocol.NSD_SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopDiscovery() {
        if (nsdManager != null && isDiscovering && discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isDiscovering = false
                discoveryListener = null
            }
        }
    }
}
