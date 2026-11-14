package com.qrishook.app.di

import com.qrishook.app.domain.webhook.OkHttpWebhookClient
import com.qrishook.app.domain.webhook.WebhookClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideWebhookClient(client: OkHttpClient): WebhookClient = OkHttpWebhookClient(client)
}
