package com.technource.android.module.statsModule.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technource.android.R
import com.technource.android.local.AppDatabase
import com.technource.android.local.Task
import com.technource.android.local.toTasks
import com.technource.android.system_status.SystemStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import java.io.Serializable

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
data class SubtaskType(val type: String, val completed: Int, val total: Int, val score: Float)
data class RewardPoint(val time: String, val score: Float)
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
    private val database: AppDatabase,
) : ViewModel() {
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

    init {
        setupTaskObserver()
        loadStats()
    }

    private fun setupTaskObserver() {
        viewModelScope.launch {
            try {
                database.taskDao().getTodayTasksFlow(
                    LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                ).collectLatest { taskEntities ->
                    val tasks = taskEntities.toTasks()
                    SystemStatus.logEvent("StatsViewModel", "Task data changed, recalculating stats for ${tasks.size} tasks")
                    val statsData = calculateDailyStats(tasks)
                    _stats.postValue(statsData)
                    _isLoading.postValue(false)
                    processStatsData(tasks, statsData)
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("StatsViewModel", "Error in task observer: ${e.message}")
                _stats.postValue(StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "Error observing tasks: ${e.message}"
                ))
                _isLoading.postValue(false)
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val taskEntities = database.taskDao().getTasks()
//                val taskEntities = withContext(Dispatchers.IO) {
//                    database.taskDao().getTodayTasks(
//                        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
//                        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
//                    )
//                }

                val tasks = taskEntities.toTasks()
                SystemStatus.logEvent("StatsViewModel", "Loading stats for ${tasks.size} tasks")
                val statsData = calculateDailyStats(tasks)
                _stats.value = statsData
                _isLoading.value = false
                processStatsData(tasks, statsData)
            } catch (e: Exception) {
                SystemStatus.logEvent("StatsViewModel", "Error fetching stats: ${e.message}")
                _stats.value = StatsData(
                    completionRate = 0f,
                    tasks = emptyList(),
                    error = "Error fetching data: ${e.message}"
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

            val completedTasks = tasks.count { it.completionStatus == 1f }
            val completionRate = if (tasks.isNotEmpty()) completedTasks.toFloat() / tasks.size else 0f

            val categoryStats = tasks.groupBy { it.category }.mapValues { entry ->
                val categoryTasks = entry.value
                val completed = categoryTasks.count { it.completionStatus == 1f }
                completed.toFloat() / categoryTasks.size
            }

            val durationEfficiency = tasks.map { calculateDurationEfficiency(it) }
                .average().toFloat().takeIf { it.isFinite() } ?: 0f

            val now = ZonedDateTime.now().toEpochSecond()
            val tasksBehindSchedule = tasks.count { task ->
                try {
                    task.completionStatus < 1f && ZonedDateTime.parse(task.endTime).toEpochSecond() < now
                } catch (e: Exception) {
                    SystemStatus.logEvent("StatsViewModel", "Error checking schedule for task ${task.title}: ${e.message}")
                    false
                }
            }

            val totalScoreObtained = tasks.flatMap { it.subtasks ?: emptyList() }
                .sumByDouble { it.finalScore.toDouble() }.toFloat()
            val totalScoreAssociated = tasks.flatMap { it.subtasks ?: emptyList() }
                .sumByDouble { it.baseScore.toDouble() }.toFloat()

            return StatsData(
                completionRate = completionRate,
                tasks = tasks,
                durationEfficiency = durationEfficiency,
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

    private fun calculateDurationEfficiency(task: Task): Float {
        try {
            val startTime = ZonedDateTime.parse(task.startTime).toEpochSecond()
            val endTime = ZonedDateTime.parse(task.endTime).toEpochSecond()
            val actualDuration = (endTime - startTime) / 60f
            val expectedDuration = task.duration.toFloat()
            return if (actualDuration > 0) (expectedDuration / actualDuration).coerceIn(0f, 1f) else 0f
        } catch (e: Exception) {
            SystemStatus.logEvent("StatsViewModel", "Error parsing date for task ${task.title}: ${e.message}")
            return 0f
        }
    }

    private fun processStatsData(tasks: List<Task>, statsData: StatsData) {
        calculateTopAchievement(tasks, statsData)
        calculateBaseMetrics(tasks, statsData)
        calculateAdvancedMetrics(tasks, statsData)
        calculateChartData(tasks, statsData)
    }

    private fun calculateTopAchievement(tasks: List<Task>, statsData: StatsData) {
        val completionRate = statsData.completionRate * 100

        val deepWorkScore = tasks.flatMap { it.subtasks ?: emptyList() }
            .filter { it.measurementType == "DeepWork" }
            .sumBy { it.finalScore.toInt() }
            .toFloat()

        val workScore = tasks.filter { it.category == "Work" }
            .sumByDouble { it.taskScore.toDouble() }
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
                value = completionRate,
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
        if (topAchievement != null && topAchievement.value > topAchievement.threshold) {
            _topAchievement.postValue(topAchievement)
        } else {
            _topAchievement.postValue(null)
        }
    }

    private fun calculateBaseMetrics(tasks: List<Task>, statsData: StatsData) {
        val completionRate = statsData.completionRate * 100
        val completedTasks = tasks.count { it.completionStatus >= 1f }
        val totalTasks = tasks.size

        val overallScore = if (statsData.totalScoreAssociated > 0)
            statsData.totalScoreObtained / statsData.totalScoreAssociated * 100 else 0f

        val totalTimeAllocated = tasks.sumBy { it.duration } / 60f
        val efficiencyQuotient = if (totalTimeAllocated > 0)
            statsData.totalScoreObtained / totalTimeAllocated else 0f

        val focusLevel = completionRate

        val baseMetrics = listOf(
            Metric(
                name = "Completion",
                value = "${completionRate.toInt()}%",
                trend = calculateTrend(tasks, "completion"),
                detail = "$completedTasks/$totalTasks",
                icon = R.drawable.ic_target
            ),
            Metric(
                name = "Score",
                value = "${overallScore.toInt()}%",
                trend = calculateTrend(tasks, "score"),
                detail = "${statsData.totalScoreObtained.toInt()}/${statsData.totalScoreAssociated.toInt()}",
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
                value = "${focusLevel.toInt()}%",
                trend = calculateTrend(tasks, "focus"),
                detail = "consistency",
                icon = R.drawable.ic_brainn
            )
        )

        _baseMetrics.postValue(baseMetrics)
    }

    private fun calculateAdvancedMetrics(tasks: List<Task>, statsData: StatsData) {
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

        _advancedMetrics.postValue(advancedMetrics)
    }

    private fun calculateChartData(tasks: List<Task>, statsData: StatsData) {
        val scoreCurveData = tasks.groupBy {
            ZonedDateTime.parse(it.startTime).toLocalTime().hour / 2 * 2
        }.map { (hour, groupTasks) ->
            val expected = groupTasks.sumByDouble { it.taskScore.toDouble() }.toFloat()
            val achieved = groupTasks.sumByDouble {
                it.taskScore * it.completionStatus.toDouble()
            }.toFloat()
            ScorePoint("${hour.toString().padStart(2, '0')}:00", expected, achieved)
        }.sortedBy { it.time }
        _scoreCurveData.postValue(scoreCurveData)

        val deepWorkData = tasks.flatMap { it.subtasks ?: emptyList() }
            .filter { it.measurementType == "DeepWork" }
            .groupBy {
                ZonedDateTime.parse(tasks.first { t -> t.subtasks?.contains(it) == true }.startTime)
                    .toLocalTime().hour / 2 * 2
            }.map { (hour, groupSubtasks) ->
                val required = groupSubtasks.sumBy { it.baseScore }.toFloat()
                val achieved = groupSubtasks.sumByDouble { it.finalScore.toDouble() }.toFloat()
                DeepWorkPoint("${hour.toString().padStart(2, '0')}:00", required, achieved)
            }.sortedBy { it.time }
        _deepWorkData.postValue(deepWorkData)

        val timeDistributionData = tasks.groupBy { it.category }.map { (category, categoryTasks) ->
            val hours = categoryTasks.sumBy { it.duration } / 60f
            TimeDistribution(
                category,
                hours,
                when (category) {
                    "Work" -> "#6366f1"
                    "Personal" -> "#f59e0b"
                    "Fitness" -> "#10b981"
                    "Leisure" -> "#ec4899"
                    else -> "#8b5cf6"
                }
            )
        }
        _timeDistributionData.postValue(timeDistributionData)

        val categoryData = tasks.groupBy { it.category }.map { (category, categoryTasks) ->
            val scoreAchieved = categoryTasks.sumByDouble { it.taskScore.toDouble() }.toFloat()
            val scorePossible = categoryTasks.sumByDouble {
                (it.subtasks?.sumBy { subtask -> subtask.baseScore } ?: 0).toDouble()
            }.toFloat()
            CategoryPerformance(category, scoreAchieved, scorePossible)
        }
        _categoryPerformanceData.postValue(categoryData)

        val subtaskTypes = tasks.flatMap { it.subtasks ?: emptyList() }
            .groupBy { it.measurementType }
            .map { (type, subtasks) ->
                val completed = subtasks.count { it.completionStatus >= 1f }
                val total = subtasks.size
                val score = subtasks.sumByDouble { it.finalScore.toDouble() }.toFloat()
                SubtaskType(type, completed, total, score)
            }
        _subtaskTypeData.postValue(subtaskTypes)

        val rewardCurveData = tasks.groupBy {
            ZonedDateTime.parse(it.startTime).toLocalTime().hour / 2 * 2
        }.map { (hour, groupTasks) ->
            val score = groupTasks.sumByDouble { it.taskScore.toDouble() }.toFloat()
            RewardPoint("${hour.toString().padStart(2, '0')}:00", score)
        }.sortedBy { it.time }
        _rewardCurveData.postValue(rewardCurveData)

        val hourlyProductivityData = tasks.groupBy {
            ZonedDateTime.parse(it.startTime).toLocalTime().hour
        }.map { (hour, groupTasks) ->
            HourlyProductivity("${hour.toString().padStart(2, '0')}:00", groupTasks.size)
        }.sortedBy { it.time }
        _hourlyProductivityData.postValue(hourlyProductivityData)
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
}