package com.ppai.voicetotask.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class OutputLanguage(val code: String, val displayName: String) {
    AUTO("auto", "Auto-detect"),
    ENGLISH("en", "English"),
    THAI("th", "ไทย"),
    CHINESE("zh", "中文"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN("ru", "Русский"),
    ARABIC("ar", "العربية"),
    HINDI("hi", "हिन्दी")
}

data class UserPreferencesData(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = true,
    val outputLanguage: OutputLanguage = OutputLanguage.AUTO
)

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        val OUTPUT_LANGUAGE = stringPreferencesKey("output_language")
    }
    
    val userPreferencesFlow: Flow<UserPreferencesData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapPreferences(preferences)
        }
    
    suspend fun updateThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[Keys.THEME_MODE] = themeMode.name
        }
    }
    
    suspend fun updateDynamicColors(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.DYNAMIC_COLORS] = enabled
        }
    }
    
    suspend fun updateOutputLanguage(language: OutputLanguage) {
        dataStore.edit { preferences ->
            preferences[Keys.OUTPUT_LANGUAGE] = language.code
        }
    }
    
    private fun mapPreferences(preferences: Preferences): UserPreferencesData {
        val themeMode = preferences[Keys.THEME_MODE]?.let { 
            try {
                ThemeMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } ?: ThemeMode.SYSTEM
        
        val dynamicColors = preferences[Keys.DYNAMIC_COLORS] ?: true
        
        val outputLanguage = preferences[Keys.OUTPUT_LANGUAGE]?.let { code ->
            OutputLanguage.values().find { it.code == code }
        } ?: OutputLanguage.AUTO
        
        return UserPreferencesData(
            themeMode = themeMode,
            dynamicColors = dynamicColors,
            outputLanguage = outputLanguage
        )
    }
}