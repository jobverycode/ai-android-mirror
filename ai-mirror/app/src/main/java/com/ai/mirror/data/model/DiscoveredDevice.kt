package com.ai.mirror.data.model

data class DiscoveredDevice(
    val id: String,
    val name: String,
    val ip: String,
    val port: Int,
    val role: DeviceRole,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
) {
    val endpoint: String
        get() = "$ip:$port"

    val isSender: Boolean
        get() = role == DeviceRole.SENDER

    val isReceiver: Boolean
        get() = role == DeviceRole.RECEIVER
}
