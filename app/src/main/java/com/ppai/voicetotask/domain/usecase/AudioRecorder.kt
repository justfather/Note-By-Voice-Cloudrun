package com.ppai.voicetotask.domain.usecase

import java.io.File

interface AudioRecorder {
    fun startRecording(outputFile: File)
    fun pauseRecording()
    fun resumeRecording()
    fun stopRecording(): File?
    fun isRecording(): Boolean
    fun isPaused(): Boolean
    fun cancelRecording()
}