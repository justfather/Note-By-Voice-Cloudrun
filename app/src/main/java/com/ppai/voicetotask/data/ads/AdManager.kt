package com.ppai.voicetotask.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.ppai.voicetotask.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AdManager"
        private const val TEST_DEVICE_ID = "YOUR_TEST_DEVICE_ID" // Add your test device ID
    }
    
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private val _isInterstitialAdReady = MutableStateFlow(false)
    val isInterstitialAdReady: StateFlow<Boolean> = _isInterstitialAdReady.asStateFlow()
    
    private val _isRewardedAdReady = MutableStateFlow(false)
    val isRewardedAdReady: StateFlow<Boolean> = _isRewardedAdReady.asStateFlow()
    
    init {
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob SDK initialized")
            initializationStatus.adapterStatusMap.forEach { (className, status) ->
                Log.d(TAG, "Adapter: $className, Status: ${status.initializationState}")
            }
            
            // Configure test devices
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR, TEST_DEVICE_ID))
                .build()
            MobileAds.setRequestConfiguration(configuration)
            
            // Preload ads
            loadInterstitialAd()
            loadRewardedAd()
        }
    }
    
    fun loadInterstitialAd() {
        val adUnitId = if (AdConfig.USE_TEST_ADS) {
            AdConfig.TEST_INTERSTITIAL_ID
        } else {
            BuildConfig.ADMOB_INTERSTITIAL_ID
        }
        
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Interstitial Ad ID not configured")
            return
        }
        
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded")
                    interstitialAd = ad
                    _isInterstitialAdReady.value = true
                    setupInterstitialCallbacks(ad)
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${error.message}")
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                }
            }
        )
    }
    
    private fun setupInterstitialCallbacks(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                _isInterstitialAdReady.value = false
                // Preload next ad
                loadInterstitialAd()
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Failed to show interstitial ad: ${error.message}")
                interstitialAd = null
                _isInterstitialAdReady.value = false
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
            }
        }
    }
    
    fun showInterstitialAd(
        activity: Activity,
        onAdDismissed: () -> Unit,
        onAdFailed: () -> Unit = onAdDismissed
    ) {
        // Check offline mode first
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline mode - skipping ad")
            onAdDismissed()
            return
        }
        
        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdDismissed()
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                    loadInterstitialAd() // Preload next ad
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Ad failed to show: ${error.message}")
                    onAdFailed()
                    interstitialAd = null
                    _isInterstitialAdReady.value = false
                    loadInterstitialAd()
                }
            }
            ad.show(activity)
        } ?: run {
            Log.w(TAG, "No interstitial ad available")
            onAdDismissed()
        }
    }
    
    fun loadRewardedAd() {
        val adUnitId = if (AdConfig.USE_TEST_ADS) {
            AdConfig.TEST_REWARDED_ID
        } else {
            BuildConfig.ADMOB_REWARDED_ID
        }
        
        if (adUnitId.isEmpty()) {
            Log.w(TAG, "Rewarded Ad ID not configured")
            return
        }
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded")
                    rewardedAd = ad
                    _isRewardedAdReady.value = true
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Failed to load rewarded ad: ${error.message}")
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                }
            }
        )
    }
    
    fun showRewardedAd(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit,
        onFailed: () -> Unit = onDismissed
    ) {
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Offline mode - skipping rewarded ad")
            onDismissed()
            return
        }
        
        rewardedAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onDismissed()
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd() // Preload next ad
                }
                
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                    onFailed()
                    rewardedAd = null
                    _isRewardedAdReady.value = false
                    loadRewardedAd()
                }
            }
            
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewarded()
            }
        } ?: run {
            Log.w(TAG, "No rewarded ad available")
            onDismissed()
        }
    }
    
    fun createBannerAdView(context: Context): AdView {
        val adUnitId = if (AdConfig.USE_TEST_ADS) {
            AdConfig.TEST_BANNER_ID
        } else {
            BuildConfig.ADMOB_BANNER_ID
        }
        
        return AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}