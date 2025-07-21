package com.technource.android.eTMS.macro

import android.Manifest
import android.app.*
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.technource.android.utils.PermissionActivity
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.utils.TTSManager
import com.technource.android.eTMS.micro.TaskIterator
import com.technource.android.local.AppDatabase
import com.technource.android.local.Task
import com.technource.android.local.TaskDao
import com.technource.android.local.TaskResponse
import com.technource.android.local.TaskStatus
import com.technource.android.local.TimeTable
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.GlobalScope.coroutineContext
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalStdlibApi::class, DelicateCoroutinesApi::class)
@AndroidEntryPoint
class EternalTimeTableUnitService : Service() {
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var apiService: ApiService
    @Inject lateinit var gson: Gson
    @Inject lateinit var taskDao: TaskDao

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
        SystemStatus.logEvent("EternalTimeTableUnitService", "Coroutine error: ${throwable.message}")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Coroutine error: ${throwable.message}",
            "ERROR",
            DateFormatter.formatDateTime(Date()),
//            mapOf("error" to throwable.message)
        )
    })
    private lateinit var taskIterator: TaskIterator
    private var currentTimeTable: TimeTable = TimeTable(tasks = emptyList())
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val defaultTaskFetcher by lazy {
        DefaultTaskFetcher(database, apiService, gson)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            // Minimal setup before foreground
            val notification = buildNotification()
            startForeground(ETMSConfig.NOTIFICATION_ID, notification)

            // Heavy initialization after foreground
            taskIterator = TaskIterator(this, database.taskDao(), TTSManager(this)).apply {
                onTaskStatusUpdated = { _, _ ->
                    scope.launch {
                        saveFullState(
                            timetable = currentTimeTable,
                            lastHandledTaskId = null,
                            taskStatusMap = database.taskDao().getTasks().toTasks()
                                .associate { it.id to (it.status?.name ?: "UPCOMING") }
                        )
                    }
                }
            }

            val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(1001, ComponentName(this, RestartServiceJob::class.java))
                .setPeriodic(15 * 60 * 1000L)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .build()
            jobScheduler.schedule(jobInfo)
            SystemStatus.logEvent("EternalTimeTableUnitService", "Scheduled RestartServiceJob")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Scheduled RestartServiceJob",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(this, EternalTimeTableUnitService::class.java).let { intent ->
                PendingIntent.getService(
                    this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                alarmIntent
            )
            SystemStatus.logEvent("EternalTimeTableUnitService", "Scheduled exact AlarmManager watchdog")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Scheduled exact AlarmManager watchdog",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )

            if (!isServiceRunning(WatchdogService::class.java)) {
                val watchdogIntent = Intent(this, WatchdogService::class.java)
                ContextCompat.startForegroundService(this, watchdogIntent)
                SystemStatus.logEvent("EternalTimeTableUnitService", "Started WatchdogService")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Started WatchdogService",
                    "INFO",
                    DateFormatter.formatDateTime(Date())
                )
            }

            scope.launch {
                while (isActive) {
                    PersistentStore.saveHeartbeat(this@EternalTimeTableUnitService)
                    delay(60_000) // Increased to 60s to reduce battery usage
                }
            }

            startHeartbeatCheck()

            SystemStatus.logEvent("EternalTimeTableUnitService", "Service started at ${timestampFormat.format(Date())}")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Service started at ${timestampFormat.format(Date())}",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )

            scope.launch {
                try {
                    val lastState = PersistentStore.loadState(this@EternalTimeTableUnitService)
                    restoreState(lastState)
                }
                catch (e: Exception) {
                    SystemStatus.logEvent("EternalTimeTableUnitService", "State restoration failed in onCreate: ${e.message}")
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "State restoration failed in onCreate: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
//                        mapOf("error" to e.message)
                    )
                    rollbackToDefaultTimetable()
                }
                startDailySync()
                startExpiredTaskCleanup()
                TaskSyncBackendWorker.schedule(this@EternalTimeTableUnitService)
            }

            // Ensure taskIterator starts even if restoreState fails
            scope.launch {
                markMissedTasksOnStartup()
                taskIterator.start()
                SystemStatus.logEvent("EternalTimeTableUnitService", "Task iterator started successfully")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Task iterator started successfully",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
            }
        }
        catch (e: Exception) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "onCreate failed: ${e.message}")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "onCreate failed: ${e.message}",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
