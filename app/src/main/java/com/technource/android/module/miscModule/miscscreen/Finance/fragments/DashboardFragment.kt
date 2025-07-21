package com.technource.android.module.miscModule.miscscreen.Finance.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.QuickActionsAdapter
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.RecentTransactionsAdapter
import com.technource.android.module.miscModule.miscscreen.Finance.models.QuickAction
import com.technource.android.module.miscModule.miscscreen.Finance.models.Transaction

class DashboardFragment : Fragment() {

    private lateinit var netWorthText: TextView
    private lateinit var monthlyIncomeText: TextView
    private lateinit var investmentsText: TextView
    private lateinit var financialHealthText: TextView
    private lateinit var quickActionsRecycler: RecyclerView
    private lateinit var recentTransactionsRecycler: RecyclerView
    private lateinit var addExpenseCard: MaterialCardView
    private lateinit var transferMoneyCard: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupData()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        netWorthText = view.findViewById(R.id.net_worth_amount)
        monthlyIncomeText = view.findViewById(R.id.monthly_income_amount)
        investmentsText = view.findViewById(R.id.investments_amount)
        financialHealthText = view.findViewById(R.id.financial_health_score)
        quickActionsRecycler = view.findViewById(R.id.quick_actions_recycler)
        recentTransactionsRecycler = view.findViewById(R.id.recent_transactions_recycler)
        addExpenseCard = view.findViewById(R.id.add_expense_card)
        transferMoneyCard = view.findViewById(R.id.transfer_money_card)
    }

    private fun setupData() {
        // Mock data - replace with actual data from your backend
        netWorthText.text = "$124,231.89"
        monthlyIncomeText.text = "$8,450.00"
        investmentsText.text = "$86,335.42"
        financialHealthText.text = "72/100"

        setupQuickActions()
        setupRecentTransactions()
    }

    private fun setupQuickActions() {
        val quickActions = listOf(
            QuickAction("Pay Bills", R.drawable.ic_bill, "3 pending"),
            QuickAction("Budget Check", R.drawable.ic_budget, "85% used"),
            QuickAction("Investments", R.drawable.ic_investment, "+4.2% today"),
            QuickAction("Goals", R.drawable.ic_goal, "2 on track")
        )

        quickActionsRecycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        quickActionsRecycler.adapter = QuickActionsAdapter(quickActions) { action ->
            handleQuickActionClick(action)
        }
    }

    private fun setupRecentTransactions() {
        val transactions = listOf(
            Transaction("Salary Deposit", "+$4,250.00", "May 8, 2025", true),
            Transaction("Rent Payment", "-$1,800.00", "May 5, 2025", false),
            Transaction("Grocery Shopping", "-$124.35", "May 2, 2025", false),
            Transaction("Investment SIP", "-$500.00", "May 1, 2025", false)
        )

        recentTransactionsRecycler.layoutManager = LinearLayoutManager(context)
        recentTransactionsRecycler.adapter = RecentTransactionsAdapter(transactions)
    }

    private fun setupClickListeners() {
        addExpenseCard.setOnClickListener {
            // Open add expense dialog or activity
            // startActivity(Intent(context, AddExpenseActivity::class.java))
        }

        transferMoneyCard.setOnClickListener {
            // Open transfer money dialog or activity
            // startActivity(Intent(context, TransferMoneyActivity::class.java))
        }
    }

    private fun handleQuickActionClick(action: QuickAction) {
        when (action.title) {
            "Pay Bills" -> {
                // Navigate to bills fragment
            }
            "Budget Check" -> {
                // Navigate to budget fragment
            }
            "Investments" -> {
                // Navigate to portfolio fragment
            }
            "Goals" -> {
                // Navigate to goals fragment
            }
        }
    }
}
