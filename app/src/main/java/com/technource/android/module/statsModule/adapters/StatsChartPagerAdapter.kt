package com.technource.android.module.statsModule.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.technource.android.module.statsModule.fragments.CategoryChartsFragment
import com.technource.android.module.statsModule.fragments.ScoreChartsFragment
import com.technource.android.module.statsModule.fragments.SubtaskChartsFragment
import com.technource.android.module.statsModule.fragments.TimeChartsFragment

class StatsChartPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ScoreChartsFragment()
            1 -> CategoryChartsFragment()
            2 -> SubtaskChartsFragment()
            3 -> TimeChartsFragment()
            else -> ScoreChartsFragment()
        }
    }
}