package com.ppai.voicetotask.di

import android.content.Context
import com.ppai.voicetotask.data.billing.BillingManager
import com.ppai.voicetotask.data.preferences.SubscriptionPreferences
import com.ppai.voicetotask.data.repository.SubscriptionRepositoryImpl
import com.ppai.voicetotask.domain.repository.SubscriptionRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    
    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context
    ): BillingManager {
        return BillingManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSubscriptionPreferences(
        @ApplicationContext context: Context
    ): SubscriptionPreferences {
        return SubscriptionPreferences(context)
    }
    
    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        subscriptionPreferences: SubscriptionPreferences,
        billingManager: BillingManager
    ): SubscriptionRepository {
        return SubscriptionRepositoryImpl(subscriptionPreferences, billingManager)
    }
}