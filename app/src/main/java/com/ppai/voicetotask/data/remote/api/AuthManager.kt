package com.ppai.voicetotask.data.remote.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import com.auth0.android.jwt.JWT

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backendApiService: BackendApiService
) {
    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_IS_PREMIUM = stringPreferencesKey("is_premium")
    }
    
    private val authDataStore = context.authDataStore
    
    val authToken: Flow<String?> = authDataStore.data
        .map { preferences -> preferences[KEY_AUTH_TOKEN] }
    
    val userId: Flow<String?> = authDataStore.data
        .map { preferences -> preferences[KEY_USER_ID] }
    
    val userEmail: Flow<String?> = authDataStore.data
        .map { preferences -> preferences[KEY_USER_EMAIL] }
    
    val isPremium: Flow<Boolean> = authDataStore.data
        .map { preferences -> preferences[KEY_IS_PREMIUM]?.toBoolean() ?: false }
    
    suspend fun login(email: String, password: String): AuthResponse {
        val response = backendApiService.login(AuthRequest(email, password))
        saveAuthData(response.data)
        return response
    }
    
    suspend fun register(email: String, password: String): AuthResponse {
        val response = backendApiService.register(AuthRequest(email, password))
        saveAuthData(response.data)
        return response
    }
    
    suspend fun logout() {
        authDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun getValidToken(): String? {
        val token = authToken.firstOrNull()
        
        if (token == null) return null
        
        // Check if token is expired
        return try {
            val jwt = JWT(token)
            val expiresAt = jwt.expiresAt
            
            if (expiresAt != null && expiresAt.before(Date())) {
                // Token is expired, try to refresh
                refreshToken()
            } else {
                token
            }
        } catch (e: Exception) {
            // If JWT parsing fails, try to refresh
            refreshToken()
        }
    }
    
    private suspend fun refreshToken(): String? {
        val userId = userId.firstOrNull() ?: return null
        val email = userEmail.firstOrNull() ?: return null
        
        return try {
            val response = backendApiService.refreshToken(
                RefreshTokenRequest(userId, email)
            )
            
            authDataStore.edit { preferences ->
                preferences[KEY_AUTH_TOKEN] = response.data.token
            }
            
            response.data.token
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveAuthData(authData: AuthData) {
        authDataStore.edit { preferences ->
            preferences[KEY_AUTH_TOKEN] = authData.token
            preferences[KEY_USER_ID] = authData.user.id
            preferences[KEY_USER_EMAIL] = authData.user.email
            preferences[KEY_IS_PREMIUM] = authData.user.isPremium.toString()
        }
    }
    
    suspend fun isLoggedIn(): Boolean {
        return getValidToken() != null
    }
}