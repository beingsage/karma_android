package com.technource.android.ETMS.macro

import com.google.gson.Gson
import com.technource.android.local.AppDatabase
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.local.Task
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TaskFetcher  @Inject constructor(
    private val database: AppDatabase,
    private val apiService: ApiService,
    private val gson: Gson
) {

    suspend fun fetchTasks(
        onTasksFetched: (List<Task>) -> Unit,
        onError: (String) -> Unit,
        onLoading: (Boolean) -> Unit,
        loadSampleData: () -> Unit,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // Default to current date
    ) {
        onLoading(true)
        onError(null.toString())
        try {
            // Check local database first
            val localTaskEntity = withContext(Dispatchers.IO) {
                database.taskDao().getTasks()
            }
            if (localTaskEntity.isNotEmpty() && localTaskEntity.any { it.timestamp > System.currentTimeMillis() }) {
                // Use valid local data
                onTasksFetched(localTaskEntity.toTasks())
                SystemStatus.logEvent("TaskFetcher", "Fetched ${localTaskEntity.size} tasks from local storage")
            } else {
                fetchFromBackend(onTasksFetched, onError, loadSampleData, date)
            }
        } catch (e: Exception) {
            onError("Error fetching local tasks: ${e.message}")
            SystemStatus.logEvent("TaskFetcher", "Local fetch error: ${e.message}")
            loadSampleData()
        } finally {
            onLoading(false)
        }
    }

    private suspend fun fetchFromBackend(
        onTasksFetched: (List<Task>) -> Unit,
        onError: (String) -> Unit,
        loadSampleData: () -> Unit,
        date: String // Add date parameter
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                apiService.getTasks(date)
            }

            if (response.success) {
                val tasks = response.data.tasks
                val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000 // 24 hours
                val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }

                withContext(Dispatchers.IO) {
                    database.taskDao().clearTasks()
                    database.taskDao().insertTasks(taskEntities)
                }
                onTasksFetched(tasks)
                SystemStatus.logEvent("TaskFetcher", "Fetched ${tasks.size} tasks from backend")
            } else {
                onError("Backend fetch failed")
                SystemStatus.logEvent("TaskFetcher", "Backend fetch failed")
                loadSampleData()
            }
        } catch (e: Exception) {
            onError("Backend error: ${e.message}")
            SystemStatus.logEvent("TaskFetcher", "Backend error: ${e.message}")
            loadSampleData()
        }
    }
}