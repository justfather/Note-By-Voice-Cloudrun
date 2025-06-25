package com.ppai.voicetotask.domain.model

import java.util.Date
import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
    val dueDate: Date? = null,
    val priority: Priority = Priority.MEDIUM,
    val noteId: String,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)