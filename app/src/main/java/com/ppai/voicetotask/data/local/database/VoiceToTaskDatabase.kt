package com.ppai.voicetotask.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ppai.voicetotask.data.local.dao.NoteDao
import com.ppai.voicetotask.data.local.dao.TaskDao
import com.ppai.voicetotask.data.local.entities.NoteEntity
import com.ppai.voicetotask.data.local.entities.TaskEntity

@Database(
    entities = [NoteEntity::class, TaskEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class VoiceToTaskDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    
    companion object {
        const val DATABASE_NAME = "voice_to_task_db"
    }
}