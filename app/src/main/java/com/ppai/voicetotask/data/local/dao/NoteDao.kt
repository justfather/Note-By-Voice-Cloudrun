package com.ppai.voicetotask.data.local.dao

import androidx.room.*
import com.ppai.voicetotask.data.local.entities.NoteEntity
import com.ppai.voicetotask.data.local.entities.NoteWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>
    
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?
    
    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteWithTasks(noteId: String): NoteWithTasks?
    
    @Transaction
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotesWithTasks(): Flow<List<NoteWithTasks>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)
    
    @Update
    suspend fun updateNote(note: NoteEntity)
    
    @Delete
    suspend fun deleteNote(note: NoteEntity)
    
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: String)
    
    @Query("SELECT * FROM notes WHERE title LIKE :query OR transcript LIKE :query OR summary LIKE :query ORDER BY createdAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
}