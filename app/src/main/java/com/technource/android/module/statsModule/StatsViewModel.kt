package com.technource.android.module.statsModule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.technource.android.R
import com.technource.android.local.AppDatabase
import com.technource.android.local.Task
import com.technource.android.local.toTasks
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime
import java.time.ZoneId
import javax.inject.Inject
import java.io.Serializable
import java.text.ParseException
import java.time.LocalDateTime
import java.time.format.ResolverStyle
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

enum class AchievementType {
    DEEP_WORK,
    EFFICIENCY,
    TASK_COMPLETION,
    WORK
}

data class Achievement(
    val name: String,
    val value: Float,
    val threshold: Float,
    val type: AchievementType,
    val description: String
)

data class Metric(
    val name: String,
    val value: String,
    val trend: Float,
    val detail: String,
    val icon: Int
)

data class ScorePoint(val time: String, val expected: Float, val achieved: Float)
data class DeepWorkPoint(val time: String, val required: Float, val achieved: Float)
data class TimeDistribution(val name: String, val value: Float, val color: String)
data class CategoryPerformance(val category: String, val scoreAchieved: Float, val scorePossible: Float)
data class SubtaskType(
    val type: String,
    val completed: Int, 
    val total: Int,
    val score: Float,
    val startTime: String = "" // Add startTime field
)
data class RewardPoint(
    val time: String,
    val score: Float,
    val startTime: String = "" // Add startTime field
)
data class HourlyProductivity(val time: String, val tasks: Int)

