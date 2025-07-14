# Cloud Run Deployment Guide - ขั้นตอนละเอียด

## ขั้นที่ 1: เตรียม Google Cloud Project

### 1.1 สร้าง Project ใหม่ (หรือใช้ที่มีอยู่)
1. ไปที่ [Google Cloud Console](https://console.cloud.google.com/)
2. คลิก "Select a project" → "New Project"
3. ตั้งชื่อ project (เช่น "voice-task-backend")
4. คลิก "Create"

### 1.2 Enable Billing
1. ไปที่ "Billing" ใน Cloud Console
2. Link project กับ billing account
3. (จำเป็นสำหรับ Cloud Run)

### 1.3 Enable APIs
```bash
# ตั้งค่า project ID
gcloud config set project YOUR_PROJECT_ID

# Enable APIs ที่จำเป็น
gcloud services enable run.googleapis.com
gcloud services enable cloudbuild.googleapis.com
gcloud services enable secretmanager.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

## ขั้นที่ 2: สร้าง Secret Manager

### 2.1 เก็บ Gemini API Key
```bash
# สร้าง secret สำหรับ Gemini API key
echo -n "YOUR_GEMINI_API_KEY_HERE" | gcloud secrets create gemini-api-key --data-file=-
```

### 2.2 สร้าง Service Account
```bash
# สร้าง service account
gcloud iam service-accounts create voice-task-backend \
    --display-name="Voice Task Backend"

# ให้สิทธิ์เข้าถึง Secret Manager
gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

## ขั้นที่ 3: เตรียมโค้ด Backend

### 3.1 Update .env สำหรับ production
```bash
cd backend
cp .env.example .env
```

แก้ไขไฟล์ `.env`:
```env
NODE_ENV=production
PORT=8080
GOOGLE_CLOUD_PROJECT_ID=YOUR_PROJECT_ID
GEMINI_API_KEY_SECRET_NAME=gemini-api-key
JWT_SECRET=your-super-secret-jwt-key
ALLOWED_ORIGINS=https://your-app-domain.com
```

### 3.2 สร้าง Dockerfile (ถ้ายังไม่มี)
```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY . .
RUN npm run build

EXPOSE 8080

CMD ["node", "dist/index.js"]
```

## ขั้นที่ 4: Deploy ผ่าน Cloud Console (วิธีง่าย)

### 4.1 ไปที่ Cloud Run
1. ไปที่ [Cloud Run Console](https://console.cloud.google.com/run)
2. คลิก "Create Service"

### 4.2 ตั้งค่า Deployment
1. **Service name**: `voice-task-backend`
2. **Region**: เลือก region ที่ใกล้ที่สุด (เช่น `asia-southeast1`)
3. **Source**: เลือก "Deploy from source repository"

### 4.3 เชื่อมต่อ GitHub Repository
1. คลิก "Set up with Cloud Build"
2. เลือก "GitHub" → Authorize
3. เลือก repository ของคุณ
4. **Branch**: `main`
5. **Build Type**: `Dockerfile`
6. **Dockerfile path**: `backend/Dockerfile`

### 4.4 ตั้งค่า Container
1. **Container port**: `8080`
2. **Memory**: `512 MiB`
3. **CPU**: `1`
4. **Request timeout**: `300 seconds`
5. **Maximum requests per container**: `80`

### 4.5 ตั้งค่า Environment Variables
กดที่ "Variables & Secrets" → "Add variable":

```
NODE_ENV = production
PORT = 8080
GOOGLE_CLOUD_PROJECT_ID = YOUR_PROJECT_ID
GEMINI_API_KEY_SECRET_NAME = gemini-api-key
JWT_SECRET = your-super-secret-jwt-key-here
ALLOWED_ORIGINS = *
```

### 4.6 ตั้งค่า Service Account
1. กดที่ "Security" tab
2. **Service account**: เลือก `voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com`

### 4.7 ตั้งค่า Traffic
1. **Ingress**: Allow all traffic
2. **Authentication**: Allow unauthenticated invocations

### 4.8 Deploy
1. คลิก "Create"
2. รอ deployment เสร็จ (ประมาณ 2-5 นาที)

## ขั้นที่ 5: ทดสอบ Backend

### 5.1 หา Service URL
1. หลัง deploy เสร็จ จะได้ URL แบบนี้:
   ```
   https://voice-task-backend-xxxxx-as.a.run.app
   ```

### 5.2 ทดสอบ Health Check
```bash
curl https://voice-task-backend-xxxxx-as.a.run.app/health
```

ควรได้ response:
```json
{
  "status": "ok",
  "timestamp": "2024-01-15T10:30:00.000Z",
  "uptime": 123.45
}
```

### 5.3 ทดสอบ Registration
```bash
curl -X POST https://voice-task-backend-xxxxx-as.a.run.app/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "testpassword123"
  }'
```

## ขั้นที่ 6: Update Android App

### 6.1 แก้ไข FeatureFlags.kt
```kotlin
object FeatureFlags {
    const val USE_BACKEND_API = true
    const val BACKEND_API_URL = "https://voice-task-backend-xxxxx-as.a.run.app/"
}
```

### 6.2 Build และทดสอบ
```bash
./gradlew assembleDebug
```

## การแก้ไขปัญหาที่พบบ่อย

### ปัญหา 1: Service Account ไม่มีสิทธิ์
```bash
# ให้สิทธิ์ Secret Manager
gcloud secrets add-iam-policy-binding gemini-api-key \
    --member="serviceAccount:voice-task-backend@YOUR_PROJECT_ID.iam.gserviceaccount.com" \
    --role="roles/secretmanager.secretAccessor"
```

### ปัญหา 2: Port ไม่ถูกต้อง
- Cloud Run ต้องใช้ port 8080
- ตรวจสอบใน `src/config/env.ts`:
```typescript
port: process.env.PORT || 8080
```

### ปัญหา 3: Environment Variables ไม่ถูกต้อง
1. ไปที่ Cloud Run Console
2. คลิกที่ service name
3. กดที่ "Edit & Deploy New Revision"
4. ตรวจสอบ Variables & Secrets

### ปัญหา 4: Build ล้มเหลว
1. ตรวจสอบ Cloud Build logs
2. ไปที่ Cloud Build → History
3. คลิกที่ build ที่ล้มเหลว

## การ Monitor และ Debug

### 1. ดู Logs
```bash
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=voice-task-backend" --limit 50
```

### 2. Monitor ใน Console
1. ไปที่ Cloud Run Console
2. คลิกที่ service name
3. ดูที่ "Logs" และ "Metrics" tabs

### 3. Set up Alerts
1. ไปที่ "Monitoring" → "Alerting"
2. สร้าง alert policy สำหรับ:
   - Error rate > 5%
   - Response time > 2 seconds
   - CPU usage > 80%

## การ Update Backend

### วิธีที่ 1: ผ่าน Git Push
1. Push code ไปที่ GitHub
2. Cloud Build จะ auto-deploy

### วิธีที่ 2: Manual Deploy
1. ไปที่ Cloud Run Console
2. คลิก service name
3. คลิก "Edit & Deploy New Revision"
4. เปลี่ยนแปลงตามต้องการ
5. คลิก "Deploy"

## Security Best Practices

1. **ใช้ HTTPS เท่านั้น** (Cloud Run ให้ automatic)
2. **ตั้งค่า CORS** ให้เฉพาะ domain ที่ต้องการ
3. **Monitor logs** เพื่อหาการใช้งานผิดปกติ
4. **Rotate API keys** เป็นประจำ
5. **Set up proper IAM roles** ให้เฉพาะสิทธิ์ที่จำเป็น

## ค่าใช้จ่าย (ประมาณการ)

- **Cloud Run**: ฟรี 2 ล้าน requests/เดือน
- **Secret Manager**: ฟรี 10,000 operations/เดือน
- **Cloud Build**: ฟรี 120 build minutes/วัน
- **Artifact Registry**: ฟรี 0.5 GB storage