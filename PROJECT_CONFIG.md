# Project Configuration - notebyvoice

## Project Details

- **Project ID**: `notebyvoice`
- **Service Account**: `144159064911-compute@developer.gserviceaccount.com`
- **Repository**: https://github.com/justfather/Note-By-Voice-Cloudrun.git

## Quick Setup Commands

### 1. Set Project ID
```bash
gcloud config set project notebyvoice
```

### 2. Enable Required APIs
```bash
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### 3. Create Gemini API Key Secret
```bash
# Replace YOUR_GEMINI_API_KEY with your actual key
echo -n "YOUR_GEMINI_API_KEY" | gcloud secrets create gemini-api-key --data-file=-
```

### 4. Grant Service Account Access to Secret
```bash
gcloud secrets add-iam-policy-binding gemini-api-key \
    --member="serviceAccount:144159064911-compute@developer.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

## Cloud Run Deployment Configuration

### Environment Variables for Cloud Run
```
NODE_ENV=production
PORT=8080
GOOGLE_CLOUD_PROJECT_ID=notebyvoice
GEMINI_API_KEY_SECRET_NAME=gemini-api-key
JWT_SECRET=your-super-secret-jwt-key-here
ALLOWED_ORIGINS=*
```

### Service Account Configuration
- **Service Account**: `144159064911-compute@developer.gserviceaccount.com`
- **Required Roles**: 
  - `roles/secretmanager.secretAccessor`

## Cloud Run Service Configuration

### Recommended Settings
- **Service Name**: `voice-task-backend`
- **Region**: `asia-southeast1` (Singapore)
- **CPU**: 1
- **Memory**: 512Mi
- **Min Instances**: 0
- **Max Instances**: 100
- **Request Timeout**: 300 seconds
- **Concurrency**: 80

### Build Configuration
- **Source**: GitHub repository
- **Repository**: `justfather/Note-By-Voice-Cloudrun`
- **Branch**: `main`
- **Build Type**: `Dockerfile`
- **Dockerfile Path**: `backend/Dockerfile`

## Android App Configuration

### Update FeatureFlags.kt
```kotlin
object FeatureFlags {
    const val USE_BACKEND_API = true
    const val BACKEND_API_URL = "https://voice-task-backend-xxxxx-as.a.run.app/"
    // Replace xxxxx with your actual Cloud Run URL
}
```

## Security Checklist

- [ ] Gemini API key stored in Secret Manager
- [ ] Service account has minimal required permissions
- [ ] JWT secret is secure and random
- [ ] CORS configured for your domain
- [ ] HTTPS enabled (automatic with Cloud Run)
- [ ] Rate limiting enabled

## Testing Commands

### Health Check
```bash
curl https://voice-task-backend-xxxxx-as.a.run.app/health
```

### Test Registration
```bash
curl -X POST https://voice-task-backend-xxxxx-as.a.run.app/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpassword123"
  }'
```

### Test Voice Note Processing
```bash
# First get token from registration/login, then:
curl -X POST https://voice-task-backend-xxxxx-as.a.run.app/api/process-voice-note \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "transcript": "I need to finish the report by Friday and call John about the meeting",
    "currentDate": "2024-01-15",
    "language": "English"
  }'
```

## Monitoring

### View Logs
```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=voice-task-backend" --limit 50
```

### Monitor Metrics
- Go to Cloud Console → Cloud Run → voice-task-backend
- Check "Metrics" tab for requests, latency, errors

## Cost Optimization

### Expected Usage (Free Tier)
- **Cloud Run**: 2M requests/month free
- **Secret Manager**: 10K operations/month free
- **Cloud Build**: 120 build minutes/day free

### Estimated Monthly Cost (Beyond Free Tier)
- **Cloud Run**: ~$0.10 per 100K requests
- **Secret Manager**: $0.06 per 10K operations
- **Gemini API**: Variable based on usage

## Support

If you encounter issues:
1. Check Cloud Run logs
2. Verify service account permissions
3. Test Secret Manager access
4. Review environment variables
5. Check CORS configuration