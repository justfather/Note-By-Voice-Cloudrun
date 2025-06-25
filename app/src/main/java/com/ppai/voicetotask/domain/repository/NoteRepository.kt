package com.ppai.voicetotask.domain.repository

import com.ppai.voicetotask.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getAllNotesWithTasks(): Flow<List<Note>>
    suspend fun getNoteById(noteId: String): Note?
    suspend fun getNoteWithTasks(noteId: String): Note?
    suspend fun insertNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(noteId: String)
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun deleteAllNotes()
}