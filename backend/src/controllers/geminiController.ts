import { Response, NextFunction } from 'express';
import { AuthRequest } from '../types';
import { geminiService } from '../services/geminiService';
import { AppError } from '../middleware/errorHandler';

export const generateTitle = async (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const { transcript, language } = req.body;

    if (!transcript) {
      throw new AppError('Transcript is required', 400);
    }

    const title = await geminiService.generateTitle({ transcript, language });
    
    res.json({
      status: 'success',
      data: { title },
    });
  } catch (error) {
    next(error);
  }
};

export const generateSummary = async (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const { transcript, language } = req.body;

    if (!transcript) {
      throw new AppError('Transcript is required', 400);
    }

    const summary = await geminiService.generateSummary({ transcript, language });
    
    res.json({
      status: 'success',
      data: { summary },
    });
  } catch (error) {
    next(error);
  }
};

export const extractTasks = async (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const { transcript, currentDate, language } = req.body;

    if (!transcript) {
      throw new AppError('Transcript is required', 400);
    }

    if (!currentDate) {
      throw new AppError('Current date is required', 400);
    }

    const tasks = await geminiService.extractTasks({ transcript, currentDate, language });
    
    res.json({
      status: 'success',
      data: { tasks },
    });
  } catch (error) {
    next(error);
  }
};

export const processVoiceNote = async (
  req: AuthRequest,
  res: Response,
  next: NextFunction
) => {
  try {
    const { transcript, currentDate, language } = req.body;

    if (!transcript) {
      throw new AppError('Transcript is required', 400);
    }

    if (!currentDate) {
      throw new AppError('Current date is required', 400);
    }

    const result = await geminiService.processVoiceNote({ 
      transcript, 
      currentDate, 
      language 
    });
    
    res.json({
      status: 'success',
      data: result,
    });
  } catch (error) {
    next(error);
  }
};