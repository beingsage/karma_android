package com.technource.android.module.homeModule

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.local.Task
import com.technource.android.local.TaskStatus
import com.technource.android.utils.DateFormatter
import java.time.LocalDateTime

class TimelineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var tasks: List<Task> = emptyList()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dashedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val currentTimePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val currentTimeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val taskDurationPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var recyclerView: RecyclerView? = null
    private var currentTimePosition = 0f
    private var timelineHeight = 0f
    private var dotRadius = 8f
    private var lineWidth = 3f
    private var blinkValue = 0f
    private var currentTimeAnimValue = 0f
    private val blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            blinkValue = it.animatedValue as Float
            invalidate()
        }
    }

    private val currentTimeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            currentTimeAnimValue = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        // Line paint setup
        linePaint.color = ContextCompat.getColor(context, R.color.text_muted)
        linePaint.strokeWidth = lineWidth

        // Dashed line paint setup
        dashedLinePaint.color = ContextCompat.getColor(context, R.color.text_muted)
        dashedLinePaint.strokeWidth = lineWidth
        dashedLinePaint.style = Paint.Style.STROKE
        dashedLinePaint.pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)

        // Time text paint setup
        timeTextPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        timeTextPaint.textSize = 12f
        timeTextPaint.textAlign = Paint.Align.RIGHT
        timeTextPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        // Current time indicator setup
        currentTimePaint.color = ContextCompat.getColor(context, R.color.primary)
        currentTimePaint.strokeWidth = 4f
        currentTimePaint.strokeCap = Paint.Cap.ROUND

        // Current time circle paint
        currentTimeCirclePaint.color = ContextCompat.getColor(context, R.color.primary)
        currentTimeCirclePaint.style = Paint.Style.FILL

        // Task duration paint
        taskDurationPaint.style = Paint.Style.FILL
        taskDurationPaint.alpha = 80

        // Start animations
        blinkAnimator.start()
        currentTimeAnimator.start()
    }

    fun setRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                invalidate() // Redraw timeline on scroll
            }
        })
    }

    fun setTasks(tasks: List<Task>) {
        this.tasks = tasks
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        timelineHeight = h.toFloat() * 2 // Extendable height
        updateCurrentTimePosition()
    }

    private fun updateCurrentTimePosition() {
        val currentTime = DateFormatter.millisToLocalDateTime(System.currentTimeMillis())
        val totalMinutesInDay = 24 * 60
        val minutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
        currentTimePosition = (minutesSinceMidnight.toFloat() / totalMinutesInDay) * timelineHeight
        invalidate()

        // Update every minute
        postDelayed({ updateCurrentTimePosition() }, 60000)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val visibleHeight = height.toFloat()
        val scrollY = (recyclerView?.computeVerticalScrollOffset() ?: 0).toFloat()

        // Draw timeline background
        val timelineWidth = 28f
        val timelineBgPaint = Paint().apply {
            color = Color.argb(15, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.drawRect(centerX - timelineWidth/2, 0f, centerX + timelineWidth/2, visibleHeight, timelineBgPaint)

        // Draw main timeline line
        canvas.drawLine(centerX, 0f, centerX, visibleHeight, linePaint)

        // Draw time stamps (every 2 hours)
        val hoursToShow = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
        timeTextPaint.textSize = 12f

        for (hour in hoursToShow) {
            val minutesSinceMidnight = hour * 60
            val positionY = (minutesSinceMidnight.toFloat() / (24 * 60)) * timelineHeight - scrollY

            if (positionY in 0f..visibleHeight) {
                // Draw hour marker line
                canvas.drawLine(centerX - 10, positionY, centerX + 10, positionY, linePaint)

                // Format hour text (12-hour format with AM/PM)
                val hourText = when {
                    hour == 0 -> "12 AM"
                    hour < 12 -> "$hour AM"
                    hour == 12 -> "12 PM"
                    else -> "${hour - 12} PM"
                }

                // Draw hour text
                canvas.drawText(hourText, centerX - 15, positionY + 4, timeTextPaint)
            }
        }

        // Draw task indicators and durations
        tasks.forEach { task ->
            try {
                val startTime = DateFormatter.parseIsoDateTime(task.startTime)
                val startMinutes = startTime.hour * 60 + startTime.minute
                val topPosition = (startMinutes.toFloat() / (24 * 60)) * timelineHeight - scrollY

                if (topPosition in -50f..visibleHeight + 50f) { // Add padding for partially visible tasks
                    // Set dot color based on task status
                    when (task.status) {
                        TaskStatus.LOGGED -> {
                            dotPaint.color = ContextCompat.getColor(context, R.color.status_completed)
                        }
                        TaskStatus.MISSED -> {
                            dotPaint.color = ContextCompat.getColor(context, R.color.status_missed)
                        }
                        TaskStatus.RUNNING -> {
                            val alpha = (128 + (127 * blinkValue)).toInt()
                            dotPaint.color = Color.argb(alpha, 25, 118, 210) // Blinking In Progress
                        }
                        TaskStatus.UPCOMING -> {
                            dotPaint.color = ContextCompat.getColor(context, R.color.status_upcoming)
                        }
                        TaskStatus.SYSTEM_FAILURE -> {
                            dotPaint.color = Color.parseColor("#FF00FF") // Magenta
                        }
                        null -> {
                            dotPaint.color = ContextCompat.getColor(context, R.color.status_upcoming)
                        }
                    }

                    // Draw task duration background if end time exists
                    task.endTime?.let { endTimeStr ->
                        val endTime = DateFormatter.parseIsoDateTime(endTimeStr)
                        val endMinutes = endTime.hour * 60 + endTime.minute
                        val bottomPosition = (endMinutes.toFloat() / (24 * 60)) * timelineHeight - scrollY

                        if (bottomPosition > topPosition) {
                            // Draw task duration background
                            taskDurationPaint.color = dotPaint.color
                            val durationRect = RectF(
                                centerX - timelineWidth/2,
                                topPosition,
                                centerX + timelineWidth/2,
                                bottomPosition
                            )
                            canvas.drawRoundRect(durationRect, 4f, 4f, taskDurationPaint)
                        }
                    }

                    // Draw task dot
                    canvas.drawCircle(centerX, topPosition, dotRadius, dotPaint)
                }
            } catch (e: Exception) {
                // Handle parsing errors gracefully
            }
        }

        // Draw current time indicator with animation
        if (currentTimePosition in 0f..visibleHeight) {
            // Draw pulsating circle
            val pulseRadius = 6 + (3 * Math.sin(currentTimeAnimValue * Math.PI * 2)).toFloat()
            canvas.drawCircle(centerX, currentTimePosition, pulseRadius, currentTimeCirclePaint)

            // Draw horizontal line
            canvas.drawLine(centerX - 20, currentTimePosition, centerX + 20, currentTimePosition, currentTimePaint)
        }
    }

    // Helper method to get color for task category
    private fun getCategoryColor(category: String?): Int {
        return when (category?.lowercase()) {
            "work" -> ContextCompat.getColor(context, R.color.category_work)
            "personal" -> ContextCompat.getColor(context, R.color.category_personal)
            "health" -> ContextCompat.getColor(context, R.color.category_health)
            "learning" -> ContextCompat.getColor(context, R.color.category_learning)
            else -> ContextCompat.getColor(context, R.color.category_other)
        }
    }
}
