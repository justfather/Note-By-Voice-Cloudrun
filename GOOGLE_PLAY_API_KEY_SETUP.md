# Google Play API Key Configuration Guide

## Current Setup
Your Gemini API key is currently stored in `local.properties`:
```
GEMINI_API_KEY=your_api_key_here
```

This file is **NOT** included in version control (git ignored), which is correct for security.

## How the API Key Works in Your App

1. **Build Time**: The `app/build.gradle.kts` reads the API key from `local.properties`:
   ```kotlin
   val geminiApiKey = localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""
   buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
   ```

2. **Runtime**: Your app accesses the key via `BuildConfig.GEMINI_API_KEY`

## Google Play Deployment Options

### Option 1: Keep API Key in APK (Current Setup) ✅
- **Pros**: Simple, works immediately
- **Cons**: API key is embedded in the APK (though obfuscated by ProGuard)
- **Security**: Adequate for most apps, especially with API key restrictions

**No changes needed** - just build your release APK:
```bash
./gradlew assembleRelease
```

### Option 2: Use a Backend Proxy (More Secure) 🔒
If you want enhanced security:
1. Set up a backend server (Firebase Functions, AWS Lambda, etc.)
2. Store the API key on your server
3. Your app calls your server, which then calls Gemini API
4. Remove the API key from the app entirely

### Option 3: Use Environment Variables in CI/CD
For automated builds:
1. Store `GEMINI_API_KEY` as a secret in your CI/CD platform
2. The build script will use: `System.getenv("GEMINI_API_KEY")`
3. This keeps the key out of your repository

## Recommended API Key Restrictions

**IMPORTANT**: Restrict your API key in Google Cloud Console:
1. Go to [Google Cloud Console](https://console.cloud.google.com/apis/credentials)
2. Find your Gemini API key
3. Click "Edit API key"
4. Add restrictions:
   - **Application restrictions**: Android apps
   - **Package name**: `com.ppai.voicetotask`
   - **SHA-1 certificate fingerprint**: Add both debug and release fingerprints

### Get your SHA-1 fingerprints:
```bash
# Debug fingerprint
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

# Release fingerprint (after creating your release keystore)
keytool -list -v -keystore release-keystore.jks -alias your_key_alias
```

## Pre-Launch Checklist
- [x] API key in local.properties
- [x] ProGuard enabled for release builds
- [ ] API key restricted in Google Cloud Console
- [ ] Release keystore created
- [ ] Both SHA-1 fingerprints added to API restrictions
- [ ] Test release build with restricted API key

## Building for Google Play
```bash
# Clean build
./gradlew clean

# Create release APK
./gradlew assembleRelease

# Or create App Bundle (recommended for Play Store)
./gradlew bundleRelease
```

Your signed APK/AAB will be in:
- APK: `app/build/outputs/apk/release/`
- AAB: `app/build/outputs/bundle/release/`