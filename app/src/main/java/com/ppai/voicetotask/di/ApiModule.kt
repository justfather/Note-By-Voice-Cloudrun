package com.ppai.voicetotask.di

import com.ppai.voicetotask.data.remote.api.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiModule {
    
    @Binds
    @Singleton
    abstract fun bindSpeechToTextService(
        geminiSpeechToTextService: GeminiSpeechToTextService
    ): SpeechToTextService
    
    companion object {
        @Provides
        @Singleton
        fun provideAiService(
            geminiAiService: GeminiAiService,
            backendAiService: BackendAiService
        ): AiService {
            return if (FeatureFlags.USE_BACKEND_API) {
                backendAiService
            } else {
                geminiAiService
            }
        }
    }
}