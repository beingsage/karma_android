package com.technource.android.module.miscModule.miscscreen.Finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.Investment

class InvestmentAdapter(
    private val investments: List<Investment>,
    private val onInvestmentClick: (Investment) -> Unit
) : RecyclerView.Adapter<InvestmentAdapter.InvestmentViewHolder>() {

    class InvestmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val symbol: TextView = view.findViewById(R.id.investment_symbol)
        val name: TextView = view.findViewById(R.id.investment_name)
        val value: TextView = view.findViewById(R.id.investment_value)
        val change: TextView = view.findViewById(R.id.investment_change)
        val progressBar: ProgressBar = view.findViewById(R.id.investment_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_investment, parent, false)
        return InvestmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val investment = investments[position]
        holder.symbol.text = investment.symbol
        holder.name.text = investment.name
        holder.value.text = investment.value
        holder.change.text = investment.change
        holder.progressBar.progress = investment.percentage.toInt()
        
        // Set color based on positive/negative change
        val context = holder.itemView.context
        val color = if (investment.change.startsWith("+")) {
            context.getColor(R.color.success_green)
        } else {
            context.getColor(R.color.error_red)
        }
        holder.change.setTextColor(color)
        
        holder.itemView.setOnClickListener {
            onInvestmentClick(investment)
        }
    }

    override fun getItemCount() = investments.size
}
