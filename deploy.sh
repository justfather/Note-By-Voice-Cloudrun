#!/bin/bash

# Deployment script for notebyvoice project
# Project ID: notebyvoice
# Service Account: 144159064911-compute@developer.gserviceaccount.com

echo "🚀 Starting deployment for notebyvoice project..."

# Set project ID
echo "Setting project ID..."
gcloud config set project notebyvoice

# Enable required APIs
echo "Enabling required APIs..."
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable artifactregistry.googleapis.com

# Check if Gemini API key secret exists
echo "Checking Gemini API key secret..."
if gcloud secrets describe gemini-api-key --quiet 2>/dev/null; then
    echo "✅ Secret 'gemini-api-key' already exists"
else
    echo "❌ Secret 'gemini-api-key' not found"
    echo "Please create it manually:"
    echo "echo -n 'YOUR_GEMINI_API_KEY' | gcloud secrets create gemini-api-key --data-file=-"
    echo ""
    read -p "Press Enter after creating the secret..."
fi

# Grant service account access to secret
echo "Granting service account access to secret..."
gcloud secrets add-iam-policy-binding gemini-api-key \
    --member="serviceAccount:144159064911-compute@developer.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"

echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Go to Cloud Run Console: https://console.cloud.google.com/run"
echo "2. Create Service → Deploy from source repository"
echo "3. Connect to GitHub repository: justfather/Note-By-Voice-Cloudrun"
echo "4. Use these settings:"
echo "   - Service name: voice-task-backend"
echo "   - Region: asia-southeast1"
echo "   - Branch: main"
echo "   - Dockerfile path: backend/Dockerfile"
echo "   - Service account: 144159064911-compute@developer.gserviceaccount.com"
echo ""
echo "Environment variables to set:"
echo "NODE_ENV=production"
echo "PORT=8080"
echo "GOOGLE_CLOUD_PROJECT_ID=notebyvoice"
echo "GEMINI_API_KEY_SECRET_NAME=gemini-api-key"
echo "JWT_SECRET=$(openssl rand -base64 32)"
echo "ALLOWED_ORIGINS=*"
echo ""
echo "📚 For detailed instructions, see PROJECT_CONFIG.md"