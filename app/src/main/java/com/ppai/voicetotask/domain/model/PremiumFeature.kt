package com.ppai.voicetotask.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class PremiumFeature(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isFreeFeature: Boolean = false
) {
    // Free features
    BASIC_RECORDING(
        title = "Voice Recording",
        description = "Record up to 2 minutes",
        icon = Icons.Default.Mic,
        isFreeFeature = true
    ),
    BASIC_TRANSCRIPTION(
        title = "AI Transcription",
        description = "Convert speech to text",
        icon = Icons.Default.TextFields,
        isFreeFeature = true
    ),
    BASIC_TASKS(
        title = "Task Extraction",
        description = "Extract tasks from notes",
        icon = Icons.Default.Task,
        isFreeFeature = true
    ),
    MONTHLY_LIMIT(
        title = "Monthly Recordings",
        description = "30 recordings per month",
        icon = Icons.Default.CalendarMonth,
        isFreeFeature = true
    ),
    
    // Premium features
    EXTENDED_RECORDING(
        title = "Extended Recording",
        description = "Record up to 10 minutes",
        icon = Icons.Default.Timer
    ),
    UNLIMITED_RECORDINGS(
        title = "Unlimited Recordings",
        description = "No monthly limits",
        icon = Icons.Default.AllInclusive
    ),
    NO_ADS(
        title = "Ad-Free Experience",
        description = "No interruptions",
        icon = Icons.Default.Block
    ),
    BACKGROUND_PROCESSING(
        title = "Background Processing",
        description = "Continue using the app while processing",
        icon = Icons.Default.Sync
    ),
    PRIORITY_PROCESSING(
        title = "Priority Processing",
        description = "Faster AI processing",
        icon = Icons.Default.Speed
    ),
    ADVANCED_AI(
        title = "Advanced AI Features",
        description = "Better summaries & task extraction",
        icon = Icons.Default.AutoAwesome
    ),
    EXPORT_OPTIONS(
        title = "Export Options",
        description = "Export to PDF, Markdown, CSV",
        icon = Icons.Default.FileDownload
    ),
    CALENDAR_SYNC(
        title = "Calendar Integration",
        description = "Sync tasks with calendar",
        icon = Icons.Default.CalendarToday
    );
    
    companion object {
        fun getFreeFeatures() = values().filter { it.isFreeFeature }
        fun getPremiumFeatures() = values().filter { !it.isFreeFeature }
    }
}