//package com.technource.android.ETMS.micro
//
//import android.app.WallpaperManager
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.os.Handler
//import android.os.Looper
//import java.util.Arrays
//import java.util.Timer
//import java.util.TimerTask
//import kotlin.math.min
//
//class DynamicWallpaperTaskIterator : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        if ("com.example.dynamictaskwallpaper.APPLY_WALLPAPER" == intent.action) {
//            startTaskWallpaperCycle(context)
//        }
//    }
//
//    private class TaskMock(var title: String, var subtasks: List<String>)
//    companion object {
//        private val MOCK_TASKS: List<TaskMock> = Arrays.asList(
//            TaskMock("Morning Workout", mutableListOf("Warm-up", "Push-ups", "Stretching")),
//            TaskMock("Deep Work Block", mutableListOf("Research AGI", "Code Refactor")),
//            TaskMock("Team Sync", mutableListOf("Update Board", "Demo Feature", "Q&A"))
//        )
//
//        private var currentTaskIndex = 0
//        private const val CHANGE_INTERVAL_MS = (30 * 1000 // 30 second
//                ).toLong()
//        private var lastUpdateTime: Long = 0
//        private var timer: Timer? = null
//
//        @Synchronized
//        fun startTaskWallpaperCycle(context: Context) {
//            lastUpdateTime = System.currentTimeMillis()
//            updateWallpaper(context) // Initial draw
//
//            if (timer != null) timer!!.cancel()
//
//            timer = Timer()
//
//            // Timer for updating the wallpaper with progress every 1 second
//            timer!!.schedule(object : TimerTask() {
//                override fun run() {
//                    Handler(Looper.getMainLooper()).post {
//                        updateWallpaper(
//                            context
//                        )
//                    }
//                }
//            }, 0, 1000) // update every second
//
//            // Timer for switching task every 30 seconds
//            timer!!.schedule(object : TimerTask() {
//                override fun run() {
//                    Handler(Looper.getMainLooper()).post {
//                        lastUpdateTime =
//                            System.currentTimeMillis()
//                        currentTaskIndex =
//                            (currentTaskIndex + 1) % MOCK_TASKS.size
//                    }
//                }
//            }, CHANGE_INTERVAL_MS, CHANGE_INTERVAL_MS)
//        }
//
//
//        private fun updateWallpaper(context: Context) {
//            val task = MOCK_TASKS[currentTaskIndex]
//            currentTaskIndex = (currentTaskIndex + 1) % MOCK_TASKS.size
//
//            val width = context.resources.displayMetrics.widthPixels
//            val height = context.resources.displayMetrics.heightPixels
//            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(bmp)
//            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
//
//            // Background
//            canvas.drawColor(Color.BLACK)
//
//            // Title
//            paint.color = Color.WHITE
//            paint.textAlign = Paint.Align.CENTER
//            paint.textSize = width / 15f
//            canvas.drawText("Current Task:", width / 2f, height / 6f, paint)
//
//            // Task Name
//            paint.textSize = width / 12f
//            canvas.drawText(task.title, width / 2f, height / 6f + 80, paint)
//
//            // Subtasks
//            paint.textSize = width / 25f
//            var subtaskY = height / 2 - 50
//            for (sub in task.subtasks) {
//                canvas.drawText("- $sub", width / 2f, subtaskY.toFloat(), paint)
//                subtaskY += 50
//            }
//
//            // Loader
//            val progress = calculateProgress()
//            val loaderWidth = width * 0.7f
//            val loaderHeight = 20f
//            val loaderX = (width - loaderWidth) / 2f
//            val loaderY = (height - 150).toFloat()
//
//            // Background
//            paint.color = Color.GRAY
//            canvas.drawRect(loaderX, loaderY, loaderX + loaderWidth, loaderY + loaderHeight, paint)
//
//            // Foreground (progress)
//            paint.color = Color.GREEN
//            val filledWidth = loaderWidth * progress
//            canvas.drawRect(loaderX, loaderY, loaderX + filledWidth, loaderY + loaderHeight, paint)
//
//            try {
//                WallpaperManager.getInstance(context).setBitmap(bmp)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                bmp.recycle()
//            }
//        }
//
//        private fun calculateProgress(): Float {
//            val currentTime = System.currentTimeMillis()
//            val elapsedTime = currentTime - lastUpdateTime
//            return min(1.0, (elapsedTime.toFloat() / CHANGE_INTERVAL_MS).toDouble())
//                .toFloat()
//        }
//
//        fun applyWallpaperIntent(context: Context) {
//            val intent = Intent(context, DynamicWallpaperTaskIterator::class.java)
//            intent.setAction("com.example.dynamictaskwallpaper.APPLY_WALLPAPER")
//            context.sendBroadcast(intent)
//        }
//    }
//}




