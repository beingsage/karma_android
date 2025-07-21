package com.technource.android.module.miscModule.miscscreen.Finance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.InsightsAdapter
import com.technource.android.module.miscModule.miscscreen.Finance.models.Insight

class InsightsFragment : Fragment() {

    private lateinit var insightsRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_insights, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupInsights()
    }

    private fun initViews(view: View) {
        insightsRecycler = view.findViewById(R.id.insights_recycler)
    }

    private fun setupInsights() {
        val insights = listOf(
            Insight(
                "Budget Alert",
                "You've spent 85% of your monthly budget. Consider reducing discretionary spending.",
                "warning",
                "2 hours ago"
            ),
            Insight(
                "Investment Opportunity",
                "Your portfolio is overweight in tech stocks. Consider diversifying into other sectors.",
                "suggestion",
                "1 day ago"
            ),
            Insight(
                "Savings Goal",
                "Great job! You're ahead of schedule on your emergency fund goal.",
                "success",
                "2 days ago"
            ),
            Insight(
                "Bill Reminder",
                "Your credit card payment of $1,245 is due in 3 days.",
                "reminder",
                "3 days ago"
            ),
            Insight(
                "Market Update",
                "Your portfolio gained $234 today due to strong tech sector performance.",
                "info",
                "1 week ago"
            ),
            Insight(
                "Tax Optimization",
                "Consider maxing out your 401(k) contribution to reduce taxable income.",
                "suggestion",
                "1 week ago"
            )
        )

        insightsRecycler.layoutManager = LinearLayoutManager(context)
        insightsRecycler.adapter = InsightsAdapter(insights)
    }
}
