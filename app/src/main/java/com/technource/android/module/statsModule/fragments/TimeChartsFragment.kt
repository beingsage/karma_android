package com.technource.android.module.statsModule.fragments

import android.graphics.Color
import androidx.activity.viewModels
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.technource.android.R
import com.technource.android.module.statsModule.models.DeepWorkPoint
import com.technource.android.module.statsModule.models.HourlyProductivity
import com.technource.android.module.statsModule.models.StatsViewModel


class TimeChartsFragment : Fragment() {

    private val viewModel: StatsViewModel by activityViewModels()
    private lateinit var deepWorkChart: LineChart
    private lateinit var hourlyProductivityChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_time_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deepWorkChart = view.findViewById(R.id.deep_work_chart)
        hourlyProductivityChart = view.findViewById(R.id.hourly_productivity_chart)

        setupDeepWorkChart()
        setupHourlyProductivityChart()

        observeData()
    }

    private fun setupDeepWorkChart() {
        with(deepWorkChart) {
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

            animateX(1500, Easing.EaseInOutQuad)
        }
    }

    private fun setupHourlyProductivityChart() {
        with(hourlyProductivityChart) {
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

            animateX(1500, Easing.EaseInOutQuad)
        }
    }

    private fun observeData() {
        viewModel.deepWorkData.observe(viewLifecycleOwner) { data ->
            updateDeepWorkChart(data)
        }

        viewModel.hourlyProductivityData.observe(viewLifecycleOwner) { data ->
            updateHourlyProductivityChart(data)
        }
    }

    private fun updateDeepWorkChart(data: List<DeepWorkPoint>) {
        val requiredEntries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.required)
        }

        val achievedEntries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.achieved)
        }

        val requiredDataSet = LineDataSet(requiredEntries, "Required").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    ContextCompat.getColor(requireContext(), R.color.indigo_500_alpha_80),
                    ContextCompat.getColor(requireContext(), R.color.indigo_500_alpha_10)
                )
            )
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val achievedDataSet = LineDataSet(achievedEntries, "Achieved").apply {
            color = ContextCompat.getColor(requireContext(), R.color.green_500)
            lineWidth = 2f
            setDrawCircles(false)
            setDrawFilled(true)
            fillDrawable = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    ContextCompat.getColor(requireContext(), R.color.green_500_alpha_80),
                    ContextCompat.getColor(requireContext(), R.color.green_500_alpha_10)
                )
            )
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        val dataSets = ArrayList<ILineDataSet>().apply {
            add(requiredDataSet)
            add(achievedDataSet)
        }

        val lineData = LineData(dataSets)

        deepWorkChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].time else ""
            }
        }

        deepWorkChart.data = lineData
        deepWorkChart.invalidate()
    }

    private fun updateHourlyProductivityChart(data: List<HourlyProductivity>) {
        val entries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.tasks.toFloat())
        }

        val dataSet = LineDataSet(entries, "Tasks").apply {
            color = ContextCompat.getColor(requireContext(), R.color.pink_500)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.pink_500))
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val lineData = LineData(dataSet)

        hourlyProductivityChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].time else ""
            }
        }

        hourlyProductivityChart.data = lineData
        hourlyProductivityChart.invalidate()
    }
}