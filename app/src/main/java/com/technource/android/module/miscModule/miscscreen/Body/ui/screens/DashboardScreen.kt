//package com.technource.android.module.miscModule.miscscreen.Body.ui.screens
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.LazyRow
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
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun DashboardScreen(navController: NavController, viewModel: MainViewModel) {
//    val todayStats by viewModel.todayStats.collectAsState()
//    val recentActivities by viewModel.recentActivities.collectAsState()
//
//    LazyColumn(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        verticalArrangement = Arrangement.spacedBy(16.dp)
//    ) {
//        item {
//            Text(
//                text = "Good Morning, John!",
//                style = MaterialTheme.typography.headlineMedium,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = "Here's your health overview for today",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//
//        item {
//            QuickStatsRow(todayStats, viewModel)
//        }
//
//        item {
//            QuickActionsSection(navController, viewModel)
//        }
//
//        item {
//            RecentActivitiesSection(recentActivities)
//        }
//
//        item {
//            HealthRemindersSection(viewModel)
//        }
//    }
//}
//
//@Composable
//fun QuickStatsRow(stats: TodayStats, viewModel: MainViewModel) {
//    LazyRow(
//        horizontalArrangement = Arrangement.spacedBy(12.dp)
//    ) {
//        items(stats.getStatsList()) { stat ->
//            StatCard(stat = stat)
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun StatCard(stat: StatItem) {
//    Card(
//        modifier = Modifier.width(120.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(12.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                imageVector = stat.icon,
//                contentDescription = stat.title,
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(24.dp)
//            )
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = stat.value,
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold
//            )
//            Text(
//                text = stat.title,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//@Composable
//fun QuickActionsSection(navController: NavController, viewModel: MainViewModel) {
//    Column {
//        Text(
//            text = "Quick Actions",
//            style = MaterialTheme.typography.titleLarge,
//            fontWeight = FontWeight.Medium,
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        LazyRow(
//            horizontalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            items(getQuickDashboardActions()) { action ->
//                QuickActionButton(
//                    action = action,
//                    onClick = { action.onClick(navController, viewModel) }
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun QuickActionButton(
//    action: DashboardAction,
//    onClick: () -> Unit
//) {
//    Card(
//        onClick = onClick,
//        modifier = Modifier.size(80.dp)
//    ) {
//        Column(
//            modifier = Modifier.fillMaxSize(),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            Icon(
//                imageVector = action.icon,
//                contentDescription = action.title,
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(24.dp)
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Text(
//                text = action.title,
//                style = MaterialTheme.typography.labelSmall,
//                maxLines = 2
//            )
//        }
//    }
//}
//
//@Composable
//fun RecentActivitiesSection(activities: List<RecentActivity>) {
//    Column {
//        Text(
//            text = "Recent Activities",
//            style = MaterialTheme.typography.titleLarge,
//            fontWeight = FontWeight.Medium,
//            modifier = Modifier.padding(bottom = 8.dp)
//        )
//
//        activities.take(5).forEach { activity ->
//            ActivityItem(activity = activity)
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ActivityItem(activity: RecentActivity) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 2.dp)
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = activity.icon,
//                contentDescription = activity.type,
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(20.dp)
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = activity.title,
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Medium
//                )
//                Text(
//                    text = activity.subtitle,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Text(
//                text = activity.time,
//                style = MaterialTheme.typography.bodySmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//@Composable
//fun HealthRemindersSection(viewModel: MainViewModel) {
//    val reminders by viewModel.healthReminders.collectAsState()
//
//    if (reminders.isNotEmpty()) {
//        Column {
//            Text(
//                text = "Health Reminders",
//                style = MaterialTheme.typography.titleLarge,
//                fontWeight = FontWeight.Medium,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//
//            reminders.forEach { reminder ->
//                ReminderCard(
//                    reminder = reminder,
//                    onDismiss = { viewModel.dismissReminder(reminder.id) },
//                    onAction = { viewModel.executeReminderAction(reminder.id) }
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ReminderCard(
//    reminder: HealthReminder,
//    onDismiss: () -> Unit,
//    onAction: () -> Unit
//) {
//    Card(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 2.dp),
//        colors = CardDefaults.cardColors(
//            containerColor = MaterialTheme.colorScheme.primaryContainer
//        )
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(12.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = reminder.icon,
//                contentDescription = reminder.title,
//                tint = MaterialTheme.colorScheme.onPrimaryContainer,
//                modifier = Modifier.size(20.dp)
//            )
//
//            Spacer(modifier = Modifier.width(12.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = reminder.title,
//                    style = MaterialTheme.typography.bodyMedium,
//                    fontWeight = FontWeight.Medium,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer
//                )
//                Text(
//                    text = reminder.message,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
//                )
//            }
//
//            Row {
//                TextButton(onClick = onDismiss) {
//                    Text("Dismiss")
//                }
//                TextButton(onClick = onAction) {
//                    Text("Do It")
//                }
//            }
//        }
//    }
//}
//
//// Data classes and helper functions
//data class StatItem(
//    val title: String,
//    val value: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector
//)
//
//data class TodayStats(
//    val steps: Int = 0,
//    val calories: Int = 0,
//    val water: Int = 0,
//    val workouts: Int = 0,
//    val weight: Float = 0f
//) {
//    fun getStatsList(): List<StatItem> = listOf(
//        StatItem("Steps", "$steps", Icons.Default.DirectionsWalk),
//        StatItem("Calories", "$calories", Icons.Default.LocalFireDepartment),
//        StatItem("Water", "${water}L", Icons.Default.LocalDrink),
//        StatItem("Workouts", "$workouts", Icons.Default.FitnessCenter),
//        StatItem("Weight", "${weight}kg", Icons.Default.Scale)
//    )
//}
//
//data class DashboardAction(
//    val title: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector,
//    val onClick: (NavController, MainViewModel) -> Unit
//)
//
//data class RecentActivity(
//    val title: String,
//    val subtitle: String,
//    val time: String,
//    val type: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector
//)
//
//data class HealthReminder(
//    val id: String,
//    val title: String,
//    val message: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector,
//    val actionType: String
//)
//
//fun getQuickDashboardActions(): List<DashboardAction> = listOf(
//    DashboardAction("Water", Icons.Default.LocalDrink) { _, viewModel ->
//        viewModel.logWater()
//    },
//    DashboardAction("Photo", Icons.Default.CameraAlt) { navController, _ ->
//        navController.navigate("camera")
//    },
//    DashboardAction("Weight", Icons.Default.Scale) { _, viewModel ->
//        viewModel.logWeight()
//    },
//    DashboardAction("Meal", Icons.Default.Restaurant) { _, viewModel ->
//        viewModel.quickMealLog()
//    },
//    DashboardAction("Workout", Icons.Default.Timer) { _, viewModel ->
//        viewModel.startWorkout()
//    }
//)
