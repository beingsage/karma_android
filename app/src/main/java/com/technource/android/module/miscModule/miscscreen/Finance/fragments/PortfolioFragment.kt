package com.technource.android.module.miscModule.miscscreen.Finance.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.miscModule.miscscreen.Finance.adapters.InvestmentAdapter
import com.technource.android.module.miscModule.miscscreen.Finance.models.Investment

class PortfolioFragment : Fragment() {

    private lateinit var totalValueText: TextView
    private lateinit var todayChangeText: TextView
    private lateinit var totalReturnText: TextView
    private lateinit var investmentsRecycler: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_portfolio, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupPortfolioData()
    }

    private fun initViews(view: View) {
        totalValueText = view.findViewById(R.id.total_portfolio_value)
        todayChangeText = view.findViewById(R.id.today_change)
        totalReturnText = view.findViewById(R.id.total_return)
        investmentsRecycler = view.findViewById(R.id.investments_recycler)
    }

    private fun setupPortfolioData() {
        totalValueText.text = "$86,335.42"
        todayChangeText.text = "+$1,234.56 (+1.45%)"
        totalReturnText.text = "+$12,335.42 (+16.7%)"

        val investments = listOf(
            Investment("AAPL", "Apple Inc.", "$15,234.50", "+2.3%", 18.5f),
            Investment("VTSAX", "Vanguard Total Stock", "$12,450.00", "+1.8%", 15.2f),
            Investment("GOOGL", "Alphabet Inc.", "$8,750.25", "-0.5%", 10.8f),
            Investment("TSLA", "Tesla Inc.", "$6,890.00", "+4.2%", 8.5f),
            Investment("BTC", "Bitcoin", "$5,234.67", "+8.7%", 6.4f),
            Investment("MSFT", "Microsoft Corp.", "$4,876.00", "+1.2%", 5.9f),
            Investment("Real Estate", "REIT Portfolio", "$3,500.00", "+0.8%", 4.3f),
            Investment("Gold ETF", "Gold Investment", "$2,400.00", "-1.2%", 2.9f)
        )

        investmentsRecycler.layoutManager = LinearLayoutManager(context)
        investmentsRecycler.adapter = InvestmentAdapter(investments) { investment ->
            // Handle investment click
        }
    }
}
