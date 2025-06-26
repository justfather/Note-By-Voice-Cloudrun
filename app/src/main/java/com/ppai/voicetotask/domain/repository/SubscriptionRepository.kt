package com.ppai.voicetotask.domain.repository

import com.ppai.voicetotask.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    fun getCurrentSubscription(): Flow<Subscription>
    suspend fun updateSubscription(subscription: Subscription)
    suspend fun incrementRecordingCount()
    suspend fun resetMonthlyQuota()
    suspend fun isRecordingAllowed(): Boolean
    suspend fun getRemainingRecordings(): Int
    suspend fun getMaxRecordingDuration(): Long
}