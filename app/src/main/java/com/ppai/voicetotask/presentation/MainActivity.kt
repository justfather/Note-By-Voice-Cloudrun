package com.ppai.voicetotask.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ppai.voicetotask.presentation.navigation.DetailScreen
import com.ppai.voicetotask.presentation.navigation.Screen
import com.ppai.voicetotask.presentation.navigation.VoiceToTaskNavigation
import com.ppai.voicetotask.presentation.theme.VoiceToTaskTheme
import com.ppai.voicetotask.presentation.ui.components.GradientBackground
import com.ppai.voicetotask.presentation.ui.components.RecordingFAB
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceToTaskTheme {
                VoiceToTaskApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceToTaskApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val bottomNavItems = listOf(
        Screen.Notes,
        Screen.Tasks,
        Screen.Settings
    )
    
    val showBottomBar = when {
        currentRoute in bottomNavItems.map { it.route } -> true
        currentRoute?.startsWith("note_detail/") == true -> true
        else -> false
    }
    val showFab = currentRoute == Screen.Notes.route || currentRoute == Screen.Tasks.route
    
    GradientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    screen.icon, 
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(20.dp)
                                ) 
                            },
                            label = { 
                                Text(
                                    screen.title,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                RecordingFAB(
                    onNavigateToRecording = {
                        android.util.Log.d("MainActivity", "FAB permission check passed - navigating to recording screen")
                        try {
                            navController.navigate(DetailScreen.Recording.route)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Failed to navigate to recording screen", e)
                            // Show error toast
                            android.util.Log.e("MainActivity", "Navigation error: ${e.message}")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
                VoiceToTaskNavigation(navController = navController)
            }
        }
    }
}