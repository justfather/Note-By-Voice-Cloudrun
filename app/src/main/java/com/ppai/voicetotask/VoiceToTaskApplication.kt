package com.ppai.voicetotask

import android.app.Application
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.ppai.voicetotask.data.ads.AdConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VoiceToTaskApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob
        if (AdConfig.USE_TEST_ADS) {
            Log.d("VoiceToTaskApp", "Initializing AdMob with test ads")
        } else {
            Log.d("VoiceToTaskApp", "Initializing AdMob with production ads")
        }
        
        MobileAds.initialize(this) { initializationStatus ->
            Log.d("VoiceToTaskApp", "AdMob initialized successfully")
            initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                Log.d("VoiceToTaskApp", "Adapter: $adapter, Status: ${status.initializationState}")
            }
        }
    }
}