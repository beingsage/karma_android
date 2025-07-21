package com.technource.android.module.miscModule.miscscreen.Finance.models

data class FinancialGoal(
    val title: String,
    val targetAmount: String,
    val currentAmount: String,
    val progress: Float,
    val deadline: String
)
