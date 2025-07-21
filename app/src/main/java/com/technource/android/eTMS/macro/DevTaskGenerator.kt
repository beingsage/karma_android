package com.technource.android.eTMS.macro

import com.technource.android.local.BinaryMeasurement
import com.technource.android.local.DeepWorkMeasurement
import com.technource.android.local.QuantMeasurement
import com.technource.android.local.SubTask
import com.technource.android.local.Task
import com.technource.android.local.TaskDao
import com.technource.android.local.TimeMeasurement
import com.technource.android.local.toTaskEntity
import com.technource.android.system_status.SystemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.random.Random

object DevTaskGenerator {
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private val categories = listOf("routine", "study", "work")
    private val measurementTypes = listOf("deepwork", "time", "quant", "binary")
    private val quantUnits = listOf(
        "pages", "pushups", "articles", "problems", "exercises",
        "chapters", "questions", "slides", "minutes", "sets",
        "laps", "words", "paragraphs", "sections", "reps"
    )

    suspend fun generateDevTasks(taskDao: TaskDao) {
        withContext(Dispatchers.IO) {
            try {
                // Clear existing tasks to avoid duplication
                taskDao.clearTasks()

                // Generate 3-4 tasks
                val taskCount = Random.nextInt(3, 5)
                val tasks = mutableListOf<Task>()
                val startTimes = mutableListOf<LocalDateTime>()
                val durations = mutableListOf<Int>() // Store durations for each task
                var currentTime = LocalDateTime.now(ZoneId.of("UTC"))

                for (i in 1..taskCount) {
                    startTimes.add(currentTime)
                    val durationSeconds = Random.nextInt(80, 120)
                    durations.add(durationSeconds) // Save duration for this task
                    currentTime = currentTime.plusSeconds(durationSeconds.toLong())
                }

                // Now generate tasks with correct start/end times
                for (i in 0 until taskCount) {
                    val startTime = startTimes[i]
                    val endTime = if (i < taskCount - 1) startTimes[i + 1] else startTime.plusSeconds(durations[i].toLong())
                    val durationSeconds = durations[i] // Use the stored duration

                    // Generate 1-2 subtasks
                    val subTaskCount = Random.nextInt(1, 3) // 1 or 2 subtasks
                    val subtasks = mutableListOf<SubTask>()
                    for (j in 1..subTaskCount) {
                        val measurementType = measurementTypes.random()
                        val baseScore = Random.nextInt(10, 51) // Random base score 10-50
                        val subTask = when (measurementType) {
                            "deepwork" -> SubTask(
                                id = UUID.randomUUID().toString(),
                                subTaskId = UUID.randomUUID().toString() ,
                                title = "Deep Work Subtask $j",
                                measurementType = "deepwork",
                                baseScore = baseScore,
                                deepwork = DeepWorkMeasurement(
                                    template = "Proof: {cloudinary_url}",
                                    deepworkScore = null
                                ),
                                binary = BinaryMeasurement(completed = false),
                                quant =  QuantMeasurement( targetValue = 0,  targetUnit = "",  achievedValue = 0),
                                time =  TimeMeasurement(setDuration = 0, timeSpent = 0),
                                completionStatus = 0f,
                                finalScore = 0f,
                            )
                            "time" -> SubTask(
                                id = UUID.randomUUID().toString(),
                                subTaskId = UUID.randomUUID().toString() ,
                                title = "Time Subtask $j",
                                measurementType = "time",
                                baseScore = baseScore,
                                time = TimeMeasurement(
                                    setDuration = durationSeconds / 60, // Match task duration
                                    timeSpent = 0
                                ),
                                quant =  QuantMeasurement( targetValue = 0,  targetUnit = "",  achievedValue = 0),
                                binary = BinaryMeasurement(completed = false),
                                deepwork = DeepWorkMeasurement( template =  "", deepworkScore = 0),
                                completionStatus = 0f,
                                finalScore = 0f
                            )
                            "quant" -> SubTask(
                                id = UUID.randomUUID().toString(),
                                subTaskId = UUID.randomUUID().toString() ,
                                title = "Quant Subtask $j",
                                measurementType = "quant",
                                baseScore = baseScore,
                                quant = QuantMeasurement(
                                    targetValue = Random.nextInt(1, 10),
                                    targetUnit = quantUnits.random(), // Use random unit
                                    achievedValue = 0
                                ),
                                time =  TimeMeasurement(setDuration = 0, timeSpent = 0),
                                binary = BinaryMeasurement(completed = false),
                                deepwork = DeepWorkMeasurement( template =  "", deepworkScore = 0),
                                completionStatus = 0f,
                                finalScore = 0f
                            )
                            "binary" -> SubTask(
                                id = UUID.randomUUID().toString(),
                                subTaskId = UUID.randomUUID().toString() ,
                                title = "Binary Subtask $j",
                                measurementType = "binary",
                                baseScore = baseScore,
                                binary = BinaryMeasurement(completed = false),
                                quant =  QuantMeasurement( targetValue = 0,  targetUnit = "",  achievedValue = 0),
                                time =  TimeMeasurement(setDuration = 0, timeSpent = 0),
                                deepwork = DeepWorkMeasurement( template =  "", deepworkScore = 0),
                                completionStatus = 0f,
                                finalScore = 0f
                            )
                            else -> null
                        }
                        subTask?.let { subtasks.add(it) }
                    }

                    val task = Task(
                        id = UUID.randomUUID().toString(),
                        title = "Dev Task $i",
                        category = categories.random(), // Randomly select routine, study, or work
                        color = "#${String.format("%06X", Random.nextInt(0xFFFFFF))}",
                        startTime = startTime.format(isoFormatter),
                        endTime = endTime.format(isoFormatter),
                        duration = durationSeconds / 60, // Duration in minutes
                        subtasks = subtasks, // Include generated subtasks
                        taskScore = 0f,
                        taskId = UUID.randomUUID().toString(),
                        completionStatus = 0f,
                        status = null // Set to null, let DB handle it
                    )

                    tasks.add(task)
                }

                // Insert tasks into database with 7-day expiration
                val expirationTime = System.currentTimeMillis() + ETMSConfig.SEVEN_DAYS_MILLIS
                val taskEntities = tasks.map { it.toTaskEntity(expirationTime) }
                taskDao.insertTasks(taskEntities)

                SystemStatus.logEvent(
                    "DevTaskGenerator",
                    "Generated and inserted $taskCount dev tasks with subtasks into database"
                )
            } catch (e: Exception) {
                SystemStatus.logEvent(
                    "DevTaskGenerator",
                    "Error generating dev tasks: ${e.message}"
                )
            }
        }
    }
}