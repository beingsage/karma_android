package com.technource.android.module.settingsModule.data


data class ApiStatus(
    val success: Boolean,
    val message: String,
    val version: String,
    val status: String,
    val timestamp: String,
    val timetable: String,
    val defaultTimeTable: String,
    val templates: String,
    val routine: String,
    val stats: String,
    val analytics: String,
    val llm: String
)