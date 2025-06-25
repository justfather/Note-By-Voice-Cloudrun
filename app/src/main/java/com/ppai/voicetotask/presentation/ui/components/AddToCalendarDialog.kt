package com.ppai.voicetotask.presentation.ui.components

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawStyle
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.util.CalendarHelper
import com.ppai.voicetotask.util.MultiplePermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathMeasure

@Composable
fun AddToCalendarDialog(
    task: Task,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendarHelper = remember { CalendarHelper(context) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCalendarList by remember { mutableStateOf(false) }
    var calendars by remember { mutableStateOf<List<CalendarHelper.CalendarInfo>>(emptyList()) }
    var requestPermissions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Animation states
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isLoading || showSuccess || showError) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (showSuccess) 500 else 300
        )
    )
    
    val animatedScale by animateFloatAsState(
        targetValue = when {
            showSuccess -> 1.1f
            showError -> 1.2f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = if (showSuccess) Spring.DampingRatioHighBouncy else Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    // Success fade out animation
    var shouldFadeOut by remember { mutableStateOf(false) }
    val fadeOutAlpha by animateFloatAsState(
        targetValue = if (shouldFadeOut) 0f else 1f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        finishedListener = {
            if (shouldFadeOut) {
                onDismiss()
            }
        }
    )
    
    // Start fade out after showing success
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000) // Show success for 2 seconds
            shouldFadeOut = true
        }
    }
    
    // Checkmark animation
    val checkmarkProgress by animateFloatAsState(
        targetValue = if (showSuccess) 1f else 0f,
        animationSpec = tween(
            durationMillis = 600,
            easing = FastOutSlowInEasing
        )
    )
    
    // Automatically try to add to calendar when dialog opens
    LaunchedEffect(Unit) {
        if (calendarHelper.hasCalendarPermission()) {
            android.util.Log.d("AddToCalendarDialog", "Has permission, adding to calendar directly")
            addTaskToCalendar(calendarHelper, task, context, onDismiss, { loading ->
                isLoading = loading
            })
        } else {
            android.util.Log.d("AddToCalendarDialog", "No permission, requesting...")
            requestPermissions = true
        }
    }
    
    // Handle permission request with rationale
    if (requestPermissions) {
        MultiplePermissionHandler(
            permissions = listOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR
            ),
            onPermissionResult = { permissions ->
                requestPermissions = false
                val allGranted = permissions.values.all { it }
                android.util.Log.d("AddToCalendarDialog", "Permission result: $permissions, All granted: $allGranted")
                
                if (allGranted) {
                    // Permissions granted, load calendars
                    scope.launch {
                        calendars = withContext(Dispatchers.IO) {
                            calendarHelper.getCalendars()
                        }
                        android.util.Log.d("AddToCalendarDialog", "Found ${calendars.size} calendars")
                        
                        if (calendars.isNotEmpty()) {
                            showCalendarList = calendars.size > 1
                            if (calendars.size == 1) {
                                // Only one calendar, use it directly
                                addTaskToCalendar(calendarHelper, task, context, onDismiss, { loading ->
                                    isLoading = loading
                                })
                            }
                        } else {
                            Toast.makeText(context, "No calendars found. Please add a Google account to Calendar app.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                } else {
                    Toast.makeText(context, "Calendar permission is required to add tasks", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            }
        ) {
            // Empty content - the permission dialog will be shown by MultiplePermissionHandler
        }
        return // Important: return here to prevent showing other dialogs
    }
    
    // Show animated loading/success/error states
    if ((isLoading || showSuccess || showError) && !requestPermissions) {
        Dialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !isLoading,
                dismissOnClickOutside = !isLoading
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (showSuccess) animatedAlpha * fadeOutAlpha else animatedAlpha),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .scale(animatedScale),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Animated icon container
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        showSuccess -> MaterialTheme.colorScheme.primaryContainer
                                        showError -> MaterialTheme.colorScheme.errorContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                showSuccess -> {
                                    AnimatedCheckmark(
                                        progress = checkmarkProgress,
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                showError -> {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Error",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                else -> {
                                    CalendarLoadingAnimation()
                                }
                            }
                        }
                        
                        // Title
                        Text(
                            text = when {
                                showSuccess -> "Added to Calendar!"
                                showError -> "Oops!"
                                else -> "Adding to Calendar"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        
                        // Message
                        if (!showSuccess) {
                            Text(
                                text = when {
                                    showError -> errorMessage.ifEmpty { "Failed to add task to calendar" }
                                    else -> "Please wait while we add \"${task.title}\" to your calendar"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Action button only for error states
                        if (showError) {
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
        }
        return
    }
    
    if (showCalendarList) {
        Dialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !isLoading,
                dismissOnClickOutside = !isLoading
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Calendar",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(calendars) { calendar ->
                            Surface(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        addTaskToSpecificCalendar(
                                            calendarHelper,
                                            task,
                                            calendar.id,
                                            context,
                                            onDismiss,
                                            onSuccess = { showSuccess = true },
                                            onError = { msg -> 
                                                showError = true
                                                errorMessage = msg
                                            }
                                        ) { loading ->
                                            isLoading = loading
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = calendar.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = calendar.accountName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
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

@Composable
private fun CalendarLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition()
    
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        // Calendar base icon
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .alpha(0.3f),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        
        // Animated plus icon
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .scale(
                    animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = keyframes {
                                durationMillis = 1000
                                0.8f at 0
                                1.2f at 500
                                0.8f at 1000
                            },
                            repeatMode = RepeatMode.Restart
                        )
                    ).value
                ),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun AnimatedCheckmark(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.1f
        val padding = strokeWidth / 2
        
        // Calculate checkmark path
        val width = size.width - padding * 2
        val height = size.height - padding * 2
        
        // Checkmark points
        val startX = padding + width * 0.2f
        val startY = padding + height * 0.5f
        
        val midX = padding + width * 0.45f
        val midY = padding + height * 0.7f
        
        val endX = padding + width * 0.8f
        val endY = padding + height * 0.3f
        
        // Draw the checkmark based on progress
        if (progress > 0) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(startX, startY)
                
                if (progress <= 0.5f) {
                    // First half: draw from start to mid
                    val firstProgress = progress * 2
                    lineTo(
                        startX + (midX - startX) * firstProgress,
                        startY + (midY - startY) * firstProgress
                    )
                } else {
                    // Complete first half and draw second half
                    lineTo(midX, midY)
                    val secondProgress = (progress - 0.5f) * 2
                    lineTo(
                        midX + (endX - midX) * secondProgress,
                        midY + (endY - midY) * secondProgress
                    )
                }
            }
            
            drawPath(
                path = path,
                color = color,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = strokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )
        }
    }
}

private suspend fun addTaskToCalendar(
    calendarHelper: CalendarHelper,
    task: Task,
    context: android.content.Context,
    onDismiss: () -> Unit,
    setLoading: (Boolean) -> Unit,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    android.util.Log.d("AddToCalendarDialog", "Starting to add task to calendar: ${task.title}")
    android.util.Log.d("AddToCalendarDialog", "Task details - Title: ${task.title}, Due: ${task.dueDate}, Priority: ${task.priority}")
    setLoading(true)
    
    try {
        // First check if we have any calendars
        val calendars = withContext(Dispatchers.IO) {
            calendarHelper.getCalendars()
        }
        android.util.Log.d("AddToCalendarDialog", "Available calendars: ${calendars.size}")
        calendars.forEach { cal ->
            android.util.Log.d("AddToCalendarDialog", "Calendar: ${cal.displayName} (${cal.accountName})")
        }
        
        if (calendars.isEmpty()) {
            withContext(Dispatchers.Main) {
                setLoading(false)
                onError("No calendars found. Please add a Google account to your Calendar app.")
                delay(2000)
                onDismiss()
            }
            return
        }
        
        val success = withContext(Dispatchers.IO) {
            calendarHelper.addTaskToCalendar(task)
        }
        
        android.util.Log.d("AddToCalendarDialog", "Add task result: $success")
        
        withContext(Dispatchers.Main) {
            setLoading(false)
            
            if (success) {
                // The calendar helper now handles verification and fallback
                onSuccess()
                delay(3000) // Show success animation with fade out
                onDismiss()
            } else {
                // Show error state briefly, then show option to open calendar app manually
                onError("We couldn't add the event directly")
                delay(2000)
                
                android.app.AlertDialog.Builder(context)
                    .setTitle("Calendar Issue")
                    .setMessage("Would you like to open your calendar app to add it manually?")
                    .setPositiveButton("Open Calendar") { _, _ ->
                        try {
                            val intent = calendarHelper.createCalendarIntent(task)
                            context.startActivity(intent)
                            onDismiss()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open calendar app", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        onDismiss()
                    }
                    .show()
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AddToCalendarDialog", "Exception while adding to calendar", e)
        withContext(Dispatchers.Main) {
            setLoading(false)
            onError("Error: ${e.message}")
            delay(2000)
            onDismiss()
        }
    }
}

private suspend fun addTaskToSpecificCalendar(
    calendarHelper: CalendarHelper,
    task: Task,
    calendarId: Long,
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    
    val success = withContext(Dispatchers.IO) {
        try {
            calendarHelper.addTaskToSpecificCalendar(task, calendarId)
        } catch (e: Exception) {
            false
        }
    }
    
    withContext(Dispatchers.Main) {
        setLoading(false)
        
        if (success) {
            onSuccess()
            delay(1500) // Show success animation
            onDismiss()
        } else {
            onError("Failed to add task to calendar")
            delay(2000)
            onDismiss()
        }
    }
}