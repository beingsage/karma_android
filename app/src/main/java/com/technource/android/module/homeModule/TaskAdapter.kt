package com.technource.android.module.homeModule

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.technource.android.system_status.SystemStatus
import com.technource.android.utils.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone


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

    // Add timeline height listener
    private var onCardExpandedListener: ((Int) -> Unit)? = null

    fun setOnCardExpandedListener(listener: (Int) -> Unit) {
        onCardExpandedListener = listener
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
        // SystemStatus.logEvent("TaskAdapter", "Binding task at position $position: ${task.id}")
        holder.bind(task, task.id == expandedTaskId)
    }

    override fun submitList(list: List<Task>?) {
        // SystemStatus.logEvent("TaskAdapter", "Submitting new list with ${list?.size ?: 0} items")
        super.submitList(list)
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {
    
        init {
            binding.cardTask.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val task = getItem(position)
                    // Toggle expansion state
                    expandedTaskId = if (expandedTaskId == task.id) null else task.id
                    // Force a full rebind
                    notifyDataSetChanged()
                    onTaskExpandedListener?.invoke(task, expandedTaskId == task.id)
                }
            }
        }

        fun bind(task: Task, isExpanded: Boolean) {
            try {
                // Add this near the start of the bind function
                (binding.cardTask as MaterialCardView).setCardBackgroundColor(
                    getCategoryColor(task.category)
                )

                // Show gradient background for RUNNING tasks
                binding.gradientBackground.visibility = if (task.status == TaskStatus.RUNNING) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                // Set category indicator with safe color parsing
                binding.categoryIndicator.setBackgroundColor(
                    try {
                        getCategoryColor(task.category)
                    } catch (e: Exception) {
                        Color.GRAY // Default color for category
                    }
                )

                binding.categoryIndicator.setBackgroundColor(
                 when (task.status) {
                TaskStatus.LOGGED -> ContextCompat.getColor(binding.root.context, R.color.status_logged)
                TaskStatus.MISSED -> ContextCompat.getColor(binding.root.context, R.color.status_missed)
                TaskStatus.RUNNING -> ContextCompat.getColor(binding.root.context, R.color.status_running)
                TaskStatus.UPCOMING -> ContextCompat.getColor(binding.root.context, R.color.status_upcoming)
                TaskStatus.SYSTEM_FAILURE -> ContextCompat.getColor(itemView.context, R.color.status_system_failure)
                null -> ContextCompat.getColor(binding.root.context, R.color.status_upcoming)
            }
        )


                // Safely set text fields with null checks
                binding.textViewTitle.text = task.title ?: "Untitled Task"

                // Make sure time is being set and visible
                binding.textViewStartTime.apply {
                    visibility = View.VISIBLE
                    text = if (task.endTime != null) {
                        "${viewModel.formatTime(task.startTime)} - ${viewModel.formatTime(task.endTime)}"
                    } else {
                        viewModel.formatTime(task.startTime)
                    }
                }
                // Log.d("TimeDebug", "Start Time: ${task.startTime}")
                // Log.d("TimeDebug", "Formatted Time: ${viewModel.formatTime(task.startTime)}")

                // Set progress with safe calculation
                val completionStatus = task.subtasks?.let {
                    if (it.isEmpty()) 0f
                    else it.count { subtask -> subtask.completionStatus == 1.0f }.toFloat() / it.size
                } ?: task.completionStatus ?: 0f

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
                    TaskStatus.SYSTEM_FAILURE -> binding.taskDot.setBackgroundColor(Color.MAGENTA)
                    null -> binding.taskDot.setBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.status_upcoming))
                }

                // Always use detailed view
                binding.textViewDuration.visibility = View.GONE
                binding.layoutDetailedInfo.visibility = View.VISIBLE

                // Calculate achieved score (current task score)
                val achievedScore = task.taskScore.toInt()
                
                // Calculate net possible score (sum of all subtask base scores)
                val netPossibleScore = task.subtasks?.sumOf { it.baseScore } ?: 0

                // Set score in achieved/possible format
                binding.textViewScore.text = "$achievedScore/$netPossibleScore"

                // Set measurement type
                val measureTypeText = task.subtasks?.firstOrNull()?.measurementType?.capitalize() ?: ""
                binding.textViewMeasureType.text = measureTypeText

                // Handle expanded state
                binding.layoutExpandedContent.apply {
                    if (isExpanded) {
                        visibility = View.VISIBLE
                        binding.layoutSubtasks.removeAllViews()
                        task.subtasks?.forEach { subtask ->
                            val subtaskBinding = ItemSubtaskBinding.inflate(
                                LayoutInflater.from(context),
                                binding.layoutSubtasks,
                                false
                            )
                            subtaskBinding.textViewSubtaskText.text = subtask.title
                            subtaskBinding.imageViewStatus.setImageResource(
                                if (subtask.completionStatus == 1.0f)
                                    R.drawable.ic_circle_completed
                                else R.drawable.ic_circle_incomplete
                            )
                            binding.layoutSubtasks.addView(subtaskBinding.root)
                        }
                    } else {
                        visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                // Log any binding errors for debugging
                e.printStackTrace()
            }
        }

       
       
        fun formatTime(dateString: String): String {
         try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(dateString) ?: return ""

        val outputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return outputFormat.format(date)
        } catch (e: Exception) {
        return ""
          }
         }

        private fun expandView(view: View) {
            view.visibility = View.VISIBLE
            
            // Get parent width safely
            val parent = view.parent as? View
            val parentWidth = parent?.width ?: 0
            
            view.measure(
                View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val targetHeight = view.measuredHeight.coerceAtMost(500) // Increased max height

            val anim = ValueAnimator.ofInt(0, targetHeight)
            anim.duration = 300 // Increased duration
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.addUpdateListener { animation ->
                val height = animation.animatedValue as Int
                val layoutParams = view.layoutParams
                layoutParams.height = height
                view.layoutParams = layoutParams
                view.requestLayout()
            }
            anim.start()
        }

        private fun collapseView(view: View) {
            val initialHeight = view.measuredHeight
            val anim = ValueAnimator.ofInt(initialHeight, 0)
            anim.duration = 300
            anim.interpolator = AccelerateDecelerateInterpolator()
            anim.addUpdateListener { animation ->
                val height = animation.animatedValue as Int
                val layoutParams = view.layoutParams
                layoutParams.height = height
                view.layoutParams = layoutParams
                view.requestLayout()
            }
            anim.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    view.visibility = View.GONE
                    val layoutParams = view.layoutParams
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    view.layoutParams = layoutParams
                }
            })
            anim.start()
        }
    }

    // Helper method to get color for task category
    private fun getCategoryColor(category: String?): Int {
        return when (category?.lowercase()) {
            "routine" -> Color.argb(20, 25, 118, 210)  // Light Blue with 20% opacity
            "work" -> Color.argb(20, 46, 125, 50)     // Light Green with 20% opacity
            "study" -> Color.argb(20, 123, 31, 162)   // Light Purple with 20% opacity
            else -> Color.argb(20, 25, 118, 210)      // Default to Light Blue
        }
    }



    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}