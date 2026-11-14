package com.qrishook.app.di

import com.qrishook.app.data.db.AutomationDao
import com.qrishook.app.data.db.LogDao
import com.qrishook.app.data.repository.AutomationRepository
import com.qrishook.app.data.repository.LogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideAutomationRepository(dao: AutomationDao): AutomationRepository =
        AutomationRepository(dao)

    @Provides
    @Singleton
    fun provideLogRepository(dao: LogDao): LogRepository = LogRepository(dao)
}
