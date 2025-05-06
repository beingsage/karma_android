//package com.technource.android.ETMS.Micro
//
//import android.annotation.SuppressLint
//import android.app.PendingIntent
//import android.appwidget.AppWidgetManager
//import android.appwidget.AppWidgetProvider
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.RemoteViews
//import com.google.gson.Gson
//import com.technource.android.R
//import com.technource.android.local.Task
//import com.technource.android.system_status.SystemStatus
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.util.Timer
//import kotlin.concurrent.schedule
//
//data class WidgetTask(
//    val task: Task,
//    val startTime: String,
//    val endTime: String,
//    val totalScore: Int
//) : java.io.Serializable
//
//class TaskWidgetProvider : AppWidgetProvider() {
//
//    companion object {
//        private const val ACTION_PREVIOUS = "com.technource.android.ETMS.PREVIOUS"
//        private const val ACTION_NEXT = "com.technource.android.ETMS.NEXT"
//        private const val EXTRA_TASKS = "extra_tasks"
//        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
//        private const val MAX_NAVIGATION = 3
//        private const val AUTO_RETURN_DELAY = 30_000L // 30 seconds
//
//        private val handler = Handler(Looper.getMainLooper())
//        private var progress = 45f // Starting progress in seconds
//        private var beamPosition = 0f
//        private var beamActive = true
//        private var timer: Timer? = null // Nullable timer
//        private var autoReturnTimer: Timer? = null
//        private var currentTasks: List<WidgetTask> = emptyList()
//        private var appContext: Context? = null
//        private val gson = Gson()
//
//        fun updateHomeScreenWidget(context: Context, tasks: List<Task>, startTime: LocalDateTime, endTime: LocalDateTime) {
//            Log.d("TaskWidgetProvider", "updateHomeScreenWidget called with ${tasks.size} tasks")
//            appContext = context.applicationContext
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
//            Log.d("TaskWidgetProvider", "Found ${appWidgetIds.size} widget IDs: ${appWidgetIds.joinToString()}")
//
//            // Format times
//            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
//            val startTimeStr = startTime.format(timeFormatter)
//            val endTimeStr = endTime.format(timeFormatter)
//
//            // Create WidgetTasks
//            currentTasks = tasks.map { task ->
//                val totalScore = task.subtasks?.sumOf { it.baseScore } ?: 0
//                WidgetTask(task, startTimeStr, endTimeStr, totalScore)
//            }
//            Log.d("TaskWidgetProvider", "Created ${currentTasks.size} WidgetTasks")
//
//            // Test serialization
//            try {
//                if (currentTasks.isNotEmpty()) {
//                    val json = gson.toJson(currentTasks)
//                    val deserialized = gson.fromJson(json, Array<WidgetTask>::class.java)
//                    Log.d("TaskWidgetProvider", "Serialization test: ${deserialized.size} tasks")
//                }
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Serialization test failed: ${e.message}", e)
//            }
//
//            // Update all widgets
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//
//            // Start animations
//            startAnimations(context)
//
//            SystemStatus.logEvent("TaskIterator", "Home screen widget updated for: ${tasks.firstOrNull()?.title ?: "No tasks"}")
//        }
//
//        private fun startAnimations(context: Context) {
//            timer?.cancel() // Cancel existing timer
//            timer = Timer() // Reinitialize
//            Log.d("TaskWidgetProvider", "Starting animations")
//            timer?.schedule(500, 500) {
//                progress += 0.1f
//                if (progress > 240f) progress = 240f
//                beamPosition = (beamPosition + 1) % 100
//                beamActive = !beamActive
//                updateAllWidgets(context)
//            }
//        }
//
//        private fun updateAllWidgets(context: Context) {
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//            Log.d("TaskWidgetProvider", "Updating all widgets: ${appWidgetIds.size} IDs")
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//        }
//
//        @SuppressLint("RemoteViewLayout")
//        private fun updateWidget(
//            context: Context,
//            appWidgetManager: AppWidgetManager,
//            appWidgetId: Int,
//            tasks: List<WidgetTask>,
//            currentIndex: Int,
//            taskHistory: List<Int>
//        ) {
//            Log.d("TaskWidgetProvider", "Updating widget ID: $appWidgetId, tasks: ${tasks.size}, currentIndex: $currentIndex")
//            try {
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//                if (tasks.isEmpty()) {
//                    Log.d("TaskWidgetProvider", "No tasks available for widget ID: $appWidgetId")
//                    views.setTextViewText(R.id.task_title, "No Tasks Available")
//                    views.setTextViewText(R.id.total_score, "0")
//                    views.setViewVisibility(R.id.subtasks_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.progress_bar, android.view.View.GONE)
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                if (currentIndex < 0 || currentIndex >= tasks.size) {
//                    Log.e("TaskWidgetProvider", "Invalid currentIndex: $currentIndex for tasks size: ${tasks.size}")
//                    views.setTextViewText(R.id.task_title, "Error: Invalid Task Index")
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                val widgetTask = tasks[currentIndex]
//                val task = widgetTask.task
//                Log.d("TaskWidgetProvider", "Displaying task: ${task.title}, score: ${widgetTask.totalScore}")
//
//                // Set task data
//                views.setTextViewText(R.id.total_score, widgetTask.totalScore.toString())
//                views.setTextViewText(R.id.task_title, task.title)
//
//                // Set subtasks
//                views.removeAllViews(R.id.subtasks_container)
//                task.subtasks?.forEachIndexed { index, subTask ->
//                    val subTaskView = RemoteViews(context.packageName, R.layout.item_subtask_widget)
//                    subTaskView.setTextViewText(R.id.subtask_name, subTask.title)
//                    subTaskView.setTextViewText(R.id.subtask_score, "${subTask.baseScore} pts")
//                    when (subTask.measurementType) {
//                        "Quant" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundColor", Color.parseColor("#E9D5FF"))
//                            subTaskView.setTextViewText(R.id.subtask_category, "Quant")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#7C3AED"))
//                        }
//                        "Time" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundColor", Color.parseColor("#CCFBF1"))
//                            subTaskView.setTextViewText(R.id.subtask_category, "Time")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#0D9488"))
//                        }
//                        "DeepWork" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundColor", Color.parseColor("#DBEAFE"))
//                            subTaskView.setTextViewText(R.id.subtask_category, "DeepWork")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#1E40AF"))
//                        }
//                        "Binary" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundColor", Color.parseColor("#FEF3C7"))
//                            subTaskView.setTextViewText(R.id.subtask_category, "Binary")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#B45309"))
//                        }
//                    }
//                    views.addView(R.id.subtasks_container, subTaskView)
//                }
//
//                // Set progress bar
//                val duration = task.duration // Duration in seconds
//                val progressPercentage = if (duration > 0) (progress / duration.toFloat()) * 100f else 0f
//                views.setProgressBar(R.id.progress_bar, 100, progressPercentage.toInt(), false)
//
//                // Set beaming effect
//                views.setFloat(R.id.beam_view, "setAlpha", if (beamActive) 0.6f else 0.3f)
//                views.setViewPadding(R.id.beam_view, (beamPosition * 2).toInt(), 0, 0, 0)
//
//                // Set time indicators
//                views.setTextViewText(R.id.start_time, widgetTask.startTime)
//                views.setTextViewText(R.id.end_time, widgetTask.endTime)
//
//                // Set navigation buttons
//                val prevIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_PREVIOUS
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val nextIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_NEXT
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val prevPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//                val nextPendingIntent = PendingIntent.getBroadcast(context, appWidgetId + 1, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
//                views.setOnClickPendingIntent(R.id.btn_previous, prevPendingIntent)
//                views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
//
//                // Set navigation dots
//                for (i in 0 until 5) {
//                    val dotId = context.resources.getIdentifier("dot_$i", "id", context.packageName)
//                    if (dotId == 0) {
//                        Log.w("TaskWidgetProvider", "Dot ID dot_$i not found")
//                        continue
//                    }
//                    if (i < tasks.size) {
//                        views.setInt(dotId, "setBackgroundResource", if (i == currentIndex) R.drawable.dot_active else R.drawable.dot_inactive)
//                    } else {
//                        views.setViewVisibility(dotId, android.view.View.GONE)
//                    }
//                }
//
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Failed to update widget ID: $appWidgetId: ${e.message}", e)
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//                views.setTextViewText(R.id.task_title, "Error Loading Widget")
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            }
//        }
//
//        private fun scheduleAutoReturn(context: Context, tasks: List<WidgetTask>, currentIndex: Int, taskHistory: List<Int>) {
//            autoReturnTimer?.cancel()
//            autoReturnTimer = Timer()
//            autoReturnTimer?.schedule(AUTO_RETURN_DELAY) {
//                handler.post {
//                    updateWidget(context, AppWidgetManager.getInstance(context), AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java)).firstOrNull() ?: return@post, tasks, 0, listOf(0))
//                }
//            }
//        }
//    }
//
//    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
//        Log.d("TaskWidgetProvider", "onUpdate called with ${appWidgetIds.size} widget IDs")
//        appContext = context.applicationContext
//        for (appWidgetId in appWidgetIds) {
//            updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//        }
//    }
//
//    override fun onEnabled(context: Context) {
//        super.onEnabled(context)
//        Log.d("TaskWidgetProvider", "Widget enabled")
//        appContext = context.applicationContext
//    }
//
//    override fun onDisabled(context: Context) {
//        super.onDisabled(context)
//        Log.d("TaskWidgetProvider", "Widget disabled")
//        timer?.cancel()
//        timer = null
//        autoReturnTimer?.cancel()
//        autoReturnTimer = null
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        super.onReceive(context, intent)
//        Log.d("TaskWidgetProvider", "onReceive action: ${intent.action}")
//        val appWidgetManager = AppWidgetManager.getInstance(context)
//        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//        Log.d("TaskWidgetProvider", "onReceive found ${appWidgetIds.size} widget IDs")
//
//        val tasksJson = intent.getStringExtra(EXTRA_TASKS) ?: gson.toJson(currentTasks)
//        val tasks = try {
//            val type = object : com.google.gson.reflect.TypeToken<List<WidgetTask>>() {}.type
//            gson.fromJson<List<WidgetTask>>(tasksJson, type) ?: currentTasks
//        } catch (e: Exception) {
//            Log.e("TaskWidgetProvider", "Failed to deserialize tasks: ${e.message}", e)
//            currentTasks
//        }
//        val currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
//        var taskHistory = intent.getIntegerArrayListExtra("task_history") ?: listOf(0)
//
//        when (intent.action) {
//            ACTION_PREVIOUS -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    val prevIndex = (currentIndex - 1).coerceAtLeast(0)
//                    taskHistory = taskHistory + prevIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, prevIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, prevIndex, taskHistory)
//                }
//            }
//            ACTION_NEXT -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    val nextIndex = (currentIndex + 1).coerceAtMost(tasks.size - 1)
//                    taskHistory = taskHistory + nextIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, nextIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, nextIndex, taskHistory)
//                }
//            }
//        }
//    }
//}

