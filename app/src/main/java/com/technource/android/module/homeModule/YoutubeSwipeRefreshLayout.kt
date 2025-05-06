package com.technource.android.module.homeModule

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.technource.android.R

/**
 * Custom SwipeRefreshLayout that mimics YouTube's pull-to-refresh animation
 * with a wave-like effect and custom progress indicator
 */
class YoutubeSwipeRefreshLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SwipeRefreshLayout(context, attrs) {

    private val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.primary)
        style = Paint.Style.FILL
        alpha = 80
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }

    private val wavePath = Path()
    private var waveHeight = 0f
    private var waveWidth = 0f
    private var waveOffset = 0f
    private var progressAngle = 0f
    private var isAnimating = false

    private val waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        interpolator = DecelerateInterpolator()
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            waveOffset = it.animatedValue as Float * 360f
            invalidate()
        }
    }

    private val progressAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 1500
        interpolator = DecelerateInterpolator()
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            progressAngle = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        setColorSchemeColors(ContextCompat.getColor(context, R.color.primary))
        setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context, R.color.background))
        setOnRefreshListener(null) // Will be set by the activity
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        waveWidth = w.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isRefreshing && !isAnimating) {
            startAnimations()
        } else if (!isRefreshing && isAnimating) {
            stopAnimations()
        }

        if (isRefreshing) {
            // Draw wave effect
            wavePath.reset()
            wavePath.moveTo(0f, 0f)

            var x = 0f
            while (x <= waveWidth) {
                val y = (Math.sin((x / waveWidth * 4 * Math.PI) + Math.toRadians(waveOffset.toDouble())) * waveHeight).toFloat()
                wavePath.lineTo(x, y)
                x += 10
            }

            wavePath.lineTo(waveWidth, 0f)
            wavePath.close()

            canvas.drawPath(wavePath, wavePaint)

            // Draw circular progress
            val centerX = waveWidth / 2
            val centerY = waveHeight * 2
            val radius = 30f

            canvas.drawArc(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius,
                0f,
                progressAngle,
                false,
                progressPaint
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        waveHeight = measuredHeight * 0.05f // 5% of height
    }

    private fun startAnimations() {
        isAnimating = true
        waveAnimator.start()
        progressAnimator.start()
    }

    private fun stopAnimations() {
        isAnimating = false
        waveAnimator.cancel()
        progressAnimator.cancel()
    }

    override fun setOnRefreshListener(listener: OnRefreshListener?) {
        super.setOnRefreshListener {
            listener?.onRefresh()
            // Auto hide after 2 seconds if still refreshing
            postDelayed({
                if (isRefreshing) {
                    isRefreshing = false
                }
            }, 2000)
        }
    }
}
