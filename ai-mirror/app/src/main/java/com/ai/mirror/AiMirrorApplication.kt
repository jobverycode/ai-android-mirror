package com.ai.mirror

import android.app.Application
import com.ai.mirror.data.repository.SettingsRepository
import com.ai.mirror.utils.LocaleHelper

class AiMirrorApplication : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository(this)

        val settings = settingsRepository.settings.value
        LocaleHelper.applyLanguage(settings.language)
    }

    companion object {
        lateinit var instance: AiMirrorApplication
            private set
    }
}
