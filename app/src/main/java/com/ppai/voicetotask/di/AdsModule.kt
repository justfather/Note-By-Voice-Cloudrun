package com.ppai.voicetotask.di

import android.content.Context
import com.ppai.voicetotask.data.ads.AdManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdsModule {
    
    @Provides
    @Singleton
    fun provideAdManager(
        @ApplicationContext context: Context
    ): AdManager {
        return AdManager(context)
    }
}