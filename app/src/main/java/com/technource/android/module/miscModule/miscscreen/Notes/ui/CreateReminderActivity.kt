package com.technource.android.module.miscModule.miscscreen.Notes.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mindkeep.data.model.Priority
import com.mindkeep.data.model.Reminder
import com.technource.android.R
import com.technource.android.databinding.ActivityCreateReminderBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.ReminderViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.ReminderViewModelFactory
import com.technource.android.module.miscModule.miscscreen.Notes.utils.NotificationScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateReminderActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateReminderBinding
    private var selectedDateTime: Date? = null
    private var selectedPriority: Priority = Priority.MEDIUM
    private var selectedCategory: String = "General"
    private var reminderId: Long = 0L
    private var isEditMode = false
    
    private val viewModel: ReminderViewModel by viewModels {
        ReminderViewModelFactory((application as NotesActivity).reminderRepository)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateReminderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
        checkEditMode()
        setupPrioritySelector()
        setupCategorySelector()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Reminder" else "Create Reminder"
    }
    
    private fun checkEditMode() {
        reminderId = intent.getLongExtra("REMINDER_ID", 0L)
        if (reminderId != 0L) {
            isEditMode = true
            loadReminderData()
        }
    }
    
    private fun loadReminderData() {
        lifecycleScope.launch {
            // Load reminder data for editing
        }
    }
    
    private fun setupClickListeners() {
        binding.selectDateTimeButton.setOnClickListener {
            showDateTimePicker()
        }
        
        binding.quickTimeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chip_1_hour -> setQuickTime(1, Calendar.HOUR_OF_DAY)
                R.id.chip_tomorrow -> setQuickTime(1, Calendar.DAY_OF_MONTH)
                R.id.chip_next_week -> setQuickTime(1, Calendar.WEEK_OF_YEAR)
                R.id.chip_custom -> showDateTimePicker()
            }
        }
        
        binding.repeatSwitch.setOnCheckedChangeListener { _, isChecked ->
            binding.repeatOptionsGroup.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
    
    private fun setupPrioritySelector() {
        binding.priorityChipGroup.setOnCheckedChangeListener { _, checkedId ->
            selectedPriority = when (checkedId) {
                R.id.chip_priority_low -> Priority.LOW
                R.id.chip_priority_medium -> Priority.MEDIUM
                R.id.chip_priority_high -> Priority.HIGH
                else -> Priority.MEDIUM
            }
            updatePriorityColors()
        }
    }
    
    private fun setupCategorySelector() {
        binding.categorySpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val categories = resources.getStringArray(R.array.reminder_categories)
                selectedCategory = categories[position]
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }
    
    private fun updatePriorityColors() {
        val color = when (selectedPriority) {
            Priority.HIGH -> getColor(R.color.priority_high)
            Priority.MEDIUM -> getColor(R.color.priority_medium)
            Priority.LOW -> getColor(R.color.priority_low)
        }
        binding.priorityIndicator.setBackgroundColor(color)
    }
    
    private fun setQuickTime(amount: Int, field: Int) {
        val calendar = Calendar.getInstance()
        calendar.add(field, amount)
        selectedDateTime = calendar.time
        updateDateTimeDisplay()
    }
    
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                selectedDateTime = calendar.time
                updateDateTimeDisplay()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun updateDateTimeDisplay() {
        selectedDateTime?.let { dateTime ->
            val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            binding.selectedDateTimeText.text = format.format(dateTime)
            binding.selectedDateTimeText.visibility = android.view.View.VISIBLE
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_reminder, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveReminder()
                true
            }
            R.id.action_delete -> {
                if (isEditMode) deleteReminder()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveReminder() {
        val title = binding.reminderTitleEditText.text.toString().trim()
        val description = binding.reminderDescriptionEditText.text.toString().trim()
        
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (selectedDateTime == null) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show()
            return
        }
        
        val reminder = Reminder(
            id = if (isEditMode) reminderId else 0,
            title = title,
            description = description,
            dateTime = selectedDateTime!!,
            priority = selectedPriority,
            category = selectedCategory,
            createdAt = if (isEditMode) Date() else Date(),
            updatedAt = Date()
        )
        
        lifecycleScope.launch {
            val savedReminderId = if (isEditMode) {
                viewModel.updateReminder(reminder)
                reminderId
            } else {
                viewModel.insertReminder(reminder)
            }
            
            // Schedule notification
            NotificationScheduler.scheduleReminder(this@CreateReminderActivity, reminder.copy(id = savedReminderId))
            
            runOnUiThread {
                Toast.makeText(this@CreateReminderActivity, "Reminder saved", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun deleteReminder() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Reminder")
            .setMessage("Are you sure you want to delete this reminder?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    // Delete reminder logic
                    NotificationScheduler.cancelReminder(this@CreateReminderActivity, reminderId.toInt())
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}