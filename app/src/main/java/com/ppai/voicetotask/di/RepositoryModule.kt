package com.ppai.voicetotask.di

import com.ppai.voicetotask.data.repository.NoteRepositoryImpl
import com.ppai.voicetotask.data.repository.SettingsRepositoryImpl
import com.ppai.voicetotask.data.repository.TaskRepositoryImpl
import com.ppai.voicetotask.domain.repository.NoteRepository
import com.ppai.voicetotask.domain.repository.SettingsRepository
import com.ppai.voicetotask.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository
    
    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository
    
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}