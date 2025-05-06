package com.technource.android.ETMS.micro

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.technource.android.R
import com.technource.android.local.TaskDao
import com.technource.android.local.toTasks
import com.technource.android.local.Task
import com.technource.android.module.homeModule.HomeScreen
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject
import kotlin.concurrent.schedule

@AndroidEntryPoint
class TaskNotificationService : Service() {
    private val CHANNEL_ID = "task_notifications"
    private val NOTIFICATION_ID = 2
    private val NOTIFICATION_ICON = R.drawable.app_logo
    private val handler = Handler(Looper.getMainLooper())
    private var currentTaskIndex = 0
    private var isPreviewing = false
    private var progress = 0f // Progress in seconds
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentTasks: List<Task> = emptyList()
    private val MAX_NAVIGATION = 3
    private var taskHistory = mutableListOf(0)
    private var autoReturnTimer: Timer? = null
    private var progressTimer: Timer? = null
    private val AUTO_RETURN_DELAY = 30_000L // 30 seconds
    private val PROGRESS_UPDATE_INTERVAL = 1000L // Update progress every second
    private val TAG = "TaskNotificationService"
    private lateinit var mediaSession: MediaSessionCompat
    private var currentTaskId: String? = null
    private var startTimeStr: String? = null
    private var endTimeStr: String? = null

    @Inject
    lateinit var taskDao: TaskDao

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        createNotificationChannel()
        Log.d(TAG, "onCreate: TaskDao initialized via Hilt")
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "TaskMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

            // Set media session callback if needed
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {}
                override fun onPause() {}
                override fun onSkipToNext() { handleAction("next") }
                override fun onSkipToPrevious() { handleAction("previous") }
            })

            // Set metadata to make system treat this as media
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, "Karma Tasks and skjoisggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggangioehnioanhion3904u26903h03niynekohniaejy903j90qj690qh3inygnkeslklnkhgsnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnnsoiinohaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "Task Management")
                .build()
            setMetadata(metadata)

            isActive = true
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Task Notifications",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Channel for task-related notifications"
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            setSound(null, null) // No sound for media-style
            // Important for media style
            setBypassDnd(true)
            enableVibration(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun startForegroundImmediately() {
        val intent = Intent(this, HomeScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(NOTIFICATION_ICON)
            .setContentTitle("Karma - Loading")
            .setContentText("Loading tasks...")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, builder.build())
            }
            Log.d(TAG, "Foreground service started with temporary notification")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service immediately: ${e.message}", e)
            stopSelf()
        }
    }

    private fun loadTasksFromDao(currentTaskId: String) {
        if (!::taskDao.isInitialized) {
            Log.e(TAG, "TaskDao is not initialized")
            updateNotificationWithError("Task database not initialized")
            return
        }

        scope.launch {
            try {
                val taskEntities = taskDao.getTasksAround(currentTaskId, MAX_NAVIGATION * 2 + 1) // Fetch 7 tasks (current + 3 prev + 3 next)
                currentTasks = taskEntities.toTasks() // Convert TaskEntity to Task
                if (currentTasks.isNotEmpty()) {
                    currentTaskIndex = currentTasks.indexOfFirst { it.id == currentTaskId }.coerceAtLeast(0)
                    taskHistory = mutableListOf(currentTaskIndex)
                    startProgressTracking()
                    updateNotification()
                    SystemStatus.logEvent(TAG, "Tasks loaded: ${currentTasks.map { it.title }}")
                } else {
                    SystemStatus.logEvent(TAG, "No tasks found around ID: $currentTaskId")
                    updateNotificationWithNoTasks()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load tasks from DAO: ${e.message}", e)
                updateNotificationWithError(e.message)
            }
        }
    }

    private fun startProgressTracking() {
        progressTimer?.cancel()
        progressTimer = Timer()
        progress = 0f
        progressTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (currentTasks.isEmpty()) return
                val task = currentTasks[currentTaskIndex]
                val durationSeconds = task.duration.toFloat()
                if (durationSeconds <= 0) return

                progress += 1f // Increment by 1 second
                if (progress >= durationSeconds) {
                    progress = durationSeconds
                    handler.post {
                        handleAction("next")
                    }
                }
                handler.post {
                    updateNotification()
                }
            }
        }, PROGRESS_UPDATE_INTERVAL, PROGRESS_UPDATE_INTERVAL)
    }

    private fun updateNotificationWithNoTasks() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            stopSelf()
            return
        }

        val intent = Intent(this, HomeScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(NOTIFICATION_ICON)
            .setContentTitle("Karma - now")
            .setContentText("No tasks available")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
    }

    private fun updateNotificationWithError(errorMessage: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            stopSelf()
            return
        }

        val intent = Intent(this, HomeScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(NOTIFICATION_ICON)
            .setContentTitle("Karma - Error")
            .setContentText(errorMessage ?: "Failed to load tasks")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
    }

