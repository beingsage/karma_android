package com.technource.android.ETMS.micro

import android.content.Context
import android.util.Log
import com.technource.android.local.Task
import com.technource.android.local.SubTask
import com.technource.android.local.QuantMeasurement
import com.technource.android.local.TimeMeasurement
import com.technource.android.local.BinaryMeasurement
import com.technource.android.local.DeepWorkMeasurement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


object MockDataGenerator {
    private const val TAG = "MockDataGenerator"

    fun generateMockTasks(): List<Task> {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

        return listOf(
            Task(
                id = "task_1",
                title = "Complete Project Report",
                category = "Work",
                color = "#F44336",
                startTime = now.format(formatter),
                endTime = now.plusHours(2).format(formatter),
                duration = 7200,
                subtasks = listOf(
                    SubTask(
                        id = "subtask_1_1",
                        title = "Write Executive Summary",
                        measurementType = "Quant",
                        baseScore = 25,
                        completionStatus = 0.8f,
                        finalScore = 20f,
                        binary = null,
                        time = null,
                        quant = QuantMeasurement(targetValue = 500, targetUnit = "words", achievedValue = 400),
                        deepwork = null,
                        subTaskId = "subtask_id_1_1"
                    ),
                    SubTask(
                        id = "subtask_1_2",
                        title = "Create Data Visualizations",
                        measurementType = "Time",
                        baseScore = 15,
                        completionStatus = 0.5f,
                        finalScore = 7.5f,
                        binary = null,
                        time = TimeMeasurement(setDuration = 60, timeSpent = 30),
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_1_2"
                    ),
                    SubTask(
                        id = "subtask_1_3",
                        title = "Proofread Document",
                        measurementType = "Binary",
                        baseScore = 10,
                        completionStatus = 0f,
                        finalScore = 0f,
                        binary = BinaryMeasurement(completed = false),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_1_3"
                    )
                ),
                taskScore = 50f,
                completionStatus = 0.5f,
                taskId = "task_id_1"
            ),
            Task(
                id = "task_2",
                title = "Morning Workout",
                category = "Routine",
                color = "#4CAF50",
                startTime = now.plusHours(3).format(formatter),
                endTime = now.plusHours(4).format(formatter),
                duration = 3600,
                subtasks = listOf(
                    SubTask(
                        id = "subtask_2_1",
                        title = "Cardio",
                        measurementType = "Time",
                        baseScore = 20,
                        completionStatus = 1.0f,
                        finalScore = 20f,
                        binary = null,
                        time = TimeMeasurement(setDuration = 20, timeSpent = 20),
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_2_1"
                    ),
                    SubTask(
                        id = "subtask_2_2",
                        title = "Strength Training",
                        measurementType = "Quant",
                        baseScore = 30,
                        completionStatus = 0.67f,
                        finalScore = 20f,
                        binary = null,
                        time = null,
                        quant = QuantMeasurement(targetValue = 3, targetUnit = "sets", achievedValue = 2),
                        deepwork = null,
                        subTaskId = "subtask_id_2_2"
                    )
                ),
                taskScore = 40f,
                completionStatus = 0.8f,
                taskId = "task_id_2"
            ),
            Task(
                id = "task_3",
                title = "Study for Exam",
                category = "Study",
                color = "#2196F3",
                startTime = now.plusHours(5).format(formatter),
                endTime = now.plusHours(8).format(formatter),
                duration = 10800,
                subtasks = listOf(
                    SubTask(
                        id = "subtask_3_1",
                        title = "Review Notes",
                        measurementType = "DeepWork",
                        baseScore = 40,
                        completionStatus = 0.75f,
                        finalScore = 30f,
                        binary = null,
                        time = null,
                        quant = null,
                        deepwork = DeepWorkMeasurement(template = "focused_study", deepworkScore = 30),
                       subTaskId = "subtask_id_3_1"
                    ),
                    SubTask(
                        id = "subtask_3_2",
                        title = "Practice Problems",
                        measurementType = "Quant",
                        baseScore = 35,
                        completionStatus = 0.4f,
                        finalScore = 14f,
                        binary = null,
                        time = null,
                        quant = QuantMeasurement(targetValue = 20, targetUnit = "problems", achievedValue = 8),
                        deepwork = null,
                        subTaskId = "subtask_id_3_2"
                    ),
                    SubTask(
                        id = "subtask_3_3",
                        title = "Create Flashcards",
                        measurementType = "Binary",
                        baseScore = 15,
                        completionStatus = 1.0f,
                        finalScore = 15f,
                        binary = BinaryMeasurement(completed = true),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_3_3"
                    )
                ),
                taskScore = 59f,
                completionStatus = 0.65f,
                taskId = "task_id_3"
            )
        )
    }

    fun updateWidgetWithMockData(context: Context) {
        try {
            val mockTasks = generateMockTasks()
            val now = LocalDateTime.now()

            Log.d(TAG, "Updating widget with ${mockTasks.size} mock tasks")
            TaskWidgetProvider.updateHomeScreenWidget(
                context,
                mockTasks,
                now,
                now.plusHours(2)
            )

            Log.d(TAG, "Widget updated successfully with mock data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget with mock data: ${e.message}", e)
        }
    }
}