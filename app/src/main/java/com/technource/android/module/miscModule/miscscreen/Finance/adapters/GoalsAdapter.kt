package com.technource.android.module.miscModule.miscscreen.Finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.FinancialGoal

class GoalsAdapter(
    private val goals: List<FinancialGoal>,
    private val onGoalClick: (FinancialGoal) -> Unit
) : RecyclerView.Adapter<GoalsAdapter.GoalViewHolder>() {

    class GoalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.goal_title)
        val currentAmount: TextView = view.findViewById(R.id.goal_current_amount)
        val targetAmount: TextView = view.findViewById(R.id.goal_target_amount)
        val deadline: TextView = view.findViewById(R.id.goal_deadline)
        val progressBar: ProgressBar = view.findViewById(R.id.goal_progress)
        val progressText: TextView = view.findViewById(R.id.goal_progress_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.title.text = goal.title
        holder.currentAmount.text = goal.currentAmount
        holder.targetAmount.text = "of ${goal.targetAmount}"
        holder.deadline.text = "Target: ${goal.deadline}"
        holder.progressBar.progress = goal.progress.toInt()
        holder.progressText.text = "${goal.progress.toInt()}%"
        
        holder.itemView.setOnClickListener {
            onGoalClick(goal)
        }
    }

    override fun getItemCount() = goals.size
}
