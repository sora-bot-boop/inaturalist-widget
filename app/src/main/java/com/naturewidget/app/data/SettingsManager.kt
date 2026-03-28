package com.naturewidget.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore should be a singleton - define at top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nature_widget_settings")

class SettingsManager private constructor(private val context: Context) {
    
    companion object {
        private val USER_LOGIN = stringPreferencesKey("user_login")
        
        @Volatile
        private var INSTANCE: SettingsManager? = null
        
        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    val userLoginFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_LOGIN] ?: ""
        }
    
    suspend fun getUserLogin(): String {
        return try {
            userLoginFlow.first()
        } catch (e: Exception) {
            ""
        }
    }
    
    suspend fun setUserLogin(username: String) {
        try {
            context.dataStore.edit { preferences ->
                preferences[USER_LOGIN] = username.trim()
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }
}
