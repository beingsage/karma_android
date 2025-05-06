package com.technource.android.module.statsModule.fragments

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
import com.technource.android.module.statsModule.models.RewardPoint
import com.technource.android.module.statsModule.models.ScorePoint
import com.technource.android.module.statsModule.models.StatsViewModel

class ScoreChartsFragment : Fragment() {

    private val viewModel: StatsViewModel by activityViewModels()
    private lateinit var scoreCurveChart: LineChart
    private lateinit var rewardCurveChart: LineChart

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_score_charts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scoreCurveChart = view.findViewById(R.id.score_curve_chart)
        rewardCurveChart = view.findViewById(R.id.reward_curve_chart)

        setupScoreCurveChart()
        setupRewardCurveChart()

        observeData()
    }

    private fun setupScoreCurveChart() {
        with(scoreCurveChart) {
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

    private fun setupRewardCurveChart() {
        with(rewardCurveChart) {
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
        viewModel.scoreCurveData.observe(viewLifecycleOwner) { data ->
            updateScoreCurveChart(data)
        }

        viewModel.rewardCurveData.observe(viewLifecycleOwner) { data ->
            updateRewardCurveChart(data)
        }
    }

    private fun updateScoreCurveChart(data: List<ScorePoint>) {
        val expectedEntries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.expected)
        }

        val achievedEntries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.achieved)
        }

        val expectedDataSet = LineDataSet(expectedEntries, "Expected").apply {
            color = ContextCompat.getColor(requireContext(), R.color.purple_500)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.purple_500))
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val achievedDataSet = LineDataSet(achievedEntries, "Achieved").apply {
            color = ContextCompat.getColor(requireContext(), R.color.green_500)
            lineWidth = 2f
            setDrawCircles(true)
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.green_500))
            circleRadius = 3f
            setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val dataSets = ArrayList<ILineDataSet>().apply {
            add(expectedDataSet)
            add(achievedDataSet)
        }

        val lineData = LineData(dataSets)

        scoreCurveChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].time else ""
            }
        }

        scoreCurveChart.data = lineData
        scoreCurveChart.invalidate()
    }

    private fun updateRewardCurveChart(data: List<RewardPoint>) {
        val entries = data.mapIndexed { index, point ->
            Entry(index.toFloat(), point.score)
        }

        val dataSet = LineDataSet(entries, "Score").apply {
            color = ContextCompat.getColor(requireContext(), R.color.indigo_500)
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

        val lineData = LineData(dataSet)

        rewardCurveChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()
                return if (index >= 0 && index < data.size) data[index].time else ""
            }
        }

        rewardCurveChart.data = lineData
        rewardCurveChart.invalidate()
    }
}