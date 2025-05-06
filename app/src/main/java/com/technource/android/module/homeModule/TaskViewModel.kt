package com.technource.android.module.homeModule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technource.android.local.AppDatabase
import com.technource.android.local.TaskEntity
import com.technource.android.local.Task
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

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    private val _filteredTasks = MutableLiveData<List<Task>>()
    val filteredTasks: LiveData<List<Task>> = _filteredTasks

    private val _todayFormatted = MutableLiveData<String>()
    val todayFormatted: LiveData<String> = _todayFormatted

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentCategory: String? = null

    init {
        updateTodayDate()
        fetchTasks()
    }

    fun fetchTasks() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                withContext(Dispatchers.IO) {
                    val taskEntities = taskDao.getTasks()
                    val tasks = taskEntities.map { it.toTask() }

                    if (tasks.isNotEmpty() && isCurrentDate(tasks)) {
                        _tasks.postValue(tasks)
                        applyFilters(tasks)
                    } else {
                        _tasks.postValue(emptyList())
                        _filteredTasks.postValue(emptyList())
                        _errorMessage.postValue("No timetable for today")
                    }
                }
            } catch (e: Exception) {
                _errorMessage.postValue("Error fetching tasks: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun filterTasksByCategory(category: String?) {
        currentCategory = category
        _tasks.value?.let { applyFilters(it) }
    }

    private fun applyFilters(tasks: List<Task>) {
        val filtered = if (currentCategory != null) {
            tasks.filter { it.category.equals(currentCategory, ignoreCase = true) }
        } else {
            tasks
        }

        // Sort by start time
        val sorted = filtered.sortedBy { parseDate(it.startTime).time }
        _filteredTasks.postValue(sorted)
    }

    private fun isCurrentDate(tasks: List<Task>): Boolean {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        return tasks.any { task ->
            val taskDate = parseDate(task.startTime)
            val taskCalendar = Calendar.getInstance().apply { time = taskDate }

            taskCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    taskCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                    taskCalendar.get(Calendar.DAY_OF_MONTH) == today.get(Calendar.DAY_OF_MONTH)
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
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(dateString) ?: return ""

            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            return outputFormat.format(date)
        } catch (e: Exception) {
            return ""
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

    private fun parseDate(dateString: String): Date {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.parse(dateString) ?: Date()
    }

    // Mapping function from TaskEntity to Task
    private fun TaskEntity.toTask(): Task {
        return Task(
            id = id,
            title = title,
            category = category,
            color = color,
            startTime = DateFormatter.formatIsoDateTime(DateFormatter.millisToLocalDateTime(startTime)),
            endTime = DateFormatter.formatIsoDateTime(DateFormatter.millisToLocalDateTime(endTime)),duration = duration,
            subtasks = subtasks,
            taskScore = taskScore,
            taskId = taskId,
            isExpanded = false,
            completionStatus = completionStatus
        )
    }
}
