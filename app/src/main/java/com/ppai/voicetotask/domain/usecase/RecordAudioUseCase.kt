package com.ppai.voicetotask.domain.usecase

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import android.util.Log

class RecordAudioUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "RecordAudioUseCase"
    }
    
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var previousAudioFile: File? = null  // Store previous audio for resume
    private var isResuming: Boolean = false
    private val audioMerger = AudioMerger()
    private var pausedAudioFiles: MutableList<File> = mutableListOf()
    private var isPaused: Boolean = false
    private var isRecording: Boolean = false
    
    fun startRecording(resumeFromPrevious: Boolean = false) {
        try {
            Log.d(TAG, "Starting recording, resumeFromPrevious: $resumeFromPrevious")
            
            // Create audio file
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            // If resuming, keep the previous audio file reference
            if (resumeFromPrevious && previousAudioFile?.exists() == true) {
                isResuming = true
            } else {
                isResuming = false
                previousAudioFile = null
            }
            
            audioFile = File(recordingsDir, "recording_${System.currentTimeMillis()}.m4a")
            Log.d(TAG, "Audio file path: ${audioFile?.absolutePath}")
            
            // Release any existing recorder
            mediaRecorder?.release()
            mediaRecorder = null
            
            // Initialize MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    Log.d(TAG, "Setting audio source...")
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    Log.d(TAG, "Audio source set successfully")
                    
                    Log.d(TAG, "Setting output format...")
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    Log.d(TAG, "Output format set successfully")
                    
                    Log.d(TAG, "Setting audio encoder...")
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    Log.d(TAG, "Audio encoder configured successfully")
                    
                    Log.d(TAG, "Setting output file: ${audioFile?.absolutePath}")
                    setOutputFile(audioFile?.absolutePath)
                    Log.d(TAG, "Output file set successfully")
                    
                    Log.d(TAG, "Preparing MediaRecorder...")
                    prepare()
                    Log.d(TAG, "MediaRecorder prepared successfully")
                    
                    Log.d(TAG, "Starting MediaRecorder...")
                    start()
                    isRecording = true
                    Log.d(TAG, "Recording started successfully")
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException: Microphone permission denied", e)
                    release()
                    throw RecordingException("Microphone permission denied. Please grant permission to record audio.", e)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "IllegalStateException: MediaRecorder in invalid state", e)
                    release()
                    throw RecordingException("Recording failed: Device may be in use by another app.", e)
                } catch (e: IOException) {
                    Log.e(TAG, "IOException: Failed to access storage", e)
                    release()
                    throw RecordingException("Failed to create recording file. Check storage permissions.", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error starting MediaRecorder", e)
                    release()
                    throw RecordingException("Failed to start recording: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            throw RecordingException("Failed to start recording: ${e.message}", e)
        }
    }
    
    fun pauseRecording() {
        try {
            Log.d(TAG, "Pausing recording, isRecording: $isRecording")
            
            if (!isRecording) {
                Log.w(TAG, "Cannot pause - not recording")
                return
            }
            
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping recorder during pause", e)
                }
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            // Add current recording to paused files list
            audioFile?.let { 
                if (it.exists()) {
                    pausedAudioFiles.add(it)
                    Log.d(TAG, "Added file to paused list: ${it.name}, size: ${it.length()}")
                }
            }
            isPaused = true
            
            Log.d(TAG, "Recording paused. Total paused files: ${pausedAudioFiles.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause recording", e)
            throw RecordingException("Failed to pause recording", e)
        }
    }
    
    fun resumeRecording() {
        try {
            Log.d(TAG, "Resuming recording, isPaused: $isPaused")
            
            // Create new audio file for resumed recording
            val recordingsDir = File(context.filesDir, "recordings")
            if (!recordingsDir.exists()) {
                recordingsDir.mkdirs()
            }
            
            audioFile = File(recordingsDir, "recording_${System.currentTimeMillis()}.m4a")
            
            // Release any existing recorder
            mediaRecorder?.release()
            mediaRecorder = null
            
            // Initialize MediaRecorder for resumed recording
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                try {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setAudioEncodingBitRate(128000)
                    setAudioSamplingRate(44100)
                    setOutputFile(audioFile?.absolutePath)
                    
                    prepare()
                    start()
                    isRecording = true
                    Log.d(TAG, "Recording resumed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to resume MediaRecorder", e)
                    release()
                    throw e
                }
            }
            
            isPaused = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume recording", e)
            isRecording = false
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            throw RecordingException("Failed to resume recording: ${e.message}", e)
        }
    }
    
    fun stopRecording(): File? {
        return try {
            Log.d(TAG, "Stopping recording, isRecording: $isRecording")
            
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping recorder", e)
                    }
                    release()
                }
                isRecording = false
            }
            mediaRecorder = null
            
            // Add final recording to paused files if any
            audioFile?.let { 
                if (it.exists() && it.length() > 0) {
                    pausedAudioFiles.add(it)
                    Log.d(TAG, "Added final file: ${it.name}, size: ${it.length()}")
                }
            }
            
            // Merge all audio files if we have multiple segments
            val finalFile = when {
                pausedAudioFiles.isEmpty() -> {
                    Log.w(TAG, "No audio files recorded")
                    null
                }
                pausedAudioFiles.size > 1 -> {
                    Log.d(TAG, "Merging ${pausedAudioFiles.size} audio segments")
                    mergeMultipleAudioFiles(pausedAudioFiles)
                }
                pausedAudioFiles.size == 1 -> {
                    pausedAudioFiles.first()
                }
                else -> {
                    // If we were resuming from previous session
                    if (isResuming && previousAudioFile?.exists() == true && audioFile?.exists() == true) {
                        val mergedFile = mergeAudioFiles(previousAudioFile!!, audioFile!!)
                        previousAudioFile?.delete()
                        audioFile?.delete()
                        mergedFile
                    } else {
                        audioFile
                    }
                }
            }
            
            // Store for potential future resume and clean up
            previousAudioFile = finalFile
            pausedAudioFiles.clear()
            isPaused = false
            isResuming = false
            
            Log.d(TAG, "Recording stopped. Final file: ${finalFile?.name}, size: ${finalFile?.length()}")
            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopRecording", e)
            // Clean up on error
            pausedAudioFiles.forEach { it.delete() }
            pausedAudioFiles.clear()
            audioFile?.delete()
            null
        }
    }
    
    fun cancelRecording() {
        try {
            Log.d(TAG, "Canceling recording")
            
            if (isRecording) {
                mediaRecorder?.apply {
                    try {
                        stop()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping recorder during cancel", e)
                    }
                    release()
                }
                isRecording = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cancel", e)
        } finally {
            mediaRecorder = null
            audioFile?.delete()
            audioFile = null
            previousAudioFile?.delete()
            previousAudioFile = null
            isResuming = false
            pausedAudioFiles.forEach { it.delete() }
            pausedAudioFiles.clear()
            isPaused = false
        }
    }
    
    fun hasPreviousRecording(): Boolean {
        return previousAudioFile?.exists() == true
    }
    
    fun isCurrentlyRecording(): Boolean {
        return isRecording
    }
    
    private fun mergeAudioFiles(file1: File, file2: File): File {
        // Create merged file
        val recordingsDir = File(context.filesDir, "recordings")
        val mergedFile = File(recordingsDir, "merged_${System.currentTimeMillis()}.m4a")
        
        Log.d(TAG, "Merging audio files: ${file1.name} + ${file2.name} -> ${mergedFile.name}")
        
        // Use AudioMerger for proper audio merging
        val success = audioMerger.mergeAudioFiles(file1, file2, mergedFile)
        
        if (!success) {
            Log.e(TAG, "Failed to merge audio files, falling back to simple concatenation")
            // Fallback to simple concatenation if MediaMuxer fails
            try {
                mergedFile.outputStream().use { output ->
                    file1.inputStream().use { input1 ->
                        input1.copyTo(output)
                    }
                    file2.inputStream().use { input2 ->
                        input2.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback concatenation also failed", e)
                throw RecordingException("Failed to merge audio files", e)
            }
        }
        
        Log.d(TAG, "Audio merge completed. Merged file size: ${mergedFile.length()} bytes")
        return mergedFile
    }
    
    private fun mergeMultipleAudioFiles(files: List<File>): File {
        if (files.isEmpty()) throw RecordingException("No files to merge")
        if (files.size == 1) return files.first()
        
        val recordingsDir = File(context.filesDir, "recordings")
        var currentMerged = files.first()
        
        // Merge files sequentially
        for (i in 1 until files.size) {
            val tempMerged = File(recordingsDir, "temp_merged_${System.currentTimeMillis()}.m4a")
            
            Log.d(TAG, "Merging segment $i/${files.size - 1}")
            
            val success = audioMerger.mergeAudioFiles(currentMerged, files[i], tempMerged)
            
            if (!success) {
                Log.e(TAG, "Failed to merge segment $i")
                // Fallback to simple concatenation
                try {
                    tempMerged.outputStream().use { output ->
                        currentMerged.inputStream().use { input ->
                            input.copyTo(output)
                        }
                        files[i].inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Fallback concatenation also failed", e)
                    throw RecordingException("Failed to merge audio segments", e)
                }
            }
            
            // Clean up previous merged file if it's not the original first file
            if (i > 1) {
                currentMerged.delete()
            }
            
            currentMerged = tempMerged
        }
        
        // Clean up original segment files
        files.forEach { file ->
            if (file != currentMerged) {
                file.delete()
            }
        }
        
        // Rename to final file
        val finalFile = File(recordingsDir, "merged_${System.currentTimeMillis()}.m4a")
        currentMerged.renameTo(finalFile)
        
        Log.d(TAG, "Multi-file merge completed. Final size: ${finalFile.length()} bytes")
        return finalFile
    }
}

class RecordingException(message: String, cause: Throwable? = null) : Exception(message, cause)