package com.technource.android.module.statsModule.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.technource.android.R
import com.technource.android.module.statsModule.StatsViewModel
import com.technource.android.module.statsModule.SubtaskType

class SubtaskChartsFragment : Fragment() {

    private val viewModel: StatsViewModel by activityViewModels()
    private lateinit var subtaskTypeChart: BarChart
    private lateinit var subtaskCompletionChart: BarChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_subtask_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subtaskTypeChart = view.findViewById(R.id.subtask_type_chart)
        subtaskCompletionChart = view.findViewById(R.id.subtask_completion_chart)

        setupSubtaskTypeChart()
        setupSubtaskCompletionChart()

        observeData()
    }

    private fun setupSubtaskTypeChart() {
        with(subtaskTypeChart) {
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

    private fun setupSubtaskCompletionChart() {
        with(subtaskCompletionChart) {
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
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = 100f

            axisRight.isEnabled = false

            animateY(1500, Easing.EaseInOutQuad)
        }
    }

    private fun observeData() {
        viewModel.subtaskTypeData.observe(viewLifecycleOwner) { data ->
            updateSubtaskTypeChart(data)
            updateSubtaskCompletionChart(data)
        }
    }

    private fun updateSubtaskTypeChart(data: List<SubtaskType>) {
        val entries = data.mapIndexed { index, subtaskType ->
            BarEntry(index.toFloat(), subtaskType.score)
        }

        val colors = listOf(
            ContextCompat.getColor(requireContext(), R.color.indigo_500),
            ContextCompat.getColor(requireContext(), R.color.blue_500),
            ContextCompat.getColor(requireContext(), R.color.green_500),
            ContextCompat.getColor(requireContext(), R.color.amber_500)
        )

        val dataSet = BarDataSet(entries, "Score Earned").apply {
            setColors(colors)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 10f
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        subtaskTypeChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) {
                    when (data[index].type) {
                        "DeepWork" -> "Deep Work"
                        "Time" -> "Time-Based"
                        "Quant" -> "Quantifiable"
                        "Binary" -> "Boolean"
                        else -> data[index].type
                    }
                } else ""
            }
        }

        subtaskTypeChart.data = barData
        subtaskTypeChart.invalidate()
    }

    private fun updateSubtaskCompletionChart(data: List<SubtaskType>) {
        val entries = data.mapIndexed { index, subtaskType ->
            val completionRate = if (subtaskType.total > 0)
                (subtaskType.completed.toFloat() / subtaskType.total) * 100 else 0f
            BarEntry(index.toFloat(), completionRate)
        }

        val dataSet = BarDataSet(entries, "Completion Rate").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }

        val barData = BarData(dataSet).apply {
            barWidth = 0.6f
        }

        subtaskCompletionChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) {
                    when (data[index].type) {
                        "DeepWork" -> "Deep Work"
                        "Time" -> "Time-Based"
                        "Quant" -> "Quantifiable"
                        "Binary" -> "Boolean"
                        else -> data[index].type
                    }
                } else ""
            }
        }

        subtaskCompletionChart.data = barData
        subtaskCompletionChart.invalidate()
    }
}