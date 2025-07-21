package com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.technource.android.databinding.FragmentCategoriesBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters.CategoriesAdapter
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.CategoriesViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.CategoriesViewModelFactory
import kotlinx.coroutines.launch

class CategoriesFragment : Fragment() {
    
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var categoriesAdapter: CategoriesAdapter
    
    private val viewModel: CategoriesViewModel by viewModels {
        CategoriesViewModelFactory((requireActivity().application as NotesActivity).notesRepository)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeCategories()
    }
    
    private fun setupRecyclerView() {
        categoriesAdapter = CategoriesAdapter { category ->
            // Navigate to category-specific notes
            // This would typically use Navigation Component
        }
        
        binding.categoriesRecyclerView.apply {
            adapter = categoriesAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }
    
    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.categories.collect { categories ->
                categoriesAdapter.submitList(categories)
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}