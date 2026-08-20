package com.ai.mirror.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.ai.mirror.data.model.AppSettings
import com.ai.mirror.data.model.CompressionQuality
import com.ai.mirror.data.model.DeviceRole
import com.ai.mirror.data.model.FpsPreset
import com.ai.mirror.data.model.ResolutionPreset
import com.ai.mirror.data.protocol.MirrorProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_mirror_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = UUID.randomUUID().toString().substring(0, 8)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    val deviceName: String
        get() {
            val savedName = prefs.getString(KEY_DEVICE_NAME, null)
            if (!savedName.isNullOrBlank()) {
                return savedName
            }
            val defaultName = "${Build.BRAND.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            return if (defaultName.isBlank()) "Android-$deviceId" else defaultName
        }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit().apply {
            putString(KEY_LANGUAGE, newSettings.language)
            putString(KEY_DEVICE_NAME, newSettings.deviceName)
            putString(KEY_ROLE, newSettings.preferredRole.name)
            putString(KEY_RESOLUTION, newSettings.resolution.name)
            putInt(KEY_FPS, newSettings.fps.fps)
            putInt(KEY_QUALITY, newSettings.quality.qualityPercent)
            putInt(KEY_PORT, newSettings.serverPort)
            putBoolean(KEY_AUTO_ACCEPT, newSettings.autoAcceptPairing)
            putBoolean(KEY_KEEP_SCREEN_ON, newSettings.keepScreenOn)
            putBoolean(KEY_MIRROR_FLIP, newSettings.mirrorFlipHorizontal)
            apply()
        }
        _settings.value = newSettings
    }

    private fun loadSettings(): AppSettings {
        val lang = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
        val roleStr = prefs.getString(KEY_ROLE, DeviceRole.SENDER.name) ?: DeviceRole.SENDER.name
        val role = try { DeviceRole.valueOf(roleStr) } catch (e: Exception) { DeviceRole.SENDER }
        val resStr = prefs.getString(KEY_RESOLUTION, ResolutionPreset.HD_720P.name) ?: ResolutionPreset.HD_720P.name
        val res = try { ResolutionPreset.valueOf(resStr) } catch (e: Exception) { ResolutionPreset.HD_720P }
        val fpsVal = prefs.getInt(KEY_FPS, 30)
        val qualityVal = prefs.getInt(KEY_QUALITY, 80)
        val port = prefs.getInt(KEY_PORT, MirrorProtocol.DEFAULT_STREAM_PORT)
        val autoAccept = prefs.getBoolean(KEY_AUTO_ACCEPT, true)
        val keepScreen = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val mirrorFlip = prefs.getBoolean(KEY_MIRROR_FLIP, true)

        return AppSettings(
            language = lang,
            deviceName = deviceName,
            preferredRole = role,
            resolution = res,
            fps = FpsPreset.fromValue(fpsVal),
            quality = CompressionQuality.fromValue(qualityVal),
            serverPort = port,
            autoAcceptPairing = autoAccept,
            keepScreenOn = keepScreen,
            mirrorFlipHorizontal = mirrorFlip
        )
    }

    companion object {
        private const val KEY_DEVICE_ID = "pref_device_id"
        private const val KEY_DEVICE_NAME = "pref_device_name"
        private const val KEY_LANGUAGE = "pref_language"
        private const val KEY_ROLE = "pref_role"
        private const val KEY_RESOLUTION = "pref_resolution"
        private const val KEY_FPS = "pref_fps"
        private const val KEY_QUALITY = "pref_quality"
        private const val KEY_PORT = "pref_port"
        private const val KEY_AUTO_ACCEPT = "pref_auto_accept"
        private const val KEY_KEEP_SCREEN_ON = "pref_keep_screen_on"
        private const val KEY_MIRROR_FLIP = "pref_mirror_flip"
    }
}
