package com.ppai.voicetotask.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ppai.voicetotask.presentation.ui.notes.NotesListScreen
import com.ppai.voicetotask.presentation.ui.notes.NoteDetailScreen
import com.ppai.voicetotask.presentation.ui.tasks.TasksListScreen
import com.ppai.voicetotask.presentation.ui.recording.RecordingScreen
import com.ppai.voicetotask.presentation.ui.settings.SettingsScreen

@Composable
fun VoiceToTaskNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Notes.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Bottom Navigation Screens
        composable(Screen.Notes.route) {
            NotesListScreen(
                onNoteClick = { noteId ->
                    navController.navigate(DetailScreen.NoteDetail.createRoute(noteId))
                }
            )
        }
        
        composable(Screen.Tasks.route) {
            TasksListScreen()
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
        
        // Detail Screens
        composable(
            route = DetailScreen.NoteDetail.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getString("noteId") ?: ""
            NoteDetailScreen(
                noteId = noteId,
                onBackClick = { navController.popBackStack() }
            )
        }
        
        composable(DetailScreen.Recording.route) {
            RecordingScreen(
                onRecordingComplete = { noteId ->
                    if (noteId != null) {
                        // Navigate to the note detail screen with the saved note ID
                        navController.navigate(DetailScreen.NoteDetail.createRoute(noteId)) {
                            // Remove the recording screen from the back stack
                            popUpTo(DetailScreen.Recording.route) { inclusive = true }
                        }
                    } else {
                        // If no note ID, just go back
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}