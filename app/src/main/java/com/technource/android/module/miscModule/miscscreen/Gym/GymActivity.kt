package com.technource.android.module.miscModule.miscscreen.Gym

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Gym.fragments.PersonalRecordsFragment
import com.technource.android.module.miscModule.miscscreen.Gym.fragments.HistoryFragment
import com.technource.android.module.miscModule.miscscreen.Gym.fragments.SettingsFragment
import com.technource.android.module.miscModule.miscscreen.Gym.fragments.StatsFragment
import com.technource.android.module.miscModule.miscscreen.Gym.fragments.WorkoutFragment
import com.technource.android.module.miscModule.miscscreen.Gym.utils.SoundManager
import com.technource.android.module.miscModule.miscscreen.Gym.utils.VoiceCommandManager
import com.technource.android.module.miscModule.miscscreen.Gym.utils.WorkoutManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GymActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var workoutManager: WorkoutManager
    private lateinit var soundManager: SoundManager
    private lateinit var voiceCommandManager: VoiceCommandManager
    private lateinit var notificationManager: NotificationManager

    private val RECORD_AUDIO_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gym)

        initializeManagers()
        setupViewPager()
        requestPermissions()
    }

    private fun initializeManagers() {
        workoutManager = WorkoutManager(this)
        soundManager = SoundManager(this)
        voiceCommandManager = VoiceCommandManager(this) { command ->
                handleVoiceCommand(command)
        }
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun setupViewPager() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
            0 -> "Workout"
            1 -> "History"
            2 -> "Stats"
            3 -> "PRs"
            4 -> "Settings"
                else -> ""
        }
        }.attach()
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_CODE
            )
        }
    }

    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("start") -> workoutManager.startWorkout()
            command.contains("pause") -> workoutManager.pauseWorkout()
            command.contains("complete set") -> workoutManager.completeCurrentSet()
            command.contains("next exercise") -> workoutManager.nextExercise()
        }
    }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 5

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WorkoutFragment()
                1 -> HistoryFragment()
                2 -> StatsFragment()
                3 -> PersonalRecordsFragment()
                4 -> SettingsFragment()
                else -> WorkoutFragment()
            }
        }
    }
}
