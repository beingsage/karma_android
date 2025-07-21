package com.technource.android.module.miscModule.miscscreen.Finance.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.Transaction

class RecentTransactionsAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<RecentTransactionsAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val description: TextView = view.findViewById(R.id.transaction_description)
        val amount: TextView = view.findViewById(R.id.transaction_amount)
        val date: TextView = view.findViewById(R.id.transaction_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.description.text = transaction.description
        holder.amount.text = transaction.amount
        holder.date.text = transaction.date
        // Optionally set color based on income/expense
        val context = holder.itemView.context
        val color = if (transaction.isIncome) {
            context.getColor(R.color.success_green)
        } else {
            context.getColor(R.color.error_red)
        }
        holder.amount.setTextColor(color)
    }

    override fun getItemCount() = transactions.size
}