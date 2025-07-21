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
import com.technource.android.databinding.FragmentJournalBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.CreateJournalActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters.JournalAdapter
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.JournalViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.JournalViewModelFactory
import kotlinx.coroutines.launch

class JournalFragment : Fragment() {
    
    private var _binding: FragmentJournalBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var journalAdapter: JournalAdapter
    
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory((requireActivity().application as NotesActivity).journalRepository)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeJournalEntries()
        setupClickListeners()
    }
    
    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter { journalEntry ->
            val intent = Intent(requireContext(), CreateJournalActivity::class.java)
            intent.putExtra("JOURNAL_ID", journalEntry.id)
            startActivity(intent)
        }
        
        binding.journalEntriesRecyclerView.apply {
            adapter = journalAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
    
    private fun observeJournalEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allJournalEntries.collect { entries ->
                journalAdapter.submitList(entries)
                binding.emptyJournalText.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.blankJournalButton.setOnClickListener {
            startActivity(Intent(requireContext(), CreateJournalActivity::class.java))
            binding.templateSelectorCard.visibility = View.GONE
        }
        
        binding.uploadJournalButton.setOnClickListener {
            val intent = Intent(requireContext(), CreateJournalActivity::class.java)
            intent.putExtra("SHOW_UPLOAD", true)
            startActivity(intent)
            binding.templateSelectorCard.visibility = View.GONE
        }
    }
    
    fun showTemplateSelector() {
        binding.templateSelectorCard.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}