data class StatsData(
    val completionRate: Float,
    val tasks: List<Task>,
    val durationEfficiency: Float = 0f,
    val categoryCompletionRates: Map<String, Float> = emptyMap(),
    val totalScoreObtained: Float = 0.0f,
    val totalScoreAssociated: Float = 0.0f,
    val tasksBehindSchedule: Int = 0,
    val error: String? = null
) : Serializable

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {
    
    // Ensure direct database observation
    private val taskDao = database.taskDao()
    
    val tasks: LiveData<List<Task>> = taskDao.getTasksLiveData().map { entities ->
        entities.toTasks()
    }
    
    private val _stats = MutableLiveData<StatsData>()
    val stats: LiveData<StatsData> get() = _stats

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _topAchievement = MutableLiveData<Achievement?>()
    val topAchievement: LiveData<Achievement?> get() = _topAchievement

    private val _baseMetrics = MutableLiveData<List<Metric>>()
    val baseMetrics: LiveData<List<Metric>> get() = _baseMetrics

    private val _advancedMetrics = MutableLiveData<List<Metric>>()
    val advancedMetrics: LiveData<List<Metric>> get() = _advancedMetrics

    // Chart data
    private val _scoreCurveData = MutableLiveData<List<ScorePoint>>()
    val scoreCurveData: LiveData<List<ScorePoint>> get() = _scoreCurveData

    private val _deepWorkData = MutableLiveData<List<DeepWorkPoint>>()
    val deepWorkData: LiveData<List<DeepWorkPoint>> get() = _deepWorkData

    private val _timeDistributionData = MutableLiveData<List<TimeDistribution>>()
    val timeDistributionData: LiveData<List<TimeDistribution>> get() = _timeDistributionData

    private val _categoryPerformanceData = MutableLiveData<List<CategoryPerformance>>()
    val categoryPerformanceData: LiveData<List<CategoryPerformance>> get() = _categoryPerformanceData

    private val _subtaskTypeData = MutableLiveData<List<SubtaskType>>()
    val subtaskTypeData: LiveData<List<SubtaskType>> get() = _subtaskTypeData

    private val _rewardCurveData = MutableLiveData<List<RewardPoint>>()
    val rewardCurveData: LiveData<List<RewardPoint>> get() = _rewardCurveData

    private val _hourlyProductivityData = MutableLiveData<List<HourlyProductivity>>()
    val hourlyProductivityData: LiveData<List<HourlyProductivity>> get() = _hourlyProductivityData

    private fun parseDateTime(dateString: String): ZonedDateTime {
        try {
            val cleanedDateString = dateString.trim()
            
            // Create formatters with strict parsing
            val isoFormat = DateTimeFormatter.ISO_DATE_TIME
            val istFormat = DateTimeFormatter.ofPattern("h:mm a, MMM d, yyyy")
                .withLocale(Locale.ENGLISH)
                .withZone(ZoneId.of("Asia/Kolkata"))
                
            // Try parsing as ISO 8601
            return try {
                // Parse directly to ZonedDateTime for ISO format
                ZonedDateTime.parse(cleanedDateString, isoFormat)
            } catch (e: Exception) {
                try {
                    // Try parsing as IST format
                    val localDateTime = LocalDateTime.parse(cleanedDateString, istFormat)
                    localDateTime.atZone(ZoneId.of("Asia/Kolkata"))
                } catch (e2: Exception) {
                    SystemStatus.logEvent(
                        "StatsViewModel",
                        "Failed to parse date '$cleanedDateString': ${e2.message}"
                    )
                    throw IllegalArgumentException("Invalid date format: $cleanedDateString", e2)
                }
            }
        } catch (e: Exception) {
            SystemStatus.logEvent(
                "StatsViewModel",
                "Date parsing failed: $dateString - ${e.message}"
            )
            throw e
        }
    }

    // Add a validation function
    private fun isValidDateString(dateStr: String): Boolean {
        return try {
            parseDateTime(dateStr)
            true
        } catch (e: Exception) {
            false
        }
    }


    init {
        loadStats() // Initial load
        
        // Observe task changes
        tasks.observeForever { newTasks ->
            viewModelScope.launch {
                try {
                    val statsData = calculateDailyStats(newTasks)
                    _stats.postValue(statsData)
                    processStatsData(newTasks, statsData)
                } catch (e: Exception) {
                    SystemStatus.logEvent("StatsViewModel", "Error processing task update: ${e.message}")
                }
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                SystemStatus.logEvent("StatsViewModel", "Starting to load stats")
                
                // Get tasks directly from database
                val currentTasks = tasks.value ?: emptyList()
                
                val statsData = calculateDailyStats(currentTasks)
                _stats.value = statsData
                _isLoading.value = false
                
                processStatsData(currentTasks, statsData)
                
            } catch (e: Exception) {
                SystemStatus.logEvent("StatsViewModel", "Error loading stats: ${e.message}")
                _stats.value = StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "Error loading data: ${e.message}"
                )
                _isLoading.value = false
            }
        }
    }

    fun updateStats(newStats: StatsData) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    _stats.postValue(newStats)
                    SystemStatus.logEvent("StatsViewModel", "Updated stats")
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("StatsViewModel", "Error updating stats: ${e.message}")
                _stats.postValue(StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "Error updating stats: ${e.message}"
                ))
            }
        }
    }

    private fun calculateDailyStats(tasks: List<Task>): StatsData {
        try {
            if (tasks.isEmpty()) {
                return StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "No tasks found for today"
                )
            }

            // Filter tasks with valid date strings
            val validTasks = tasks.filter { task ->
                isValidDateString(task.startTime) && isValidDateString(task.endTime)
            }.also {
                if (it.size < tasks.size) {
                    SystemStatus.logEvent(
                        "StatsViewModel",
                        "Filtered out ${tasks.size - it.size} tasks with invalid dates: ${
                            tasks.filterNot { task -> isValidDateString(task.startTime) && isValidDateString(task.endTime) }
                                .joinToString { "${it.title} (start: ${it.startTime}, end: ${it.endTime})" }
                        }"
                    )
                }
            }

            if (validTasks.isEmpty()) {
                return StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "No tasks with valid dates found"
                )
            }

            val completedTasks = validTasks.count { it.completionStatus == 1f }
            val completionRate = if (validTasks.isNotEmpty()) completedTasks.toFloat() / validTasks.size else 0f

            val categoryStats = validTasks.groupBy { it.category }.mapValues { entry ->
                val categoryTasks = entry.value
                val completed = categoryTasks.count { it.completionStatus == 1f }
                completed.toFloat() / categoryTasks.size
            }

            val now = ZonedDateTime.now(DateFormatter.IST_ZONE).toEpochSecond()
            val tasksBehindSchedule = validTasks.count { task ->
                try {
                    val endDateTime = parseDateTime(task.endTime)
                    task.completionStatus < 1f && endDateTime.toEpochSecond() < now
                } catch (e: Exception) {
                    SystemStatus.logEvent(
                        "StatsViewModel",
                        "Schedule check failed for task ${task.title}: ${e.message}"
                    )
                    false
                }
            }

            val totalScoreObtained = validTasks.flatMap { it.subtasks ?: emptyList() }
                .sumByDouble { it.finalScore.toDouble() }.toFloat()
            val totalScoreAssociated = validTasks.flatMap { it.subtasks ?: emptyList() }
                .sumByDouble { it.baseScore.toDouble() }.toFloat()

            return StatsData(
                completionRate = completionRate,
                tasks = validTasks,
                categoryCompletionRates = categoryStats,
                totalScoreObtained = totalScoreObtained,
                totalScoreAssociated = totalScoreAssociated,
                tasksBehindSchedule = tasksBehindSchedule
            )
        } catch (e: Exception) {
            SystemStatus.logEvent("StatsViewModel", "Error calculating daily stats: ${e.message}")
            return StatsData(
                completionRate = 0f,
                tasks = emptyList(),
                error = "Error calculating stats: ${e.message}"
            )
        }
    }

