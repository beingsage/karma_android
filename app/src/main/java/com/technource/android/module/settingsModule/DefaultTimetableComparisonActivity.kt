package com.technource.android.module.settingsModule

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.technource.android.databinding.ActivityDefaultTimetableComparisonBinding
import com.technource.android.local.AppDatabase
import com.technource.android.local.Task
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.eTMS.macro.DefaultTaskFetcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DefaultTimetableComparisonActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDefaultTimetableComparisonBinding

    @Inject lateinit var database: AppDatabase
    @Inject lateinit var apiService: ApiService

    private val fetcher by lazy { DefaultTaskFetcher(database, apiService, Gson()) }
    private var currentTasks: List<Task>? = null
    private var newTasks: List<Task>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDefaultTimetableComparisonBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadCurrentDefaultTasks()

        binding.btnFetchNew.setOnClickListener {
            fetchNewDefaultTasks()
        }

        binding.btnConfirm.setOnClickListener {
            newTasks?.let {
                lifecycleScope.launch {
                    fetcher.storeDefaultTasks(it)
                    currentTasks = it
                    SystemStatus.logEvent("DefaultTimetableComparison", "Confirmed new default timetable with ${it.size} tasks")
                    updateUI()
                }
            }
        }
    }

    private fun loadCurrentDefaultTasks() {
        lifecycleScope.launch {
            currentTasks = fetcher.getCurrentDefaultTasks()
            SystemStatus.logEvent("DefaultTimetableComparison", "Loaded ${currentTasks?.size ?: 0} current default tasks")
            updateUI()
        }
    }

    private fun fetchNewDefaultTasks() {
        lifecycleScope.launch {
            fetcher.fetchNewDefaultTasks(
                onTasksFetched = { tasks ->
                    newTasks = tasks
                    binding.tvNotification.text = "New default timetable available! Compare below."
                    SystemStatus.logEvent("DefaultTimetableComparison", "Fetched ${tasks.size} new default tasks")
                    updateUI()
                },
                onError = { error ->
                    binding.tvNotification.text = error
                    SystemStatus.logEvent("DefaultTimetableComparison", "Error fetching new default tasks: $error")
                }
            )
        }
    }

    private fun updateUI() {
        binding.tvCurrentTasks.text = currentTasks?.joinToString("\n") {
            "${it.title} (${it.startTime} - ${it.endTime})"
        } ?: "No default TT yet"
        binding.tvNewTasks.text = newTasks?.joinToString("\n") {
            "${it.title} (${it.startTime} - ${it.endTime})"
        } ?: "Fetch new TT to compare"
    }
}