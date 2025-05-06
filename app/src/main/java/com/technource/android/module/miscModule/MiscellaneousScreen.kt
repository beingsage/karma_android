//package com.technource.android.ui.appscreen
//
//import android.app.Activity
//import android.content.Intent
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyRow
//import androidx.compose.foundation.lazy.items
//import androidx.compose.material3.*
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import com.technource.android.R
//import com.technource.android.ui.miscscreen.Body.BodyActivity
//import com.technource.android.ui.miscscreen.Calendar.CalendarActivity
//import com.technource.android.ui.miscscreen.Diet.DietActivity
//import com.technource.android.ui.miscscreen.English.EnglishActivity
//import com.technource.android.ui.miscscreen.Finance.FinanceActivity
//import com.technource.android.ui.miscscreen.Gym.GymActivity
//import com.technource.android.ui.miscscreen.Habits.HabitsActivity
//import com.technource.android.ui.miscscreen.Journals.JournalsActivity
//import com.technource.android.ui.miscscreen.Meditation.MeditationActivity
//import com.technource.android.ui.miscscreen.Neuroplasticity.NeuroplasticityActivity
//import com.technource.android.ui.miscscreen.Notes.NotesActivity
//import com.technource.android.ui.miscscreen.Projects.ProjectsActivity
//import com.technource.android.ui.miscscreen.Quants.QuantsActivity
//import com.technource.android.ui.miscscreen.Reminders.RemindersActivity
//import com.technource.android.ui.miscscreen.Snaps.SnapsActivity
//import com.technource.android.ui.miscscreen.Spotify.SpotifyActivity
//import com.technource.android.ui.miscscreen.iCBT.iCBTActivity
//
//data class CategoryItem(
//    val name: String,
//    val iconRes: Int,
//    val category: String,
//    val activityClass: Class<out Activity>? = null // Make nullable
//)
//
//@Composable
//fun MiscellaneousScreen(navController: NavController) {
//    Surface(modifier = Modifier.fillMaxSize()) {
//        Column(
//            modifier = Modifier.padding(16.dp),
//            verticalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            CategorySection("Health & Wellness", getHealthWellnessItems(), navController)
////            CategorySection("Productivity", getProductivityItems(), navController)
////            CategorySection("Finance", getFinanceItems(), navController)
////            CategorySection("Personal Development", getPersonalDevelopmentItems(), navController)
////            CategorySection("Entertainment", getEntertainmentItems(), navController)
//        }
//    }
//}
//
//
//@Composable
//fun CategorySection(title: String, items: List<CategoryItem>, navController: NavController) {
//    val context = LocalContext.current
//
//    Column {
//        Text(text = title, style = MaterialTheme.typography.headlineSmall)
//        LazyRow {
//            items(items) { item ->
//                CategoryCard(item)
//                    when (item.name) {
//                        // Health & Wellness
//                        "Meditation" -> context.startActivity(Intent(context, MeditationActivity::class.java))
////                        "Gym" -> context.startActivity(Intent(context, GymActivity::class.java))
////                        "Diet" -> context.startActivity(Intent(context, DietActivity::class.java))
////                        "Body" -> context.startActivity(Intent(context, BodyActivity::class.java))
////                        "Habits" -> context.startActivity(Intent(context, HabitsActivity::class.java))
////
////                        // Productivity
////                        "Projects" -> context.startActivity(Intent(context, ProjectsActivity::class.java))
////                        "Notes" -> context.startActivity(Intent(context, NotesActivity::class.java))
////                        "Journals" -> context.startActivity(Intent(context, JournalsActivity::class.java))
////                        "Reminders" -> context.startActivity(Intent(context, RemindersActivity::class.java))
////                        "Calendar" -> context.startActivity(Intent(context, CalendarActivity::class.java))
////
////                        // Finance
////                        "Finance" -> context.startActivity(Intent(context, FinanceActivity::class.java))
////                        "Quants" -> context.startActivity(Intent(context, QuantsActivity::class.java))
////
////                        // Personal Development
////                        "Neuroplasticity" -> context.startActivity(Intent(context, NeuroplasticityActivity::class.java))
////                        "iCBT" -> context.startActivity(Intent(context, iCBTActivity::class.java))
////                        "English" -> context.startActivity(Intent(context, EnglishActivity::class.java))
////
////                        // Entertainment
////                        "Spotify" -> context.startActivity(Intent(context, SpotifyActivity::class.java))
////                        "Snaps" -> context.startActivity(Intent(context, SnapsActivity::class.java))
//
//                        // Default case
//                        else -> navController.navigate("detail/${item.category}/${item.name}")
//                    }
//                }
//            }
//        }
//    }
//
//
//
//
//
//
//@Composable
//fun CategoryCard(item: CategoryItem) {
//    val context = LocalContext.current
//    Card(
//        modifier = Modifier
//            .width(120.dp)
//            .clickable {
//                val intent = Intent(context, item.activityClass)
//                context.startActivity(intent)
//            },
//        elevation = CardDefaults.cardElevation(4.dp)
//    ) {
//        Column(
//            modifier = Modifier.padding(8.dp),
//            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
//        ) {
//            Image(
//                painter = painterResource(id = item.iconRes),
//                contentDescription = item.name,
//                modifier = Modifier.size(48.dp)
//            )
//            Text(
//                text = item.name,
//                style = MaterialTheme.typography.bodyMedium
//            )
//        }
//    }
//}
//
//
//// Helper functions to get category items
//private fun getHealthWellnessItems() = listOf(
////    CategoryItem("Gym", R.drawable.ic_gym, "health" ),
//    CategoryItem("Meditation", R.drawable.ic_meditation, "health" ),
////    CategoryItem("Diet", R.drawable.ic_diet, "health" ),
////    CategoryItem("Body", R.drawable.ic_body, "health"),
////    CategoryItem("Habits", R.drawable.ic_habits, "health")
//)
//
////// Add similar functions for other categories...
////private fun getProductivityItems() = listOf(
////    CategoryItem("Projects", R.drawable.ic_projects, "productivity"),
////    CategoryItem("Notes", R.drawable.ic_notes, "productivity"),
////    CategoryItem("Journals", R.drawable.ic_journal, "productivity"),
////    CategoryItem("Reminders", R.drawable.ic_reminders, "productivity"),
////    CategoryItem("Calendar", R.drawable.ic_calendar, "productivity")
////)
////
////private fun getFinanceItems() = listOf(
////    CategoryItem("Finance", R.drawable.ic_finance, "finance"),
////    CategoryItem("Quants", R.drawable.ic_quants, "finance")
////)
////
////private fun getPersonalDevelopmentItems() = listOf(
////    CategoryItem("Neuroplasticity", R.drawable.ic_brain, "development"),
////    CategoryItem("iCBT", R.drawable.ic_therapy, "development"),
////    CategoryItem("English", R.drawable.ic_language, "development")
////)
////
////private fun getEntertainmentItems() = listOf(
////    CategoryItem("Spotify", R.drawable.ic_music, "entertainment"),
////    CategoryItem("Snaps", R.drawable.ic_camera, "entertainment")
////)



