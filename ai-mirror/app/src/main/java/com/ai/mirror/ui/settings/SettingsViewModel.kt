package com.ai.mirror.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ai.mirror.AiMirrorApplication
import com.ai.mirror.data.model.AppSettings
import com.ai.mirror.data.model.CompressionQuality
import com.ai.mirror.data.model.FpsPreset
import com.ai.mirror.data.model.ResolutionPreset
import com.ai.mirror.data.repository.SettingsRepository
import com.ai.mirror.utils.LocaleHelper
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository: SettingsRepository =
        (application as AiMirrorApplication).settingsRepository

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    fun setLanguage(language: String) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(language = language))
        LocaleHelper.applyLanguage(language)
    }

    fun setDeviceName(name: String) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(deviceName = name))
    }

    fun setResolution(resolution: ResolutionPreset) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(resolution = resolution))
    }

    fun setFps(fps: FpsPreset) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(fps = fps))
    }

    fun setQuality(quality: CompressionQuality) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(quality = quality))
    }

    fun setServerPort(port: Int) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(serverPort = port))
    }

    fun setAutoAcceptPairing(enabled: Boolean) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(autoAcceptPairing = enabled))
    }

    fun setKeepScreenOn(enabled: Boolean) {
        val current = settings.value
        settingsRepository.updateSettings(current.copy(keepScreenOn = enabled))
    }
}
