package com.ppai.voicetotask.data.remote.api

import com.ppai.voicetotask.domain.model.Task

interface AiService {
    suspend fun generateSummary(text: String): String
    suspend fun extractTasks(text: String): List<Task>
}