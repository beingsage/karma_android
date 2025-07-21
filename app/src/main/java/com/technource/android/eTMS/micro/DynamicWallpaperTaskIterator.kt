package com.technource.android.eTMS.micro

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import com.technource.android.local.SubTask
import com.technource.android.local.Task
import com.technource.android.local.TaskStatus
import com.technource.android.utils.DateFormatter
import java.util.*
import kotlin.math.min

class DynamicWallpaperTaskIterator : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if ("com.example.dynamictaskwallpaper.APPLY_WALLPAPER" == intent.action) {
            startTaskWallpaperCycle(context)
        }
    }

    companion object {
        // Mock data for testing
        private var MOCK_TASKS = listOf(
            Task(
                id = "task1",
                title = "Morning Workout",
                category = "Health",
                color = "#FFFFFF", // Set to white for text
                startTime = "2025-05-02T06:30:00.000Z",
                endTime = "2025-05-02T07:30:00.000Z",
                duration = 3600,
                subtasks = listOf(
                    SubTask(
                        id = "sub1",
                        title = "Warm-up",
                        measurementType = "time",
                        baseScore = 10,
                        completionStatus = 0.8f,
                        finalScore = 8f,
                        binary = null,
                        time = com.technource.android.local.TimeMeasurement(
                            setDuration = 600,
                            timeSpent = 480
                        ),
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub1_id"
                    ),
                    SubTask(
                        id = "sub2",
                        title = "Push-ups",
                        measurementType = "quant",
                        baseScore = 15,
                        completionStatus = 1.0f,
                        finalScore = 15f,
                        binary = null,
                        time = null,
                        quant = com.technource.android.local.QuantMeasurement(
                            targetValue = 20,
                            targetUnit = "reps",
                            achievedValue = 20
                        ),
                        deepwork = null,
                        subTaskId = "sub2_id"
                    ),
                    SubTask(
                        id = "sub3",
                        title = "Stretching",
                        measurementType = "binary",
                        baseScore = 5,
                        completionStatus = 1.0f,
                        finalScore = 5f,
                        binary = com.technource.android.local.BinaryMeasurement(
                            completed = true
                        ),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub3_id"
                    )
                ),
                taskScore = 28f,
                taskId = "task1_id",
                isExpanded = false,
                completionStatus = 0.9f,
                status = TaskStatus.RUNNING
            ),
            Task(
                id = "task2",
                title = "Deep Work Block",
                category = "Work",
                 color = "#FFFFFF", // Set to white for text
                startTime = "2025-05-02T09:00:00.000Z",
                endTime = "2025-05-02T12:00:00.000Z",
                duration = 10800,
                subtasks = listOf(
                    SubTask(
                        id = "sub4",
                        title = "Research AGI",
                        measurementType = "deepwork",
                        baseScore = 30,
                        completionStatus = 0.7f,
                        finalScore = 21f,
                        binary = null,
                        time = null,
                        quant = null,
                        deepwork = com.technource.android.local.DeepWorkMeasurement(
                            template = "research",
                            deepworkScore = 21
                        ),
                        subTaskId = "sub4_id"
                    ),
                    SubTask(
                        id = "sub5",
                        title = "Code Refactor",
                        measurementType = "time",
                        baseScore = 25,
                        completionStatus = 0.6f,
                        finalScore = 15f,
                        binary = null,
                        time = com.technource.android.local.TimeMeasurement(
                            setDuration = 7200,
                            timeSpent = 4320
                        ),
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub5_id"
                    )
                ),
                taskScore = 36f,
                taskId = "task2_id",
                isExpanded = false,
                completionStatus = 0.65f,
                status = TaskStatus.UPCOMING
            ),
            Task(
                id = "task3",
                title = "Team Sync",
                category = "Meeting",
                 color = "#FFFFFF", // Set to white for text
                startTime = "2025-05-02T14:00:00.000Z",
                endTime = "2025-05-02T15:00:00.000Z",
                duration = 3600,
                subtasks = listOf(
                    SubTask(
                        id = "sub6",
                        title = "Update Board",
                        measurementType = "binary",
                        baseScore = 10,
                        completionStatus = 0.0f,
                        finalScore = 0f,
                        binary = com.technource.android.local.BinaryMeasurement(
                            completed = false
                        ),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub6_id"
                    ),
                    SubTask(
                        id = "sub7",
                        title = "Demo Feature",
                        measurementType = "binary",
                        baseScore = 20,
                        completionStatus = 0.0f,
                        finalScore = 0f,
                        binary = com.technource.android.local.BinaryMeasurement(
                            completed = false
                        ),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub7_id"
                    ),
                    SubTask(
                        id = "sub8",
                        title = "Q&A",
                        measurementType = "time",
                        baseScore = 15,
                        completionStatus = 0.0f,
                        finalScore = 0f,
                        binary = null,
                        time = com.technource.android.local.TimeMeasurement(
                            setDuration = 1800,
                            timeSpent = 0
                        ),
                        quant = null,
                        deepwork = null,
                        subTaskId = "sub8_id"
                    )
                ),
                taskScore = 45f,
                taskId = "task3_id",
                isExpanded = false,
                completionStatus = 0.0f,
                status = TaskStatus.UPCOMING
            )
        )

        private var currentTaskIndex = 0
        private var nextTaskIndex = 1
        private var CHANGE_INTERVAL_MS = (30 * 1000).toLong() // Default, but will be set dynamically
        private var lastUpdateTime: Long = 0
        private var timer: Timer? = null

        // Animation properties
        private var progressAnimator: ValueAnimator? = null
        private var animatedProgress: Float = 0f

        // Cached objects to avoid GC pressure
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()

        @Synchronized
        fun startTaskWallpaperCycle(context: Context) {
            lastUpdateTime = System.currentTimeMillis()

            // Initialize animators
            setupAnimators()

            // Initial draw
            updateWallpaper(context)

            if (timer != null) timer!!.cancel()
            timer = Timer()

            // Timer for updating the wallpaper with progress every 1 second
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        updateWallpaper(context)
                    }
                }
            }, 0, 5000) // update every second

            // Timer for switching task every 30 seconds
            timer!!.schedule(object : TimerTask() {
                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        lastUpdateTime = System.currentTimeMillis()
                        currentTaskIndex = (currentTaskIndex + 1) % MOCK_TASKS.size
                        nextTaskIndex = (currentTaskIndex + 1) % MOCK_TASKS.size

                        // Reset and restart animations
                        resetAnimations()
                    }
                }
            }, CHANGE_INTERVAL_MS, CHANGE_INTERVAL_MS)
        }

        private fun setupAnimators() {
            // Progress animator
            progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CHANGE_INTERVAL_MS
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    animatedProgress = animation.animatedValue as Float
                }
                start()
            }
        }

        private fun resetAnimations() {
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CHANGE_INTERVAL_MS
                interpolator = LinearInterpolator()
                addUpdateListener { animation ->
                    animatedProgress = animation.animatedValue as Float
                }
                start()
            }
        }

        private fun updateWallpaper(context: Context) {
            val currentTask = MOCK_TASKS[currentTaskIndex]
            val nextTask = MOCK_TASKS[nextTaskIndex]

            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels

            // Create bitmap with hardware acceleration if possible
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)

            // Draw the wallpaper
            drawWallpaper(canvas, width, height, currentTask, nextTask, context)

            try {
                // Set wallpaper only for lock screen
                val wallpaperManager = WallpaperManager.getInstance(context)
                wallpaperManager.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bmp.recycle()
            }
        }

        private fun drawWallpaper(
            canvas: Canvas,
            width: Int,
            height: Int,
            currentTask: Task,
            nextTask: Task,
            context: Context
        ) {

            paint.color = Color.BLACK
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
           
            // Draw the main task card
            drawTaskCard(canvas, width, height, currentTask)

            // Draw the next task preview
            drawNextTaskPreview(canvas, width, height, nextTask)

            // Draw the custom animated loader
            PatagoniaBar(canvas, width, height)
        }

        private fun drawTaskCard(canvas: Canvas, width: Int, height: Int, task: Task) {
            val cardWidth = width * 0.75f
            val cardHeight = height * 0.4f
            val cardLeft = (width - cardWidth) / 2f
            val cardTop = height * 0.15f

                 // Draw card outline
         rect.set(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            // Draw task title
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 16f
            textPaint.isFakeBoldText = true
            val titleX = cardLeft + 20f
            val titleY = cardTop + 80f
            canvas.drawText(task.title, titleX, titleY, textPaint)

            // Draw task time
            textPaint.textSize = width / 25f
            textPaint.isFakeBoldText = false
            val startTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(task.startTime).atZone(DateFormatter.IST_ZONE)
                    .toInstant().toEpochMilli()
            )
            val endTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(task.endTime).atZone(DateFormatter.IST_ZONE)
                    .toInstant().toEpochMilli()
            )
            val timeText = "$startTime - $endTime"
            canvas.drawText(timeText, titleX, titleY + 60f, textPaint)

            // Draw task score
            textPaint.textSize = width / 18f
            textPaint.isFakeBoldText = true
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(
                "${task.taskScore.toInt()} pts",
                cardLeft + cardWidth - 20f,
                titleY,
                textPaint
            )
            textPaint.textAlign = Paint.Align.LEFT

            // Draw subtasks
            drawSubtasks(canvas, task, cardLeft + 20f, titleY + 100f, cardWidth - 40f)
        }

        private fun drawSubtasks(canvas: Canvas, task: Task, x: Float, y: Float, width: Float) {
            val subtasks = task.subtasks ?: return
            if (subtasks.isEmpty()) return

            var currentY = y

            // Subtasks header with underline
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 12f
            textPaint.isFakeBoldText = true
            canvas.drawText("Subtasks", x, currentY + 20f, textPaint)
            
            // Draw header underline
            paint.color = Color.WHITE
            paint.strokeWidth = 2f
            canvas.drawLine(x, currentY + 30f, x + width, currentY + 30f, paint)
            currentY += 60f

            // Draw each subtask
            for (subtask in subtasks) {
                val cardHeight = 100f // Increased height for better spacing
                
                // Subtask card background with slight transparency
                rect.set(x, currentY, x + width, currentY + cardHeight)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#22FFFFFF") // Semi-transparent white
                canvas.drawRoundRect(rect, 12f, 12f, paint)
                
                // Card border
                paint.style = Paint.Style.STROKE
                paint.color = Color.WHITE
                paint.strokeWidth = 2f
                canvas.drawRoundRect(rect, 12f, 12f, paint)

                // Subtask title with icon based on type
                textPaint.textSize = width / 18f
                textPaint.isFakeBoldText = true
                val icon = when (subtask.measurementType) {
                    "time" -> "⏱"
                    "quant" -> "📊"
                    "binary" -> "✓"
                    "deepwork" -> "🎯"
                    else -> ""
                }
                canvas.drawText("$icon ${subtask.title}", x + 20f, currentY + 35f, textPaint)

                // Measurement value with better formatting
                val measurementText = when (subtask.measurementType) {
                    "time" -> "${subtask.time?.setDuration?.div(60) ?: 0} minutes"
                    "quant" -> "${subtask.quant?.targetValue ?: 0} ${subtask.quant?.targetUnit ?: ""}"
                    "binary" -> if (subtask.binary?.completed == true) "Complete" else "Pending"
                    "deepwork" -> "Deep Score: ${subtask.deepwork?.deepworkScore ?: 0}"
                    else -> ""
                }
                
                textPaint.textSize = width / 22f
                textPaint.isFakeBoldText = false
                canvas.drawText(measurementText, x + 20f, currentY + 70f, textPaint)

                // Score with percentage
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.isFakeBoldText = true
                val percentage = (subtask.completionStatus * 100).toInt()
                val scoreText = "${subtask.finalScore.toInt()}/${subtask.baseScore} • ${percentage}%"
                canvas.drawText(scoreText, x + width - 20f, currentY + 35f, textPaint)
                textPaint.textAlign = Paint.Align.LEFT

                // Progress bar with rounded corners
                val progressBarHeight = 8f
                val progressBarY = currentY + cardHeight - progressBarHeight - 15f
                val cornerRadius = progressBarHeight / 2

                // Background
                rect.set(x + 20f, progressBarY, x + width - 20f, progressBarY + progressBarHeight)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#44FFFFFF")
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                // Progress
                rect.set(x + 20f, progressBarY, 
                        x + 20f + (width - 40f) * subtask.completionStatus,
                        progressBarY + progressBarHeight)
                paint.color = Color.WHITE
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

                currentY += cardHeight + 20f // Increased spacing between cards
            }
        }

        private fun drawNextTaskPreview(canvas: Canvas, width: Int, height: Int, nextTask: Task) {
            val previewWidth = width * 0.55f
            val previewHeight = height * 0.05f
            val previewLeft = (width - previewWidth) / 2f
            val previewTop = height * 0.675f

            // Draw preview outline
            rect.set(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            paint.style = Paint.Style.FILL

            // Draw next task title
            textPaint.textSize = width / 30f
            canvas.drawText(nextTask.title, previewLeft + 40f, previewTop + 70f, textPaint)

            // Draw next task time
            textPaint.textSize = width / 40f
            textPaint.isFakeBoldText = false
            val startTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(nextTask.startTime)
                    .atZone(DateFormatter.IST_ZONE)
                    .toInstant().toEpochMilli()
            )
            canvas.drawText("Starting at $startTime", previewLeft + 40f, previewTop + 100f, textPaint)

            // Draw next task score
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = width / 30f
            textPaint.isFakeBoldText = true
            canvas.drawText(
                "${nextTask.taskScore.toInt()} pts",
                previewLeft + previewWidth - 20f,
                previewTop + 70f,
                textPaint
            )
            textPaint.textAlign = Paint.Align.LEFT
        }

        private fun  PatagoniaBar(canvas: Canvas, width: Int, height: Int) {
            val loaderY = height * 0.760f
            val loaderHeight = height * 0.03f
            val loaderWidth = width * 0.85f
            val loaderLeft = (width - loaderWidth) / 2f

            // Draw remaining time
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 30f
            textPaint.isFakeBoldText = true
            textPaint.textAlign = Paint.Align.CENTER
            val totalDuration = CHANGE_INTERVAL_MS / 1000
            val remainingTime = (totalDuration * (1 - animatedProgress)).toInt()
            val remainingText = if (remainingTime > 0) "${remainingTime}s" else "Changing..."
            canvas.drawText(remainingText, width / 2f, loaderY - 20f, textPaint)
            textPaint.textAlign = Paint.Align.LEFT

            // Draw progress bar background
            rect.set(loaderLeft, loaderY, loaderLeft + loaderWidth, loaderY + loaderHeight)
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, loaderHeight / 2, loaderHeight / 2, paint)

            // Draw progress
            val progressWidth = loaderWidth * animatedProgress
            rect.set(loaderLeft, loaderY, loaderLeft + progressWidth, loaderY + loaderHeight)
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, loaderHeight / 2, loaderHeight / 2, paint)
        }

        fun applyWallpaperIntent(context: Context) {
            val intent = Intent(context, DynamicWallpaperTaskIterator::class.java)
            intent.action = "com.example.dynamictaskwallpaper.APPLY_WALLPAPER"
            context.sendBroadcast(intent)
        }

        fun setCurrentTask(context: Context, task: Task) {
            MOCK_TASKS = listOf(task)
            currentTaskIndex = 0
            nextTaskIndex = 0
            // Calculate duration in ms from task.startTime and task.endTime
            val start = DateFormatter.parseIsoDateTime(task.startTime)
            val end = DateFormatter.parseIsoDateTime(task.endTime)
            CHANGE_INTERVAL_MS = java.time.Duration.between(start, end).toMillis()
            startTaskWallpaperCycle(context)
        }
    }
}