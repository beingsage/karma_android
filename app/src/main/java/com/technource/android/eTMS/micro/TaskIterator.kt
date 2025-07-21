package com.technource.android.eTMS.micro

import android.content.Context
import android.content.Intent
import android.util.Log
import com.technource.android.eTMS.DashboardActivity
import com.technource.android.local.TaskDao
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.local.Task
import com.technource.android.local.TaskStatus
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import com.technource.android.utils.TTSManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException
import java.util.Date

class TaskIterator(
    private val context: Context,
    private val taskDao: TaskDao,
    private val ttsManager: TTSManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var taskJob: Job? = null
    private val handledTasks = mutableSetOf<String>() // Track handled tasks by ID
    private var resumeFromTaskId: String? = null

    suspend fun start() {
        SystemStatus.logEvent("TaskIterator", "TaskIterator Started At Time ${LocalDateTime.now(ZoneId.systemDefault())}")
        DashboardActivity.logEventToWebView(
            "TaskIterator",
            "TaskIterator Started At Time ${LocalDateTime.now(ZoneId.systemDefault())}",
            "INFO",
            DateFormatter.formatDateTime(Date())
        )
        handledTasks.clear() // Reset handled tasks on start
        startTaskProcessing()
    }

    private suspend fun startTaskProcessing() {
        try {
            val tasks = withContext(Dispatchers.IO) {
                taskDao.getTasks().toTasks()
            }
            if (tasks.isEmpty()) {
                SystemStatus.logEvent("TaskIterator", "No tasks found in database.")
                DashboardActivity.logEventToWebView(
                    "TaskIterator",
                    "No tasks found in database.",
                    "WARNING",
                    DateFormatter.formatDateTime(Date())
                )
                return
            }
            scheduleNextTask(tasks)
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    SystemStatus.logEvent("TaskIterator", "Task loading job was cancelled: ${e.message}")
                    mapOf("error" to e.message)?.let {
                        DashboardActivity.logEventToWebView(
                            "TaskIterator",
                            "Task loading job was cancelled: ${e.message}",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
//                            it
                        )
                    }
                }
                else -> {
                    SystemStatus.logEvent("TaskIterator", "Error loading tasks: ${e.message}")
                    mapOf("error" to e.message)?.let {
                        DashboardActivity.logEventToWebView(
                            "TaskIterator",
                            "Error loading tasks: ${e.message}",
                            "ERROR",
                            DateFormatter.formatDateTime(Date()),
//                            it
                        )
                    }
                    e.printStackTrace()
                }
            }
        }
    }


    fun setResumeFromTaskId(taskId: String?) {
        resumeFromTaskId = taskId
    }

    private suspend fun scheduleNextTask(tasks: List<Task>) {
        taskJob?.cancel()
        var resumeFound = resumeFromTaskId == null
        while (true) {
            val now = LocalDateTime.now(ZoneId.systemDefault())
            val allTasksEnded = tasks.all { task ->
                val endTime = DateFormatter.parseIsoDateTime(task.endTime)
                now.isAfter(endTime)
            }
            if (allTasksEnded) {
                SystemStatus.logEvent("TaskIterator", "All tasks have ended.")
                DashboardActivity.logEventToWebView(
                    "TaskIterator",
                    "All tasks have ended.",
                    "INFO",
                    DateFormatter.formatDateTime(Date())
                )
                return
            }
            val currentTask = findCurrentTask(tasks, now)
            if (currentTask == null) {
                delay(10_000) // Wait before checking again
                continue
            }
            // Skip tasks until resume point is found
            if (!resumeFound) {
                if (currentTask.id == resumeFromTaskId) {
                    resumeFound = true
                } else {
                    delay(10_000)
                    continue
                }
            }
            if (currentTask.id in handledTasks) {
                delay(10_000) // Skip already handled tasks
                continue 
            }

            // Only log when actually scheduling a new task
            SystemStatus.logEvent("TaskIterator", "Scheduling task: ${currentTask.title}")
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "Scheduling task: ${currentTask.title}",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskId" to currentTask.id, "taskTitle" to currentTask.title)
            )
            taskJob = scope.launch {
                handleTaskExecution(currentTask)
            }
            onTaskStatusUpdated?.invoke(currentTask, currentTask.status ?: TaskStatus.UPCOMING)
            delay(10_000) // Check every 10 seconds
        }
    }

    private fun findCurrentTask(tasks: List<Task>, now: LocalDateTime): Task? {
        if (tasks.isEmpty()) {
            SystemStatus.logEvent("TaskIterator", "Task list is empty")
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "Task list is empty",
                "WARNING",
                DateFormatter.formatDateTime(Date())
            )
            return null
        }

        return tasks.firstOrNull { task ->
            try {
                // Parse ISO format dates
                val startTime = DateFormatter.parseIsoDateTime(task.startTime)
                val endTime = DateFormatter.parseIsoDateTime(task.endTime)
                
                // SystemStatus.logEvent("TaskIterator", 
                //     "Checking task '${task.title}':\n" +
                //     "Start: ${task.startTime} -> $startTime\n" +
                //     "End: ${task.endTime} -> $endTime\n" +
                //     "Now: $now"
                // )
                
                now.isEqual(startTime) || (now.isAfter(startTime) && now.isBefore(endTime))
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskIterator", 
                    "Parse error for task '${task.title}':\n" +
                    "Start: '${task.startTime}'\n" +
                    "Error: ${e.message}"
                )
                mapOf("taskId" to task.id, "taskTitle" to task.title, "startTime" to task.startTime, "error" to e.message)?.let {
                    DashboardActivity.logEventToWebView(
                        "TaskIterator",
                        "Parse error for task \\'${task.title}\\': Start: \\'${task.startTime}\\', Error: ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
//                        it
                    )
                }
                false
            }
    }?.also { task ->
        SystemStatus.logEvent("TaskIterator", "Found current task: ${task.title}")
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "Found current task: ${task.title}",
                "INFO",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskId" to task.id, "taskTitle" to task.title)
            )
    }
}

    private suspend fun handleTaskExecution(task: Task) {
        try {
            // Parse the ISO format dates directly
            val startTime = DateFormatter.parseIsoDateTime(task.startTime)
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)
            val now = LocalDateTime.now(ZoneId.systemDefault())

            // --- Ensure only one RUNNING task at a time ---
            withContext(Dispatchers.IO) {
                val runningTasks = taskDao.getTasks().toTasks().filter { it.status == TaskStatus.RUNNING && it.id != task.id }
                runningTasks.forEach { runningTask ->
                    // Mark as SYSTEM_FAILURE or MISSED (choose your policy)
                    runningTask.status = TaskStatus.SYSTEM_FAILURE
                    taskDao.updateTask(runningTask.toTaskEntity())
                    SystemStatus.logEvent("TaskIterator", "Previous running task '${runningTask.title}' forcibly closed as SYSTEM_FAILURE")
                    DashboardActivity.logEventToWebView(
                        "TaskIterator",
                        "Previous running task \\'${runningTask.title}\\' forcibly closed as SYSTEM_FAILURE",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
                        mapOf("taskId" to runningTask.id, "taskTitle" to runningTask.title, "statusReported" to "SYSTEM_FAILURE")
                    )
                }
            }

            // Determine initial status based on current time
            when {
                now.isBefore(startTime) -> {
                    updateTaskStatus(task, TaskStatus.UPCOMING)
                }
                now.isAfter(endTime) -> {
                    // If task has ended without being logged, mark as MISSED
                    if (task.status != TaskStatus.LOGGED) {
                        updateTaskStatus(task, TaskStatus.MISSED)
                    }
                }
                else -> {
                    // Task is currently running
                    updateTaskStatus(task, TaskStatus.RUNNING)
                    handledTasks.add(task.id)

    withContext(Dispatchers.Main) {
        SystemStatus.logEvent("TaskIterator", "Executing task: ${task.title}")
        mapOf(
            "taskId" to task.id,
            "taskTitle" to task.title,
            "ttsTriggered" to ttsManager.isReady(),
            "statusReported" to task.status?.name
        )?.let {
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "Executing task: ${task.title}",
                "INFO",
                DateFormatter.formatDateTime(Date()),
//                it
            )
        }
        var spoken = false
        var attempts = 0
        val maxAttempts = 3
        while (!spoken && attempts < maxAttempts) {
            try {
                // Before calling speakHinglish
                if (ttsManager.isReady()) {
                    ttsManager.speakHinglish(task.title)
                } else {
                    SystemStatus.logEvent("TaskIterator", "TTS not initialized yet")
                    DashboardActivity.logEventToWebView(
                        "TaskIterator",
                        "TTS not initialized yet",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
                        mapOf("taskId" to task.id, "taskTitle" to task.title, "ttsTriggered" to false)
                    )
                }
                // Optionally, check if TTS is speaking and wait for it to finish
                delay(500) // Give TTS time to start
                if (ttsManager.isSpeaking()) {
                    spoken = true
                } else {
                    throw Exception("TTS did not start speaking")
                }
            } catch (e: Exception) {
                attempts++
                SystemStatus.logEvent("TaskIterator", "TTS attempt $attempts failed for '${task.title}': ${e.message}")
                mapOf("taskId" to task.id, "taskTitle" to task.title, "ttsTriggered" to false, "error" to e.message, "attempt" to attempts)?.let {
                    DashboardActivity.logEventToWebView(
                        "TaskIterator",
                        "TTS attempt $attempts failed for \\'${task.title}\\': ${e.message}",
                        "ERROR",
                        DateFormatter.formatDateTime(Date()),
//                        it
                    )
                }
                delay(700) // Wait before retry
            }
        }
        if (!spoken) {
            SystemStatus.logEvent("TaskIterator", "TTS failed after $maxAttempts attempts for '${task.title}'")
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "TTS failed after $maxAttempts attempts for \\'${task.title}\\'",
                "ERROR",
                DateFormatter.formatDateTime(Date()),
                mapOf("taskId" to task.id, "taskTitle" to task.title, "ttsTriggered" to false)
            )
        }
        updateLockScreenOverlay(task)
        updateHomeScreenWidget(task, startTime, endTime)
    }

                    // Calculate delay until end time minus 1 minute
                    val zonedEndTime = endTime.atZone(ZoneId.systemDefault())
                    val loggerTimeMs = zonedEndTime.toInstant().toEpochMilli() - 60_000
                    val currentTimeMs = System.currentTimeMillis()
                    val delayMs = (loggerTimeMs - currentTimeMs).coerceAtLeast(0)
                        .coerceAtMost(120_000L) // 2-minute cap

                    if (delayMs > 0) {
                        try {
                            delay(delayMs)
                            withContext(Dispatchers.Main) {
                                ttsManager.speakHinglish("Log Your Data")
                                var loggerLaunched = false
                                var loggerAttempts = 0
                                val maxLoggerAttempts = 3
                                while (!loggerLaunched && loggerAttempts < maxLoggerAttempts) {
                                    try {
                                        triggerTaskLogger(task)
                                        updateTaskStatus(task, TaskStatus.LOGGED)
                                        loggerLaunched = true
                                        SystemStatus.logEvent("TaskIterator", "Task logger successfully triggered for: ${task.title}")
                                        mapOf("taskId" to task.id, "taskTitle" to task.title, "loggerTriggered" to true, "statusReported" to task.status?.name)?.let {
                                            DashboardActivity.logEventToWebView(
                                                "TaskIterator",
                                                "Task logger successfully triggered for: ${task.title}",
                                                "SUCCESS",
                                                DateFormatter.formatDateTime(Date()),
//                                                it
                                            )
                                        }
                                    } catch (e: Exception) {
                                        loggerAttempts++
                                        SystemStatus.logEvent("TaskIterator", "Logger attempt $loggerAttempts failed for '${task.title}': ${e.message}")
                                        mapOf("taskId" to task.id, "taskTitle" to task.title, "loggerTriggered" to false, "error" to e.message, "attempt" to loggerAttempts)?.let {
                                            DashboardActivity.logEventToWebView(
                                                "TaskIterator",
                                                "Logger attempt $loggerAttempts failed for \\'${task.title}\\': ${e.message}",
                                                "ERROR",
                                                DateFormatter.formatDateTime(Date()),
//                                                it
                                            )
                                        }
                                        delay(700) // Wait before retry
                                    }
                                }
                                if (!loggerLaunched) {
                                    SystemStatus.logEvent("TaskIterator", "Logger failed after $maxLoggerAttempts attempts for '${task.title}'")
                                    DashboardActivity.logEventToWebView(
                                        "TaskIterator",
                                        "Logger failed after $maxLoggerAttempts attempts for \\'${task.title}\\'",
                                        "ERROR",
                                        DateFormatter.formatDateTime(Date()),
                                        mapOf("taskId" to task.id, "taskTitle" to task.title, "loggerTriggered" to false)
                                    )
                                    updateTaskStatus(task, TaskStatus.SYSTEM_FAILURE)
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        }
                    }
                }
            }

            // Update task in database after status change
            withContext(Dispatchers.IO) {
                taskDao.updateTask(task.toTaskEntity())
            }

        } catch (e: Exception) {
            SystemStatus.logEvent("TaskIterator", 
                "Error in handleTaskExecution for ${task.title}: ${e.message}")
            mapOf("taskId" to task.id, "taskTitle" to task.title, "error" to e.message)?.let {
                DashboardActivity.logEventToWebView(
                    "TaskIterator",
                    "Error in handleTaskExecution for ${task.title}: ${e.message}",
                    "ERROR",
                    DateFormatter.formatDateTime(Date()),
//                    it
                )
            }
            updateTaskStatus(task, TaskStatus.SYSTEM_FAILURE)
        }
    }

    private suspend fun checkAndTriggerEOD() {
        val allTasks = taskDao.getTasks().toTasks()
        val allDone = allTasks.all { it.status == TaskStatus.LOGGED || it.status == TaskStatus.MISSED || it.status == TaskStatus.SYSTEM_FAILURE}
        if (allDone && allTasks.isNotEmpty()) {
            SystemStatus.logEvent("TaskIterator", "All tasks finished, triggering EOD sync")
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "All tasks finished, triggering EOD sync",
                "INFO",
                DateFormatter.formatDateTime(Date())
            )
            // You may need to get a reference to EternalTaskManagementService or use a callback/event
            withContext(Dispatchers.Main) {
                val intent = Intent(context, com.technource.android.eTMS.macro.EternalTimeTableUnitService::class.java)
                intent.action = "ACTION_EOD_SYNC"
                context.startService(intent)
            }
        }
    }

    var onTaskStatusUpdated: ((Task, TaskStatus) -> Unit)? = null

    private fun updateTaskStatus(task: Task, status: TaskStatus) {
        val taskId = task.taskId
        val title  = task.title
        val oldStatus = task.status
        task.status = status
        
        // Log status change
        SystemStatus.logEvent("TaskIterator", """
            Task status updated:
            ID: ${task.id}
            Title: ${task.title}
            Old Status: $oldStatus
            New Status: $status
            Time: ${LocalDateTime.now(ZoneId.systemDefault())}
        """.trimIndent())
        mapOf("taskId" to task.id, "taskTitle" to task.title, "oldStatus" to oldStatus?.name, "statusReported" to status.name)?.let {
            DashboardActivity.logEventToWebView(
                "TaskIterator",
                "Task status updated: ID: ${task.id}, Title: ${task.title}, Old Status: $oldStatus, New Status: $status",
                "INFO",
                DateFormatter.formatDateTime(Date()),
//                it
            )
        }


        // Update database
        scope.launch(Dispatchers.IO) {
            try {
                taskDao.updateTask(task.toTaskEntity())
                checkAndTriggerEOD()
                // --- Save state after every status update ---
                onTaskStatusUpdated?.invoke(task, status)
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskIterator", 
                    "Failed to update task status in database: ${e.message}")
            }
        }

        SystemStatus.logEvent("TaskIterator", "Task status updated: $taskId, $title, $status")
    }

    fun triggerTaskLogger(task: Task) {
        val intent = Intent(context, TaskLoggerActivity::class.java).apply {
            putExtra("TASK", task)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required for launching from non-activity context
            // Removed other flags that might interfere
        }
        try {
            context.startActivity(intent)
            SystemStatus.logEvent("TaskIterator", "Task logger triggered for: ${task.title}")
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskIterator", "Error starting TaskLoggerActivity: ${e.message}")
            e.printStackTrace()
        }
    }

    fun updateLockScreenOverlay(task: Task) {

        DynamicWallpaperTaskIterator.setCurrentTask(context, task)
        SystemStatus.logEvent("TaskIterator", "Lock screen overlay updated for: ${task.title}")
    }

    fun updateHomeScreenWidget(task: Task, startTime: LocalDateTime, endTime: LocalDateTime) {
        Log.d("TaskWidgetProvider", "Updating widget with tasks: ${task}, first task: ${task.title}")
        TaskWidgetProvider.updateHomeScreenWidget(context, listOf(task), startTime, endTime)
    }

    fun stop() {
        scope.cancel()
        SystemStatus.logEvent("TaskIterator", "TaskIterator stopped")
    }
}