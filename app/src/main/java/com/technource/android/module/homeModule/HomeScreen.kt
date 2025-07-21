package com.technource.android.module.homeModule

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.tabs.TabLayout
import com.technource.android.R
import com.technource.android.utils.HeaderComponent
import com.technource.android.local.Task
import com.technource.android.local.TaskDao
import com.technource.android.local.TaskStatus
import com.technource.android.system_status.SystemStatus
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
    private lateinit var textViewCurrentTask: TextView

    // Add a handler for delayed scrolling
    private val scrollHandler = Handler(Looper.getMainLooper())
    private var isUserInteracting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        initializeViews()
        setupTabLayout()
        setupSwipeRefresh()
        setupFabCurrentTask()

        // Add touch listener to detect user interaction
        recyclerViewTasks.setOnTouchListener { _, _ ->
            isUserInteracting = true
            scrollHandler.removeCallbacks(autoScrollRunnable) // Cancel any pending auto-scroll
            false
        }

        // Add scroll listener to detect when user stops interacting
        recyclerViewTasks.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    isUserInteracting = false
                    scrollHandler.postDelayed(autoScrollRunnable, 3000) // Start auto-scroll after 3 seconds
                }
            }
        })

        // Perform heavy setup in a background thread
        lifecycleScope.launch(Dispatchers.Default) {
            setupViews()
            withContext(Dispatchers.Main) {
                observeViewModel()
                showSkeletonLoading(true)
                delay(800)
                viewModel.fetchTasks()
            }
        }
       val header = findViewById<HeaderComponent>(R.id.header)
        
        // Set the title
        header.setTitle("Home")
        
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

    // Runnable to perform the auto-scroll
    private val autoScrollRunnable = Runnable {
        if (!isUserInteracting) {
            scrollToCurrentTask()
        }
    }

    private fun initializeViews() {
        recyclerViewTasks = findViewById(R.id.recyclerViewTasks)
        textViewDate = findViewById(R.id.textViewDate)
        textViewGreeting = findViewById(R.id.textViewGreeting)
        progressBar = findViewById(R.id.progressBar)
        shimmerFrameLayout = findViewById(R.id.shimmerFrameLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyStateView = findViewById(R.id.emptyStateView)
        fabCurrentTask = findViewById(R.id.fabCurrentTask)
        tabLayout = findViewById(R.id.tabLayout)
        progressDaily = findViewById(R.id.progressDaily)
        textViewProgressPercentage = findViewById(R.id.textViewProgressPercentage)
        textViewCompletedCount = findViewById(R.id.textViewTotalCount)
        textViewInProgressCount = findViewById(R.id.textViewInProgressCount)
        textViewUpcomingCount = findViewById(R.id.textViewUpcomingCount)
        textViewCurrentTask = findViewById(R.id.textViewCurrentTask)

        // Set up bottom navigation
        val bottomNavigation = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }

    private fun setupTabLayout() {
        tabLayout.removeAllTabs()

        // Add tabs with custom styling
        val tabs = listOf(
            "ALL" to null,
            "ROUTINE" to R.color.category_routine_text,
            "WORK" to R.color.category_work_text,
            "STUDY" to R.color.category_study_text
        )

        tabs.forEach { (text, colorRes) ->
            tabLayout.addTab(tabLayout.newTab().apply {
                setText(text)
                if (colorRes != null) {
                    setCustomView(createCustomTabView(text, colorRes))
                }
            })
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = when (tab.position) {
                    0 -> null // All tasks
                    1 -> "routine"
                    2 -> "work"
                    3 -> "study"
                    else -> null
                }
                filterTasksByCategory(category)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun createCustomTabView(text: String, colorRes: Int): View {
        return layoutInflater.inflate(R.layout.custom_tab, null).apply {
            findViewById<TextView>(R.id.tabText).apply {
                setText(text)
                setTextColor(ContextCompat.getColor(this@HomeScreen, colorRes))
            }
        }
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
            setHasFixedSize(false) // Changed to false to allow dynamic sizing
            adapter = taskAdapter
        }
    }

    private fun observeViewModel() {
        // Observe date
        viewModel.todayFormatted.observe(this) { date ->
            SystemStatus.logEvent("HomeScreen", "Date updated: $date")
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
            SystemStatus.logEvent("HomeScreen", "Tasks updated: ${tasks.size} tasks received")
            showSkeletonLoading(false)
            swipeRefreshLayout.isRefreshing = false

            try {
                updateTaskList(tasks)
                updateTaskStats(tasks)

                // Show empty state if needed
                emptyStateView.visibility = if (tasks.isEmpty()) {
                    SystemStatus.logEvent("HomeScreen", "Showing empty state - no tasks")
                    View.VISIBLE
                } else View.GONE

                // Update current task display
                val currentTask = tasks.firstOrNull { it.status == TaskStatus.RUNNING }
                SystemStatus.logEvent("HomeScreen", "Current task: ${currentTask?.title ?: "None"}")
                if (currentTask != null) {
                    textViewCurrentTask.text = "Current: ${currentTask.title}"
                    textViewCurrentTask.visibility = View.VISIBLE
                } else {
                    textViewCurrentTask.text = "No task is currently running"
                    textViewCurrentTask.visibility = View.VISIBLE // Show placeholder
                }

                // Scroll to current task after a short delay
                if (tasks.isNotEmpty()) {
                    lifecycleScope.launch {
                        delay(500)
                        scrollToCurrentTask()
                    }
                }
            } catch (e: Exception) {
                SystemStatus.logEvent("HomeScreen", "Error updating UI: ${e.message}")
            }
        }

        // Add logging to loading state
        viewModel.isLoading.observe(this) { isLoading ->
            SystemStatus.logEvent("HomeScreen", "Loading state changed: $isLoading")
            progressBar.visibility = if (isLoading && !swipeRefreshLayout.isRefreshing) View.VISIBLE else View.GONE
        }

        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }

        viewModel.missedCount.observe(this) { count ->
            findViewById<TextView>(R.id.textViewMissedCount).text = count.toString()
        }

        viewModel.systemFailureCount.observe(this) { count ->
            findViewById<TextView>(R.id.textViewSystemFailure).text = count.toString()
        }

        viewModel.netScore.observe(this) { score ->
            findViewById<TextView>(R.id.textViewNetScore).text = score.toString()
        }
    }

    private fun updateTaskList(tasks: List<Task>) {
        taskAdapter.submitList(tasks){
            // Add fade-in animation after the list is updated
            val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            animation.duration = 300
            recyclerViewTasks.startAnimation(animation)
        }
    }

    private fun updateTaskStats(tasks: List<Task>) {
        try {
            // Calculate task statistics with strict status checking
            val totalTasks = tasks.size  // Total tasks regardless of status
            
            // Use strict equality checks for status
            val completedTasks = tasks.count { it.status == TaskStatus.LOGGED }
            val runningTasks = tasks.count { it.status == TaskStatus.RUNNING }
            val upcomingTasks = tasks.count { it.status == TaskStatus.UPCOMING }
            val missedTasks = tasks.count { it.status == TaskStatus.MISSED }
            val systemFailures = tasks.count { it.status == TaskStatus.SYSTEM_FAILURE }

            // Debug logging to check task statuses
            tasks.forEach { task ->
                SystemStatus.logEvent("HomeScreen", 
                    "Task ${task.id}: Title=${task.title}, Status=${task.status}")
            }
            
            // Calculate net score (achieved score) - sum of all task scores
            val achievedScore = tasks.sumOf { it.taskScore.toInt() }
            
            // Calculate total possible score - sum of all subtask base scores
            val netPossibleScore = tasks.sumOf { task -> 
                task.subtasks?.sumOf { it.baseScore } ?: 0 
            }

            // Update UI with new counts
            textViewCompletedCount.text = completedTasks.toString()
            textViewInProgressCount.text = completedTasks.toString()
            textViewUpcomingCount.text = upcomingTasks.toString()
            findViewById<TextView>(R.id.textViewTotalCount).text = totalTasks.toString()
            findViewById<TextView>(R.id.textViewMissedCount).text = missedTasks.toString()
            findViewById<TextView>(R.id.textViewSystemFailure).text = systemFailures.toString()
            
            // Update net score display with achieved/possible format
            findViewById<TextView>(R.id.textViewNetScore).text = "$achievedScore/$netPossibleScore"

            // Update progress percentage based on completed tasks only
            if (totalTasks > 0) {
                val progressPercentage = (completedTasks.toFloat() / totalTasks) * 100
                progressDaily.progress = progressPercentage.toInt()
                textViewProgressPercentage.text = "${progressPercentage.toInt()}%"
            } else {
                progressDaily.progress = 0
                textViewProgressPercentage.text = "0%"
            }

        } catch (e: Exception) {
            SystemStatus.logEvent("HomeScreen", "Error updating stats: ${e.message}")
        }
    }

    private fun filterTasksByCategory(category: String?) {
        SystemStatus.logEvent("HomeScreen", "Filtering by category: $category")
        viewModel.tasks.value?.let { allTasks ->
            val filteredTasks = if (category != null) {
                allTasks.filter { it.category.equals(category, ignoreCase = true) }
            } else {
                allTasks
            }
            SystemStatus.logEvent("HomeScreen", "Filtered tasks count: ${filteredTasks.size}")
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

    override fun onDestroy() {
        super.onDestroy()
        scrollHandler.removeCallbacks(autoScrollRunnable) // Clean up handler callbacks
    }
}
