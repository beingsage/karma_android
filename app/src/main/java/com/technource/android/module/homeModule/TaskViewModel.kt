package com.technource.android.module.homeModule

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.map

import com.technource.android.local.AppDatabase
import com.technource.android.local.TaskEntity
import com.technource.android.local.Task
import com.technource.android.local.TaskStatus
import com.technource.android.local.toTasks
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    database: AppDatabase
) : ViewModel() {

    private val taskDao = database.taskDao()

    // Directly observe LiveData from TaskDao
    val tasks: LiveData<List<Task>> = taskDao.getTasksLiveData().map { taskEntities ->
        taskEntities.toTasks()
    }

    private val _filteredTasks = MutableLiveData<List<Task>>()
    val filteredTasks: LiveData<List<Task>> = _filteredTasks

    private val _todayFormatted = MutableLiveData<String>()
    val todayFormatted: LiveData<String> = _todayFormatted

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Add new private variables
    private val _missedCount = MutableLiveData<Int>(0)
    val missedCount: LiveData<Int> = _missedCount

    private val _systemFailureCount = MutableLiveData<Int>(0)
    val systemFailureCount: LiveData<Int> = _systemFailureCount

    private val _netScore = MutableLiveData<Int>(0)
    val netScore: LiveData<Int> = _netScore

    private var currentCategory: String? = null

    init {
        updateTodayDate()
        fetchTasks()
    }

    fun fetchTasks() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            SystemStatus.logEvent("TaskViewModel", "fetchTasks called, isLoading set to true")
            try {
                withContext(Dispatchers.IO) {
                    val taskEntities = taskDao.getTasks()
                    SystemStatus.logEvent("TaskViewModel", "Fetched ${taskEntities.size} tasks from DB")
                    
                    val tasks = taskEntities.mapNotNull { entity ->
                        try {
                            val task = entity.toTask()
                            // Verify essential fields
                            if (task.title.isNullOrEmpty() || task.color.isNullOrEmpty()) {
                                // SystemStatus.logEvent("TaskViewModel", "Invalid task data: ${task.id} - missing title or color")
                                null
                            } else {
                                task
                            }
                        } catch (e: Exception) {
                            // SystemStatus.logEvent("TaskViewModel", "Error converting entity: ${e.message}, Entity ID: ${entity.id}")
                            null
                        }
                    }
                    
                    // SystemStatus.logEvent("TaskViewModel", "Mapped ${tasks.size} valid tasks after filtering invalid entries")
                    
                    val isCurrentDateResult = isCurrentDate(tasks)
                    // SystemStatus.logEvent("TaskViewModel", "isCurrentDate check result: $isCurrentDateResult for ${tasks.size} tasks")

                    if (tasks.isNotEmpty() && isCurrentDateResult) {
                        // Removed _tasks.postValue(tasks) as _tasks is no longer used
                        // SystemStatus.logEvent("TaskViewModel", "Valid tasks fetched and current date confirmed, tasks ready for filtering")
                        
                        applyFilters(tasks)
                        // SystemStatus.logEvent("TaskViewModel", "Applied filters to tasks")
                    } else {
                        _filteredTasks.postValue(emptyList())
                        
                        if (tasks.isEmpty()) {
                            // SystemStatus.logEvent("TaskViewModel", "No valid tasks found after mapping")
                            _errorMessage.postValue("No valid tasks found")
                        } else {
                            // SystemStatus.logEvent("TaskViewModel", "Tasks exist but none for today's date")
                            _errorMessage.postValue("No timetable for today")
                        }
                    }
                }
            } catch (e: Exception) {
                // SystemStatus.logEvent("TaskViewModel", "Error fetching tasks: ${e.message}")
                _errorMessage.postValue("Error fetching tasks: ${e.message}")
                _filteredTasks.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
                // SystemStatus.logEvent("TaskViewModel", "fetchTasks finished, isLoading set to false")
            }
        }
    }

    fun filterTasksByCategory(category: String?) {
        currentCategory = category
        _filteredTasks.value?.let { applyFilters(it) }
    }

    private fun applyFilters(tasks: List<Task>) {
        val filtered = if (currentCategory != null) {
            tasks.filter { it.category.equals(currentCategory, ignoreCase = true) }
        } else {
            tasks
        }
        _filteredTasks.postValue(filtered.sortedBy { parseDate(it.startTime).time })
    }

    private fun isCurrentDate(tasks: List<Task>): Boolean {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        val tasksForToday = tasks.filter { task ->
            try {
                // Just compare the date part from the ISO string
                val taskDate = task.startTime.substring(0, 10) // Get "YYYY-MM-DD" part
                val todayStr = SimpleDateFormat("yyyy-MM-dd").format(today.time)
                
                taskDate == todayStr
                
            } catch (e: Exception) {
                SystemStatus.logEvent("TaskViewModel", "Error parsing date for task ${task.id}: ${e.message}")
                false
            }
        }

        SystemStatus.logEvent("TaskViewModel", "isCurrentDate check for ${tasks.size} tasks, today is: ${today.time}")
        return tasksForToday.isNotEmpty()
    }

    // Helper function to convert month abbreviation to number
    private fun monthNameToNumber(month: String): Int {
        return when (month.lowercase()) {
            "jan" -> 1
            "feb" -> 2
            "mar" -> 3
            "apr" -> 4
            "may" -> 5
            "jun" -> 6
            "jul" -> 7
            "aug" -> 8
            "sep" -> 9
            "oct" -> 10
            "nov" -> 11
            "dec" -> 12
            else -> throw IllegalArgumentException("Invalid month abbreviation: $month")
        }
    }

    private fun updateTodayDate() {
        viewModelScope.launch(Dispatchers.Main) {
            val calendar = Calendar.getInstance()
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val monthName = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time)
            val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
            _todayFormatted.setValue("$dayOfMonth $monthName, $dayOfWeek")
        }
    }

    fun formatTime(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()) 
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault() // Convert to local timezone

            val date = inputFormat.parse(dateString.trim())
            if (date != null) {
                outputFormat.format(date)
            } else {
                Log.e("TaskViewModel", "Failed to parse date: $dateString")
                ""
            }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Error formatting time: $dateString", e)
            ""
        }
    }

    fun getCurrentActiveTask(): Task? {
        val now = Calendar.getInstance().time

        return _filteredTasks.value?.firstOrNull { task ->
            val startTime = parseDate(task.startTime)
            val endTime = task.endTime?.let { parseDate(it) } ?: Calendar.getInstance().apply {
                time = startTime
                add(Calendar.HOUR, 1) // Default 1 hour duration if no end time
            }.time

            now.after(startTime) && now.before(endTime) && task.completionStatus < 1f
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // Update parseDate function to handle IST format
    private fun parseDate(dateString: String): Date {
        try {
            // Directly parse ISO format
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            formatter.timeZone = TimeZone.getTimeZone("UTC")
            return formatter.parse(dateString) ?: Date()
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskViewModel", "Error parsing date: $dateString - ${e.message}")
            return Date()
        }
    }

    // Mapping function from TaskEntity to Task
    private fun TaskEntity.toTask(): Task {
        return Task(
            id = id,
            title = title,
            category = category,
            color = color,
            startTime = startTime, // Already in IST format
            endTime = endTime,     // Already in IST format
            duration = duration,
            subtasks = subtasks,
            taskScore = taskScore,
            taskId = taskId,
            isExpanded = false,
            completionStatus = completionStatus
        )
    }

    // Modify updateTaskStats() to include these counts
    private fun updateTaskStats(tasks: List<Task>) {
        var missed = 0
        var systemFailure = 0
        var totalScore = 0

        tasks.forEach { task ->
            when (task.status) {
                TaskStatus.MISSED -> missed++
                TaskStatus.SYSTEM_FAILURE -> systemFailure++
                TaskStatus.UPCOMING -> TODO()
                TaskStatus.RUNNING -> TODO()
                TaskStatus.LOGGED -> TODO()
                null -> TODO()
            }
            totalScore += task.taskScore.toInt()
        }

        _missedCount.postValue(missed)
        _systemFailureCount.postValue(systemFailure)
        _netScore.postValue(totalScore)
    }
}