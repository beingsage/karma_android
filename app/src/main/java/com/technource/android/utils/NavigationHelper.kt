package com.technource.android.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.technource.android.R
import com.technource.android.module.homeModule.HomeScreen
import com.technource.android.module.miscModule.MiscellaneousScreen
import com.technource.android.module.settingsModule.SettingsScreen
import com.technource.android.module.statsModule.StatsScreen

object NavigationHelper {
    fun setupBottomNavigation(context: Context, bottomNavigation: BottomNavigationView) {
        bottomNavigation.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(context, HomeScreen::class.java)
                R.id.nav_stats -> Intent(context, StatsScreen::class.java)
                R.id.nav_misc -> Intent(context, MiscellaneousScreen::class.java)
                R.id.nav_settings -> Intent(context, SettingsScreen::class.java)
                else -> null
            }
            intent?.let {
                // Use CLEAR_TOP and SINGLE_TOP to manage the back stack
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(context, it, null)
                // Finish the current activity if it's not the target (except HomeScreen as the root)
                if (context is Activity && context !is HomeScreen) {
                    context.finish()
                }
                true
            } ?: false
        }

        // Highlight the current top-level activity
        when (context) {
            is HomeScreen -> bottomNavigation.menu.findItem(R.id.nav_home)?.isChecked = true
            is StatsScreen -> bottomNavigation.menu.findItem(R.id.nav_stats)?.isChecked = true
            is MiscellaneousScreen -> bottomNavigation.menu.findItem(R.id.nav_misc)?.isChecked = true
            is SettingsScreen -> bottomNavigation.menu.findItem(R.id.nav_settings)?.isChecked = true
            else -> bottomNavigation.menu.findItem(R.id.nav_home)?.isChecked = true // Default to Home
        }
    }
}