//    private fun calculateDurationEfficiency(task: Task): Float {
//        return try {
//            if (!isValidDateString(task.startTime) || !isValidDateString(task.endTime)) {
//                SystemStatus.logEvent("StatsViewModel",
//                    "Invalid date format - Start: ${task.startTime}, End: ${task.endTime}")
//                return 0f
//            }
//
//            val startTime = parseDateTime(task.startTime).toEpochSecond()
//            val endTime = parseDateTime(task.endTime).toEpochSecond()
//            val actualDuration = (endTime - startTime) / 60f
//            val expectedDuration = task.duration.toFloat()
//
//            if (actualDuration > 0) {
//                (expectedDuration / actualDuration).coerceIn(0f, 1f)
//            } else {
//                0f
//            }
//        } catch (e: Exception) {
//            SystemStatus.logEvent(
//                "StatsViewModel",
//                "Error calculating duration efficiency for task ${task.title}: ${e.message}"
//            )
//            0f
//        }
//    }

    private fun processStatsData(tasks: List<Task>, statsData: StatsData) {
        viewModelScope.launch {
            calculateTopAchievement(tasks, statsData)
            calculateBaseMetrics(tasks)
            calculateAdvancedMetrics(tasks, statsData)
            calculateChartData(tasks, statsData)
            _isLoading.postValue(false)
        }
    }

    private fun calculateTopAchievement(tasks: List<Task>, statsData: StatsData) {
        if (tasks.isEmpty()) {
            _topAchievement.postValue(null)
            return
        }

        // Calculate scores
        val deepWorkScore = tasks.flatMap { it.subtasks ?: emptyList() }
            .filter { it.measurementType == "DeepWork" }
            .sumOf { it.finalScore.toDouble() }
            .toFloat()

        val workScore = tasks.filter { it.category == "Work" }
            .sumOf { it.taskScore.toDouble() }
            .toFloat()

        val efficiencyScore = if (statsData.totalScoreAssociated > 0)
            statsData.totalScoreObtained / statsData.totalScoreAssociated * 100 else 0f

        val achievements = listOf(
            Achievement(
                name = "Deep Work Master",
                value = deepWorkScore,
                threshold = 75f,
                type = AchievementType.DEEP_WORK,
                description = "Exceptional focus on deep, meaningful work"
            ),
            Achievement(
                name = "Efficiency Expert",
                value = efficiencyScore,
                threshold = 85f,
                type = AchievementType.EFFICIENCY,
                description = "Outstanding output with minimal resource usage"
            ),
            Achievement(
                name = "Task Champion",
                value = statsData.completionRate * 100, // Fix: use statsData.completionRate instead
                threshold = 80f,
                type = AchievementType.TASK_COMPLETION,
                description = "Excellent at completing planned tasks"
            ),
            Achievement(
                name = "Work Star",
                value = workScore,
                threshold = 85f,
                type = AchievementType.WORK,
                description = "Outstanding performance in work activities"
            )
        )

        val topAchievement = achievements.maxByOrNull { it.value - it.threshold }
        _topAchievement.postValue(topAchievement)
    }

    private fun calculateBaseMetrics(tasks: List<Task>) {
        val timeNow = System.currentTimeMillis()
        
        // Real completion rate
        val completionRate = (tasks.sumOf { it.completionStatus.toDouble() } / tasks.size * 100).toInt()
        
        // Actual score calculation
        val achievedScore = tasks.sumOf { task ->
            task.subtasks?.sumOf { it.finalScore.toDouble() } ?: 0.0
        }.toInt()
        
        val possibleScore = tasks.sumOf { task ->
            task.subtasks?.sumOf { it.baseScore.toDouble() } ?: 0.0
        }.toInt()
        
        // Real efficiency calculation
        val totalTimeSpent = tasks.sumOf { task ->
            val startTime = parseDateTime(task.startTime).toEpochSecond()
            val endTime = parseDateTime(task.endTime).toEpochSecond()
            (endTime - startTime).toInt()
        } / 3600f // Convert to hours
        
        val efficiencyQuotient = if(totalTimeSpent > 0) 
            achievedScore / totalTimeSpent else 0f
            
        val baseMetrics = listOf(
            Metric(
                name = "Completion",
                value = "$completionRate%",
                trend = calculateTrend(tasks, "completion"),
                detail = "${tasks.count { it.completionStatus >= 1f }}/${tasks.size}",
                icon = R.drawable.ic_target
            ),
            Metric(
                name = "Score",
                value = "${achievedScore}%",
                trend = calculateTrend(tasks, "score"),
                detail = "$achievedScore/$possibleScore",
                icon = R.drawable.ic_star
            ),
            Metric(
                name = "Efficiency",
                value = String.format("%.1f", efficiencyQuotient),
                trend = calculateTrend(tasks, "efficiency"),
                detail = "pts/hour",
                icon = R.drawable.ic_lightning
            ),
            Metric(
                name = "Focus",
                value = "${completionRate.toInt()}%",
                trend = calculateTrend(tasks, "focus"),
                detail = "consistency",
                icon = R.drawable.ic_brainn
            )
        )

        SystemStatus.logEvent("StatsViewModel", "Posting ${baseMetrics.size} base metrics")
        _baseMetrics.postValue(baseMetrics)
    }

    private fun calculateAdvancedMetrics(tasks: List<Task>, statsData: StatsData) {
        SystemStatus.logEvent("StatsViewModel", "Calculating advanced metrics for ${tasks.size} tasks")
        val totalScoreFromSubtasks = tasks.flatMap { it.subtasks ?: emptyList() }
            .sumByDouble { it.finalScore.toDouble() }
            .toFloat()

        val depthRatio = if (statsData.totalScoreObtained > 0)
            totalScoreFromSubtasks / statsData.totalScoreObtained * 100 else 0f

        val completedTasks = tasks.count { it.completionStatus >= 1f }
        val taskQualityIndex = if (completedTasks > 0)
            statsData.totalScoreObtained / completedTasks else 0f

        val efficiencyScore = if (statsData.totalScoreAssociated > 0)
            statsData.totalScoreObtained / statsData.totalScoreAssociated * 100 else 0f

        val advancedMetrics = listOf(
            Metric(
                name = "Depth Ratio",
                value = "${depthRatio.toInt()}%",
                trend = calculateTrend(tasks, "depth_ratio"),
                detail = "subtask/total",
                icon = R.drawable.ic_brainn
            ),
            Metric(
                name = "Task Quality",
                value = String.format("%.1f", taskQualityIndex),
                trend = calculateTrend(tasks, "task_quality"),
                detail = "score/task",
                icon = R.drawable.ic_award
            ),
            Metric(
                name = "Efficiency",
                value = "${efficiencyScore.toInt()}%",
                trend = calculateTrend(tasks, "efficiency_score"),
                detail = "achieved/possible",
                icon = R.drawable.ic_zap
            )
        )

        SystemStatus.logEvent("StatsViewModel", "Posting ${advancedMetrics.size} advanced metrics")
        _advancedMetrics.postValue(advancedMetrics)
    }

    // Update the chart data calculation function:
    private fun calculateChartData(tasks: List<Task>, statsData: StatsData) {
        viewModelScope.launch {
            val validTasks = tasks.filter { task ->
                isValidDateString(task.startTime) && isValidDateString(task.endTime)
            }.also {
                if (it.size < tasks.size) {
                    SystemStatus.logEvent(
                        "StatsViewModel",
                        "Filtered out ${tasks.size - it.size} tasks with invalid dates for chart data: ${
                            tasks.filterNot { task -> isValidDateString(task.startTime) && isValidDateString(task.endTime) }
                                .joinToString { "${it.title} (start: ${it.startTime}, end: ${it.endTime})" }
                        }"
                    )
                }
            }

            if (validTasks.isEmpty()) {
                SystemStatus.logEvent("StatsViewModel", "No valid tasks for chart data")
                _scoreCurveData.postValue(emptyList())
                _deepWorkData.postValue(emptyList())
                _timeDistributionData.postValue(emptyList())
                _categoryPerformanceData.postValue(emptyList())
                _subtaskTypeData.postValue(emptyList())
                _rewardCurveData.postValue(emptyList())
                _hourlyProductivityData.postValue(emptyList())
                return@launch
            }

            val scoreCurveData = validTasks.groupBy {
                parseDateTime(it.startTime).toLocalTime().hour / 2 * 2
            }.map { (hour, groupTasks) ->
                val expected = groupTasks.sumByDouble { it.taskScore.toDouble() }.toFloat()
                val achieved = groupTasks.sumByDouble {
                    it.taskScore * it.completionStatus.toDouble()
                }.toFloat()
                ScorePoint("${hour.toString().padStart(2, '0')}:00", expected, achieved)
            }.sortedBy { it.time }
            _scoreCurveData.postValue(scoreCurveData)

            val deepWorkData = validTasks.flatMap { task ->
                // Map both task and subtask together
                (task.subtasks ?: emptyList()).map { subtask -> 
                    Pair(task, subtask)
                }
            }.filter { 
                it.second.measurementType == "DeepWork" 
            }.groupBy { 
                // Use parent task's startTime for grouping
                parseDateTime(it.first.startTime).toLocalDate()
            }.map { (date, taskSubtaskPairs) ->
                val required = taskSubtaskPairs.sumByDouble { it.second.baseScore.toDouble() }.toFloat()
                val achieved = taskSubtaskPairs.sumByDouble { it.second.finalScore.toDouble() }.toFloat()
                DeepWorkPoint(date.toString(), required, achieved)
            }.sortedBy { it.time }
            _deepWorkData.postValue(deepWorkData)

            val timeDistributionData = validTasks.groupBy { task ->
                parseDateTime(task.startTime).hour / 2
            }.map { (hour, groupTasks) ->
                val totalDuration = groupTasks.sumByDouble { task ->
                    val start = parseDateTime(task.startTime).toEpochSecond()
                    val end = parseDateTime(task.endTime).toEpochSecond()
                    (end - start) / 60.0
                }.toFloat()
                
                // Generate proper hex color with # prefix and ensure 6 digits
                val color = String.format("#%06X", (0xFF000000 or (((hour * 25) shl 16).toLong()) or (((hour * 15) shl 8).toLong()) or ((hour * 20).toLong())).toInt())
                
                TimeDistribution(
                    name = "${hour * 2}:00", 
                    value = totalDuration,
                    color = color
                )
            }.sortedBy { it.name }
            _timeDistributionData.postValue(timeDistributionData)

            val categoryPerformanceData = validTasks.groupBy { it.category }
                .map { (category, groupTasks) ->
                    val achieved = groupTasks.sumByDouble { it.taskScore * it.completionStatus.toDouble() }.toFloat()
                    val possible = groupTasks.sumByDouble { it.taskScore.toDouble() }.toFloat()
                    CategoryPerformance(category, achieved, possible)
                }.sortedByDescending { it.scoreAchieved }
            _categoryPerformanceData.postValue(categoryPerformanceData)

            // For subtask types, group by measurementType instead of type
            val subtaskTypeData = validTasks.flatMap { task -> 
                task.subtasks?.map { subtask ->
                    Pair(task, subtask) // Pair subtask with its parent task
                } ?: emptyList()
            }.groupBy { 
                it.second.measurementType // Group by measurementType instead of type
            }.map { (measurementType, taskSubtaskPairs) ->
                val completed = taskSubtaskPairs.count { it.second.finalScore >= it.second.baseScore }
                val total = taskSubtaskPairs.size
                val score = taskSubtaskPairs.sumByDouble { it.second.finalScore.toDouble() }.toFloat()
                val startTime = taskSubtaskPairs.firstOrNull()?.first?.startTime ?: "" // Use parent task's startTime
                SubtaskType(
                    type = measurementType ?: "Unknown",
                    completed = completed,
                    total = total,
                    score = score,
                    startTime = startTime
                )
            }.sortedByDescending { it.completed }
            _subtaskTypeData.postValue(subtaskTypeData)

            // For reward curve, use parent task's time
            val rewardCurveData = validTasks.flatMap { task ->
                task.subtasks?.filter { it.measurementType == "Reward" }?.map { subtask ->
                    RewardPoint(
                        time = task.startTime, // Use parent task's startTime
                        score = subtask.finalScore
                    )
                } ?: emptyList()
            }.groupBy { it.time }
            .map { (time, rewards) ->
                val totalScore = rewards.sumByDouble { it.score.toDouble() }.toFloat()
                RewardPoint(time = time, score = totalScore)
            }.sortedBy { it.time }
            _rewardCurveData.postValue(rewardCurveData)

            val hourlyProductivityData = validTasks.groupBy { task ->
                parseDateTime(task.startTime).hour
            }.map { (hour, groupTasks) ->
                val taskCount = groupTasks.size
                HourlyProductivity("$hour:00", taskCount)
            }.sortedBy { it.time }
            _hourlyProductivityData.postValue(hourlyProductivityData)
        }
    }

    private fun calculateTrend(tasks: List<Task>, metric: String): Float {
        // Implement trend calculation based on historical data
        // This is a placeholder - actual implementation would query historical data
        return when (metric) {
            "completion" -> 2.5f
            "score" -> 1.8f
            "efficiency" -> -0.3f
            "focus" -> 3.2f
            "depth_ratio" -> 1.5f
            "task_quality" -> 0.4f
            "efficiency_score" -> -0.7f
            else -> 0f
        }
    }

    private fun calculateRealTimeStats(tasks: List<Task>) {
        viewModelScope.launch {
            try {
                // Daily stats
                val todayTasks = tasks.filter {
                    val taskDate = parseDateTime(it.startTime).toLocalDate()
                    val today = LocalDate.now()
                    taskDate == today
                }
                
                // Calculate real metrics
                val completedTasks = todayTasks.count { it.completionStatus >= 1f }
                val totalTasks = todayTasks.size
                val completionRate = if(totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                
                // Calculate actual score
                val achievedScore = todayTasks.sumOf { task ->
                    task.subtasks?.sumOf { subtask -> 
                        subtask.finalScore.toDouble() 
                    } ?: 0.0
                }.toFloat()
                
                val possibleScore = todayTasks.sumOf { task ->
                    task.subtasks?.sumOf { subtask -> 
                        subtask.baseScore.toDouble()
                    } ?: 0.0
                }.toFloat()

                // Update live data
                _stats.postValue(StatsData(
                    completionRate = completionRate,
                    tasks = todayTasks,
                    totalScoreObtained = achievedScore,
                    totalScoreAssociated = possibleScore
                ))
            } catch (e: Exception) {
                SystemStatus.logEvent("StatsViewModel", "Error calculating stats: ${e.message}")
            }
        }
    }
}