package com.technource.android.module.miscModule.miscscreen.Notes.ui.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.technource.android.databinding.FragmentNotesBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters.NotesAdapter
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.NotesViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.NotesViewModelFactory
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notesAdapter: NotesAdapter
    
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory((requireActivity().application as NotesActivity).notesRepository)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeNotes()
        setupSearch()
    }
    
    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter { note ->
            // Handle note click
        }
        
        binding.notesRecyclerView.apply {
            adapter = notesAdapter
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        }
    }
    
    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allNotes.collect { notes ->
                notesAdapter.submitList(notes)
                binding.emptyNotesText.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                s?.let { viewModel.searchNotes(it.toString()) }
            }
        })
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
