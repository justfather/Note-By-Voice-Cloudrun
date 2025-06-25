package com.ppai.voicetotask.data.remote.api

import java.io.File

interface SpeechToTextService {
    suspend fun transcribeAudio(audioFile: File): String
}