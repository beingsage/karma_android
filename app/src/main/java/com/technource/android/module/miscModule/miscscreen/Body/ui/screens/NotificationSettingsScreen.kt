//package com.technource.android.module.miscModule.miscscreen.Body.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import androidx.compose.runtime.getValue
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NotificationSettingsScreen(navController: NavController, viewModel: MainViewModel) {
//
////    val notificationSettings by viewModel.notificationSettings.collectAsState()
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            IconButton(onClick = { navController.popBackStack() }) {
//                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//            }
//            Text(
//                text = "Notification Settings",
//                style = MaterialTheme.typography.headlineSmall,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(start = 8.dp)
//            )
//        }
//
//        Spacer(modifier = Modifier.height(24.dp))
//
////        LazyColumn(
////            verticalArrangement = Arrangement.spacedBy(8.dp)
////        ) {
////            items(getNotificationCategories()) { category ->
////                NotificationCategoryCard(
////                    category = category,
////                    settings = notificationSettings,
////                    onSettingChanged = { type, enabled ->
////                        viewModel.updateNotificationSetting(type, enabled)
////                    },
////                    onTimeChanged = { type, time ->
////                        viewModel.updateNotificationTime(type, time)
////                    }
////                )
////            }
////        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun NotificationCategoryCard(
//    category: NotificationCategory,
//    settings: NotificationSettings,
//    onSettingChanged: (String, Boolean) -> Unit,
//    onTimeChanged: (String, String) -> Unit
//) {
//    var expanded by remember { mutableStateOf(false) }
//
//    Card(
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Column(
//            modifier = Modifier.padding(16.dp)
//        ) {
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Icon(
//                        imageVector = category.icon,
//                        contentDescription = category.title,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(24.dp)
//                    )
//                    Spacer(modifier = Modifier.width(12.dp))
//                    Column {
//                        Text(
//                            text = category.title,
//                            style = MaterialTheme.typography.titleMedium,
//                            fontWeight = FontWeight.Medium
//                        )
//                        Text(
//                            text = category.description,
//                            style = MaterialTheme.typography.bodySmall,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//
//                Switch(
//                    checked = settings.isEnabled(category.type),
//                    onCheckedChange = { enabled ->
//                        onSettingChanged(category.type, enabled)
//                    }
//                )
//            }
//
//            if (settings.isEnabled(category.type)) {
//                Spacer(modifier = Modifier.height(16.dp))
//
//                // Time settings
//                category.timeOptions.forEach { timeOption ->
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 4.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = timeOption.label,
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//
//                        OutlinedButton(
//                            onClick = {
//                                // Open time picker
//                                // For now, just cycle through preset times
//                                val times = listOf("08:00", "12:00", "18:00", "20:00")
//                                val currentIndex = times.indexOf(settings.getTime(timeOption.key))
//                                val nextIndex = (currentIndex + 1) % times.size
//                                onTimeChanged(timeOption.key, times[nextIndex])
//                            }
//                        ) {
//                            Text(settings.getTime(timeOption.key))
//                        }
//                    }
//                }
//
//                // Frequency settings
//                if (category.hasFrequency) {
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Frequency",
//                            style = MaterialTheme.typography.bodyMedium
//                        )
//
//                        val frequencies = listOf("Every hour", "Every 2 hours", "Every 4 hours")
//                        var selectedFrequency by remember { mutableStateOf(frequencies[0]) }
//
//                        OutlinedButton(
//                            onClick = {
//                                val currentIndex = frequencies.indexOf(selectedFrequency)
//                                selectedFrequency = frequencies[(currentIndex + 1) % frequencies.size]
//                            }
//                        ) {
//                            Text(selectedFrequency)
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//data class NotificationCategory(
//    val type: String,
//    val title: String,
//    val description: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector,
//    val timeOptions: List<TimeOption> = emptyList(),
//    val hasFrequency: Boolean = false
//)
//
//data class TimeOption(
//    val key: String,
//    val label: String
//)
//
//data class NotificationSettings(
//    val waterReminders: Boolean = true,
//    val workoutReminders: Boolean = true,
//    val weightReminders: Boolean = true,
//    val mealReminders: Boolean = true,
//    val sleepReminders: Boolean = true,
//    val waterTime: String = "08:00",
//    val workoutTime: String = "18:00",
//    val weightTime: String = "08:00",
//    val mealTimes: Map<String, String> = mapOf(
//        "breakfast" to "08:00",
//        "lunch" to "12:00",
//        "dinner" to "18:00"
//    ),
//    val sleepTime: String = "22:00"
//) {
//    fun isEnabled(type: String): Boolean = when (type) {
//        "water" -> waterReminders
//        "workout" -> workoutReminders
//        "weight" -> weightReminders
//        "meal" -> mealReminders
//        "sleep" -> sleepReminders
//        else -> false
//    }
//
//    fun getTime(key: String): String = when (key) {
//        "water" -> waterTime
//        "workout" -> workoutTime
//        "weight" -> weightTime
//        "breakfast" -> mealTimes["breakfast"] ?: "08:00"
//        "lunch" -> mealTimes["lunch"] ?: "12:00"
//        "dinner" -> mealTimes["dinner"] ?: "18:00"
//        "sleep" -> sleepTime
//        else -> "08:00"
//    }
//}
//
//fun getNotificationCategories(): List<NotificationCategory> = listOf(
//    NotificationCategory(
//        type = "water",
//        title = "Water Reminders",
//        description = "Stay hydrated throughout the day",
//        icon = Icons.Default.LocalDrink,
//        hasFrequency = true
//    ),
//    NotificationCategory(
//        type = "workout",
//        title = "Workout Reminders",
//        description = "Don't skip your daily exercise",
//        icon = Icons.Default.FitnessCenter,
//        timeOptions = listOf(
//            TimeOption("workout", "Workout Time")
//        )
//    ),
//    NotificationCategory(
//        type = "weight",
//        title = "Weight Tracking",
//        description = "Daily weight logging reminders",
//        icon = Icons.Default.Scale,
//        timeOptions = listOf(
//            TimeOption("weight", "Weigh-in Time")
//        )
//    ),
//    NotificationCategory(
//        type = "meal",
//        title = "Meal Logging",
//        description = "Remember to log your meals",
//        icon = Icons.Default.Restaurant,
//        timeOptions = listOf(
//            TimeOption("breakfast", "Breakfast"),
//            TimeOption("lunch", "Lunch"),
//            TimeOption("dinner", "Dinner")
//        )
//    ),
//    NotificationCategory(
//        type = "sleep",
//        title = "Sleep Reminders",
//        description = "Maintain a healthy sleep schedule",
//        icon = Icons.Default.Bedtime,
//        timeOptions = listOf(
//            TimeOption("sleep", "Bedtime")
//        )
//    )
//)
