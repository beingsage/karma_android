package com.technource.android.eTMS.macro

import com.technource.android.local.Task
import com.technource.android.utils.DateFormatter
import java.time.format.DateTimeParseException

data class ValidationResult(val isValid: Boolean, val message: String)

object TaskValidator {
    fun validateTasks(tasks: List<Task>): ValidationResult {
        if (tasks.isEmpty()) {
            return ValidationResult(false, "Task list is empty")
        }

        tasks.forEachIndexed { index, task ->
            // Check required fields
            if (task.id.isBlank()) {
                return ValidationResult(false, "Task at index $index has empty ID")
            }
            if (task.title.isBlank()) {
                return ValidationResult(false, "Task at index $index has empty title")
            }
            if (task.startTime.isBlank()) {
                return ValidationResult(false, "Task at index $index has empty start time")
            }
            if (task.endTime.isBlank()) {
                return ValidationResult(false, "Task at index $index has empty end time")
            }

            // Validate date formats
            try {
                DateFormatter.parseIsoDateTime(task.startTime)
                DateFormatter.parseIsoDateTime(task.endTime)
            } catch (e: DateTimeParseException) {
                return ValidationResult(false, "Task at index $index has invalid date format: ${e.message}")
            }

            // Validate start time is before end time
            val startTime = DateFormatter.parseIsoDateTime(task.startTime)
            val endTime = DateFormatter.parseIsoDateTime(task.endTime)
            if (!startTime.isBefore(endTime)) {
                return ValidationResult(false, "Task at index $index has start time after or equal to end time")
            }

            // Validate score (if applicable)
//            if (task.score < 0) {
//                return ValidationResult(false, "Task at index $index has negative score")
//            }
        }

        return ValidationResult(true, "Tasks are valid")
    }
}