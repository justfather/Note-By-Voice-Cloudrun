# Note By Voice Android App - Development Guide

## Project Overview
Note By Voice is an Android app that converts voice recordings into structured notes and tasks using Google's Gemini AI. The app features task management, note-taking, and calendar integration.

## Important Configuration

### API Keys
- The Gemini API key is stored in `local.properties` (not in version control)
- Format: `GEMINI_API_KEY=your_api_key_here`
- The app reads this key via BuildConfig

### Package Name
- Package: `com.ppai.voicetotask`
- App ID: `com.ppai.voicetotask`

### Signing Configuration
- Release signing requires `keystore.properties` file (not in version control)
- Generate keystore: `keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias your_key_alias`

## Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing configuration)
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint
./gradlew lint

# Clean build
./gradlew clean
```

## Architecture
- **Presentation Layer**: Jetpack Compose UI with ViewModels
- **Domain Layer**: Use cases and business logic
- **Data Layer**: Repository pattern with Room database and Retrofit API calls
- **DI**: Hilt for dependency injection

## Key Features
1. Voice recording and transcription
2. AI-powered task extraction from voice notes
3. Task management with priority levels
4. Calendar integration
5. Note organization

## Play Store Requirements Checklist
- ✅ API key secured in local.properties
- ✅ Package name updated from com.example
- ✅ Signing configuration added
- ✅ ProGuard/R8 enabled for release builds
- ✅ Version management implemented
- ✅ Privacy policy URL added (https://sites.google.com/view/notebyvoice/home)
- ✅ App name changed to "Note By Voice"
- ⬜ Permission rationale dialogs needed
- ⬜ App description and metadata needed

## Pending Improvements
1. Error handling with user feedback
2. Audio file cleanup mechanism
3. Privacy policy implementation
4. Permission request dialogs
5. Paywall/monetization (Google Play Billing)

## Testing on Device
1. Enable Developer Options on Android device
2. Enable USB Debugging
3. Connect device and run: `./gradlew installDebug`

## Important Files
- `app/build.gradle.kts` - Main build configuration
- `local.properties` - API keys and local config
- `keystore.properties` - Signing configuration
- `proguard-rules.pro` - ProGuard configuration
- `VERSION.md` - Version tracking