package com.ppai.voicetotask.presentation.ui.debug

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MicrophoneTestScreen() {
    val context = LocalContext.current
    val micPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    var isRecording by remember { mutableStateOf(false) }
    var audioLevel by remember { mutableStateOf(0f) }
    var micStatus by remember { mutableStateOf("ไม่ทราบสถานะ") }
    
    LaunchedEffect(micPermissionState.status.isGranted) {
        if (micPermissionState.status.isGranted) {
            micStatus = "ไมค์พร้อมใช้งาน"
        } else {
            micStatus = "ไม่ได้รับอนุญาตใช้ไมค์"
        }
    }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            withContext(Dispatchers.IO) {
                val bufferSize = AudioRecord.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                try {
                    val audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        44100,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    
                    audioRecord.startRecording()
                    val buffer = ShortArray(bufferSize)
                    
                    while (isActive && isRecording) {
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            val amplitude = buffer.take(read).map { kotlin.math.abs(it.toInt()) }.average()
                            withContext(Dispatchers.Main) {
                                audioLevel = ((amplitude / 32768.0).toFloat()).coerceIn(0f, 1f)
                            }
                        }
                        delay(100)
                    }
                    
                    audioRecord.stop()
                    audioRecord.release()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        micStatus = "เกิดข้อผิดพลาด: ${e.message}"
                    }
                }
            }
        } else {
            audioLevel = 0f
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ทดสอบไมโครโฟน",
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Text(
            text = "สถานะ: $micStatus",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // แสดงระดับเสียง
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 32.dp)
        ) {
            LinearProgressIndicator(
                progress = audioLevel,
                modifier = Modifier.fillMaxSize(),
                color = when {
                    audioLevel > 0.7f -> Color.Red
                    audioLevel > 0.4f -> Color.Yellow
                    else -> Color.Green
                }
            )
        }
        
        Text(
            text = "ระดับเสียง: ${(audioLevel * 100).toInt()}%",
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!micPermissionState.status.isGranted) {
            Button(
                onClick = { micPermissionState.launchPermissionRequest() }
            ) {
                Text("ขออนุญาตใช้ไมโครโฟน")
            }
        } else {
            Button(
                onClick = { isRecording = !isRecording },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRecording) "หยุดทดสอบ" else "เริ่มทดสอบไมค์")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (isRecording) "พูดใส่ไมค์เพื่อดูระดับเสียง" else "กดปุ่มเพื่อเริ่มทดสอบ",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}