package com.ppai.voicetotask.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ppai.voicetotask.data.local.dao.NoteDao
import com.ppai.voicetotask.data.local.dao.TaskDao
import com.ppai.voicetotask.data.local.database.VoiceToTaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE notes ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    @Provides
    @Singleton
    fun provideVoiceToTaskDatabase(
        @ApplicationContext context: Context
    ): VoiceToTaskDatabase {
        return Room.databaseBuilder(
            context,
            VoiceToTaskDatabase::class.java,
            VoiceToTaskDatabase.DATABASE_NAME
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }
    
    @Provides
    @Singleton
    fun provideNoteDao(database: VoiceToTaskDatabase): NoteDao {
        return database.noteDao()
    }
    
    @Provides
    @Singleton
    fun provideTaskDao(database: VoiceToTaskDatabase): TaskDao {
        return database.taskDao()
    }
}