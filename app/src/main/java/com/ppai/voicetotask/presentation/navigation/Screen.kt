package com.ppai.voicetotask.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Notes : Screen(
        route = "notes",
        title = "Notes",
        icon = Icons.Default.Mic
    )
    
    object Tasks : Screen(
        route = "tasks",
        title = "Tasks",
        icon = Icons.Default.CheckBox
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )
}

sealed class DetailScreen(val route: String) {
    object NoteDetail : DetailScreen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
    
    object EditNote : DetailScreen("edit_note/{noteId}") {
        fun createRoute(noteId: String) = "edit_note/$noteId"
    }
    
    object Recording : DetailScreen("recording")
}