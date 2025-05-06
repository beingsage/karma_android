package com.technource.android.ETMS.micro

import android.content.Context
import android.content.Intent
import android.util.Log
import com.technource.android.local.TaskDao
import com.technource.android.local.toTaskEntity
import com.technource.android.local.toTasks
import com.technource.android.local.Task
import com.technource.android.module.homeModule.TaskAdapter
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
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
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskIterator(
    private val context: Context,
    private val taskDao: TaskDao,
    private val ttsManager: TTSManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var taskJob: Job? = null
    private var taskAdapter: TaskAdapter? = null
    private val statusTimestamps = mutableListOf<TaskStatus>()
    private val handledTasks = mutableSetOf<String>() // Track handled tasks by ID

    data class TaskStatus(
        val taskId: String,
        val title: String,
        val status: String, // "RUNNING", "LOGGED", "EOD"
        val timestamp: Long = System.currentTimeMillis()
    )

    // Expose timestamps for external access
    fun getStatusTimestamps(): List<TaskStatus> = statusTimestamps.toList()

    suspend fun start() {
        SystemStatus.logEvent("TaskIterator", "TaskIterator Started At Time ${LocalDateTime.now(ZoneId.systemDefault())}")
        handledTasks.clear() // Reset handled tasks on start
        startTaskProcessing()
    }

    private suspend fun startTaskProcessing() {
        try {
            val tasks = withContext(Dispatchers.IO) {
                taskDao.getTasks().toTasks()
            }
            if (tasks.isEmpty()) {
                SystemStatus.logEvent("TaskIterator", "No tasks found in database")
                statusTimestamps.add(TaskStatus("", "", "EOD"))
                return
            }
            scheduleNextTask(tasks)
        } catch (e: Exception) {
            when (e) {
                is CancellationException -> {
                    SystemStatus.logEvent("TaskIterator", "Task loading job was cancelled: ${e.message}")
                }
                else -> {
                    SystemStatus.logEvent("TaskIterator", "Error loading tasks: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun scheduleNextTask(tasks: List<Task>) {
        taskJob?.cancel()
        while (true) {
            val now = LocalDateTime.now(ZoneId.systemDefault())
            SystemStatus.logEvent("TaskIterator", "Current time: $now, Checking tasks: ${tasks.map { "${it.title}: ${it.startTime} to ${it.endTime}" }}")

            // Check if all tasks are past their end time
            val allTasksEnded = tasks.all { task ->
                val endTime = DateFormatter.parseIsoDateTime(task.endTime)
                now.isAfter(endTime)
            }
            if (allTasksEnded) {
                SystemStatus.logEvent("TaskIterator", "All tasks have ended at current time: $now")
                statusTimestamps.add(TaskStatus("", "", "EOD"))
//                withContext(Dispatchers.Main) { taskAdapter?.stopBlinker() }
                return
            }

            val currentTask = findCurrentTask(tasks, now)
            if (currentTask == null) {
                SystemStatus.logEvent("TaskIterator", "No active task at current time: $now")
                delay(10_000) // Wait before checking again
                continue
            }

            // Skip if the task has already been handled
            if (currentTask.id in handledTasks) {
                SystemStatus.logEvent("TaskIterator", "Task ${currentTask.title} already handled, skipping")
                delay(10_000) // Wait before checking again
                continue
            }

            SystemStatus.logEvent("TaskIterator", "Scheduling task: ${currentTask.title}")
            taskJob = scope.launch {
                handleTaskExecution(currentTask)
            }
            delay(10_000) // Check every 10 seconds
        }
    }

    private fun findCurrentTask(tasks: List<Task>, now: LocalDateTime): Task? {
        if (tasks.isEmpty()) {
            SystemStatus.logEvent("TaskIterator", "Task list is empty")
            return null
        }

        tasks.forEach { task ->
            try {
                val startTime = DateFormatter.parseIsoDateTime(task.startTime)
                val endTime = DateFormatter.parseIsoDateTime(task.endTime)
                SystemStatus.logEvent("TaskIterator", "Task ${task.title}: $startTime to $endTime")
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskIterator", "Invalid date format for task ${task.title}: ${e.message}")
            }
        }

        return tasks.firstOrNull { task ->
            val startTime = DateFormatter.parseIsoDateTime(task.startTime)
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)
            SystemStatus.logEvent("TaskIterator", "Evaluating task ${task.title}: startTime=$startTime, endTime=$endTime, now=$now")
            val isCurrent = now.isEqual(startTime) || (now.isAfter(startTime) && now.isBefore(endTime))
            SystemStatus.logEvent("TaskIterator", "Task ${task.title} isCurrent: $isCurrent")
            isCurrent
        }
    }

    private suspend fun handleTaskExecution(task: Task) {
        try {
            val startTime = DateFormatter.parseIsoDateTime(task.startTime)
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)

            statusTimestamps.add(TaskStatus(task.id, task.title, "RUNNING"))
            handledTasks.add(task.id)

            withContext(Dispatchers.Main) {
                SystemStatus.logEvent("TaskIterator", "Executing task: ${task.title}")
                ttsManager.speakHinglish(task.title)
                updateLockScreenOverlay(task, startTime, endTime)
                updateHomeScreenWidget(task, startTime, endTime)
            }

            SystemStatus.logEvent("TaskIterator", "yahan tak sab kuch changa si 11111111")
            val zonedEndTime = endTime.atZone(ZoneId.systemDefault()) // Use system time zone
            val loggerTimeMs = zonedEndTime.toInstant().toEpochMilli() - 60_000
            val currentTimeMs = System.currentTimeMillis()
            val delayMs = (loggerTimeMs - currentTimeMs).coerceAtLeast(0)
            SystemStatus.logEvent("TaskIterator", "Calculated delayMs: $delayMs, loggerTimeMs: $loggerTimeMs, currentTimeMs: $currentTimeMs")

            val maxDelayMs = 120_000L // 2-minute cap
            val effectiveDelayMs = delayMs.coerceAtMost(maxDelayMs)
            if (delayMs > maxDelayMs) {
                SystemStatus.logEvent("TaskIterator", "Warning: DelayMs $delayMs exceeds max cap, using $effectiveDelayMs")
            }
            SystemStatus.logEvent("TaskIterator", "Effective delayMs: $effectiveDelayMs, EndTime: $endTime")

            if (effectiveDelayMs > 0) {
                SystemStatus.logEvent("TaskIterator", "yahan tak sab kuch changa si 3333333")
                try {
                    SystemStatus.logEvent("TaskIterator", "Starting delay: $effectiveDelayMs ms")
                    delay(effectiveDelayMs)
                    SystemStatus.logEvent("TaskIterator", "yahan tak sab kuch changa si 444444444")
                    withContext(Dispatchers.Main) {
                        SystemStatus.logEvent("TaskIterator", "Triggering logger for task: ${task.title}")
                        ttsManager.speakHinglish("Log Your Data")
                        try {
                            triggerTaskLogger(task)
                            statusTimestamps.add(TaskStatus(task.id, task.title, "LOGGED"))
                        } catch (e: Exception) {
                            SystemStatus.logEvent("TaskIterator", "Failed to trigger logger for task ${task.title}: ${e.message}")
                            e.printStackTrace()
                        }
                    }
                    delay(6_000L)
                    withContext(Dispatchers.IO) {
                        taskDao.updateTask(task.toTaskEntity())
                    }
                } catch (e: CancellationException) {
                    SystemStatus.logEvent("TaskIterator", "Delay cancelled: ${e.message}")
                    throw e
                } catch (e: Exception) {
                    SystemStatus.logEvent("TaskIterator", "Error during delay: ${e.message}")
                    throw e
                }
            }
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskIterator", "Error in handleTaskExecution for ${task.title}: ${e.message}")
            statusTimestamps.add(TaskStatus(task.id, task.title, "FAILED", System.currentTimeMillis()))
        }
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

    fun updateLockScreenOverlay(task: Task, startTime: LocalDateTime, endTime: LocalDateTime) {
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val startTimeStr = startTime.format(timeFormatter)
        val endTimeStr = endTime.format(timeFormatter)
        val intent = Intent(context, TaskNotificationService::class.java).apply {
            putExtra("current_task_id", task.id)
            putExtra("start_time", startTimeStr)
            putExtra("end_time", endTimeStr)
        }
        TaskNotificationService.startService(context, intent)
        SystemStatus.logEvent("TaskIterator", "Lock screen overlay updated for: ${task.title}")
    }

    fun updateHomeScreenWidget(task: Task, startTime: LocalDateTime, endTime: LocalDateTime) {
        Log.d("TaskWidgetProvider", "Updating widget with tasks: ${task}, first task: ${task.title}")
        TaskWidgetProvider.updateHomeScreenWidget(context, listOf(task), startTime, endTime)
    }

    fun stop() {
        scope.cancel()
        statusTimestamps.add(TaskStatus("", "", "EOD"))
        SystemStatus.logEvent("TaskIterator", "TaskIterator stopped")
    }
}