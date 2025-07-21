package com.technource.android.module.miscModule.miscscreen.Finance

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.fragments.DashboardFragment
import com.technource.android.module.miscModule.miscscreen.Finance.fragments.GoalsFragment
import com.technource.android.module.miscModule.miscscreen.Finance.fragments.InsightsFragment
import com.technource.android.module.miscModule.miscscreen.Finance.fragments.PortfolioFragment
import com.technource.android.module.miscModule.miscscreen.Finance.fragments.TransactionsFragment

class FinanceActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finance)

        bottomNavigation = findViewById(R.id.bottom_navigation)

        // Set default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, DashboardFragment())
                .commit()
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_portfolio -> PortfolioFragment()
                R.id.nav_goals -> GoalsFragment()
                R.id.nav_insights -> InsightsFragment()
                else -> DashboardFragment()
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit()
            true
        }
    }
}
