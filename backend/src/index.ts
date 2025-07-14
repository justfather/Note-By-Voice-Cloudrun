import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { config } from './config/env';
import routes from './routes';
import { errorHandler, notFound } from './middleware/errorHandler';
import { apiLimiter } from './middleware/rateLimiter';

const app = express();

// Security middleware
app.use(helmet());

// CORS configuration
app.use(cors({
  origin: (origin, callback) => {
    // Allow requests with no origin (like mobile apps or curl requests)
    if (!origin) return callback(null, true);
    
    if (config.cors.allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true,
}));

// Body parsing middleware
app.use(express.json({ limit: '10mb' })); // Increased limit for base64 audio
app.use(express.urlencoded({ extended: true }));

// Logging middleware
if (config.server.nodeEnv !== 'test') {
  app.use(morgan(config.server.nodeEnv === 'production' ? 'combined' : 'dev'));
}

// General rate limiting
app.use('/api', apiLimiter);

// Routes
app.use('/', routes);

// Error handling
app.use(notFound);
app.use(errorHandler);

// Start server
const PORT = config.server.port;
app.listen(PORT, () => {
  console.log(`🚀 Server running on port ${PORT} in ${config.server.nodeEnv} mode`);
  
  if (!config.googleCloud.projectId) {
    console.warn('⚠️  GOOGLE_CLOUD_PROJECT_ID not set');
  }
  
  if (!config.googleCloud.credentials && config.isProduction) {
    console.warn('⚠️  GOOGLE_APPLICATION_CREDENTIALS not set');
  }
});

// Handle unhandled promise rejections
process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
  // In production, you might want to gracefully shutdown the server
  if (config.isProduction) {
    process.exit(1);
  }
});

export default app;