package com.qrishook.app.di

import com.qrishook.app.data.prefs.EncryptedSecurePrefs
import com.qrishook.app.data.prefs.SecurePrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurePrefsModule {
    @Provides
    @Singleton
    fun provideSecurePrefs(impl: EncryptedSecurePrefs): SecurePrefs = impl
}
