import { Router } from 'express';
import { 
  generateTitle, 
  generateSummary, 
  extractTasks, 
  processVoiceNote 
} from '../controllers/geminiController';
import { authenticateToken } from '../middleware/auth';
import { geminiLimiter } from '../middleware/rateLimiter';

const router = Router();

// All Gemini routes require authentication
router.use(authenticateToken);

// Apply Gemini-specific rate limiting
router.use(geminiLimiter);

// Individual endpoints
router.post('/generate-title', generateTitle);
router.post('/generate-summary', generateSummary);
router.post('/extract-tasks', extractTasks);

// Combined endpoint for processing entire voice note
router.post('/process-voice-note', processVoiceNote);

export default router;