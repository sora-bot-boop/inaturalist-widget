package com.naturewidget.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

class SettingsManager private constructor(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nature_widget_prefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_hours"
        private const val KEY_LOCALE = "locale"
        private const val KEY_MODE = "widget_mode"
        private const val DEFAULT_REFRESH_INTERVAL = 4 // hours
        
        // Widget modes
        enum class WidgetMode(val displayName: String, val description: String) {
            PERSONAL("Personal", "Your own observations"),
            ALL("All", "Random from everyone"),
            DISCOVER("Discover", "Recent observations near you")
        }
        
        // Supported languages for iNaturalist common names
        val SUPPORTED_LOCALES = listOf(
            "auto" to "Auto (Device Language)",
            "en" to "English",
            "de" to "Deutsch",
            "fr" to "Français",
            "es" to "Español",
            "it" to "Italiano",
            "pt" to "Português",
            "nl" to "Nederlands",
            "pl" to "Polski",
            "ru" to "Русский",
            "ja" to "日本語",
            "zh" to "中文"
        )
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun getUserLogin(): String {
        return prefs.getString(KEY_USER_LOGIN, "") ?: ""
    }
    
    fun setUserLogin(username: String) {
        prefs.edit().putString(KEY_USER_LOGIN, username.trim()).apply()
    }
    
    fun getRefreshInterval(): Int {
        return prefs.getInt(KEY_REFRESH_INTERVAL, DEFAULT_REFRESH_INTERVAL)
    }
    
    fun setRefreshInterval(hours: Int) {
        prefs.edit().putInt(KEY_REFRESH_INTERVAL, hours.coerceIn(1, 24)).apply()
    }
    
    fun getLocaleSetting(): String {
        return prefs.getString(KEY_LOCALE, "auto") ?: "auto"
    }
    
    fun setLocale(locale: String) {
        prefs.edit().putString(KEY_LOCALE, locale).apply()
    }
    
    /**
     * Returns the actual locale code to use for API calls.
     * If set to "auto", returns the device's language code.
     */
    fun getEffectiveLocale(): String {
        val setting = getLocaleSetting()
        return if (setting == "auto") {
            Locale.getDefault().language
        } else {
            setting
        }
    }
    
    fun getWidgetMode(): WidgetMode {
        val modeName = prefs.getString(KEY_MODE, WidgetMode.ALL.name) ?: WidgetMode.ALL.name
        return try {
            WidgetMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            WidgetMode.ALL
        }
    }
    
    fun setWidgetMode(mode: WidgetMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }
}