//
//package com.technource.android.ETMS.Micro
//
//import android.annotation.SuppressLint
//import android.app.PendingIntent
//import android.appwidget.AppWidgetManager
//import android.appwidget.AppWidgetProvider
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.RemoteViews
//import com.google.gson.Gson
//import com.technource.android.R
//import com.technource.android.local.Task
//import com.technource.android.system_status.SystemStatus
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.util.Timer
//import kotlin.concurrent.schedule
//
//data class WidgetTask(
//    val task: Task,
//    val startTime: String, // Formatted string for display (e.g., "1:39 am")
//    val endTime: String,   // Formatted string for display (e.g., "2:39 am")
//    val totalScore: Int,
//    val startTimeRaw: LocalDateTime, // Raw LocalDateTime for calculations
//    val endTimeRaw: LocalDateTime    // Raw LocalDateTime for calculations
//) : java.io.Serializable
//
//class TaskWidgetProvider : AppWidgetProvider() {
//
//    companion object {
//        private const val ACTION_PREVIOUS = "com.technource.android.ETMS.PREVIOUS"
//        private const val ACTION_NEXT = "com.technource.android.ETMS.NEXT"
//        private const val EXTRA_TASKS = "extra_tasks"
//        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
//        private const val MAX_NAVIGATION = 3
//        private const val AUTO_RETURN_DELAY = 30_000L // 30 seconds
//
//        private val handler = Handler(Looper.getMainLooper())
//        private var progress = 45f // Starting progress in seconds
//        private var snakePosition = 0f // For snake loader animation
//        private var snakeActive = true // For snake loader pulsing
//        private var timer: Timer? = null
//        private var autoReturnTimer: Timer? = null
//        private var currentTasks: List<WidgetTask> = emptyList()
//        private var appContext: Context? = null
//        private val gson = Gson()
//
//        fun updateHomeScreenWidget(context: Context, tasks: List<Task>, startTime: LocalDateTime, endTime: LocalDateTime) {
//            Log.d("TaskWidgetProvider", "updateHomeScreenWidget called with ${tasks.size} tasks")
//            appContext = context.applicationContext
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
//            Log.d("TaskWidgetProvider", "Found ${appWidgetIds.size} widget IDs: ${appWidgetIds.joinToString()}")
//
//            // Format times for display
//            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
//            val startTimeStr = startTime.format(timeFormatter)
//            val endTimeStr = endTime.format(timeFormatter)
//
//
//            // Create WidgetTasks
//            currentTasks = tasks.map { task ->
//                val totalScore = task.subtasks?.sumOf { it.baseScore } ?: 0
//                WidgetTask(task, startTimeStr, endTimeStr, totalScore, startTime, endTime)
//            }
//            Log.d("TaskWidgetProvider", "Created ${currentTasks.size} WidgetTasks")
//
//            // Test serialization
//            try {
//                if (currentTasks.isNotEmpty()) {
//                    val json = gson.toJson(currentTasks)
//                    val deserialized = gson.fromJson(json, Array<WidgetTask>::class.java)
//                    Log.d("TaskWidgetProvider", "Serialization test: ${deserialized.size} tasks")
//                }
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Serialization test failed: ${e.message}", e)
//            }
//
//            // Update all widgets
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//
//            // Start animations
//            startAnimations(context)
//
//            SystemStatus.logEvent("TaskIterator", "Home screen widget updated for: ${tasks.firstOrNull()?.title ?: "No tasks"}")
//        }
//
//        private fun startAnimations(context: Context) {
//            timer?.cancel()
//            timer = Timer()
//            Log.d("TaskWidgetProvider", "Starting animations")
//            timer?.schedule(200, 200) { // Faster animation for smoothness
//                progress += 0.1f
//                if (progress > 3600f) progress = 0f // Reset after 1 hour
//                snakePosition = (snakePosition + 2) % 100
//                snakeActive = !snakeActive
//                updateAllWidgets(context)
//            }
//        }
//
//        private fun updateAllWidgets(context: Context) {
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//            Log.d("TaskWidgetProvider", "Updating all widgets: ${appWidgetIds.size} IDs")
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//        }
//
//        @SuppressLint("RemoteViewLayout")
//        private fun updateWidget(
//            context: Context,
//            appWidgetManager: AppWidgetManager,
//            appWidgetId: Int,
//            tasks: List<WidgetTask>,
//            currentIndex: Int,
//            taskHistory: List<Int>
//        ) {
//            Log.d("TaskWidgetProvider", "Updating widget ID: $appWidgetId, tasks: ${tasks.size}, currentIndex: $currentIndex")
//            try {
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//
//                // Skeleton loading view
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.VISIBLE)
//                views.setViewVisibility(R.id.content_container, android.view.View.GONE)
//
//                // No tasks state
//                if (tasks.isEmpty()) {
//                    Log.d("TaskWidgetProvider", "No tasks available for widget ID: $appWidgetId")
//                    views.setTextViewText(R.id.task_title, "I am sleeping! 😴")
//                    views.setTextViewText(R.id.total_score, "")
//                    views.setViewVisibility(R.id.subtasks_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.snake_loader, android.view.View.GONE)
//                    views.setViewVisibility(R.id.time_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.navigation_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.stats_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                if (currentIndex < 0 || currentIndex >= tasks.size) {
//                    Log.e("TaskWidgetProvider", "Invalid currentIndex: $currentIndex for tasks size: ${tasks.size}")
//                    views.setTextViewText(R.id.task_title, "Error: Invalid Task Index")
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                val widgetTask = tasks[currentIndex]
//                val task = widgetTask.task
//                Log.d("TaskWidgetProvider", "Displaying task: ${task.title}, score: ${widgetTask.totalScore}")
//
//                // Set category-based background gradient
//                val (gradientStart, gradientEnd) = when (task.category?.lowercase()) {
//                    "routine" -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#81C784")) // Green gradient
//                    "study" -> Pair(Color.parseColor("#2196F3"), Color.parseColor("#64B5F6")) // Blue gradient
//                    "work" -> Pair(Color.parseColor("#F44336"), Color.parseColor("#EF5350")) // Red gradient
//                    else -> Pair(Color.parseColor("#B0BEC5"), Color.parseColor("#CFD8DC")) // Grey gradient
//                }
//                views.setInt(R.id.widget_background, "setBackgroundResource", R.drawable.widget_background_gradient)
//                // Note: We'll define the gradient in drawable
//
//                // Set task data
//                views.setTextViewText(R.id.task_title, task.title)
//                views.setTextViewText(R.id.total_score, "${widgetTask.totalScore}")
//
//                // Set subtasks
//                views.removeAllViews(R.id.subtasks_container)
//                task.subtasks?.forEachIndexed { index, subTask ->
//                    val subTaskView = RemoteViews(context.packageName, R.layout.item_subtask_widget)
//                    subTaskView.setTextViewText(R.id.subtask_name, subTask.title)
//                    subTaskView.setTextViewText(R.id.subtask_score, "${subTask.baseScore} pts")
//                    when (subTask.measurementType) {
//                        "Quant" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_quant_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Quant")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#7C3AED"))
//                        }
//                        "Time" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_time_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Time")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#0D9488"))
//                        }
//                        "DeepWork" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_deepwork_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "DeepWork")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#1E40AF"))
//                        }
//                        "Binary" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_binary_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Binary")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#B45309"))
//                        }
//                    }
//                    views.addView(R.id.subtasks_container, subTaskView)
//                }
//
//                // Set snake loader
//                val duration = task.duration // Duration in seconds
//                val progressPercentage = if (duration > 0) (progress / duration.toFloat()) * 100f else 0f
//                views.setInt(R.id.snake_loader, "setProgress", progressPercentage.toInt())
//                views.setFloat(R.id.snake_loader, "setAlpha", if (snakeActive) 0.8f else 0.4f)
//                views.setViewPadding(R.id.snake_loader, (snakePosition * 2).toInt(), 0, 0, 0)
//
//                // Calculate time left using raw LocalDateTime
//                val currentTime = LocalDateTime.now()
//                val timeLeftSeconds = java.time.Duration.between(currentTime, widgetTask.endTimeRaw).seconds
//                val timeLeftText = if (timeLeftSeconds > 0) {
//                    val hours = timeLeftSeconds / 3600
//                    val minutes = (timeLeftSeconds % 3600) / 60
//                    val seconds = timeLeftSeconds % 60
//                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
//                } else {
//                    "-${String.format("%02d:%02d:%02d", -timeLeftSeconds / 3600, (-timeLeftSeconds % 3600) / 60, -timeLeftSeconds % 60)}"
//                }
//                views.setTextViewText(R.id.time_left, timeLeftText)
//                views.setTextViewText(R.id.start_time, widgetTask.startTime)
//                views.setTextViewText(R.id.end_time, widgetTask.endTime)
//
//                // Set navigation buttons
//                val prevIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_PREVIOUS
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val nextIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_NEXT
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val prevPendingIntent = PendingIntent.getBroadcast(
//                    context, appWidgetId * 2, prevIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//                val nextPendingIntent = PendingIntent.getBroadcast(
//                    context, appWidgetId * 2 + 1, nextIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//                views.setOnClickPendingIntent(R.id.btn_previous, prevPendingIntent)
//                views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
//
//                // Enable/disable navigation buttons
//                views.setViewVisibility(R.id.btn_previous, if (currentIndex > 0) android.view.View.VISIBLE else android.view.View.GONE)
//                views.setViewVisibility(R.id.btn_next, if (currentIndex < tasks.size - 1) android.view.View.VISIBLE else android.view.View.GONE)
//
//                // Set navigation dots
//                for (i in 0 until 5) {
//                    val dotId = context.resources.getIdentifier("dot_$i", "id", context.packageName)
//                    if (dotId == 0) {
//                        Log.w("TaskWidgetProvider", "Dot ID dot_$i not found")
//                        continue
//                    }
//                    if (i < tasks.size) {
//                        views.setInt(dotId, "setBackgroundResource", if (i == currentIndex) R.drawable.dot_active else R.drawable.dot_inactive)
//                        views.setViewVisibility(dotId, android.view.View.VISIBLE)
//                    } else {
//                        views.setViewVisibility(dotId, android.view.View.GONE)
//                    }
//                }
//
//                // Set task stats (for previous tasks in history)
//                views.removeAllViews(R.id.stats_container)
//                taskHistory.forEachIndexed { index, taskIndex ->
//                    if (index >= tasks.size) return@forEachIndexed
//                    val statTask = tasks[taskIndex]
//                    val statView = RemoteViews(context.packageName, R.layout.item_stat)
//                    statView.setTextViewText(R.id.stat_title, statTask.task.title)
//                    statView.setTextViewText(R.id.stat_score, "${statTask.totalScore}")
//                    val statColor = when (statTask.task.category?.lowercase()) {
//                        "routine" -> Color.parseColor("#4CAF50")
//                        "study" -> Color.parseColor("#2196F3")
//                        "work" -> Color.parseColor("#F44336")
//                        else -> Color.parseColor("#B0BEC5")
//                    }
//                    statView.setInt(R.id.stat_indicator, "setBackgroundColor", statColor)
//                    views.addView(R.id.stats_container, statView)
//                }
//
//                // Show content after loading
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
//                views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
//
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Failed to update widget ID: $appWidgetId: ${e.message}", e)
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//                views.setTextViewText(R.id.task_title, "Error Loading Widget")
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            }
//        }
//
//        private fun scheduleAutoReturn(context: Context, tasks: List<WidgetTask>, currentIndex: Int, taskHistory: List<Int>) {
//            autoReturnTimer?.cancel()
//            autoReturnTimer = Timer()
//            autoReturnTimer?.schedule(AUTO_RETURN_DELAY) {
//                handler.post {
//                    updateWidget(context, AppWidgetManager.getInstance(context), AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java)).firstOrNull() ?: return@post, tasks, 0, listOf(0))
//                }
//            }
//        }
//    }
//
//    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
//        Log.d("TaskWidgetProvider", "onUpdate called with ${appWidgetIds.size} widget IDs")
//        appContext = context.applicationContext
//        for (appWidgetId in appWidgetIds) {
//            updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//        }
//    }
//
//    override fun onEnabled(context: Context) {
//        super.onEnabled(context)
//        Log.d("TaskWidgetProvider", "Widget enabled")
//        appContext = context.applicationContext
//    }
//
//    override fun onDisabled(context: Context) {
//        super.onDisabled(context)
//        Log.d("TaskWidgetProvider", "Widget disabled")
//        timer?.cancel()
//        timer = null
//        autoReturnTimer?.cancel()
//        autoReturnTimer = null
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        super.onReceive(context, intent)
//        Log.d("TaskWidgetProvider", "onReceive action: ${intent.action}")
//        val appWidgetManager = AppWidgetManager.getInstance(context)
//        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//        Log.d("TaskWidgetProvider", "onReceive found ${appWidgetIds.size} widget IDs")
//
//        val tasksJson = intent.getStringExtra(EXTRA_TASKS) ?: gson.toJson(currentTasks)
//        val tasks = try {
//            val type = object : com.google.gson.reflect.TypeToken<List<WidgetTask>>() {}.type
//            gson.fromJson<List<WidgetTask>>(tasksJson, type) ?: currentTasks
//        } catch (e: Exception) {
//            Log.e("TaskWidgetProvider", "Failed to deserialize tasks: ${e.message}", e)
//            currentTasks
//        }
//        var currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
//        var taskHistory = intent.getIntegerArrayListExtra("task_history") ?: listOf(0)
//
//        when (intent.action) {
//            ACTION_PREVIOUS -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
//                    taskHistory = taskHistory + currentIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
//                }
//            }
//            ACTION_NEXT -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    currentIndex = (currentIndex + 1).coerceAtMost(tasks.size - 1)
//                    taskHistory = taskHistory + currentIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
//                }
//            }
//        }
//    }
//}


