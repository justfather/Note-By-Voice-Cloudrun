package com.ppai.voicetotask.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.ppai.voicetotask.BuildConfig
import com.ppai.voicetotask.domain.model.Subscription
import com.ppai.voicetotask.domain.model.SubscriptionTier
import com.ppai.voicetotask.domain.model.SubscriptionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener {
    
    companion object {
        private const val TAG = "BillingManager"
        const val MONTHLY_SUB_ID = "premium_monthly"
        const val YEARLY_SUB_ID = "premium_yearly"
    }
    
    private var billingClient: BillingClient? = null
    
    private val _billingState = MutableStateFlow(BillingState())
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()
    
    private val _currentSubscription = MutableStateFlow<Subscription?>(null)
    val currentSubscription: StateFlow<Subscription?> = _currentSubscription.asStateFlow()
    
    init {
        initializeBillingClient()
    }
    
    private fun initializeBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        
        connectToBillingService()
    }
    
    private fun connectToBillingService() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing service connected")
                    _billingState.value = _billingState.value.copy(isConnected = true)
                    
                    // Query purchases to check subscription status
                    queryPurchases()
                    
                    // Query available products
                    queryProducts()
                } else {
                    Log.e(TAG, "Failed to connect to billing service: ${billingResult.debugMessage}")
                    _billingState.value = _billingState.value.copy(
                        isConnected = false,
                        error = BillingError.ConnectionError(billingResult.debugMessage)
                    )
                }
            }
            
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
                _billingState.value = _billingState.value.copy(isConnected = false)
                // Try to reconnect
                connectToBillingService()
            }
        })
    }
    
    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_SUB_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_SUB_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Products loaded: ${productDetailsList.size}")
                _billingState.value = _billingState.value.copy(
                    availableProducts = productDetailsList,
                    error = null
                )
            } else {
                Log.e(TAG, "Failed to load products: ${billingResult.debugMessage}")
                _billingState.value = _billingState.value.copy(
                    error = BillingError.ProductsNotAvailable
                )
            }
        }
    }
    
    private fun queryPurchases() {
        billingClient?.let { client ->
            if (!client.isReady) {
                Log.w(TAG, "Billing client not ready for purchase query")
                return
            }
            
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases)
                } else {
                    Log.e(TAG, "Failed to query purchases: ${billingResult.debugMessage}")
                }
            }
        }
    }
    
    private fun processPurchases(purchases: List<Purchase>) {
        val activePurchase = purchases.firstOrNull { 
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
            it.isAcknowledged
        }
        
        if (activePurchase != null) {
            // Check if user is test user
            val isTestUser = BuildConfig.TEST_USER_EMAIL.isNotEmpty() && 
                             activePurchase.accountIdentifiers?.obfuscatedAccountId == BuildConfig.TEST_USER_EMAIL
            
            _currentSubscription.value = Subscription(
                userId = activePurchase.accountIdentifiers?.obfuscatedAccountId ?: "",
                tier = SubscriptionTier.PREMIUM,
                purchaseToken = activePurchase.purchaseToken,
                startDate = Date(activePurchase.purchaseTime),
                isTestUser = isTestUser
            )
        } else {
            // No active subscription - set to FREE tier
            _currentSubscription.value = Subscription.DEFAULT_FREE
        }
        
        // Acknowledge purchases if needed
        purchases.filter { 
            it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged 
        }.forEach { purchase ->
            acknowledgePurchase(purchase)
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged")
            } else {
                Log.e(TAG, "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }
    
    fun launchBillingFlow(activity: Activity, subscriptionType: SubscriptionType) {
        val productDetails = _billingState.value.availableProducts.find { 
            it.productId == subscriptionType.skuId 
        }
        
        if (productDetails == null) {
            Log.e(TAG, "Product not found: ${subscriptionType.skuId}")
            _billingState.value = _billingState.value.copy(
                error = BillingError.ProductNotAvailable
            )
            return
        }
        
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            Log.e(TAG, "No offer available for product: ${subscriptionType.skuId}")
            return
        }
        
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .setOfferToken(offerToken)
            .build()
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Failed to launch billing flow: ${billingResult?.debugMessage}")
            _billingState.value = _billingState.value.copy(
                error = BillingError.Unknown(billingResult?.debugMessage ?: "Unknown error")
            )
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { processPurchases(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "Purchase cancelled by user")
                _billingState.value = _billingState.value.copy(
                    error = BillingError.PurchaseCancelled
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item already owned")
                queryPurchases() // Refresh purchases
            }
            else -> {
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
                _billingState.value = _billingState.value.copy(
                    error = BillingError.Unknown(billingResult.debugMessage)
                )
            }
        }
    }
    
    suspend fun restorePurchases(): Boolean = suspendCancellableCoroutine { continuation ->
        billingClient?.let { client ->
            if (!client.isReady) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            
            client.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    processPurchases(purchases)
                    continuation.resume(purchases.isNotEmpty())
                } else {
                    continuation.resume(false)
                }
            }
        } ?: continuation.resume(false)
    }
    
    fun checkGracePeriod(expiryDate: Date): Boolean {
        val gracePeriodEnd = Date(expiryDate.time + Subscription.GRACE_PERIOD_DAYS * 24 * 60 * 60 * 1000)
        return Date() < gracePeriodEnd
    }
    
    fun isTestUser(email: String?): Boolean {
        return email == BuildConfig.TEST_USER_EMAIL
    }
}

data class BillingState(
    val isConnected: Boolean = false,
    val availableProducts: List<ProductDetails> = emptyList(),
    val error: BillingError? = null
)

sealed class BillingError {
    object NetworkError : BillingError()
    object PurchaseCancelled : BillingError()
    object PurchasePending : BillingError()
    object ProductNotAvailable : BillingError()
    object ProductsNotAvailable : BillingError()
    data class ConnectionError(val message: String) : BillingError()
    data class Unknown(val message: String) : BillingError()
}