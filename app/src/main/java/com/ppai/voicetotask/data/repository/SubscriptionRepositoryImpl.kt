package com.ppai.voicetotask.data.repository

import com.ppai.voicetotask.BuildConfig
import com.ppai.voicetotask.data.billing.BillingManager
import com.ppai.voicetotask.data.preferences.SubscriptionPreferences
import com.ppai.voicetotask.domain.model.Subscription
import com.ppai.voicetotask.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionPreferences: SubscriptionPreferences,
    private val billingManager: BillingManager
) : SubscriptionRepository {
    
    override fun getCurrentSubscription(): Flow<Subscription> {
        // Combine local preferences with billing manager state
        return combine(
            subscriptionPreferences.subscriptionFlow,
            billingManager.currentSubscription
        ) { localSub, billingSub ->
            when {
                // Check if test user
                isTestUser() -> localSub.copy(
                    isTestUser = true,
                    tier = com.ppai.voicetotask.domain.model.SubscriptionTier.PREMIUM
                )
                // Use billing subscription if available
                billingSub != null -> {
                    // Update local storage
                    subscriptionPreferences.updateSubscription(billingSub)
                    billingSub
                }
                // Otherwise use local subscription
                else -> {
                    // Check if monthly quota needs reset
                    if (shouldResetMonthlyQuota(localSub.lastResetDate)) {
                        subscriptionPreferences.resetMonthlyQuota()
                        localSub.copy(recordingsThisMonth = 0, lastResetDate = Date())
                    } else {
                        localSub
                    }
                }
            }
        }
    }
    
    override suspend fun updateSubscription(subscription: Subscription) {
        subscriptionPreferences.updateSubscription(subscription)
    }
    
    override suspend fun incrementRecordingCount() {
        val currentSub = subscriptionPreferences.subscriptionFlow.first()
        if (currentSub.tier == com.ppai.voicetotask.domain.model.SubscriptionTier.FREE) {
            subscriptionPreferences.incrementRecordingCount()
        }
    }
    
    override suspend fun resetMonthlyQuota() {
        subscriptionPreferences.resetMonthlyQuota()
    }
    
    override suspend fun isRecordingAllowed(): Boolean {
        val subscription = getCurrentSubscription().first()
        return subscription.canRecord()
    }
    
    override suspend fun getRemainingRecordings(): Int {
        val subscription = getCurrentSubscription().first()
        return subscription.getRemainingRecordings()
    }
    
    override suspend fun getMaxRecordingDuration(): Long {
        val subscription = getCurrentSubscription().first()
        return subscription.getMaxRecordingDuration()
    }
    
    private fun shouldResetMonthlyQuota(lastResetDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.time = lastResetDate
        val lastResetMonth = calendar.get(Calendar.MONTH)
        val lastResetYear = calendar.get(Calendar.YEAR)
        
        return currentYear > lastResetYear || 
               (currentYear == lastResetYear && currentMonth > lastResetMonth)
    }
    
    private fun isTestUser(): Boolean {
        // For now, we'll check if the test user email is configured
        // In a real app, you'd check against the actual logged-in user
        return BuildConfig.TEST_USER_EMAIL.isNotEmpty()
    }
}