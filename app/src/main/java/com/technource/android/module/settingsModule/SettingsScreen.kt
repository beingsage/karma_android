package com.technource.android.module.settingsModule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.technource.android.ETMS.micro.DynamicWallpaperTaskIterator
import com.technource.android.ETMS.micro.MockDataGenerator
import com.technource.android.R
import com.technource.android.databinding.ActivitySettingsBinding
import com.technource.android.local.AppDatabase
import com.technource.android.local.TaskDao
import com.technource.android.local.toTaskEntity
import com.technource.android.local.Task
import com.technource.android.local.BinaryMeasurement
import com.technource.android.local.DeepWorkMeasurement
import com.technource.android.local.QuantMeasurement
import com.technource.android.local.SubTask
import com.technource.android.local.TimeMeasurement
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.system_status.SystemStatusActivity
import com.technource.android.ETMS.micro.TTSManager
import com.technource.android.ETMS.micro.TaskIterator
import com.technource.android.utils.DateFormatter
import com.technource.android.utils.NavigationHelper
import com.technource.android.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

@AndroidEntryPoint
class SettingsScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var taskIterator: TaskIterator
    private var testTasksJob: Job? = null
    private val testScope = CoroutineScope(Dispatchers.IO) // Persistent scope for testing

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var apiService: ApiService
    @Inject lateinit var gson: Gson
    @Inject lateinit var taskDao: TaskDao
    @Inject lateinit var ttsManager: TTSManager
    @Inject lateinit var taskPopulatorTest: TaskPopulatorTest

    private lateinit var bottomNavigation: BottomNavigationView
    private var isDynamicDataVisible = false

    companion object {
        const val TASK_DURATION_MINUTES = 2L
        private const val TAG = "SettingsScreen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        taskIterator = TaskIterator(this, taskDao, ttsManager)

        // Dynamic wallpaper button
        binding.applyWallpaperButton.setOnClickListener {
            DynamicWallpaperTaskIterator.applyWallpaperIntent(this)
            Log.e(TAG, "Applying dynamic wallpaper")
            Toast.makeText(this, "Applying dynamic wallpaper", Toast.LENGTH_SHORT).show()
        }

        // Combined populate and update tasks toggle button
        binding.populateTasksButton.setOnClickListener {
            if (testTasksJob == null || testTasksJob?.isCancelled == true) {
                Log.e(TAG, "Starting test tasks: Populating and updating")
                binding.populateTasksButton.text = "Stop Test Tasks"
                testTasksJob = testScope.launch {
                    try {
                        // Populate test tasks
                        Log.e(TAG, "Populating test tasks")
                        taskPopulatorTest.populateTasksForTesting()
                        SystemStatus.logEvent("SettingsActivity", "Test tasks populated successfully")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsScreen, "Test tasks populated", Toast.LENGTH_SHORT).show()
                        }

                        // Start status updates
                        Log.e(TAG, "Starting test task status updates")
                        taskPopulatorTest.updateTasksForTesting()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsScreen, "Test task updates started", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Test tasks failed: ${e.message}")
                        SystemStatus.logEvent("SettingsActivity", "Test tasks failed: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsScreen, "Test tasks failed", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Log.e(TAG, "Stopping test tasks: Cancelling updates and clearing tasks")
                binding.populateTasksButton.text = "Start Test Tasks"
                testTasksJob?.cancel()
                testTasksJob = null
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        taskDao.clearTasks()
                        Log.e(TAG, "Cleared all test tasks from database")
                        SystemStatus.logEvent("SettingsActivity", "Test tasks cleared from database")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsScreen, "Test tasks stopped and cleared", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to clear test tasks: ${e.message}")
                        SystemStatus.logEvent("SettingsActivity", "Failed to clear test tasks: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SettingsScreen, "Failed to clear test tasks", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        // Remove updateTasksButton since it's merged with populateTasksButton
        // binding.updateTasksButton.visibility = View.GONE // If you want to hide it in the layout

        // Test timetable button
        binding.testTimetableButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    Log.e(TAG, "Starting timetable test with 10 tasks")
                    val timetable = generateFullDayTimetable(taskCount = 10)
                    insertTimetable(timetable)
                    startService(Intent(this@SettingsScreen, TaskIteratorService::class.java))

                    var processedTasks = 0
                    while (processedTasks < timetable.size) {
                        val statuses = taskIterator.getStatusTimestamps()
                        processedTasks = statuses.count { it.status == "LOGGED" || it.status == "FAILED" }
                        kotlinx.coroutines.delay(60000)
                        Log.e(TAG, "Processed $processedTasks out of ${timetable.size} tasks")
                        SystemStatus.logEvent("SettingsActivity", "Processed $processedTasks out of ${timetable.size} tasks")
                    }

                    taskIterator.stop()
                    deleteTimetable()
                    Log.e(TAG, "Timetable test completed with $processedTasks tasks processed")
                    SystemStatus.logEvent("SettingsActivity", "Timetable test completed with $processedTasks tasks processed")
                    Toast.makeText(this@SettingsScreen, "Timetable test completed", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Timetable test failed: ${e.message}")
                    SystemStatus.logEvent("SettingsActivity", "Timetable test failed: ${e.message}")
                    taskIterator.stop()
                    deleteTimetable()
                    Toast.makeText(this@SettingsScreen, "Timetable test failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        setupPreferenceToggles()

        binding.startTaskIteratorButton.setOnClickListener {
            lifecycleScope.launch {
                Log.e(TAG, "Starting task iterator")
                taskIterator.start()
                binding.startTaskIteratorButton.isEnabled = false
                binding.stopTaskIteratorButton.isEnabled = true
                Toast.makeText(this@SettingsScreen, "Task iterator started", Toast.LENGTH_SHORT).show()
            }
        }

        binding.stopTaskIteratorButton.setOnClickListener {
            Log.e(TAG, "Stopping task iterator")
            taskIterator.stop()
            binding.startTaskIteratorButton.isEnabled = true
            binding.stopTaskIteratorButton.isEnabled = false
            Toast.makeText(this@SettingsScreen, "Task iterator stopped", Toast.LENGTH_SHORT).show()
        }
        binding.stopTaskIteratorButton.isEnabled = false

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
            updateDynamicDataComposeView()
        }

        updateDynamicDataComposeView()

        binding.btnDebugTts.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val hinglishMessage =
                        "Meeting at 9:00 AM!! Bring 100kg of materials (1st priority). Email: test@example.com"
                    val processedText = ttsManager.preprocessText(hinglishMessage)
                    withContext(Dispatchers.Main) {
                        ttsManager.speakHinglish(processedText)
                        Log.e(TAG, "Hinglish TTS triggered: $processedText")
                        SystemStatus.logEvent("SettingsActivity", "Hinglish TTS triggered")
                        Toast.makeText(this@SettingsScreen, "TTS triggered", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "TTS error: ${e.message}")
                        SystemStatus.logEvent("SettingsActivity", "TTS error: ${e.message}")
                        Toast.makeText(this@SettingsScreen, "TTS error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnDebugWallpaper.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Use the mock data generator instead
                    MockDataGenerator.updateWidgetWithMockData(this@SettingsScreen)

                    Log.e(TAG, "Debug wallpaper update triggered with mock tasks")
                    SystemStatus.logEvent(
                        "SettingsActivity",
                        "Debug wallpaper update triggered with mock tasks"
                    )
                    Toast.makeText(this@SettingsScreen, "Wallpaper updated", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Debug wallpaper update failed: ${e.message}")
                    SystemStatus.logEvent("SettingsActivity", "Debug wallpaper update failed: ${e.message}")
                    Toast.makeText(this@SettingsScreen, "Wallpaper update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnDebugTaskLogger.setOnClickListener {
            val mockTask = Task(
                id = "task1",
                title = "Morning Routine",
                category = "routine",
                color = "#FF6B6B",
                startTime = "2025-04-20T06:00:00.000Z",
                endTime = "2025-04-20T06:45:00.000Z",
                duration = 45,
                subtasks = listOf(
                    SubTask(
                        id = "subtask1",
                        title = "Brush Teeth",
                        measurementType = "binary",
                        baseScore = 2,
                        completionStatus = 1.0f,
                        finalScore = 2.0f,
                        binary = BinaryMeasurement(completed = true),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_1"
                    ),
                    SubTask(
                        id = "subtask2",
                        title = "Shower",
                        measurementType = "time",
                        baseScore = 3,
                        completionStatus = 1.0f,
                        finalScore = 3.0f,
                        binary = null,
                        time = TimeMeasurement(setDuration = 15, timeSpent = 0),
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_2"
                    ),
                    SubTask(
                        id = "subtask3",
                        title = "Run 2km",
                        measurementType = "quant",
                        baseScore = 4,
                        completionStatus = 0.5f,
                        finalScore = 2.0f,
                        binary = null,
                        time = null,
                        quant = QuantMeasurement(
                            targetValue = 2,
                            targetUnit = "km",
                            achievedValue = 1
                        ),
                        deepwork = null,
                        subTaskId = "subtask_id_3"
                    ),
                    SubTask(
                        id = "subtask4",
                        title = "Plan Morning",
                        measurementType = "deepwork",
                        baseScore = 5,
                        completionStatus = 0.8f,
                        finalScore = 4.0f,
                        binary = null,
                        time = null,
                        quant = null,
                        deepwork = DeepWorkMeasurement(
                            template = "planning",
                            deepworkScore = 80
                        ),
                        subTaskId = "subtask_id_4"
                    )
                ),
                taskScore = 11.0f,
                taskId = "task_id_mock",
                completionStatus = 0.825f
            )
            Log.e(TAG, "Logging mock task: ${mockTask.title}")
            taskIterator.triggerTaskLogger(mockTask)
            SystemStatus.logEvent("SettingsActivity", "Task logged: ${mockTask.title}, Category: ${mockTask.category}, Subtasks: ${mockTask.subtasks?.joinToString { it.title }}")
            Toast.makeText(this, "Task logged", Toast.LENGTH_SHORT).show()
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }

    private fun generateFullDayTimetable(taskCount: Int = 10): List<Task> {
        val tasks = mutableListOf<Task>()
        val now = LocalDateTime.now(ZoneId.systemDefault())
        val startTime = now

        for (i in 0 until taskCount) {
            val taskStartTime = startTime.plusMinutes(i * TASK_DURATION_MINUTES)
            val taskEndTime = taskStartTime.plusMinutes(TASK_DURATION_MINUTES)
            val task = Task(
                id = "task_$i",
                title = "Task ${i + 1}",
                category = "routine",
                color = "#FF6B6B",
                startTime = DateFormatter.formatIsoDateTime(taskStartTime),
                endTime = DateFormatter.formatIsoDateTime(taskEndTime),
                duration = TASK_DURATION_MINUTES.toInt(),
                subtasks = listOf(
                    SubTask(
                        id = "subtask_${i}_1",
                        title = "Subtask ${i + 1}",
                        measurementType = "binary",
                        baseScore = 2,
                        completionStatus = 0.0f,
                        finalScore = 0.0f,
                        binary = BinaryMeasurement(completed = null),
                        time = null,
                        quant = null,
                        deepwork = null,
                        subTaskId = "subtask_id_${i}_1"
                    )
                ),
                taskScore = 0f,
                taskId = "task_id_$i",
                completionStatus = 0f
            )
            tasks.add(task)
        }
        Log.e(TAG, "Generated timetable with ${tasks.size} tasks")
        return tasks
    }

    private suspend fun insertTimetable(tasks: List<Task>) {
        withContext(Dispatchers.IO) {
            taskDao.insertTasks(tasks.map { it.toTaskEntity() })
            Log.e(TAG, "Inserted ${tasks.size} tasks into TaskDao")
            SystemStatus.logEvent("SettingsActivity", "Inserted ${tasks.size} tasks into TaskDao")
        }
    }

    private suspend fun deleteTimetable() {
        withContext(Dispatchers.IO) {
            taskDao.clearTasks()
            Log.e(TAG, "Deleted all tasks from TaskDao")
            SystemStatus.logEvent("SettingsActivity", "Deleted all tasks from TaskDao")
        }
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
                    DynamicDataDisplay()
                }
            }
        }
    }

    @Composable
    private fun CompareDefaultTTButton() {
        Text(text = "Placeholder CompareDefaultTTButton")
    }

    @Composable
    private fun DynamicDataDisplay() {
        Text(text = "Placeholder DynamicDataDisplay")
    }

    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
        Log.e(TAG, "SettingsScreen resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        taskIterator.stop()
        Log.e(TAG, "SettingsScreen destroyed, stopping task iterator")
        // Note: testTasksJob is not cancelled here to allow it to persist across navigation
    }
}