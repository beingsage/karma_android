package com.technource.android.module.statsModule

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.technource.android.R
import com.technource.android.module.statsModule.adapters.StatsChartPagerAdapter
import com.technource.android.module.statsModule.models.Achievement
import com.technource.android.module.statsModule.models.AchievementType
import com.technource.android.module.statsModule.models.Metric
import com.technource.android.module.statsModule.models.StatsData
import com.technource.android.module.statsModule.models.StatsViewModel
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StatsScreen : AppCompatActivity() {

    private val viewModel: StatsViewModel by viewModels()
    private lateinit var bottomNavigation: BottomNavigationView

    private lateinit var medalContainer: View
    private lateinit var baseMetricsContainer: View
    private lateinit var advancedMetricsContainer: View
    private lateinit var chartsContainer: View
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var loadingView: View
    private lateinit var contentView: View
    private lateinit var dateTextView: TextView
    private lateinit var noDataTextView: TextView

    private val baseMetricViews = mutableListOf<View>()
    private val advancedMetricViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        initViews()
        setupTabLayout()
        setupObservers()

        // Set current date
        val dateFormat = SimpleDateFormat("MMMM d", Locale.getDefault())
        dateTextView.text = dateFormat.format(Date())

        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }

    private fun initViews() {
        medalContainer = findViewById(R.id.medal_container)
        baseMetricsContainer = findViewById(R.id.base_metrics_container)
        advancedMetricsContainer = findViewById(R.id.advanced_metrics_container)
        chartsContainer = findViewById(R.id.charts_container)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        loadingView = findViewById(R.id.loading_view)
        contentView = findViewById(R.id.content_view)
        dateTextView = findViewById(R.id.date_text)
        noDataTextView = findViewById(R.id.no_data_text)

        // Find base metric views
        baseMetricViews.add(findViewById(R.id.metric_completion))
        baseMetricViews.add(findViewById(R.id.metric_score))
        baseMetricViews.add(findViewById(R.id.metric_efficiency))
        baseMetricViews.add(findViewById(R.id.metric_focus))

        // Find advanced metric views
        advancedMetricViews.add(findViewById(R.id.metric_depth_ratio))
        advancedMetricViews.add(findViewById(R.id.metric_task_quality))
        advancedMetricViews.add(findViewById(R.id.metric_efficiency_score))

        // Initially hide content
        medalContainer.alpha = 0f
        baseMetricsContainer.alpha = 0f
        advancedMetricsContainer.alpha = 0f
        chartsContainer.alpha = 0f
        noDataTextView.isVisible = false

        // Set up medal animation
        val medalIcon = findViewById<View>(R.id.medal_icon)
        val pulseAnimator = ObjectAnimator.ofFloat(medalIcon, "alpha", 0.6f, 1.0f)
        pulseAnimator.duration = 1000
        pulseAnimator.repeatCount = ValueAnimator.INFINITE
        pulseAnimator.repeatMode = ValueAnimator.REVERSE
        pulseAnimator.start()
    }

    private fun setupTabLayout() {
        val adapter = StatsChartPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.ic_line_chart)
                1 -> tab.setIcon(R.drawable.ic_pie_chart)
                2 -> tab.setIcon(R.drawable.ic_bar_chart)
                3 -> tab.setIcon(R.drawable.ic_activity)
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
        viewModel.loadStats()
    }

    private fun setupObservers() {
        viewModel.stats.observe(this) { stats ->
            if (stats != null && stats.tasks.isNotEmpty()) {
                updateUI(stats)
                animateContentIn()
                loadingView.isVisible = false
                contentView.isVisible = true
                noDataTextView.isVisible = false
            } else {
                loadingView.isVisible = false
                contentView.isVisible = false
                noDataTextView.isVisible = true
                noDataTextView.text = "No stats available for today"
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            loadingView.isVisible = isLoading
            contentView.isVisible = !isLoading && viewModel.stats.value?.tasks?.isNotEmpty() == true
            noDataTextView.isVisible = !isLoading && viewModel.stats.value?.tasks?.isEmpty() == true
        }

        viewModel.topAchievement.observe(this) { achievement ->
            if (achievement != null) {
                updateMedalUI(achievement)
                medalContainer.isVisible = true
            } else {
                medalContainer.isVisible = false
            }
        }

        viewModel.baseMetrics.observe(this) { metrics ->
            updateBaseMetricsUI(metrics)
        }

        viewModel.advancedMetrics.observe(this) { metrics ->
            updateAdvancedMetricsUI(metrics)
        }
    }

    private fun updateUI(stats: StatsData) {
        // This method will be called when stats are loaded
        // The individual UI components will be updated by their respective observers
        SystemStatus.logEvent("StatsActivity", "Stats updated with completion rate: ${stats.completionRate}")
    }

    private fun updateMedalUI(achievement: Achievement) {
        val titleTextView = findViewById<TextView>(R.id.medal_title)
        val descriptionTextView = findViewById<TextView>(R.id.medal_description)
        val medalContainer = findViewById<View>(R.id.medal_container)

        titleTextView.text = achievement.name
        descriptionTextView.text = achievement.description

        // Set background gradient based on achievement type
        val gradientDrawable = when (achievement.type) {
            AchievementType.DEEP_WORK -> R.drawable.bg_gradient_deep_work
            AchievementType.EFFICIENCY -> R.drawable.bg_gradient_efficiency
            AchievementType.TASK_COMPLETION -> R.drawable.bg_gradient_task
            AchievementType.WORK -> R.drawable.bg_gradient_work
        }

        medalContainer.background = ContextCompat.getDrawable(this, gradientDrawable)
    }

    private fun updateBaseMetricsUI(metrics: List<Metric>) {
        metrics.forEachIndexed { index, metric ->
            if (index < baseMetricViews.size) {
                val view = baseMetricViews[index]
                val valueTextView = view.findViewById<TextView>(R.id.metric_value)
                val nameTextView = view.findViewById<TextView>(R.id.metric_name)
                val detailTextView = view.findViewById<TextView>(R.id.metric_detail)
                val trendContainer = view.findViewById<View>(R.id.trend_container)
                val trendIcon = view.findViewById<View>(R.id.trend_icon)
                val trendValue = view.findViewById<TextView>(R.id.trend_value)

                valueTextView.text = metric.value
                nameTextView.text = metric.name
                detailTextView.text = metric.detail

                if (metric.trend != 0f) {
                    trendContainer.isVisible = true
                    trendValue.text = String.format("%.1f", Math.abs(metric.trend))

                    if (metric.trend > 0) {
                        trendIcon.setBackgroundResource(R.drawable.ic_trend_up)
                        trendValue.setTextColor(ContextCompat.getColor(this, R.color.trend_positive))
                    } else {
                        trendIcon.setBackgroundResource(R.drawable.ic_trend_down)
                        trendValue.setTextColor(ContextCompat.getColor(this, R.color.trend_negative))
                    }
                } else {
                    trendContainer.isVisible = false
                }
            }
        }
    }

    private fun updateAdvancedMetricsUI(metrics: List<Metric>) {
        metrics.forEachIndexed { index, metric ->
            if (index < advancedMetricViews.size) {
                val view = advancedMetricViews[index]
                val valueTextView = view.findViewById<TextView>(R.id.metric_value)
                val nameTextView = view.findViewById<TextView>(R.id.metric_name)
                val detailTextView = view.findViewById<TextView>(R.id.metric_detail)
                val trendContainer = view.findViewById<View>(R.id.trend_container)
                val trendIcon = view.findViewById<View>(R.id.trend_icon)
                val trendValue = view.findViewById<TextView>(R.id.trend_value)

                valueTextView.text = metric.value
                nameTextView.text = metric.name
                detailTextView.text = metric.detail

                if (metric.trend != 0f) {
                    trendContainer.isVisible = true
                    trendValue.text = String.format("%.1f", Math.abs(metric.trend))

                    if (metric.trend > 0) {
                        trendIcon.setBackgroundResource(R.drawable.ic_trend_up)
                        trendValue.setTextColor(ContextCompat.getColor(this, R.color.trend_positive))
                    } else {
                        trendIcon.setBackgroundResource(R.drawable.ic_trend_down)
                        trendValue.setTextColor(ContextCompat.getColor(this, R.color.trend_negative))
                    }
                } else {
                    trendContainer.isVisible = false
                }
            }
        }
    }

    private fun animateContentIn() {
        // Staggered animations for content sections
        val medalAnim = ObjectAnimator.ofFloat(medalContainer, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        val baseMetricsAnim = ObjectAnimator.ofFloat(baseMetricsContainer, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        val advancedMetricsAnim = ObjectAnimator.ofFloat(advancedMetricsContainer, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        val chartsAnim = ObjectAnimator.ofFloat(chartsContainer, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
        }

        // Scale animations for metric cards
        val baseMetricAnims = baseMetricViews.map { view ->
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 300
                interpolator = OvershootInterpolator()
            }
        }

        val advancedMetricAnims = advancedMetricViews.map { view ->
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY)
                duration = 300
                interpolator = OvershootInterpolator()
            }
        }

        // Create sequence
        val animSet = AnimatorSet()

        // Medal animation first
        animSet.play(medalAnim).before(baseMetricsAnim)

        // Base metrics animation
        baseMetricsAnim.doOnEnd {
            val baseMetricsSet = AnimatorSet()
            baseMetricsSet.playSequentially(baseMetricAnims)
            baseMetricsSet.start()
        }

        // Advanced metrics after base metrics
        animSet.play(advancedMetricsAnim).after(baseMetricsAnim)

        advancedMetricsAnim.doOnEnd {
            val advancedMetricsSet = AnimatorSet()
            advancedMetricsSet.playSequentially(advancedMetricAnims)
            advancedMetricsSet.start()
        }

        // Charts animation last
        animSet.play(chartsAnim).after(advancedMetricsAnim)
        animSet.start()
    }
}