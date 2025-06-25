package com.ppai.voicetotask.domain.usecase

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import java.io.File
import java.nio.ByteBuffer

class AudioMerger {
    
    companion object {
        private const val TAG = "AudioMerger"
        private const val BUFFER_SIZE = 1024 * 1024 // 1MB buffer
    }
    
    fun mergeAudioFiles(file1: File, file2: File, outputFile: File): Boolean {
        try {
            // Create MediaMuxer for output
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            
            // Process first file
            val extractor1 = MediaExtractor()
            extractor1.setDataSource(file1.absolutePath)
            
            // Find audio track
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            
            for (i in 0 until extractor1.trackCount) {
                val format = extractor1.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            
            if (audioTrackIndex == -1 || audioFormat == null) {
                Log.e(TAG, "No audio track found in first file")
                return false
            }
            
            // Add audio track to muxer
            val outputAudioTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()
            
            // Copy data from first file
            extractor1.selectTrack(audioTrackIndex)
            val buffer = ByteBuffer.allocate(BUFFER_SIZE)
            val bufferInfo = MediaCodec.BufferInfo()
            
            var lastPresentationTimeUs = 0L
            
            while (true) {
                buffer.clear()
                val sampleSize = extractor1.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor1.sampleTime
                bufferInfo.flags = extractor1.sampleFlags
                bufferInfo.offset = 0
                
                muxer.writeSampleData(outputAudioTrackIndex, buffer, bufferInfo)
                lastPresentationTimeUs = bufferInfo.presentationTimeUs
                
                if (!extractor1.advance()) break
            }
            
            extractor1.release()
            
            // Process second file
            val extractor2 = MediaExtractor()
            extractor2.setDataSource(file2.absolutePath)
            
            // Find audio track in second file
            for (i in 0 until extractor2.trackCount) {
                val format = extractor2.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime?.startsWith("audio/") == true) {
                    extractor2.selectTrack(i)
                    break
                }
            }
            
            // Copy data from second file with adjusted timestamps
            while (true) {
                buffer.clear()
                val sampleSize = extractor2.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = extractor2.sampleTime + lastPresentationTimeUs
                bufferInfo.flags = extractor2.sampleFlags
                bufferInfo.offset = 0
                
                muxer.writeSampleData(outputAudioTrackIndex, buffer, bufferInfo)
                
                if (!extractor2.advance()) break
            }
            
            extractor2.release()
            
            // Finish muxing
            muxer.stop()
            muxer.release()
            
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to merge audio files", e)
            return false
        }
    }
}