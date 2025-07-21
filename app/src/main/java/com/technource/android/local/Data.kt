package com.technource.android.local

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName
import java.io.Serializable


// Enums for task status
enum class TaskStatus {
    UPCOMING, RUNNING, LOGGED, MISSED, SYSTEM_FAILURE
}

/**
 * Response model for API calls fetching tasks.
 * - Represents: The response structure from the API endpoint (e.g., "api/timetable").
 * - Accepts: JSON object with success boolean and TaskData.
 * - Returns: TaskResponse object with parsed data.
 * - Uses DateFormatter: Indirectly through Task.toTaskEntity() for converting startTime/endTime.
 */
data class TaskResponse(
    val success: Boolean,
    val data: TaskData
) : Serializable

/**
 * Data model for the task data within TaskResponse.
 * - Represents: The main data payload containing tasks for a specific date.
 * - Accepts: JSON object with id, date, temperature, tasks list, dayScore, and version.
 * - Returns: TaskData object with parsed fields.
 * - Uses DateFormatter: Indirectly through Task objects for time handling.
 */
data class TaskData(
    @SerializedName("_id") val id: String,
    val date: String, // Typically in ISO 8601 format (e.g., "2025-05-02T00:00:00.000Z")
    val temperature: Float,
    val tasks: List<Task>,
    val dayScore: Float,
    @SerializedName("__v") val version: Int
) : Serializable

/**
 * Model for a single task, used for both API and local data.
 * - Represents: A task with its properties (e.g., title, time, category).
 * - Accepts: JSON object with task details (startTime/endTime as ISO 8601 strings).
 * - Returns: Task object with parsed fields.
 * - Uses DateFormatter: In toTaskEntity() to convert startTime/endTime strings to Long (epoch milliseconds).
 */
data class Task(
    val id: String,
    val title: String,
    val category: String,
    val color: String,
    val startTime: String, // ISO 8601 string (e.g., "2025-05-02T14:30:00.000Z")
    val endTime: String,   // ISO 8601 string
    val duration: Int,     // Duration in seconds
    val subtasks: List<SubTask>?,
    var taskScore: Float,
    @SerializedName("_id") val taskId: String,
    @Ignore @Transient var isExpanded: Boolean = false, // For UI state, not stored in Room
    var completionStatus: Float = 0f,         // Tracks completion dynamically
    var status: TaskStatus? = null // Added status field, nullable as it’s not from backend
) : Serializable

/**
 * Model for a subtask within a Task.
 * - Represents: A subtask with its properties (e.g., title, measurement type, scores).
 * - Accepts: JSON object with subtask details.
 * - Returns: SubTask object with parsed fields.
 * - Uses DateFormatter: Not directly, but TimeMeasurement may involve time-related calculations.
 */
data class SubTask(
    val id: String,
    val title: String,
    val measurementType: String,
    val baseScore: Int,
    var completionStatus: Float,
    var finalScore: Float,
    val binary: BinaryMeasurement?,
    val time: TimeMeasurement?,
    val quant: QuantMeasurement?,
    val deepwork: DeepWorkMeasurement?,
    @SerializedName("_id") val subTaskId: String
) : Serializable

/**
 * Measurement model for binary subtasks (e.g., completed or not).
 * - Represents: A simple yes/no measurement.
 * - Accepts: JSON object with completed boolean.
 * - Returns: BinaryMeasurement object.
 * - Uses DateFormatter: Not applicable.
 */
data class BinaryMeasurement(
    var completed: Boolean?
) : Serializable

/**
 * Measurement model for time-based subtasks.
 * - Represents: A subtask with a duration goal and time spent.
 * - Accepts: JSON object with setDuration and timeSpent.
 * - Returns: TimeMeasurement object.
 * - Uses DateFormatter: Potentially for converting timeSpent to a display format if needed in UI.
 */
data class TimeMeasurement(
    val setDuration: Int, // Duration in seconds
    var timeSpent: Int?   // Time spent in seconds
) : Serializable

/**
 * Measurement model for quantity-based subtasks.
 * - Represents: A subtask with a target value and unit (e.g., "5 pages").
 * - Accepts: JSON object with targetValue, targetUnit, and achievedValue.
 * - Returns: QuantMeasurement object.
 * - Uses DateFormatter: Not applicable.
 */
data class QuantMeasurement(
    val targetValue: Int,
    val targetUnit: String,
    var achievedValue: Int?
) : Serializable

/**
 * Measurement model for deep work subtasks.
 * - Represents: A subtask with a deep work template and score.
 * - Accepts: JSON object with template and deepworkScore.
 * - Returns: DeepWorkMeasurement object.
 * - Uses DateFormatter: Not applicable.
 */
data class DeepWorkMeasurement(
    var template: String,
    var deepworkScore: Int?
) : Serializable

/**
 * Model for a timetable containing a list of tasks.
 * - Represents: A collection of tasks for scheduling.
 * - Accepts: List of Task objects (defaults to empty list).
 * - Returns: TimeTable object.
 * - Uses DateFormatter: Indirectly through Task objects for time handling.
 */
data class TimeTable(
    val tasks: List<Task> = emptyList()
) : Serializable