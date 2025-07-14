# Voice to Task - Secure API Deployment Guide

This guide explains how to deploy the secure backend API system using Google Cloud services.

## Architecture Overview

```
Android App → Backend API (Cloud Run) → Google Secret Manager → Gemini AI API
                    ↓
              Authentication
                    ↓
              Rate Limiting
```

## Prerequisites

1. Google Cloud Project with billing enabled
2. Google Cloud CLI installed
3. Docker installed (for local testing)
4. Gemini API key from Google AI Studio

## Step 1: Google Cloud Setup

### 1.1 Enable Required APIs

```bash
gcloud config set project YOUR_PROJECT_ID

# Enable required services
gcloud services enable secretmanager.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### 1.2 Create Service Account

```bash
# Create service account for Cloud Run
gcloud iam service-accounts create voice-task-backend \
    --display-name="Voice Task Backend Service Account"

# Grant Secret Manager access
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

## Step 2: Store Gemini API Key in Secret Manager

```bash
# Create secret
echo -n "YOUR_GEMINI_API_KEY" | gcloud secrets create gemini-api-key \
    --data-file=- \
    --replication-policy="automatic"

# Grant service account access to the secret
gcloud secrets add-iam-policy-binding gemini-api-key \
    --member="serviceAccount:voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

## Step 3: Deploy Backend to Cloud Run

### 3.1 Build and Push Docker Image

```bash
cd backend

# Configure Docker for Google Artifact Registry
gcloud auth configure-docker YOUR_REGION-docker.pkg.dev

# Build and push image
docker build -t YOUR_REGION-docker.pkg.dev/YOUR_PROJECT_ID/voice-task/backend:latest .
docker push YOUR_REGION-docker.pkg.dev/YOUR_PROJECT_ID/voice-task/backend:latest
```

### 3.2 Deploy to Cloud Run

```bash
gcloud run deploy voice-task-backend \
    --image YOUR_REGION-docker.pkg.dev/YOUR_PROJECT_ID/voice-task/backend:latest \
    --platform managed \
    --region YOUR_REGION \
    --service-account voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com \
    --set-env-vars="GOOGLE_CLOUD_PROJECT_ID=YOUR_PROJECT_ID" \
    --set-env-vars="NODE_ENV=production" \
    --set-env-vars="JWT_SECRET=$(openssl rand -base64 32)" \
    --set-env-vars="GEMINI_API_KEY_SECRET_NAME=gemini-api-key" \
    --allow-unauthenticated \
    --min-instances=0 \
    --max-instances=100 \
    --memory=512Mi \
    --cpu=1
```

## Step 4: Update Android App Configuration

### 4.1 Update Feature Flags

Edit `app/src/main/java/com/ppai/voicetotask/di/FeatureFlags.kt`:

```kotlin
object FeatureFlags {
    // Enable backend API
    const val USE_BACKEND_API = true
    
    // Your Cloud Run service URL
    const val BACKEND_API_URL = "https://voice-task-backend-xxxxx-uc.a.run.app/"
}
```

### 4.2 Build and Deploy Android App

```bash
# Build release APK
./gradlew assembleRelease

# Or build AAB for Play Store
./gradlew bundleRelease
```

## Step 5: Monitoring and Maintenance

### 5.1 View Logs

```bash
# View Cloud Run logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=voice-task-backend" --limit 50

# Stream logs
gcloud alpha logging tail "resource.type=cloud_run_revision AND resource.labels.service_name=voice-task-backend"
```

### 5.2 Update API Key

```bash
# Update secret version
echo -n "NEW_GEMINI_API_KEY" | gcloud secrets versions add gemini-api-key --data-file=-
```

### 5.3 Monitor Usage

1. Go to Cloud Console → Cloud Run → voice-task-backend
2. Check metrics: requests, latency, errors
3. Set up alerts for high error rates or usage

## Security Best Practices

1. **API Key Rotation**: Rotate Gemini API key monthly
2. **Rate Limiting**: Adjust limits based on usage patterns
3. **Authentication**: Implement stronger auth (OAuth2, Firebase Auth)
4. **HTTPS**: Always use HTTPS (Cloud Run provides this)
5. **Monitoring**: Set up alerts for suspicious activity

## Cost Optimization

1. **Cloud Run**: 
   - Set min instances to 0 for scale-to-zero
   - Use appropriate CPU/memory limits
   
2. **Secret Manager**:
   - Free tier: 10,000 access operations/month
   - $0.06 per 10,000 operations after

3. **Gemini API**:
   - Monitor usage in Google AI Studio
   - Implement caching for repeated requests

## Troubleshooting

### Backend Not Starting
```bash
# Check deployment status
gcloud run services describe voice-task-backend --region YOUR_REGION

# Check service account permissions
gcloud projects get-iam-policy YOUR_PROJECT_ID
```

### Authentication Issues
1. Verify JWT_SECRET is set correctly
2. Check token expiration in Android app
3. Verify CORS settings match your app

### Secret Manager Access Denied
```bash
# Grant access to service account
gcloud secrets add-iam-policy-binding gemini-api-key \
    --member="serviceAccount:voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

## Local Development

### Backend
```bash
cd backend
cp .env.example .env
# Edit .env with your settings
npm install
npm run dev
```

### Android App (with local backend)
1. Update `FeatureFlags.kt`:
   ```kotlin
   const val BACKEND_API_URL = "http://10.0.2.2:3000/" // For emulator
   ```
2. Run the app in Android Studio

## Production Checklist

- [ ] Enable Cloud Run authentication for production
- [ ] Set up custom domain with SSL
- [ ] Configure backup and disaster recovery
- [ ] Implement proper user management (database)
- [ ] Set up monitoring and alerting
- [ ] Document API endpoints for team
- [ ] Create staging environment
- [ ] Implement CI/CD pipeline