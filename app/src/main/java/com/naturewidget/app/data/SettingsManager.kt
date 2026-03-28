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

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        private val USER_LOGIN = stringPreferencesKey("user_login")
    }
    
    val userLoginFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_LOGIN] ?: ""
        }
    
    suspend fun getUserLogin(): String {
        return userLoginFlow.first()
    }
    
    suspend fun setUserLogin(username: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_LOGIN] = username.trim()
        }
    }
}