//                mapOf("error" to e.message)
            )
            val errorNotification = NotificationCompat.Builder(this, ETMSConfig.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("ETMS Error")
                .setContentText("Service failed to start: ${e.message}")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            startForeground(ETMSConfig.NOTIFICATION_ID, errorNotification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        taskIterator.stop()
        SystemStatus.logEvent("EternalTimeTableUnitService", "Service destroyed. Will be relaunched.")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val restartIntent = Intent(this, EternalTimeTableUnitService::class.java)
        restartIntent.setPackage(packageName)
        val restartPendingIntent = PendingIntent.getService(
            this, 1, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExact(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartPendingIntent
        )
        SystemStatus.logEvent("EternalTimeTableUnitService", "Service swiped, scheduling restart")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Service swiped, scheduling restart",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
        super.onTaskRemoved(rootIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_EOD_SYNC" -> {
                scope.launch { performEODSync() }
            }
            "ACTION_ROLLBACK_TO_DEFAULT" -> {
                scope.launch {
                    rollbackToDefaultTimetable()
                    launchIteratorWithWatchdog()
                }
            }
        }
        return START_STICKY // Ensure service is sticky
    }





    private fun buildNotification(): Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = ETMSConfig.NOTIFICATION_CHANNEL_ID

        if (notificationManager.getNotificationChannel(channelId) == null) {
            val channel = NotificationChannel(
                channelId,
                "Eternal Service",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    NotificationManager.IMPORTANCE_DEFAULT
                } else {
                    NotificationManager.IMPORTANCE_LOW
                }
            )
            notificationManager.createNotificationChannel(channel)
            SystemStatus.logEvent("EternalTimeTableUnitService", "Created notification channel: $channelId")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Created notification channel: $channelId",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(this, PermissionActivity::class.java).apply {
                action = "REQUEST_NOTIFICATION_PERMISSION"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            SystemStatus.logEvent("EternalTimeTableUnitService", "Notification permission missing, prompting user")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Notification permission missing, prompting user",
                "WARNING",
                DateFormatter.formatDateTime(Date())
            )
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("ETMS Running")
            .setContentText("Task management service is active")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setPriority(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                NotificationCompat.PRIORITY_DEFAULT
            } else {
                NotificationCompat.PRIORITY_LOW
            })
            .build()
    }

    private fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, ETMSConfig.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Use HIGH for warnings
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun startHeartbeatCheck() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = 60_000
        val heartbeatIntent = Intent(this, HeartbeatReceiver::class.java).apply {
            action = "com.technource.android.HEARTBEAT_CHECK"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 2, heartbeatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + time,
            pendingIntent
        )
        SystemStatus.logEvent("EternalTimeTableUnitService", "Started exact heartbeat check check every ${time}")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Started exact heartbeat check check every ${time}",
            "INFO",
            DateFormatter.formatDateTime(Date()),
            mapOf("interval" to time)
        )
    }

    private fun determineTaskStatus(task: Task): TaskStatus {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val startTime = DateFormatter.parseIsoDateTime(task.startTime)
        val endTime = DateFormatter.parseIsoDateTime(task.endTime)

        // Sanity check for invalid timestamps
        if (startTime.isAfter(endTime)) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Invalid timestamps for task ${task.id}: start=$startTime, end=$endTime")
            mapOf("taskId" to task.id, "startTime" to startTime.toString(), "endTime" to endTime.toString()).let {
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Invalid timestamps for task ${task.id}: start=$startTime, end=$endTime",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
        //                    it
                )
            }
            return TaskStatus.SYSTEM_FAILURE
        }

        return when {
            now.isBefore(startTime) -> TaskStatus.UPCOMING
            now.isAfter(endTime) -> TaskStatus.MISSED
            else -> TaskStatus.RUNNING
        }
    }

    private fun calculateExponentialBackoff(retryCount: Int): Long {
        val baseDelay = ETMSConfig.RETRY_DELAY_MS
        val maxDelay = 5 * 60 * 1000L // 5 minutes
        return minOf(baseDelay * (1 shl minOf(retryCount, 5)), maxDelay)
    }

    private fun CoroutineDispatcher.isCurrentDispatcher(): Boolean {
        return coroutineContext[CoroutineDispatcher] == this
    }





    private suspend fun restoreState(state: ServiceState?) {
        if (state != null) {
            try {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Recovered state from ${Date(state.lastSynced)}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Recovered state from ${Date(state.lastSynced)}",
                    "INFO",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("lastSynced" to state.lastSynced)
                )

                state.timetableJson?.let {
                    currentTimeTable = gson.fromJson(it, TimeTable::class.java)
                    if (currentTimeTable.tasks.isNotEmpty()) {
                        val expirationTime = System.currentTimeMillis() + ETMSConfig.SEVEN_DAYS_MILLIS
                        val taskEntities = currentTimeTable.tasks.map { t -> t.toTaskEntity(expirationTime) }
                        database.taskDao().clearTasks()
                        database.taskDao().insertTasks(taskEntities)
                    }
                }
                state.taskStatusMap?.let { statusMap ->
                    val tasks = database.taskDao().getTasks().toTasks()
                    tasks.forEach { task ->
                        statusMap[task.id]?.let { statusStr ->
                            try {
                                task.status = TaskStatus.valueOf(statusStr)
                                database.taskDao().updateTask(task.toTaskEntity())
                            } catch (e: IllegalArgumentException) {
                                SystemStatus.logEvent("EternalTimeTableUnitService", "Invalid task status: $statusStr")
                                DashboardActivity.logEventToWebView(
                                    "EternalTimeTableUnitService",
                                    "Invalid task status: $statusStr",
                                    "ERROR",
                                    DateFormatter.formatDateTime(Date()),
                                    mapOf("statusStr" to statusStr)
                                )
                            }
                        }
                    }
                }
                if (state.pendingLogs.isNotEmpty()) {
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Processing ${state.pendingLogs.size} pending logs")
//                    DashboardActivity.logEventToWebView(
//                        "EternalTimeTableUnitService",
//                        "Invalid task status: $statusStr",
//                        "ERROR",
//                        DateFormatter.formatIsoDateTime(Date()),
//                        mapOf("statusStr" to statusStr)
//                    )
                    retryPendingLogs(state.pendingLogs)
                }
                taskIterator.setResumeFromTaskId(state.lastHandledTaskId)
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "State restoration failed: ${e.message}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "State restoration failed: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    mapOf("error" to e.message)
                )
                rollbackToDefaultTimetable()
            }
        } else {
            SystemStatus.logEvent("EternalTimeTableUnitService", "No prior state. Starting fresh.")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "No prior state. Starting fresh.",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
            rollbackToDefaultTimetable()
        }
    }

    private suspend fun retryPendingLogs(pendingLogs: List<String>) {
        val tasks = pendingLogs.mapNotNull { gson.fromJson(it, Task::class.java) }
        if (tasks.isNotEmpty()) {
            sendPreviousDayTimetable(tasks)
        }
    }

    private suspend fun startDailySync() {
        var retryCount = 0
        while (true) {
            try {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Starting daily sync at ${timestampFormat.format(Date())}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Starting daily sync at ${timestampFormat.format(Date())}",
                    "INFO",
                    DateFormatter.formatDateTime(Date())
                )
                performDailySync()
                SystemStatus.logEvent("EternalTimeTableUnitService", "Daily sync completed. Waiting for next cycle.")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Daily sync completed. Waiting for next cycle.",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
                retryCount = 0 // Reset retry count on success
                delay(24 * 60 * 60 * 1000) // Next day
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Daily sync error: ${e.message}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Daily sync error: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    mapOf("error" to e.message)
                )
                retryCount++
                delay(calculateExponentialBackoff(retryCount))
            }
        }
    }

    private suspend fun startExpiredTaskCleanup() {
        var retryCount = 0
        while (true) {
            try {
                val currentTime = System.currentTimeMillis()
                database.taskDao().deleteExpired(currentTime)
                SystemStatus.logEvent("EternalTimeTableUnitService", "Cleaned up expired tasks at ${timestampFormat.format(Date())}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Cleaned up expired tasks at ${timestampFormat.format(Date())}",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
                retryCount = 0 // Reset retry count on success
                delay(24 * 60 * 60 * 1000) // Run daily
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Expired task cleanup error: ${e.message}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Expired task cleanup error: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    mapOf("error" to e.message)
                )
                retryCount++
                delay(calculateExponentialBackoff(retryCount))
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

        // Step 2: Fetch new timetable or generate dev tasks
        val success = if (ETMSConfig.DEV_MODE) {
            DevTaskGenerator.generateDevTasks(taskDao) // Generate dev tasks
            currentTimeTable = TimeTable(tasks = taskDao.getTasks().toTasks())
            true
        } else {
            fetchNewTimetable()
        }
        if (!success) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Fetch failed after retries. Using default timetable.")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Fetch failed after retries. Using default timetable.",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
                mapOf("error" to "Fetch failed after retries")
            )
            rollbackToDefaultTimetable()
        }

        // --- Set all tasks' status to UPCOMING if null ---
        val tasks = database.taskDao().getTasks().toTasks()
        tasks.forEach { task ->
            if (task.status == null) {
                task.status = TaskStatus.UPCOMING
                database.taskDao().updateTask(task.toTaskEntity())
            }
        }

        // Step 3: Start task iterator
        launchIteratorWithWatchdog()
    }

    private suspend fun sendPreviousDayTimetable(tasks: List<Task> = emptyList()): Boolean {
        SystemStatus.logEvent("EternalTimeTableUnitService", "Attempting to send previous day's timetable")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Attempting to send previous day\\'s timetable",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )

        val tasksToSend = tasks.ifEmpty {
            val startOfDay = DateFormatter.getStartOfDayMillis() - 24 * 60 * 60 * 1000 // Previous day
            val endOfDay = DateFormatter.getStartOfNextDayMillis() - 24 * 60 * 60 * 1000
            database.taskDao().getTodayTasks(startOfDay, endOfDay).toTasks()
        }

        // Mark all tasks as MISSED if not already LOGGED/MISSED/SYSTEM_FAILURE
        tasksToSend.forEach { task ->
            if (task.status != TaskStatus.LOGGED &&
                task.status != TaskStatus.MISSED &&
                task.status != TaskStatus.SYSTEM_FAILURE
            ) {
                task.status = TaskStatus.MISSED
                database.taskDao().updateTask(task.toTaskEntity())
                SystemStatus.logEvent("EternalTimeTableUnitService", "Marked as MISSED before sending: ${task.title}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Marked as MISSED before sending: ${task.title}",
                    "INFO",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("taskId" to task.id, "taskTitle" to task.title, "statusReported" to "MISSED")
                )
            }
        }

        var success = false
        val lastState = PersistentStore.loadState(this)
        val timetableJson = lastState?.timetableJson

        if (timetableJson != null) {
            val timetableMap = gson.fromJson(timetableJson, MutableMap::class.java) as MutableMap<String, Any?>
            val data = timetableMap["data"] as? MutableMap<String, Any?> ?: mutableMapOf()
            data["tasks"] = tasksToSend
            timetableMap["data"] = data

            val finalJson = gson.toJson(timetableMap)
            val taskResponse = gson.fromJson(finalJson, TaskResponse::class.java)

            // Try sending up to 3 times
            repeat(ETMSConfig.RETRY_ATTEMPTS) { attempt ->
                try {
                    val response = apiService.sendTasks(taskResponse)
                    if (response.isSuccessful && response.body()?.success == true) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Successfully sent tasks to backend")
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Successfully sent tasks to backend",
                            "SUCCESS",
                            DateFormatter.formatDateTime(Date())
                        )
                        // Proceed with clearing tasks, updating state, etc.
                        database.taskDao().clearTasks()
                        success = true
                        return@repeat
                    } else {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Backend did not confirm success:")
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Backend did not confirm success",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                            mapOf("error" to "Backend did not confirm success")
                        )
                        sendNotification("Backend Sync Failed", "Backend did not confirm success. Retrying...")
                        delay(ETMSConfig.RETRY_DELAY_MS)
                    }
                } catch (e: Exception) {
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Send attempt ${attempt + 1} failed: ${e.message}")
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "Send attempt ${attempt + 1} failed: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
//                        mapOf("error" to e.message, "attempt" to attempt + 1)
                    )
                    sendNotification("Backend Sync Failed", "Failed to send timetable. Retrying...")
                    delay(ETMSConfig.RETRY_DELAY_MS)
                }
            }
        }

        if (!success) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "All send attempts failed. Saving state.")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "All send attempts failed. Saving state.",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
                mapOf("pendingTasksCount" to tasksToSend.size)
            )
            sendNotification("Timetable Not Sent", "Previous day's timetable has not been sent and will expire in 7 days.")
            saveStateWithPendingLogs(tasksToSend)
        }

        // Update tasks with 7-day expiration
        val expirationTime = System.currentTimeMillis() + ETMSConfig.SEVEN_DAYS_MILLIS
        val taskEntities = tasksToSend.map { it.toTaskEntity(expirationTime) }
        database.taskDao().insertTasks(taskEntities)
        SystemStatus.logEvent("EternalTimeTableUnitService", "Set ${tasksToSend.size} tasks to expire in 7 days")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Set ${tasksToSend.size} tasks to expire in 7 days",
            "INFO",
            DateFormatter.formatDateTime(Date()),
            mapOf("taskCount" to tasksToSend.size)
        )

        // Check if today's timetable was already synced
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        // Only load lastState once, reuse above
        if (lastState?.lastSyncedDate == today) {
            SystemStatus.logEvent("ETMS", "Timetable already synced for $today, skipping send.")
            DashboardActivity.logEventToWebView(
                "ETMS",
                "Timetable already synced for $today, skipping send.",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("date" to today)
            )
            return true
        }

        return success
    }

    private suspend fun fetchNewTimetable(): Boolean {
        // Check if a valid timetable already exists
        val startOfDay = DateFormatter.getStartOfDayMillis()
        val endOfDay = DateFormatter.getStartOfNextDayMillis()
        val existingTasks = database.taskDao().getTodayTasks(startOfDay, endOfDay).toTasks()
        if (existingTasks.isNotEmpty() && currentTimeTable.tasks.isNotEmpty()) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Valid timetable already loaded for today")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Valid timetable already loaded for today",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
            return true
        }

        val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
        var retryCount = 0
        repeat(ETMSConfig.RETRY_ATTEMPTS) { attempt ->
            try {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Fetching timetable for $date, attempt ${attempt + 1}")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Fetching timetable for $date, attempt ${attempt + 1}",
                    "INFO",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("date" to date, "attempt" to attempt + 1)
                )
                val response = apiService.getTasks(date)
                if (response.success) {

                    val tasks = response.data.tasks

                    // Validate received tasks
                    val validationResult = TaskValidator.validateTasks(tasks)
                    if (!validationResult.isValid) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Received task validation failed: ${validationResult.message}")
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Received task validation failed: ${validationResult.message}",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                            mapOf("error" to validationResult.message)
                        )
                        sendNotification("Task Validation Failed", validationResult.message)
                        return@repeat
                    }
                    // Additional timetable validation
                    if (tasks.isEmpty()) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Invalid timetable: empty tasks or null data")
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Invalid timetable: empty tasks or null data",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                            mapOf("error" to "Invalid timetable")
                        )
                        sendNotification("Invalid Timetable", "Received empty or invalid timetable data")
                        return@repeat
                    }

                    val expirationTime = System.currentTimeMillis() + ETMSConfig.SEVEN_DAYS_MILLIS
                    val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }
                    database.taskDao().clearTasks() // Ensure only one timetable
                    database.taskDao().insertTasks(taskEntities)
                    currentTimeTable = TimeTable(tasks = tasks)
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Stored ${tasks.size} new tasks locally with 7-day expiration")
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "Stored ${tasks.size} new tasks locally with 7-day expiration",
                        "SUCCESS",
                        DateFormatter.formatDateTime(Date()),
                        mapOf("taskCount" to tasks.size)
                    )
                    // After fetching and validating the timetable:
                    val timetableJson = gson.toJson(response)
                    PersistentStore.saveState(this, ServiceState(
                        lastSynced = System.currentTimeMillis(),
                        lastTaskCompleted = null,
                        pendingLogs = emptyList(),
                        timetableJson = timetableJson,
                        lastHandledTaskId = null,
                        taskStatusMap = null,
                        lastSyncedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    ))
                    return true
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Fetch attempt ${attempt + 1} failed: ${e.message}")
                mapOf("error" to e.message, "attempt" to attempt + 1).let {
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "Fetch attempt ${attempt + 1} failed: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
            //                        it
                    )
                }
                sendNotification("Fetch Failed", "Failed to fetch new timetable. Retrying...")
                retryCount = attempt + 1
                delay(ETMSConfig.RETRY_DELAY_MS) // 1 minute between retries
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
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Waiting until 5 AM for final fetch attempt",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
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
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Received task validation failed: ${validationResult.message}",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                            mapOf("error" to validationResult.message)
                        )
                        sendNotification("Task Validation Failed", validationResult.message)
                        return false
                    }
                    if (tasks.isEmpty()) {
                        SystemStatus.logEvent("EternalTimeTableUnitService", "Invalid timetable: empty tasks or null data")
                        DashboardActivity.logEventToWebView(
                            "EternalTimeTableUnitService",
                            "Invalid timetable: empty tasks or null data",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                            mapOf("error" to "Invalid timetable")
                        )
                        sendNotification("Invalid Timetable", "Received empty or invalid timetable data")
                        return false
                    }

                    val expirationTime = System.currentTimeMillis() + ETMSConfig.SEVEN_DAYS_MILLIS
                    val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }
                    database.taskDao().clearTasks()
                    database.taskDao().insertTasks(taskEntities)
                    currentTimeTable = TimeTable(tasks = tasks)
                    SystemStatus.logEvent("EternalTimeTableUnitService", "Final fetch succeeded. Stored ${tasks.size} tasks with 7-day expiration")
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "Final fetch succeeded. Stored ${tasks.size} tasks with 7-day expiration",
                        "SUCCESS",
                        DateFormatter.formatDateTime(Date()),
                        mapOf("taskCount" to tasks.size)
                    )
                    return true
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Final fetch failed: ${e.message}")
                mapOf("error" to e.message).let {
                    DashboardActivity.logEventToWebView(
                        "EternalTimeTableUnitService",
                        "Final fetch failed: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
            //                        it
                    )
                }
            }
        }
        return false
    }

    private suspend fun rollbackToDefaultTimetable() {
        val defaultTasks = defaultTaskFetcher.getCurrentDefaultTasks()
        if (defaultTasks != null) {
            val expirationTime = System.currentTimeMillis() + 24 * 60 * 60 * 1000
            val taskEntities = defaultTasks.map { it.toTaskEntity(expirationTime) }
            database.taskDao().clearTasks()
            database.taskDao().insertTasks(taskEntities)
            currentTimeTable = TimeTable(tasks = defaultTasks)
            SystemStatus.logEvent("EternalTimeTableUnitService", "Rolled back to ${defaultTasks.size} default tasks")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Rolled back to ${defaultTasks.size} default tasks",
                "SUCCESS",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskCount" to defaultTasks.size)
            )
        } else {
            SystemStatus.logEvent("EternalTimeTableUnitService", "No default tasks available for rollback")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "No default tasks available for rollback",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
                mapOf("error" to "No default tasks")
            )
            currentTimeTable = TimeTable(tasks = emptyList())
        }
        saveFullState(
            timetable = currentTimeTable,
            lastHandledTaskId = null,
            taskStatusMap = database.taskDao().getTasks().toTasks().associate { it.id to (it.status?.name ?: "UPCOMING") }
        )
    }

    private suspend fun launchIteratorWithWatchdog() {
        scope.launch {
            try {
                runCatching {
                    taskIterator.start()
                    persistTaskLogs()
                }.onFailure {
                    SystemStatus.logEvent("ETMS", "Iterator failed: ${it.message}")
                    mapOf("error" to it.message).let { it1 ->
                        DashboardActivity.logEventToWebView(
                            "ETMS",
                            "Iterator failed: ${it.message}",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
                //                            it
                        )
                    }
                    markUnfinishedTasks()
                    delay(ETMSConfig.RETRY_DELAY_MS)
                    launchIteratorWithWatchdog() // Retry
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("ETMS", "Watchdog failed: ${e.message}")
                mapOf("error" to e.message).let {
                    DashboardActivity.logEventToWebView(
                        "ETMS",
                        "Watchdog failed: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
            //                        it
                    )
                }
            }
        }
    }

    private suspend fun persistTaskLogs(tasks: List<Task>? = null) {
        if (Dispatchers.Main.isCurrentDispatcher()) {
            SystemStatus.logEvent("ETMS", "Warning: persistTaskLogs running on Main thread!")
            DashboardActivity.logEventToWebView(
                "ETMS",
                "Warning: persistTaskLogs running on Main thread!",
                "WARNING",
                DateFormatter.formatDateTime(Date())
            )
        }

        val tasksToProcess = tasks ?: taskDao.getTasks().toTasks()
        val dao = database.taskDao()
        
        tasksToProcess.forEach { task ->
            try {
                // Get existing task from database
                val existingTask = dao.getTaskById(task.id)
                
                // Update task status if needed
                if (existingTask != null) {
                    val updatedTask = task.copy().apply {
                        // Preserve existing status if already LOGGED or MISSED
                        status = when (existingTask.status?.let { TaskStatus.valueOf(it) }) {
                            TaskStatus.LOGGED, TaskStatus.MISSED -> existingTask.status.let { TaskStatus.valueOf(it) }
                            else -> determineTaskStatus(task)
                        }
                    }
                    
                    // Log the persistence
                    SystemStatus.logEvent("ETMS", """
                        Persisting task:
                        ID: ${task.id}
                        Title: ${task.title}
                        Status: ${updatedTask.status}
                        Time: ${LocalDateTime.now(ZoneId.systemDefault())}
                    """.trimIndent())

                    mapOf("taskId" to task.id, "taskTitle" to task.title, "statusReported" to updatedTask.status?.name).let {
                        DashboardActivity.logEventToWebView(
                            "ETMS",
                            "Persisting task: ID: ${task.id}, Title: ${task.title}, Status: ${updatedTask.status}",
                            "INFO",
                            DateFormatter.formatDateTime(Date()),
                //                            it
                        )
                    }
                    
                    // Update in database
                    dao.updateTask(updatedTask.toTaskEntity())
                } else {
                    // New task - set initial status
                    task.status = determineTaskStatus(task)
                    dao.insertTask(task.toTaskEntity())
                }
                
            } catch (e: Exception) {
                SystemStatus.logEvent("ETMS", 
                    "Error persisting task ${task.id}: ${e.message}")
                mapOf("taskId" to task.id, "error" to e.message).let {
                    DashboardActivity.logEventToWebView(
                        "ETMS",
                        "Error persisting task ${task.id}: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
            //                        it
                    )
                }
            }
        }
    }

    private suspend fun markUnfinishedTasks(tasks: List<Task>? = null) {
        if (Dispatchers.Main.isCurrentDispatcher()) {
            SystemStatus.logEvent("ETMS", "Warning: markUnfinishedTasks running on Main thread!")
            DashboardActivity.logEventToWebView(
                "ETMS",
                "Warning: markUnfinishedTasks running on Main thread!",
                "WARNING",
                DateFormatter.formatDateTime(Date())
            )
        }
    val tasksToProcess = tasks ?: taskDao.getTasks().toTasks()
    val now = LocalDateTime.now(ZoneId.systemDefault())
    tasksToProcess.forEach { task ->
        try {
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)
            if (now.isAfter(endTime) && task.status != TaskStatus.LOGGED) {
                // If the app crashed or was killed, mark as SYSTEM_FAILURE
                task.status = TaskStatus.SYSTEM_FAILURE
                SystemStatus.logEvent("ETMS", """
                    Task marked as SYSTEM_FAILURE:
                    ID: ${task.id}
                    Title: ${task.title}
                    End Time: ${task.endTime}
                    Current Time: $now
                """.trimIndent())

                DashboardActivity.logEventToWebView(
                    "ETMS",
                    "Task marked as SYSTEM_FAILURE: ID: ${task.id}, Title: ${task.title}, End Time: ${task.endTime}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("taskId" to task.id, "taskTitle" to task.title, "endTime" to task.endTime, "statusReported" to "SYSTEM_FAILURE")
                )
                // Update database
                database.taskDao().updateTask(task.toTaskEntity())
            }
        } catch (e: Exception) {
            SystemStatus.logEvent("ETMS", 
                "Error marking unfinished task ${task.id}: ${e.message}")
            mapOf("taskId" to task.id, "error" to e.message).let {
                DashboardActivity.logEventToWebView(
                    "ETMS",
                    "Error marking unfinished task ${task.id}: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
        //                    it
                )
            }
        }
    }
}

    private suspend fun markMissedTasksOnStartup() {
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val tasks = database.taskDao().getTasks().toTasks()
        tasks.forEach { task ->
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)
            if (now.isAfter(endTime) && task.status != TaskStatus.LOGGED && task.status != TaskStatus.MISSED) {
                task.status = TaskStatus.MISSED
                database.taskDao().updateTask(task.toTaskEntity())
                SystemStatus.logEvent("ETMS", "Marked missed on startup: ${task.title}")
                DashboardActivity.logEventToWebView(
                    "ETMS",
                    "Marked missed on startup: ${task.title}",
                    "INFO",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("taskId" to task.id, "taskTitle" to task.title, "statusReported" to "MISSED")
                )
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
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "Saved state with ${tasks.size} pending tasks",
            "INFO",
            DateFormatter.formatDateTime(Date()),
            mapOf("pendingTasksCount" to tasks.size)
        )
    }

    private suspend fun performEODSync() {
        SystemStatus.logEvent("EternalTimeTableUnitService", "EOD sync triggered by TaskIterator")
        DashboardActivity.logEventToWebView(
            "EternalTimeTableUnitService",
            "EOD sync triggered by TaskIterator",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
        val startOfDay = com.technource.android.utils.DateFormatter.getStartOfDayMillis()
        val startOfNextDay = com.technource.android.utils.DateFormatter.getStartOfNextDayMillis()
        val tasks = database.taskDao().getTodayTasks(startOfDay, startOfNextDay).toTasks()
        sendPreviousDayTimetable(tasks)
        // Start polling for new TT
        pollForNextTimetable()
    }

    private suspend fun pollForNextTimetable() {
        val maxWaitHours = 12 // Don't poll forever
        val pollIntervalMs = 15 * 60 * 1000L // 15 minutes
        val startTime = System.currentTimeMillis()
        var fetched = false
        var retryCount = 0
        while (System.currentTimeMillis() - startTime < maxWaitHours * 60 * 60 * 1000L) {
            SystemStatus.logEvent("EternalTimeTableUnitService", "Polling for new timetable...")
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Polling for new timetable...",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
            fetched = fetchNewTimetable()
            if (fetched) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Fetched new timetable after EOD, starting iterator.")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Fetched new timetable after EOD, starting iterator.",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
                // Set all statuses to UPCOMING if needed
                val tasks = database.taskDao().getTasks().toTasks()
                tasks.forEach { task ->
                    if (task.status == null) {
                        task.status = TaskStatus.UPCOMING
                        database.taskDao().updateTask(task.toTaskEntity())
                    }
                }
                launchIteratorWithWatchdog()
                SystemStatus.logEvent("EternalTimeTableUnitService", "Successfully fetched timetable after EOD")
//            webView.evaluateJavascript(
//                "logEvent('EternalTimeTableUnitService', 'Successfully fetched timetable after EOD', 'SUCCESS', '${DateFormatter.formatIsoDateTime(Date())}')",
//                null
//            )
                return
            }
            retryCount++
            delay(calculateExponentialBackoff(retryCount))
            // Check if it's after 5 AM
            val now = Calendar.getInstance()
            if (now.get(Calendar.HOUR_OF_DAY) >= 5) {
                SystemStatus.logEvent("EternalTimeTableUnitService", "Backend did not provide valid timetable till 5 AM, rolling back to default timetable.")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Backend did not provide valid timetable till 5 AM, rolling back to default timetable.",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
                    mapOf("error" to "Backend did not provide valid timetable")
                )
                rollbackToDefaultTimetable()
                launchIteratorWithWatchdog()
                SystemStatus.logEvent("EternalTimeTableUnitService", "Rolled back to default timetable after 5 AM")
                DashboardActivity.logEventToWebView(
                    "EternalTimeTableUnitService",
                    "Rolled back to default timetable after 5 AM",
                    "SUCCESS",
                    DateFormatter.formatDateTime(Date())
                )
                return
            }
        }
        SystemStatus.logEvent("EternalTimeTableUnitService", "Stopped polling for new timetable after max wait.")