package com.technource.android.ETMS.micro

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.res.ResourcesCompat
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
        private val MOCK_TASKS = listOf(
            Task(
                id = "task1",
                title = "Morning Workout",
                category = "Health",
                color = "#FF5722", // Deep Orange
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
                color = "#2196F3", // Blue
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
                color = "#9C27B0", // Purple
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
        private const val CHANGE_INTERVAL_MS = (30 * 1000).toLong() // 30 seconds
        private var lastUpdateTime: Long = 0
        private var timer: Timer? = null

        // Animation properties
        private var progressAnimator: ValueAnimator? = null
        private var animatedProgress: Float = 0f
        private var pulseAnimator: ValueAnimator? = null
        private var pulseValue: Float = 0f

        // Cached objects to avoid GC pressure
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        private val path = Path()
        private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val gradientColors = IntArray(3)
        private val matrix = Matrix()

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
            }, 0, 1000) // update every second

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
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    animatedProgress = animation.animatedValue as Float
                }
                start()
            }

            // Pulse animator for subtle effects
            pulseAnimator = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
                duration = 2000
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { animation ->
                    pulseValue = animation.animatedValue as Float
                }
                start()
            }
        }

        private fun resetAnimations() {
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = CHANGE_INTERVAL_MS
                interpolator = AccelerateDecelerateInterpolator()
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
                WallpaperManager.getInstance(context).setBitmap(bmp)
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
            // Parse the task color
            val taskColorInt = Color.parseColor(currentTask.color)
            val taskColorDark = ColorUtils.blendARGB(taskColorInt, Color.BLACK, 0.7f)
            val taskColorLight = ColorUtils.blendARGB(taskColorInt, Color.WHITE, 0.3f)

            // Create a beautiful gradient background
            gradientColors[0] = taskColorDark
            gradientColors[1] = taskColorInt
            gradientColors[2] = taskColorLight

            val gradient = LinearGradient(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            )

            // Apply a subtle rotation to the gradient for visual interest
            matrix.reset()
            matrix.setRotate(45f, width / 2f, height / 2f)
            gradient.setLocalMatrix(matrix)

            paint.shader = gradient
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            paint.shader = null

            // Add subtle pattern overlay for texture
            drawPatternOverlay(canvas, width, height, taskColorInt)

            // Draw the main task card
            drawTaskCard(canvas, width, height, currentTask)

            // Draw the next task preview
            drawNextTaskPreview(canvas, width, height, nextTask)

            // Draw the custom animated loader
            drawCustomLoader(canvas, width, height, taskColorInt)
        }

        private fun drawPatternOverlay(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
            // Create a subtle dot pattern for texture
            paint.color = ColorUtils.setAlphaComponent(baseColor, 40) // Very transparent

            val dotSize = 3f
            val spacing = 30f

            for (x in 0 until width step spacing.toInt()) {
                for (y in 0 until height step spacing.toInt()) {
                    // Add slight randomness to dot positions
                    val offsetX = (Math.random() * 5 - 2.5).toFloat()
                    val offsetY = (Math.random() * 5 - 2.5).toFloat()

                    canvas.drawCircle(
                        x.toFloat() + offsetX,
                        y.toFloat() + offsetY,
                        dotSize,
                        paint
                    )
                }
            }
        }

        private fun drawTaskCard(canvas: Canvas, width: Int, height: Int, task: Task) {
            val cardWidth = width * 0.85f
            val cardHeight = height * 0.5f
            val cardLeft = (width - cardWidth) / 2f
            val cardTop = height * 0.15f

            // Card background with rounded corners
            rect.set(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

            // Draw card shadow
            shadowPaint.color = Color.BLACK
            shadowPaint.alpha = 80
            shadowPaint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(
                rect.left + 5, rect.top + 5,
                rect.right + 5, rect.bottom + 5,
                30f, 30f, shadowPaint
            )

            // Draw glossy card background
            val cardColor = Color.parseColor(task.color)
            val cardGradient = LinearGradient(
                rect.left, rect.top,
                rect.left, rect.bottom,
                ColorUtils.blendARGB(cardColor, Color.WHITE, 0.2f),
                ColorUtils.blendARGB(cardColor, Color.BLACK, 0.1f),
                Shader.TileMode.CLAMP
            )

            paint.shader = cardGradient
            paint.alpha = 230 // Slightly transparent
            canvas.drawRoundRect(rect, 30f, 30f, paint)
            paint.shader = null

            // Add glossy highlight at the top
            path.reset()
            path.moveTo(rect.left + 30f, rect.top + 2f)
            path.lineTo(rect.right - 30f, rect.top + 2f)
            path.quadTo(rect.right - 10f, rect.top + 15f, rect.right - 40f, rect.top + 40f)
            path.quadTo(rect.left + (rect.width() / 2f), rect.top + 25f, rect.left + 40f, rect.top + 40f)
            path.quadTo(rect.left + 10f, rect.top + 15f, rect.left + 30f, rect.top + 2f)

            paint.color = Color.WHITE
            paint.alpha = 90
            canvas.drawPath(path, paint)

            // Draw category indicator
            val categoryIndicatorWidth = 8f
            paint.color = ColorUtils.blendARGB(cardColor, Color.WHITE, 0.5f)
            rect.set(
                cardLeft, cardTop,
                cardLeft + categoryIndicatorWidth, cardTop + cardHeight
            )
            canvas.drawRoundRect(
                rect,
                categoryIndicatorWidth/2, categoryIndicatorWidth/2,
                paint
            )

            // Draw task title with custom typography
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 14f
            textPaint.isFakeBoldText = true
            textPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK)

            // Add a subtle text glow effect based on pulse animation
            val glowRadius = 2f + (pulseValue * 2f)
            textPaint.setShadowLayer(glowRadius, 0f, 0f, ColorUtils.setAlphaComponent(Color.WHITE, (50 + pulseValue * 50).toInt()))

            val titleX = cardLeft + 30f
            val titleY = cardTop + 60f
            canvas.drawText(task.title, titleX, titleY, textPaint)

            // Draw task time
            textPaint.textSize = width / 25f
            textPaint.isFakeBoldText = false
            textPaint.setShadowLayer(1f, 1f, 1f, Color.BLACK)

            val startTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(task.startTime).atZone(DateFormatter.IST_ZONE)
                    .toInstant()
                    .toEpochMilli()
            )

            val endTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(task.endTime).atZone(DateFormatter.IST_ZONE)
                    .toInstant()
                    .toEpochMilli()
            )

            val timeText = "$startTime - $endTime"

            canvas.drawText(timeText, titleX, titleY + 40f, textPaint)

            // Draw task score with a circular background
            val scoreRadius = width / 16f
            val scoreX = rect.right - scoreRadius - 20f
            val scoreY = titleY - scoreRadius / 2

            // Score circle background
            paint.color = ColorUtils.setAlphaComponent(Color.WHITE, 50)
            canvas.drawCircle(scoreX, scoreY, scoreRadius, paint)

            // Score text
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 18f
            textPaint.isFakeBoldText = true
            textPaint.textAlign = Paint.Align.CENTER

            canvas.drawText(
                task.taskScore.toInt().toString(),
                scoreX,
                scoreY + textPaint.textSize / 3,
                textPaint
            )

            // Small "pts" text
            textPaint.textSize = width / 40f
            textPaint.isFakeBoldText = false
            canvas.drawText("pts", scoreX, scoreY + scoreRadius / 2 + 10f, textPaint)

            // Reset text align
            textPaint.textAlign = Paint.Align.LEFT

            // Draw subtasks
            drawSubtasks(canvas, task, cardLeft + 20f, titleY + 80f, cardWidth - 40f)

            // Draw status indicator
            val statusText = when (task.status) {
                TaskStatus.RUNNING -> "IN PROGRESS"
                TaskStatus.UPCOMING -> "UPCOMING"
                TaskStatus.LOGGED -> "COMPLETED"
                TaskStatus.MISSED -> "MISSED"
                else -> "SCHEDULED"
            }

            val statusColor = when (task.status) {
                TaskStatus.RUNNING -> Color.parseColor("#4CAF50") // Green
                TaskStatus.UPCOMING -> Color.parseColor("#2196F3") // Blue
                TaskStatus.LOGGED -> Color.parseColor("#9C27B0") // Purple
                TaskStatus.MISSED -> Color.parseColor("#F44336") // Red
                else -> Color.parseColor("#FF9800") // Orange
            }

            // Status pill
            val statusWidth = 120f
            val statusHeight = 30f
            val statusLeft = rect.right - statusWidth - 20f
            val statusTop = rect.bottom - statusHeight - 20f

            rect.set(statusLeft, statusTop, statusLeft + statusWidth, statusTop + statusHeight)
            paint.color = statusColor
            canvas.drawRoundRect(rect, statusHeight / 2, statusHeight / 2, paint)

            // Status text
            textPaint.color = Color.WHITE
            textPaint.textSize = statusHeight * 0.6f
            textPaint.isFakeBoldText = true
            textPaint.textAlign = Paint.Align.CENTER

            canvas.drawText(
                statusText,
                statusLeft + statusWidth / 2,
                statusTop + statusHeight * 0.7f,
                textPaint
            )

            // Reset text align
            textPaint.textAlign = Paint.Align.LEFT
        }

        private fun drawSubtasks(canvas: Canvas, task: Task, x: Float, y: Float, width: Float) {
            val subtasks = task.subtasks ?: return
            if (subtasks.isEmpty()) return

            var currentY = y

            // Subtasks header
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 20f
            textPaint.isFakeBoldText = true
            canvas.drawText("Subtasks", x, currentY, textPaint)
            currentY += 30f

            // Reset text properties
            textPaint.isFakeBoldText = false
            textPaint.textSize = width / 25f

            // Draw each subtask
            for (subtask in subtasks) {
                // Subtask background
                rect.set(x, currentY, x + width - 20f, currentY + 60f)
                paint.color = ColorUtils.setAlphaComponent(Color.WHITE, 30)
                canvas.drawRoundRect(rect, 10f, 10f, paint)

                // Subtask title
                textPaint.color = Color.WHITE
                canvas.drawText(subtask.title, x + 10f, currentY + 25f, textPaint)

                // Subtask measurement info
                val measurementText = when (subtask.measurementType) {
                    "time" -> {
                        val duration = subtask.time?.setDuration ?: 0
                        val minutes = duration / 60
                        "$minutes min"
                    }
                    "quant" -> {
                        val target = subtask.quant?.targetValue ?: 0
                        val unit = subtask.quant?.targetUnit ?: ""
                        "$target $unit"
                    }
                    "binary" -> "Yes/No"
                    "deepwork" -> "Deep Work"
                    else -> ""
                }

                // Draw measurement type
                textPaint.textSize = width / 30f
                canvas.drawText(
                    measurementText,
                    x + 10f,
                    currentY + 50f,
                    textPaint
                )

                // Draw score
                textPaint.textAlign = Paint.Align.RIGHT
                textPaint.isFakeBoldText = true
                canvas.drawText(
                    "${subtask.finalScore.toInt()}/${subtask.baseScore}",
                    x + width - 30f,
                    currentY + 35f,
                    textPaint
                )

                // Reset text properties
                textPaint.textAlign = Paint.Align.LEFT
                textPaint.isFakeBoldText = false
                textPaint.textSize = width / 25f

                // Move to next subtask
                currentY += 70f
            }
        }

        private fun drawNextTaskPreview(canvas: Canvas, width: Int, height: Int, nextTask: Task) {
            val previewWidth = width * 0.85f
            val previewHeight = height * 0.15f
            val previewLeft = (width - previewWidth) / 2f
            val previewTop = height * 0.7f

            // Preview background with rounded corners
            rect.set(previewLeft, previewTop, previewLeft + previewWidth, previewTop + previewHeight)

            // Draw preview shadow
            shadowPaint.color = Color.BLACK
            shadowPaint.alpha = 60
            shadowPaint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(
                rect.left + 5, rect.top + 5,
                rect.right + 5, rect.bottom + 5,
                20f, 20f, shadowPaint
            )

            // Draw preview background
            val nextTaskColor = Color.parseColor(nextTask.color)
            paint.color = ColorUtils.setAlphaComponent(nextTaskColor, 180)
            canvas.drawRoundRect(rect, 20f, 20f, paint)

            // Draw "Next Up" label
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 30f
            textPaint.isFakeBoldText = true
            textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)

            canvas.drawText("NEXT UP", previewLeft + 20f, previewTop + 30f, textPaint)

            // Draw next task title
            textPaint.textSize = width / 20f
            canvas.drawText(
                nextTask.title,
                previewLeft + 20f,
                previewTop + 70f,
                textPaint
            )

            // Draw next task time
            textPaint.textSize = width / 30f
            textPaint.isFakeBoldText = false

            val startTime = DateFormatter.formatDisplayTimeFromMillis(
                DateFormatter.parseIsoDateTime(nextTask.startTime)
                    .atZone(DateFormatter.IST_ZONE)
                    .toInstant()
                    .toEpochMilli()
            )
            canvas.drawText(
                "Starting at $startTime",
                previewLeft + 20f,
                previewTop + 100f,
                textPaint
            )

            // Draw next task score
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.isFakeBoldText = true
            textPaint.textSize = width / 20f

            canvas.drawText(
                "${nextTask.taskScore.toInt()} pts",
                previewLeft + previewWidth - 20f,
                previewTop + 70f,
                textPaint
            )

            // Reset text align
            textPaint.textAlign = Paint.Align.LEFT
        }

        private fun drawCustomLoader(canvas: Canvas, width: Int, height: Int, baseColor: Int) {
            val loaderY = height * 0.9f
            val loaderHeight = height * 0.03f
            val loaderWidth = width * 0.85f
            val loaderLeft = (width - loaderWidth) / 2f

            // Calculate progress
            val progress = animatedProgress

            // Draw time text
            textPaint.color = Color.WHITE
            textPaint.textSize = width / 30f
            textPaint.isFakeBoldText = true
            textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
            textPaint.textAlign = Paint.Align.CENTER

            // Calculate remaining time
            val totalDuration = CHANGE_INTERVAL_MS / 1000
            val remainingTime = (totalDuration * (1 - progress)).toInt()
            val remainingText = if (remainingTime > 0) "${remainingTime}s" else "Changing..."

            canvas.drawText(
                remainingText,
                width / 2f,
                loaderY - 20f,
                textPaint
            )

            // Reset text align
            textPaint.textAlign = Paint.Align.LEFT

            // Draw loader background
            rect.set(loaderLeft, loaderY, loaderLeft + loaderWidth, loaderY + loaderHeight)
            paint.color = ColorUtils.setAlphaComponent(Color.WHITE, 50)
            canvas.drawRoundRect(rect, loaderHeight / 2, loaderHeight / 2, paint)

            // Draw loader progress with animated gradient
            val progressWidth = loaderWidth * progress
            rect.set(loaderLeft, loaderY, loaderLeft + progressWidth, loaderY + loaderHeight)

            // Create animated gradient for loader
            val loaderGradient = LinearGradient(
                rect.left, rect.top,
                rect.right, rect.top,
                intArrayOf(
                    baseColor,
                    ColorUtils.blendARGB(baseColor, Color.WHITE, 0.7f),
                    baseColor
                ),
                null,
                Shader.TileMode.CLAMP
            )

            // Animate the gradient
            matrix.reset()
            matrix.setTranslate(animatedProgress * width, 0f)
            loaderGradient.setLocalMatrix(matrix)

            paint.shader = loaderGradient
            canvas.drawRoundRect(rect, loaderHeight / 2, loaderHeight / 2, paint)
            paint.shader = null

            // Draw pulsing glow effect around the progress bar
            shadowPaint.color = baseColor
            shadowPaint.alpha = (100 * pulseValue).toInt()
            shadowPaint.maskFilter = BlurMaskFilter(10f + (5f * pulseValue), BlurMaskFilter.Blur.NORMAL)
            canvas.drawRoundRect(rect, loaderHeight / 2, loaderHeight / 2, shadowPaint)

            // Draw small circles along the progress bar for visual interest
            paint.color = Color.WHITE
            val numDots = 5
            val dotSpacing = progressWidth / (numDots + 1)

            for (i in 1..numDots) {
                val dotX = loaderLeft + (i * dotSpacing)
                val dotRadius = 3f + (2f * pulseValue)
                canvas.drawCircle(dotX, loaderY + (loaderHeight / 2), dotRadius, paint)
            }
        }

        private fun calculateProgress(): Float {
            val currentTime = System.currentTimeMillis()
            val elapsedTime = currentTime - lastUpdateTime
            return min(1.0, (elapsedTime.toFloat() / CHANGE_INTERVAL_MS).toDouble()).toFloat()
        }

        fun applyWallpaperIntent(context: Context) {
            val intent = Intent(context, DynamicWallpaperTaskIterator::class.java)
            intent.action = "com.example.dynamictaskwallpaper.APPLY_WALLPAPER"
            context.sendBroadcast(intent)
        }
    }
}
