import { Router } from 'express';
import { register, login, refreshToken } from '../controllers/authController';
import { authLimiter } from '../middleware/rateLimiter';

const router = Router();

// Apply rate limiting to auth routes
router.use(authLimiter);

// Auth routes
router.post('/register', register);
router.post('/login', login);
router.post('/refresh', refreshToken);

export default router;