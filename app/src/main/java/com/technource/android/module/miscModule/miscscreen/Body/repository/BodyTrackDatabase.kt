//package com.technource.android.module.miscModule.miscscreen.Body.repository
//
//interface WaterDao {
//    fun insertWaterLog(log: WaterLog)
//    fun getUnsyncedLogs(): List<WaterLog>
//    fun markAsSynced(id: String)
//    fun getWaterForDay(day: Long): Int
//}
//
//interface WeightDao {
//    fun insertWeightLog(log: WeightLog)
//    fun getUnsyncedLogs(): List<WeightLog>
//    fun markAsSynced(id: String)
//    fun getLatestWeight(): Float?
//}
//
//interface WorkoutDao {
//    fun insertWorkoutSession(session: WorkoutSession)
//    fun getUnsyncedSessions(): List<WorkoutSession>
//    fun markAsSynced(id: String)
//    fun getWorkoutsForDay(day: Long): Int
//}
//
//interface NutritionDao {
//    fun getUnsyncedLogs(): List<NutritionInfo>
//    fun markAsSynced(id: String)
//    fun getNutritionByBarcode(barcode: String): NutritionInfo?
//    fun getCaloriesForDay(day: Long): Int
//}
//
//interface PhotoDao {
//    fun getUnsyncedPhotos(): List<Any>
//    fun markAsSynced(id: String)
//}
//
//interface StepsDao {
//    fun getStepsForDay(day: Long): Int
//}
//
//interface ActivityDao {
//    fun getRecentActivities(limit: Int): List<com.technource.android.module.miscModule.miscscreen.Body.ui.screens.RecentActivity>
//}
//
//interface ReminderDao {
//    fun getActiveReminders(): List<com.technource.android.module.miscModule.miscscreen.Body.ui.screens.HealthReminder>
//}
//
//class BodyTrackDatabase {
//    fun waterDao(): WaterDao = TODO()
//    fun weightDao(): WeightDao = TODO()
//    fun workoutDao(): WorkoutDao = TODO()
//    fun nutritionDao(): NutritionDao = TODO()
//    fun photoDao(): PhotoDao = TODO()
//    fun stepsDao(): StepsDao = TODO()
//    fun activityDao(): ActivityDao = TODO()
//    fun reminderDao(): ReminderDao = TODO()
//}