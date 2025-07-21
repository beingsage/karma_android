package com.technource.android.module.statsModule

import android.animation.Animator
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
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.technource.android.R
import com.technource.android.utils.HeaderComponent
import com.technource.android.module.statsModule.adapters.StatsChartPagerAdapter
import com.technource.android.module.statsModule.adapters.MetricsAdapter
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class StatsScreen : AppCompatActivity() {
    private val statsViewModel: StatsViewModel by viewModels()

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

    // Add these properties at the class level
    private val baseMetricViews = mutableListOf<View>()
    private val advancedMetricViews = mutableListOf<View>()

    // Add these properties
    private lateinit var baseMetricsAdapter: MetricsAdapter
    private lateinit var advancedMetricsAdapter: MetricsAdapter

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

         val header = findViewById<HeaderComponent>(R.id.header)
        
        // Set the title
        header.setTitle("Stats")
        
        // Set system status
        header.setSystemStatus(HeaderComponent.SystemStatus.NORMAL)
        
        // Handle notification clicks
        header.setOnNotificationClickListener {
            // Show your notification panel/drawer
            showNotifications()
        }

    }

     private fun showNotifications() {
        // Implement your notification display logic here
    }

    private fun initViews() {
        // Initialize views
        medalContainer = findViewById(R.id.medal_container)
        baseMetricsContainer = findViewById<RecyclerView>(R.id.base_metrics_container).apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@StatsScreen, 2)
        }
        advancedMetricsContainer = findViewById<RecyclerView>(R.id.advanced_metrics_container).apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@StatsScreen, 3)
        }
        chartsContainer = findViewById(R.id.charts_container)
        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        loadingView = findViewById(R.id.loading_view)
        contentView = findViewById(R.id.content_view)
        dateTextView = findViewById(R.id.date_text)
        noDataTextView = findViewById(R.id.no_data_text)

        // Initialize adapters
        baseMetricsAdapter = MetricsAdapter()
        advancedMetricsAdapter = MetricsAdapter()

        // Set adapters
        (baseMetricsContainer as RecyclerView).adapter = baseMetricsAdapter
        (advancedMetricsContainer as RecyclerView).adapter = advancedMetricsAdapter

        setupMetricViewObservers()
        setupInitialVisibility()
        setupMedalAnimation()
    }

    private fun setupMetricViewObservers() {
        baseMetricsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                baseMetricViews.clear()
                for (i in 0 until baseMetricsAdapter.itemCount) {
                    (baseMetricsContainer as RecyclerView).findViewHolderForAdapterPosition(i)?.itemView?.let {
                        baseMetricViews.add(it)
                    }
                }
            }
        })

        advancedMetricsAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                advancedMetricViews.clear()
                for (i in 0 until advancedMetricsAdapter.itemCount) {
                    (advancedMetricsContainer as RecyclerView).findViewHolderForAdapterPosition(i)?.itemView?.let {
                        advancedMetricViews.add(it)
                    }
                }
            }
        })
    }

    private fun setupInitialVisibility() {
        medalContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
        }
        baseMetricsContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
        }
        advancedMetricsContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
        }
        chartsContainer.apply {
            visibility = View.VISIBLE
            alpha = 0f
        }
        contentView.visibility = View.VISIBLE
        noDataTextView.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
    }

    private fun setupMedalAnimation() {
        findViewById<View>(R.id.medal_icon)?.let { medalIcon ->
            ObjectAnimator.ofFloat(medalIcon, "alpha", 0.6f, 1.0f).apply {
                duration = 1000
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                start()
            }
        }
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

        // Force reload stats if empty
        if (statsViewModel.stats.value == null || statsViewModel.stats.value?.tasks?.isEmpty() == true) {
            SystemStatus.logEvent("StatsScreen", "Forcing stats reload in onResume")
        }

        // Ensure visibility
        contentView.visibility = View.VISIBLE
        medalContainer.visibility = View.VISIBLE
        baseMetricsContainer.visibility = View.VISIBLE
        advancedMetricsContainer.visibility = View.VISIBLE

        statsViewModel.loadStats()
    }

    private fun setupObservers() {
        statsViewModel.stats.observe(this) { stats ->
            if (stats != null) {
                // Remove the tasks.isNotEmpty() check to allow UI updates even with empty task list
                updateUI(stats)
                animateContentIn()
                loadingView.isVisible = false
                contentView.isVisible = true
                noDataTextView.isVisible = false

                // Make sure panels are visible
                medalContainer.visibility = View.VISIBLE
                baseMetricsContainer.visibility = View.VISIBLE
                advancedMetricsContainer.visibility = View.VISIBLE
            } else {
                loadingView.isVisible = false
                contentView.isVisible = false
                noDataTextView.isVisible = true
            }
        }

        statsViewModel.isLoading.observe(this) { isLoading ->
            loadingView.isVisible = isLoading
            contentView.isVisible = !isLoading && statsViewModel.stats.value?.tasks?.isNotEmpty() == true
            noDataTextView.isVisible = !isLoading && statsViewModel.stats.value?.tasks?.isEmpty() == true
        }

        statsViewModel.topAchievement.observe(this) { achievement ->
            if (achievement != null) {
                updateMedalUI(achievement)
                medalContainer.isVisible = true
            } else {
                medalContainer.isVisible = false
            }
        }

        statsViewModel.baseMetrics.observe(this) { metrics ->
            updateBaseMetricsUI(metrics)
        }

        statsViewModel.advancedMetrics.observe(this) { metrics ->
            updateAdvancedMetricsUI(metrics)
        }
    }

    private fun updateUI(stats: StatsData) {
        SystemStatus.logEvent("StatsActivity", """
        UI Update:
        Medal Container Visible: ${medalContainer.isVisible}
        Base Metrics Visible: ${baseMetricsContainer.isVisible}
        Advanced Metrics Visible: ${advancedMetricsContainer.isVisible}
        Charts Container Visible: ${chartsContainer.isVisible}
        Content View Visible: ${contentView.isVisible}
    """.trimIndent())
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
        baseMetricsAdapter.updateMetrics(metrics)
    }

    private fun updateAdvancedMetricsUI(metrics: List<Metric>) {
        advancedMetricsAdapter.updateMetrics(metrics)
    }

    private fun animateContentIn() {
        val animSet = AnimatorSet()
        val animations = mutableListOf<Animator>()

        // Create fade-in animations
        val fadeInAnimations = listOf(
            medalContainer,
            baseMetricsContainer,
            advancedMetricsContainer,
            chartsContainer
        ).map { view ->
            ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
                duration = 500
                interpolator = AccelerateDecelerateInterpolator()
            }
        }

        // Add fade-ins to sequence
        animations.addAll(fadeInAnimations)

        // Create metric card scale animations
        val metricAnimations = (baseMetricViews + advancedMetricViews).map { view ->
            AnimatorSet().apply {
                val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1f)
                val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1f)
                playTogether(scaleX, scaleY)
                duration = 300
                interpolator = OvershootInterpolator()
            }
        }

        // Play the sequence
        animSet.playSequentially(animations)

        // Add scale animations after fade-ins complete
        fadeInAnimations.last().addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                AnimatorSet().apply {
                    playSequentially(metricAnimations)
                    start()
                }
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animSet.start()
    }
}