package com.ppai.voicetotask.di

import javax.inject.Singleton

@Singleton
object FeatureFlags {
    // Set to true to use backend API, false to use direct Gemini API
    const val USE_BACKEND_API = false
    
    // Backend API URL - Update this with your actual backend URL
    const val BACKEND_API_URL = "https://your-backend-url.com/"
    
    // For local development, you might use:
    // const val BACKEND_API_URL = "http://10.0.2.2:3000/" // Android emulator
    // const val BACKEND_API_URL = "http://localhost:3000/" // Physical device on same network
}