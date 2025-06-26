package com.ppai.voicetotask.domain.model

import java.util.Date

data class Subscription(
    val userId: String,
    val tier: SubscriptionTier,
    val purchaseToken: String? = null,
    val startDate: Date? = null,
    val expiryDate: Date? = null,
    val isInGracePeriod: Boolean = false,
    val recordingsThisMonth: Int = 0,
    val lastResetDate: Date = Date(),
    val isTestUser: Boolean = false
) {
    fun isPremium(): Boolean = tier == SubscriptionTier.PREMIUM || isTestUser
    
    fun isExpired(): Boolean {
        return when {
            isTestUser -> false
            expiryDate == null -> false
            isInGracePeriod -> false
            else -> Date().after(expiryDate)
        }
    }
    
    fun canRecord(): Boolean {
        return when (tier) {
            SubscriptionTier.FREE -> recordingsThisMonth < FREE_MONTHLY_LIMIT
            SubscriptionTier.PREMIUM -> true
        }
    }
    
    fun getRemainingRecordings(): Int {
        return when (tier) {
            SubscriptionTier.FREE -> (FREE_MONTHLY_LIMIT - recordingsThisMonth).coerceAtLeast(0)
            SubscriptionTier.PREMIUM -> Int.MAX_VALUE
        }
    }
    
    fun getMaxRecordingDuration(): Long {
        return when (tier) {
            SubscriptionTier.FREE -> FREE_MAX_DURATION_MS
            SubscriptionTier.PREMIUM -> PREMIUM_MAX_DURATION_MS
        }
    }
    
    companion object {
        const val FREE_MONTHLY_LIMIT = 30
        const val FREE_MAX_DURATION_MS = 2 * 60 * 1000L // 2 minutes
        const val PREMIUM_MAX_DURATION_MS = 10 * 60 * 1000L // 10 minutes
        const val GRACE_PERIOD_DAYS = 7
        
        val DEFAULT_FREE = Subscription(
            userId = "",
            tier = SubscriptionTier.FREE,
            recordingsThisMonth = 0,
            lastResetDate = Date()
        )
    }
}

enum class SubscriptionTier {
    FREE,
    PREMIUM
}

enum class SubscriptionType(val skuId: String, val price: String) {
    MONTHLY("premium_monthly", "$4.99/month"),
    YEARLY("premium_yearly", "$39.99/year")
}