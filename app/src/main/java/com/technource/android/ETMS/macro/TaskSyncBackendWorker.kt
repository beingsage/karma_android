package com.technource.android.ETMS.macro

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.technource.android.local.AppDatabase
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.local.TaskResponse
import com.technource.android.local.TaskData
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
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

    companion object {
        private const val WORK_NAME = "BackEnd_Sync"

        fun schedule(context: Context) {
            val workManager = WorkManager.getInstance(context)
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 2)
                set(Calendar.MINUTE, 55)
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
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            SystemStatus.logStatus("TaskSyncWorker", "Scheduled")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            SystemStatus.logEvent("TaskSyncWorker", "Worker started at ${timestampFormat.format(Date())}")
            SystemStatus.logStatus("TaskSyncWorker", "Running")

            performSyncTask()

            SystemStatus.logEvent("TaskSyncWorker", "Worker completed")
            SystemStatus.logStatus("TaskSyncWorker", "Completed")
            Result.success()
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Error occurred: ${e.message}")
            SystemStatus.logStatus("TaskSyncWorker", "Failed")
            defaultTaskFetcher.rollbackToDefaultTasks()
            Result.retry()
        }
    }

    private suspend fun performSyncTask() {
        // Fetch local tasks
        SystemStatus.logEvent("TaskSyncWorker", "Fetching old tasks from local Room database")
        val oldTaskEntities = db.taskDao().getTasks()
        val oldTasks = oldTaskEntities.toTasks()
        val oldTaskResponse = TaskResponse(
            success = true,
            data = TaskData(
                id = "local_${System.currentTimeMillis()}",
                date = SimpleDateFormat("yyyy-MM-dd").format(Date()),
                temperature = 0.0f,
                tasks = oldTasks,
                dayScore = 0f,
                version = 0
            )
        )
        if (oldTasks.isNotEmpty()) {
            SystemStatus.logEvent("TaskSyncWorker", "Fetched ${oldTasks.size} old tasks from local Room")
            SystemStatus.logStatus("TaskSyncWorker", "LocalFetchSuccess")
        } else {
            SystemStatus.logEvent("TaskSyncWorker", "No old tasks found in local Room")
            SystemStatus.logStatus("TaskSyncWorker", "LocalFetchEmpty")
        }

        // Send old tasks to backend
        if (oldTasks.isNotEmpty()) {
            SystemStatus.logEvent("TaskSyncWorker", "Sending old tasks to backend")
            try {
                apiService.sendTasks(oldTaskResponse)
                SystemStatus.logEvent("TaskSyncWorker", "Successfully sent old tasks to backend")
                SystemStatus.logStatus("TaskSyncWorker", "BackendSendSuccess")
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskSyncWorker", "Failed to send old tasks to backend: ${e.message}")
                SystemStatus.logStatus("TaskSyncWorker", "BackendSendFailed")
                throw e
            }
        }

        // Fetch new tasks from backend
        SystemStatus.logEvent("TaskSyncWorker", "Fetching new tasks from backend")
        val newTaskResponse = try {
            apiService.getTasks(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to fetch new tasks from backend: ${e.message}")
            SystemStatus.logStatus("TaskSyncWorker", "BackendFetchFailed")
            throw e
        }
        SystemStatus.logEvent("TaskSyncWorker", "Successfully fetched ${newTaskResponse.data.tasks.size} new tasks from backend")
        SystemStatus.logStatus("TaskSyncWorker", "BackendFetchSuccess")

        // Store new tasks locally
        SystemStatus.logEvent("TaskSyncWorker", "Storing new tasks in local Room")
        val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
        val newTaskEntities = newTaskResponse.data.tasks.map { it.toTaskEntity(expirationTime) }
        try {
            db.taskDao().clearTasks()
            db.taskDao().insertTasks(newTaskEntities)
            SystemStatus.logEvent("TaskSyncWorker", "Stored ${newTaskEntities.size} new tasks locally")
            SystemStatus.logStatus("TaskSyncWorker", "LocalStoreSuccess")
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to store new tasks locally: ${e.message}")
            SystemStatus.logStatus("TaskSyncWorker", "LocalStoreFailed")
            throw e
        }

        // Clean up expired tasks
        SystemStatus.logEvent("TaskSyncWorker", "Cleaning up expired tasks from local Room")
        try {
            db.taskDao().deleteExpired(System.currentTimeMillis())
            SystemStatus.logEvent("TaskSyncWorker", "Successfully deleted expired tasks")
            SystemStatus.logStatus("TaskSyncWorker", "CleanupSuccess")
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskSyncWorker", "Failed to delete expired tasks: ${e.message}")
            SystemStatus.logStatus("TaskSyncWorker", "CleanupFailed")
            throw e
        }
    }
}