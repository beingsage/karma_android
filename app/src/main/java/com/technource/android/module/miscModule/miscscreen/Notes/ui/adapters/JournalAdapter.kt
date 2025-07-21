package com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.mindkeep.data.model.JournalEntry
import com.technource.android.databinding.ItemJournalEntryBinding
import java.text.SimpleDateFormat
import java.util.*

class JournalAdapter(
    private val onJournalClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(JournalDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JournalViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class JournalViewHolder(
        private val binding: ItemJournalEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(journalEntry: JournalEntry) {
            binding.apply {
                journalDate.text = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(journalEntry.createdAt)
                journalTitle.text = journalEntry.title
                journalPreview.text = journalEntry.content
                journalTemplateType.text = journalEntry.templateType
                
                // Handle attachment
                if (journalEntry.attachmentPath != null) {
                    journalAttachment.visibility = android.view.View.VISIBLE
                    Glide.with(itemView.context)
                        .load(journalEntry.attachmentPath)
                        .into(journalAttachment)
                } else {
                    journalAttachment.visibility = android.view.View.GONE
                }
                
                // Handle tags
                journalTagsChipGroup.removeAllViews()
                journalEntry.tags.forEach { tag ->
                    val chip = Chip(itemView.context)
                    chip.text = tag
                    journalTagsChipGroup.addView(chip)
                }
                
                root.setOnClickListener { onJournalClick(journalEntry) }
            }
        }
    }
    
    private class JournalDiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}