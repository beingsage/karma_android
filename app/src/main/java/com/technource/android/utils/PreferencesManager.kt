package com.technource.android.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@SuppressLint("StaticFieldLeak")
object PreferencesManager {
    private lateinit var context: Context

    fun init(context: Context) {
        if (!PreferencesManager::context.isInitialized) {
            PreferencesManager.context = context.applicationContext
        }
    }

    private val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
    private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val TASK_LOGGER_ENABLED = booleanPreferencesKey("task_logger_enabled")
    private val WIDGETS_ENABLED = booleanPreferencesKey("widgets_enabled")
    private val VOICE_COMMANDS_ENABLED = booleanPreferencesKey("voice_commands_enabled")
    private val LOCK_SCREEN_ENABLED = booleanPreferencesKey("lock_screen_enabled")
    private val ALARM_SCREEN_ENABLED = booleanPreferencesKey("alarm_screen_enabled")
    private val REFRESH_ENABLED = booleanPreferencesKey("refresh_enabled")
    private val ALARM_VIBRATION_ENABLED = booleanPreferencesKey("alarm_vibration_enabled")

    fun isTtsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[TTS_ENABLED] ?: true }
    fun isNotificationsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    fun isVibrationEnabled(): Flow<Boolean> = context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }
    fun isTaskLoggerEnabled(): Flow<Boolean> = context.dataStore.data.map { it[TASK_LOGGER_ENABLED] ?: true }
    fun isWidgetsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[WIDGETS_ENABLED] ?: true }
    fun isVoiceCommandsEnabled(): Flow<Boolean> = context.dataStore.data.map { it[VOICE_COMMANDS_ENABLED] ?: true }
    fun isLockScreenEnabled(): Flow<Boolean> = context.dataStore.data.map { it[LOCK_SCREEN_ENABLED] ?: true }
    fun isAlarmScreenEnabled(): Flow<Boolean> = context.dataStore.data.map { it[ALARM_SCREEN_ENABLED] ?: true }
    fun isRefreshEnabled(): Flow<Boolean> = context.dataStore.data.map { it[REFRESH_ENABLED] ?: true }
    fun isAlarmVibrationEnabled(): Flow<Boolean> = context.dataStore.data.map { it[ALARM_VIBRATION_ENABLED] ?: true }

    fun isVibrationEnabledSync(): Boolean = runBlocking { context.dataStore.data.map { it[VIBRATION_ENABLED] ?: true }.first() }
    fun isNotificationsEnabledSync(): Boolean = runBlocking { context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }.first() }
    fun isVoiceCommandsEnabledSync(): Boolean = runBlocking { context.dataStore.data.map { it[VOICE_COMMANDS_ENABLED] ?: true }.first() }
    fun isTaskLoggerEnabledSync(): Boolean = runBlocking { context.dataStore.data.map { it[TASK_LOGGER_ENABLED] ?: true }.first() }
    fun isWidgetsEnabledSync(): Boolean = runBlocking { context.dataStore.data.map { it[WIDGETS_ENABLED] ?: true }.first() }

    suspend fun setTtsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TTS_ENABLED] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    suspend fun setTaskLoggerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[TASK_LOGGER_ENABLED] = enabled }
    }

    suspend fun setWidgetsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[WIDGETS_ENABLED] = enabled }
    }

    suspend fun setVoiceCommandsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[VOICE_COMMANDS_ENABLED] = enabled }
    }

    suspend fun setLockScreenEnabled(enabled: Boolean) {
        context.dataStore.edit { it[LOCK_SCREEN_ENABLED] = enabled }
    }

    suspend fun setAlarmScreenEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ALARM_SCREEN_ENABLED] = enabled }
    }

    suspend fun setRefreshEnabled(enabled: Boolean) {
        context.dataStore.edit { it[REFRESH_ENABLED] = enabled }
    }

    suspend fun setAlarmVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ALARM_VIBRATION_ENABLED] = enabled }
    }

    private fun checkInitialized() {
        check(PreferencesManager::context.isInitialized) { "PreferencesManager must be initialized with a context first" }
    }
}