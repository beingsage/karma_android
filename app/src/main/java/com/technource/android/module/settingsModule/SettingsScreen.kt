package com.technource.android.module.settingsModule

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.technource.android.R
import com.technource.android.databinding.ActivitySettingsBinding
import com.technource.android.eTMS.micro.DynamicWallpaperTaskIterator
import com.technource.android.utils.TTSManager
import com.technource.android.eTMS.micro.TaskIterator
import com.technource.android.eTMS.micro.TaskWidgetProvider
import com.technource.android.local.AppDatabase
import com.technource.android.local.BinaryMeasurement
import com.technource.android.local.DeepWorkMeasurement
import com.technource.android.local.QuantMeasurement
import com.technource.android.local.SubTask
import com.technource.android.local.Task
import com.technource.android.local.TaskDao
import com.technource.android.local.TaskStatus
import com.technource.android.local.TimeMeasurement
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.system_status.SystemStatusActivity
import com.technource.android.utils.NavigationHelper
import com.technource.android.utils.NeckbandVibrationUtil
import com.technource.android.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import android.content.SharedPreferences
import android.os.Build
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.technource.android.utils.HeaderComponent

@AndroidEntryPoint
class SettingsScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var apiService: ApiService
    @Inject lateinit var gson: Gson
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var ttsManager: TTSManager
    @Inject lateinit var taskIterator: TaskIterator
    @Inject lateinit var sharedPreferences: SharedPreferences

    private lateinit var bottomNavigation: BottomNavigationView
    private var isDynamicDataVisible = false
