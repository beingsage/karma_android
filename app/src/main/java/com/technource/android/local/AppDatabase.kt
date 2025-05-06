package com.technource.android.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.technource.android.ETMS.macro.DefaultTaskDao
import com.technource.android.ETMS.macro.DefaultTaskEntity

@Database(entities = [TaskEntity::class, DefaultTaskEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun defaultTaskDao(): DefaultTaskDao

    companion object {
        private var instance: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                ).fallbackToDestructiveMigration() // Drop and recreate database on version change
                    .build()
                    .also { instance = it }
            }
        }
    }
}