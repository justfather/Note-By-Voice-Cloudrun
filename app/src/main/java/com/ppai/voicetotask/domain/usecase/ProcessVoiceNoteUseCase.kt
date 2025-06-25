package com.ppai.voicetotask.domain.usecase

import android.media.MediaMetadataRetriever
import com.ppai.voicetotask.data.remote.api.AiService
import com.ppai.voicetotask.data.remote.api.GeminiAiService
import com.ppai.voicetotask.data.remote.api.SpeechToTextService
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.model.Priority
import com.ppai.voicetotask.domain.model.Task
import java.io.File
import java.util.*
import javax.inject.Inject
import android.util.Log

class ProcessVoiceNoteUseCase @Inject constructor(
    private val speechToTextService: SpeechToTextService,
    private val aiService: AiService
) {
    companion object {
        private const val TAG = "ProcessVoiceNoteUseCase"
    }
    suspend operator fun invoke(audioFilePath: String): Note {
        Log.d(TAG, "Starting to process audio file: $audioFilePath")
        
        // 1. Transcribe audio to text
        val transcript = try {
            Log.d(TAG, "Starting transcription...")
            val result = speechToTextService.transcribeAudio(File(audioFilePath))
            Log.d(TAG, "Transcription successful. Length: ${result.length} characters")
            Log.d(TAG, "Transcript preview: ${result.take(100)}...")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            // If transcription fails completely, throw to handle at higher level
            throw e
        }
        
        // 2. Generate title from transcript using AI
        val title = try {
            Log.d(TAG, "Generating title...")
            val aiTitle = if (aiService is GeminiAiService) {
                aiService.generateTitle(transcript)
            } else {
                generateTitle(transcript)
            }
            Log.d(TAG, "Generated title: $aiTitle")
            aiTitle
        } catch (e: Exception) {
            Log.e(TAG, "Title generation failed, using fallback", e)
            // Fallback to manual title generation if AI fails
            generateTitle(transcript)
        }
        
        // 3. Generate AI summary
        val summary = try {
            aiService.generateSummary(transcript)
        } catch (e: Exception) {
            // Return empty summary if AI fails
            ""
        }
        
        // 4. Extract tasks from transcript
        val tasks = try {
            val extractedTasks = aiService.extractTasks(transcript)
            // Log the extracted tasks for debugging
            println("ProcessVoiceNoteUseCase: Extracted ${extractedTasks.size} tasks from transcript")
            extractedTasks.forEach { task ->
                println("Task: ${task.title}, Priority: ${task.priority}, Due: ${task.dueDate}")
            }
            extractedTasks
        } catch (e: Exception) {
            println("ProcessVoiceNoteUseCase: Failed to extract tasks - ${e.message}")
            // Return empty task list if AI fails
            emptyList()
        }
        
        // 5. Get audio duration
        val duration = getAudioDuration(audioFilePath)
        Log.d(TAG, "Audio duration: $duration ms")
        
        // Create and return the note
        val note = Note(
            title = title,
            transcript = transcript,
            summary = summary,
            audioFilePath = audioFilePath,
            duration = duration,
            tasks = tasks
        )
        
        Log.d(TAG, "Note created successfully with ID: ${note.id}")
        return note
    }
    
    private fun generateTitle(transcript: String): String {
        // Fallback title generation - take first sentence or first few words
        val firstSentence = transcript.split(Regex("[.!?]")).firstOrNull()?.trim() ?: transcript
        return if (firstSentence.length > 50) {
            firstSentence.take(47) + "..."
        } else {
            firstSentence.ifEmpty { "Voice Note" }
        }
    }
    
    private fun getAudioDuration(audioFilePath: String): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFilePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            // Log error and return 0 if unable to get duration
            0L
        }
    }
}