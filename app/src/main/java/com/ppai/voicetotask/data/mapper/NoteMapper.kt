package com.ppai.voicetotask.data.mapper

import com.ppai.voicetotask.data.local.entities.NoteEntity
import com.ppai.voicetotask.data.local.entities.NoteWithTasks
import com.ppai.voicetotask.domain.model.Note

fun NoteEntity.toDomainModel(): Note {
    return Note(
        id = id,
        title = title,
        transcript = transcript,
        summary = summary,
        audioFilePath = audioFilePath,
        duration = duration,
        createdAt = createdAt,
        updatedAt = updatedAt,
        tasks = emptyList(),
        archived = archived
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        transcript = transcript,
        summary = summary,
        audioFilePath = audioFilePath,
        duration = duration,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archived = archived
    )
}

fun NoteWithTasks.toDomainModel(): Note {
    return Note(
        id = note.id,
        title = note.title,
        transcript = note.transcript,
        summary = note.summary,
        audioFilePath = note.audioFilePath,
        duration = note.duration,
        createdAt = note.createdAt,
        updatedAt = note.updatedAt,
        tasks = tasks.map { it.toDomainModel() },
        archived = note.archived
    )
}