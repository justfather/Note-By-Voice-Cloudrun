package com.ppai.voicetotask.util

import android.os.Bundle
import android.util.Log

object AnalyticsEvents {
    private const val TAG = "Analytics"
    
    // Event names
    const val PAYWALL_SHOWN = "paywall_shown"
    const val PAYWALL_DISMISSED = "paywall_dismissed"
    const val SUBSCRIPTION_STARTED = "subscription_started"
    const val SUBSCRIPTION_CANCELLED = "subscription_cancelled"
    const val AD_SHOWN = "ad_shown"
    const val AD_COMPLETED = "ad_completed"
    const val AD_FAILED = "ad_failed"
    const val RECORDING_LIMIT_REACHED = "recording_limit_reached"
    const val RESTORE_PURCHASE_CLICKED = "restore_purchase_clicked"
    const val WHY_PREMIUM_CLICKED = "why_premium_clicked"
    const val GRACE_PERIOD_STARTED = "grace_period_started"
    
    // Parameter keys
    const val PARAM_SOURCE = "source"
    const val PARAM_SUBSCRIPTION_TYPE = "subscription_type"
    const val PARAM_AD_TYPE = "ad_type"
    const val PARAM_ERROR_MESSAGE = "error_message"
    const val PARAM_LIMIT_TYPE = "limit_type"
    
    fun logEvent(eventName: String, params: Bundle? = null) {
        // For now, just log to console. Later can integrate with Firebase Analytics
        Log.d(TAG, "Event logged: $eventName, params: $params")
    }
    
    // Convenience methods for common events
    fun logPaywallShown(source: String) {
        val params = Bundle().apply {
            putString(PARAM_SOURCE, source)
        }
        logEvent(PAYWALL_SHOWN, params)
    }
    
    fun logPaywallDismissed() {
        logEvent(PAYWALL_DISMISSED)
    }
    
    fun logSubscriptionStarted(subscriptionType: String) {
        val params = Bundle().apply {
            putString(PARAM_SUBSCRIPTION_TYPE, subscriptionType)
        }
        logEvent(SUBSCRIPTION_STARTED, params)
    }
    
    fun logAdShown(adType: String) {
        val params = Bundle().apply {
            putString(PARAM_AD_TYPE, adType)
        }
        logEvent(AD_SHOWN, params)
    }
    
    fun logAdCompleted(adType: String) {
        val params = Bundle().apply {
            putString(PARAM_AD_TYPE, adType)
        }
        logEvent(AD_COMPLETED, params)
    }
    
    fun logAdFailed(adType: String, error: String) {
        val params = Bundle().apply {
            putString(PARAM_AD_TYPE, adType)
            putString(PARAM_ERROR_MESSAGE, error)
        }
        logEvent(AD_FAILED, params)
    }
    
    fun logRecordingLimitReached(limitType: String) {
        val params = Bundle().apply {
            putString(PARAM_LIMIT_TYPE, limitType)
        }
        logEvent(RECORDING_LIMIT_REACHED, params)
    }
}