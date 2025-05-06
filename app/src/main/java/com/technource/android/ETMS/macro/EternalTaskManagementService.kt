package com.technource.android.ETMS.macro

import android.app.*
import android.content.*
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.technource.android.ETMS.micro.TTSManager
import com.technource.android.ETMS.micro.TaskIterator
import com.technource.android.local.AppDatabase
import com.technource.android.local.Task
import com.technource.android.local.TaskData
import com.technource.android.local.TaskResponse
import com.technource.android.local.TimeTable
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

// Data Model for State Recovery
data class ServiceState(
    val lastSynced: Long,
    val lastTaskCompleted: String?,
    val pendingLogs: List<String>
)

// Persistence Store
object PersistentStore {
    private val Context.dataStore by preferencesDataStore("service_state")

    suspend fun saveState(context: Context, state: ServiceState) {
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey("state")] = Gson().toJson(state)
        }
    }

    suspend fun loadState(context: Context): ServiceState? {
        val json = context.dataStore.data.first()[stringPreferencesKey("state")]
        return json?.let { Gson().fromJson(it, ServiceState::class.java) }
    }
}

@AndroidEntryPoint
class EternalTimeTableUnitService : Service() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var apiService: ApiService
    @Inject lateinit var gson: Gson

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val NOTI_ID = 1
    private lateinit var taskIterator: TaskIterator
    private var currentTimeTable: TimeTable = TimeTable(tasks = emptyList())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L


    override fun onCreate() {
        super.onCreate()
        taskIterator = TaskIterator(this, database.taskDao(), TTSManager(this))
        startForeground(NOTI_ID, buildNotification())
        SystemStatus.logEvent("EternalTimeTableUnitService", "Service started at ${timestampFormat.format(Date())}")

        scope.launch {
            val lastState = PersistentStore.loadState(this@EternalTimeTableUnitService)
            restoreState(lastState)
            startDailySync()
            startExpiredTaskCleanup()
        }
    }

    private fun buildNotification(): Notification {
        val channelId = "etms_service"
        val channel = NotificationChannel(channelId, "Eternal Service", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ETMS Running")
            .setContentText("Task management service is active")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .build()
    }

    private suspend fun restoreState(state: ServiceState?) {
        if (state != null) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Recovered state from ${Date(state.lastSynced)}")
            if (state.pendingLogs.isNotEmpty()) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Processing ${state.pendingLogs.size} pending logs")
                retryPendingLogs(state.pendingLogs)
            }
        } else {
            SystemStatus.logEvent("EternalTimeTableUnitService", "No prior state. Starting fresh.")
        }
    }

    private suspend fun retryPendingLogs(pendingLogs: List<String>) {
        val tasks = pendingLogs.mapNotNull { gson.fromJson(it, Task::class.java) }
        if (tasks.isNotEmpty()) {
            sendPreviousDayTimetable(tasks)
        }
    }

    private suspend fun startDailySync() {
        while (true) {
            try {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Starting daily sync at ${timestampFormat.format(Date())}")
                performDailySync()
                SystemStatus.logEvent("EternalTimeTableUnitService", "Daily sync completed. Waiting for next cycle.")
                delay(24 * 60 * 60 * 1000) // Next day
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Daily sync error: ${e.message}")
                delay(60 * 1000) // Retry after 1 minute
            }
        }
    }

    private suspend fun startExpiredTaskCleanup() {
        while (true) {
            try {
                val currentTime = System.currentTimeMillis()
                database.taskDao().deleteExpired(currentTime)
                SystemStatus.logEvent("EternalTimeTableUnitService", "Cleaned up expired tasks at ${timestampFormat.format(Date())}")
                delay(24 * 60 * 60 * 1000) // Run daily
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Expired task cleanup error: ${e.message}")
                delay(60 * 1000) // Retry after 1 minute
            }
        }
    }


    private suspend fun performDailySync() {
        // Step 1: Send previous day's timetable at 9 PM
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.HOUR_OF_DAY) >= 21) {
            val startOfDay = DateFormatter.getStartOfDayMillis()
            val startOfNextDay = DateFormatter.getStartOfNextDayMillis()
            val tasks = database.taskDao().getTodayTasks(startOfDay, startOfNextDay).toTasks()
            sendPreviousDayTimetable(tasks)
        }

        // Step 2: Fetch new timetable
        val success = fetchNewTimetable()
        if (!success) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Fetch failed after retries. Using default timetable.")
            rollbackToDefaultTimetable()
        }

        // Step 3: Start task iterator
        launchIteratorWithWatchdog()
    }

    private suspend fun sendPreviousDayTimetable(tasks: List<Task> = emptyList()): Boolean {
        SystemStatus.logEvent("EternalTimeTableUnitService", "Attempting to send previous day's timetable")
        val tasksToSend = tasks.ifEmpty {
            val startOfDay = DateFormatter.getStartOfDayMillis() - 24 * 60 * 60 * 1000 // Previous day
            val endOfDay = DateFormatter.getStartOfNextDayMillis() - 24 * 60 * 60 * 1000
            database.taskDao().getTodayTasks(startOfDay, endOfDay).toTasks()
        }

        if (tasksToSend.isEmpty()) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "No previous tasks to send")
            return true
        }

        // Validate tasks before sending
        val validationResult = TaskValidator.validateTasks(tasksToSend)
        if (!validationResult.isValid) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Task validation failed: ${validationResult.message}")
            sendNotification("Task Validation Failed", validationResult.message)
            saveStateWithPendingLogs(tasksToSend)
            return false
        }

        val taskResponse = TaskResponse(
            success = true,
            data = TaskData(
                id = "local_${System.currentTimeMillis()}",
                date = SimpleDateFormat("yyyy-MM-dd").format(Date()),
                temperature = 0.0f,
                tasks = tasksToSend,
                dayScore = 0f,
                version = 0
            )
        )

        var success = false
        repeat(3) { attempt ->
            try {
                apiService.sendTasks(taskResponse)
                SystemStatus.logEvent("EternalTimeTableUnitService", "Successfully sent ${tasksToSend.size} tasks to backend")
                database.taskDao().clearTasks() // Delete after successful send
                success = true
                return@repeat
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Send attempt ${attempt + 1} failed: ${e.message}")
                sendNotification("Backend Sync Failed", "Failed to send timetable. Retrying...")
                delay(1 * 60 * 1000) // 1 minute between retries
            }
        }

        if (!success) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "All send attempts failed. Saving state.")
            sendNotification("Timetable Not Sent", "Previous day's timetable has not been sent and will expire in 7 days.")
            saveStateWithPendingLogs(tasksToSend)
        }

        // Update tasks with 7-day expiration
        val expirationTime = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
        val taskEntities = tasksToSend.map { it.toTaskEntity(expirationTime) }
        database.taskDao().insertTasks(taskEntities)
        SystemStatus.logEvent("EternalTimeTableUnitService", "Set ${tasksToSend.size} tasks to expire in 7 days")

        return success
    }

    private suspend fun fetchNewTimetable(): Boolean {
        // Check if a valid timetable already exists
        val startOfDay = DateFormatter.getStartOfDayMillis()
        val endOfDay = DateFormatter.getStartOfNextDayMillis()
        val existingTasks = database.taskDao().getTodayTasks(startOfDay, endOfDay).toTasks()
        if (existingTasks.isNotEmpty() && currentTimeTable.tasks.isNotEmpty()) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Valid timetable already loaded for today")
            return true
        }

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        repeat(3) { attempt ->
            try {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Fetching timetable for $date, attempt ${attempt + 1}")
                val response = apiService.getTasks(date)
                if (response.success) {
                    val tasks = response.data.tasks

                    // Validate received tasks
                    val validationResult = TaskValidator.validateTasks(tasks)
                    if (!validationResult.isValid) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Received task validation failed: ${validationResult.message}")
                        sendNotification("Task Validation Failed", validationResult.message)
                        return@repeat
                    }

                    val expirationTime = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                    val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }
                    database.taskDao().clearTasks() // Ensure only one timetable
                    database.taskDao().insertTasks(taskEntities)
                    currentTimeTable = TimeTable(tasks = tasks)
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Stored ${tasks.size} new tasks locally with 7-day expiration")
                    return true
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Fetch attempt ${attempt + 1} failed: ${e.message}")
                sendNotification("Fetch Failed", "Failed to fetch new timetable. Retrying...")
                delay(1 * 60 * 1000) // 1 minute between retries
            }
        }

        // Check if within 5 hours until 5 AM
        val now = Calendar.getInstance()
        val target = now.clone() as Calendar
        target.set(Calendar.HOUR_OF_DAY, 5)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        if (now.before(target)) {
            val delayMillis = target.timeInMillis - now.timeInMillis
            SystemStatus.logEvent("EternalTimeTableUnitService", "Waiting until 5 AM for final fetch attempt")
//            sendNotification("Fetch WIll retry until 5 AM.")
            delay(delayMillis)
            try {
                val response = apiService.getTasks(date)
                if (response.success) {
                    val tasks = response.data.tasks

                    // Validate received tasks
                    val validationResult = TaskValidator.validateTasks(tasks)
                    if (!validationResult.isValid) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Received task validation failed: ${validationResult.message}")
                        sendNotification("Task Validation Failed", validationResult.message)
                        return false
                    }

                    val expirationTime = System.currentTimeMillis() + SEVEN_DAYS_MILLIS
                    val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }
                    database.taskDao().clearTasks()
                    database.taskDao().insertTasks(taskEntities)
                    currentTimeTable = TimeTable(tasks = tasks)
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Final fetch succeeded. Stored ${tasks.size} tasks with 7-day expiration")
                    return true
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Final fetch failed: ${e.message}")
            }
        }
        return false
    }

    private suspend fun rollbackToDefaultTimetable() {
        val defaultTasks = database.defaultTaskDao().getDefaultTask()?.toTasks(gson)
        if (defaultTasks != null) {
            val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            val taskEntities = defaultTasks.map { it.toTaskEntity(expirationTime) }
            database.taskDao().clearTasks()
            database.taskDao().insertTasks(taskEntities)
            currentTimeTable = TimeTable(tasks = defaultTasks)
            SystemStatus.logEvent("EternalTimeTableUnitService", "Rolled back to ${defaultTasks.size} default tasks")
        } else {
            SystemStatus.logEvent("EternalTimeTableUnitService", "No default tasks available for rollback")
            currentTimeTable = TimeTable(tasks = emptyList())
        }
    }

    private suspend fun launchIteratorWithWatchdog() {
        scope.launch {
            runCatching {
                taskIterator.start()
                persistTaskLogs() // Persist logs after iterator completes
            }.onFailure {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Task iterator crashed: ${it.message}")
                markUnfinishedTasks()
                persistTaskLogs() // Persist logs on crash
                delay(60 * 1000) // Wait 1 minute before restart
                launchIteratorWithWatchdog()
            }
        }
    }

    private suspend fun persistTaskLogs() {
        val statuses = taskIterator.getStatusTimestamps()
        if (statuses.isNotEmpty()) {
            val logMessage = statuses.joinToString("\n") {
                "Task ${it.title} (${it.taskId}): ${it.status} at ${timestampFormat.format(Date(it.timestamp))}"
            }
            SystemStatus.logEvent("EternalTimeTableUnitService", "Persisting task logs:\n$logMessage")
            // Optionally save to persistent storage or database
            val state = ServiceState(
                lastSynced = System.currentTimeMillis(),
                lastTaskCompleted = statuses.lastOrNull()?.taskId,
                pendingLogs = emptyList() // Clear pending logs as they are now persisted
            )
            PersistentStore.saveState(this, state)
        }
    }

    private suspend fun markUnfinishedTasks() {
        val startOfDay = DateFormatter.getStartOfDayMillis()
        val endOfDay = DateFormatter.getStartOfNextDayMillis()
        val tasks = database.taskDao().getTodayTasks(startOfDay, endOfDay).toTasks()
        val statuses = taskIterator.getStatusTimestamps()
        tasks.forEach { task ->
            if (statuses.none { it.taskId == task.id && it.status == "LOGGED" }) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Marking task ${task.title} as unfinished (score=0)")
                // Update task score to 0 in database (assuming TaskEntity has a score field)
//                val updatedTask = task.copy(score = 0f)
//                database.taskDao().updateTask(updatedTask.toTaskEntity(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
            }
        }
    }

    private suspend fun saveStateWithPendingLogs(tasks: List<Task>) {
        val state = ServiceState(
            lastSynced = System.currentTimeMillis(),
            lastTaskCompleted = null,
            pendingLogs = tasks.map { gson.toJson(it) }
        )
        PersistentStore.saveState(this, state)
        SystemStatus.logEvent("EternalTimeTableUnitService", "Saved state with ${tasks.size} pending tasks")
    }

    private fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, "etms_service")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        taskIterator.stop()
        SystemStatus.logEvent("EternalTimeTableUnitService", "Service destroyed. Will be relaunched.")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

// AutoStart Receiver
class AutoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action in listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_POWER_CONNECTED)) {
            context?.let {
                val serviceIntent = Intent(it, EternalTimeTableUnitService::class.java)
                ContextCompat.startForegroundService(it, serviceIntent)
                if (intent != null) {
                    SystemStatus.logEvent("AutoStartReceiver", "Restarted service after ${intent.action}")
                }
            }
        }
    }
}