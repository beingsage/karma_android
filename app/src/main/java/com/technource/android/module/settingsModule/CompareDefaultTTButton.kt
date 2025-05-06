package com.technource.android.module.settingsModule

import android.content.Intent
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CompareDefaultTTButton() {
    val context = LocalContext.current
    Button(onClick = {
        context.startActivity(Intent(context, DefaultTimetableComparisonActivity::class.java))
    }) {
        Text("Compare Default TT")
    }
}