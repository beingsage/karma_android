package com.technource.android.network

import com.technource.android.local.TaskResponse
import com.technource.android.module.settingsModule.data.ApiStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * Retrofit API service for interacting with the backend.
 * - Represents: API endpoints for fetching and sending tasks.
 * - Uses DateFormatter: To format the date query parameter as ISO 8601 string.
 */
interface ApiService {
    /**
     * Fetches tasks for a specific date from the backend.
     * @param date ISO 8601 string representing the date (e.g., "2025-05-02T00:00:00.000Z").
     * @return TaskResponse containing the tasks for the specified date.
     * Use Case:
     * - Used to fetch tasks for a specific day to display in HomeScreen or calculate stats in StatsScreen.
     * - DateFormatter: Use formatIsoDateTime to format the date parameter.
     * Example:
     * - val date = DateFormatter.formatIsoDateTime(LocalDateTime.now())
     * - apiService.getTasks(date)
     */
    @GET("api/timetable")
    suspend fun getTasks(@Query("date") date: String): TaskResponse

    /**
     * Sends tasks to the backend to update the timetable.
     * @param taskResponse TaskResponse object containing the tasks to send.
     * @return Response<Void> indicating success or failure.
     * Use Case:
     * - Used to sync local tasks with the backend after modifications.
     * - DateFormatter: TaskResponse.data.date should be formatted with formatIsoDateTime.
     */
    @PUT("api/timetable")
    suspend fun sendTasks(@Body taskResponse: TaskResponse): Response<TaskResponse>

    /**
     * Fetches default tasks from the backend.
     * @return TaskResponse containing the default tasks.
     * Use Case:
     * - Used to fetch a default timetable (e.g., for initializing the app).
     * - DateFormatter: Not directly used here, but the response's Task objects will use it in toTaskEntity().
     */
    @GET("api/default_time_table/utc")
    suspend fun getDefaultTasks(): TaskResponse

    /**
     * Fetches the API status from the backend.
     * @return ApiStatus object containing the status of the API.
     * Use Case:
     * - Used to check the health or status of the API (e.g., for debugging or monitoring).
     */
    @GET("/")
    suspend fun getApiStatus(): ApiStatus
}