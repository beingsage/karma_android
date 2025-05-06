package com.technource.android.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.technource.android.utils.DateFormatter

/**
 * Room Entity representing a task in the database.
 * - Represents: A task stored in the "tasks" table.
 * - Accepts: Fields like id, title, startTime (as Long), etc.
 * - Returns: TaskEntity object for Room operations.
 * - Uses DateFormatter: In toTasks() to convert startTime/endTime from Long to ISO 8601 strings.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val color: String,
    val startTime: Long, // Epoch milliseconds (changed from String)
    val endTime: Long,   // Epoch milliseconds (changed from String)
    val duration: Int,   // Duration in seconds
    val subtasks: List<SubTask>?,
    val taskScore: Float,
    val taskId: String,
    val completionStatus: Float,
    val timestamp: Long,  // For expiration
    val status: String? = null // Store status as String (enum name)
)

/**
 * Converts a List<TaskEntity> to a List<Task>.
 * @receiver List<TaskEntity> from the database.
 * @return List<Task> for use in the app (e.g., API models, UI).
 * Use Case:
 * - Used in StatsViewModel and HomeScreen to convert database entities to API-compatible models.
 * - DateFormatter: Converts startTime/endTime from Long to ISO 8601 strings.
 */
fun List<TaskEntity>.toTasks(): List<Task> {
    return map {
        Task(
            id = it.id,
            title = it.title,
            category = it.category,
            color = it.color,
            startTime = DateFormatter.formatIsoDateTime(DateFormatter.millisToLocalDateTime(it.startTime)),
            endTime = DateFormatter.formatIsoDateTime(DateFormatter.millisToLocalDateTime(it.endTime)),
            duration = it.duration,
            subtasks = it.subtasks,
            taskScore = it.taskScore,
            taskId = it.taskId,
            completionStatus = it.completionStatus,
            status = it.status?.let { status -> TaskStatus.valueOf(status) }
        )
    }
}

/**
 * Converts a Task to a TaskEntity for storage in Room.
 * @receiver Task object (e.g., from API).
 * @param timestamp Optional timestamp for expiration (defaults to current time).
 * @return TaskEntity object for Room storage.
 * Use Case:
 * - Used when saving API-fetched tasks to the local database.
 * - DateFormatter: Converts startTime/endTime from ISO 8601 strings to Long (epoch milliseconds).
 */
fun Task.toTaskEntity(timestamp: Long = System.currentTimeMillis()): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        category = category,
        color = color,
        startTime = DateFormatter.localDateTimeToMillis(DateFormatter.parseIsoDateTime(startTime)),
        endTime = DateFormatter.localDateTimeToMillis(DateFormatter.parseIsoDateTime(endTime)),
        duration = duration,
        subtasks = subtasks,
        taskScore = taskScore,
        taskId = taskId ?: "",
        completionStatus = completionStatus,
        timestamp = timestamp,
        status = status?.name // Store enum as String
    )
}