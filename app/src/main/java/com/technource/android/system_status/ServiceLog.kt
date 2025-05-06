package com.technource.android.system_status

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Entity(tableName = "service_logs")
data class ServiceLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serviceName: String,
    val timestamp: Long,
    val log: String,
    val status: String = "N/A"
)

@Dao
interface ServiceLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ServiceLog)

    @Query("SELECT * FROM service_logs WHERE serviceName = :serviceName ORDER BY timestamp DESC")
    fun getLogs(serviceName: String): LiveData<List<ServiceLog>>

    @Query("SELECT * FROM service_logs ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<ServiceLog>>

    @Query("SELECT DISTINCT serviceName FROM service_logs")
    fun getAllServiceNames(): LiveData<List<String>>

    @Query("DELETE FROM service_logs WHERE timestamp < :timeThreshold")
    suspend fun deleteOldLogs(timeThreshold: Long)

    @Query("DELETE FROM service_logs WHERE serviceName = :serviceName")
    suspend fun deleteLogsForService(serviceName: String)

    @Query("DELETE FROM service_logs")
    suspend fun deleteAllLogs()
}

@Database(entities = [ServiceLog::class], version = 1)
abstract class ServiceLogDatabase : RoomDatabase() {
    abstract fun serviceLogDao(): ServiceLogDao

    companion object {
        @Volatile private var instance: ServiceLogDatabase? = null

        fun getDatabase(context: Context): ServiceLogDatabase {
            return instance ?: synchronized(this) {
                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    ServiceLogDatabase::class.java, "service_logs.db"
                ).build()
                instance = newInstance
                newInstance
            }
        }
    }
}