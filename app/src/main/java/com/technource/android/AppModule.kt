package com.technource.android

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.technource.android.local.AppDatabase
import com.technource.android.local.TaskDao
import com.technource.android.network.ApiService
import com.technource.android.eTMS.micro.TaskIterator
import com.technource.android.utils.PreferencesManager
import com.technource.android.utils.TTSManager
import com.technource.android.utils.VoiceAssistantManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTaskIterator(@ApplicationContext context: Context?, taskDao: TaskDao, ttsManager: TTSManager): TaskIterator {
        return TaskIterator(context!!, taskDao, ttsManager )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideTTSManager(@ApplicationContext context: Context?): TTSManager {
        return TTSManager(context!!)
    }

    @Provides
    @Singleton
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()


    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        PreferencesManager.init(context)
        return PreferencesManager
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return Retrofit.Builder()
            .baseUrl("https://karma-backend-zyft.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVoiceAssistantManager(@ApplicationContext context: Context): VoiceAssistantManager {
        return VoiceAssistantManager(context)
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

    // Add this provider method
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }
}