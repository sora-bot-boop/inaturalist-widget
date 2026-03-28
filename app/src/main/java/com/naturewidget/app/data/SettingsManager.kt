package com.naturewidget.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "nature_widget_prefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_USER_LOGIN = "user_login"
        private const val KEY_REFRESH_INTERVAL = "refresh_interval_hours"
        private const val DEFAULT_REFRESH_INTERVAL = 4 // hours
        
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
}
