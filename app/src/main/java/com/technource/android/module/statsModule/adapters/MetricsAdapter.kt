package com.technource.android.module.statsModule.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.module.statsModule.Metric
import kotlin.math.abs

class MetricsAdapter : RecyclerView.Adapter<MetricsAdapter.MetricViewHolder>() {
    private var metrics: List<Metric> = emptyList()

    fun updateMetrics(newMetrics: List<Metric>) {
        metrics = newMetrics
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_metric_card, parent, false)
        return MetricViewHolder(view)
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        holder.bind(metrics[position])
    }

    override fun getItemCount() = metrics.size

    class MetricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val valueTextView: TextView = itemView.findViewById(R.id.metric_value)
        private val nameTextView: TextView = itemView.findViewById(R.id.metric_name)
        private val detailTextView: TextView = itemView.findViewById(R.id.metric_detail)
        private val trendContainer: View = itemView.findViewById(R.id.trend_container)
        private val trendIcon: View = itemView.findViewById(R.id.trend_icon)
        private val trendValue: TextView = itemView.findViewById(R.id.trend_value)

        fun bind(metric: Metric) {
            valueTextView.text = metric.value
            nameTextView.text = metric.name
            detailTextView.text = metric.detail

            if (metric.trend != 0f) {
                trendContainer.isVisible = true
                trendValue.text = String.format("%.1f", abs(metric.trend))

                if (metric.trend > 0) {
                    trendIcon.setBackgroundResource(R.drawable.ic_trend_up)
                    trendValue.setTextColor(ContextCompat.getColor(itemView.context, R.color.trend_positive))
                } else {
                    trendIcon.setBackgroundResource(R.drawable.ic_trend_down)
                    trendValue.setTextColor(ContextCompat.getColor(itemView.context, R.color.trend_negative))
                }
            } else {
                trendContainer.isVisible = false
            }
        }
    }
}