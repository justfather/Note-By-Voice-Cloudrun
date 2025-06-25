package com.ppai.voicetotask.di

import com.ppai.voicetotask.data.remote.api.AiService
import com.ppai.voicetotask.data.remote.api.GeminiAiService
import com.ppai.voicetotask.data.remote.api.GeminiSpeechToTextService
import com.ppai.voicetotask.data.remote.api.SpeechToTextService
import dagger.Binds
import dagger.Module
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
    
    @Binds
    @Singleton
    abstract fun bindAiService(
        geminiAiService: GeminiAiService
    ): AiService
}