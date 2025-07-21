package com.technource.android.module.miscModule.miscscreen.Gym.models
import java.util.*

data class Exercise(
    val id: Int,
    val name: String,
    val targetMuscle: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    var completed: Int = 0,
    var inProgress: Boolean = false,
    val restTime: Int,
    var notes: String = "",
    val videoUrl: String = "",
    val personalRecord: Double? = null,
    val lastWeight: Double? = null,
    val suggestions: List<String> = emptyList()
)

data class WorkoutSection(
    val id: Int,
    val name: String,
    val type: SectionType,
    val exercises: List<Exercise>,
    var completed: Boolean = false
)

enum class SectionType {
    WARMUP, EXERCISE, STRETCHING
}

data class Workout(
    val id: Long = System.currentTimeMillis(),
    val date: Date = Date(),
    val sections: List<WorkoutSection>,
    var isActive: Boolean = false,
    var isPaused: Boolean = false,
    var startTime: Date? = null,
    var endTime: Date? = null,
    var notes: String = "",
    var photoUrl: String? = null
) {
    val totalSets: Int
        get() = sections.flatMap { it.exercises }.sumOf { it.sets }

    val completedSets: Int
        get() = sections.flatMap { it.exercises }.sumOf { it.completed }

    val totalReps: Int
        get() = sections.flatMap { it.exercises }.sumOf { it.reps * it.completed }

    val totalWeight: Double
        get() = sections.flatMap { it.exercises }.sumOf { it.weight * it.completed }

    val totalVolume: Double
        get() = sections.flatMap { it.exercises }.sumOf { it.weight * it.reps * it.completed }

    val estimatedCalories: Int
        get() {
            val duration = if (startTime != null) {
                (System.currentTimeMillis() - startTime!!.time) / (1000 * 60) // minutes
            } else 0
            val baseCaloriesPerMinute = 8.0
            val intensityMultiplier = minOf(totalVolume / 10000.0, 2.0)
            return (baseCaloriesPerMinute * duration * intensityMultiplier).toInt()
        }

    val progressPercentage: Int
        get() = if (totalSets > 0) ((completedSets.toDouble() / totalSets.toDouble()) * 100).toInt() else 0
}

data class PersonalRecord(
    val exerciseName: String,
    val weight: Double,
    val reps: Int,
    val date: Date,
    val previousRecord: Double? = null
)

data class WorkoutHistory(
    val id: Long,
    val date: Date,
    val duration: Int, // in minutes
    val totalVolume: Double,
    val totalSets: Int,
    val totalReps: Int,
    val muscleGroups: List<String>,
    val notes: String,
    val photoUrl: String? = null
)

data class WorkoutPlan(
    val id: Long,
    val name: String,
    val description: String,
    val daysPerWeek: Int,
    val workouts: List<Workout>,
    val isActive: Boolean = false
)
