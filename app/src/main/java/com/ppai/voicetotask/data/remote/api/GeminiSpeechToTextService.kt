package com.ppai.voicetotask.data.remote.api

import android.util.Log
import com.ppai.voicetotask.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiSpeechToTextService @Inject constructor() : SpeechToTextService {
    
    companion object {
        private const val TAG = "GeminiSpeechToText"
        private const val MODEL_NAME = "gemini-1.5-flash"
    }
    
    private val generativeModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    init {
        if (BuildConfig.GEMINI_API_KEY.isEmpty()) {
            Log.e(TAG, "GEMINI_API_KEY is not configured! Please add it to local.properties")
        } else {
            Log.d(TAG, "Gemini API configured with model: $MODEL_NAME")
        }
    }
    
    override suspend fun transcribeAudio(audioFile: File): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting transcription for file: ${audioFile.absolutePath}")
            
            if (!audioFile.exists()) {
                throw IllegalArgumentException("Audio file does not exist: ${audioFile.absolutePath}")
            }
            
            if (audioFile.length() == 0L) {
                throw IllegalArgumentException("Audio file is empty: ${audioFile.absolutePath}")
            }
            
            Log.d(TAG, "Audio file size: ${audioFile.length()} bytes")
            Log.d(TAG, "Audio file format: ${audioFile.extension}")
            
            val audioBytes = audioFile.readBytes()
            
            // Retry logic for API calls
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    Log.d(TAG, "Sending request to Gemini API with model: $MODEL_NAME (attempt ${attempt + 1})")
                    
                    val response = generativeModel.generateContent(
                        content {
                            text("Generate a transcript of the speech in the audio file. Preserve the original language of the speech. Provide only the transcript without any additional commentary or formatting.")
                            // Determine mime type based on file extension
                            val mimeType = when (audioFile.extension.lowercase()) {
                                "m4a" -> "audio/mp4"  // m4a files use mp4 container
                                "mp3" -> "audio/mpeg"
                                "wav" -> "audio/wav"
                                "aac" -> "audio/aac"
                                else -> "audio/mp4"  // Default to mp4 for compatibility
                            }
                            Log.d(TAG, "Using mime type: $mimeType for file extension: ${audioFile.extension}")
                            blob(mimeType, audioBytes)
                        }
                    )
                    
                    val transcript = response.text?.trim()?.takeIf { it.isNotBlank() } 
                        ?: throw Exception("No transcript generated from audio")
                        
                    Log.d(TAG, "Transcription successful. Length: ${transcript.length} characters")
                    return@withContext transcript
                } catch (e: Exception) {
                    lastException = e
                    Log.e(TAG, "Transcription attempt ${attempt + 1} failed", e)
                    
                    // Log more details for debugging
                    when (e) {
                        is GoogleGenerativeAIException -> {
                            Log.e(TAG, "GoogleGenerativeAI Error - Message: ${e.message}")
                            Log.e(TAG, "GoogleGenerativeAI Error - Cause: ${e.cause}")
                            
                            // Check for specific error types
                            when (e.cause) {
                                is java.net.UnknownHostException -> {
                                    throw Exception("ไม่มีการเชื่อมต่ออินเทอร์เน็ต กรุณาตรวจสอบการเชื่อมต่อ")
                                }
                                is java.net.SocketTimeoutException -> {
                                    throw Exception("หมดเวลาการเชื่อมต่อ กรุณาลองใหม่อีกครั้ง")
                                }
                                else -> {
                                    if (e.message?.contains("API key not valid") == true) {
                                        throw Exception("API key ไม่ถูกต้อง กรุณาตรวจสอบการตั้งค่า")
                                    }
                                }
                            }
                        }
                        else -> {
                            Log.e(TAG, "General Error - Type: ${e.javaClass.simpleName}")
                            Log.e(TAG, "General Error - Message: ${e.message}")
                        }
                    }
                    
                    // Check if it's a retryable error
                    val isRetryable = when {
                        e.cause is java.net.UnknownHostException -> false // Don't retry network errors
                        e is GoogleGenerativeAIException -> {
                            val message = e.message ?: ""
                            message.contains("503") || 
                            message.contains("UNAVAILABLE") || 
                            message.contains("500") ||
                            message.contains("429")
                        }
                        else -> true // Retry other errors once
                    }
                    
                    if (isRetryable && attempt < 2) {
                        Log.d(TAG, "Retryable error, retrying after delay...")
                        delay(1000L * (attempt + 1)) // Exponential backoff
                    } else {
                        throw e
                    }
                }
            }
            
            // If we get here, all retries failed
            throw lastException ?: Exception("Failed to transcribe audio after all retries")
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw Exception("Failed to transcribe audio: ${e.message}", e)
            }
        }
    }
}