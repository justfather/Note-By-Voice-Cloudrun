package com.ppai.voicetotask.domain.model

import java.util.Date
import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val transcript: String,
    val summary: String = "",
    val audioFilePath: String? = null,
    val duration: Long = 0L, // in milliseconds
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val tasks: List<Task> = emptyList(),
    val archived: Boolean = false
)