package com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.databinding.ItemCategoryBinding
import com.technource.android.module.miscModule.miscscreen.Notes.utils.CategoryItem

class CategoriesAdapter(
    private val onCategoryClick: (CategoryItem) -> Unit
) : ListAdapter<CategoryItem, CategoriesAdapter.CategoryViewHolder>(CategoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class CategoryViewHolder(
        private val binding: ItemCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(category: CategoryItem) {
            binding.apply {
                categoryName.text = category.name
                categoryCount.text = "${category.count} items"
                categoryIcon.setImageResource(category.iconRes)
                categoryCard.setCardBackgroundColor(category.color)
                
                root.setOnClickListener { onCategoryClick(category) }
            }
        }
    }
    
    private class CategoryDiffCallback : DiffUtil.ItemCallback<CategoryItem>() {
        override fun areItemsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem.name == newItem.name
        }
        
        override fun areContentsTheSame(oldItem: CategoryItem, newItem: CategoryItem): Boolean {
            return oldItem == newItem
        }
    }
}