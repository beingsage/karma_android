package com.technource.android.module.homeModule

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.technource.android.R
import com.technource.android.databinding.ItemSubtaskBinding
import com.technource.android.databinding.ItemTaskBinding
import com.technource.android.local.Task
import com.technource.android.local.TaskStatus
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAdapter(private val viewModel: TaskViewModel) :
    ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private var expandedTaskId: String? = null
    private var onTaskExpandedListener: ((Task, Boolean) -> Unit)? = null
    private var blinkValue = 0f
    private val blinkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            blinkValue = it.animatedValue as Float
            // Update only tasks with RUNNING status
            currentList.forEachIndexed { index, task ->
                if (task.status == TaskStatus.RUNNING) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    init {
        // Start animator for RUNNING tasks
        CoroutineScope(Dispatchers.Main).launch {
            blinkAnimator.start()
        }
    }

    fun setOnTaskExpandedListener(listener: (Task, Boolean) -> Unit) {
        onTaskExpandedListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task, task.isExpanded)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    task.isExpanded = !task.isExpanded
                    expandedTaskId = if (task.isExpanded) task.id else null
                    onTaskExpandedListener?.invoke(task, task.isExpanded)
                    notifyItemChanged(position)
                }
            }
        }

        fun bind(task: Task, isExpanded: Boolean) {
            // Set card elevation based on status
            val cardView = binding.root as MaterialCardView
            cardView.elevation = when (task.status) {
                TaskStatus.RUNNING -> 8f
                TaskStatus.UPCOMING -> 4f
                else -> 2f
            }

            // Set category indicator color
            binding.categoryIndicator.setBackgroundColor(getCategoryColor(task.category))

            // Set task details using DateFormatter
            binding.textViewTitle.text = task.title
            binding.textViewStartTime.text = try {
                val startTime = DateFormatter.parseIsoDateTime(task.startTime)
                DateFormatter.formatDisplayTime(startTime)
            } catch (e: Exception) {
                "Invalid time"
            }

            // Calculate completion status
            val completionStatus = task.subtasks?.let {
                if (it.isEmpty()) 0f
                else it.count { subtask -> subtask.completionStatus == 1.0f }.toFloat() / it.size
            } ?: task.completionStatus

            // Set progress indicator
            binding.progressTask.progress = (completionStatus * 100).toInt()

            // Set dot color and blinking based on status
            val isBlinking = task.status == TaskStatus.RUNNING
            when (task.status) {
                TaskStatus.LOGGED -> binding.taskDot.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_completed))
                TaskStatus.MISSED -> binding.taskDot.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_missed))
                TaskStatus.RUNNING -> {
                    val alpha = (128 + (127 * blinkValue)).toInt()
                    binding.taskDot.setBackgroundColor(Color.argb(alpha, 25, 118, 210)) // Blinking In Progress
                }
                TaskStatus.UPCOMING -> binding.taskDot.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_upcoming))
                TaskStatus.SYSTEM_FAILURE -> binding.taskDot.setBackgroundColor(Color.parseColor("#FF00FF")) // Magenta
                null -> binding.taskDot.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_upcoming))
            }

            // Always use detailed view
            binding.textViewDuration.visibility = View.GONE
            binding.layoutDetailedInfo.visibility = View.VISIBLE

            // Set score
            binding.textViewScore.text = "${task.taskScore.toInt()}/${task.subtasks?.sumOf { it.baseScore } ?: 0}"

            // Set measurement type
            val measureTypeText = task.subtasks?.firstOrNull()?.measurementType?.capitalize() ?: ""
            binding.textViewMeasureType.text = measureTypeText

            // Handle expanded state
            if (isExpanded) {
                expandView(binding.layoutExpandedContent)

                // Set subtasks
                if (!task.subtasks.isNullOrEmpty()) {
                    binding.layoutSubtasks.visibility = View.VISIBLE
                    binding.layoutSubtasks.removeAllViews()

                    task.subtasks.forEach { subtask ->
                        val subtaskBinding = ItemSubtaskBinding.inflate(
                            LayoutInflater.from(binding.root.context),
                            binding.layoutSubtasks,
                            false
                        )

                        subtaskBinding.textViewSubtaskText.text = subtask.title
                        subtaskBinding.imageViewStatus.setImageResource(
                            if (subtask.completionStatus == 1.0f) R.drawable.ic_circle_completed
                            else R.drawable.ic_circle_incomplete
                        )

                        if (subtask.completionStatus == 1.0f) {
                            subtaskBinding.textViewSubtaskText.apply {
                                setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                                paintFlags = paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                            }
                        }

                        // Display measurement details
                        when (subtask.measurementType) {
                            "binary" -> {
                                subtaskBinding.textViewSubtaskText.append(
                                    " (Yes/No: ${subtask.binary?.completed?.toString() ?: "Pending"})"
                                )
                            }
                            "time" -> {
                                subtaskBinding.textViewSubtaskText.append(
                                    " (${subtask.time?.timeSpent ?: 0}/${subtask.time?.setDuration}m)"
                                )
                            }
                            "quant" -> {
                                subtaskBinding.textViewSubtaskText.append(
                                    " (${subtask.quant?.achievedValue ?: 0}/${subtask.quant?.targetValue} ${subtask.quant?.targetUnit})"
                                )
                            }
                            "deepwork" -> {
                                subtaskBinding.textViewSubtaskText.append(
                                    " (Score: ${subtask.deepwork?.deepworkScore ?: "Pending"})"
                                )
                            }
                        }

                        binding.layoutSubtasks.addView(subtaskBinding.root)
                    }
                } else {
                    binding.layoutSubtasks.visibility = View.GONE
                }
            } else {
                collapseView(binding.layoutExpandedContent)
            }
        }

        private fun expandView(view: View) {
            view.visibility = View.VISIBLE
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val targetHeight = view.measuredHeight
            view.layoutParams.height = 0
            view.alpha = 0f

            val animator = ValueAnimator.ofInt(0, targetHeight)
            animator.addUpdateListener {
                view.layoutParams.height = it.animatedValue as Int
                view.requestLayout()
                val progress = (it.animatedValue as Int).toFloat() / targetHeight.toFloat()
                view.alpha = progress
            }
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.duration = 300
            animator.start()
        }

        private fun collapseView(view: View) {
            val initialHeight = view.measuredHeight
            val animator = ValueAnimator.ofInt(initialHeight, 0)
            animator.addUpdateListener {
                view.layoutParams.height = it.animatedValue as Int
                view.requestLayout()
                val progress = 1 - (it.animatedValue as Int).toFloat() / initialHeight.toFloat()
                view.alpha = 1 - progress
            }
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    view.visibility = View.GONE
                }
            })
            animator.interpolator = AccelerateDecelerateInterpolator()
            animator.duration = 300
            animator.start()
        }
    }

    // Helper method to get color for task category
    private fun getCategoryColor(category: String?): Int {
        return when (category?.lowercase()) {
            "work" -> Color.parseColor("#5E35B1") // Deep Purple
            "personal" -> Color.parseColor("#00897B") // Teal
            "health" -> Color.parseColor("#D81B60") // Pink
            "learning" -> Color.parseColor("#FB8C00") // Orange
            else -> Color.parseColor("#546E7A") // Blue Grey
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem && oldItem.isExpanded == newItem.isExpanded
        }
    }
}
