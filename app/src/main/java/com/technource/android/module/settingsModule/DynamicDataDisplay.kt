package com.technource.android.module.settingsModule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.technource.android.module.settingsModule.data.ApiStatus
import com.technource.android.network.ApiService
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject


@Composable
fun DynamicDataDisplay(apiService: ApiService) {  // Pass apiService as parameter
    var apiData by remember { mutableStateOf<ApiStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(Unit) {
        try {
            val response = withContext(Dispatchers.IO) {
                apiService.getApiStatus()
            }
            apiData = response
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
            SystemStatus.logEvent("SettingsScreen", "Failed to load API status: ${e.message}")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            apiData != null -> {
                apiData?.let { data ->
                    // Status Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (data.status == "active")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = data.message,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "v${data.version}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Status: ${data.status}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Last Updated: ${DateFormatter.parseIsoDateTime(data.timestamp)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // API Endpoints Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Available Endpoints",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            val endpoints = listOf(
                                Pair("Timetable", data.timetable),
                                Pair("Default Timetable", data.defaultTimeTable),
                                Pair("Templates", data.templates),
                                Pair("Routine", data.routine),
                                Pair("Stats", data.stats),
                                Pair("Analytics", data.analytics),
                                Pair("LLM", data.llm)
                            )

                            endpoints.forEachIndexed { index, (name, path) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = path,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (index < endpoints.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Text(
                    text = "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}