package com.technource.android.module.homeModule

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.technource.android.R
import com.technource.android.local.Task
import com.technource.android.local.TaskDao
import com.technource.android.local.TaskStatus
import com.technource.android.utils.DateFormatter
import com.technource.android.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HomeScreen : AppCompatActivity() {
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var taskAdapter: TaskAdapter
    @Inject lateinit var taskDao: TaskDao

    // UI components
    private lateinit var recyclerViewTasks: RecyclerView
    private lateinit var textViewDate: TextView
    private lateinit var textViewGreeting: TextView
    private lateinit var progressBar: View
    private lateinit var timelineView: TimelineView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var swipeRefreshLayout: YoutubeSwipeRefreshLayout
    private lateinit var emptyStateView: View
    private lateinit var fabCurrentTask: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    private lateinit var progressDaily: LinearProgressIndicator
    private lateinit var textViewProgressPercentage: TextView
    private lateinit var textViewCompletedCount: TextView
    private lateinit var textViewInProgressCount: TextView
    private lateinit var textViewUpcomingCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupTabLayout()
        setupSwipeRefresh()
        setupFabCurrentTask()

        // Perform heavy setup in a background thread
        lifecycleScope.launch(Dispatchers.Default) {
            setupViews()
            withContext(Dispatchers.Main) {
                observeViewModel()
                showSkeletonLoading(true)
                // Simulate initial loading delay for smoother UX
                delay(800)
                viewModel.fetchTasks()
            }
        }
    }


