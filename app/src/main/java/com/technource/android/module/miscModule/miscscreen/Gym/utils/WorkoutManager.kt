package com.technource.android.module.miscModule.miscscreen.Gym.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.technource.android.module.miscModule.miscscreen.Gym.models.Exercise
import com.technource.android.module.miscModule.miscscreen.Gym.models.PersonalRecord
import com.technource.android.module.miscModule.miscscreen.Gym.models.SectionType
import com.technource.android.module.miscModule.miscscreen.Gym.models.Workout
import com.technource.android.module.miscModule.miscscreen.Gym.models.WorkoutHistory
import com.technource.android.module.miscModule.miscscreen.Gym.models.WorkoutSection
import java.util.*

class WorkoutManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("workout_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var currentWorkout: Workout
    private var workoutHistory: MutableList<WorkoutHistory> = mutableListOf()
    private var personalRecords: MutableList<PersonalRecord> = mutableListOf()

    private var onWorkoutUpdateListener: ((Workout) -> Unit)? = null
    private var onSetCompleteListener: ((Exercise) -> Unit)? = null

    init {
        currentWorkout = loadCurrentWorkout() ?: createDefaultWorkout()
        workoutHistory = loadWorkoutHistory()
        personalRecords = loadPersonalRecords()
    }

    fun setOnWorkoutUpdateListener(listener: (Workout) -> Unit) {
        onWorkoutUpdateListener = listener
    }

    fun setOnSetCompleteListener(listener: (Exercise) -> Unit) {
        onSetCompleteListener = listener
    }

    fun getCurrentWorkout(): Workout = currentWorkout

    fun getWorkoutSections(): List<WorkoutSection> = currentWorkout.sections

    fun getWorkoutHistory(): List<WorkoutHistory> = workoutHistory

    fun getPersonalRecords(): List<PersonalRecord> = personalRecords

    fun startWorkout() {
        currentWorkout = currentWorkout.copy(
            isActive = true,
            isPaused = false,
            startTime = Date()
        )
        saveCurrentWorkout()
        notifyWorkoutUpdate()
    }

    fun pauseWorkout() {
        currentWorkout = currentWorkout.copy(isPaused = true)
        saveCurrentWorkout()
        notifyWorkoutUpdate()
    }

    fun resumeWorkout() {
        currentWorkout = currentWorkout.copy(isPaused = false)
        saveCurrentWorkout()
        notifyWorkoutUpdate()
    }

    fun finishWorkout() {
        val endTime = Date()
        val duration = if (currentWorkout.startTime != null) {
            ((endTime.time - currentWorkout.startTime!!.time) / (1000 * 60)).toInt()
        } else 0

        currentWorkout = currentWorkout.copy(
            isActive = false,
            isPaused = false,
            endTime = endTime
        )

        // Save to history
        val historyEntry = WorkoutHistory(
            id = System.currentTimeMillis(),
            date = currentWorkout.date,
            duration = duration,
            totalVolume = currentWorkout.totalVolume,
            totalSets = currentWorkout.completedSets,
            totalReps = currentWorkout.totalReps,
            muscleGroups = currentWorkout.sections
                .filter { it.type == SectionType.EXERCISE }
                .flatMap { section -> section.exercises.map { it.targetMuscle.capitalize() } }
                .distinct(),
            notes = currentWorkout.notes,
            photoUrl = currentWorkout.photoUrl
        )

        workoutHistory.add(0, historyEntry)
        saveWorkoutHistory()

        // Reset for next workout
        currentWorkout = createDefaultWorkout()
        saveCurrentWorkout()
        notifyWorkoutUpdate()
    }

    fun completeSet(sectionId: Int, exerciseId: Int) {
        val updatedSections = currentWorkout.sections.map { section ->
            if (section.id == sectionId) {
                val updatedExercises = section.exercises.map { exercise ->
                    if (exercise.id == exerciseId && exercise.completed < exercise.sets) {
                        val newCompleted = exercise.completed + 1
                        val updatedExercise = exercise.copy(
                            completed = newCompleted,
                            inProgress = newCompleted < exercise.sets
                        )

                        // Check for personal record
                        checkPersonalRecord(updatedExercise)

                        // Notify set completion
                        onSetCompleteListener?.invoke(updatedExercise)

                        updatedExercise
                    } else exercise
                }
                section.copy(exercises = updatedExercises)
            } else section
        }

        currentWorkout = currentWorkout.copy(sections = updatedSections)
        saveCurrentWorkout()
        notifyWorkoutUpdate()
    }

    fun completeCurrentSet() {
        // Find the first incomplete exercise and complete a set
        for (section in currentWorkout.sections) {
            for (exercise in section.exercises) {
                if (exercise.completed < exercise.sets) {
                    completeSet(section.id, exercise.id)
                    return
                }
            }
        }
    }

    fun nextExercise() {
        // Mark current exercise as complete and move to next
        // Implementation depends on your specific logic
    }

    fun updateExerciseNotes(sectionId: Int, exerciseId: Int, notes: String) {
        val updatedSections = currentWorkout.sections.map { section ->
            if (section.id == sectionId) {
                val updatedExercises = section.exercises.map { exercise ->
                    if (exercise.id == exerciseId) {
                        exercise.copy(notes = notes)
                    } else exercise
                }
                section.copy(exercises = updatedExercises)
            } else section
        }

        currentWorkout = currentWorkout.copy(sections = updatedSections)
        saveCurrentWorkout()
    }

    fun updateWorkoutNotes(notes: String) {
        currentWorkout = currentWorkout.copy(notes = notes)
        saveCurrentWorkout()
    }

    fun saveProgressPhoto(photoUrl: String) {
        currentWorkout = currentWorkout.copy(photoUrl = photoUrl)
        saveCurrentWorkout()
    }

    private fun checkPersonalRecord(exercise: Exercise) {
        val currentVolume = exercise.weight * exercise.reps
        val existingPR = personalRecords.find { it.exerciseName == exercise.name }

        if (existingPR == null || currentVolume > existingPR.weight * existingPR.reps) {
            val newPR = PersonalRecord(
                exerciseName = exercise.name,
                weight = exercise.weight,
                reps = exercise.reps,
                date = Date(),
                previousRecord = existingPR?.let { it.weight * it.reps }
            )

            personalRecords.removeAll { it.exerciseName == exercise.name }
            personalRecords.add(newPR)
            savePersonalRecords()
        }
    }

    private fun createDefaultWorkout(): Workout {
        val warmupSection = WorkoutSection(
            id = 1,
            name = "Warm Up",
            type = SectionType.WARMUP,
            exercises = listOf(
                Exercise(
                    id = 101,
                    name = "Dynamic Stretching",
                    targetMuscle = "full-body",
                    sets = 1,
                    reps = 1,
                    weight = 0.0,
                    restTime = 0,
                    videoUrl = "https://example.com/warmup-video"
                )
            )
        )

        val mainWorkoutSection = WorkoutSection(
            id = 2,
            name = "Main Workout",
            type = SectionType.EXERCISE,
            exercises = listOf(
                Exercise(
                    id = 201,
                    name = "Bench Press",
                    targetMuscle = "chest",
                    sets = 4,
                    reps = 10,
                    weight = 135.0,
                    restTime = 90,
                    videoUrl = "https://example.com/bench-press",
                    personalRecord = 225.0,
                    lastWeight = 130.0,
                    suggestions = listOf("Try increasing weight by 5lbs", "Focus on controlled negatives")
                ),
                Exercise(
                    id = 202,
                    name = "Pull-ups",
                    targetMuscle = "back",
                    sets = 3,
                    reps = 8,
                    weight = 0.0,
                    restTime = 90,
                    videoUrl = "https://example.com/pull-ups",
                    personalRecord = 15.0,
                    suggestions = listOf("Add weight if completing all reps easily", "Try different grip variations")
                ),
                Exercise(
                    id = 203,
                    name = "Squats",
                    targetMuscle = "legs",
                    sets = 4,
                    reps = 12,
                    weight = 185.0,
                    restTime = 120,
                    videoUrl = "https://example.com/squats",
                    personalRecord = 315.0,
                    lastWeight = 180.0,
                    suggestions = listOf("Increase weight gradually", "Focus on depth and form")
                ),
                Exercise(
                    id = 204,
                    name = "Shoulder Press",
                    targetMuscle = "shoulders",
                    sets = 3,
                    reps = 10,
                    weight = 95.0,
                    restTime = 90,
                    videoUrl = "https://example.com/shoulder-press",
                    personalRecord = 115.0,
                    lastWeight = 90.0,
                    suggestions = listOf("Try seated variation for stability", "Warm up shoulders thoroughly")
                )
            )
        )

        val cooldownSection = WorkoutSection(
            id = 3,
            name = "Cool Down",
            type = SectionType.STRETCHING,
            exercises = listOf(
                Exercise(
                    id = 301,
                    name = "Static Stretching",
                    targetMuscle = "full-body",
                    sets = 1,
                    reps = 1,
                    weight = 0.0,
                    restTime = 0,
                    videoUrl = "https://example.com/stretching"
                )
            )
        )

        return Workout(
            sections = listOf(warmupSection, mainWorkoutSection, cooldownSection)
        )
    }

    private fun notifyWorkoutUpdate() {
        onWorkoutUpdateListener?.invoke(currentWorkout)
    }

    private fun saveCurrentWorkout() {
        val json = gson.toJson(currentWorkout)
        sharedPrefs.edit().putString("current_workout", json).apply()
    }

    private fun loadCurrentWorkout(): Workout? {
        val json = sharedPrefs.getString("current_workout", null)
        return if (json != null) {
            try {
                gson.fromJson(json, Workout::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    private fun saveWorkoutHistory() {
        val json = gson.toJson(workoutHistory)
        sharedPrefs.edit().putString("workout_history", json).apply()
    }

    private fun loadWorkoutHistory(): MutableList<WorkoutHistory> {
        val json = sharedPrefs.getString("workout_history", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<WorkoutHistory>>() {}.type
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else mutableListOf()
    }

    private fun savePersonalRecords() {
        val json = gson.toJson(personalRecords)
        sharedPrefs.edit().putString("personal_records", json).apply()
    }

    private fun loadPersonalRecords(): MutableList<PersonalRecord> {
        val json = sharedPrefs.getString("personal_records", null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<PersonalRecord>>() {}.type
                gson.fromJson(json, type) ?: mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
        } else mutableListOf()
    }
}
