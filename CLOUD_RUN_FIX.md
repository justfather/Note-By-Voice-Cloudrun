# Cloud Run Build Failed - วิธีแก้ไข

## 🔍 ตรวจสอบ Build Error

### 1. ดู Error Log
1. ไปที่ [Cloud Build Console](https://console.cloud.google.com/cloud-build/builds)
2. คลิกที่ build ที่ล้มเหลว (สีแดง)
3. ดู error message ในส่วน "Build log"

### 2. Error ที่เจอบ่อย

## ❌ Error: "No such file or directory: backend/Dockerfile"

**สาเหตุ**: Cloud Run หา Dockerfile ไม่เจอ

**วิธีแก้**:
1. ไปที่ Cloud Run Console
2. คลิก "Edit & Deploy New Revision"
3. ไปที่ "Container" tab
4. **Source**: เลือก "Container image URL" แทน
5. ใส่: `gcr.io/buildpacks/builder:v1`
6. หรือใช้ **Buildpacks** แทน Dockerfile

## ❌ Error: "Permission denied"

**สาเหตุ**: Service account ไม่มีสิทธิ์

**วิธีแก้**:
```bash
# Grant permissions to service account
gcloud projects add-iam-policy-binding notebyvoice \
    --member="serviceAccount:144159064911-compute@developer.gserviceaccount.com" \
    --role="roles/cloudbuild.builds.builder"

gcloud projects add-iam-policy-binding notebyvoice \
    --member="serviceAccount:144159064911-compute@developer.gserviceaccount.com" \
    --role="roles/artifactregistry.writer"
```

## ❌ Error: "Node modules not found"

**สาเหตุ**: Build process ไม่เจอ package.json

**วิธีแก้**: ใช้ **Google Cloud Buildpacks**

## 🔧 วิธีแก้ที่ง่ายที่สุด: ใช้ Buildpacks

### ขั้นตอนที่ 1: ลบ Service เก่า
1. ไปที่ [Cloud Run Console](https://console.cloud.google.com/run)
2. คลิกที่ service ที่ล้มเหลว
3. คลิก "Delete" ถ้าจำเป็น

### ขั้นตอนที่ 2: สร้าง Service ใหม่
1. คลิก "Create Service"
2. เลือก "Continuously deploy from a repository"
3. คลิก "Set up with Cloud Build"

### ขั้นตอนที่ 3: Repository Settings
1. **Repository provider**: GitHub
2. **Repository**: `justfather/Note-By-Voice-Cloudrun`
3. **Branch**: `main`
4. **Build type**: เลือก **"Go, Node.js, Python, Java, .NET Core, PHP, or Ruby (via Google Cloud Buildpacks)"**
5. **Source location**: `/backend` (สำคัญมาก!)

### ขั้นตอนที่ 4: Service Settings
1. **Service name**: `voice-task-backend`
2. **Region**: `asia-southeast1`
3. **Authentication**: Allow unauthenticated
4. **Container port**: `8080`
5. **Memory**: `512 MiB`
6. **CPU**: `1`

### ขั้นตอนที่ 5: Environment Variables
คลิกที่ "Container, Variables & Secrets, Connections, Security" และเพิ่ม:

```
NODE_ENV=production
PORT=8080
GOOGLE_CLOUD_PROJECT_ID=notebyvoice
GEMINI_API_KEY_SECRET_NAME=gemini-api-key
JWT_SECRET=mySecretJWTKey2024
ALLOWED_ORIGINS=*
```

### ขั้นตอนที่ 6: Service Account
1. ไปที่ "Security" tab
2. **Service account**: `144159064911-compute@developer.gserviceaccount.com`

### ขั้นตอนที่ 7: Deploy
1. คลิก "Create"
2. รอ build (อาจใช้เวลา 3-5 นาที)

## 🔧 แก้ไข package.json (ถ้ายังไม่ได้)

ปัญหาอาจเกิดจาก backend/package.json ไม่มี start script ที่ถูกต้อง

### ตรวจสอบ package.json:
```json
{
  "scripts": {
    "start": "node dist/index.js",
    "build": "tsc",
    "dev": "nodemon src/index.ts"
  }
}
```

### ถ้าไม่มี ให้แก้ไข:
```json
{
  "scripts": {
    "start": "npm run build && node dist/index.js",
    "build": "tsc",
    "dev": "nodemon src/index.ts"
  }
}
```

## 🔧 สร้าง .gcloudignore

สร้างไฟล์ `backend/.gcloudignore`:
```
node_modules
.env
.git
dist
*.log
```

## 🔧 แก้ไข Dockerfile (ถ้าจำเป็น)

อัพเดท `backend/Dockerfile`:
```dockerfile
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm ci --only=production

# Copy source code
COPY . .

# Build TypeScript
RUN npm run build

# Expose port
EXPOSE 8080

# Start application
CMD ["npm", "start"]
```

## 🔧 ตรวจสอบ Environment Variables

ใน Cloud Run Console:
1. ไปที่ service ที่สร้าง
2. คลิก "Edit & Deploy New Revision"
3. ไปที่ "Variables & Secrets"
4. ตรวจสอบว่ามี environment variables ครบ

## 🔧 Test หลัง Deploy สำเร็จ

```bash
# ใช้ URL ที่ได้จาก Cloud Run
curl https://voice-task-backend-xxxxx-as.a.run.app/health

# ควรได้ response:
# {"status":"ok","timestamp":"...","uptime":123}
```

## 🚨 ถ้ายังไม่ได้ให้ลองนี้:

### วิธีแก้ด่วน: Deploy แบบ Simple
1. ไปที่ Cloud Run Console
2. คลิก "Create Service"
3. เลือก "Deploy one revision from an existing container image"
4. ใส่: `gcr.io/cloudrun/hello`
5. ทดสอบก่อนว่า service รันได้
6. แล้วค่อยเปลี่ยนเป็น repository ทีหลัง

## 💡 Tips

1. **ใช้ Buildpacks แทน Dockerfile** (ง่ายกว่า)
2. **ตั้ง Source location เป็น `/backend`** (สำคัญมาก!)
3. **ใช้ PORT=8080** (Cloud Run ต้องการ)
4. **ตรวจสอบ Service Account permissions**
5. **ใช้ asia-southeast1 region** (ใกล้ที่สุด)

ให้บอกว่า error message ที่เจอคืออะไร แล้วผมจะช่วยแก้ให้เฉพาะเจาะจง!