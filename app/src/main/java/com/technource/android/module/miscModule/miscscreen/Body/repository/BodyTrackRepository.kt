//package com.technource.android.module.miscModule.miscscreen.Body.repository
//
//
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.HealthReminder
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.RecentActivity
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.TodayStats
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class BodyTrackRepository @Inject constructor(
//    private val database: BodyTrackDatabase,
//    private val api: BodyTrackApi
//) {
//
//    suspend fun logWaterIntake(amount: Int) {
//        try {
//            // Save locally first
//            database.waterDao().insertWaterLog(
//                WaterLog(
//                    amount = amount,
//                    timestamp = System.currentTimeMillis()
//                )
//            )
//
//            // Sync with web app
//            api.logWater(amount)
//        } catch (e: Exception) {
//            // Handle offline mode - data will sync later
//        }
//    }
//
//    suspend fun logWeight(weight: Float) {
//        try {
//            database.weightDao().insertWeightLog(
//                WeightLog(
//                    weight = weight,
//                    timestamp = System.currentTimeMillis()
//                )
//            )
//
//            api.logWeight(weight)
//        } catch (e: Exception) {
//            // Handle offline mode
//        }
//    }
//
//    suspend fun startWorkoutSession(): String {
//        val sessionId = generateSessionId()
//        try {
//            database.workoutDao().insertWorkoutSession(
//                WorkoutSession(
//                    id = sessionId,
//                    startTime = System.currentTimeMillis(),
//                    isActive = true
//                )
//            )
//
//            api.startWorkout(sessionId)
//        } catch (e: Exception) {
//            // Handle offline mode
//        }
//        return sessionId
//    }
//
//    suspend fun getNutritionInfoByBarcode(barcode: String): NutritionInfo? {
//        return try {
//            // Try API first
//            api.getNutritionByBarcode(barcode)
//        } catch (e: Exception) {
//            // Fallback to local database
//            database.nutritionDao().getNutritionByBarcode(barcode)
//        }
//    }
//
//    suspend fun getTodayStats(): TodayStats {
//        val today = getTodayTimestamp()
//
//        return TodayStats(
//            steps = database.stepsDao().getStepsForDay(today),
//            calories = database.nutritionDao().getCaloriesForDay(today),
//            water = database.waterDao().getWaterForDay(today),
//            workouts = database.workoutDao().getWorkoutsForDay(today),
//            weight = database.weightDao().getLatestWeight() ?: 0f
//        )
//    }
//
//    suspend fun getRecentActivities(): List<RecentActivity> {
//        return database.activityDao().getRecentActivities(limit = 10)
//    }
//
//    suspend fun getHealthReminders(): List<HealthReminder> {
//        return database.reminderDao().getActiveReminders()
//    }
//
//    // Sync functions
//    suspend fun syncWithWebApp() {
//        try {
//            syncWaterLogs()
//            syncWeightLogs()
//            syncWorkoutSessions()
//            syncNutritionLogs()
//            syncProgressPhotos()
//        } catch (e: Exception) {
//            // Handle sync errors
//        }
//    }
//
//    private suspend fun syncWaterLogs() {
//        val unsyncedLogs = database.waterDao().getUnsyncedLogs()
//        unsyncedLogs.forEach { log ->
//            try {
//                api.logWater(log.amount)
//                database.waterDao().markAsSynced(log.id)
//            } catch (e: Exception) {
//                // Will retry on next sync
//            }
//        }
//    }
//
//    private suspend fun syncWeightLogs() {
//        val unsyncedLogs = database.weightDao().getUnsyncedLogs()
//        unsyncedLogs.forEach { log ->
//            try {
//                api.logWeight(log.weight)
//                database.weightDao().markAsSynced(log.id)
//            } catch (e: Exception) {
//                // Will retry on next sync
//            }
//        }
//    }
//
//    private suspend fun syncWorkoutSessions() {
//        val unsyncedSessions = database.workoutDao().getUnsyncedSessions()
//        unsyncedSessions.forEach { session ->
//            try {
//                api.syncWorkoutSession(session)
//                database.workoutDao().markAsSynced(session.id)
//            } catch (e: Exception) {
//                // Will retry on next sync
//            }
//        }
//    }
//
//    private suspend fun syncNutritionLogs() {
//        val unsyncedLogs = database.nutritionDao().getUnsyncedLogs()
//        unsyncedLogs.forEach { log ->
//            try {
//                api.logNutrition(log)
//                database.nutritionDao().markAsSynced(log.id)
//            } catch (e: Exception) {
//                // Will retry on next sync
//            }
//        }
//    }
//
//    private suspend fun syncProgressPhotos() {
//        val unsyncedPhotos = database.photoDao().getUnsyncedPhotos()
//        unsyncedPhotos.forEach { photo ->
//            try {
//                api.uploadProgressPhoto(photo)
////                database.photoDao().markAsSynced(photo.id)
//            } catch (e: Exception) {
//                // Will retry on next sync
//            }
//        }
//    }
//
//    private fun generateSessionId(): String {
//        return "session_${System.currentTimeMillis()}"
//    }
//
//    private fun getTodayTimestamp(): Long {
//        // Return timestamp for start of today
//        return System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
//    }
//}
//
//// Data classes for local storage
//data class WaterLog(
//    val id: String = generateId(),
//    val amount: Int,
//    val timestamp: Long,
//    val synced: Boolean = false
//)
//
//data class WeightLog(
//    val id: String = generateId(),
//    val weight: Float,
//    val timestamp: Long,
//    val synced: Boolean = false
//)
//
//data class WorkoutSession(
//    val id: String,
//    val startTime: Long,
//    val endTime: Long? = null,
//    val isActive: Boolean,
//    val exercises: List<Exercise> = emptyList(),
//    val synced: Boolean = false
//)
//
//data class Exercise(
//    val name: String,
//    val sets: Int,
//    val reps: String,
//    val weight: String
//)
//
//data class NutritionInfo(
//    val barcode: String,
//    val name: String,
//    val calories: Int,
//    val protein: Float,
//    val carbs: Float,
//    val fat: Float,
//    val id: String
//)
//
//private fun generateId(): String = "id_${System.currentTimeMillis()}_${(0..999).random()}"
