//package com.technource.android.module.settingsModule
//
//import com.technource.android.local.toTaskEntity
//import com.technource.android.network.ApiService
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.technource.android.local.TaskDao
//import com.technource.android.local.toTaskResponse
//import com.technource.android.utils.PreferencesManager
////import com.technource.android.utils.SystemLogger
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.SharingStarted
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.stateIn
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class SettingsViewModel @Inject constructor(
//    private val dao: TaskDao,
//    private val taskApi: ApiService,
//    private val preferencesManager: PreferencesManager
//) : ViewModel() {
//
//    val isTtsEnabled: Flow<Boolean> = preferencesManager.isTtsEnabled()
//    val isLockScreenEnabled: Flow<Boolean> = preferencesManager.isLockScreenEnabled()
//    val isAlarmScreenEnabled: Flow<Boolean> = preferencesManager.isAlarmScreenEnabled()
//    val isRefreshEnabled: Flow<Boolean> = preferencesManager.isRefreshEnabled()
//    val isAlarmVibrationEnabled: StateFlow<Boolean> = preferencesManager.isRefreshEnabled().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
//
//    fun setTtsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setTtsEnabled(enabled) }
//    fun setLockScreenEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setLockScreenEnabled(enabled) }
//    fun setAlarmScreenEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setAlarmScreenEnabled(enabled) }
//    fun setRefreshEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setRefreshEnabled(enabled) }
//    fun setAlarmVibrationEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setAlarmVibrationEnabled(enabled) }
//
//    fun terminateDay(onServiceStop: () -> Unit) {
//        viewModelScope.launch {
//            val entity = dao.getTask() ?: return@launch
//            val response = entity.toTaskResponse()
//            val updatedTasks = response.data.tasks.map { task ->
//                task.copy(
//                    completionStatus = if (task.completionStatus == 0f) -1f else task.completionStatus,
//                    efficiencyDone = 0
//                )
//            }
//            val updatedResponse = response.copy(data = response.data.copy(tasks = updatedTasks))
//            dao.insert(updatedResponse.toTaskEntity(entity.timestamp))
////            SystemLogger.log("Day terminated, resetting tasks")
//            onServiceStop()
//            try {
////                taskApi.syncData(updatedResponse)    // THIS WAS USED HERE IN CASE WHEN I HAVE TO TERMINATE THE TIME TABLE THEN IT WILL SEND THE DATA TO BACKEND AS IT IS
////                SystemLogger.log("Day terminated, data synced to backend")
//            } catch (e: Exception) {
////                SystemLogger.log("Failed to sync data: ${e.message}")
//            }
//        }
//    }
//
//    fun syncData() {
//        viewModelScope.launch {
//            val entity = dao.getTask() ?: return@launch
//            val localData = entity.toTaskResponse()
//            try {
////                taskApi.syncData(localData)
////                SystemLogger.log("Data synced to backend manually")
//            } catch (e: Exception) {
////                SystemLogger.log("Sync failed: ${e.message}")
//            }
//        }
//    }
//}


package com.technource.android.module.settingsModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technource.android.local.TaskDao
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.network.ApiService
import com.technource.android.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dao: TaskDao,
    private val taskApi: ApiService,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val isTtsEnabled: Flow<Boolean> = preferencesManager.isTtsEnabled()
    val isLockScreenEnabled: Flow<Boolean> = preferencesManager.isLockScreenEnabled()
    val isAlarmScreenEnabled: Flow<Boolean> = preferencesManager.isAlarmScreenEnabled()
    val isRefreshEnabled: Flow<Boolean> = preferencesManager.isRefreshEnabled()
    val isAlarmVibrationEnabled: StateFlow<Boolean> = preferencesManager.isRefreshEnabled().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun setTtsEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setTtsEnabled(enabled) }
    fun setLockScreenEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setLockScreenEnabled(enabled) }
    fun setAlarmScreenEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setAlarmScreenEnabled(enabled) }
    fun setRefreshEnabled(enabled: Boolean) = viewModelScope.launch { preferencesManager.setRefreshEnabled(enabled) }
    fun setAlarmVibrationEnabled(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAlarmVibrationEnabled(enabled)
    }

    fun terminateDay(onServiceStop: () -> Unit) {
        viewModelScope.launch {
            val entities = dao.getTasks()
            val tasks = entities.toTasks()
            val updatedTasks = tasks.map { task ->
                task.copy(
                    completionStatus = if (task.completionStatus == 0f) -1f else task.completionStatus
                )
            }
            val updatedEntities = updatedTasks.map { it.toTaskEntity(entities.find { e -> e.id == it.id }?.timestamp ?: System.currentTimeMillis()) }
            dao.clearTasks()
            dao.insertTasks(updatedEntities)
            onServiceStop()
            try {
                // taskApi.syncData(updatedTasks) // Uncomment when API is ready
                // SystemLogger.log("Day terminated, data synced to backend")
            } catch (e: Exception) {
                // SystemLogger.log("Failed to sync data: ${e.message}")
            }
        }
    }

    fun syncData() {
        viewModelScope.launch {
            val entities = dao.getTasks()
            val tasks = entities.toTasks()
            try {
                // taskApi.syncData(tasks) // Uncomment when API is ready
                // SystemLogger.log("Data synced to backend manually")
            } catch (e: Exception) {
                // SystemLogger.log("Sync failed: ${e.message}")
            }
        }
    }
}