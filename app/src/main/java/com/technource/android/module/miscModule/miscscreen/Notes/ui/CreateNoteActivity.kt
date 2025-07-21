package com.technource.android.module.miscModule.miscscreen.Notes.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mindkeep.data.model.Note
import com.technource.android.R
import com.technource.android.databinding.ActivityCreateNoteBinding
import com.technource.android.module.miscModule.miscscreen.Notes.NotesActivity
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.NotesViewModel
import com.technource.android.module.miscModule.miscscreen.Notes.ui.viewmodels.NotesViewModelFactory
import com.technource.android.module.miscModule.miscscreen.Notes.utils.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateNoteActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateNoteBinding
    private var currentPhotoPath: String? = null
    private var reminderTime: Date? = null
    private var selectedColor: Int = Color.parseColor("#FFFFF9C4") // Default yellow
    private val tags = mutableListOf<String>()
    private var selectedCategory: String? = null
    private var noteId: Long = 0L
    private var isEditMode = false
    
    private val viewModel: NotesViewModel by viewModels {
        NotesViewModelFactory((application as NotesActivity).notesRepository)
    }
    
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                displayImage(Uri.fromFile(File(path)))
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupClickListeners()
        checkEditMode()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (isEditMode) "Edit Note" else "Create Note"
    }
    
    private fun checkEditMode() {
        noteId = intent.getLongExtra("NOTE_ID", 0L)
        if (noteId != 0L) {
            isEditMode = true
            loadNoteData()
        }
    }
    
    private fun loadNoteData() {
        lifecycleScope.launch {
            // Load note data for editing
            // This would typically come from the ViewModel
        }
    }
    
    private fun setupClickListeners() {
        binding.attachImageButton.setOnClickListener {
            showImageSelectionDialog()
        }
        
        binding.addReminderButton.setOnClickListener {
            showDateTimePicker()
        }
        
        binding.addCategoryButton.setOnClickListener {
            showCategoryDialog()
        }
        
        binding.colorPalette.setOnColorSelectedListener { color ->
            selectedColor = color
            binding.notePreview.setBackgroundColor(color)
        }
        
        binding.addTagButton.setOnClickListener {
            showAddTagDialog()
        }
    }
    
    private fun showImageSelectionDialog() {
        val options = arrayOf("Camera", "Gallery")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }
    
    private fun openCamera() {
        val photoFile = FileUtils.createImageFile(this)
        currentPhotoPath = photoFile.absolutePath
        val photoURI = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(photoURI)
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun handleImageSelection(uri: Uri) {
        currentPhotoPath = FileUtils.copyImageToInternalStorage(this, uri)
        displayImage(uri)
    }
    
    private fun displayImage(uri: Uri) {
        binding.attachedImage.visibility = android.view.View.VISIBLE
        Glide.with(this)
            .load(uri)
            .into(binding.attachedImage)
    }
    
    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute)
                reminderTime = calendar.time
                updateReminderDisplay()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    
    private fun updateReminderDisplay() {
        reminderTime?.let { time ->
            val format = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            binding.reminderText.text = "Reminder: ${format.format(time)}"
            binding.reminderText.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun showCategoryDialog() {
        val categories = arrayOf("Work", "Personal", "Health", "Finance", "Education", "Travel", "Other")
        MaterialAlertDialogBuilder(this)
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                selectedCategory = categories[which]
                binding.categoryText.text = "Category: ${categories[which]}"
                binding.categoryText.visibility = android.view.View.VISIBLE
            }
            .show()
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
        menuInflater.inflate(R.menu.menu_create_note, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveNote()
                true
            }
            R.id.action_delete -> {
                if (isEditMode) deleteNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveNote() {
        val title = binding.noteTitleEditText.text.toString().trim()
        val content = binding.noteContentEditText.text.toString().trim()
        
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Please enter title or content", Toast.LENGTH_SHORT).show()
            return
        }
        
        val note = Note(
            id = if (isEditMode) noteId else 0,
            title = title.ifEmpty { "Untitled" },
            content = content,
            color = selectedColor,
            tags = tags.toList(),
            attachmentPath = currentPhotoPath,
            reminderTime = reminderTime,
            category = selectedCategory,
            createdAt = if (isEditMode) Date() else Date(), // In real app, preserve original date
            updatedAt = Date()
        )
        
        if (isEditMode) {
            viewModel.updateNote(note)
        } else {
            viewModel.insertNote(note)
        }
        
        Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun deleteNote() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note?")
            .setPositiveButton("Delete") { _, _ ->
                // Delete note logic
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}