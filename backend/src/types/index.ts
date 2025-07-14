import { Request } from 'express';

export interface AuthRequest extends Request {
  userId?: string;
  userEmail?: string;
}

export interface TranscriptionRequest {
  audioBase64?: string;
  audioUrl?: string;
  language?: string;
}

export interface GenerateTitleRequest {
  transcript: string;
  language?: string;
}

export interface GenerateSummaryRequest {
  transcript: string;
  language?: string;
}

export interface ExtractTasksRequest {
  transcript: string;
  currentDate: string;
  language?: string;
}

export interface ProcessVoiceNoteRequest {
  transcript: string;
  currentDate: string;
  language?: string;
}

export interface GeminiResponse {
  title?: string;
  summary?: string;
  tasks?: Array<{
    title: string;
    priority: 'Low' | 'Medium' | 'High';
    dueDate?: string;
  }>;
}

export interface User {
  id: string;
  email: string;
  passwordHash: string;
  createdAt: Date;
  updatedAt: Date;
  isActive: boolean;
  isPremium: boolean;
  monthlyRecordingCount: number;
  lastResetDate: Date;
}