package com.ppai.voicetotask.data.remote.api

import android.util.Log
import com.ppai.voicetotask.BuildConfig
import com.ppai.voicetotask.domain.model.Task
import com.ppai.voicetotask.domain.model.Priority
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.ppai.voicetotask.util.DateParser

@Singleton
class GeminiAiService @Inject constructor() : AiService {
    
    private fun detectLanguage(text: String): String {
        // Simple Thai language detection based on Thai Unicode range
        val thaiPattern = Regex("[\u0E00-\u0E7F]")
        val thaiCharCount = thaiPattern.findAll(text).count()
        val totalChars = text.filter { it.isLetter() }.length
        
        return if (totalChars > 0 && thaiCharCount.toFloat() / totalChars > 0.3f) {
            "th"
        } else {
            "en"
        }
    }
    
    companion object {
        private const val TAG = "GeminiAiService"
        private const val MODEL_NAME = "gemini-1.5-flash"
    }
    
    private val generativeModel = GenerativeModel(
        modelName = MODEL_NAME,
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    
    
    suspend fun generateTitle(text: String): String = withContext(Dispatchers.IO) {
        
        repeat(3) { attempt ->
            try {
                Log.d(TAG, "Generating title for text: ${text.take(100)}... (attempt ${attempt + 1})")
                
                val prompt = """
                    Generate a concise, descriptive title for the following transcript.
                    The title should be 3-8 words that capture the main topic or purpose.
                    Do not use quotes around the title.
                    Write in the same language as the transcript.
                    
                    Transcript:
                    $text
                    
                    Title:
                """.trimIndent()
                
                val response = generativeModel.generateContent(
                    content {
                        text(prompt)
                    }
                )
                
                val title = response.text?.trim()?.take(100) ?: "Voice Note"
                Log.d(TAG, "Title generated successfully: $title")
                return@withContext title
            } catch (e: Exception) {
                Log.e(TAG, "Title generation attempt ${attempt + 1} failed", e)
                
                if (attempt < 2) {
                    delay(500L * (attempt + 1))
                } else {
                    // Fallback to simple title generation
                    val firstSentence = text.split(Regex("[.!?]")).firstOrNull()?.trim() ?: text
                    return@withContext if (firstSentence.length > 50) {
                        firstSentence.take(47) + "..."
                    } else {
                        firstSentence.ifEmpty { 
                            if (detectLanguage(text) == "th") "บันทึกเสียง" else "Voice Note"
                        }
                    }
                }
            }
        }
        
        // Final fallback
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val fallbackTitle = if (Locale.getDefault().language == "th") {
            "บันทึกเสียง ${dateFormat.format(Date())}"
        } else {
            "Voice Note ${dateFormat.format(Date())}"
        }
        return@withContext fallbackTitle
    }
    
    override suspend fun generateSummary(text: String): String = withContext(Dispatchers.IO) {
        
        repeat(3) { attempt ->
            try {
                Log.d(TAG, "Generating summary for text: ${text.take(100)}... (attempt ${attempt + 1})")
                
                val prompt = """
                    Please provide a concise summary of the following transcript as bullet points.
                    - Include 3-5 key points
                    - Each bullet point should be clear and concise
                    - Focus on the main topics, decisions, and action items
                    - Write in the same language as the transcript
                    
                    Transcript:
                    $text
                    
                    Summary (as bullet points):
                """.trimIndent()
                
                val response = generativeModel.generateContent(
                    content {
                        text(prompt)
                    }
                )
                
                val summary = response.text?.trim() ?: "No summary generated"
                Log.d(TAG, "Summary generated successfully")
                return@withContext summary
            } catch (e: Exception) {
                Log.e(TAG, "Summary generation attempt ${attempt + 1} failed", e)
                
                // Check if it's a server error (5xx) and retry
                if (e is GoogleGenerativeAIException && (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true) && attempt < 2) {
                    Log.d(TAG, "Server error, retrying after delay...")
                    delay(1000L * (attempt + 1)) // Exponential backoff
                } else if (attempt < 2) {
                    // For other errors, also retry with delay
                    delay(500L * (attempt + 1))
                } else {
                    // On final attempt, return fallback instead of throwing
                    Log.e(TAG, "All summary attempts failed, returning fallback")
                    val fallbackMessage = if (detectLanguage(text) == "th") {
                        "ไม่สามารถสร้างสรุปได้ชั่วคราว"
                    } else {
                        "Summary generation temporarily unavailable"
                    }
                    return@withContext fallbackMessage
                }
            }
        }
        
        // Fallback if all retries failed
        return@withContext "Summary generation temporarily unavailable"
    }
    
    override suspend fun extractTasks(text: String): List<Task> = withContext(Dispatchers.IO) {
        
        repeat(3) { attempt ->
            try {
                Log.d(TAG, "Extracting tasks from text: ${text.take(100)}... (attempt ${attempt + 1})")
                
                val isThaiLanguage = detectLanguage(text) == "th"
                
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                
                val prompt = if (isThaiLanguage) {
                    """
                    ดึงงานที่ต้องทำจากข้อความต่อไปนี้
                    มองหา:
                    - สิ่งที่ต้องทำ (เช่น "ต้อง...", "ควร...", "จะ...")
                    - งานหรือสิ่งที่ต้องจำ (เช่น "โทรหา...", "ซื้อ...", "ทำ...")
                    - กำหนดเวลาหรืองานที่มีเวลาจำกัด
                    - สิ่งที่ต้องจำหรือเตือนความจำ
                    
                    สำคัญมาก: เขียนชื่องานทั้งหมดเป็นภาษาไทย
                    
                    ส่งผลลัพธ์เป็น JSON array โดยแต่ละงานมี:
                    - title: คำอธิบายงานที่ชัดเจนและกระชับเป็นภาษาไทย
                    - priority: "high", "medium", หรือ "low" (ตามความเร่งด่วนหรือความสำคัญ)
                    - dueDate: วันที่ในรูปแบบ ISO 8601 ถ้ามีการกล่าวถึงกำหนดเวลา (ไม่บังคับ, รูปแบบ: "2024-01-15T09:00:00")
                    
                    การแปลงวันที่และเวลา (วันนี้คือ $currentDate):
                    วันที่:
                    - "วันนี้" = ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}
                    - "พรุ่งนี้" = ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)}
                    - "มะรืน" หรือ "มะรืนนี้" = ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.time)}
                    - วันในสัปดาห์ (จันทร์, อังคาร, พุธ, พฤหัส, ศุกร์, เสาร์, อาทิตย์) = วันนั้นในสัปดาห์หน้า
                    - รูปแบบวันที่ DD/MM/YYYY (ถ้าปี > 2500 ให้ลบ 543)
                    
                    เวลา (สำคัญมาก - ต้องดึงเวลาที่ระบุ):
                    - "10 โมง" หรือ "10 โมงเช้า" = T10:00:00
                    - "บ่าย 3" หรือ "บ่าย 3 โมง" = T15:00:00
                    - "3 ทุ่ม" = T21:00:00 (18:00 + 3)
                    - "เที่ยง" = T12:00:00
                    - ถ้าไม่ระบุเวลา ให้ใช้ T09:00:00
                    
                    ตัวอย่าง:
                    [
                        {"title": "โทรนัดหมอฟัน", "priority": "medium"},
                        {"title": "ซื้อนมและขนมปัง", "priority": "low"},
                        {"title": "ทำรายงานโครงการให้เสร็จ", "priority": "high", "dueDate": "2024-01-20T09:00:00"}
                    ]
                    
                    ใส่เฉพาะงานที่ต้องทำจริงๆ ไม่ใช่คำพูดทั่วไปหรือการสังเกต
                    ถ้าไม่พบงานใดๆ ให้ส่ง []
                    
                    ข้อความ:
                    $text
                    
                    ผลลัพธ์ JSON:
                    """.trimIndent()
                } else {
                    """
                    Extract actionable tasks from the following transcript.
                    Look for:
                    - Action items mentioned (e.g., "I need to...", "I should...", "I have to...")
                    - To-dos or tasks (e.g., "call someone", "buy something", "finish something")
                    - Deadlines or time-sensitive items
                    - Reminders or things to remember
                    
                    IMPORTANT: Write all task titles in the same language as the transcript (English).
                    
                    Return the result as a JSON array where each task has:
                    - title: A clear, concise task description in English
                    - priority: "high", "medium", or "low" (based on urgency or importance mentioned)
                    - dueDate: ISO 8601 date string if a deadline is mentioned (optional, format: "2024-01-15T09:00:00")
                    
                    Date and time parsing (today is $currentDate):
                    Dates:
                    - "today" = ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}
                    - "tomorrow" = ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)}
                    - Day names (Monday, Tuesday, etc.) = next occurrence of that day
                    
                    Times (IMPORTANT - extract specific times if mentioned):
                    - "10 am" = T10:00:00
                    - "3 pm" = T15:00:00
                    - "noon" = T12:00:00
                    - If no time specified, use T09:00:00
                    
                    Example response:
                    [
                        {"title": "Call dentist to schedule appointment", "priority": "medium"},
                        {"title": "Buy milk and bread", "priority": "low"},
                        {"title": "Finish project report", "priority": "high", "dueDate": "2024-01-20T09:00:00"}
                    ]
                    
                    Only include actual actionable tasks, not general statements or observations.
                    If no tasks are found, return an empty array [].
                    
                    Transcript:
                    $text
                    
                    JSON Response:
                    """.trimIndent()
                }
                
                val response = generativeModel.generateContent(
                    content {
                        text(prompt)
                    }
                )
                
                val jsonText = response.text?.trim() ?: "[]"
                Log.d(TAG, "Received JSON response: $jsonText")
                
                val tasks = parseTasksFromJson(jsonText)
                Log.d(TAG, "Extracted ${tasks.size} tasks")
                tasks.forEachIndexed { index, task ->
                    Log.d(TAG, "Task $index: ${task.title}, Priority: ${task.priority}, Due: ${task.dueDate}")
                }
                return@withContext tasks
            } catch (e: Exception) {
                Log.e(TAG, "Task extraction attempt ${attempt + 1} failed", e)
                
                // Check if it's a server error (5xx) and retry
                if (e is GoogleGenerativeAIException && (e.message?.contains("503") == true || e.message?.contains("UNAVAILABLE") == true) && attempt < 2) {
                    Log.d(TAG, "Server error, retrying after delay...")
                    delay(1000L * (attempt + 1)) // Exponential backoff
                } else if (attempt < 2) {
                    // For other errors, also retry with delay
                    delay(500L * (attempt + 1))
                } else {
                    // On final attempt, return empty list instead of throwing
                    Log.e(TAG, "All task extraction attempts failed")
                    return@withContext emptyList()
                }
            }
        }
        
        // Fallback if all retries failed
        return@withContext emptyList()
    }
    
    private fun parseTasksFromJson(jsonText: String): List<Task> {
        return try {
            // Extract JSON array from response (in case there's extra text)
            val startIndex = jsonText.indexOf('[')
            val endIndex = jsonText.lastIndexOf(']')
            
            if (startIndex == -1 || endIndex == -1) {
                Log.w(TAG, "No JSON array found in response")
                return emptyList()
            }
            
            val jsonArrayString = jsonText.substring(startIndex, endIndex + 1)
            val jsonArray = JSONArray(jsonArrayString)
            
            val tasks = mutableListOf<Task>()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            
            for (i in 0 until jsonArray.length()) {
                try {
                    val taskJson = jsonArray.getJSONObject(i)
                    
                    val title = taskJson.getString("title")
                    val priorityStr = taskJson.optString("priority", "medium").lowercase()
                    val dueDateStr = taskJson.optString("dueDate", null)
                    
                    val priority = when (priorityStr) {
                        "high" -> Priority.HIGH
                        "low" -> Priority.LOW
                        else -> Priority.MEDIUM
                    }
                    
                    val dueDate = dueDateStr?.let {
                        try {
                            dateFormat.parse(it)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    tasks.add(
                        Task(
                            title = title,
                            priority = priority,
                            dueDate = dueDate,
                            noteId = "" // Will be set later in ProcessVoiceNoteUseCase
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse task at index $i", e)
                }
            }
            
            tasks
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tasks JSON", e)
            emptyList()
        }
    }
}