package com.ai.mirror.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun applyLanguage(languageCode: String) {
        if (languageCode.equals("system", ignoreCase = true)) {
            return
        }

        val targetLocale = when (languageCode.lowercase()) {
            "zh", "zh_cn", "zh-cn" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }

        Locale.setDefault(targetLocale)
    }

    fun wrapContext(context: Context, languageCode: String): Context {
        if (languageCode.equals("system", ignoreCase = true)) {
            return context
        }

        val targetLocale = when (languageCode.lowercase()) {
            "zh", "zh_cn", "zh-cn" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }

        Locale.setDefault(targetLocale)
        val config = Configuration(context.resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(targetLocale))
            return context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            config.locale = targetLocale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            return context
        }
    }
}