//        webView.evaluateJavascript(
//            "logEvent('EternalTimeTableUnitService', 'Stopped polling for new timetable after max wait.', 'INFO', '${DateFormatter.formatIsoDateTime(Date())}')",
//            null
//        )
    }

    private suspend fun saveFullState(
        pendingLogs: List<Task> = emptyList(),
        timetable: TimeTable = currentTimeTable,
        lastHandledTaskId: String? = null,
        taskStatusMap: Map<String, String>? = null
    ) {
        val state = ServiceState(
            lastSynced = System.currentTimeMillis(),
            lastTaskCompleted = lastHandledTaskId,
            pendingLogs = pendingLogs.map { gson.toJson(it) },
            timetableJson = gson.toJson(timetable),
            lastHandledTaskId = lastHandledTaskId,
            taskStatusMap = taskStatusMap
        )
        PersistentStore.saveState(this, state)
        SystemStatus.logEvent("EternalTimeTableUnitService", "Saved full state with iterator progress")
        mapOf("lastHandledTaskId" to lastHandledTaskId).let {
            DashboardActivity.logEventToWebView(
                "EternalTimeTableUnitService",
                "Saved full state with iterator progress",
                "INFO",
                DateFormatter.formatDateTime(Date()),
    //                it
            )
        }
    }
}


fun Context.isServiceRunning(serviceClass: Class<*>): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == serviceClass.name && it.foreground }
        } else {
            manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == serviceClass.name }
        }
    } catch (e: SecurityException) {
        SystemStatus.logEvent("EternalTimeTableUnitService", "SecurityException in isServiceRunning: ${e.message}")
//        webView.evaluateJavascript(
//            "logEvent('EternalTimeTableUnitService', 'SecurityException in isServiceRunning: ${e.message}', 'ERROR', '${DateFormatter.formatIsoDateTime(Date())}', ${gson.toJson(mapOf("error" to e.message))})",
//            null
//        )
        // Fallback: Assume service is not running if permission is restricted
        false
    }
}
