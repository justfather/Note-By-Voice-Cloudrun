package com.ppai.voicetotask.presentation.viewmodel

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.data.preferences.ThemeMode
import com.ppai.voicetotask.data.preferences.UserPreferencesData
import com.ppai.voicetotask.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    
    val userPreferences: StateFlow<UserPreferencesData> = settingsRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesData()
        )
}

@Composable
fun UserPreferencesData.shouldUseDarkTheme(systemInDarkTheme: Boolean = isSystemInDarkTheme()): Boolean {
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}