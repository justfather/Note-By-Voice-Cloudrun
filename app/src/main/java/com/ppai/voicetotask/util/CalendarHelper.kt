package com.ppai.voicetotask.util

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.ppai.voicetotask.domain.model.Priority
import com.ppai.voicetotask.domain.model.Task
import java.text.SimpleDateFormat
import java.util.*

class CalendarHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "CalendarHelper"
    }
    
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun addTaskToCalendar(task: Task): Boolean {
        Log.d(TAG, "=== ADDING TASK TO CALENDAR ===")
        Log.d(TAG, "Task: ${task.title}")
        Log.d(TAG, "Due date: ${task.dueDate}")
        Log.d(TAG, "Priority: ${task.priority}")
        
        // Debug calendar setup first
        debugCalendarSetup()
        
        val calendarId = getPrimaryCalendarId()
        if (calendarId == -1L) {
            Log.e(TAG, "No calendar found!")
            return false
        }
        
        Log.d(TAG, "Using calendar ID: $calendarId")
        
        // Try direct insertion first
        val success = addTaskToSpecificCalendar(task, calendarId)
        Log.d(TAG, "Direct insertion result: $success")
        
        // If direct insertion shows success but might not sync, try intent as backup
        if (success) {
            // Wait a bit for the event to be written
            Thread.sleep(1000)  // Increased wait time
            
            // Force a sync notification
            context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null, true)
            
            // Verify the event was actually created
            val eventExists = verifyEventExists(task.title, calendarId)
            Log.d(TAG, "Event verification result: $eventExists")
            
            if (!eventExists) {
                Log.w(TAG, "Event insertion reported success but event not found. Using intent fallback.")
                try {
                    val intent = createCalendarIntent(task)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Intent fallback also failed", e)
                }
            }
        }
        
        Log.d(TAG, "=== END ADDING TASK TO CALENDAR ===")
        return success
    }
    
    private fun verifyEventExists(title: String, calendarId: Long): Boolean {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.DTSTART
        )
        
        // Just search by calendar ID and title - don't filter by time as it might not be set correctly
        val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.TITLE} = ?"
        val selectionArgs = arrayOf(calendarId.toString(), title)
        
        try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val exists = cursor.count > 0
                Log.d(TAG, "Event verification: found ${cursor.count} matching events for title: '$title' in calendar: $calendarId")
                
                // Log the events found for debugging
                while (cursor.moveToNext()) {
                    val eventId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                    val eventTitle = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                    val startTime = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                    Log.d(TAG, "Found event - ID: $eventId, Title: '$eventTitle', Start: $startTime")
                }
                
                return exists
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying event", e)
        }
        
        return false
    }
    
    fun addTaskToSpecificCalendar(task: Task, calendarId: Long): Boolean {
        if (!hasCalendarPermission()) {
            Log.e(TAG, "No calendar permission")
            return false
        }
        
        return try {
            if (calendarId == -1L) {
                Log.e(TAG, "No calendar found")
                return false
            }
            
            // First verify the calendar exists and is writable
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.VISIBLE
            )
            
            val selection = "${CalendarContract.Calendars._ID} = ?"
            val selectionArgs = arrayOf(calendarId.toString())
            
            var calendarExists = false
            var calendarInfo = ""
            
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    calendarExists = true
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                    val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))
                    val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                    val visible = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE))
                    
                    calendarInfo = "Calendar: $displayName, Account: $accountName, Type: $accountType, Access: $accessLevel, Visible: $visible"
                    Log.d(TAG, "Calendar info: $calendarInfo")
                    
                    if (accessLevel < CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) {
                        Log.e(TAG, "Calendar is read-only! Access level: $accessLevel")
                        return false
                    }
                }
            }
            
            if (!calendarExists) {
                Log.e(TAG, "Calendar ID $calendarId does not exist!")
                return false
            }
            
            Log.d(TAG, "Using calendar ID: $calendarId - $calendarInfo")
            
            val eventId = insertEvent(calendarId, task)
            if (eventId != -1L) {
                // Add reminder
                insertReminder(eventId, task.priority)
                Log.d(TAG, "Successfully added task to calendar: ${task.title} (Event ID: $eventId)")
                true
            } else {
                Log.e(TAG, "Failed to insert event")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding task to calendar", e)
            false
        }
    }
    
    private fun getPrimaryCalendarId(): Long {
        Log.d(TAG, "Getting primary calendar ID...")
        
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE
        )
        
        // First try to find primary Google calendar that is visible and writable
        val selection = "${CalendarContract.Calendars.IS_PRIMARY} = ? AND ${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf("1", "1", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        
        var calendarId = -1L
        
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                    val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                    Log.d(TAG, "Found primary calendar: $displayName ($accountName) - Access: $accessLevel")
                } else {
                    Log.d(TAG, "No primary calendar found")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying primary calendar", e)
        }
        
        // If no primary calendar found, get the first visible and writable Google calendar
        if (calendarId == -1L) {
            val googleSelection = "${CalendarContract.Calendars.ACCOUNT_TYPE} LIKE ? AND ${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val googleSelectionArgs = arrayOf("%google%", "1", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            
            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    googleSelection,
                    googleSelectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                        val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                        val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                        Log.d(TAG, "Found Google calendar: $displayName ($accountName) - Access: $accessLevel")
                    } else {
                        Log.d(TAG, "No Google calendar found")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying Google calendar", e)
            }
        }
        
        // If still no calendar found, get the first available visible and writable calendar
        if (calendarId == -1L) {
            val anySelection = "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
            val anySelectionArgs = arrayOf("1", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
            
            try {
                context.contentResolver.query(
                    CalendarContract.Calendars.CONTENT_URI,
                    projection,
                    anySelection,
                    anySelectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        calendarId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                        val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                        val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                        val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                        Log.d(TAG, "Using first available calendar: $displayName ($accountName) - Access: $accessLevel")
                    } else {
                        Log.e(TAG, "No writable calendars found at all!")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying any calendar", e)
            }
        }
        
        Log.d(TAG, "Returning calendar ID: $calendarId")
        return calendarId
    }
    
    private fun insertEvent(calendarId: Long, task: Task): Long {
        // Use today if no date specified
        val calendar = Calendar.getInstance()
        if (task.dueDate == null) {
            // Set to tomorrow at 9 AM (gives user time to see the task)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            calendar.time = task.dueDate
            // If the time is midnight (00:00), set it to 9 AM
            if (calendar.get(Calendar.HOUR_OF_DAY) == 0 && calendar.get(Calendar.MINUTE) == 0) {
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
            }
        }
        
        val startMillis = calendar.timeInMillis
        val endMillis = startMillis + (60 * 60 * 1000) // 1 hour duration
        
        // Log detailed time information
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d(TAG, "Event start time: ${dateFormat.format(Date(startMillis))}")
        Log.d(TAG, "Event end time: ${dateFormat.format(Date(endMillis))}")
        Log.d(TAG, "Timezone: ${TimeZone.getDefault().id}")
        
        // Check if calendar is syncing
        checkCalendarSyncStatus(calendarId)
        
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, task.title)
            put(CalendarContract.Events.DESCRIPTION, "Task from Note By Voice\n\nPriority: ${task.priority.name}")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.EVENT_END_TIMEZONE, TimeZone.getDefault().id) // Some providers need this
            put(CalendarContract.Events.ALL_DAY, 0) // Not all-day event
            
            // These fields are optional but can help with sync
            put(CalendarContract.Events.HAS_ALARM, 1)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            
            // IMPORTANT: Some calendar providers need these
            put(CalendarContract.Events.HAS_ATTENDEE_DATA, 0)
            put(CalendarContract.Events.GUESTS_CAN_MODIFY, 0)
            put(CalendarContract.Events.GUESTS_CAN_INVITE_OTHERS, 0)
            put(CalendarContract.Events.GUESTS_CAN_SEE_GUESTS, 1)
            
            // Add access level
            put(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_DEFAULT)
        }
        
        Log.d(TAG, "Inserting event with values:")
        Log.d(TAG, "  Calendar ID: $calendarId")
        Log.d(TAG, "  Title: ${task.title}")
        Log.d(TAG, "  Start: $startMillis (${dateFormat.format(Date(startMillis))})")
        Log.d(TAG, "  End: $endMillis (${dateFormat.format(Date(endMillis))})")
        
        try {
            // First, let's make sure we're using the correct calendar
            val calendarUri = CalendarContract.Calendars.CONTENT_URI
            val calendarProjection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE,
                CalendarContract.Calendars.OWNER_ACCOUNT
            )
            
            context.contentResolver.query(
                calendarUri,
                calendarProjection,
                "${CalendarContract.Calendars._ID} = ?",
                arrayOf(calendarId.toString()),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val ownerAccount = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT))
                    Log.d(TAG, "Calendar owner account: $ownerAccount")
                    
                    // Add owner account to event
                    values.put(CalendarContract.Events.ORGANIZER, ownerAccount)
                }
            }
            
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull() ?: -1L
            
            Log.d(TAG, "Insert result - URI: $uri, Event ID: $eventId")
            
            if (uri == null) {
                Log.e(TAG, "ContentResolver.insert returned null URI!")
                return -1L
            }
            
            // Force sync
            if (eventId != -1L) {
                context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
                
                // Wait a bit for the event to be written
                Thread.sleep(100)
                
                // Query to verify the event was created
                val projection = arrayOf(
                    CalendarContract.Events._ID,
                    CalendarContract.Events.TITLE,
                    CalendarContract.Events.DTSTART,
                    CalendarContract.Events.CALENDAR_ID,
                    CalendarContract.Events.VISIBLE,
                    CalendarContract.Events.DELETED,
                    CalendarContract.Events.DIRTY
                )
                val selection = "${CalendarContract.Events._ID} = ?"
                val selectionArgs = arrayOf(eventId.toString())
                
                context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val title = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                        val calId = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID))
                        val visible = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.VISIBLE))
                        val deleted = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.DELETED))
                        val dirty = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.DIRTY))
                        Log.d(TAG, "Event verified - Title: $title, Calendar: $calId, Visible: $visible, Deleted: $deleted, Dirty: $dirty")
                        
                        // Try to force sync the specific calendar
                        if (dirty == 1) {
                            Log.d(TAG, "Event is marked as dirty, forcing sync...")
                            val syncUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "false")
                                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "com.google")
                                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, "com.google")
                                .build()
                            context.contentResolver.notifyChange(syncUri, null, true)
                        }
                    } else {
                        Log.e(TAG, "Event not found after insertion! Checking all events...")
                        
                        // List recent events to debug
                        val debugSelection = "${CalendarContract.Events.DTSTART} >= ?"
                        val debugArgs = arrayOf((System.currentTimeMillis() - 24 * 60 * 60 * 1000).toString())
                        
                        context.contentResolver.query(
                            CalendarContract.Events.CONTENT_URI,
                            projection,
                            debugSelection,
                            debugArgs,
                            "${CalendarContract.Events.DTSTART} DESC LIMIT 5"
                        )?.use { debugCursor ->
                            Log.d(TAG, "Recent events in calendar:")
                            while (debugCursor.moveToNext()) {
                                val debugTitle = debugCursor.getString(debugCursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE))
                                val debugId = debugCursor.getLong(debugCursor.getColumnIndexOrThrow(CalendarContract.Events._ID))
                                Log.d(TAG, "  Event ID: $debugId, Title: $debugTitle")
                            }
                        }
                    }
                }
            }
            
            return eventId
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing calendar permission", e)
            return -1L
        } catch (e: Exception) {
            Log.e(TAG, "Exception during event insertion", e)
            return -1L
        }
    }
    
    private fun insertReminder(eventId: Long, priority: Priority) {
        val reminderMinutes = when (priority) {
            Priority.HIGH -> 15 // 15 minutes before
            Priority.MEDIUM -> 30 // 30 minutes before
            Priority.LOW -> 60 // 1 hour before
        }
        
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, reminderMinutes)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        
        Log.d(TAG, "Inserting reminder for event $eventId: $reminderMinutes minutes before")
        
        val uri = context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        Log.d(TAG, "Reminder inserted: $uri")
    }
    
    fun getCalendars(): List<CalendarInfo> {
        if (!hasCalendarPermission()) {
            return emptyList()
        }
        
        val calendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        
        // Only get calendars that are visible and writable
        val selection = "${CalendarContract.Calendars.VISIBLE} = ? AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf("1", CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))
                
                // Format account name for Gmail accounts
                val formattedAccountName = if (accountType.contains("google", true)) {
                    "Gmail: $accountName"
                } else {
                    accountName
                }
                
                calendars.add(CalendarInfo(id, displayName, formattedAccountName, accountType))
            }
        }
        
        Log.d(TAG, "Found ${calendars.size} writable calendars")
        calendars.forEach { 
            Log.d(TAG, "Calendar: ${it.displayName} (${it.accountName}) - Type: ${it.accountType}")
        }
        
        return calendars
    }
    
    data class CalendarInfo(
        val id: Long,
        val displayName: String,
        val accountName: String,
        val accountType: String
    )
    
    fun createCalendarIntent(task: Task): android.content.Intent {
        val calendar = Calendar.getInstance()
        if (task.dueDate != null) {
            calendar.time = task.dueDate
        } else {
            // Default to today at 9 AM
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
        }
        
        val startMillis = calendar.timeInMillis
        val endMillis = startMillis + (60 * 60 * 1000) // 1 hour duration
        
        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, task.title)
            putExtra(CalendarContract.Events.DESCRIPTION, "Task from Note By Voice\n\nPriority: ${task.priority.name}")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            putExtra(CalendarContract.Events.HAS_ALARM, true)
            putExtra(CalendarContract.Reminders.MINUTES, when(task.priority) {
                Priority.HIGH -> 15
                Priority.MEDIUM -> 30
                Priority.LOW -> 60
            })
        }
        
        return intent
    }
    
    fun debugCalendarSetup() {
        Log.d(TAG, "=== CALENDAR SETUP DEBUG ===")
        
        // Check permissions
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasWritePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "READ_CALENDAR permission: $hasReadPermission")
        Log.d(TAG, "WRITE_CALENDAR permission: $hasWritePermission")
        
        if (!hasReadPermission || !hasWritePermission) {
            Log.e(TAG, "Missing calendar permissions!")
            return
        }
        
        // List all calendars
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.IS_PRIMARY
        )
        
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                Log.d(TAG, "Found ${cursor.count} calendars:")
                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val displayName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                    val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))
                    val accessLevel = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL))
                    val visible = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE))
                    val syncEvents = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.SYNC_EVENTS))
                    val isPrimary = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY))
                    
                    Log.d(TAG, "Calendar #$index:")
                    Log.d(TAG, "  ID: $id")
                    Log.d(TAG, "  Display Name: $displayName")
                    Log.d(TAG, "  Account: $accountName")
                    Log.d(TAG, "  Type: $accountType")
                    Log.d(TAG, "  Access Level: $accessLevel (${getAccessLevelString(accessLevel)})")
                    Log.d(TAG, "  Visible: $visible")
                    Log.d(TAG, "  Sync Events: $syncEvents")
                    Log.d(TAG, "  Is Primary: $isPrimary")
                    index++
                }
                
                if (cursor.count == 0) {
                    Log.e(TAG, "NO CALENDARS FOUND! Please add a Google account to Calendar app.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendars", e)
        }
        
        // Try to create a test event
        Log.d(TAG, "=== TESTING EVENT CREATION ===")
        val calendarId = getPrimaryCalendarId()
        if (calendarId != -1L) {
            Log.d(TAG, "Creating test event in calendar ID: $calendarId")
            val testTask = Task(
                id = "test",
                title = "Test Event from Note By Voice",
                priority = Priority.MEDIUM,
                dueDate = Date(),
                completed = false,
                noteId = "test"
            )
            val success = addTaskToSpecificCalendar(testTask, calendarId)
            Log.d(TAG, "Test event creation result: $success")
        } else {
            Log.e(TAG, "Cannot create test event - no calendar found")
        }
        
        Log.d(TAG, "=== END CALENDAR DEBUG ===")
    }
    
    private fun getAccessLevelString(level: Int): String {
        return when (level) {
            CalendarContract.Calendars.CAL_ACCESS_NONE -> "NONE"
            CalendarContract.Calendars.CAL_ACCESS_FREEBUSY -> "FREEBUSY"
            CalendarContract.Calendars.CAL_ACCESS_READ -> "READ"
            CalendarContract.Calendars.CAL_ACCESS_RESPOND -> "RESPOND"
            CalendarContract.Calendars.CAL_ACCESS_OVERRIDE -> "OVERRIDE"
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR -> "CONTRIBUTOR"
            CalendarContract.Calendars.CAL_ACCESS_EDITOR -> "EDITOR"
            CalendarContract.Calendars.CAL_ACCESS_OWNER -> "OWNER"
            CalendarContract.Calendars.CAL_ACCESS_ROOT -> "ROOT"
            else -> "UNKNOWN($level)"
        }
    }
    
    private fun checkCalendarSyncStatus(calendarId: Long) {
        val projection = arrayOf(
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.VISIBLE
        )
        
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars._ID} = ?",
            arrayOf(calendarId.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val syncEvents = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.SYNC_EVENTS))
                val accountName = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                val accountType = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))
                val visible = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE))
                
                Log.d(TAG, "Calendar sync status - Sync: $syncEvents, Account: $accountName ($accountType), Visible: $visible")
                
                if (syncEvents == 0) {
                    Log.w(TAG, "Calendar sync is disabled! Events may not appear.")
                }
            }
        }
    }
}