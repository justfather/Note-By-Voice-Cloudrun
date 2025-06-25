package com.ppai.voicetotask.domain.repository

import com.ppai.voicetotask.domain.model.Task
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByNoteId(noteId: String): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    fun getActiveTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getOverdueTasks(): Flow<List<Task>>
    fun getTodayTasks(): Flow<List<Task>>
    suspend fun insertTask(task: Task)
    suspend fun insertTasks(tasks: List<Task>)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean, updatedAt: Date)
    fun searchTasks(query: String): Flow<List<Task>>
}