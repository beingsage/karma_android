package com.technource.android.module.settingsModule

import android.util.Log
import com.technource.android.local.BinaryMeasurement
import com.technource.android.local.DeepWorkMeasurement
import com.technource.android.local.QuantMeasurement
import com.technource.android.local.SubTask
import com.technource.android.local.TaskDao
import com.technource.android.local.TaskEntity
import com.technource.android.local.TaskStatus
import com.technource.android.local.TimeMeasurement
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * A utility class to populate the database with 15 mock tasks for testing the HomeScreen.
 * - Generates 15 tasks covering 24 hours (9 PM today to 9 PM tomorrow).
 * - Includes 5 debug tasks of 1-minute duration starting from the current local time.
 * - Tasks before the current time are set to LOGGED, MISSED, or SYSTEM_FAILURE.
 * - Tasks after debug tasks are set to UPCOMING, with one transitioning to RUNNING at a time.
 * - Ensures only one task is RUNNING at any moment.
 * - Assigns random, relevant metrics for subtasks to simulate a typical day.
 * - Ensures 5-10 tasks have 3-5 subtasks for realistic testing.
 * - Logs key actions for debugging and coherence checking.
 */
class TaskPopulatorTest @Inject constructor(
    private val taskDao: TaskDao
) {
    companion object {
        private const val TAG = "TaskPopulatorTest"
        private const val TOTAL_TASKS = 15
        private const val DEBUG_TASK_COUNT = 5
        private const val REGULAR_TASK_COUNT = TOTAL_TASKS - DEBUG_TASK_COUNT
    }

    /**
     * Populates the database with 15 tasks for the current day (9 PM to 9 PM).
     * - Clears existing tasks.
     * - Generates 10 regular tasks and 5 debug tasks, fitting around the current local time.
     * - Tasks before current time are LOGGED, MISSED, or SYSTEM_FAILURE.
     * - Debug tasks (1-minute) start at current time; others after are UPCOMING.
     * - Ensures 5-10 tasks have 3-5 subtasks for realistic testing.
     * - Logs task creation and insertion.
     */
    suspend fun populateTasksForTesting() {
        withContext(Dispatchers.IO) {
            // Clear existing tasks
            Log.e(TAG, "Clearing existing tasks from database")
            taskDao.clearTasks()

            val now = DateFormatter.millisToLocalDateTime(System.currentTimeMillis())
            val startOfPeriod = now.withHour(21).withMinute(0).withSecond(0).withNano(0) // 9 PM today
            val endOfPeriod = startOfPeriod.plusDays(1) // 9 PM tomorrow
            val startOfPeriodMillis = DateFormatter.localDateTimeToMillis(startOfPeriod)
            val endOfPeriodMillis = DateFormatter.localDateTimeToMillis(endOfPeriod)
            val currentTimeMillis = System.currentTimeMillis()

            val debugTaskDuration = 60 * 1000L // 1 minute for debug tasks
            val totalDurationMillis = endOfPeriodMillis - startOfPeriodMillis // 24 hours
            val regularTaskDuration = (totalDurationMillis - (DEBUG_TASK_COUNT * debugTaskDuration)) / REGULAR_TASK_COUNT // Evenly distribute remaining time

            val tasks = mutableListOf<TaskEntity>()
            val categories = listOf("work", "personal", "health", "learning")
            val colors = listOf("#FF6B6B", "#4CAF50", "#2196F3", "#FFC107")
            val pastStatuses = listOf(TaskStatus.LOGGED, TaskStatus.MISSED, TaskStatus.SYSTEM_FAILURE)

            // Calculate number of tasks before and after current time
            val timeElapsed = currentTimeMillis - startOfPeriodMillis
            val tasksBeforeNow = (REGULAR_TASK_COUNT * timeElapsed / totalDurationMillis).toInt().coerceAtMost(REGULAR_TASK_COUNT - 1)
            val tasksAfterDebug = REGULAR_TASK_COUNT - tasksBeforeNow

            var currentTimeSlot = startOfPeriodMillis
            var taskIndex = 1

            // Generate tasks before current time (LOGGED, MISSED, SYSTEM_FAILURE)
            Log.e(TAG, "Generating $tasksBeforeNow tasks before current time")
            repeat(tasksBeforeNow) {
                val category = categories[(taskIndex - 1) % categories.size]
                val color = colors[(taskIndex - 1) % categories.size]
                val status = pastStatuses[Random.nextInt(pastStatuses.size)]
                val subtaskCount = Random.nextInt(3, 6)
                val subtasks = generateSubtasks(taskIndex, subtaskCount)

                val task = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Task $taskIndex",
                    category = category,
                    color = color,
                    startTime = currentTimeSlot,
                    endTime = currentTimeSlot + regularTaskDuration,
                    duration = (regularTaskDuration / 1000).toInt(),
                    subtasks = subtasks,
                    taskScore = calculateTaskScore(subtasks),
                    taskId = "task_$taskIndex",
                    completionStatus = calculateCompletionStatus(subtasks),
                    timestamp = System.currentTimeMillis(),
                    status = status.name
                )

                Log.e(TAG, "Created past task ${task.title}: Category=$category, Status=${task.status}, Subtasks=$subtaskCount, StartTime=${DateFormatter.millisToLocalDateTime(task.startTime)}")
                tasks.add(task)
                currentTimeSlot += regularTaskDuration
                taskIndex++
            }

            // Generate 5 debug tasks (1 minute each) starting from current time
            Log.e(TAG, "Generating $DEBUG_TASK_COUNT debug tasks starting at current time")
            val debugStartTime = currentTimeMillis
            repeat(DEBUG_TASK_COUNT) { index ->
                val category = categories[(taskIndex - 1) % categories.size]
                val color = colors[(taskIndex - 1) % categories.size]
                val subtaskCount = Random.nextInt(3, 6)
                val subtasks = generateSubtasks(taskIndex, subtaskCount)

                val task = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Debug Task $taskIndex",
                    category = category,
                    color = color,
                    startTime = debugStartTime + (index * debugTaskDuration),
                    endTime = debugStartTime + ((index + 1) * debugTaskDuration),
                    duration = (debugTaskDuration / 1000).toInt(),
                    subtasks = subtasks,
                    taskScore = calculateTaskScore(subtasks),
                    taskId = "debug_task_$taskIndex",
                    completionStatus = calculateCompletionStatus(subtasks),
                    timestamp = System.currentTimeMillis(),
                    status = if (index == 0) TaskStatus.RUNNING.name else TaskStatus.UPCOMING.name // First debug task is RUNNING
                )

                Log.e(TAG, "Created debug task ${task.title}: Category=$category, Status=${task.status}, Subtasks=$subtaskCount, StartTime=${DateFormatter.millisToLocalDateTime(task.startTime)}")
                tasks.add(task)
                taskIndex++
            }

            // Generate remaining tasks after debug tasks (UPCOMING)
            currentTimeSlot = debugStartTime + (DEBUG_TASK_COUNT * debugTaskDuration)
            Log.e(TAG, "Generating $tasksAfterDebug tasks after debug tasks")
            repeat(tasksAfterDebug) {
                val category = categories[(taskIndex - 1) % categories.size]
                val color = colors[(taskIndex - 1) % categories.size]
                val subtaskCount = Random.nextInt(3, 6)
                val subtasks = generateSubtasks(taskIndex, subtaskCount)

                val task = TaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Task $taskIndex",
                    category = category,
                    color = color,
                    startTime = currentTimeSlot,
                    endTime = currentTimeSlot + regularTaskDuration,
                    duration = (regularTaskDuration / 1000).toInt(),
                    subtasks = subtasks,
                    taskScore = calculateTaskScore(subtasks),
                    taskId = "task_$taskIndex",
                    completionStatus = calculateCompletionStatus(subtasks),
                    timestamp = System.currentTimeMillis(),
                    status = TaskStatus.UPCOMING.name
                )

                Log.e(TAG, "Created upcoming task ${task.title}: Category=$category, Status=${task.status}, Subtasks=$subtaskCount, StartTime=${DateFormatter.millisToLocalDateTime(task.startTime)}")
                tasks.add(task)
                currentTimeSlot += regularTaskDuration
                taskIndex++
            }

            // Insert tasks into database
            Log.e(TAG, "Inserting ${tasks.size} tasks into database")
            taskDao.insertTasks(tasks)
            Log.e(TAG, "Task insertion completed")
        }
    }

    /**
     * Generates a list of subtasks with varied measurement types for a task.
     * - Includes binary, time, quant, and deepwork subtasks.
     * - Randomizes completion status and relevant metrics.
     * - Logs subtask creation.
     * @param taskNumber Used to create unique subtask titles and IDs.
     * @param subtaskCount Number of subtasks to generate (3-5).
     * @return List of SubTask objects.
     */
    private fun generateSubtasks(taskNumber: Int, subtaskCount: Int): List<SubTask> {
        val subtasks = mutableListOf<SubTask>()
        val measurementTypes = listOf("binary", "time", "quant", "deepwork")
        val random = Random

        Log.e(TAG, "Generating $subtaskCount subtasks for task $taskNumber")
        repeat(subtaskCount) { index ->
            val measurementType = measurementTypes[index % measurementTypes.size]
            val isCompleted = random.nextBoolean()
            val completionStatus = if (isCompleted) 1.0f else 0.0f
            val baseScore = when (measurementType) {
                "binary" -> 10
                "time" -> 15
                "quant" -> 20
                "deepwork" -> 25
                else -> 10
            }
            val finalScore = if (isCompleted) baseScore.toFloat() else 0.0f

            val subtask = when (measurementType) {
                "binary" -> SubTask(
                    id = "subtask_$taskNumber-$index",
                    title = "Binary Subtask $taskNumber-$index",
                    measurementType = "binary",
                    baseScore = baseScore,
                    completionStatus = completionStatus,
                    finalScore = finalScore,
                    binary = BinaryMeasurement(completed = isCompleted),
                    time = null,
                    quant = null,
                    deepwork = null,
                    subTaskId = "subtask_id_$taskNumber-$index"
                )
                "time" -> SubTask(
                    id = "subtask_$taskNumber-$index",
                    title = "Time Subtask $taskNumber-$index",
                    measurementType = "time",
                    baseScore = baseScore,
                    completionStatus = completionStatus,
                    finalScore = finalScore,
                    binary = null,
                    time = TimeMeasurement(
                        setDuration = 30,
                        timeSpent = if (isCompleted) 30 else random.nextInt(5, 20)
                    ),
                    quant = null,
                    deepwork = null,
                    subTaskId = "subtask_id_$taskNumber-$index"
                )
                "quant" -> SubTask(
                    id = "subtask_$taskNumber-$index",
                    title = "Quant Subtask $taskNumber-$index",
                    measurementType = "quant",
                    baseScore = baseScore,
                    completionStatus = completionStatus,
                    finalScore = finalScore,
                    binary = null,
                    time = null,
                    quant = QuantMeasurement(
                        targetValue = 100,
                        achievedValue = if (isCompleted) 100 else random.nextInt(20, 80),
                        targetUnit = "pages"
                    ),
                    deepwork = null,
                    subTaskId = "subtask_id_$taskNumber-$index"
                )
                "deepwork" -> SubTask(
                    id = "subtask_$taskNumber-$index",
                    title = "Deepwork Subtask $taskNumber-$index",
                    measurementType = "deepwork",
                    baseScore = baseScore,
                    completionStatus = completionStatus,
                    finalScore = finalScore,
                    binary = null,
                    time = null,
                    quant = null,
                    deepwork = DeepWorkMeasurement(
                        template = "focus",
                        deepworkScore = if (isCompleted) random.nextInt(80, 100) else random.nextInt(20, 60)
                    ),
                    subTaskId = "subtask_id_$taskNumber-$index"
                )
                else -> throw IllegalArgumentException("Unknown measurement type")
            }

            Log.e(TAG, "Created subtask ${subtask.title}: Type=$measurementType, Completed=$isCompleted, Score=$finalScore")
            subtasks.add(subtask)
        }

        return subtasks
    }

    /**
     * Calculates the task score based on subtasks' final scores.
     * @param subtasks List of SubTask objects.
     * @return Total score for the task.
     */
    private fun calculateTaskScore(subtasks: List<SubTask>): Float {
        val score = subtasks.sumOf { subtask ->
            subtask.finalScore.toDouble()
        }.toFloat()
        Log.e(TAG, "Calculated task score: $score from ${subtasks.size} subtasks")
        return score
    }

    /**
     * Calculates the task completion status based on subtasks.
     * @param subtasks List of SubTask objects.
     * @return Completion status as a float (0.0 to 1.0).
     */
    private fun calculateCompletionStatus(subtasks: List<SubTask>): Float {
        val status = if (subtasks.isEmpty()) {
            0.0f
        } else {
            val completedCount = subtasks.count { it.completionStatus == 1.0f }
            completedCount.toFloat() / subtasks.size
        }
        Log.e(TAG, "Calculated completion status: $status from ${subtasks.size} subtasks")
        return status
    }

    /**
     * Updates tasks to cycle through statuses for testing.
     * - Updates tasks before current time to LOGGED, MISSED, or SYSTEM_FAILURE.
     * - Ensures only one task is RUNNING at a time, transitioning from UPCOMING to RUNNING.
     * - After RUNNING, tasks move to LOGGED, MISSED, or SYSTEM_FAILURE.
     * - Runs every 10 seconds to simulate status changes.
     * - Logs status updates and task changes.
     */
    suspend fun updateTasksForTesting() {
        withContext(Dispatchers.IO) {
            val startTime = DateFormatter.getStartOfDayMillis()
            val endTime = DateFormatter.getStartOfNextDayMillis()
            val pastStatuses = listOf(TaskStatus.LOGGED, TaskStatus.MISSED, TaskStatus.SYSTEM_FAILURE)

            Log.e(TAG, "Starting task status update loop")
            while (true) {
                val tasks = taskDao.getTodayTasks(startTime, endTime)
                if (tasks.isEmpty()) {
                    Log.e(TAG, "No tasks found for update, waiting 10 seconds")
                    delay(10.seconds)
                    continue
                }

                val now = System.currentTimeMillis()
                val pastTasks = tasks.filter { it.endTime < now }
                val currentAndFutureTasks = tasks.filter { it.endTime >= now }.sortedBy { it.startTime }
                val runningTask = currentAndFutureTasks.firstOrNull { it.status == TaskStatus.RUNNING.name }
                val upcomingTasks = currentAndFutureTasks.filter { it.status == TaskStatus.UPCOMING.name }

                // Update past tasks
                Log.e(TAG, "Updating ${pastTasks.size} past tasks")
                pastTasks.forEach { task ->
                    if (task.status !in pastStatuses.map { it.name }) {
                        val newStatus = pastStatuses[Random.nextInt(pastStatuses.size)]
                        val updatedSubtasks = task.subtasks?.map { subtask ->
                            val newCompletionStatus = when (newStatus) {
                                TaskStatus.LOGGED -> 1.0f
                                TaskStatus.MISSED, TaskStatus.SYSTEM_FAILURE -> 0.0f
                                else -> subtask.completionStatus
                            }
                            val newFinalScore = if (newCompletionStatus == 1.0f) subtask.baseScore.toFloat() else 0.0f
                            subtask.copy(
                                completionStatus = newCompletionStatus,
                                finalScore = newFinalScore
                            )
                        } ?: emptyList()

                        val updatedTask = task.copy(
                            status = newStatus.name,
                            subtasks = updatedSubtasks,
                            completionStatus = calculateCompletionStatus(updatedSubtasks),
                            taskScore = calculateTaskScore(updatedSubtasks)
                        )
                        Log.e(TAG, "Updated past task ${task.title}: NewStatus=${newStatus.name}, Subtasks=${updatedSubtasks.size}")
                        taskDao.updateTask(updatedTask)
                    }
                }

                // Update current and future tasks
                if (runningTask == null && upcomingTasks.isNotEmpty()) {
                    // No running task, select the next UPCOMING task
                    val nextTask = upcomingTasks.first()
                    val updatedSubtasks = nextTask.subtasks?.map { subtask ->
                        val newCompletionStatus = if (Random.nextBoolean()) 1.0f else subtask.completionStatus
                        val newFinalScore = if (newCompletionStatus == 1.0f) subtask.baseScore.toFloat() else 0.0f
                        subtask.copy(
                            completionStatus = newCompletionStatus,
                            finalScore = newFinalScore
                        )
                    } ?: emptyList()

                    val updatedTask = nextTask.copy(
                        status = TaskStatus.RUNNING.name,
                        subtasks = updatedSubtasks,
                        completionStatus = calculateCompletionStatus(updatedSubtasks),
                        taskScore = calculateTaskScore(updatedSubtasks)
                    )
                    Log.e(TAG, "Set task ${nextTask.title} to RUNNING: StartTime=${DateFormatter.millisToLocalDateTime(nextTask.startTime)}")
                    taskDao.updateTask(updatedTask)
                } else if (runningTask != null) {
                    // Move RUNNING task to LOGGED, MISSED, or SYSTEM_FAILURE
                    val newStatus = pastStatuses[Random.nextInt(pastStatuses.size)]
                    val updatedSubtasks = runningTask.subtasks?.map { subtask ->
                        val newCompletionStatus = when (newStatus) {
                            TaskStatus.LOGGED -> 1.0f
                            TaskStatus.MISSED, TaskStatus.SYSTEM_FAILURE -> 0.0f
                            else -> subtask.completionStatus
                        }
                        val newFinalScore = if (newCompletionStatus == 1.0f) subtask.baseScore.toFloat() else 0.0f
                        subtask.copy(
                            completionStatus = newCompletionStatus,
                            finalScore = newFinalScore
                        )
                    } ?: emptyList()

                    val updatedTask = runningTask.copy(
                        status = newStatus.name,
                        subtasks = updatedSubtasks,
                        completionStatus = calculateCompletionStatus(updatedSubtasks),
                        taskScore = calculateTaskScore(updatedSubtasks)
                    )
                    Log.e(TAG, "Updated running task ${runningTask.title} to ${newStatus.name}: Subtasks=${updatedSubtasks.size}")
                    taskDao.updateTask(updatedTask)
                }

                Log.e(TAG, "Completed update cycle, waiting 10 seconds")
                delay(10.seconds)
            }
        }
    }
}