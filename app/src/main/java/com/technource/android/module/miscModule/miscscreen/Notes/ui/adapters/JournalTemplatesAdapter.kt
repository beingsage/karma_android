package com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.databinding.ItemJournalTemplateBinding
import com.technource.android.module.miscModule.miscscreen.Notes.utils.JournalTemplates

class JournalTemplatesAdapter(
    private val onTemplateClick: (JournalTemplates.JournalTemplate) -> Unit
) : ListAdapter<JournalTemplates.JournalTemplate, JournalTemplatesAdapter.TemplateViewHolder>(TemplateDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemJournalTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class TemplateViewHolder(
        private val binding: ItemJournalTemplateBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(template: JournalTemplates.JournalTemplate) {
            binding.apply {
                templateTitle.text = template.name
                templateDescription.text = template.description
                templateIcon.setImageResource(template.iconRes)
                
                useTemplateButton.setOnClickListener { onTemplateClick(template) }
                root.setOnClickListener { onTemplateClick(template) }
            }
        }
    }
    
    private class TemplateDiffCallback : DiffUtil.ItemCallback<JournalTemplates.JournalTemplate>() {
        override fun areItemsTheSame(oldItem: JournalTemplates.JournalTemplate, newItem: JournalTemplates.JournalTemplate): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: JournalTemplates.JournalTemplate, newItem: JournalTemplates.JournalTemplate): Boolean {
            return oldItem == newItem
        }
    }
}