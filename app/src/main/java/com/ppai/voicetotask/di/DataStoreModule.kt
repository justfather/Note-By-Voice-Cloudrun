package com.ppai.voicetotask.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.ppai.voicetotask.data.preferences.UserPreferences
import com.ppai.voicetotask.data.preferences.userPreferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    
    @Provides
    @Singleton
    fun provideUserPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.userPreferencesDataStore
    }
    
    @Provides
    @Singleton
    fun provideUserPreferences(
        dataStore: DataStore<Preferences>
    ): UserPreferences {
        return UserPreferences(dataStore)
    }
}