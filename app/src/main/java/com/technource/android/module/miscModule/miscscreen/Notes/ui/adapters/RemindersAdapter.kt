package com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mindkeep.data.model.Priority
import com.mindkeep.data.model.Reminder
import com.technource.android.R
import com.technource.android.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.*

class RemindersAdapter(
    private val onReminderClick: (Reminder) -> Unit,
    private val onReminderComplete: (Reminder) -> Unit
) : ListAdapter<Reminder, RemindersAdapter.ReminderViewHolder>(ReminderDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReminderViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ReminderViewHolder(
        private val binding: ItemReminderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(reminder: Reminder) {
            binding.apply {
                reminderTitle.text = reminder.title
                reminderDescription.text = reminder.description
                reminderCategory.text = reminder.category
                reminderCheckbox.isChecked = reminder.isCompleted
                
                // Format date and time
                val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
                reminderDateTime.text = dateFormat.format(reminder.dateTime)
                
                // Set priority
                reminderPriority.text = reminder.priority.name
                val priorityColor = when (reminder.priority) {
                    Priority.HIGH -> ContextCompat.getColor(itemView.context, R.color.priority_high)
                    Priority.MEDIUM -> ContextCompat.getColor(itemView.context, R.color.priority_medium)
                    Priority.LOW -> ContextCompat.getColor(itemView.context, R.color.priority_low)
                }
                reminderPriority.setBackgroundColor(priorityColor)
                
                // Handle completion
                reminderCheckbox.setOnCheckedChangeListener { _, _ ->
                    onReminderComplete(reminder)
                }
                
                root.setOnClickListener { onReminderClick(reminder) }
            }
        }
    }
    
    private class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {
        override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
            return oldItem == newItem
        }
    }
}