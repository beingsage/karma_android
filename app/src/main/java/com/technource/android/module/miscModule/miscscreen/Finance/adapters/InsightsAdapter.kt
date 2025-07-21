package com.technource.android.module.miscModule.miscscreen.Finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.Insight

class InsightsAdapter(private val insights: List<Insight>) :
    RecyclerView.Adapter<InsightsAdapter.InsightViewHolder>() {

    class InsightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.insight_icon)
        val title: TextView = view.findViewById(R.id.insight_title)
        val description: TextView = view.findViewById(R.id.insight_description)
        val timestamp: TextView = view.findViewById(R.id.insight_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_insight, parent, false)
        return InsightViewHolder(view)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        val insight = insights[position]
        holder.title.text = insight.title
        holder.description.text = insight.description
        holder.timestamp.text = insight.timestamp
        
        // Set icon based on type
        val iconRes = when (insight.type) {
            "warning" -> R.drawable.ic_warning
            "suggestion" -> R.drawable.ic_lightbulb
            "success" -> R.drawable.ic_check_circle
            "reminder" -> R.drawable.ic_alarm
            else -> R.drawable.ic_info
        }
        holder.icon.setImageResource(iconRes)
    }

    override fun getItemCount() = insights.size
}
