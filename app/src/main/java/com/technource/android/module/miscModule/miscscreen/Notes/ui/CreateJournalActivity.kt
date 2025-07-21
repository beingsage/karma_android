package com.technource.android.module.miscModule.miscscreen.Notes.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mindkeep.data.model.JournalEntry
import com.technource.android.R
import com.technource.android.databinding.ActivityCreateJournalBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.adapters.JournalTemplatesAdapter
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.JournalViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.JournalViewModelFactory
import com.technource.android.module.miscModule.miscscreen.Notes.utils.FileUtils
import com.technource.android.module.miscModule.miscscreen.Notes.utils.JournalTemplates
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class CreateJournalActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateJournalBinding
    private var currentPhotoPath: String? = null
    private var selectedTemplate: String = "blank"
    private val tags = mutableListOf<String>()
    private var journalId: Long = 0L
    private var isEditMode = false
    
    private val viewModel: JournalViewModel by viewModels {
        JournalViewModelFactory((application as NotesActivity).journalRepository)
    }
    
    private val documentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleDocumentSelection(it) }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                displayImage(Uri.fromFile(File(path)))
                processHandwrittenJournal(path)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateJournalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupTemplateSelector()
        setupClickListeners()
        checkEditMode()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Journal" else "New Journal Entry"
    }
    
    private fun checkEditMode() {
        journalId = intent.getLongExtra("JOURNAL_ID", 0L)
        if (journalId != 0L) {
            isEditMode = true
            loadJournalData()
        }
    }
    
    private fun loadJournalData() {
        lifecycleScope.launch {
            // Load journal data for editing
        }
    }
    
    private fun setupTemplateSelector() {
        val templates = JournalTemplates.getAllTemplates()
        val adapter = JournalTemplatesAdapter { template ->
            selectedTemplate = template.id
            applyTemplate(template)
            binding.templateSelectorCard.visibility = View.GONE
        }
        
        binding.templatesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.templatesRecyclerView.adapter = adapter
        adapter.submitList(templates)
    }
    
    private fun setupClickListeners() {
        binding.selectTemplateButton.setOnClickListener {
            binding.templateSelectorCard.visibility = android.view.View.VISIBLE
        }
        
        binding.uploadJournalButton.setOnClickListener {
            showUploadOptions()
        }
        
        binding.blankJournalButton.setOnClickListener {
            selectedTemplate = "blank"
            binding.templateSelectorCard.visibility = android.view.View.GONE
        }
        
        binding.addTagButton.setOnClickListener {
            showAddTagDialog()
        }
        
        binding.moodSelector.setOnCheckedChangeListener { _, checkedId ->
            // Handle mood selection
        }
        
        binding.weatherSelector.setOnCheckedChangeListener { _, checkedId ->
            // Handle weather selection
        }
    }
    
    private fun applyTemplate(template: JournalTemplates.JournalTemplate) {
        binding.journalTitleEditText.setText(template.title)
        binding.journalContentEditText.setText(template.content)
        binding.templateIndicator.text = "Template: ${template.name}"
        binding.templateIndicator.visibility = android.view.View.VISIBLE
    }
    
    private fun showUploadOptions() {
        val options = arrayOf("Take Photo of Journal", "Upload Document")
        MaterialAlertDialogBuilder(this)
            .setTitle("Upload Journal")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCameraForJournal()
                    1 -> openDocumentPicker()
                }
            }
            .show()
    }
    
    private fun openCameraForJournal() {
        val photoFile = FileUtils.createImageFile(this)
        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoURI)
    }
    
    private fun openDocumentPicker() {
        documentLauncher.launch("*/*")
    }
    
    private fun handleDocumentSelection(uri: Uri) {
        // Handle document upload and processing
        currentPhotoPath = FileUtils.copyFileToInternalStorage(this, uri)
        Toast.makeText(this, "Document uploaded. Processing...", Toast.LENGTH_SHORT).show()
        // Here you would send to backend for processing
    }
    
    private fun displayImage(uri: Uri) {
        binding.attachedImage.visibility = android.view.View.VISIBLE
        Glide.with(this)
            .load(uri)
            .into(binding.attachedImage)
    }
    
    private fun processHandwrittenJournal(imagePath: String) {
        // Here you would integrate with OCR service or backend API
        Toast.makeText(this, "Processing handwritten journal...", Toast.LENGTH_LONG).show()
        
        // Simulate OCR processing
        lifecycleScope.launch {
            // In real app, call OCR API
            kotlinx.coroutines.delay(2000)
            
            // Simulate extracted text
            val extractedText = "This is simulated extracted text from the handwritten journal..."
            runOnUiThread {
                binding.journalContentEditText.setText(extractedText)
                Toast.makeText(this@CreateJournalActivity, "Text extracted successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddTagDialog() {
        val input = android.widget.EditText(this)
        input.hint = "Enter tag"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("Add Tag")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty() && !tags.contains(tag)) {
                    tags.add(tag)
                    addTagChip(tag)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun addTagChip(tag: String) {
        val chip = Chip(this)
        chip.text = tag
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            binding.tagsChipGroup.removeView(chip)
            tags.remove(tag)
        }
        binding.tagsChipGroup.addView(chip)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_journal, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveJournal()
                true
            }
            R.id.action_delete -> {
                if (isEditMode) deleteJournal()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveJournal() {
        val title = binding.journalTitleEditText.text.toString().trim()
        val content = binding.journalContentEditText.text.toString().trim()
        
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter title or content", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedMood = getSelectedMood()
        val selectedWeather = getSelectedWeather()
        
        val journalEntry = JournalEntry(
            id = if (isEditMode) journalId else 0,
            title = title.ifEmpty { "Journal Entry - ${Date()}" },
            content = content,
            templateType = selectedTemplate,
            tags = tags.toList(),
            attachmentPath = currentPhotoPath,
            mood = selectedMood,
            weather = selectedWeather,
            createdAt = if (isEditMode) Date() else Date(),
            updatedAt = Date()
        )
        
        if (isEditMode) {
            viewModel.updateJournalEntry(journalEntry)
        } else {
            viewModel.insertJournalEntry(journalEntry)
        }
        
        Toast.makeText(this, "Journal entry saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun getSelectedMood(): String? {
        return when (binding.moodSelector.checkedRadioButtonId) {
            R.id.mood_happy -> "Happy"
            R.id.mood_neutral -> "Neutral"
            R.id.mood_sad -> "Sad"
            R.id.mood_excited -> "Excited"
            R.id.mood_anxious -> "Anxious"
            else -> null
        }
    }
    
    private fun getSelectedWeather(): String? {
        return when (binding.weatherSelector.checkedRadioButtonId) {
            R.id.weather_sunny -> "Sunny"
            R.id.weather_cloudy -> "Cloudy"
            R.id.weather_rainy -> "Rainy"
            R.id.weather_snowy -> "Snowy"
            else -> null
        }
    }
    
    private fun deleteJournal() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Journal Entry")
            .setMessage("Are you sure you want to delete this journal entry?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete journal logic
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}