//    override fun onEnabled(context: Context) {
//        super.onEnabled(context)
//        Log.d("TaskWidgetProvider", "Widget enabled")
//        appContext = context.applicationContext
//    }
//
//    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
//        Log.d("TaskWidgetProvider", "Widget updating: ${appWidgetIds.joinToString()}")
//        appContext = context.applicationContext
//        for (appWidgetId in appWidgetIds) {
//            updateWidget(context, appWidgetManager, appWidgetId, currentTasks, 0, listOf(0))
//        }
//    }
//

    private fun initializeViews() {
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        textViewDate = findViewById(R.id.textViewDate)
        textViewGreeting = findViewById(R.id.textViewGreeting)
        progressBar = findViewById(R.id.progressBar)
        timelineView = findViewById(R.id.timelineView)
        shimmerFrameLayout = findViewById(R.id.shimmerFrameLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyStateView = findViewById(R.id.emptyStateView)
        fabCurrentTask = findViewById(R.id.fabCurrentTask)
        tabLayout = findViewById(R.id.tabLayout)
        progressDaily = findViewById(R.id.progressDaily)
        textViewProgressPercentage = findViewById(R.id.textViewProgressPercentage)
        textViewCompletedCount = findViewById(R.id.textViewCompletedCount)
        textViewInProgressCount = findViewById(R.id.textViewInProgressCount)
        textViewUpcomingCount = findViewById(R.id.textViewUpcomingCount)

        // Set up bottom navigation
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }

    private fun setupTabLayout() {
        // Add tabs for different categories
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Work"))
        tabLayout.addTab(tabLayout.newTab().setText("Personal"))
        tabLayout.addTab(tabLayout.newTab().setText("Health"))
        tabLayout.addTab(tabLayout.newTab().setText("Learning"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    0 -> null // All tasks
                    1 -> "work"
                    2 -> "personal"
                    3 -> "health"
                    4 -> "learning"
                    else -> null
                }
                filterTasksByCategory(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            // Animate the recycler view items out
            val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
            animation.duration = 200
            recyclerViewTasks.startAnimation(animation)

            // Fetch fresh data
            lifecycleScope.launch {
                delay(300) // Short delay for better UX
                viewModel.fetchTasks()
            }
        }
    }

    private fun setupFabCurrentTask() {
        fabCurrentTask.setOnClickListener {
            scrollToCurrentTask()
        }
    }

    private fun setupViews() {
        taskAdapter = TaskAdapter(viewModel).apply {
            setOnTaskExpandedListener { task, isExpanded ->
                // Animate expansion/collapse
                if (isExpanded) {
                    val position = taskAdapter.currentList.indexOf(task)
                    if (position != -1) {
                        recyclerViewTasks.smoothScrollToPosition(position)
                    }
                }
            }
        }

        recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(this@HomeScreen)
            setHasFixedSize(true) // Optimize RecyclerView performance
            adapter = taskAdapter

            // Add animation to items
            val animation = AnimationUtils.loadAnimation(this@HomeScreen, R.anim.item_animation_from_right)
            layoutAnimation = android.view.animation.LayoutAnimationController(animation)
        }

        // Connect timeline view with recycler view
        timelineView.setRecyclerView(recyclerViewTasks)
    }

    private fun observeViewModel() {
        // Observe date
        viewModel.todayFormatted.observe(this) { date ->
            textViewDate.text = date

            // Set greeting based on time of day
            val currentTime = DateFormatter.millisToLocalDateTime(System.currentTimeMillis())
            val hourOfDay = currentTime.hour

            textViewGreeting.text = when {
                hourOfDay < 12 -> getString(R.string.good_morning)
                hourOfDay < 18 -> getString(R.string.good_afternoon)
                else -> getString(R.string.good_evening)
            }
        }

        // Observe tasks
        viewModel.tasks.observe(this) { tasks ->
            showSkeletonLoading(false)
            swipeRefreshLayout.isRefreshing = false

            // Animate the recycler view items in
            recyclerViewTasks.scheduleLayoutAnimation()

            updateTaskList(tasks)
            updateTaskStats(tasks)
            timelineView.setTasks(tasks)

            // Show empty state if needed
            emptyStateView.visibility = if (tasks.isEmpty()) View.VISIBLE else View.GONE

            // Scroll to current task after a short delay
            if (tasks.isNotEmpty()) {
                lifecycleScope.launch {
                    delay(500) // Wait for layout to complete
                    scrollToCurrentTask()
                }
            }
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateTaskList(tasks: List<Task>) {
        taskAdapter.submitList(tasks)
    }

    private fun updateTaskStats(tasks: List<Task>) {
        // Calculate task statistics
        val completedTasks = tasks.count { it.status == TaskStatus.LOGGED }
        val inProgressTasks = tasks.count { it.status == TaskStatus.RUNNING }
        val upcomingTasks = tasks.count { it.status == TaskStatus.UPCOMING }
        val totalTasks = tasks.size

        // Update progress indicators
        if (totalTasks > 0) {
            val progressPercentage = (completedTasks.toFloat() / totalTasks) * 100
            progressDaily.progress = progressPercentage.toInt()
            textViewProgressPercentage.text = "${progressPercentage.toInt()}%"
        } else {
            progressDaily.progress = 0
            textViewProgressPercentage.text = "0%"
        }

        // Update task counts
        textViewCompletedCount.text = completedTasks.toString()
        textViewInProgressCount.text = inProgressTasks.toString()
        textViewUpcomingCount.text = upcomingTasks.toString()
    }

    private fun filterTasksByCategory(category: String?) {
        viewModel.tasks.value?.let { allTasks ->
            val filteredTasks = if (category != null) {
                allTasks.filter { it.category.equals(category, ignoreCase = true) }
            } else {
                allTasks
            }
            updateTaskList(filteredTasks)
            updateTaskStats(filteredTasks)
        }
    }

    private fun scrollToCurrentTask() {
        val currentTask = viewModel.tasks.value?.firstOrNull { it.status == TaskStatus.RUNNING }

        if (currentTask != null) {
            val position = taskAdapter.currentList.indexOf(currentTask)
            if (position != -1) {
                // Animate scroll to current task
                recyclerViewTasks.smoothScrollToPosition(position)

                // Highlight the task with a pulse animation
                lifecycleScope.launch {
                    delay(300) // Wait for scroll to complete
                    val viewHolder = recyclerViewTasks.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { view ->
                        val pulseAnimation = AnimationUtils.loadAnimation(this@HomeScreen, R.anim.pulse_animation)
                        view.startAnimation(pulseAnimation)
                    }
                }
            }
        } else {
            // Find the next upcoming task if no current task
            val upcomingTask = viewModel.tasks.value?.firstOrNull { it.status == TaskStatus.UPCOMING }
            if (upcomingTask != null) {
                val position = taskAdapter.currentList.indexOf(upcomingTask)
                if (position != -1) {
                    recyclerViewTasks.smoothScrollToPosition(position)
                }
            }
        }
    }

    private fun showSkeletonLoading(show: Boolean) {
        if (show) {
            shimmerFrameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.startShimmer()
            recyclerViewTasks.visibility = View.INVISIBLE
        } else {
            shimmerFrameLayout.stopShimmer()
            shimmerFrameLayout.visibility = View.GONE
            recyclerViewTasks.visibility = View.VISIBLE
        }
    }


}