//    private fun createNotificationBuilder(pendingIntent: PendingIntent): NotificationCompat.Builder {
//        if (currentTasks.isEmpty()) return fallbackNotification(pendingIntent)
//
//        val remoteViews = createRemoteViews()
//        return if (remoteViews != null) {
//            NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(NOTIFICATION_ICON)
//                .setCustomContentView(remoteViews)
//                .setContentIntent(pendingIntent)
//                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//                .setPriority(NotificationCompat.PRIORITY_HIGH)
//                .setOngoing(true)
//                .setShowWhen(false)
////                .setStyle(
////                    androidx.media.app.NotificationCompat.MediaStyle()
////                        .setMediaSession(mediaSession.sessionToken)
////                        .setShowActionsInCompactView(0, 1) // Previous and Next buttons
////                )
//                .apply {
//                    if (remoteViews != null) {
//                        // Set both content view and media style
//                        setCustomContentView(remoteViews)
//                        setStyle(
//                            androidx.media.app.NotificationCompat.MediaStyle()
//                                .setMediaSession(mediaSession.sessionToken)
//                                .setShowActionsInCompactView(0, 1) // Show previous/next in compact view
////                                .setCustomContentView(remoteViews) // This is key for media style
//                        )
//                    } else {
//                        // Fallback with media style
//                        setContentTitle("Karma - now")
//                        setContentText(if (currentTasks.isNotEmpty()) currentTasks[currentTaskIndex].title else "No tasks")
//                        setStyle(
//                            androidx.media.app.NotificationCompat.MediaStyle()
//                                .setMediaSession(mediaSession.sessionToken)
//                        )
//                    }}
//        } else {
//            fallbackNotification(pendingIntent)
//        }
//    }

    private fun createNotificationBuilder(pendingIntent: PendingIntent): NotificationCompat.Builder {
        val remoteViews = createRemoteViews()

        return NotificationCompat.Builder(this, CHANNEL_ID).apply {
            setSmallIcon(NOTIFICATION_ICON)
            setContentIntent(pendingIntent)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setOngoing(true)
            setShowWhen(false)

            if (remoteViews != null) {
                // For the expanded notification
                setCustomContentView(remoteViews)

                // For the collapsed media-style notification
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1) // Indices of actions to show in compact view
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(
                            PendingIntent.getService(
                                this@TaskNotificationService,
                                0,
                                Intent(this@TaskNotificationService, TaskNotificationService::class.java)
                                    .putExtra("action", "cancel"),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                )

                // Add media actions
                addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_previous,
                        "Previous",
                        createPendingIntent("previous")
                    ).build()
                )

                addAction(
                    NotificationCompat.Action.Builder(
                        R.drawable.ic_next,
                        "Next",
                        createPendingIntent("next")
                    ).build()
                )
            } else {
                // Fallback if RemoteViews creation fails
                setContentTitle("Karma - now")
                setContentText(if (currentTasks.isNotEmpty()) currentTasks[currentTaskIndex].title else "No tasks")
                setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                )
            }
        }
    }

    private fun fallbackNotification(pendingIntent: PendingIntent): NotificationCompat.Builder {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(NOTIFICATION_ICON)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(pendingIntent)

        if (currentTasks.isNotEmpty()) {
            val task = currentTasks[currentTaskIndex]
            builder.setContentTitle("Karma: ${task.title}")
                .setContentText("${task.category} • ${task.duration} min")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("${task.title}\n${task.category}\nDuration: ${task.duration} min"))
        } else {
            builder.setContentTitle("Karma - now")
                .setContentText("No tasks available")
        }

        return builder
    }

    @SuppressLint("RemoteViewLayout")
