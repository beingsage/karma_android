package com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mindkeep.data.model.Note
import com.technource.android.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(note: Note) {
            binding.apply {
                noteTitle.text = note.title
                noteContent.text = note.content
                noteDate.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(note.createdAt)
                
                // Set note color
                noteCardView.setCardBackgroundColor(note.color)
                
                // Handle attachment
                if (note.attachmentPath != null) {
                    noteAttachment.visibility = android.view.View.VISIBLE
                    Glide.with(itemView.context)
                        .load(note.attachmentPath)
                        .into(noteAttachment)
                } else {
                    noteAttachment.visibility = android.view.View.GONE
                }
                
                // Handle tags
                tagsChipGroup.removeAllViews()
                note.tags.forEach { tag ->
                    val chip = com.google.android.material.chip.Chip(itemView.context)
                    chip.text = tag
                    tagsChipGroup.addView(chip)
                }
                
                // Handle reminder
                reminderIcon.visibility = if (note.reminderTime != null) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
                
                root.setOnClickListener { onNoteClick(note) }
            }
        }
    }
    
    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }
    }
}
