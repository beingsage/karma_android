package com.technource.android.module.miscModule.miscscreen.Finance.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.TransactionAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.models.Transaction

class TransactionsFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var transactionsRecycler: RecyclerView
    private lateinit var totalIncomeText: TextView
    private lateinit var totalExpenseText: TextView
    private lateinit var fabAddTransaction: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupTabs()
        setupTransactions()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tabLayout = view.findViewById(R.id.tab_layout)
        transactionsRecycler = view.findViewById(R.id.transactions_recycler)
        totalIncomeText = view.findViewById(R.id.total_income)
        totalExpenseText = view.findViewById(R.id.total_expense)
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction)
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Income"))
        tabLayout.addTab(tabLayout.newTab().setText("Expense"))
        tabLayout.addTab(tabLayout.newTab().setText("Investment"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                filterTransactions(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupTransactions() {
        totalIncomeText.text = "+$12,450.00"
        totalExpenseText.text = "-$8,234.50"

        val transactions = getAllTransactions()
        transactionsRecycler.layoutManager = LinearLayoutManager(context)
        transactionsRecycler.adapter = TransactionAdapter(transactions)
    }

    private fun getAllTransactions(): List<Transaction> {
        return listOf(
            Transaction("Salary Deposit", "+$4,250.00", "May 8, 2025", true),
            Transaction("Freelance Payment", "+$1,200.00", "May 7, 2025", true),
            Transaction("Rent Payment", "-$1,800.00", "May 5, 2025", false),
            Transaction("Investment SIP", "-$500.00", "May 3, 2025", false),
            Transaction("Grocery Shopping", "-$124.35", "May 2, 2025", false),
            Transaction("Utility Bills", "-$89.50", "May 1, 2025", false),
            Transaction("Stock Dividend", "+$45.20", "Apr 30, 2025", true),
            Transaction("Restaurant", "-$67.80", "Apr 29, 2025", false),
            Transaction("Gas Station", "-$52.40", "Apr 28, 2025", false),
            Transaction("Online Shopping", "-$234.99", "Apr 27, 2025", false)
        )
    }

    private fun filterTransactions(tabPosition: Int) {
        val allTransactions = getAllTransactions()
        val filteredTransactions = when (tabPosition) {
            1 -> allTransactions.filter { it.isIncome }
            2 -> allTransactions.filter { !it.isIncome }
            3 -> allTransactions.filter { it.description.contains("Investment") || it.description.contains("Stock") }
            else -> allTransactions
        }
        
        (transactionsRecycler.adapter as TransactionAdapter).updateTransactions(filteredTransactions)
    }

    private fun setupClickListeners() {
        fabAddTransaction.setOnClickListener {
            // Open add transaction dialog or activity
        }
    }
}
