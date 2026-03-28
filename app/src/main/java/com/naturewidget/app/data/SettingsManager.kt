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
}
