import dotenv from 'dotenv';

dotenv.config();

export const config = {
  server: {
    port: process.env.PORT || 3000,
    nodeEnv: process.env.NODE_ENV || 'development',
  },
  jwt: {
    secret: process.env.JWT_SECRET || 'your-default-secret',
    expiresIn: process.env.JWT_EXPIRES_IN || '7d',
  },
  googleCloud: {
    projectId: process.env.GOOGLE_CLOUD_PROJECT_ID,
    credentials: process.env.GOOGLE_APPLICATION_CREDENTIALS,
  },
  secrets: {
    geminiApiKeyName: process.env.GEMINI_API_KEY_SECRET_NAME || 'gemini-api-key',
  },
  rateLimit: {
    windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS || '60000'),
    max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS || '100'),
  },
  cors: {
    allowedOrigins: (process.env.ALLOWED_ORIGINS || 'http://localhost:3000').split(','),
  },
  isProduction: process.env.NODE_ENV === 'production',
};