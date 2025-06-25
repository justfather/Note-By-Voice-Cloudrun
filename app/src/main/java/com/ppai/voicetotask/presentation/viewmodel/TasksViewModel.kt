package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.domain.repository.TaskRepository
import com.ppai.voicetotask.presentation.ui.components.TaskFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow<TaskFilter>(TaskFilter.All)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()
    
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()
    
    init {
        observeTasks()
    }
    
    private fun observeTasks() {
        viewModelScope.launch {
            try {
                combine(
                    _selectedFilter,
                    _searchQuery,
                    taskRepository.getAllTasks()
                ) { filter, query, allTasks ->
                    Triple(filter, query, allTasks)
                }.collect { (filter, query, allTasks) ->
                println("TasksViewModel: Loaded ${allTasks.size} tasks from database")
                allTasks.forEach { task ->
                    println("Task: ${task.title}, Note: ${task.noteId}, Completed: ${task.completed}")
                }
                val filteredByType = when (filter) {
                    TaskFilter.All -> allTasks
                    TaskFilter.Today -> allTasks.filter { task ->
                        task.dueDate != null && isToday(task.dueDate) && !task.completed
                    }
                    TaskFilter.Overdue -> allTasks.filter { task ->
                        task.dueDate != null && task.dueDate.before(Date()) && !isToday(task.dueDate) && !task.completed
                    }
                    TaskFilter.Upcoming -> allTasks.filter { task ->
                        task.dueDate != null && task.dueDate.after(Date()) && !task.completed
                    }
                    TaskFilter.Completed -> allTasks.filter { it.completed }
                    else -> allTasks
                }
                
                val filteredTasks = if (query.isEmpty()) {
                    filteredByType
                } else {
                    filteredByType.filter { task ->
                        task.title.contains(query, ignoreCase = true)
                    }
                }
                
                _uiState.update { currentState ->
                    currentState.copy(
                        allTasks = allTasks,
                        filteredTasks = filteredTasks,
                        isLoading = false,
                        error = null
                    )
                }
            }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Failed to load tasks: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }
    
    fun onFilterSelected(filter: TaskFilter) {
        _selectedFilter.value = filter
    }
    
    fun toggleTaskComplete(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId } ?: return@launch
                taskRepository.updateTaskCompletion(
                    taskId = taskId,
                    completed = !task.completed,
                    updatedAt = Date()
                )
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = "Failed to update task: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun isToday(date: Date): Boolean {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.time
        
        calendar.time = date
        val dateToCheck = calendar.time
        
        return isSameDay(today, dateToCheck)
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)