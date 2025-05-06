package com.technource.android.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for interacting with the tasks table in Room.
 * - Represents: Database operations for TaskEntity (e.g., insert, update, delete, query).
 * - Uses DateFormatter: For defining time ranges in queries (e.g., getTodayTasksFlow).
 */
@Dao
interface TaskDao {
    /**
     * Inserts a list of tasks into the database.
     * @param tasks List of TaskEntity objects to insert.
     * @return Nothing (suspend function).
     * Use Case:
     * - Used by TaskPopulator to insert generated tasks.
     * - Used after fetching tasks from API to store them locally.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    /**
     * Retrieves all tasks from the database.
     * @return List of TaskEntity objects.
     * Use Case:
     * - Used for debugging or when all tasks are needed without filtering.
     */
    @Query("SELECT * FROM tasks")
    suspend fun getTasks(): List<TaskEntity>

    /**
     * Updates a single task in the database.
     * @param task TaskEntity object to update.
     * @return Nothing (suspend function).
     * Use Case:
     * - Used when a task's properties (e.g., completionStatus) are updated.
     */
    @Update
    suspend fun updateTask(task: TaskEntity)

    /**
     * Deletes tasks with a timestamp older than the specified time.
     * @param currentTime Epoch milliseconds to compare against TaskEntity.timestamp.
     * @return Nothing (suspend function).
     * Use Case:
     * - Used to clean up expired tasks (e.g., older than a certain time).
     */
    @Query("DELETE FROM tasks WHERE timestamp < :currentTime")
    suspend fun deleteExpired(currentTime: Long)

    /**
     * Deletes all tasks from the database.
     * @return Nothing (suspend function).
     * Use Case:
     * - Used by TaskPopulator to clear existing tasks before populating new ones.
     */
    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    /**
     * Retrieves all tasks as a Flow for reactive updates.
     * @return Flow emitting a List of TaskEntity objects whenever the table changes.
     * Use Case:
     * - Used for live updates in the UI when the task list changes.
     */
    @Query("SELECT * FROM tasks")
    fun getTasksFlow(): Flow<List<TaskEntity>>

    /**
     * Retrieves tasks around a specific task (for pagination or context).
     * @param currentTaskId ID of the reference task.
     * @param limit Number of tasks to fetch (split evenly before and after the reference task).
     * @return List of TaskEntity objects ordered by startTime.
     * Use Case:
     * - Used in HomeScreen to display a subset of tasks around the current task.
     * Note: startTime is stored as Long (epoch milliseconds) in TaskEntity.
     */
    @Query("""
        SELECT * FROM tasks
        WHERE startTime >= (
            SELECT MIN(startTime) FROM (
                SELECT startTime FROM tasks 
                WHERE startTime <= (SELECT startTime FROM tasks WHERE id = :currentTaskId)
                ORDER BY startTime DESC LIMIT :limit / 2
            ) AS prev_tasks
        ) 
        AND startTime <= (
            SELECT MAX(startTime) FROM (
                SELECT startTime FROM tasks 
                WHERE startTime >= (SELECT startTime FROM tasks WHERE id = :currentTaskId)
                ORDER BY startTime ASC LIMIT :limit / 2 + 1
            ) AS next_tasks
        )
        ORDER BY startTime ASC
    """)
    suspend fun getTasksAround(currentTaskId: String, limit: Int): List<TaskEntity>

    /**
     * Retrieves tasks for the current day based on a time range.
     * @param startTime Start of the time range in epoch milliseconds (e.g., start of day).
     * @param endTime End of the time range in epoch milliseconds (e.g., start of next day).
     * @return List of TaskEntity objects within the time range.
     * Use Case:
     * - Used in StatsViewModel to fetch tasks for calculating daily stats.
     * - DateFormatter: Use getStartOfDayMillis() and getStartOfNextDayMillis() to set the range.
     */
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :startTime AND :endTime")
    suspend fun getTodayTasks(startTime: Long, endTime: Long): List<TaskEntity>

    /**
     * Retrieves tasks for the current day as a Flow for reactive updates.
     * @param startTime Start of the time range in epoch milliseconds (e.g., start of day).
     * @param endTime End of the time range in epoch milliseconds (e.g., start of next day).
     * @return Flow emitting a List of TaskEntity objects within the time range whenever the table changes.
     * Use Case:
     * - Used in StatsViewModel to observe tasks for the current day and update stats reactively.
     * - DateFormatter: Use getStartOfDayMillis() and getStartOfNextDayMillis() to set the range.
     */
    @SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
    @Query("SELECT * FROM tasks WHERE startTime BETWEEN :startTime AND :endTime")
    fun getTodayTasksFlow(startTime: Long, endTime: Long): Flow<List<TaskEntity>>

    // New query to fetch tasks by status
    @Query("SELECT * FROM tasks WHERE status = :status")
    fun getTasksByStatusFlow(status: String): Flow<List<TaskEntity>>
}