//package com.technource.android.ETMS.Micro
//
//import android.annotation.SuppressLint
//import android.app.PendingIntent
//import android.appwidget.AppWidgetManager
//import android.appwidget.AppWidgetProvider
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.widget.RemoteViews
//import com.google.gson.Gson
//import com.technource.android.R
//import com.technource.android.local.Task
//import com.technource.android.system_status.SystemStatus
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//import java.util.Locale
//import java.util.Timer
//import kotlin.concurrent.schedule
//
//data class WidgetTask(
//    val task: Task,
//    val startTime: String, // Formatted string for display (e.g., "1:39 am")
//    val endTime: String,   // Formatted string for display (e.g., "2:39 am")
//    val totalScore: Int,
//    val startTimeRaw: LocalDateTime, // Raw LocalDateTime for calculations
//    val endTimeRaw: LocalDateTime    // Raw LocalDateTime for calculations
//) : java.io.Serializable
//
//class TaskWidgetProvider : AppWidgetProvider() {
//
//    companion object {
//        private const val ACTION_PREVIOUS = "com.technource.android.ETMS.PREVIOUS"
//        private const val ACTION_NEXT = "com.technource.android.ETMS.NEXT"
//        private const val EXTRA_TASKS = "extra_tasks"
//        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
//        private const val MAX_NAVIGATION = 3
//        private const val AUTO_RETURN_DELAY = 30_000L // 30 seconds
//
//        private val handler = Handler(Looper.getMainLooper())
//        private var progress = 45f // Starting progress in seconds
//        private var snakePosition = 0f // For snake loader animation
//        private var snakeActive = true // For snake loader pulsing
//        private var timer: Timer? = null
//        private var autoReturnTimer: Timer? = null
//        private var currentTasks: List<WidgetTask> = emptyList()
//        private var appContext: Context? = null
//        private val gson = Gson()
//
//        fun updateHomeScreenWidget(context: Context, tasks: List<Task>, startTime: LocalDateTime, endTime: LocalDateTime) {
//            Log.d("TaskWidgetProvider", "updateHomeScreenWidget called with ${tasks.size} tasks")
//            appContext = context.applicationContext
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
//            Log.d("TaskWidgetProvider", "Found ${appWidgetIds.size} widget IDs: ${appWidgetIds.joinToString()}")
//
//            // Format times for display
//            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
//            val startTimeStr = startTime.format(timeFormatter)
//            val endTimeStr = endTime.format(timeFormatter)
//
//            // Create WidgetTasks
//            currentTasks = tasks.map { task ->
//                val totalScore = task.subtasks?.sumOf { it.baseScore } ?: 0
//                WidgetTask(task, startTimeStr, endTimeStr, totalScore, startTime, endTime)
//            }
//            Log.d("TaskWidgetProvider", "Created ${currentTasks.size} WidgetTasks")
//
//            // Test serialization
//            try {
//                if (currentTasks.isNotEmpty()) {
//                    val json = gson.toJson(currentTasks)
//                    val deserialized = gson.fromJson(json, Array<WidgetTask>::class.java)
//                    Log.d("TaskWidgetProvider", "Serialization test: ${deserialized.size} tasks")
//                }
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Serialization test failed: ${e.message}", e)
//            }
//
//            // Update all widgets
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//
//            // Start animations
//            startAnimations(context)
//
//            SystemStatus.logEvent("TaskIterator", "Home screen widget updated for: ${tasks.firstOrNull()?.title ?: "No tasks"}")
//        }
//
//        private fun startAnimations(context: Context) {
//            timer?.cancel()
//            timer = Timer()
//            Log.d("TaskWidgetProvider", "Starting animations")
//            timer?.schedule(200, 200) { // Faster animation for smoothness
//                progress += 0.1f
//                if (progress > 3600f) progress = 0f // Reset after 1 hour
//                snakePosition = (snakePosition + 2) % 100
//                snakeActive = !snakeActive
//                updateAllWidgets(context)
//            }
//        }
//
//        private fun updateAllWidgets(context: Context) {
//            val appWidgetManager = AppWidgetManager.getInstance(context)
//            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//            Log.d("TaskWidgetProvider", "Updating all widgets: ${appWidgetIds.size} IDs")
//            for (appWidgetId in appWidgetIds) {
//                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//            }
//        }
//
//        @SuppressLint("RemoteViewLayout")
//        private fun updateWidget(
//            context: Context,
//            appWidgetManager: AppWidgetManager,
//            appWidgetId: Int,
//            tasks: List<WidgetTask>,
//            currentIndex: Int,
//            taskHistory: List<Int>
//        ) {
//            Log.d("TaskWidgetProvider", "Updating widget ID: $appWidgetId, tasks: ${tasks.size}, currentIndex: $currentIndex")
//            try {
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//
//                // Skeleton loading view
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.VISIBLE)
//                views.setViewVisibility(R.id.content_container, android.view.View.GONE)
//
//                // No tasks state
//                if (tasks.isEmpty()) {
//                    Log.d("TaskWidgetProvider", "No tasks available for widget ID: $appWidgetId")
//                    views.setTextViewText(R.id.task_title, "I am sleeping! 😴")
//                    views.setTextViewText(R.id.total_score, "")
//                    views.setViewVisibility(R.id.subtasks_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.snake_loader, android.view.View.GONE)
//                    views.setViewVisibility(R.id.time_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.navigation_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.stats_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                if (currentIndex < 0 || currentIndex >= tasks.size) {
//                    Log.e("TaskWidgetProvider", "Invalid currentIndex: $currentIndex for tasks size: ${tasks.size}")
//                    views.setTextViewText(R.id.task_title, "Error: Invalid Task Index")
//                    appWidgetManager.updateAppWidget(appWidgetId, views)
//                    return
//                }
//
//                val widgetTask = tasks[currentIndex]
//                val task = widgetTask.task
//                Log.d("TaskWidgetProvider", "Displaying task: ${task.title}, score: ${widgetTask.totalScore}")
//
//                // Set category-based background gradient
//                val (gradientStart, gradientEnd) = when (task.category?.lowercase()) {
//                    "routine" -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#81C784")) // Green gradient
//                    "study" -> Pair(Color.parseColor("#2196F3"), Color.parseColor("#64B5F6")) // Blue gradient
//                    "work" -> Pair(Color.parseColor("#F44336"), Color.parseColor("#EF5350")) // Red gradient
//                    else -> Pair(Color.parseColor("#B0BEC5"), Color.parseColor("#CFD8DC")) // Grey gradient
//                }
//                views.setInt(R.id.widget_background, "setBackgroundResource", R.drawable.widget_background_gradient)
//
//                // Set task data
//                views.setTextViewText(R.id.task_title, task.title)
//                views.setTextViewText(R.id.total_score, "${widgetTask.totalScore}")
//
//                // Set subtasks
//                views.removeAllViews(R.id.subtasks_container)
//                task.subtasks?.forEachIndexed { index, subTask ->
//                    val subTaskView = RemoteViews(context.packageName, R.layout.item_subtask_widget)
//                    subTaskView.setTextViewText(R.id.subtask_name, subTask.title)
//                    subTaskView.setTextViewText(R.id.subtask_score, "${subTask.baseScore} pts")
//                    when (subTask.measurementType) {
//                        "Quant" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_quant_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Quant")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#7C3AED"))
//                        }
//                        "Time" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_time_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Time")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#0D9488"))
//                        }
//                        "DeepWork" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_deepwork_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "DeepWork")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#1E40AF"))
//                        }
//                        "Binary" -> {
//                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_binary_background)
//                            subTaskView.setTextViewText(R.id.subtask_category, "Binary")
//                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#B45309"))
//                        }
//                    }
//                    views.addView(R.id.subtasks_container, subTaskView)
//                }
//
//                // Set snake loader
//                val duration = task.duration // Duration in seconds
//                val progressPercentage = if (duration > 0) (progress / duration.toFloat()) * 100f else 0f
//                views.setInt(R.id.snake_loader, "setProgress", progressPercentage.toInt())
//                views.setFloat(R.id.snake_loader, "setAlpha", if (snakeActive) 0.8f else 0.4f)
//                views.setViewPadding(R.id.snake_loader, (snakePosition * 2).toInt(), 0, 0, 0)
//
//                // Calculate time left using raw LocalDateTime
//                val currentTime = LocalDateTime.now()
//                val timeLeftSeconds = java.time.Duration.between(currentTime, widgetTask.endTimeRaw).seconds
//                val timeLeftText = if (timeLeftSeconds > 0) {
//                    val hours = timeLeftSeconds / 3600
//                    val minutes = (timeLeftSeconds % 3600) / 60
//                    val seconds = timeLeftSeconds % 60
//                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
//                } else {
//                    "-${String.format("%02d:%02d:%02d", -timeLeftSeconds / 3600, (-timeLeftSeconds % 3600) / 60, -timeLeftSeconds % 60)}"
//                }
//                views.setTextViewText(R.id.time_left, timeLeftText)
//                views.setTextViewText(R.id.start_time, widgetTask.startTime)
//                views.setTextViewText(R.id.end_time, widgetTask.endTime)
//
//                // Set navigation buttons
//                val prevIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_PREVIOUS
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val nextIntent = Intent(context, TaskWidgetProvider::class.java).apply {
//                    action = ACTION_NEXT
//                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
//                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
//                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
//                }
//                val prevPendingIntent = PendingIntent.getBroadcast(
//                    context, appWidgetId * 2, prevIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//                val nextPendingIntent = PendingIntent.getBroadcast(
//                    context, appWidgetId * 2 + 1, nextIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//                views.setOnClickPendingIntent(R.id.btn_previous, prevPendingIntent)
//                views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
//
//                // Enable/disable navigation buttons
//                views.setViewVisibility(R.id.btn_previous, if (currentIndex > 0) android.view.View.VISIBLE else android.view.View.GONE)
//                views.setViewVisibility(R.id.btn_next, if (currentIndex < tasks.size - 1) android.view.View.VISIBLE else android.view.View.GONE)
//
//                // Set navigation dots
//                for (i in 0 until 5) {
//                    val dotId = context.resources.getIdentifier("dot_$i", "id", context.packageName)
//                    if (dotId == 0) {
//                        Log.w("TaskWidgetProvider", "Dot ID dot_$i not found")
//                        continue
//                    }
//                    if (i < tasks.size) {
//                        views.setInt(dotId, "setBackgroundResource", if (i == currentIndex) R.drawable.dot_widget_active else R.drawable.dot_widget_inactive)
//                        views.setViewVisibility(dotId, android.view.View.VISIBLE)
//                    } else {
//                        views.setViewVisibility(dotId, android.view.View.GONE)
//                    }
//                }
//
//                // Set task stats (for previous tasks in history)
//                views.removeAllViews(R.id.stats_container)
//                taskHistory.forEachIndexed { index, taskIndex ->
//                    if (index >= tasks.size) return@forEachIndexed
//                    val statTask = tasks[taskIndex]
//                    val statView = RemoteViews(context.packageName, R.layout.item_stat)
//                    statView.setTextViewText(R.id.stat_title, statTask.task.title)
//                    statView.setTextViewText(R.id.stat_score, "${statTask.totalScore}")
//                    val statColor = when (statTask.task.category?.lowercase()) {
//                        "routine" -> Color.parseColor("#4CAF50")
//                        "study" -> Color.parseColor("#2196F3")
//                        "work" -> Color.parseColor("#F44336")
//                        else -> Color.parseColor("#B0BEC5")
//                    }
//                    statView.setInt(R.id.stat_indicator, "setBackgroundColor", statColor)
//                    views.addView(R.id.stats_container, statView)
//                }
//
//                // Show content after loading
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
//                views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
//
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            } catch (e: Exception) {
//                Log.e("TaskWidgetProvider", "Failed to update widget ID: $appWidgetId", e)
//                val views = RemoteViews(context.packageName, R.layout.widget_layout)
//                views.setTextViewText(R.id.task_title, "Error Loading Widget: ${e.message}")
//                appWidgetManager.updateAppWidget(appWidgetId, views)
//            }
//        }
//
//        private fun scheduleAutoReturn(context: Context, tasks: List<WidgetTask>, currentIndex: Int, taskHistory: List<Int>) {
//            autoReturnTimer?.cancel()
//            autoReturnTimer = Timer()
//            autoReturnTimer?.schedule(AUTO_RETURN_DELAY) {
//                handler.post {
//                    updateWidget(context, AppWidgetManager.getInstance(context), AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java)).firstOrNull() ?: return@post, tasks, 0, listOf(0))
//                }
//            }
//        }
//    }
//
//    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
//        Log.d("TaskWidgetProvider", "onUpdate called with ${appWidgetIds.size} widget IDs")
//        appContext = context.applicationContext
//        for (appWidgetId in appWidgetIds) {
//            updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//        }
//    }
//
//    override fun onEnabled(context: Context) {
//        super.onEnabled(context)
//        Log.d("TaskWidgetProvider", "Widget enabled")
//        appContext = context.applicationContext
//    }
//
//    override fun onDisabled(context: Context) {
//        super.onDisabled(context)
//        Log.d("TaskWidgetProvider", "Widget disabled")
//        timer?.cancel()
//        timer = null
//        autoReturnTimer?.cancel()
//        autoReturnTimer = null
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        super.onReceive(context, intent)
//        Log.d("TaskWidgetProvider", "onReceive action: ${intent.action}")
//        val appWidgetManager = AppWidgetManager.getInstance(context)
//        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
//        Log.d("TaskWidgetProvider", "onReceive found ${appWidgetIds.size} widget IDs")
//
//        val tasksJson = intent.getStringExtra(EXTRA_TASKS) ?: gson.toJson(currentTasks)
//        val tasks = try {
//            val type = object : com.google.gson.reflect.TypeToken<List<WidgetTask>>() {}.type
//            gson.fromJson<List<WidgetTask>>(tasksJson, type) ?: currentTasks
//        } catch (e: Exception) {
//            Log.e("TaskWidgetProvider", "Failed to deserialize tasks: ${e.message}", e)
//            currentTasks
//        }
//        var currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
//        var taskHistory = intent.getIntegerArrayListExtra("task_history") ?: listOf(0)
//
//        when (intent.action) {
//            ACTION_PREVIOUS -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
//                    taskHistory = taskHistory + currentIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
//                }
//            }
//            ACTION_NEXT -> {
//                if (taskHistory.size < MAX_NAVIGATION) {
//                    currentIndex = (currentIndex + 1).coerceAtMost(tasks.size - 1)
//                    taskHistory = taskHistory + currentIndex
//                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
//                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
//                }
//            }
//        }
//    }
//}

package com.technource.android.ETMS.micro

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.technource.android.R
import com.technource.android.local.Task
import com.technource.android.system_status.SystemStatus
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.Timer
import kotlin.concurrent.schedule

data class WidgetTask(
    val task: Task,
    val startTime: String, // Formatted string for display (e.g., "1:39 am")
    val endTime: String,   // Formatted string for display (e.g., "2:39 am")
    val totalScore: Int,
    val startTimeRaw: LocalDateTime, // Raw LocalDateTime for calculations
    val endTimeRaw: LocalDateTime    // Raw LocalDateTime for calculations
) : java.io.Serializable

class TaskWidgetProvider : AppWidgetProvider() {

    companion object {
        private val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, object : JsonSerializer<LocalDateTime>,
                JsonDeserializer<LocalDateTime> {
                override fun serialize(src: LocalDateTime, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                    return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                }

                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDateTime {
                    return LocalDateTime.parse(json.asJsonPrimitive.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            })
            .create()
        private const val ACTION_PREVIOUS = "com.technource.android.ETMS.PREVIOUS"
        private const val ACTION_NEXT = "com.technource.android.ETMS.NEXT"
        private const val EXTRA_TASKS = "extra_tasks"
        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
        private const val MAX_NAVIGATION = 3
        private const val AUTO_RETURN_DELAY = 30_000L // 30 seconds

        private val handler = Handler(Looper.getMainLooper())
        private var progress = 45f // Starting progress in seconds
        private var snakePosition = 0f // For snake loader animation
        private var snakeActive = true // For snake loader pulsing
        private var timer: Timer? = null
        private var autoReturnTimer: Timer? = null
        private var currentTasks: List<WidgetTask> = emptyList()
        private var appContext: Context? = null
//        private val gson = Gson()

        fun updateHomeScreenWidget(context: Context, tasks: List<Task>, startTime: LocalDateTime, endTime: LocalDateTime) {
            Log.d("TaskWidgetProvider", "updateHomeScreenWidget called with ${tasks.size} tasks")
            appContext = context.applicationContext
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TaskWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            Log.d("TaskWidgetProvider", "Found ${appWidgetIds.size} widget IDs: ${appWidgetIds.joinToString()}")

            // Format times for display
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            val startTimeStr = startTime.format(timeFormatter)
            val endTimeStr = endTime.format(timeFormatter)

            // Create WidgetTasks
            currentTasks = tasks.map { task ->
                val totalScore = task.subtasks?.sumOf { it.baseScore } ?: 0
                WidgetTask(task, startTimeStr, endTimeStr, totalScore, startTime, endTime)
            }
            Log.d("TaskWidgetProvider", "Created ${currentTasks.size} WidgetTasks")

            // Test serialization
            try {
                if (currentTasks.isNotEmpty()) {
                    val json = gson.toJson(currentTasks)
                    val deserialized = gson.fromJson(json, Array<WidgetTask>::class.java)
                    Log.d("TaskWidgetProvider", "Serialization test: ${deserialized.size} tasks")
                }
            } catch (e: Exception) {
                Log.e("TaskWidgetProvider", "Serialization test failed: ${e.message}", e)
            }

            // Update all widgets
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
            }

            // Start animations
            startAnimations(context)
            SystemStatus.logEvent("TaskIterator", "Home screen widget updated for: ${tasks.firstOrNull()?.title ?: "No tasks"}")
        }

        private fun startAnimations(context: Context) {
            timer?.cancel()
            timer = Timer()
            Log.d("TaskWidgetProvider", "Starting animations")
            timer?.schedule(200, 200) { // Faster animation for smoothness
                progress += 0.1f
                if (progress > 3600f) progress = 0f // Reset after 1 hour
                snakePosition = (snakePosition + 2) % 100
                snakeActive = !snakeActive
                updateAllWidgets(context)
            }
        }

        private fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
            Log.d("TaskWidgetProvider", "Updating all widgets: ${appWidgetIds.size} IDs")
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
            }
        }

        @SuppressLint("RemoteViewLayout")
        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            tasks: List<WidgetTask>,
            currentIndex: Int,
            taskHistory: List<Int>
        ) {
            Log.d("TaskWidgetProvider", "Updating widget ID: $appWidgetId, tasks: ${tasks.size}, currentIndex: $currentIndex")
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)

