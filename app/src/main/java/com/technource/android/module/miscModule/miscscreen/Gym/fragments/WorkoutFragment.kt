package com.technource.android.module.miscModule.miscscreen.Gym.fragments


import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Gym.models.Workout
import com.technource.android.module.miscModule.miscscreen.Gym.utils.NotificationUtils
import com.technource.android.module.miscModule.miscscreen.Gym.utils.PhotoUtils
import com.technource.android.module.miscModule.miscscreen.Gym.utils.SoundManager
import com.technource.android.module.miscModule.miscscreen.Gym.utils.WorkoutManager
import java.text.SimpleDateFormat
import java.util.*

class WorkoutFragment : Fragment() {

    private lateinit var dateTextView: MaterialTextView
    private lateinit var muscleGroupsLayout: ViewGroup
    private lateinit var timerTextView: MaterialTextView
    private lateinit var startPauseButton: MaterialButton
    private lateinit var volumeTextView: MaterialTextView
    private lateinit var setsTextView: MaterialTextView
    private lateinit var weightTextView: MaterialTextView
    private lateinit var repsTextView: MaterialTextView
    private lateinit var caloriesTextView: MaterialTextView
    private lateinit var overallProgressBar: LinearProgressIndicator
    private lateinit var workoutRecyclerView: RecyclerView
    private lateinit var photoButton: MaterialButton
    private lateinit var notesEditText: TextInputEditText

    private lateinit var workoutManager: WorkoutManager
    private lateinit var soundManager: SoundManager
    private lateinit var adapter: WorkoutSectionAdapter

    private var workoutTimer: CountDownTimer? = null
    private var pauseTimer: CountDownTimer? = null
    private var restTimer: CountDownTimer? = null

