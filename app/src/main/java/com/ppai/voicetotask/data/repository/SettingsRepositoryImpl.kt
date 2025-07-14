package com.ppai.voicetotask.data.repository

import com.ppai.voicetotask.data.preferences.OutputLanguage
import com.ppai.voicetotask.data.preferences.ThemeMode
import com.ppai.voicetotask.data.preferences.UserPreferences
import com.ppai.voicetotask.data.preferences.UserPreferencesData
import com.ppai.voicetotask.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val userPreferencesDataStore: UserPreferences
) : SettingsRepository {
    
    override val userPreferences: Flow<UserPreferencesData> = userPreferencesDataStore.userPreferencesFlow
    
    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        userPreferencesDataStore.updateThemeMode(themeMode)
    }
    
    override suspend fun updateDynamicColors(enabled: Boolean) {
        userPreferencesDataStore.updateDynamicColors(enabled)
    }
    
    override suspend fun updateOutputLanguage(language: OutputLanguage) {
        userPreferencesDataStore.updateOutputLanguage(language)
    }
}