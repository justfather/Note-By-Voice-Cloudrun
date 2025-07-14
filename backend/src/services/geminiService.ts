import { GoogleGenerativeAI } from '@google/generative-ai';
import { secretManager } from './secretManager';
import { 
  GenerateTitleRequest, 
  GenerateSummaryRequest, 
  ExtractTasksRequest,
  ProcessVoiceNoteRequest,
  GeminiResponse 
} from '../types';

class GeminiService {
  private genAI: GoogleGenerativeAI | null = null;

  private async getGenAI(): Promise<GoogleGenerativeAI> {
    if (!this.genAI) {
      const apiKey = await secretManager.getGeminiApiKey();
      this.genAI = new GoogleGenerativeAI(apiKey);
    }
    return this.genAI;
  }

  async generateTitle(request: GenerateTitleRequest): Promise<string> {
    try {
      const genAI = await this.getGenAI();
      const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
      
      const prompt = `Given the following transcript, generate a concise and descriptive title (max 50 characters):

Transcript: ${request.transcript}

Requirements:
- Title should be clear and descriptive
- Maximum 50 characters
- ${request.language ? `Output in ${request.language}` : 'Auto-detect language from transcript'}
- Return only the title, nothing else`;

      const result = await model.generateContent(prompt);
      const response = await result.response;
      const title = response.text().trim();
      
      return title.substring(0, 50); // Ensure max 50 chars
    } catch (error) {
      console.error('Error generating title:', error);
      throw new Error('Failed to generate title');
    }
  }

  async generateSummary(request: GenerateSummaryRequest): Promise<string> {
    try {
      const genAI = await this.getGenAI();
      const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
      
      const prompt = `Summarize the following transcript into clear, concise bullet points:

Transcript: ${request.transcript}

Requirements:
- Create 3-5 bullet points
- Each point should be concise and informative
- ${request.language ? `Output in ${request.language}` : 'Auto-detect language from transcript'}
- Format as: • Point 1\n• Point 2\n• Point 3`;

      const result = await model.generateContent(prompt);
      const response = await result.response;
      return response.text().trim();
    } catch (error) {
      console.error('Error generating summary:', error);
      throw new Error('Failed to generate summary');
    }
  }

  async extractTasks(request: ExtractTasksRequest): Promise<GeminiResponse['tasks']> {
    try {
      const genAI = await this.getGenAI();
      const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
      
      const prompt = `Extract actionable tasks from the following transcript:

Transcript: ${request.transcript}
Current Date: ${request.currentDate}

Requirements:
- Identify clear action items mentioned in the transcript
- Assign priority: High (urgent/important), Medium (normal), Low (optional/nice-to-have)
- Extract due dates if mentioned (return in ISO format YYYY-MM-DD)
- ${request.language ? `Output task titles in ${request.language}` : 'Auto-detect language from transcript'}
- Return as valid JSON array only

Expected format:
[
  {
    "title": "Task description",
    "priority": "High|Medium|Low",
    "dueDate": "YYYY-MM-DD or null"
  }
]`;

      const result = await model.generateContent(prompt);
      const response = await result.response;
      const text = response.text().trim();
      
      // Extract JSON from response
      const jsonMatch = text.match(/\[[\s\S]*\]/);
      if (!jsonMatch) {
        return [];
      }
      
      try {
        const tasks = JSON.parse(jsonMatch[0]);
        return tasks.filter((task: any) => 
          task.title && 
          ['Low', 'Medium', 'High'].includes(task.priority)
        );
      } catch (parseError) {
        console.error('Error parsing tasks JSON:', parseError);
        return [];
      }
    } catch (error) {
      console.error('Error extracting tasks:', error);
      throw new Error('Failed to extract tasks');
    }
  }

  async processVoiceNote(request: ProcessVoiceNoteRequest): Promise<GeminiResponse> {
    try {
      // Process all operations in parallel for efficiency
      const [title, summary, tasks] = await Promise.all([
        this.generateTitle({ transcript: request.transcript, language: request.language }),
        this.generateSummary({ transcript: request.transcript, language: request.language }),
        this.extractTasks({ 
          transcript: request.transcript, 
          currentDate: request.currentDate,
          language: request.language 
        }),
      ]);

      return {
        title,
        summary,
        tasks,
      };
    } catch (error) {
      console.error('Error processing voice note:', error);
      throw new Error('Failed to process voice note');
    }
  }
}

export const geminiService = new GeminiService();