//    private fun createRemoteViews(): RemoteViews? {
////        if (currentTasks.isEmpty()) {
////            Log.d(TAG, "No current tasks - can't create RemoteViews")
////            return null
////        }
//
//        if (currentTasks.isEmpty()) return null
//
//
//
//        return try {
//            Log.d(TAG, "Attempting to create RemoteViews with layout: task_media_notification")
//            RemoteViews(packageName, R.layout.task_media_notification).apply {
//                val task = currentTasks[currentTaskIndex]
//                Log.d(TAG, "$task")
//                // Task Image
//                setImageViewResource(R.id.task_image, R.drawable.ic_task_placeholder)
//                // Title
//                setTextViewText(R.id.task_title, task.title)
//                Log.d(TAG, "Basic text set successfully")
//
//                // Detailed subtitle with category, task score, time, and subtasks
//                var durationMinutes: Long = 0
//                try {
//                    val startTime = DateFormatter.parseIsoDateTime(task.startTime)
//                    val endTime = DateFormatter.parseIsoDateTime(task.endTime)
//                    durationMinutes = ChronoUnit.MINUTES.between(startTime, endTime)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Failed to parse task times: ${e.message}")
//                    // Fallback to raw time strings if parsing fails
//                    setTextViewText(
//                        R.id.task_subtitle,
//                        """
//                        Category: ${task.category}
//                        Score: ${task.taskScore}
//                        Time: ${startTimeStr ?: task.startTime} - ${endTimeStr ?: task.endTime}
//                        Duration: Unknown
//                        No subtasks
//                        """.trimIndent()
//                    )
//                    return@apply
//                }
//
//                val durationSeconds = task.duration.toFloat()
//                val progressPercentage = if (durationSeconds > 0) (progress / durationSeconds) * 100 else 0f
//
//                val subtasksText = task.subtasks?.joinToString("\n") { subtask ->
//                    val measurementInfo = when (subtask.measurementType) {
//                        "binary" -> "Binary"
//                        "time" -> "Time (${subtask.time?.setDuration ?: 0} min)"
//                        "quant" -> "Quant (${subtask.quant?.targetValue ?: 0} ${subtask.quant?.targetUnit ?: ""})"
//                        "deepwork" -> "Deepwork"
//                        else -> "Unknown"
//                    }
//                    "• ${subtask.title} ($measurementInfo, Score: ${subtask.baseScore})"
//                } ?: "No subtasks"
//
//                val subtitle = """
//                    Category: ${task.category}
//                    Score: ${task.taskScore}
//                    Time: ${startTimeStr ?: task.startTime} - ${endTimeStr ?: task.endTime}
//                    Duration: $durationMinutes min
//                    $subtasksText
//                """.trimIndent()
//                setTextViewText(R.id.task_subtitle, subtitle)
//
//                // Progress Bar
//                setProgressBar(R.id.progress_bar, 100, progressPercentage.toInt(), false)
//                setViewVisibility(R.id.progress_bar, View.VISIBLE)
//
//                // Media Controls (only previous and next)
//                setOnClickPendingIntent(R.id.btn_previous, createPendingIntent("previous"))
//                setViewVisibility(R.id.btn_play_pause, View.GONE) // Hide play/pause
//                setOnClickPendingIntent(R.id.btn_next, createPendingIntent("next"))
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to create RemoteViews: ${e.message}", e)
//            null
//        }
//    }

    private fun createRemoteViews(): RemoteViews? {
        if (currentTasks.isEmpty()) return null

        return try {
            RemoteViews(packageName, R.layout.task_media_notification).apply {
                val task = currentTasks[currentTaskIndex]

                // Basic info
                setImageViewResource(R.id.task_image, R.drawable.ic_task_placeholder)
                setTextViewText(R.id.task_title, task.title)

                // Calculate progress
                val durationSeconds = task.duration.toFloat()
                val progressPercentage = if (durationSeconds > 0) (progress / durationSeconds) * 100 else 0f
                setProgressBar(R.id.progress_bar, 100, progressPercentage.toInt(), false)

                // Set subtitle with time info
                val timeText = try {
                    val startTime = DateFormatter.parseIsoDateTime(task.startTime)
                    val endTime = DateFormatter.parseIsoDateTime(task.endTime)
                    val duration = ChronoUnit.MINUTES.between(startTime, endTime)
                    "${startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${endTime.format(DateTimeFormatter.ofPattern("HH:mm"))} ($duration min)"
                } catch (e: Exception) {
                    "${task.startTime} - ${task.endTime}"
                }

                setTextViewText(R.id.task_subtitle, "${task.category} • $timeText")

                // Set button click handlers
                setOnClickPendingIntent(R.id.btn_previous, createPendingIntent("previous"))
                setOnClickPendingIntent(R.id.btn_next, createPendingIntent("next"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "RemoteViews creation failed", e)
            null
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, TaskNotificationService::class.java).apply {
            putExtra("action", action)
            putExtra(EXTRA_CURRENT_INDEX, currentTaskIndex)
            putIntegerArrayListExtra("task_history", ArrayList(taskHistory))
        }
        return PendingIntent.getService(
            this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service immediately with a temporary notification
        startForegroundImmediately()

        intent?.getStringExtra("current_task_id")?.let { taskId ->
            currentTaskId = taskId
            startTimeStr = intent.getStringExtra("start_time")
            endTimeStr = intent.getStringExtra("end_time")
            loadTasksFromDao(taskId)
        } ?: run {
            Log.w(TAG, "No current_task_id provided in intent")
            updateNotificationWithNoTasks()
        }

        intent?.getStringExtra("action")?.let { action ->
            handleAction(action)
        }

        return START_STICKY
    }

    private fun handleAction(action: String) {
        when (action) {
            "previous" -> {
                if (taskHistory.size < MAX_NAVIGATION) {
                    currentTaskIndex = (currentTaskIndex - 1).coerceAtLeast(0)
                    taskHistory.add(currentTaskIndex)
                    progress = 0f // Reset progress when navigating
                    startProgressTracking()
                    updateNotification()
                    scheduleAutoReturn()
                    resetSneakPeekTimer()
                    Log.d(TAG, "Previous task selected: ${currentTasks.getOrNull(currentTaskIndex)?.title ?: "No task"}")
                }
            }
            "next" -> {
                if (taskHistory.size < MAX_NAVIGATION) {
                    currentTaskIndex = (currentTaskIndex + 1).coerceAtMost(currentTasks.size - 1)
                    taskHistory.add(currentTaskIndex)
                    progress = 0f // Reset progress when navigating
                    startProgressTracking()
                    updateNotification()
                    scheduleAutoReturn()
                    resetSneakPeekTimer()
                    Log.d(TAG, "Next task selected: ${currentTasks.getOrNull(currentTaskIndex)?.title ?: "No task"}")
                }
            }
        }
    }

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
            return
        }

        val intent = Intent(this, HomeScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = createNotificationBuilder(pendingIntent)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
    }

    private fun startSneakPeekTimer() {
        scope.launch {
            delay(10_000)
            if (!isPreviewing && currentTasks.size > 1) {
                isPreviewing = true
                val nextIndex = (currentTaskIndex + 1) % currentTasks.size
                val previewText = "Next: ${currentTasks[nextIndex].title}"
                updateNotificationPreview(previewText)
                delay(5_000)
                isPreviewing = false
                updateNotification()
            }
        }
    }

    private fun updateNotificationPreview(previewText: String) {
        val remoteViews = createRemoteViews()?.apply {
            setTextViewText(R.id.task_title, previewText)
            setViewVisibility(R.id.task_subtitle, View.GONE)
            setViewVisibility(R.id.progress_bar, View.GONE)
            setViewVisibility(R.id.btn_previous, View.GONE)
            setViewVisibility(R.id.btn_next, View.GONE)
        }
        if (remoteViews != null) {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setCustomContentView(remoteViews)
                .setStyle(
                    androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.sessionToken)
                        .setShowActionsInCompactView(0, 1)
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, HomeScreen::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setShowWhen(false)

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "POST_NOTIFICATIONS permission not granted")
                return
            }
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun resetSneakPeekTimer() {
        isPreviewing = false
        scope.coroutineContext.cancelChildren()
        startSneakPeekTimer()
    }

    private fun scheduleAutoReturn() {
        autoReturnTimer?.cancel()
        autoReturnTimer = Timer()
        autoReturnTimer?.schedule(AUTO_RETURN_DELAY) {
            handler.post {
                currentTaskIndex = currentTasks.indexOfFirst { it.id == currentTaskId }.coerceAtLeast(0)
                taskHistory = mutableListOf(currentTaskIndex)
                progress = 0f
                startProgressTracking()
                updateNotification()
                resetSneakPeekTimer()
                Log.d(TAG, "Auto-returned to current task")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        progressTimer?.cancel()
        autoReturnTimer?.cancel()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.cancel(NOTIFICATION_ID)
        stopForeground(true)
        mediaSession.release()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_CURRENT_INDEX = "extra_current_index"
        fun startService(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }
    }
}