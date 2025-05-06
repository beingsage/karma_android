package com.technource.android.ETMS.micro

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.databinding.ActivityTaskLoggerBinding
import com.technource.android.local.AppDatabase
import com.technource.android.local.TaskDao
import com.technource.android.local.toTaskEntity
import com.technource.android.local.Task
import com.technource.android.system_status.SystemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class TaskLoggerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskLoggerBinding
    private lateinit var taskDao: TaskDao
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var vibrator: Vibrator
    private val handler = Handler(Looper.getMainLooper())
    private var isLoopRunning = true
    private val TAG = "TaskLoggerActivity"
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private val shakeThreshold = 12f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskLoggerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Capture touch anywhere on the screen
        window.decorView.rootView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d(TAG, "Global screen touched — stopping loop")
                stopLoop()
                return@setOnTouchListener true
            }
            false
        }

        val task = intent.getSerializableExtra("TASK") as? Task ?: return
        val database = AppDatabase.getInstance(this)
        taskDao = database.taskDao()

        // Initialize vibrator
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!


        // Start the vibration and ringtone sequence
        startVibrationAndRingtoneSequence()
        setupUI(task)
    }

    private fun startVibrationAndRingtoneSequence() {
        handler.removeCallbacksAndMessages(null) // Prevent stacking

        if (!isLoopRunning) return

        vibrateForDuration(5_000L)
        handler.postDelayed({
            if (isLoopRunning) {
                vibrateForDuration(5_000L)
                playRingtone()
            }
        }, 5_000L)

        handler.postDelayed({
            if (isLoopRunning) {
                vibrateForDuration(5_000L)
                playRingtone()
            }
        }, 10_000L)

    }

    private fun vibrateForDuration(durationMs: Long) {
        try {
            if (vibrator.hasVibrator() && isLoopRunning) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 500, 200, 500, 200, 500, 200)
                    val amplitudes = intArrayOf(0, 128, 0, 128, 0, 128, 0)
                    val repeat = -1
                    val vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, repeat)
                    vibrator.vibrate(vibrationEffect)
                    handler.postDelayed({

                        if (isLoopRunning) vibrator.cancel()
                    }, durationMs)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs)
                }
                SystemStatus.logEvent("TaskLoggerActivity", "Vibration started for $durationMs ms")
            }
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskLoggerActivity", "Error during vibration: ${e.message}")
            Toast.makeText(this, "Error during vibration", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playRingtone(maxVolume: Boolean = false) {
        try {
            if (isLoopRunning) {
                // Release any existing MediaPlayer instance
                mediaPlayer?.release()
                mediaPlayer = null

                // Create and prepare MediaPlayer
                mediaPlayer = MediaPlayer.create(this, R.raw.sunhump)
                if (mediaPlayer == null) {
                    Log.e(TAG, "Failed to create MediaPlayer for R.raw.sunhump")
                    throw IllegalStateException("MediaPlayer creation failed")
                }

                if (maxVolume) {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolumeLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolumeLevel, 0)
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                }

                mediaPlayer?.setOnPreparedListener {
                    if (isLoopRunning) {
                        it.start()
                        Log.d(TAG, "Ringtone started${if (maxVolume) " at max volume" else ""}")
                    } else {
                        it.release()
                    }
                }

                mediaPlayer?.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    true
                }

                // Schedule stop after 15 seconds (to match ringtone duration if longer)
                handler.postDelayed({
                    if (mediaPlayer?.isPlaying == true && isLoopRunning) {
                        mediaPlayer?.stop()
                    }
                    mediaPlayer?.release()
                    mediaPlayer = null
                }, 15_000L)

                SystemStatus.logEvent("TaskLoggerActivity", "Ringtone started${if (maxVolume) " at max volume" else ""}")
            }
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskLoggerActivity", "Error playing ringtone: ${e.message}")
            Log.e(TAG, "Ringtone error: ${e.message}")
            Toast.makeText(this, "Error playing ringtone: ${e.message}", Toast.LENGTH_SHORT).show()
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isLoopRunning) return

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val acceleration = sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH
            if (acceleration > shakeThreshold) {
                Log.d(TAG, "Shake detected — stopping loop")
                stopLoop()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(sensorListener)
        stopLoop()
    }


    private fun stopLoop() {
        isLoopRunning = false
        // Stop any ongoing vibration
        vibrator.cancel()
        // Stop and release the media player
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        // Remove any pending callbacks
        handler.removeCallbacksAndMessages(null)
        SystemStatus.logEvent("TaskLoggerActivity", "Loop stopped due to touch")
        Log.d(TAG, "Loop stopped")
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLoop() // Ensure all resources are cleaned up
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    private fun setupUI(task: Task) {
        // Reset completion status
        task.completionStatus = 0.0f
        task.subtasks?.forEach { subTask ->
            subTask.completionStatus = 0.0f
            subTask.finalScore = 0.0f
            when (subTask.measurementType) {
                "binary" -> subTask.binary?.completed = false
                "time" -> subTask.time?.timeSpent = 0
                "quant" -> subTask.quant?.achievedValue = 0
                "deepwork" -> subTask.deepwork?.deepworkScore = 0
            }
        }

        try {
            val color = Color.parseColor(task.color)
            val tintedColor = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color))
            val gradientDrawable = binding.rootLayout.background as? GradientDrawable
            gradientDrawable?.setTint(tintedColor)
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskLoggerActivity", "Invalid color format for task ${task.title}: ${task.color}")
            binding.rootLayout.setBackgroundResource(R.drawable.background_gradient)
        }

        binding.taskTitle.text = task.title

        val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        utcFormat.timeZone = TimeZone.getTimeZone("UTC")
        val localFormat = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
        localFormat.timeZone = TimeZone.getDefault()

        val startTime = try {
            localFormat.format(utcFormat.parse(task.startTime) ?: Date())
        } catch (e: Exception) {
            SystemStatus.logEvent("TaskLoggerActivity", "Invalid start time for task ${task.title}")
            Toast.makeText(this, "Invalid start time for task", Toast.LENGTH_SHORT).show()
            "Unknown"
        }
        val endTime = try {
            localFormat.format(utcFormat.parse(task.endTime) ?: Date())
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid end time for task", Toast.LENGTH_SHORT).show()
            "Unknown"
        }

        binding.taskTime.text = "Time: $startTime - $endTime"
        binding.taskDuration.text = "Duration: ${task.duration} minutes"
        binding.taskProgress.progress = (task.completionStatus * 100).toInt()

        binding.subTasksRecycler.apply {
            layoutManager = LinearLayoutManager(this@TaskLoggerActivity)
            adapter = SubTaskAdapter(task.subtasks ?: emptyList()) { subTask, completionData, position ->
                when (subTask.measurementType) {
                    "binary" -> {
                        subTask.completionStatus = if (completionData as Boolean) 1.0f else 0.0f
                        subTask.finalScore = if (completionData) subTask.baseScore.toFloat() else 0.0f
                        subTask.binary?.completed = completionData
                        SystemStatus.logEvent(
                            "TaskLoggerActivity",
                            "Subtask ${subTask.title} (binary) marked as ${if (completionData) "completed" else "not completed"}"
                        )
                    }
                    "time" -> {
                        val timeSpent = completionData as Int
                        subTask.time?.timeSpent = timeSpent
                        subTask.completionStatus = if (subTask.time?.setDuration != 0) {
                            timeSpent.toFloat() / subTask.time?.setDuration!!
                        } else 0.0f
                        subTask.finalScore = subTask.baseScore * subTask.completionStatus
                        SystemStatus.logEvent(
                            "TaskLoggerActivity",
                            "Subtask ${subTask.title} (time) logged $timeSpent/${subTask.time?.setDuration}m"
                        )
                    }
                    "quant" -> {
                        val achievedValue = completionData as Int
                        subTask.quant?.achievedValue = achievedValue
                        subTask.completionStatus = if (subTask.quant?.targetValue?.toFloat() != 0f) {
                            achievedValue.toFloat() / subTask.quant?.targetValue!!
                        } else 0.0f
                        subTask.finalScore = subTask.baseScore * subTask.completionStatus
                        SystemStatus.logEvent(
                            "TaskLoggerActivity",
                            "Subtask ${subTask.title} (quant) logged $achievedValue/${subTask.quant?.targetValue} ${subTask.quant?.targetUnit}"
                        )
                    }
                    "deepwork" -> {
                        subTask.completionStatus = if (completionData as Boolean) 1.0f else 0.0f
                        subTask.finalScore = if (completionData) subTask.baseScore.toFloat() else 0.0f
                        subTask.deepwork?.deepworkScore = if (completionData) 100 else 0
                        SystemStatus.logEvent(
                            "TaskLoggerActivity",
                            "Subtask ${subTask.title} (deepwork) marked as ${if (completionData) "completed" else "not completed"}"
                        )
                    }
                }
                task.completionStatus = if (task.subtasks?.isNotEmpty() == true) {
                    task.subtasks.map { it.completionStatus }.average().toFloat()
                } else 0.0f
                binding.taskProgress.progress = (task.completionStatus * 100).toInt()
                (adapter as? SubTaskAdapter)?.notifyItemChanged(position)
            }
            addItemDecoration(SpaceItemDecoration(16))
        }

        binding.logTaskButton.setOnClickListener {
            binding.logTaskButton.isEnabled = false
            binding.logTaskButton.text = "Saving..."
            task.subtasks?.let { subtasks ->
                task.completionStatus = if (subtasks.isNotEmpty()) {
                    subtasks.map { it.completionStatus }.average().toFloat()
                } else 0.0f
                task.taskScore = subtasks.sumOf { it.finalScore.toDouble() }.toFloat()

                CoroutineScope(Dispatchers.IO).launch {
                    taskDao.updateTask(task.toTaskEntity())
                    withContext(Dispatchers.Main) {
                        SystemStatus.logEvent(
                            "TaskLoggerActivity",
                            "Task ${task.title} saved with completion status ${task.completionStatus}, score ${task.taskScore}"
                        )
                        setResult(RESULT_OK)
                        finish()
                    }
                }
            } ?: run {
                binding.subTasksRecycler.visibility = View.GONE
                SystemStatus.logEvent("TaskLoggerActivity", "No subtasks for task ${task.title}")
                finish()
            }
        }
    }
}

class SpaceItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = space
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = space
        }
    }
}