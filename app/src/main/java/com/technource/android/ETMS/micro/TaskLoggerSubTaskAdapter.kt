package com.technource.android.ETMS.micro

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.technource.android.R
import com.technource.android.databinding.ItemLockSubtaskBinding
import com.technource.android.local.SubTask

class SubTaskAdapter(
    private val subTasks: List<SubTask>,
    private val onCompletionChange: (SubTask, Any, Int) -> Unit // Add position to callback
) : RecyclerView.Adapter<SubTaskAdapter.SubTaskViewHolder>() {

    class SubTaskViewHolder(val binding: ItemLockSubtaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): SubTaskViewHolder {
        val binding = ItemLockSubtaskBinding.inflate(android.view.LayoutInflater.from(parent.context), parent, false)
        return SubTaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubTaskViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val subTask = subTasks[position]
        with(holder.binding) {
            titleTextView.text = subTask.title
            measurementTextView.text = "Type: ${subTask.measurementType}"
            scoreTextView.text = "Base Score: ${subTask.baseScore}, Final Score: ${subTask.finalScore}"
            inputLayout.removeAllViews()

            when (subTask.measurementType) {
                "binary", "deepwork" -> {
                    inputLayout.orientation = LinearLayout.HORIZONTAL
                    val isCompleted = subTask.completionStatus == 1.0f
                    val yesButton = Button(root.context).apply {
                        text = "Yes"
                        layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(root.context), 1f) // Ensure minimum height
                        isEnabled = !isCompleted
                        setBackgroundColor(if (isCompleted) Color.parseColor("#A5D6A7") else Color.parseColor("#B0BEC5"))
                        setTextColor(Color.BLACK)
                        setOnClickListener {
                                onCompletionChange(subTask, true, position)
                                holder.itemView.animate().alpha(0f).setDuration(200).withEndAction {
                                    holder.itemView.animate().alpha(1f).setDuration(200).start()
                                }.start()
                            }
                        }

                    val noButton = Button(root.context).apply {
                        text = "No"
                        layoutParams = LinearLayout.LayoutParams(0, 48.dpToPx(root.context), 1f)
                        isEnabled = isCompleted
                        setBackgroundColor(if (!isCompleted) Color.parseColor("#EF9A9A") else Color.parseColor("#B0BEC5"))
                        setTextColor(Color.BLACK)
                        setOnClickListener {
                            onCompletionChange(subTask, false, position)
                            holder.itemView.animate().alpha(0f).setDuration(200).withEndAction {
                                holder.itemView.animate().alpha(1f).setDuration(200).start()
                            }.start()
                        }
                    }
                    inputLayout.addView(yesButton)
                    inputLayout.addView(noButton)
                }
                "time" -> {
                    inputLayout.orientation = LinearLayout.VERTICAL
                    val setDuration = subTask.time?.setDuration?.takeIf { it > 0 } ?: 1
                    val timeSpent = subTask.time?.timeSpent ?: 0
                    val label = TextView(root.context).apply {
                        text = "Time Spent: $timeSpent/$setDuration minutes" // Remove <b> tags
                        setTextAppearance(android.R.style.TextAppearance_Medium)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = 8 }
                    }
                    val seekBar = SeekBar(root.context).apply {
                        max = setDuration
                        progress = timeSpent
                        thumb = ContextCompat.getDrawable(context, R.drawable.seekbar_thumb)
                        progressTintList = ColorStateList.valueOf(Color.parseColor("#BB86FC"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            48.dpToPx(root.context)
                        ).apply { bottomMargin = 8 }
                        setPadding(16, 0, 16, 0)
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                    onCompletionChange(subTask, progress, position)
                                    holder.itemView.animate().alpha(0f).setDuration(200).withEndAction {
                                        holder.itemView.animate().alpha(1f).setDuration(200).start()
                                    }.start()
                                    label.text = "Time Spent: $progress/$setDuration minutes"
                                    scoreTextView.text = "Base Score: ${subTask.baseScore}, Final Score: ${subTask.finalScore}"
                                }
                            }
                            override fun onStartTrackingTouch(seekBar: SeekBar) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar) {}
                        })
                    }
                    inputLayout.addView(label)
                    inputLayout.addView(seekBar)
                }
                "quant" -> {
                    inputLayout.orientation = LinearLayout.VERTICAL
                    val targetValue = subTask.quant?.targetValue?.toInt()?.takeIf { it > 0 } ?: 1
                    val achievedValue = subTask.quant?.achievedValue ?: 0
                    val label = TextView(root.context).apply {
                        text = "Achieved: $achievedValue/$targetValue ${subTask.quant?.targetUnit}" // Remove <b> tags
                        setTextAppearance(android.R.style.TextAppearance_Medium)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { topMargin = 8 }
                    }
                    val seekBar = SeekBar(root.context).apply {
                        max = targetValue
                        progress = achievedValue
                        thumb = ContextCompat.getDrawable(context, R.drawable.seekbar_thumb)
                        progressTintList = ColorStateList.valueOf(Color.parseColor("#BB86FC"))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            48.dpToPx(root.context)
                        ).apply { bottomMargin = 8 }
                        setPadding(16, 0, 16, 0)
                        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                    onCompletionChange(subTask, progress, position)
                                    holder.itemView.animate().alpha(0f).setDuration(200).withEndAction {
                                        holder.itemView.animate().alpha(1f).setDuration(200).start()
                                    }.start()
                                    label.text = "Achieved: $progress/$targetValue ${subTask.quant?.targetUnit}"
                                    scoreTextView.text = "Base Score: ${subTask.baseScore}, Final Score: ${subTask.finalScore}"
                                }
                            }
                            override fun onStartTrackingTouch(seekBar: SeekBar) {}
                            override fun onStopTrackingTouch(seekBar: SeekBar) {}
                        })
                    }
                    inputLayout.addView(label)
                    inputLayout.addView(seekBar)
                }
            }
        }
    }

    override fun getItemCount() = subTasks.size
}

fun Int.dpToPx(context: android.content.Context): Int {
    return (this * context.resources.displayMetrics.density).toInt()
}