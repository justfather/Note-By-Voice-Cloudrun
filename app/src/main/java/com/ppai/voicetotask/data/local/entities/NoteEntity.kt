package com.ppai.voicetotask.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val transcript: String,
    val summary: String,
    val audioFilePath: String?,
    val duration: Long,
    val createdAt: Date,
    val updatedAt: Date,
    val archived: Boolean = false
)