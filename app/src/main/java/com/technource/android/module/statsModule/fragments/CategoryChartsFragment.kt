package com.technource.android.module.statsModule.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.technource.android.R
import com.technource.android.module.statsModule.models.CategoryPerformance
import com.technource.android.module.statsModule.models.StatsViewModel
import com.technource.android.module.statsModule.models.TimeDistribution

class CategoryChartsFragment : Fragment() {

    private val viewModel: StatsViewModel by activityViewModels()
    private lateinit var categoryPerformanceChart: HorizontalBarChart
    private lateinit var timeDistributionChart: PieChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_category_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        categoryPerformanceChart = view.findViewById(R.id.category_performance_chart)
        timeDistributionChart = view.findViewById(R.id.time_distribution_chart)

        setupCategoryPerformanceChart()
        setupTimeDistributionChart()

        observeData()
    }

    private fun setupCategoryPerformanceChart() {
        with(categoryPerformanceChart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

            setTouchEnabled(true)
            setDrawGridBackground(false)
            setDrawBorders(false)

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

            axisLeft.setDrawGridLines(true)
            axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.grid_line)
            axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)

            axisRight.isEnabled = false

            animateY(1500, Easing.EaseInOutQuad)
        }
    }

    private fun setupTimeDistributionChart() {
        with(timeDistributionChart) {
            description.isEnabled = false
            legend.isEnabled = true
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_primary)

            setUsePercentValues(false)
            setDrawEntryLabels(false)
            setTouchEnabled(true)
            setDrawCenterText(false)
            setDrawHoleEnabled(true)
            holeRadius = 40f
            transparentCircleRadius = 45f

            animateY(1500, Easing.EaseInOutQuad)
        }
    }

    private fun observeData() {
        viewModel.categoryPerformanceData.observe(viewLifecycleOwner) { data ->
            updateCategoryPerformanceChart(data)
        }

        viewModel.timeDistributionData.observe(viewLifecycleOwner) { data ->
            updateTimeDistributionChart(data)
        }
    }

    private fun updateCategoryPerformanceChart(data: List<CategoryPerformance>) {
        val achievedEntries = data.mapIndexed { index, category ->
            BarEntry(index.toFloat(), category.scoreAchieved)
        }

        val possibleEntries = data.mapIndexed { index, category ->
            BarEntry(index.toFloat(), category.scorePossible)
        }

        val achievedDataSet = BarDataSet(achievedEntries, "Achieved").apply {
            color = ContextCompat.getColor(requireContext(), R.color.indigo_500)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 10f
        }

        val possibleDataSet = BarDataSet(possibleEntries, "Possible").apply {
            color = ContextCompat.getColor(requireContext(), R.color.indigo_200)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 10f
        }

        val dataSets = ArrayList<IBarDataSet>().apply {
            add(achievedDataSet)
            add(possibleDataSet)
        }

        val barData = BarData(dataSets).apply {
            barWidth = 0.4f
            groupBars(0f, 0.3f, 0.05f)
        }

        categoryPerformanceChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].category else ""
            }
        }

        categoryPerformanceChart.data = barData
        categoryPerformanceChart.invalidate()
    }

    private fun updateTimeDistributionChart(data: List<TimeDistribution>) {
        val entries = data.map { distribution ->
            PieEntry(distribution.value, distribution.name)
        }

        val colors = data.map { distribution ->
            Color.parseColor(distribution.color)
        }

        val dataSet = PieDataSet(entries, "Time Distribution").apply {
            setColors(colors)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 12f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}h"
                }
            }
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)

        timeDistributionChart.data = pieData
        timeDistributionChart.invalidate()
    }
}