package com.technource.android.module.miscModule

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.technource.android.R
import com.technource.android.utils.NavigationHelper
import dagger.hilt.android.AndroidEntryPoint
//import old.miscscreen.Meditation.MeditationActivity

@AndroidEntryPoint
class MiscellaneousScreen : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_misc)

        val composeView = findViewById<ComposeView>(R.id.compose_view)
        composeView.setContent {
            MiscellaneousScreenContent()
        }

        bottomNavigation = findViewById(R.id.bottom_navigation)
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }
    override fun onResume() {
        super.onResume()
        NavigationHelper.setupBottomNavigation(this, bottomNavigation)
    }
}




@Composable
fun MiscellaneousScreenContent() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
//            CategorySection("Health & Wellness", getHealthWellnessItems())
        }
    }
}

@Composable
fun CategorySection(title: String, items: List<CategoryItem>) {
    val context = LocalContext.current

    Column {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        LazyRow {
            items(items) { item ->
                CategoryCard(item) {
                    when (item.name) {
//                        "Meditation" -> context.startActivity(Intent(context, MeditationActivity::class.java))
                        else -> {
                            // Handle other cases or navigation if needed
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryCard(item: CategoryItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.name,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

data class CategoryItem(
    val name: String,
    val iconRes: Int,
    val category: String,
    val activityClass: Class<out ComponentActivity>? = null
)


//private fun getHealthWellnessItems() = listOf(
//    CategoryItem("Meditation", R.drawable.ic_meditation, "health", MeditationActivity::class.java)
    // Add other items as needed
//)



// make all misc screen components a composable