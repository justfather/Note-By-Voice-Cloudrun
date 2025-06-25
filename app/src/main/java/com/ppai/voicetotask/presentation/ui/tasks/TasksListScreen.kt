package com.ppai.voicetotask.presentation.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ppai.voicetotask.presentation.ui.components.*
import com.ppai.voicetotask.presentation.viewmodel.TasksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                placeholder = "Search tasks..."
            )
            
            FilterChips(
                filters = listOf(
                    TaskFilter.All,
                    TaskFilter.Today,
                    TaskFilter.Overdue,
                    TaskFilter.Upcoming,
                    TaskFilter.Completed
                ),
                selectedFilter = selectedFilter,
                onFilterSelected = viewModel::onFilterSelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.filteredTasks.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.CheckBox,
                        title = when (selectedFilter) {
                            TaskFilter.Today -> "No tasks for today"
                            TaskFilter.Overdue -> "No overdue tasks"
                            TaskFilter.Upcoming -> "No upcoming tasks"
                            TaskFilter.Completed -> "No completed tasks"
                            else -> "No tasks yet"
                        },
                        description = when (selectedFilter) {
                            TaskFilter.Completed -> "Complete some tasks to see them here"
                            else -> "Record a voice note to generate tasks"
                        }
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Group tasks by status/date
                        when (selectedFilter) {
                            TaskFilter.All -> {
                                // Group tasks by creation date
                                val groupedByDate = uiState.filteredTasks.groupBy { task ->
                                    formatDateHeader(task.createdAt)
                                }
                                
                                // Sort dates in descending order (newest first)
                                val sortedDates = groupedByDate.keys.sortedByDescending { dateHeader ->
                                    // Parse back to date for proper sorting
                                    try {
                                        when {
                                            dateHeader == "Today" -> Date()
                                            dateHeader == "Yesterday" -> Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                                            else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).parse(dateHeader) ?: Date(0)
                                        }
                                    } catch (e: Exception) {
                                        Date(0)
                                    }
                                }
                                
                                sortedDates.forEach { dateHeader ->
                                    groupedByDate[dateHeader]?.let { tasksForDate ->
                                        item {
                                            Text(
                                                text = dateHeader,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    top = 16.dp,
                                                    bottom = 8.dp
                                                )
                                            )
                                        }
                                        
                                        // Sub-group by status within each date
                                        val subGrouped = tasksForDate.groupBy { task ->
                                            when {
                                                task.completed -> "Completed"
                                                task.dueDate == null -> "Tasks"
                                                isToday(task.dueDate) -> "Due Today"
                                                isOverdue(task.dueDate) -> "Overdue"
                                                else -> "Upcoming"
                                            }
                                        }
                                        
                                        val subOrder = listOf("Overdue", "Due Today", "Upcoming", "Tasks", "Completed")
                                        subOrder.forEach { status ->
                                            subGrouped[status]?.let { tasks ->
                                                if (tasks.isNotEmpty()) {
                                                    item {
                                                        Text(
                                                            text = status,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(
                                                                start = 32.dp,
                                                                top = 8.dp,
                                                                bottom = 4.dp
                                                            )
                                                        )
                                                    }
                                                    
                                                    items(
                                                        items = tasks,
                                                        key = { it.id }
                                                    ) { task ->
                                                        TaskItem(
                                                            task = task,
                                                            onToggleComplete = {
                                                                viewModel.toggleTaskComplete(task.id)
                                                            },
                                                            showNote = true,
                                                            modifier = Modifier.padding(start = 16.dp),
                                                            onAddToCalendar = { /* Calendar integration handled in TaskItem */ }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            TaskFilter.Upcoming -> {
                                val groupedTasks = uiState.filteredTasks.groupBy { task ->
                                    when {
                                        task.completed -> "Completed"
                                        task.dueDate == null -> "No due date"
                                        isToday(task.dueDate) -> "Today"
                                        isOverdue(task.dueDate) && !task.completed -> "Overdue"
                                        else -> "Upcoming"
                                    }
                                }
                                
                                val order = listOf("Overdue", "Today", "Upcoming", "No due date", "Completed")
                                order.forEach { group ->
                                    groupedTasks[group]?.let { tasks ->
                                        item {
                                            Text(
                                                text = group,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 16.dp,
                                                    top = 16.dp,
                                                    bottom = 8.dp
                                                )
                                            )
                                        }
                                        
                                        items(
                                            items = tasks,
                                            key = { it.id }
                                        ) { task ->
                                            TaskItem(
                                                task = task,
                                                onToggleComplete = {
                                                    viewModel.toggleTaskComplete(task.id)
                                                },
                                                showNote = true,
                                                onAddToCalendar = { /* Calendar integration handled in TaskItem */ }
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {
                                items(
                                    items = uiState.filteredTasks,
                                    key = { it.id }
                                ) { task ->
                                    TaskItem(
                                        task = task,
                                        onToggleComplete = {
                                            viewModel.toggleTaskComplete(task.id)
                                        },
                                        showNote = true,
                                        onAddToCalendar = { /* Calendar integration handled in TaskItem */ }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isToday(date: java.util.Date): Boolean {
    val calendar = java.util.Calendar.getInstance()
    val today = calendar.time
    
    calendar.time = date
    val dateToCheck = calendar.time
    
    return isSameDay(today, dateToCheck)
}

private fun isOverdue(date: java.util.Date): Boolean {
    return date.before(java.util.Date()) && !isToday(date)
}

private fun isSameDay(date1: java.util.Date, date2: java.util.Date): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
    val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun formatDateHeader(date: Date): String {
    val today = Date()
    val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
    
    return when {
        isSameDay(date, today) -> "Today"
        isSameDay(date, yesterday) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    }
}