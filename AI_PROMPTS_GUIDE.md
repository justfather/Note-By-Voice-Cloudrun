# AI Prompts Guide - TestVoicetoTask App

This document shows all AI prompt locations in the codebase where you can modify the prompts for speech-to-text, summary generation, and task extraction.

## 1. Speech-to-Text Prompt

**File:** `/app/src/main/java/com/example/testvoicetotask/data/remote/api/GeminiSpeechToTextService.kt`

**Location:** Line 51

**Current Prompt:**
```kotlin
text("Generate a transcript of the speech in the audio file. Provide only the transcript without any additional commentary or formatting.")
```

**How to Modify:**
- Change this prompt to customize how Gemini transcribes audio
- You can add language hints, formatting preferences, or specific instructions
- Example modifications:
  ```kotlin
  // For better punctuation:
  text("Generate a transcript of the speech with proper punctuation and capitalization. Provide only the transcript.")
  
  // For specific language:
  text("Generate a transcript of the Spanish speech in the audio file. Provide only the transcript.")
  
  // For technical content:
  text("Generate a transcript focusing on technical terms and proper nouns. Provide only the transcript.")
  ```

## 2. Summary Generation Prompt

**File:** `/app/src/main/java/com/example/testvoicetotask/data/remote/api/GeminiAiService.kt`

**Location:** Lines 39-48

**Current Prompt:**
```kotlin
val prompt = """
    Please provide a concise summary of the following transcript in 2-3 sentences.
    Focus on the main topics and important points.
    Write in the same language as the transcript.
    
    Transcript:
    $text
    
    Summary:
""".trimIndent()
```

**How to Modify:**
- Adjust the summary length (e.g., "in 4-5 sentences")
- Change the focus (e.g., "Focus on action items and decisions")
- Add specific formatting requirements
- Example modifications:
  ```kotlin
  // For bullet points:
  val prompt = """
      Summarize the following transcript as 3-5 bullet points.
      Each bullet should capture a key point or decision.
      
      Transcript:
      $text
      
      Summary:
  """.trimIndent()
  
  // For meeting notes:
  val prompt = """
      Provide a meeting summary including:
      - Main discussion points
      - Decisions made
      - Next steps
      Keep it under 100 words.
      
      Transcript:
      $text
  """.trimIndent()
  ```

## 3. Task Extraction Prompt

**File:** `/app/src/main/java/com/example/testvoicetotask/data/remote/api/GeminiAiService.kt`

**Location:** Lines 87-101

**Current Prompt:**
```kotlin
val prompt = """
    Extract actionable tasks from the following transcript.
    Return the result as a JSON array where each task has:
    - title: A clear, concise task description
    - priority: "high", "medium", or "low"
    - dueDate: ISO 8601 date string if a deadline is mentioned (optional)
    
    Only include actual tasks or to-dos, not general statements.
    If no tasks are found, return an empty array [].
    
    Transcript:
    $text
    
    JSON Response:
""".trimIndent()
```

**How to Modify:**
- Change task detection criteria
- Add more fields to the JSON output
- Modify priority assignment rules
- Example modifications:
  ```kotlin
  // For project management:
  val prompt = """
      Extract actionable tasks and milestones from the transcript.
      Return as JSON array with:
      - title: Task description (start with verb)
      - priority: "urgent", "high", "medium", or "low"
      - assignee: Person responsible if mentioned
      - dueDate: ISO 8601 date if mentioned
      - category: "development", "design", "testing", or "other"
      
      Include both explicit tasks and implied action items.
      
      Transcript:
      $text
      
      JSON Response:
  """.trimIndent()
  
  // For personal todo lists:
  val prompt = """
      Find all personal tasks and reminders in this transcript.
      Return as JSON array with:
      - title: What needs to be done
      - priority: Based on urgency words used
      - context: "home", "work", "personal", or "shopping"
      
      Include shopping items, appointments, and reminders.
      
      Transcript:
      $text
  """.trimIndent()
  ```

## Additional Customization Tips

### 1. Model Selection
**File:** Both service files
**Lines:** 23-24 (GeminiSpeechToTextService.kt) and 27-29 (GeminiAiService.kt)

Current model: `gemini-1.5-flash`
You can change to other models like:
- `gemini-1.5-pro` for better quality
- `gemini-1.0-pro` for faster response

### 2. Error Handling and Retry Logic
Both services include retry logic (3 attempts) with exponential backoff. You can modify:
- Number of retry attempts (currently 3)
- Delay between retries
- Fallback responses

### 3. Response Processing
The task extraction includes JSON parsing logic that you might need to adjust based on your prompt changes.

## Best Practices for Prompt Modification

1. **Test thoroughly** after changing prompts
2. **Keep prompts concise** but clear
3. **Include examples** in prompts when needed
4. **Specify output format** explicitly
5. **Handle edge cases** (empty input, different languages)
6. **Version your prompts** for easy rollback

## Example Use Cases

### Medical Notes
```kotlin
// In GeminiAiService.kt for summary:
"Summarize this medical consultation focusing on symptoms, diagnosis, and treatment plan."

// For tasks:
"Extract follow-up appointments, medication schedules, and patient action items."
```

### Educational Content
```kotlin
// For summary:
"Create study notes highlighting key concepts, definitions, and important facts."

// For tasks:
"Extract homework assignments, reading tasks, and study goals with deadlines."
```

### Business Meetings
```kotlin
// For summary:
"Summarize focusing on decisions, action items, and key discussion points."

// For tasks:
"Extract all commitments made by participants with owners and deadlines."
```