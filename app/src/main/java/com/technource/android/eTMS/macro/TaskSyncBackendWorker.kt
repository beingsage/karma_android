package com.technource.android.eTMS.macro

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.local.AppDatabase
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.local.TaskResponse
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class TaskSyncBackendWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase,
    private val apiService: ApiService
) : CoroutineWorker(appContext, params) {

    private val gson = Gson()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val defaultTaskFetcher = DefaultTaskFetcher(db, apiService, gson)
    private val timetableIdStoreFile = "etms_last_timetable_id.txt"
    private val lastTimetableFile = "etms_last_timetable.json"

    // Helper to save and load timetable _id
    private fun saveTimetableId(id: String) {
        try {
            appContext.openFileOutput(timetableIdStoreFile, Context.MODE_PRIVATE).use {
                it.write(id.toByteArray())
            }
            SystemStatus.logEvent("TaskSyncWorker", "Saved timetable _id: $id")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Saved timetable _id: $id",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("timetableId" to id)
            )
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to save timetable _id: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Failed to save timetable _id: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
        }
    }

    private fun loadTimetableId(): String? {
        return try {
            appContext.openFileInput(timetableIdStoreFile).bufferedReader().readText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    // Save the full timetable JSON (from backend) for later resend
    private fun saveLastTimetableJson(json: String) {
        try {
            appContext.openFileOutput(lastTimetableFile, Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
            SystemStatus.logEvent("TaskSyncWorker", "Saved last timetable JSON")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Saved last timetable JSON",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to save last timetable JSON: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Failed to save last timetable JSON: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
        }
    }

    // Load the last timetable JSON (for resend)
    private fun loadLastTimetableJson(): String? {
        return try {
            appContext.openFileInput(lastTimetableFile).bufferedReader().readText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, ETMSConfig.SYNC_HOUR)
                set(Calendar.MINUTE, ETMSConfig.SYNC_MINUTE)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val initialDelay = calendar.timeInMillis - System.currentTimeMillis()
            val workRequest = PeriodicWorkRequestBuilder<TaskSyncBackendWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()
            SystemStatus.logEvent("TaskSyncWorker", "Scheduled worker at ${calendar.time}")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Scheduled worker at ${calendar.time}",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("scheduleTime" to calendar.time.toString())
            )
            workManager.enqueueUniquePeriodicWork(
                ETMSConfig.SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            SystemStatus.logStatus("TaskSyncWorker", "Scheduled")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Scheduled",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            SystemStatus.logEvent("TaskSyncWorker", "Worker started at ${timestampFormat.format(Date())}")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Running",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
            SystemStatus.logStatus("TaskSyncWorker", "Running")
//            webView.evaluateJavascript(
//                "logEvent('TaskSyncWorker', 'Running', 'INFO', '${DateFormatter.formatIsoDateTime(Date())}')",
//                null
//            )

            performSyncTask()

            SystemStatus.logEvent("TaskSyncWorker", "Worker completed")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Worker completed",
                "SUCCESS",
                DateFormatter.formatDateTime(Date())
            )
            Result.success()
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Error occurred: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Error occurred: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
            defaultTaskFetcher.rollbackToDefaultTasks()
            Result.retry()
        }
    }

    private suspend fun performSyncTask() {
        // Fetch local tasks
        val oldTaskEntities = db.taskDao().getTasks()
        val oldTasks = oldTaskEntities.toTasks()

        // Try to load last timetable JSON
        val lastTimetableJson = loadLastTimetableJson()
        val oldTaskResponse: MutableMap<String, Any?>

        if (lastTimetableJson != null) {
            // Parse and update tasks field
            @Suppress("UNCHECKED_CAST")
            oldTaskResponse = gson.fromJson(lastTimetableJson, Map::class.java) as MutableMap<String, Any?>
            val data = oldTaskResponse["data"] as? MutableMap<String, Any?> ?: mutableMapOf()
            data["tasks"] = oldTasks
            oldTaskResponse["data"] = data
        } else {
            // Fallback: build minimal structure
            val timetableId = loadTimetableId() ?: "local_${System.currentTimeMillis()}"
            val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) + "T00:00:00.000Z"
            oldTaskResponse = mutableMapOf(
                "success" to true,
                "data" to mutableMapOf(
                    "_id" to timetableId,
                    "date" to todayIso,
                    "temperature" to 1,
                    "tasks" to oldTasks,
                    "__v" to 0,
                    "dayScore" to 0
                )
            )
        }

        if (oldTasks.isNotEmpty()) {
            SystemStatus.logEvent("TaskSyncWorker", "Sending old tasks to backend with full timetable metadata")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Sending old tasks to backend with full timetable metadata",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskCount" to oldTasks.size)
            )
            try {
                // Convert map to JSON, then to TaskResponse
                val oldTaskResponseJson = gson.toJson(oldTaskResponse)
                val oldTaskResponseObj = gson.fromJson(oldTaskResponseJson, TaskResponse::class.java)
//                try {
//    val response = apiService.sendTasks(oldTaskResponseObj)
//    if (response.isSuccessful && response.body()?.success == true) {
//        SystemStatus.logEvent("EternalTimeTableUnitService", "Successfully sent tasks to backend")
//        // Proceed with clearing tasks, updating state, etc.
//        database.taskDao().clearTasks()
//        success = true
//        return@repeat
//    } else {
//        SystemStatus.logEvent("EternalTimeTableUnitService", "Backend did not confirm success: ${response.body()?.message}")
//        sendNotification("Backend Sync Failed", "Backend did not confirm success. Retrying...")
//        delay(ETMSConfig.RETRY_DELAY_MS)
//    }
//} catch (e: Exception) {
//    SystemStatus.logEvent("EternalTimeTableUnitService", "Send attempt ${attempt + 1} failed: ${e.message}")
//    sendNotification("Backend Sync Failed", "Failed to send timetable. Retrying...")
//    delay(ETMSConfig.RETRY_DELAY_MS)
//}
                SystemStatus.logEvent("TaskSyncWorker", "Successfully sent old tasks to backend")
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Successfully sent old tasks to backend",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskSyncWorker", "Failed to send old tasks to backend: ${e.message}")
                mapOf("error" to e.message)?.let {
                    DashboardActivity.logEventToWebView(
                        "TaskSyncWorker",
                        "Failed to send old tasks to backend: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
//                        it
                    )
                }
                throw e
            }
        }

        // Fetch new tasks from backend
        SystemStatus.logEvent("TaskSyncWorker", "Fetching new tasks from backend")
        DashboardActivity.logEventToWebView(
            "TaskSyncWorker",
            "Fetching new tasks from backend",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
        val newTaskResponse = try {
            apiService.getTasks(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to fetch new tasks from backend: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Failed to fetch new tasks from backend: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
            throw e
        }
        SystemStatus.logEvent("TaskSyncWorker", "Successfully fetched ${newTaskResponse.data.tasks.size} new tasks from backend")
        DashboardActivity.logEventToWebView(
            "TaskSyncWorker",
            "Successfully fetched ${newTaskResponse.data.tasks.size} new tasks from backend",
            "SUCCESS",
            DateFormatter.formatDateTime(Date()),
            mapOf("taskCount" to newTaskResponse.data.tasks.size)
        )

        // Before sending timetable in ETMS or TaskSyncBackendWorker:
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastState = PersistentStore.loadState(appContext)
        if (lastState?.lastSyncedDate == today) {
            SystemStatus.logEvent("ETMS", "Timetable already synced for $today, skipping send.")
            DashboardActivity.logEventToWebView(
                "ETMS",
                "Timetable already synced for $today, skipping send.",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("date" to today)
            )
            return // skip sending
        }

        // Save the new timetable _id for next sync
        val newTimetableId = newTaskResponse.data.id
        saveTimetableId(newTimetableId)

        // Save the full new timetable JSON for next resend
        saveLastTimetableJson(gson.toJson(newTaskResponse))

        // Store new tasks locally
        SystemStatus.logEvent("TaskSyncWorker", "Storing new tasks in local Room")
//        webView.evaluateJavascript(
//            "logEvent('TaskSyncWorker', 'Storing new tasks in local Room', 'INFO', '${DateFormatter.formatIsoDateTime(Date())}')",
//            null
//        )
        val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        val newTaskEntities = newTaskResponse.data.tasks.map { it.toTaskEntity(expirationTime) }
        try {
            db.taskDao().clearTasks()
            db.taskDao().insertTasks(newTaskEntities)
            SystemStatus.logEvent("TaskSyncWorker", "Stored ${newTaskEntities.size} new tasks locally")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Stored ${newTaskEntities.size} new tasks locally",
                "SUCCESS",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskCount" to newTaskEntities.size)
            )
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to store new tasks locally: ${e.message}")
//            webView.evaluateJavascript(
//                "logEvent('TaskSyncWorker', 'Failed to store new tasks locally: ${e.message}', 'ERROR', '${DateFormatter.formatIsoDateTime(Date())}', ${gson.toJson(mapOf("error" to e.message))})",
//                null
//            )
            throw e
        }

        // Clean up expired tasks
        SystemStatus.logEvent("TaskSyncWorker", "Cleaning up expired tasks from local Room")
        DashboardActivity.logEventToWebView(
            "TaskSyncWorker",
            "Cleaning up expired tasks from local Room",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
        try {
            db.taskDao().deleteExpired(System.currentTimeMillis())
            SystemStatus.logEvent("TaskSyncWorker", "Successfully deleted expired tasks")
            DashboardActivity.logEventToWebView(
                "TaskSyncWorker",
                "Successfully deleted expired tasks",
                "SUCCESS",
                DateFormatter.formatDateTime(Date())
            )
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to delete expired tasks: ${e.message}")
            mapOf("error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskSyncWorker",
                    "Failed to delete expired tasks: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
            throw e
        }

        // After successful send:
        val state = ServiceState(
            lastSynced = System.currentTimeMillis(),
            lastTaskCompleted = null,
            pendingLogs = emptyList(),
            timetableJson = null,
            lastHandledTaskId = null,
            taskStatusMap = null,
            lastSyncedDate = today // <-- set the date here
        )
        PersistentStore.saveState(appContext, state)
    }
}