package com.qrishook.app.di

import android.content.Context
import androidx.room.Room
import com.qrishook.app.data.db.AppDatabase
import com.qrishook.app.data.db.AutomationDao
import com.qrishook.app.data.db.LogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun provideAutomationDao(db: AppDatabase): AutomationDao = db.automationDao()

    @Provides
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()
}
