# Voice to Task Backend API

This backend server provides a secure proxy for the Gemini AI API, implementing authentication, rate limiting, and Google Secret Manager integration.

## Features

- 🔐 Secure API key management with Google Secret Manager
- 🔑 JWT-based authentication
- ⚡ Rate limiting per user and endpoint
- 🛡️ Security headers with Helmet
- 📝 Request logging with Morgan
- 🚦 CORS configuration
- 🔄 Error handling and validation

## Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Environment Configuration

Copy `.env.example` to `.env` and configure:

```bash
cp .env.example .env
```

### 3. Google Cloud Setup

1. Create a Google Cloud Project
2. Enable Secret Manager API
3. Create a service account with "Secret Manager Secret Accessor" role
4. Download the service account key JSON file
5. Create a secret in Secret Manager:

```bash
echo -n "your-gemini-api-key" | gcloud secrets create gemini-api-key --data-file=-
```

### 4. Development

```bash
npm run dev
```

### 5. Production Build

```bash
npm run build
npm start
```

## API Endpoints

### Authentication

- `POST /auth/register` - Register new user
- `POST /auth/login` - User login
- `POST /auth/refresh` - Refresh token

### Gemini AI Operations (Requires Authentication)

- `POST /api/generate-title` - Generate title from transcript
- `POST /api/generate-summary` - Generate summary from transcript
- `POST /api/extract-tasks` - Extract tasks from transcript
- `POST /api/process-voice-note` - Process entire voice note (combined operation)

### Health Check

- `GET /health` - Server health status

## Request Examples

### Register User
```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "securepassword"}'
```

### Process Voice Note
```bash
curl -X POST http://localhost:3000/api/process-voice-note \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "transcript": "I need to finish the report by Friday and call John about the meeting",
    "currentDate": "2024-01-15",
    "language": "English"
  }'
```

## Security Features

1. **API Key Protection**: Gemini API key stored in Google Secret Manager
2. **Authentication**: JWT tokens with configurable expiration
3. **Rate Limiting**: 
   - General API: 100 requests/minute
   - Auth endpoints: 5 requests/15 minutes
   - Gemini endpoints: 30 requests/minute
4. **CORS**: Configurable allowed origins
5. **Input Validation**: Request validation and sanitization
6. **Error Handling**: Graceful error responses without exposing internals

## Deployment

### Google Cloud Run

1. Build Docker image:
```bash
docker build -t gcr.io/YOUR_PROJECT_ID/voice-task-backend .
docker push gcr.io/YOUR_PROJECT_ID/voice-task-backend
```

2. Deploy:
```bash
gcloud run deploy voice-task-backend \
  --image gcr.io/YOUR_PROJECT_ID/voice-task-backend \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated
```

### Environment Variables for Production

- Set all environment variables in Cloud Run configuration
- Mount service account key as secret
- Configure Secret Manager permissions

## Monitoring

- Health endpoint for uptime monitoring
- Morgan logs for request tracking
- Error logging with stack traces (development only)

## Future Enhancements

- Database integration (PostgreSQL/MongoDB)
- Redis for session management
- WebSocket support for real-time updates
- Batch processing endpoints
- Usage analytics and quotas