package com.ppai.voicetotask.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object DateParser {
    private const val TAG = "DateParser"
    
    // Thai date terms
    private val THAI_DATE_TERMS = mapOf(
        "วันนี้" to 0,           // today
        "พรุ่งนี้" to 1,         // tomorrow
        "มะรืน" to 2,            // day after tomorrow
        "มะรืนนี้" to 2,         // day after tomorrow (alternative)
        "เมื่อวาน" to -1,        // yesterday
        "เมื่อวานนี้" to -1,     // yesterday (alternative)
        "สัปดาห์หน้า" to 7,      // next week
        "อาทิตย์หน้า" to 7,      // next week (alternative)
        "เดือนหน้า" to 30       // next month
    )
    
    // English date terms
    private val ENGLISH_DATE_TERMS = mapOf(
        "today" to 0,
        "tomorrow" to 1,
        "yesterday" to -1,
        "next week" to 7,
        "next month" to 30
    )
    
    // Thai day names
    private val THAI_DAYS = mapOf(
        "จันทร์" to Calendar.MONDAY,
        "อังคาร" to Calendar.TUESDAY,
        "พุธ" to Calendar.WEDNESDAY,
        "พฤหัส" to Calendar.THURSDAY,
        "พฤหัสบดี" to Calendar.THURSDAY,
        "ศุกร์" to Calendar.FRIDAY,
        "เสาร์" to Calendar.SATURDAY,
        "อาทิตย์" to Calendar.SUNDAY
    )
    
    // Thai month names
    private val THAI_MONTHS = mapOf(
        "มกราคม" to Calendar.JANUARY,
        "กุมภาพันธ์" to Calendar.FEBRUARY,
        "มีนาคม" to Calendar.MARCH,
        "เมษายน" to Calendar.APRIL,
        "พฤษภาคม" to Calendar.MAY,
        "มิถุนายน" to Calendar.JUNE,
        "กรกฎาคม" to Calendar.JULY,
        "สิงหาคม" to Calendar.AUGUST,
        "กันยายน" to Calendar.SEPTEMBER,
        "ตุลาคม" to Calendar.OCTOBER,
        "พฤศจิกายน" to Calendar.NOVEMBER,
        "ธันวาคม" to Calendar.DECEMBER
    )
    
    fun parseDateFromText(text: String): Date? {
        val lowerText = text.lowercase()
        
        // Check for relative date terms
        val calendar = Calendar.getInstance()
        
        // First, extract time if specified
        val timeInfo = parseTimeFromText(text)
        var hour = timeInfo?.first ?: 9  // Default to 9 AM
        var minute = timeInfo?.second ?: 0
        
        // Check Thai terms
        for ((term, daysToAdd) in THAI_DATE_TERMS) {
            if (text.contains(term)) {
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                // Set extracted time or default
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG, "Found Thai date term: $term with time $hour:$minute -> ${calendar.time}")
                return calendar.time
            }
        }
        
        // Check English terms
        for ((term, daysToAdd) in ENGLISH_DATE_TERMS) {
            if (lowerText.contains(term)) {
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                // Set extracted time or default
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG, "Found English date term: $term with time $hour:$minute -> ${calendar.time}")
                return calendar.time
            }
        }
        
        // Check for Thai day names (e.g., "วันจันทร์", "วันศุกร์")
        for ((dayName, dayOfWeek) in THAI_DAYS) {
            if (text.contains(dayName)) {
                // Find next occurrence of this day
                val targetDay = dayOfWeek
                val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
                var daysToAdd = targetDay - currentDay
                if (daysToAdd <= 0) {
                    daysToAdd += 7 // Next week
                }
                calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                // Set time to 9 AM
                calendar.set(Calendar.HOUR_OF_DAY, 9)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Log.d(TAG, "Found Thai day name: $dayName -> ${calendar.time}")
                return calendar.time
            }
        }
        
        // Check for date patterns (e.g., "15/3", "15/3/2024")
        val datePatterns = listOf(
            // Thai Buddhist Era year (add 543 to get Gregorian)
            Regex("(\\d{1,2})/(\\d{1,2})/(\\d{4})"), // 15/3/2567
            Regex("(\\d{1,2})/(\\d{1,2})"), // 15/3
            Regex("(\\d{1,2})\\s+(\\w+)"), // 15 มีนาคม
        )
        
        // Try parsing with date patterns
        for (pattern in datePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                try {
                    when (pattern.pattern) {
                        "(\\d{1,2})/(\\d{1,2})/(\\d{4})" -> {
                            val day = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt() - 1 // Calendar months are 0-based
                            var year = match.groupValues[3].toInt()
                            
                            // Check if it's Buddhist Era year (> 2500)
                            if (year > 2500) {
                                year -= 543 // Convert to Gregorian
                            }
                            
                            calendar.set(year, month, day, 9, 0, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            Log.d(TAG, "Parsed date pattern: $day/${month + 1}/$year -> ${calendar.time}")
                            return calendar.time
                        }
                        "(\\d{1,2})/(\\d{1,2})" -> {
                            val day = match.groupValues[1].toInt()
                            val month = match.groupValues[2].toInt() - 1
                            val year = calendar.get(Calendar.YEAR)
                            
                            calendar.set(year, month, day, 9, 0, 0)
                            calendar.set(Calendar.MILLISECOND, 0)
                            
                            // If the date is in the past, assume next year
                            if (calendar.timeInMillis < System.currentTimeMillis()) {
                                calendar.add(Calendar.YEAR, 1)
                            }
                            Log.d(TAG, "Parsed date pattern: $day/${month + 1} -> ${calendar.time}")
                            return calendar.time
                        }
                        "(\\d{1,2})\\s+(\\w+)" -> {
                            val day = match.groupValues[1].toInt()
                            val monthName = match.groupValues[2]
                            
                            // Check Thai month names
                            THAI_MONTHS[monthName]?.let { month ->
                                val year = calendar.get(Calendar.YEAR)
                                calendar.set(year, month, day, 9, 0, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                
                                // If the date is in the past, assume next year
                                if (calendar.timeInMillis < System.currentTimeMillis()) {
                                    calendar.add(Calendar.YEAR, 1)
                                }
                                Log.d(TAG, "Parsed Thai month: $day $monthName -> ${calendar.time}")
                                return calendar.time
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing date pattern", e)
                }
            }
        }
        
        Log.d(TAG, "No date found in text: $text")
        return null
    }
    
    fun parseTimeFromText(text: String): Pair<Int, Int>? {
        // Thai time patterns
        val thaiTimePatterns = listOf(
            // "10 โมง", "10 โมงเช้า"
            Regex("(\\d{1,2})\\s*โมง(?:เช้า)?"),
            // "บ่าย 3", "บ่าย 3 โมง"
            Regex("บ่าย\\s*(\\d{1,2})(?:\\s*โมง)?"),
            // "3 ทุ่ม", "4 ทุ่ม"
            Regex("(\\d{1,2})\\s*ทุ่ม"),
            // "เที่ยง"
            Regex("เที่ยง"),
            // "10:30", "10.30"
            Regex("(\\d{1,2})[:.]?(\\d{2})"),
        )
        
        // English time patterns
        val englishTimePatterns = listOf(
            // "10 am", "10am", "10 AM"
            Regex("(\\d{1,2})\\s*(?:am|AM|a\\.m\\.)"),
            // "3 pm", "3pm", "3 PM"
            Regex("(\\d{1,2})\\s*(?:pm|PM|p\\.m\\.)"),
            // "10:30", "10.30"
            Regex("(\\d{1,2})[:.]?(\\d{2})"),
        )
        
        // Check Thai patterns
        for (pattern in thaiTimePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                when (pattern.pattern) {
                    "(\\d{1,2})\\s*โมง(?:เช้า)?" -> {
                        val hour = match.groupValues[1].toInt()
                        return Pair(hour, 0)
                    }
                    "บ่าย\\s*(\\d{1,2})(?:\\s*โมง)?" -> {
                        val hour = match.groupValues[1].toInt()
                        return Pair(if (hour == 12) 12 else hour + 12, 0)
                    }
                    "(\\d{1,2})\\s*ทุ่ม" -> {
                        val thum = match.groupValues[1].toInt()
                        return Pair(18 + thum, 0) // 1 ทุ่ม = 19:00 (7 PM)
                    }
                    "เที่ยง" -> {
                        return Pair(12, 0)
                    }
                    "(\\d{1,2})[:.]?(\\d{2})" -> {
                        val hour = match.groupValues[1].toInt()
                        val minute = match.groupValues[2].toInt()
                        return Pair(hour, minute)
                    }
                }
            }
        }
        
        // Check English patterns
        for (pattern in englishTimePatterns) {
            val match = pattern.find(text.lowercase())
            if (match != null) {
                when {
                    pattern.pattern.contains("am|AM") -> {
                        val hour = match.groupValues[1].toInt()
                        return Pair(if (hour == 12) 0 else hour, 0)
                    }
                    pattern.pattern.contains("pm|PM") -> {
                        val hour = match.groupValues[1].toInt()
                        return Pair(if (hour == 12) 12 else hour + 12, 0)
                    }
                    pattern.pattern.contains("[:.]") -> {
                        val hour = match.groupValues[1].toInt()
                        val minute = if (match.groupValues.size > 2) match.groupValues[2].toInt() else 0
                        return Pair(hour, minute)
                    }
                }
            }
        }
        
        Log.d(TAG, "No time found in text: $text")
        return null
    }
    
    fun enhancePromptWithDateParsing(prompt: String): String {
        return prompt + """
            
            When parsing dates and times:
            - "วันนี้" or "today" = current date
            - "พรุ่งนี้" or "tomorrow" = next day
            - "มะรืน" or "มะรืนนี้" = day after tomorrow
            - Thai day names (จันทร์, อังคาร, พุธ, พฤหัส, ศุกร์, เสาร์, อาทิตย์) = next occurrence of that day
            - Date formats: DD/MM/YYYY (Buddhist Era year subtract 543), DD/MM, DD MonthName
            
            Time parsing:
            - Thai: "10 โมง" = 10:00, "บ่าย 3" = 15:00, "3 ทุ่ม" = 21:00, "เที่ยง" = 12:00
            - English: "10 am" = 10:00, "3 pm" = 15:00
            - Always extract specific times if mentioned, otherwise default to 09:00
        """.trimIndent()
    }
}