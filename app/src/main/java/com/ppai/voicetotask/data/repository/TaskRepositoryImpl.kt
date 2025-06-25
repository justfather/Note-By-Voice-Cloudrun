package com.ppai.voicetotask.data.repository

import com.ppai.voicetotask.data.local.dao.TaskDao
import com.ppai.voicetotask.data.mapper.toDomainModel
import com.ppai.voicetotask.data.mapper.toEntity
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override fun getTasksByNoteId(noteId: String): Flow<List<Task>> {
        return taskDao.getTasksByNoteId(noteId).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomainModel()
    }
    
    override fun getActiveTasks(): Flow<List<Task>> {
        return taskDao.getActiveTasks().map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override fun getCompletedTasks(): Flow<List<Task>> {
        return taskDao.getCompletedTasks().map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override fun getOverdueTasks(): Flow<List<Task>> {
        return taskDao.getOverdueTasks(Date()).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override fun getTodayTasks(): Flow<List<Task>> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.time
        
        return taskDao.getTodayTasks(startOfDay, endOfDay).map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
    
    override suspend fun insertTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }
    
    override suspend fun insertTasks(tasks: List<Task>) {
        taskDao.insertTasks(tasks.map { it.toEntity() })
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
    
    override suspend fun updateTaskCompletion(taskId: String, completed: Boolean, updatedAt: Date) {
        taskDao.updateTaskCompletion(taskId, completed, updatedAt)
    }
    
    override fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks("%$query%").map { tasks ->
            tasks.map { it.toDomainModel() }
        }
    }
}