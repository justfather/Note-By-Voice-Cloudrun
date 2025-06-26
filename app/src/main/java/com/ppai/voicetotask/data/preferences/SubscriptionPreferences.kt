package com.ppai.voicetotask.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ppai.voicetotask.domain.model.Subscription
import com.ppai.voicetotask.domain.model.SubscriptionTier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private val Context.subscriptionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "subscription_preferences"
)

@Singleton
class SubscriptionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_TIER = stringPreferencesKey("subscription_tier")
        private val KEY_PURCHASE_TOKEN = stringPreferencesKey("purchase_token")
        private val KEY_START_DATE = longPreferencesKey("start_date")
        private val KEY_EXPIRY_DATE = longPreferencesKey("expiry_date")
        private val KEY_IS_IN_GRACE_PERIOD = booleanPreferencesKey("is_in_grace_period")
        private val KEY_RECORDINGS_THIS_MONTH = intPreferencesKey("recordings_this_month")
        private val KEY_LAST_RESET_DATE = longPreferencesKey("last_reset_date")
        private val KEY_IS_TEST_USER = booleanPreferencesKey("is_test_user")
    }
    
    val subscriptionFlow: Flow<Subscription> = context.subscriptionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferencesToSubscription(preferences)
        }
    
    private fun mapPreferencesToSubscription(preferences: Preferences): Subscription {
        return Subscription(
            userId = preferences[KEY_USER_ID] ?: "",
            tier = preferences[KEY_TIER]?.let { 
                try {
                    SubscriptionTier.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    SubscriptionTier.FREE
                }
            } ?: SubscriptionTier.FREE,
            purchaseToken = preferences[KEY_PURCHASE_TOKEN],
            startDate = preferences[KEY_START_DATE]?.let { Date(it) },
            expiryDate = preferences[KEY_EXPIRY_DATE]?.let { Date(it) },
            isInGracePeriod = preferences[KEY_IS_IN_GRACE_PERIOD] ?: false,
            recordingsThisMonth = preferences[KEY_RECORDINGS_THIS_MONTH] ?: 0,
            lastResetDate = Date(preferences[KEY_LAST_RESET_DATE] ?: System.currentTimeMillis()),
            isTestUser = preferences[KEY_IS_TEST_USER] ?: false
        )
    }
    
    suspend fun updateSubscription(subscription: Subscription) {
        context.subscriptionDataStore.edit { preferences ->
            preferences[KEY_USER_ID] = subscription.userId
            preferences[KEY_TIER] = subscription.tier.name
            subscription.purchaseToken?.let { preferences[KEY_PURCHASE_TOKEN] = it }
            subscription.startDate?.let { preferences[KEY_START_DATE] = it.time }
            subscription.expiryDate?.let { preferences[KEY_EXPIRY_DATE] = it.time }
            preferences[KEY_IS_IN_GRACE_PERIOD] = subscription.isInGracePeriod
            preferences[KEY_RECORDINGS_THIS_MONTH] = subscription.recordingsThisMonth
            preferences[KEY_LAST_RESET_DATE] = subscription.lastResetDate.time
            preferences[KEY_IS_TEST_USER] = subscription.isTestUser
        }
    }
    
    suspend fun incrementRecordingCount() {
        context.subscriptionDataStore.edit { preferences ->
            val currentCount = preferences[KEY_RECORDINGS_THIS_MONTH] ?: 0
            preferences[KEY_RECORDINGS_THIS_MONTH] = currentCount + 1
        }
    }
    
    suspend fun resetMonthlyQuota() {
        context.subscriptionDataStore.edit { preferences ->
            preferences[KEY_RECORDINGS_THIS_MONTH] = 0
            preferences[KEY_LAST_RESET_DATE] = System.currentTimeMillis()
        }
    }
    
    suspend fun getCurrentRecordingCount(): Int {
        return context.subscriptionDataStore.data.first()[KEY_RECORDINGS_THIS_MONTH] ?: 0
    }
    
    suspend fun clearSubscription() {
        context.subscriptionDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}