//                // Skeleton loading view
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.content_container, android.view.View.GONE)
//

//                // No tasks state
                if (tasks.isEmpty()) {
                    Log.d("TaskWidgetProvider", "No tasks available for widget ID: $appWidgetId")
                    views.setTextViewText(R.id.task_title, "I am sleeping! 😴")
                    views.setTextViewText(R.id.total_score, "")
                    views.setViewVisibility(R.id.subtasks_container, android.view.View.GONE)
                    views.setViewVisibility(R.id.snake_loader, android.view.View.GONE)
                    views.setViewVisibility(R.id.time_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.navigation_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.stats_container, android.view.View.GONE)
//                    views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
                    views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                if (currentIndex < 0 || currentIndex >= tasks.size) {
                    Log.e("TaskWidgetProvider", "Invalid currentIndex: $currentIndex for tasks size: ${tasks.size}")
                    views.setTextViewText(R.id.task_title, "Error: Invalid Task Index")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    return
                }

                val widgetTask = tasks[currentIndex]
                val task = widgetTask.task
                Log.d("TaskWidgetProvider", "Displaying task: ${task.title}, score: ${widgetTask.totalScore}, ${task.taskId}, ${task.subtasks}, ${task.category}, ${task.color}, ${task.completionStatus}, ${task.duration}, ${task.endTime}, ${task.startTime}, ${task.status}, ${task.isExpanded}")
//
//                // Set category-based background gradient
                //not getting used
                val (gradientStart, gradientEnd) = when (task.category?.lowercase()) {
                    "routine" -> Pair(Color.parseColor("#4CAF50"), Color.parseColor("#81C784")) // Green gradient
                    "study" -> Pair(Color.parseColor("#2196F3"), Color.parseColor("#64B5F6")) // Blue gradient
                    "work" -> Pair(Color.parseColor("#F44336"), Color.parseColor("#EF5350")) // Red gradient
                    else -> Pair(Color.parseColor("#B0BEC5"), Color.parseColor("#CFD8DC")) // Grey gradient
                }
                views.setInt(R.id.widget_background, "setBackgroundResource", R.drawable.widget_background_gradient)
//
//                // Set task data
                views.setTextViewText(R.id.task_title, task.title )
                views.setTextViewText(R.id.total_score, "${widgetTask.totalScore}")
//
//                // Set subtasks
                views.removeAllViews(R.id.subtasks_container)
                task.subtasks?.forEachIndexed { index, subTask ->
                    val subTaskView = RemoteViews(context.packageName, R.layout.item_subtask_widget)
                    subTaskView.setTextViewText(R.id.subtask_name, subTask.title)
                    subTaskView.setTextViewText(R.id.subtask_score, "${subTask.baseScore} pts")
                    when (subTask.measurementType) {
                        "Quant" -> {
                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_quant_background)
                            subTaskView.setTextViewText(R.id.subtask_category, "Quant")
                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#7C3AED"))
                        }
                        "Time" -> {
                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_time_background)
                            subTaskView.setTextViewText(R.id.subtask_category, "Time")
                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#0D9488"))
                        }
                        "DeepWork" -> {
                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_deepwork_background)
                            subTaskView.setTextViewText(R.id.subtask_category, "DeepWork")
                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#1E40AF"))
                        }
                        "Binary" -> {
                            subTaskView.setInt(R.id.subtask_category, "setBackgroundResource", R.drawable.subtask_binary_background)
                            subTaskView.setTextViewText(R.id.subtask_category, "Binary")
                            subTaskView.setTextColor(R.id.subtask_category, Color.parseColor("#B45309"))
                        }
                    }
                    views.addView(R.id.subtasks_container, subTaskView)
                }
