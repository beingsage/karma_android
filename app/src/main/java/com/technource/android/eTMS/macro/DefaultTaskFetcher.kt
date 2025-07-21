package com.technource.android.eTMS.macro

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.google.gson.Gson
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.local.AppDatabase
import com.technource.android.local.toTaskEntity
import com.technource.android.local.Task
import com.technource.android.local.TaskData
import com.technource.android.local.TaskResponse
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class DefaultTaskFetcher(
    private val database: AppDatabase,
    private val apiService: ApiService,
    private val gson: Gson
) {

    // Fetch current default timetable from local storage
    suspend fun getCurrentDefaultTasks(): List<Task>? {
        return withContext(Dispatchers.IO) {
            val entity = database.defaultTaskDao().getDefaultTask()
            entity?.toTasks(gson)?.also {
                SystemStatus.logEvent("DefaultTaskFetcher", "Fetched ${it.size} default tasks from local storage")
//                webView.evaluateJavascript(
//                    "logEvent('DefaultTaskFetcher', 'Fetched ${it.size} default tasks from local storage', 'SUCCESS', '${DateFormatter.formatIsoDateTime(Date())}', ${gson.toJson(mapOf("taskCount" to it.size))})",
//                    null
//                )
            }
        }
    }

    // Fetch new default TT from backend
    suspend fun fetchNewDefaultTasks(
        onTasksFetched: (List<Task>) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val response = withContext(Dispatchers.IO) {
                apiService.getDefaultTasks() // Assumes endpoint like GET /default-timetable
            }
            if (response.success) {
                onTasksFetched(response.data.tasks)
                SystemStatus.logEvent("DefaultTaskFetcher", "Fetched ${response.data.tasks.size} new default tasks from backend")
                DashboardActivity.logEventToWebView(
                    "DefaultTaskFetcher",
                    "Fetched ${response.data.tasks.size} default tasks from Backend",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("taskCount" to response.data.tasks.size)
                )
            } else {
                onError("Failed to fetch new default timetable")
                SystemStatus.logEvent("DefaultTaskFetcher", "Backend response unsuccessful")
                DashboardActivity.logEventToWebView(
                    "DefaultTaskFetcher",
                    "Backend response unsuccessful",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("error" to "Backend response unsuccessful")
                )
            }
        } catch (e: Exception) {
            onError("Backend error: ${e.message}")
            SystemStatus.logEvent("DefaultTaskFetcher", "Backend error: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "DefaultTaskFetcher",
                    "Backend error: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
        }
    }

    // Store default timetable permanently
    suspend fun storeDefaultTasks(tasks: List<Task>) {
        val taskData = TaskData(
            id = "default_id_${System.currentTimeMillis()}",
            date = SimpleDateFormat("yyyy-MM-dd").format(Date()),
            temperature = 0.0f,
            tasks = tasks,
            dayScore = 0f,
            version = 0
        )
        val jsonData = gson.toJson(TaskResponse(success = true, data = taskData))
        withContext(Dispatchers.IO) {
            database.defaultTaskDao().insert(DefaultTaskEntity(jsonData = jsonData))
            SystemStatus.logEvent("DefaultTaskFetcher", "Stored ${tasks.size} default tasks locally")
            DashboardActivity.logEventToWebView(
                "DefaultTaskFetcher",
                "Stored ${tasks.size} default tasks locally",
                "SUCCESS",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskCount" to tasks.size)
            )
        }

    }

    // Rollback to default timetable
    suspend fun rollbackToDefaultTasks(): List<Task>? {
        val defaultTasks = getCurrentDefaultTasks()
        var tasksWithDate: List<Task>? = null
        if (defaultTasks != null) {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val isoDate = today // e.g., "2025-07-03"
            tasksWithDate = defaultTasks.map { task ->
                val startTime = if (task.startTime.length == 5 && task.startTime[2] == ':') {
                    // "HH:mm" format, convert to ISO 8601
                    "${isoDate}T${task.startTime}:00.000Z"
                } else task.startTime
                val endTime = if (task.endTime.length == 5 && task.endTime[2] == ':') {
                    "${isoDate}T${task.endTime}:00.000Z"
                } else task.endTime
                task.copy(
                    startTime = startTime,
                    endTime = endTime
                )
            }

            val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            val taskEntities = tasksWithDate.map { it.toTaskEntity(expirationTime) }
            withContext(Dispatchers.IO) {
                database.taskDao().clearTasks()
                database.taskDao().insertTasks(taskEntities)
                SystemStatus.logEvent("DefaultTaskFetcher", "Rolled back to ${defaultTasks.size} default tasks")
                DashboardActivity.logEventToWebView(
                    "DefaultTaskFetcher",
                    "Rolled back to ${defaultTasks.size} default tasks",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("taskCount" to defaultTasks.size)
                )
            }
        } else {
            SystemStatus.logEvent("DefaultTaskFetcher", "No default tasks available for rollback")
            DashboardActivity.logEventToWebView(
                "DefaultTaskFetcher",
                "No default tasks available for rollback",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
                mapOf("error" to "No default tasks")
            )
        }
        return tasksWithDate
    }
}


// Entity for default TT
@Entity(tableName = "default_task_entity")
data class DefaultTaskEntity(
    val jsonData: String
) {
    @androidx.room.PrimaryKey
    var id: Int = 1 // Single row, always overwrite
}

// DAO
fun DefaultTaskEntity.toTasks(gson: Gson): List<Task> {
    return gson.fromJson(jsonData, TaskResponse::class.java).data.tasks
}

@Dao
interface DefaultTaskDao {
    @Query("SELECT * FROM default_task_entity LIMIT 1")
    suspend fun getDefaultTask(): DefaultTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DefaultTaskEntity)
}