# Note-By-Voice-Cloudrun

Voice to Task Android App with Secure Backend API

## Features

- Android app for voice-to-task conversion
- Secure backend API with Google Cloud integration
- Google Secret Manager for API key storage
- JWT authentication system
- Rate limiting and security middleware
- Docker containerization
- Google Cloud Run deployment ready

## Quick Start

1. **Backend Setup:**
   ```bash
   cd backend
   npm install
   npm run dev
   ```

2. **Android App:**
   - Open in Android Studio
   - Update FeatureFlags.kt with your backend URL
   - Build and run

3. **Deploy to Cloud Run:**
   - Follow CLOUD_RUN_DEPLOYMENT.md guide

## Documentation

- [Deployment Guide](./DEPLOYMENT_GUIDE.md)
- [Cloud Run Deployment](./CLOUD_RUN_DEPLOYMENT.md)
- [Backend README](./backend/README.md)

## Architecture

```
Android App → Backend API (Cloud Run) → Google Secret Manager → Gemini AI API
                    ↓
              Authentication
                    ↓
              Rate Limiting
```

Generated with secure API management system for production use.