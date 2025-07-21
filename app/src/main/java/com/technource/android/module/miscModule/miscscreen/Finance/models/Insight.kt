package com.technource.android.module.miscModule.miscscreen.Finance.models

data class Insight(
    val title: String,
    val description: String,
    val type: String, // warning, suggestion, success, reminder, info
    val timestamp: String
)
