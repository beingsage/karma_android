//package com.technource.android.module.miscModule.miscscreen.Body
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.layout.*
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.CameraScreen
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.DashboardScreen
//import com.technource.android.module.miscModule.miscscreen.Body.ui.screens.QuickLogScreen
//import com.technource.android.module.miscModule.miscscreen.Body.ui.viewmodels.MainViewModel
//import dagger.hilt.android.AndroidEntryPoint
//
//@AndroidEntryPoint
//class BodyActivity : ComponentActivity() {
//
//    private val requestPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestMultiplePermissions()
//    ) { permissions ->
//        // Handle permission results
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        // Request necessary permissions
//        requestPermissions()
//
////        setContent {
////            BodyTrackTheme {
////                MainApp()
////            }
////        }
//    }
//
//    private fun requestPermissions() {
//        val permissions = arrayOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.BODY_SENSORS,
//            Manifest.permission.ACTIVITY_RECOGNITION,
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.RECORD_AUDIO
//        )
//
//        val permissionsToRequest = permissions.filter {
//            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
//        }
//
//        if (permissionsToRequest.isNotEmpty()) {
//            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun MainApp() {
//    val navController = rememberNavController()
//    val viewModel: MainViewModel = viewModel()
//
//    Scaffold(
//        bottomBar = { BottomNavigationBar(navController) },
//        floatingActionButton = { QuickActionFAB(navController) }
//    ) { paddingValues ->
//        NavHost(
//            navController = navController,
//            startDestination = "dashboard",
//            modifier = Modifier.padding(paddingValues)
//        ) {
//            composable("dashboard") { DashboardScreen(navController, viewModel) }
//            composable("quick_log") { QuickLogScreen(navController, viewModel) }
//            composable("camera") { CameraScreen(navController, viewModel) }
////            composable("nutrition") { NutritionScreen(navController, viewModel) }
////            composable("exercise") { ExerciseScreen(navController, viewModel) }
////            composable("health") { HealthScreen(navController, viewModel) }
////            composable("progress") { ProgressScreen(navController, viewModel) }
//        }
//    }
//}
//
//@Composable
//fun BottomNavigationBar(navController: NavHostController) {
//    NavigationBar {
//        val items = listOf(
//            "dashboard" to Icons.Default.Home,
//            "nutrition" to Icons.Default.Restaurant,
//            "exercise" to Icons.Default.FitnessCenter,
//            "health" to Icons.Default.Favorite,
//            "progress" to Icons.Default.TrendingUp
//        )
//
//        items.forEach { (route, icon) ->
//            NavigationBarItem(
//                icon = { Icon(icon, contentDescription = route) },
//                label = { Text(route.capitalize()) },
//                selected = false, // You'd implement proper selection logic here
//                onClick = { navController.navigate(route) }
//            )
//        }
//    }
//}
//
//@Composable
//fun QuickActionFAB(navController: NavHostController) {
//    var expanded by remember { mutableStateOf(false) }
//
//    Column(
//        horizontalAlignment = Alignment.End
//    ) {
//        if (expanded) {
//            FloatingActionButton(
//                onClick = { navController.navigate("camera") },
//                modifier = Modifier.padding(bottom = 8.dp),
//                containerColor = MaterialTheme.colorScheme.secondary
//            ) {
//                Icon(Icons.Default.CameraAlt, contentDescription = "Camera")
//            }
//
//            FloatingActionButton(
//                onClick = { navController.navigate("quick_log") },
//                modifier = Modifier.padding(bottom = 8.dp),
//                containerColor = MaterialTheme.colorScheme.secondary
//            ) {
//                Icon(Icons.Default.Add, contentDescription = "Quick Log")
//            }
//        }
//
//        FloatingActionButton(
//            onClick = { expanded = !expanded }
//        ) {
//            Icon(
//                if (expanded) Icons.Default.Close else Icons.Default.Add,
//                contentDescription = "Quick Actions"
//            )
//        }
//    }
//}