//
//                // Set snake loader
                val duration = task.duration // Duration in seconds
                val progressPercentage = if (duration > 0) (progress / duration.toFloat()) * 100f else 0f
                views.setInt(R.id.snake_loader, "setProgress", progressPercentage.toInt())
                views.setFloat(R.id.snake_loader, "setAlpha", if (snakeActive) 0.8f else 0.4f)
                views.setViewPadding(R.id.snake_loader, (snakePosition * 2).toInt(), 0, 0, 0)

//                // Calculate time left using raw LocalDateTime
                val currentTime = LocalDateTime.now()
                val timeLeftSeconds = java.time.Duration.between(currentTime, widgetTask.endTimeRaw).seconds
                val timeLeftText = if (timeLeftSeconds > 0) {
                    val hours = timeLeftSeconds / 3600
                    val minutes = (timeLeftSeconds % 3600) / 60
                    val seconds = timeLeftSeconds % 60
                    String.format("%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    "-${String.format("%02d:%02d:%02d", -timeLeftSeconds / 3600, (-timeLeftSeconds % 3600) / 60, -timeLeftSeconds % 60)}"
                }
                views.setTextViewText(R.id.time_left, timeLeftText)
                views.setTextViewText(R.id.start_time, widgetTask.startTime)
                views.setTextViewText(R.id.end_time, widgetTask.endTime)

//                // Set navigation buttons
                val prevIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                    action = ACTION_PREVIOUS
                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
                }
                val nextIntent = Intent(context, TaskWidgetProvider::class.java).apply {
                    action = ACTION_NEXT
                    putExtra(EXTRA_TASKS, gson.toJson(tasks))
                    putExtra(EXTRA_CURRENT_INDEX, currentIndex)
                    putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId * 2, prevIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId * 2 + 1, nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
//                views.setOnClickPendingIntent(R.id.btn_previous, prevPendingIntent)
//                views.setOnClickPendingIntent(R.id.btn_next, nextPendingIntent)
//
//                // Enable/disable navigation buttons
//                views.setViewVisibility(R.id.btn_previous, if (currentIndex > 0) android.view.View.VISIBLE else android.view.View.GONE)
//                views.setViewVisibility(R.id.btn_next, if (currentIndex < tasks.size - 1) android.view.View.VISIBLE else android.view.View.GONE)
//
//                // Set navigation dots
//                for (i in 0 until 5) {
//                    val dotId = context.resources.getIdentifier("dot_$i", "id", context.packageName)
//                    if (dotId == 0) {
//                        Log.w("TaskWidgetProvider", "Dot ID dot_$i not found")
//                        continue
//                    }
//                    if (i < tasks.size) {
//                        views.setInt(dotId, "setBackgroundResource", if (i == currentIndex) R.drawable.dot_widget_active else R.drawable.dot_widget_inactive)
//                        views.setViewVisibility(dotId, android.view.View.VISIBLE)
//                    } else {
//                        views.setViewVisibility(dotId, android.view.View.GONE)
//                    }
//                }

//                // Set task stats (for previous tasks in history)
//                views.removeAllViews(R.id.stats_container)
//                taskHistory.forEachIndexed { index, taskIndex ->
//                    if (index >= tasks.size) return@forEachIndexed
//                    val statTask = tasks[taskIndex]
//                    val statView = RemoteViews(context.packageName, R.layout.item_stat)
//                    statView.setTextViewText(R.id.stat_title, statTask.task.title)
//                    statView.setTextViewText(R.id.stat_score, "${statTask.totalScore}")
//                    val statColor = when (statTask.task.category?.lowercase()) {
//                        "routine" -> Color.parseColor("#4CAF50")
//                        "study" -> Color.parseColor("#2196F3")
//                        "work" -> Color.parseColor("#F44336")
//                        else -> Color.parseColor("#B0BEC5")
//                    }
//                    statView.setInt(R.id.stat_indicator, "setBackgroundColor", statColor)
//                    views.addView(R.id.stats_container, statView)
//                }
//
//                // Show content after loading
//                views.setViewVisibility(R.id.skeleton_container, android.view.View.GONE)
                views.setViewVisibility(R.id.content_container, android.view.View.VISIBLE)
//
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Log.e("TaskWidgetProvider", "Failed to update widget ID: $appWidgetId", e)
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.task_title, "Error Loading Widget: ${e.message}")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun scheduleAutoReturn(context: Context, tasks: List<WidgetTask>, currentIndex: Int, taskHistory: List<Int>) {
            autoReturnTimer?.cancel()
            autoReturnTimer = Timer()
            autoReturnTimer?.schedule(AUTO_RETURN_DELAY) {
                handler.post {
                    updateWidget(context, AppWidgetManager.getInstance(context), AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java)).firstOrNull() ?: return@post, tasks, 0, listOf(0))
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("TaskWidgetProvider", "onUpdate called with ${appWidgetIds.size} widget IDs")
        appContext = context.applicationContext
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.d("TaskWidgetProvider", "Widget enabled")
        appContext = context.applicationContext
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Log.d("TaskWidgetProvider", "Widget disabled")
        timer?.cancel()
        timer = null
        autoReturnTimer?.cancel()
        autoReturnTimer = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d("TaskWidgetProvider", "onReceive action: ${intent.action}")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TaskWidgetProvider::class.java))
        Log.d("TaskWidgetProvider", "onReceive found ${appWidgetIds.size} widget IDs")

        val tasksJson = intent.getStringExtra(EXTRA_TASKS) ?: gson.toJson(currentTasks)
        val tasks = try {
            val type = object : com.google.gson.reflect.TypeToken<List<WidgetTask>>() {}.type
            gson.fromJson<List<WidgetTask>>(tasksJson, type) ?: currentTasks
        } catch (e: Exception) {
            Log.e("TaskWidgetProvider", "Failed to deserialize tasks: ${e.message}", e)
            currentTasks
        }
        var currentIndex = intent.getIntExtra(EXTRA_CURRENT_INDEX, 0)
        var taskHistory = intent.getIntegerArrayListExtra("task_history") ?: listOf(0)

        when (intent.action) {
            ACTION_PREVIOUS -> {
                if (taskHistory.size < MAX_NAVIGATION) {
                    currentIndex = (currentIndex - 1).coerceAtLeast(0)
                    taskHistory = taskHistory + currentIndex
                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
                }
            }
            ACTION_NEXT -> {
                if (taskHistory.size < MAX_NAVIGATION) {
                    currentIndex = (currentIndex + 1).coerceAtMost(tasks.size - 1)
                    taskHistory = taskHistory + currentIndex
                    updateWidget(context, appWidgetManager, appWidgetIds.firstOrNull() ?: return, tasks, currentIndex, taskHistory)
                    scheduleAutoReturn(context, tasks, currentIndex, taskHistory)
                }
            }
        }
    }
}