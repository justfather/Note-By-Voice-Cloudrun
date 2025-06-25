package com.ppai.voicetotask.data.local.dao

import androidx.room.*
import com.ppai.voicetotask.data.local.entities.TaskEntity
import com.ppai.voicetotask.domain.model.Priority
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY createdAt ASC")
    fun getTasksByNoteId(noteId: String): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY dueDate ASC, priority DESC")
    fun getActiveTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE dueDate < :today AND completed = 0 ORDER BY dueDate ASC")
    fun getOverdueTasks(today: Date): Flow<List<TaskEntity>>
    
    @Query("SELECT * FROM tasks WHERE dueDate >= :startOfDay AND dueDate <= :endOfDay AND completed = 0 ORDER BY priority DESC")
    fun getTodayTasks(startOfDay: Date, endOfDay: Date): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    @Query("UPDATE tasks SET completed = :completed, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, updatedAt: Date)
    
    @Query("DELETE FROM tasks WHERE noteId = :noteId")
    suspend fun deleteTasksByNoteId(noteId: String)
    
    @Query("SELECT * FROM tasks WHERE title LIKE :query ORDER BY dueDate ASC, priority DESC")
    fun searchTasks(query: String): Flow<List<TaskEntity>>
}