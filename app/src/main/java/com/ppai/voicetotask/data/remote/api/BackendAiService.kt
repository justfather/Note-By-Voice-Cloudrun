package com.ppai.voicetotask.data.remote.api

import android.util.Log
import com.ppai.voicetotask.data.preferences.OutputLanguage
import com.ppai.voicetotask.domain.model.Priority
import com.ppai.voicetotask.domain.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendAiService @Inject constructor(
    private val backendApiService: BackendApiService,
    private val authManager: AuthManager
) : AiService {
    
    companion object {
        private const val TAG = "BackendAiService"
    }
    
    private suspend fun getAuthToken(): String {
        return authManager.getValidToken() ?: throw Exception("No valid auth token")
    }
    
    override suspend fun generateSummary(text: String): String = 
        generateSummary(text, OutputLanguage.AUTO)
    
    suspend fun generateSummary(text: String, outputLanguage: OutputLanguage): String = withContext(Dispatchers.IO) {
        try {
            val token = "Bearer ${getAuthToken()}"
            val request = GenerateSummaryRequest(
                transcript = text,
                language = outputLanguage.toApiLanguage()
            )
            
            val response = backendApiService.generateSummary(token, request)
            response.data.summary
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate summary", e)
            throw e
        }
    }
    
    override suspend fun extractTasks(text: String): List<Task> = 
        extractTasks(text, OutputLanguage.AUTO)
    
    suspend fun extractTasks(text: String, outputLanguage: OutputLanguage): List<Task> = withContext(Dispatchers.IO) {
        try {
            val token = "Bearer ${getAuthToken()}"
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val request = ExtractTasksRequest(
                transcript = text,
                currentDate = currentDate,
                language = outputLanguage.toApiLanguage()
            )
            
            val response = backendApiService.extractTasks(token, request)
            response.data.tasks.map { apiTask ->
                Task(
                    title = apiTask.title,
                    priority = when (apiTask.priority.lowercase()) {
                        "high" -> Priority.HIGH
                        "low" -> Priority.LOW
                        else -> Priority.MEDIUM
                    },
                    dueDate = apiTask.dueDate?.let {
                        try {
                            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    },
                    noteId = "" // Will be set later
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract tasks", e)
            throw e
        }
    }
    
    suspend fun generateTitle(text: String, outputLanguage: OutputLanguage): String = withContext(Dispatchers.IO) {
        try {
            val token = "Bearer ${getAuthToken()}"
            val request = GenerateTitleRequest(
                transcript = text,
                language = outputLanguage.toApiLanguage()
            )
            
            val response = backendApiService.generateTitle(token, request)
            response.data.title
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate title", e)
            throw e
        }
    }
    
    suspend fun processVoiceNote(
        text: String, 
        outputLanguage: OutputLanguage
    ): ProcessedVoiceNote = withContext(Dispatchers.IO) {
        try {
            val token = "Bearer ${getAuthToken()}"
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val request = ProcessVoiceNoteRequest(
                transcript = text,
                currentDate = currentDate,
                language = outputLanguage.toApiLanguage()
            )
            
            val response = backendApiService.processVoiceNote(token, request)
            ProcessedVoiceNote(
                title = response.data.title,
                summary = response.data.summary,
                tasks = response.data.tasks.map { apiTask ->
                    Task(
                        title = apiTask.title,
                        priority = when (apiTask.priority.lowercase()) {
                            "high" -> Priority.HIGH
                            "low" -> Priority.LOW
                            else -> Priority.MEDIUM
                        },
                        dueDate = apiTask.dueDate?.let {
                            try {
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(it)
                            } catch (e: Exception) {
                                null
                            }
                        },
                        noteId = "" // Will be set later
                    )
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process voice note", e)
            throw e
        }
    }
    
    private fun OutputLanguage.toApiLanguage(): String? {
        return when (this) {
            OutputLanguage.AUTO -> null
            OutputLanguage.ENGLISH -> "English"
            OutputLanguage.THAI -> "Thai"
            OutputLanguage.SPANISH -> "Spanish"
            OutputLanguage.FRENCH -> "French"
            OutputLanguage.GERMAN -> "German"
            OutputLanguage.JAPANESE -> "Japanese"
            OutputLanguage.KOREAN -> "Korean"
            OutputLanguage.CHINESE -> "Chinese"
        }
    }
}

data class ProcessedVoiceNote(
    val title: String,
    val summary: String,
    val tasks: List<Task>
)