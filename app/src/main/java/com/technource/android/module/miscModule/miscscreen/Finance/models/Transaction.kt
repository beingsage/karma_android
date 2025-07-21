package com.technource.android.module.miscModule.miscscreen.Finance.models

data class Transaction(
    val description: String,
    val amount: String,
    val date: String,
    val isIncome: Boolean
)
