package com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.technource.android.databinding.FragmentRemindersBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.CreateReminderActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters.RemindersAdapter
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.ReminderViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.ReminderViewModelFactory
import kotlinx.coroutines.launch

class RemindersFragment : Fragment() {
    
    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var remindersAdapter: RemindersAdapter
    
    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory((requireActivity().application as NotesActivity).reminderRepository)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupTabLayout()
        observeReminders()
    }
    
    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter(
            onReminderClick = { reminder ->
                val intent = Intent(requireContext(), CreateReminderActivity::class.java)
                intent.putExtra("REMINDER_ID", reminder.id)
                startActivity(intent)
            },
            onReminderComplete = { reminder ->
                viewModel.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
            }
        )
        
        binding.remindersRecyclerView.apply {
            adapter = remindersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun setupTabLayout() {
        binding.reminderTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> observeUpcomingReminders()
                    1 -> observeTodayReminders()
                    2 -> observeCompletedReminders()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun observeReminders() {
        observeUpcomingReminders() // Default to upcoming
    }
    
    private fun observeUpcomingReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeReminders.collect { reminders ->
                remindersAdapter.submitList(reminders)
                binding.emptyRemindersText.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun observeTodayReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.todayReminders.collect { reminders ->
                remindersAdapter.submitList(reminders)
                binding.emptyRemindersText.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun observeCompletedReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.completedReminders.collect { reminders ->
                remindersAdapter.submitList(reminders)
                binding.emptyRemindersText.visibility = if (reminders.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}