//    private lateinit var dashboardWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure root is a ScrollView with a LinearLayout child
        val scrollView = binding.root as? android.widget.ScrollView
        val mainContainer = scrollView?.getChildAt(0) as? LinearLayout

        // Add rollback button and dashboardWebView to mainContainer
        val rollbackButton = android.widget.Button(this).apply {
            text = "Rollback to Default Timetable"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 32, 32, 0)
            }
        }
        mainContainer?.addView(rollbackButton)

        val dashboardWebView = WebView(this).apply {
            settings.javaScriptEnabled = true
            webViewClient = WebViewClient()
            loadUrl("file:///android_asset/DashBoard.html")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                800 // Height in px, adjust as needed
            )
        }
        mainContainer?.addView(dashboardWebView)

        // Create a parent LinearLayout to hold all content
        val dynamicDataContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.CALL_PHONE
                ),
                1
            )
        }
        // Get the existing content from the binding root
        val existingContent = (binding.root as ViewGroup).getChildAt(0)
        (binding.root as ViewGroup).removeView(existingContent)

        // Add the existing content to main container
        dynamicDataContainer.addView(existingContent)

        // Set the main container as the only child of the ScrollView
        (binding.root as ViewGroup).addView(dynamicDataContainer)

        // Dynamic wallpaper button
        binding.applyWallpaperButton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.Default) {
                DynamicWallpaperTaskIterator.applyWallpaperIntent(this@SettingsScreen)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SettingsScreen, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setupPreferenceToggles()

        binding.viewSystemStatusButton.setOnClickListener {
            Log.e(TAG, "Navigating to SystemStatusActivity")
            startActivity(Intent(this, SystemStatusActivity::class.java))
        }

        binding.composeView.setContent {
            CompareDefaultTTButton()
        }

        binding.btnViewDynamicData.setOnClickListener {
            isDynamicDataVisible = !isDynamicDataVisible
            binding.btnViewDynamicData.text = if (isDynamicDataVisible) "Hide Dynamic Data" else "View Dynamic Data"
            Log.e(TAG, "Toggling dynamic data visibility: $isDynamicDataVisible")
            NeckbandVibrationUtil.triggerHiddenVibration(this)
            updateDynamicDataComposeView()
        }

        updateDynamicDataComposeView()

        binding.btnDebugWidget.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.btnDebugWidget.isEnabled = false
                    binding.btnDebugWidget.text = "Loading..."

                    // Create sample test data
                    val now = LocalDateTime.now()
                    val testTasks = listOf(
                        Task(
                            id = "task_1",
                            title = "Study Mathematics",
                            category = "study",
                            color = "#96CEB4",  // Soft green
                            startTime = now.toString(),
                            endTime = now.plusHours(1).toString(),
                            duration = 3600,
                            taskScore = 45.0f,
                            taskId = "t1",
                            isExpanded = false,
                            completionStatus = 0.0f,
                            status = TaskStatus.RUNNING,
                            subtasks = listOf(
                                SubTask(
                                    id = "sub_1",
                                    title = "Complete Practice Problems",
                                    measurementType = "binary",
                                    baseScore = 20,
                                    completionStatus = 0f,
                                    finalScore = 0f,
                                    subTaskId = "st1",
                                    binary = BinaryMeasurement(completed = false),
                                    time = null,
                                    quant = null,
                                    deepwork = null
                                ),
                                SubTask(
                                    id = "sub_2",
                                    title = "Review Theory",
                                    measurementType = "time",
                                    baseScore = 25,
                                    completionStatus = 0f,
                                    finalScore = 0f,
                                    subTaskId = "st2",
                                    binary = null,
                                    time = TimeMeasurement(
                                        setDuration = 1800,  // 30 minutes
                                        timeSpent = 0
                                    ),
                                    quant = null,
                                    deepwork = null
                                )
                            )
                        ),
                        Task(
                            id = "task_2",
                            title = "Deep Work Session",
                            category = "work",
                            color = "#FFEEAD",  // Soft yellow
                            startTime = now.plusHours(1).toString(),
                            endTime = now.plusHours(2).toString(),
                            duration = 3600,
                            taskScore = 60.0f,
                            taskId = "t2",
                            isExpanded = false,
                            completionStatus = 0.0f,
                            status = TaskStatus.UPCOMING,
                            subtasks = listOf(
                                SubTask(
                                    id = "sub_3",
                                    title = "Project Development",
                                    measurementType = "deepwork",
                                    baseScore = 40,
                                    completionStatus = 0f,
                                    finalScore = 0f,
                                    subTaskId = "st3",
                                    binary = null,
                                    time = null,
                                    quant = null,
                                    deepwork = DeepWorkMeasurement(
                                        template = "pomodoro",
                                        deepworkScore = 0
                                    )
                                ),
                                SubTask(
                                    id = "sub_4",
                                    title = "Documentation",
                                    measurementType = "quant",
                                    baseScore = 20,
                                    completionStatus = 0f,
                                    finalScore = 0f,
                                    subTaskId = "st4",
                                    binary = null,
                                    time = null,
                                    quant = QuantMeasurement(
                                        targetValue = 5,
                                        targetUnit = "pages",
                                        achievedValue = 0
                                    ),
                                    deepwork = null
                                )
                            )
                        )
                    )

                    // Update widget with test data
                    withContext(Dispatchers.Main) {
                        TaskWidgetProvider.updateHomeScreenWidget(
                            this@SettingsScreen,
                            testTasks,
                            now,
                            now.plusHours(2)
                        )
                        Toast.makeText(this@SettingsScreen, "Widget updated with test data", Toast.LENGTH_SHORT).show()
                        SystemStatus.logEvent("SettingsScreen", "Widget debug data loaded")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsScreen, "Widget debug failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        SystemStatus.logEvent("SettingsScreen", "Widget debug failed: ${e.message}")
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.btnDebugWidget.isEnabled = true
                        binding.btnDebugWidget.text = "Debug Widget"
                    }
                }
            }
        }
        // --- Add Rollback Button Programmatically ---
        rollbackButton.setOnClickListener {
            lifecycleScope.launch {
                // Send a broadcast or bind to the service and call rollback
                val intent = Intent(this@SettingsScreen, com.technource.android.eTMS.macro.EternalTimeTableUnitService::class.java)
                intent.action = "ACTION_ROLLBACK_TO_DEFAULT"
                startService(intent)
                Toast.makeText(this@SettingsScreen, "Rollback to default timetable scheduled for today", Toast.LENGTH_SHORT).show()
            }
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)

        val header = findViewById<HeaderComponent>(R.id.header)
        
        // Set the title
        header.setTitle("Settings")
        
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



    private fun setupPreferenceToggles() {
        lifecycleScope.launch {
            PreferencesManager.isTtsEnabled().collect { binding.switchTts.isChecked = it }
            PreferencesManager.isNotificationsEnabled().collect { binding.switchNotifications.isChecked = it }
            PreferencesManager.isVibrationEnabled().collect { binding.switchVibration.isChecked = it }
            PreferencesManager.isTaskLoggerEnabled().collect { binding.switchTaskLogger.isChecked = it }
            PreferencesManager.isWidgetsEnabled().collect { binding.switchWidgets.isChecked = it }
            PreferencesManager.isVoiceCommandsEnabled().collect { binding.switchVoiceCommands.isChecked = it }
        }

        binding.switchTts.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setTtsEnabled(isChecked)
                Log.e(TAG, "TTS preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "TTS preference set to $isChecked")
            }
        }
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setNotificationsEnabled(isChecked)
                Log.e(TAG, "Notifications preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "Notifications preference set to $isChecked")
            }
        }
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setVibrationEnabled(isChecked)
                Log.e(TAG, "Vibration preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "Vibration preference set to $isChecked")
            }
        }
        binding.switchTaskLogger.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setTaskLoggerEnabled(isChecked)
                Log.e(TAG, "Task Logger preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "Task Logger preference set to $isChecked")
            }
        }
        binding.switchWidgets.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setWidgetsEnabled(isChecked)
                Log.e(TAG, "Widgets preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "Widgets preference set to $isChecked")
            }
        }
        binding.switchVoiceCommands.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                PreferencesManager.setVoiceCommandsEnabled(isChecked)
                Log.e(TAG, "Voice Commands preference set to $isChecked")
                SystemStatus.logEvent("SettingsActivity", "Voice Commands preference set to $isChecked")
            }
        }
    }

    private fun updateDynamicDataComposeView() {
        binding.dynamicDataComposeView.setContent {
            MaterialTheme {
                if (isDynamicDataVisible) {
                    DynamicDataDisplay(apiService)  // Pass the injected apiService
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
        Log.e(TAG, "SettingsScreen resumed")
    }

     override fun onDestroy() {
        super.onDestroy()
    }
}