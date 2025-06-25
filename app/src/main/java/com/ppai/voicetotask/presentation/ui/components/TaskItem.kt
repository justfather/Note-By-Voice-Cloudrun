package com.ppai.voicetotask.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ppai.voicetotask.domain.model.Priority
import com.ppai.voicetotask.domain.model.Task
import java.text.SimpleDateFormat
import java.util.*
import com.ppai.voicetotask.presentation.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskItem(
    task: Task,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    showNote: Boolean = false,
    onAddToCalendar: ((Task) -> Unit)? = null
) {
    var showCalendarDialog by remember { mutableStateOf(false) }
    var showDateTimePicker by remember { mutableStateOf(false) }
    var taskWithDate by remember { mutableStateOf(task) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = if (task.completed) 
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
        else 
            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedColor = MaterialTheme.colorScheme.outline
                )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alpha(if (task.completed) 0.6f else 1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.dueDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = formatDueDate(task.dueDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isOverdue(task)) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    NotionPriorityChip(priority = task.priority)
                    
                }
            }
            
            // Add to calendar button
            if (onAddToCalendar != null && !task.completed) {
                IconButton(
                    onClick = { 
                        android.util.Log.d("TaskItem", "Calendar button clicked for task: ${task.title}")
                        if (task.dueDate == null) {
                            // Task has no date, show date/time picker first
                            showDateTimePicker = true
                        } else {
                            // Task has date, proceed with calendar dialog
                            showCalendarDialog = true
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add to Calendar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    // Show date/time picker for tasks without dates
    if (showDateTimePicker) {
        DateTimePickerDialog(
            onDateTimeSelected = { selectedDate ->
                // Create a new task with the selected date
                taskWithDate = task.copy(dueDate = selectedDate)
                showDateTimePicker = false
                showCalendarDialog = true
            },
            onDismiss = { showDateTimePicker = false }
        )
    }
    
    // Show calendar dialog
    if (showCalendarDialog) {
        AddToCalendarDialog(
            task = taskWithDate,
            onDismiss = { showCalendarDialog = false }
        )
    }
}

@Composable
private fun NotionPriorityChip(priority: Priority) {
    val text = when (priority) {
        Priority.HIGH -> "●●●"
        Priority.MEDIUM -> "●●"
        Priority.LOW -> "●"
    }
    
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary
    )
}

private fun isOverdue(task: Task): Boolean {
    return !task.completed && task.dueDate != null && task.dueDate.before(Date())
}

private fun formatDueDate(date: Date): String {
    val now = Date()
    val calendar = Calendar.getInstance()
    calendar.time = now
    val today = calendar.time
    
    calendar.add(Calendar.DAY_OF_YEAR, 1)
    val tomorrow = calendar.time
    
    // Get time part
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = timeFormat.format(date)
    
    // Check if it's the default time (09:00)
    val showTime = timeString != "09:00"
    
    val dateString = when {
        isSameDay(date, today) -> "Today"
        isSameDay(date, tomorrow) -> "Tomorrow"
        date.before(today) -> "Overdue"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
    
    return if (showTime) {
        "$dateString $timeString"
    } else {
        dateString
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}