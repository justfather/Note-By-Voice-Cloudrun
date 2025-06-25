package com.ppai.voicetotask.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.ppai.voicetotask.domain.model.Priority
import java.util.Date

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val completed: Boolean,
    val dueDate: Date?,
    val priority: Priority,
    val noteId: String,
    val createdAt: Date,
    val updatedAt: Date
)