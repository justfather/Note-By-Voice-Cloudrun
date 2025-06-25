package com.ppai.voicetotask.data.mapper

import com.ppai.voicetotask.data.local.entities.TaskEntity
import com.ppai.voicetotask.domain.model.Task

fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = id,
        title = title,
        completed = completed,
        dueDate = dueDate,
        priority = priority,
        noteId = noteId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        completed = completed,
        dueDate = dueDate,
        priority = priority,
        noteId = noteId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}