package com.ppai.voicetotask.domain.repository

import com.ppai.voicetotask.data.preferences.OutputLanguage
import com.ppai.voicetotask.data.preferences.ThemeMode
import com.ppai.voicetotask.data.preferences.UserPreferencesData
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userPreferences: Flow<UserPreferencesData>
    
    suspend fun updateThemeMode(themeMode: ThemeMode)
    suspend fun updateDynamicColors(enabled: Boolean)
    suspend fun updateOutputLanguage(language: OutputLanguage)
}