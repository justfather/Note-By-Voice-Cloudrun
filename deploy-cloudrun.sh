#!/bin/bash

# Deploy script for Cloud Run using gcloud CLI

echo "🚀 Deploying backend to Cloud Run..."

# Set project
gcloud config set project notebyvoice

# Deploy using buildpacks
gcloud run deploy note-by-voice-cloudrun \
    --source ./backend \
    --region asia-southeast1 \
    --platform managed \
    --allow-unauthenticated \
    --port 8080 \
    --memory 512Mi \
    --cpu 1 \
    --timeout 300 \
    --max-instances 100 \
    --min-instances 0 \
    --service-account "144159064911-compute@developer.gserviceaccount.com" \
    --set-env-vars "NODE_ENV=production" \
    --set-env-vars "PORT=8080" \
    --set-env-vars "GOOGLE_CLOUD_PROJECT_ID=notebyvoice" \
    --set-env-vars "GEMINI_API_KEY_SECRET_NAME=gemini-api-key" \
    --set-env-vars "JWT_SECRET=mySecretJWTKey2024" \
    --set-env-vars "ALLOWED_ORIGINS=*"

echo "✅ Deployment complete!"
echo ""
echo "To check status:"
echo "gcloud run services describe note-by-voice-cloudrun --region asia-southeast1"