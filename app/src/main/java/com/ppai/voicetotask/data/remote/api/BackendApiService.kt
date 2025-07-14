package com.ppai.voicetotask.data.remote.api

import retrofit2.http.*

interface BackendApiService {
    
    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse
    
    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse
    
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
    
    // Gemini proxy endpoints
    @POST("api/generate-title")
    suspend fun generateTitle(
        @Header("Authorization") token: String,
        @Body request: GenerateTitleRequest
    ): GenerateTitleResponse
    
    @POST("api/generate-summary")
    suspend fun generateSummary(
        @Header("Authorization") token: String,
        @Body request: GenerateSummaryRequest
    ): GenerateSummaryResponse
    
    @POST("api/extract-tasks")
    suspend fun extractTasks(
        @Header("Authorization") token: String,
        @Body request: ExtractTasksRequest
    ): ExtractTasksResponse
    
    @POST("api/process-voice-note")
    suspend fun processVoiceNote(
        @Header("Authorization") token: String,
        @Body request: ProcessVoiceNoteRequest
    ): ProcessVoiceNoteResponse
    
    @GET("health")
    suspend fun healthCheck(): HealthResponse
}

// Request models
data class AuthRequest(
    val email: String,
    val password: String
)

data class RefreshTokenRequest(
    val userId: String,
    val email: String
)

data class GenerateTitleRequest(
    val transcript: String,
    val language: String? = null
)

data class GenerateSummaryRequest(
    val transcript: String,
    val language: String? = null
)

data class ExtractTasksRequest(
    val transcript: String,
    val currentDate: String,
    val language: String? = null
)

data class ProcessVoiceNoteRequest(
    val transcript: String,
    val currentDate: String,
    val language: String? = null
)

// Response models
data class AuthResponse(
    val status: String,
    val data: AuthData
)

data class AuthData(
    val token: String,
    val user: UserInfo
)

data class UserInfo(
    val id: String,
    val email: String,
    val isPremium: Boolean
)

data class RefreshTokenResponse(
    val status: String,
    val data: TokenData
)

data class TokenData(
    val token: String
)

data class GenerateTitleResponse(
    val status: String,
    val data: TitleData
)

data class TitleData(
    val title: String
)

data class GenerateSummaryResponse(
    val status: String,
    val data: SummaryData
)

data class SummaryData(
    val summary: String
)

data class ExtractTasksResponse(
    val status: String,
    val data: TasksData
)

data class TasksData(
    val tasks: List<ApiTask>
)

data class ApiTask(
    val title: String,
    val priority: String,
    val dueDate: String? = null
)

data class ProcessVoiceNoteResponse(
    val status: String,
    val data: ProcessedNoteData
)

data class ProcessedNoteData(
    val title: String,
    val summary: String,
    val tasks: List<ApiTask>
)

data class HealthResponse(
    val status: String,
    val timestamp: String,
    val uptime: Double
)

// Error response
data class ErrorResponse(
    val status: String,
    val message: String
)