    private var isWorkoutActive = false
    private var isWorkoutPaused = false
    private var timeLeftInSeconds = 3600 // 60 minutes
    private var pauseStartTime = 0L

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_workout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupWorkoutManager()
        setupRecyclerView()
        updateUI()
        setupClickListeners()
    }

    private fun initializeViews(view: View) {
        dateTextView = view.findViewById(R.id.dateTextView)
        muscleGroupsLayout = view.findViewById(R.id.muscleGroupsLayout)
        timerTextView = view.findViewById(R.id.timerTextView)
        startPauseButton = view.findViewById(R.id.startPauseButton)
        volumeTextView = view.findViewById(R.id.volumeTextView)
        setsTextView = view.findViewById(R.id.setsTextView)
        weightTextView = view.findViewById(R.id.weightTextView)
        repsTextView = view.findViewById(R.id.repsTextView)
        caloriesTextView = view.findViewById(R.id.caloriesTextView)
        overallProgressBar = view.findViewById(R.id.overallProgressBar)
        workoutRecyclerView = view.findViewById(R.id.workoutRecyclerView)
        photoButton = view.findViewById(R.id.photoButton)
        notesEditText = view.findViewById(R.id.notesEditText) as TextInputEditText
    }

    private fun setupWorkoutManager() {
        workoutManager = WorkoutManager(requireContext())
        soundManager = SoundManager(requireContext())

        workoutManager.setOnWorkoutUpdateListener { workout ->
            updateWorkoutStats(workout)
        }

        workoutManager.setOnSetCompleteListener { exercise ->
            soundManager.playSetCompleteSound()
            if (exercise.restTime > 0 && exercise.completed < exercise.sets) {
                startRestTimer(exercise.restTime)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = WorkoutSectionAdapter(workoutManager.getWorkoutSections()) { sectionId, exerciseId ->
            workoutManager.completeSet(sectionId, exerciseId)
        }

        workoutRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        workoutRecyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        startPauseButton.setOnClickListener {
            toggleWorkoutTimer()
        }

        photoButton.setOnClickListener {
            // Launch camera or gallery picker
            PhotoUtils.takePicture(this) { photoUri ->
                workoutManager.saveProgressPhoto(photoUri)
            }
        }
    }

    private fun updateUI() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        dateTextView.text = dateFormat.format(Date())

        updateMuscleGroups()
        updateTimer()
        updateWorkoutStats(workoutManager.getCurrentWorkout())
    }

    private fun updateMuscleGroups() {
        muscleGroupsLayout.removeAllViews()
        val muscleGroups = listOf("Chest", "Back", "Legs", "Shoulders")

        muscleGroups.forEach { muscle ->
            val chip = LayoutInflater.from(requireContext())
                .inflate(R.layout.muscle_group_chip, muscleGroupsLayout, false)
            val textView = chip.findViewById<MaterialTextView>(R.id.chipText)
            textView.text = muscle
            muscleGroupsLayout.addView(chip)
        }
    }

    private fun updateTimer() {
        val minutes = timeLeftInSeconds / 60
        val seconds = timeLeftInSeconds % 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateWorkoutStats(workout: Workout) {
        volumeTextView.text = workout.totalVolume.toString()
        setsTextView.text = "${workout.completedSets}/${workout.totalSets}"
        weightTextView.text = "${workout.totalWeight} lbs"
        repsTextView.text = workout.totalReps.toString()
        caloriesTextView.text = workout.estimatedCalories.toString()

        val progress = if (workout.totalSets > 0) {
            (workout.completedSets.toFloat() / workout.totalSets.toFloat() * 100).toInt()
        } else 0

        overallProgressBar.progress = progress
    }

    private fun toggleWorkoutTimer() {
        if (!isWorkoutActive) {
            startWorkout()
        } else {
            if (isWorkoutPaused) {
                resumeWorkout()
            } else {
                pauseWorkout()
            }
        }
    }

    private fun startWorkout() {
        isWorkoutActive = true
        isWorkoutPaused = false
        workoutManager.startWorkout()
        soundManager.playTimerStartSound()

        startPauseButton.text = "Pause"
        startPauseButton.setBackgroundColor(resources.getColor(R.color.error, null))

        workoutTimer = object : CountDownTimer((timeLeftInSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInSeconds = (millisUntilFinished / 1000).toInt()
                updateTimer()
            }

            override fun onFinish() {
                finishWorkout()
            }
        }.start()
    }

    private fun pauseWorkout() {
        isWorkoutPaused = true
        pauseStartTime = System.currentTimeMillis()
        workoutTimer?.cancel()
        soundManager.playTimerPauseSound()

        startPauseButton.text = "Resume"
        startPauseButton.setBackgroundColor(resources.getColor(R.color.primary, null))

        // Auto-resume after 2 minutes
        pauseTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update pause indicator if needed
            }

            override fun onFinish() {
                if (isWorkoutPaused) {
                    resumeWorkout()
                    NotificationUtils.showNotification(
                        requireContext(),
                        "Pause Limit Reached",
                        "2-minute pause limit reached. Timer resumed automatically."
                    )
                }
            }
        }.start()
    }

    private fun resumeWorkout() {
        isWorkoutPaused = false
        pauseTimer?.cancel()
        soundManager.playTimerResumeSound()

        startPauseButton.text = "Pause"
        startPauseButton.setBackgroundColor(resources.getColor(R.color.error, null))

        workoutTimer = object : CountDownTimer((timeLeftInSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInSeconds = (millisUntilFinished / 1000).toInt()
                updateTimer()
            }

            override fun onFinish() {
                finishWorkout()
            }
        }.start()
    }

    private fun finishWorkout() {
        isWorkoutActive = false
        workoutTimer?.cancel()
        workoutManager.finishWorkout()
        soundManager.playWorkoutCompleteSound()

        startPauseButton.text = "Start"
        startPauseButton.setBackgroundColor(resources.getColor(R.color.primary, null))

        NotificationUtils.showNotification(
            requireContext(),
            "Workout Complete!",
            "Great job! You've completed your workout session."
        )

        photoButton.visibility = View.VISIBLE
    }

    private fun startRestTimer(duration: Int) {
        val restTimerView = LayoutInflater.from(requireContext())
            .inflate(R.layout.rest_timer_overlay, null)

        val timerText = restTimerView.findViewById<MaterialTextView>(R.id.restTimerText)
        val progressBar = restTimerView.findViewById<LinearProgressIndicator>(R.id.restProgressBar)
        val skipButton = restTimerView.findViewById<MaterialButton>(R.id.skipRestButton)

        // Add to parent view
        (view as ViewGroup).addView(restTimerView)

        restTimer = object : CountDownTimer((duration * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = (millisUntilFinished / 1000).toInt()
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)

                val progress = ((duration - secondsLeft).toFloat() / duration.toFloat() * 100).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                (view as ViewGroup).removeView(restTimerView)
                soundManager.playRestCompleteSound()
                NotificationUtils.showNotification(
                    requireContext(),
                    "Rest Complete!",
                    "Time to get back to work!"
                )
            }
        }.start()

        skipButton.setOnClickListener {
            restTimer?.cancel()
            (view as ViewGroup).removeView(restTimerView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        workoutTimer?.cancel()
        pauseTimer?.cancel()
        restTimer?.cancel()
    }
}
