package com.technource.android.module.miscModule.miscscreen.Finance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.GoalsAdapter
import com.technource.android.module.miscModule.miscscreen.Finance.models.FinancialGoal

class GoalsFragment : Fragment() {

    private lateinit var goalsRecycler: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_goals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupGoals()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        goalsRecycler = view.findViewById(R.id.goals_recycler)
        fabAddGoal = view.findViewById(R.id.fab_add_goal)
    }

    private fun setupGoals() {
        val goals = listOf(
            FinancialGoal("Emergency Fund", "$15,000", "$11,250", 75f, "Dec 2025"),
            FinancialGoal("Home Down Payment", "$50,000", "$21,000", 42f, "Jun 2026"),
            FinancialGoal("Retirement Fund", "$500,000", "$85,000", 17f, "Dec 2045"),
            FinancialGoal("Vacation Fund", "$5,000", "$3,200", 64f, "Aug 2025"),
            FinancialGoal("Car Purchase", "$25,000", "$7,500", 30f, "Mar 2026"),
            FinancialGoal("Education Fund", "$30,000", "$12,000", 40f, "Sep 2027")
        )

        goalsRecycler.layoutManager = LinearLayoutManager(context)
        goalsRecycler.adapter = GoalsAdapter(goals) { goal ->
            // Handle goal click
        }
    }

    private fun setupClickListeners() {
        fabAddGoal.setOnClickListener {
            // Open add goal dialog or activity
        }
    }
}
