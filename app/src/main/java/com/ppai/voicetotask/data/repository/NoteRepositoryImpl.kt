package com.ppai.voicetotask.data.repository

import com.ppai.voicetotask.data.local.dao.NoteDao
import com.ppai.voicetotask.data.local.dao.TaskDao
import com.ppai.voicetotask.data.mapper.toDomainModel
import com.ppai.voicetotask.data.mapper.toEntity
import com.ppai.voicetotask.domain.model.Note
import com.ppai.voicetotask.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao
) : NoteRepository {
    
    override fun getAllNotesWithTasks(): Flow<List<Note>> {
        return noteDao.getAllNotesWithTasks().map { notesWithTasks ->
            notesWithTasks.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getNoteById(noteId: String): Note? {
        return noteDao.getNoteById(noteId)?.toDomainModel()
    }
    
    override suspend fun getNoteWithTasks(noteId: String): Note? {
        return noteDao.getNoteWithTasks(noteId)?.toDomainModel()
    }
    
    override suspend fun insertNote(note: Note) {
        noteDao.insertNote(note.toEntity())
        // Don't insert tasks here as they are inserted separately in RecordingViewModel
    }
    
    override suspend fun updateNote(note: Note) {
        noteDao.updateNote(note.toEntity())
    }
    
    override suspend fun deleteNote(noteId: String) {
        // Tasks will be automatically deleted due to foreign key constraint
        noteDao.deleteNoteById(noteId)
    }
    
    override fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%").map { notes ->
            notes.map { it.toDomainModel() }
        }
    }
    
    override suspend fun deleteAllNotes() {
        // Tasks will be automatically deleted due to foreign key constraint
        noteDao.deleteAllNotes()
    }
}