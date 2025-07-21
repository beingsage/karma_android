package com.technource.android.eTMS.macro

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.technource.android.system_status.SystemStatus
import kotlinx.coroutines.flow.first

// Data Model for ServiceState
data class ServiceState(
    val lastSynced: Long,
    val lastTaskCompleted: String?,
    val pendingLogs: List<String>,
    val timetableJson: String? = null,
    val lastHandledTaskId: String? = null,
    val taskStatusMap: Map<String, String>? = null,
    val lastSyncedDate: String? = null // NEW: Date of last successful sync
)

// Persistence Store
object PersistentStore {
    private val Context.dataStore by preferencesDataStore("service_state")
    private const val PREFS_NAME = "etms_fallback_prefs"
    private const val BACKUP_FILE = "etms_state_backup.json"
    private const val WEEKLY_BACKUP_INTERVAL = 7 * 24 * 60 * 60 * 1000L // 7 days

    suspend fun saveState(context: Context, state: ServiceState) {
        try {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("state")] = Gson().toJson(state)
            }
            // Save to SharedPreferences as fallback
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString("state", Gson().toJson(state))
                .apply()
            // Backup to file periodically
            if (shouldBackup(context)) {
                saveBackupToFile(context, state)
            }
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to save state: ${e.message}")
            // Fallback to SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString("state", Gson().toJson(state))
                .apply()
        }
    }

    suspend fun loadState(context: Context): ServiceState? {
        return try {
            val json = context.dataStore.data.first()[stringPreferencesKey("state")]
            json?.let { Gson().fromJson(it, ServiceState::class.java) }
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to load state: ${e.message}")
            context.dataStore.edit { prefs -> prefs.clear() }
            // Try SharedPreferences fallback
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString("state", null)
            if (json != null) {
                SystemStatus.logEvent("PersistentStore", "Loaded state from SharedPreferences fallback")
                return Gson().fromJson(json, ServiceState::class.java)
            }
            // Try file backup
            return loadBackupFromFile(context)
        }
    }

    suspend fun saveHeartbeat(context: Context) {
        try {
            context.dataStore.edit { prefs ->
                prefs[stringPreferencesKey("heartbeat")] = System.currentTimeMillis().toString()
            }
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to save heartbeat: ${e.message}")
            // Fallback to SharedPreferences
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putString("heartbeat", System.currentTimeMillis().toString())
                .apply()
        }
    }

    suspend fun checkHeartbeat(context: Context): Boolean {
        return try {
            val lastHeartbeat = context.dataStore.data.first()[stringPreferencesKey("heartbeat")]?.toLongOrNull()
            lastHeartbeat != null && System.currentTimeMillis() - lastHeartbeat < 30_000
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to check heartbeat: ${e.message}")
            // Check SharedPreferences fallback
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val lastHeartbeat = prefs.getString("heartbeat", null)?.toLongOrNull()
            lastHeartbeat != null && System.currentTimeMillis() - lastHeartbeat < 30_000
        }
    }

    private fun shouldBackup(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastBackup = prefs.getLong("last_backup", 0)
        return System.currentTimeMillis() - lastBackup >= WEEKLY_BACKUP_INTERVAL
    }

    private fun saveBackupToFile(context: Context, state: ServiceState) {
        try {
            val json = Gson().toJson(state)
            context.openFileOutput(BACKUP_FILE, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putLong("last_backup", System.currentTimeMillis())
                .apply()
            SystemStatus.logEvent("PersistentStore", "Backed up state to file")
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to save state backup: ${e.message}")
        }
    }

    private fun loadBackupFromFile(context: Context): ServiceState? {
        return try {
            val json = context.openFileInput(BACKUP_FILE).bufferedReader().use { it.readText() }
            Gson().fromJson(json, ServiceState::class.java).also {
                SystemStatus.logEvent("PersistentStore", "Loaded state from file backup")
            }
        }
        catch (e: Exception) {
            SystemStatus.logEvent("PersistentStore", "Failed to load state backup: ${e.message}")
            null
        }
    }
}