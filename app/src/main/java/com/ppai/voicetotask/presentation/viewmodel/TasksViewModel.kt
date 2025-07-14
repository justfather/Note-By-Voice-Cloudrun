package com.ppai.voicetotask.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.domain.repository.TaskRepository
import com.ppai.voicetotask.presentation.ui.components.TaskFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
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
    
    @OptIn(FlowPreview::class)
    private fun observeTasks() {
        viewModelScope.launch {
            try {
                combine(
                    _selectedFilter,
                    _searchQuery.debounce(300).distinctUntilChanged()
                ) { filter, query ->
                    Pair(filter, query)
                }.flatMapLatest { (filter, query) ->
                    // Get appropriate flow based on filter
                    val baseFlow = when (filter) {
                        TaskFilter.All -> if (query.isEmpty()) taskRepository.getAllTasks() else taskRepository.searchTasks(query)
                        TaskFilter.Today -> taskRepository.getTodayTasks()
                        TaskFilter.Overdue -> taskRepository.getOverdueTasks()
                        TaskFilter.Completed -> taskRepository.getCompletedTasks()
                        TaskFilter.Upcoming -> taskRepository.getActiveTasks() // For upcoming tasks
                        else -> taskRepository.getAllTasks()
                    }
                    
                    baseFlow.map { tasks ->
                        // Apply additional filtering for search query on filtered results
                        val finalTasks = if (query.isNotEmpty() && filter != TaskFilter.All) {
                            tasks.filter { task ->
                                task.title.contains(query, ignoreCase = true)
                            }
                        } else {
                            tasks
                        }
                        
                        // For upcoming filter, exclude today and overdue
                        if (filter == TaskFilter.Upcoming) {
                            finalTasks.filter { task ->
                                task.dueDate != null && 
                                task.dueDate.after(Date()) && 
                                !isToday(task.dueDate) &&
                                !task.completed
                            }
                        } else {
                            finalTasks
                        }
                    }
                }.collect { filteredTasks ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            filteredTasks = filteredTasks,
                            allTasks = filteredTasks, // For consistency
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
    
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            try {
                val task = _uiState.value.allTasks.find { it.id == taskId } ?: return@launch
                taskRepository.deleteTask(task)
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        error = "Failed to delete task: ${e.message}"
                    )
                }
            }
        }
    }
}

data class TasksUiState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)