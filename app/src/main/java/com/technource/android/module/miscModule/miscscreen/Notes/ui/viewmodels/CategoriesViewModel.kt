package com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mindkeep.data.repository.NotesRepository
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Notes.utils.CategoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel(private val notesRepository: NotesRepository) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<CategoryItem>>(emptyList())
    val categories: StateFlow<List<CategoryItem>> = _categories.asStateFlow()
    
    init {
        loadCategories()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            // In a real app, you'd calculate these from your database
            val categoryList = listOf(
                CategoryItem("Work", 15, R.drawable.ic_work, android.graphics.Color.parseColor("#FF2196F3")),
                CategoryItem("Personal", 8, R.drawable.ic_person, android.graphics.Color.parseColor("#FF4CAF50")),
                CategoryItem("Health", 5, R.drawable.ic_health, android.graphics.Color.parseColor("#FFFF5722")),
                CategoryItem("Finance", 3, R.drawable.ic_money, android.graphics.Color.parseColor("#FFFF9800")),
                CategoryItem("Education", 12, R.drawable.ic_school, android.graphics.Color.parseColor("#FF9C27B0")),
                CategoryItem("Travel", 7, R.drawable.ic_travel, android.graphics.Color.parseColor("#FF00BCD4"))
            )
            _categories.value = categoryList
        }
    }
}

class CategoriesViewModelFactory(private val notesRepository: NotesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(notesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}