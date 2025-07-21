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
//fun QuickLogScreen(navController: NavController, viewModel: MainViewModel) {
//    var selectedCategory by remember { mutableStateOf("All") }
//
//    Column(
//        modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)
//    ) {
//        Text(
//            text = "Quick Log",
//            style = MaterialTheme.typography.headlineMedium,
//            fontWeight = FontWeight.Bold,
//            modifier = Modifier.padding(bottom = 16.dp)
//        )
//
//        // Category Filter
//        LazyRow(
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
//            modifier = Modifier.padding(bottom = 16.dp)
//        ) {
//            val categories = listOf("All", "Nutrition", "Exercise", "Health", "Progress")
//            items(categories) { category ->
//                FilterChip(
//                    onClick = { selectedCategory = category },
//                    label = { Text(category) },
//                    selected = selectedCategory == category
//                )
//            }
//        }
//
//        // Quick Actions Grid
//        LazyColumn(
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            items(getQuickActions(selectedCategory)) { action ->
//                QuickActionCard(
//                    action = action,
//                    onClick = { action.onClick(viewModel) }
//                )
//            }
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun QuickActionCard(
//    action: QuickAction,
//    onClick: () -> Unit
//) {
//    Card(
//        onClick = onClick,
//        modifier = Modifier.fillMaxWidth()
//    ) {
//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(16.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Icon(
//                imageVector = action.icon,
//                contentDescription = action.title,
//                tint = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.size(24.dp)
//            )
//
//            Spacer(modifier = Modifier.width(16.dp))
//
//            Column(modifier = Modifier.weight(1f)) {
//                Text(
//                    text = action.title,
//                    style = MaterialTheme.typography.titleMedium,
//                    fontWeight = FontWeight.Medium
//                )
//                Text(
//                    text = action.description,
//                    style = MaterialTheme.typography.bodyMedium,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//
//            Icon(
//                imageVector = Icons.Default.ChevronRight,
//                contentDescription = "Go",
//                tint = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
//    }
//}
//
//data class QuickAction(
//    val title: String,
//    val description: String,
//    val icon: androidx.compose.ui.graphics.vector.ImageVector,
//    val category: String,
//    val onClick: (MainViewModel) -> Unit
//)
//
//fun getQuickActions(selectedCategory: String): List<QuickAction> {
//    val allActions = listOf(
//        QuickAction(
//            title = "Log Water",
//            description = "Quick water intake logging",
//            icon = Icons.Default.LocalDrink,
//            category = "Nutrition"
//        ) { viewModel -> viewModel.logWater() },
//
//        QuickAction(
//            title = "Scan Food",
//            description = "Barcode scan for nutrition",
//            icon = Icons.Default.QrCodeScanner,
//            category = "Nutrition"
//        ) { viewModel -> viewModel.openBarcodeScanner() },
//
//        QuickAction(
//            title = "Quick Meal",
//            description = "Log meal with photo",
//            icon = Icons.Default.Restaurant,
//            category = "Nutrition"
//        ) { viewModel -> viewModel.quickMealLog() },
//
//        QuickAction(
//            title = "Log Weight",
//            description = "Record current weight",
//            icon = Icons.Default.Scale,
//            category = "Health"
//        ) { viewModel -> viewModel.logWeight() },
//
//        QuickAction(
//            title = "Progress Photo",
//            description = "Take progress picture",
//            icon = Icons.Default.CameraAlt,
//            category = "Progress"
//        ) { viewModel -> viewModel.takeProgressPhoto() },
//
//        QuickAction(
//            title = "Quick Workout",
//            description = "Start workout timer",
//            icon = Icons.Default.Timer,
//            category = "Exercise"
//        ) { viewModel -> viewModel.startWorkout() },
//
//        QuickAction(
//            title = "Heart Rate",
//            description = "Measure heart rate",
//            icon = Icons.Default.Favorite,
//            category = "Health"
//        ) { viewModel -> viewModel.measureHeartRate() },
//
//        QuickAction(
//            title = "Mood Check",
//            description = "Log current mood",
//            icon = Icons.Default.Mood,
//            category = "Health"
//        ) { viewModel -> viewModel.logMood() },
//
//        QuickAction(
//            title = "Sleep Log",
//            description = "Record sleep data",
//            icon = Icons.Default.Bedtime,
//            category = "Health"
//        ) { viewModel -> viewModel.logSleep() },
//
//        QuickAction(
//            title = "Body Measurements",
//            description = "Log body measurements",
//            icon = Icons.Default.Straighten,
//            category = "Progress"
//        ) { viewModel -> viewModel.logMeasurements() }
//    )
//
//    return if (selectedCategory == "All") {
//        allActions
//    } else {
//        allActions.filter { it.category == selectedCategory }
//    }
//}
