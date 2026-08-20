package com.ai.mirror.data.model

enum class ResolutionPreset(
    val width: Int,
    val height: Int,
    val labelKey: String
) {
    SD_480P(640, 480, "resolution_sd"),
    HD_720P(1280, 720, "resolution_hd"),
    FHD_1080P(1920, 1080, "resolution_fhd");

    companion object {
        fun fromDimensions(w: Int, h: Int): ResolutionPreset {
            return entries.firstOrNull { it.width == w && it.height == h } ?: HD_720P
        }
    }
}

enum class FpsPreset(val fps: Int) {
    FPS_15(15),
    FPS_24(24),
    FPS_30(30),
    FPS_60(60);

    companion object {
        fun fromValue(value: Int): FpsPreset {
            return entries.firstOrNull { it.fps == value } ?: FPS_30
        }
    }
}

enum class CompressionQuality(val qualityPercent: Int) {
    LOW(60),
    MEDIUM(80),
    HIGH(95);

    companion object {
        fun fromValue(value: Int): CompressionQuality {
            return entries.firstOrNull { it.qualityPercent == value } ?: MEDIUM
        }
    }
}

data class StreamConfig(
    val resolution: ResolutionPreset = ResolutionPreset.HD_720P,
    val fps: FpsPreset = FpsPreset.FPS_30,
    val quality: CompressionQuality = CompressionQuality.MEDIUM
)
