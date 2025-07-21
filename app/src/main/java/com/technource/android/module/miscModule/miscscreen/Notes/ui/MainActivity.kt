package com.technource.android.module.miscModule.miscscreen.Notes.ui

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.technource.android.databinding.ActivityGymBinding
import com.technource.android.databinding.ActivityNotesBinding
import com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments.CategoriesFragment
import com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments.JournalFragment
import com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments.NotesFragment
import com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments.RemindersFragment

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityNotesBinding
    
    private val voiceCommandLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.let { processVoiceCommand(it[0]) }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewPager()
        setupClickListeners()
    }
    
    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Notes"
                1 -> "Journal"
                2 -> "Reminders"
                3 -> "Categories"
                else -> ""
            }
        }.attach()
    }
    
    private fun setupClickListeners() {
        binding.fab.setOnClickListener {
            when (binding.viewPager.currentItem) {
                0 -> startActivity(Intent(this, CreateNoteActivity::class.java))
                1 -> startActivity(Intent(this, CreateJournalActivity::class.java))
                2 -> startActivity(Intent(this, CreateReminderActivity::class.java))
            }
        }
        
        binding.voiceCommandButton.setOnClickListener {
            startVoiceCommand()
        }
        
        binding.searchButton.setOnClickListener {
            // Toggle search functionality
            toggleSearch()
        }
    }
    
    private fun startVoiceCommand() {
        val intent = Intent(this, VoiceCommandActivity::class.java)
        startActivity(intent)
    }
    
    private fun toggleSearch() {
        // Implementation for search toggle
    }
    
    private fun processVoiceCommand(command: String) {
        // Process voice command and create appropriate note/reminder
    }
    
    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4
        
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> NotesFragment()
                1 -> JournalFragment()
                2 -> RemindersFragment()
                3 -> CategoriesFragment()
                else -> NotesFragment()
            }
        }
    }
}
