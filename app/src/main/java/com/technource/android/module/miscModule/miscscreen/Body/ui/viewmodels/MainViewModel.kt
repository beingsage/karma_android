//package com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels
//
//import androidx.compose.material.icons.filled.Favorite
//import androidx.compose.material.icons.filled.FitnessCenter
//import androidx.compose.material.icons.filled.Info
//import androidx.compose.material.icons.filled.LocalDrink
//import androidx.compose.material.icons.filled.Restaurant
//import androidx.compose.material.icons.filled.Scale
//import androidx.compose.material.icons.filled.TrendingUp
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.technource.android.module.miscModule.miscscreen.Body.repository.BodyTrackRepository
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.HealthReminder
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.RecentActivity
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.TodayStats
//import dagger.hilt.android.lifecycle.HiltViewModel
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import javax.inject.Inject
//
//@HiltViewModel
//class MainViewModel @Inject constructor(
//    private val repository: BodyTrackRepository
//) : ViewModel() {
//
//    private val _todayStats = MutableStateFlow(TodayStats())
//    val todayStats: StateFlow<TodayStats> = _todayStats.asStateFlow()
//
//    private val _recentActivities = MutableStateFlow<List<RecentActivity>>(emptyList())
//    val recentActivities: StateFlow<List<RecentActivity>> = _recentActivities.asStateFlow()
//
//    private val _healthReminders = MutableStateFlow<List<HealthReminder>>(emptyList())
//    val healthReminders: StateFlow<List<HealthReminder>> = _healthReminders.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(false)
//    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//
////    private val _notificationSettings = MutableStateFlow(NotificationSettings())
////    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings.asStateFlow()
////
//    init {
//        loadTodayStats()
//        loadRecentActivities()
//        loadHealthReminders()
//    }
//
//    // Quick logging functions
//    fun logWater(amount: Int = 250) {
//        viewModelScope.launch {
//            try {
//                repository.logWaterIntake(amount)
//                updateWaterStats(amount)
//                addRecentActivity("Water logged", "${amount}ml added", "water")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun logWeight(weight: Float? = null) {
//        viewModelScope.launch {
//            try {
//                val weightValue = weight ?: getCurrentWeight()
//                repository.logWeight(weightValue)
//                updateWeightStats(weightValue)
//                addRecentActivity("Weight logged", "${weightValue}kg", "weight")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun quickMealLog() {
//        viewModelScope.launch {
//            try {
//                // Open meal logging interface
//                addRecentActivity("Meal logged", "Quick meal entry", "nutrition")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun startWorkout() {
//        viewModelScope.launch {
//            try {
//                repository.startWorkoutSession()
//                addRecentActivity("Workout started", "Timer active", "exercise")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun takeProgressPhoto() {
//        viewModelScope.launch {
//            try {
//                // Handle progress photo capture
//                addRecentActivity("Progress photo", "New photo taken", "progress")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun measureHeartRate() {
//        viewModelScope.launch {
//            try {
//                // Start heart rate measurement
//                addRecentActivity("Heart rate", "Measurement started", "health")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun logMood() {
//        viewModelScope.launch {
//            try {
//                // Open mood logging interface
//                addRecentActivity("Mood logged", "Daily mood check", "health")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun logSleep() {
//        viewModelScope.launch {
//            try {
//                // Log sleep data
//                addRecentActivity("Sleep logged", "Sleep data recorded", "health")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun logMeasurements() {
//        viewModelScope.launch {
//            try {
//                // Open measurements logging
//                addRecentActivity("Measurements", "Body measurements logged", "progress")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    // Camera functions
//    fun capturePhoto() {
//        viewModelScope.launch {
//            try {
//                // Handle photo capture
//                addRecentActivity("Photo captured", "New photo saved", "general")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun scanBarcode() {
//        viewModelScope.launch {
//            try {
//                // Handle barcode scanning
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun captureProgressPhoto() {
//        viewModelScope.launch {
//            try {
//                // Handle progress photo capture
//                addRecentActivity("Progress photo", "New progress photo", "progress")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun processBarcodeResult(barcode: String) {
//        viewModelScope.launch {
//            try {
//                val nutritionInfo = repository.getNutritionInfoByBarcode(barcode)
//                // Process nutrition information
//                addRecentActivity("Food scanned", "Barcode: $barcode", "nutrition")
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    fun openBarcodeScanner() {
//        // Navigate to barcode scanner
//    }
//
//    // Reminder functions
//    fun dismissReminder(reminderId: String) {
//        viewModelScope.launch {
//            val currentReminders = _healthReminders.value.toMutableList()
//            currentReminders.removeAll { it.id == reminderId }
//            _healthReminders.value = currentReminders
//        }
//    }
//
//    fun executeReminderAction(reminderId: String) {
//        viewModelScope.launch {
//            val reminder = _healthReminders.value.find { it.id == reminderId }
//            reminder?.let {
//                when (it.actionType) {
//                    "water" -> logWater()
//                    "weight" -> logWeight()
//                    "workout" -> startWorkout()
//                    // Add more action types as needed
//                }
//                dismissReminder(reminderId)
//            }
//        }
//    }
//
//    // Notification settings
////    fun updateNotificationSetting(type: String, enabled: Boolean) {
////        _notificationSettings.value = _notificationSettings.value.copy(
////            // update the correct field based on type
////            waterReminders = if (type == "water") enabled else _notificationSettings.value.waterReminders,
////            workoutReminders = if (type == "workout") enabled else _notificationSettings.value.workoutReminders,
////            weightReminders = if (type == "weight") enabled else _notificationSettings.value.weightReminders,
////            mealReminders = if (type == "meal") enabled else _notificationSettings.value.mealReminders,
////            sleepReminders = if (type == "sleep") enabled else _notificationSettings.value.sleepReminders,
////        )
////    }
////
////    fun updateNotificationTime(type: String, time: String) {
////        _notificationSettings.value = _notificationSettings.value.copy(
////            waterTime = if (type == "water") time else _notificationSettings.value.waterTime,
////            workoutTime = if (type == "workout") time else _notificationSettings.value.workoutTime,
////            weightTime = if (type == "weight") time else _notificationSettings.value.weightTime,
////            mealTimes = if (type in listOf("breakfast", "lunch", "dinner")) {
////                _notificationSettings.value.mealTimes.toMutableMap().apply { put(type, time) }
////            } else _notificationSettings.value.mealTimes,
////            sleepTime = if (type == "sleep") time else _notificationSettings.value.sleepTime,
////        )
////    }
//
//    // Private helper functions
//    private fun loadTodayStats() {
//        viewModelScope.launch {
//            try {
//                val stats = repository.getTodayStats()
//                _todayStats.value = stats
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    private fun loadRecentActivities() {
//        viewModelScope.launch {
//            try {
//                val activities = repository.getRecentActivities()
//                _recentActivities.value = activities
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    private fun loadHealthReminders() {
//        viewModelScope.launch {
//            try {
//                val reminders = repository.getHealthReminders()
//                _healthReminders.value = reminders
//            } catch (e: Exception) {
//                // Handle error
//            }
//        }
//    }
//
//    private fun updateWaterStats(amount: Int) {
//        val currentStats = _todayStats.value
//        _todayStats.value = currentStats.copy(water = currentStats.water + amount / 1000)
//    }
//
//    private fun updateWeightStats(weight: Float) {
//        val currentStats = _todayStats.value
//        _todayStats.value = currentStats.copy(weight = weight)
//    }
//
//    private fun addRecentActivity(title: String, subtitle: String, type: String) {
//        val currentActivities = _recentActivities.value.toMutableList()
//        val newActivity = RecentActivity(
//            title = title,
//            subtitle = subtitle,
//            time = getCurrentTime(),
//            type = type,
//            icon = getIconForActivityType(type)
//        )
//        currentActivities.add(0, newActivity)
//        _recentActivities.value = currentActivities.take(10) // Keep only latest 10
//    }
//
//    private fun getCurrentWeight(): Float {
//        // Return last logged weight or default
//        return _todayStats.value.weight.takeIf { it > 0 } ?: 70f
//    }
//
//    private fun getCurrentTime(): String {
//        // Return current time formatted
//        return "Just now"
//    }
//
//    private fun getIconForActivityType(type: String) = when (type) {
//        "water" -> androidx.compose.material.icons.Icons.Default.LocalDrink
//        "weight" -> androidx.compose.material.icons.Icons.Default.Scale
//        "nutrition" -> androidx.compose.material.icons.Icons.Default.Restaurant
//        "exercise" -> androidx.compose.material.icons.Icons.Default.FitnessCenter
//        "health" -> androidx.compose.material.icons.Icons.Default.Favorite
//        "progress" -> androidx.compose.material.icons.Icons.Default.TrendingUp
//        else -> androidx.compose.material.icons.Icons.Default.Info
//    }
//}
