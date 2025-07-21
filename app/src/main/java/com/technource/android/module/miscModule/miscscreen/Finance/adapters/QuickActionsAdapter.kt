package com.technource.android.module.miscModule.miscscreen.Finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.QuickAction

class QuickActionsAdapter(
    private val actions: List<QuickAction>,
    private val onActionClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionsAdapter.QuickActionViewHolder>() {

    class QuickActionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.action_icon)
        val title: TextView = view.findViewById(R.id.action_title)
        val subtitle: TextView = view.findViewById(R.id.action_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuickActionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quick_action, parent, false)
        return QuickActionViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuickActionViewHolder, position: Int) {
        val action = actions[position]
        holder.icon.setImageResource(action.iconRes)
        holder.title.text = action.title
        holder.subtitle.text = action.subtitle
        
        holder.itemView.setOnClickListener {
            onActionClick(action)
        }
    }

    override fun getItemCount() = actions.size
}
