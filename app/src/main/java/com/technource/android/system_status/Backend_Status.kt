package com.technource.android.system_status

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Data model for key-value pairs
data class KeyValuePair(val key: String, val value: String)

// ViewModel to handle data fetching and state
class DataViewModel : ViewModel() {
    private val _data = mutableStateOf<List<KeyValuePair>>(emptyList())
    val data: State<List<KeyValuePair>> = _data

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        // Start auto-refresh every 5 minutes
        viewModelScope.launch {
            while (true) {
                fetchData()
                delay(5 * 60 * 1000L) // 5 minutes
            }
        }
    }

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    // Replace with your backend URL
                    URL("https://karma-backend-zyft.onrender.com").readText()
                }
                val jsonObject = JSONObject(jsonString)
                val pairs = mutableListOf<KeyValuePair>()
                jsonObject.keys().forEach { key ->
                    pairs.add(KeyValuePair(key, jsonObject.getString(key)))
                }
                _data.value = pairs
            } catch (e: Exception) {
                _error.value = "Failed to fetch data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

// Composable function for the data display
@Composable
fun DynamicDataDisplay(viewModel: DataViewModel = viewModel()) {
    val data by viewModel.data
    val isLoading by viewModel.isLoading
    val error by viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { viewModel.fetchData() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh Data")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when {
            isLoading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
            }
            data.isEmpty() -> {
                Text("No data available")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(data) { pair ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = pair.key,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pair.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
