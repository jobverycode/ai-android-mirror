package com.ai.mirror.data.model

data class AppSettings(
    val language: String = "system", // "system", "zh", "en"
    val deviceName: String = "",
    val preferredRole: DeviceRole = DeviceRole.SENDER,
    val resolution: ResolutionPreset = ResolutionPreset.HD_720P,
    val fps: FpsPreset = FpsPreset.FPS_30,
    val quality: CompressionQuality = CompressionQuality.MEDIUM,
    val serverPort: Int = 8888,
    val autoAcceptPairing: Boolean = true,
    val keepScreenOn: Boolean = true,
    val mirrorFlipHorizontal: Boolean = true
)
