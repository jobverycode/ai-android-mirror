package com.ai.mirror.data.model

enum class DeviceRole {
    SENDER,
    RECEIVER;

    val isSender: Boolean
        get() = this == SENDER

    val isReceiver: Boolean
        get() = this == RECEIVER
}
