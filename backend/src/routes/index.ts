import { Router } from 'express';
import authRoutes from './auth';
import geminiRoutes from './gemini';

const router = Router();

// Health check endpoint
router.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    uptime: process.uptime(),
  });
});

// Mount route modules
router.use('/auth', authRoutes);
router.use('/api', geminiRoutes);

export default router;