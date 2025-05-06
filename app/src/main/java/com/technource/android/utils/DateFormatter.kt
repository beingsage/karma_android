package com.technource.android.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatter {
    // Define Indian Standard Time (IST) zone
    val IST_ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

    // Formatter for ISO 8601 format (e.g., "2025-05-02T14:30:00.000Z")
    private val isoFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .withZone(IST_ZONE)

    // Formatter for display time (e.g., "8:40 pm")
    private val displayTimeFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("h:mm a", Locale.getDefault())
        .withZone(IST_ZONE)

    // Formatter for display date (e.g., "May 2")
    private val displayDateFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MMMM d", Locale.getDefault())
        .withZone(IST_ZONE)

    /**
     * Converts a LocalDateTime to an ISO 8601 string in IST.
     * @param dateTime The LocalDateTime to format (assumed to be in IST).
     * @return ISO 8601 string (e.g., "2025-05-02T14:30:00.000Z").
     * Use Cases:
     * - Serialize a LocalDateTime to a string for storage in a database (e.g., Room, MongoDB).
     * - Convert a LocalDateTime to a string for JSON serialization with Gson (e.g., sending to an API).
     * - Example: When creating a Task object to send to a backend API that expects ISO 8601 strings.
     */
    fun formatIsoDateTime(dateTime: LocalDateTime): String {
        val zonedDateTime = dateTime.atZone(IST_ZONE)
        return isoFormatter.format(zonedDateTime)
    }

    /**
     * Parses an ISO 8601 string to a LocalDateTime in IST.
     * @param isoString The ISO 8601 string (e.g., "2025-05-02T14:30:00.000Z").
     * @return LocalDateTime in IST.
     * Use Cases:
     * - Parse a date-time string from a database (e.g., MongoDB ISODate) or API response.
     * - Convert a JSON string (parsed with Gson) to a LocalDateTime for internal use.
     * - Example: When receiving a Task object from an API with startTime/endTime as ISO 8601 strings.
     * Notes:
     * - Ensure the input string matches the ISO 8601 format with milliseconds and 'Z' (e.g., "2025-05-02T14:30:00.000Z").
     * - Throws DateTimeParseException if the input string format is invalid.
     */
    fun parseIsoDateTime(isoString: String): LocalDateTime {
        val zonedDateTime = ZonedDateTime.parse(isoString, isoFormatter)
        return zonedDateTime.withZoneSameInstant(IST_ZONE).toLocalDateTime()
    }

    /**
     * Converts epoch milliseconds to a LocalDateTime in IST.
     * @param millis Epoch milliseconds (e.g., System.currentTimeMillis() output).
     * @return LocalDateTime in IST.
     * Use Cases:
     * - Convert a timestamp (e.g., from TaskEntity.startTime) to a LocalDateTime for manipulation.
     * - Use when you need to perform date-time operations (e.g., extracting hour, adding minutes) on a timestamp.
     * - Example: In StatsViewModel to group tasks by hour for chart data.
     */
    fun millisToLocalDateTime(millis: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(millis / 1000, 0, IST_ZONE.rules.getOffset(LocalDateTime.now()))
            .atZone(IST_ZONE)
            .toLocalDateTime()
    }

    /**
     * Converts a LocalDateTime in IST to epoch milliseconds.
     * @param dateTime LocalDateTime in IST.
     * @return Epoch milliseconds (e.g., a Long value suitable for System.currentTimeMillis() comparison).
     * Use Cases:
     * - Convert a LocalDateTime to a timestamp for storage in a database (e.g., Room TaskEntity.startTime).
     * - Use when comparing dates in epoch time (e.g., in TaskDao queries).
     * - Example: In TaskPopulator to set startTime and endTime as Long values in TaskEntity.
     */
    fun localDateTimeToMillis(dateTime: LocalDateTime): Long {
        return dateTime.atZone(IST_ZONE).toInstant().toEpochMilli()
    }

    /**
     * Formats a LocalDateTime for display as a time string in IST (e.g., "8:40 pm").
     * @param dateTime LocalDateTime in IST.
     * @return Formatted time string (e.g., "8:40 pm").
     * Use Cases:
     * - Display a LocalDateTime in the UI (e.g., in HomeScreen for task times).
     * - Use after parsing or manipulating a LocalDateTime to show the user-friendly time.
     * - Example: In TaskAdapter to display a task's start time.
     */
    fun formatDisplayTime(dateTime: LocalDateTime): String {
        val zonedDateTime = dateTime.atZone(IST_ZONE)
        return displayTimeFormatter.format(zonedDateTime)
    }

    /**
     * Formats a LocalDateTime for display as a date string in IST (e.g., "May 2").
     * @param dateTime LocalDateTime in IST.
     * @return Formatted date string (e.g., "May 2").
     * Use Cases:
     * - Display a LocalDateTime as a date in the UI (e.g., in StatsScreen for the header date).
     * - Use when showing a date without the time component.
     * - Example: In StatsScreen to set the date text at the top.
     */
    fun formatDisplayDate(dateTime: LocalDateTime): String {
        val zonedDateTime = dateTime.atZone(IST_ZONE)
        return displayDateFormatter.format(zonedDateTime)
    }

    /**
     * Formats epoch milliseconds for display as a time string in IST (e.g., "8:40 pm").
     * @param millis Epoch milliseconds (e.g., TaskEntity.startTime).
     * @return Formatted time string (e.g., "8:40 pm").
     * Use Cases:
     * - Display a timestamp directly in the UI without intermediate conversion to LocalDateTime.
     * - Use when TaskEntity stores startTime/endTime as Long and you need to display it.
     * - Example: In TaskAdapter to display a task's start time directly from TaskEntity.startTime.
     */
    fun formatDisplayTimeFromMillis(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return formatDisplayTime(dateTime)
    }

    /**
     * Formats epoch milliseconds for display as a date string in IST (e.g., "May 2").
     * @param millis Epoch milliseconds (e.g., System.currentTimeMillis()).
     * @return Formatted date string (e.g., "May 2").
     * Use Cases:
     * - Display a timestamp as a date in the UI.
     * - Use when you have a timestamp and need to show only the date part.
     * - Example: In StatsScreen to display the current date from System.currentTimeMillis().
     */
    fun formatDisplayDateFromMillis(millis: Long): String {
        val dateTime = millisToLocalDateTime(millis)
        return formatDisplayDate(dateTime)
    }

    /**
     * Gets the start of the current day in IST as epoch milliseconds.
     * @return Epoch milliseconds for 00:00:00 in IST (e.g., for May 2, 2025, returns 1714597200000).
     * Use Cases:
     * - Define the start of the day for querying tasks in a specific date range.
     * - Use in TaskDao queries to fetch tasks for the current day.
     * - Example: In StatsViewModel to set the start time for getTodayTasksFlow.
     */
    fun getStartOfDayMillis(): Long {
        val startOfDay = LocalDateTime.now(IST_ZONE)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return localDateTimeToMillis(startOfDay)
    }

    /**
     * Gets the start of the next day in IST as epoch milliseconds.
     * @return Epoch milliseconds for 00:00:00 of the next day in IST (e.g., for May 3, 2025, returns 1714683600000).
     * Use Cases:
     * - Define the end of the current day (start of the next day) for querying tasks in a specific date range.
     * - Use in TaskDao queries to fetch tasks for the current day.
     * - Example: In StatsViewModel to set the end time for getTodayTasksFlow.
     */
    fun getStartOfNextDayMillis(): Long {
        val startOfNextDay = LocalDateTime.now(IST_ZONE)
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return localDateTimeToMillis(startOfNextDay)
    }
}