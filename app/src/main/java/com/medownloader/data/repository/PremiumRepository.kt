package com.medownloader.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import com.medownloader.BuildConfig
import com.revenuecat.purchases.*
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.StoreProduct
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Repository for managing premium state via RevenueCat.
 * 
 * Pricing Model: $3.99 LIFETIME ONLY (No Subscriptions)
 * 
 * Why Lifetime Only?
 * - Download managers are "burst usage" tools (download 10 movies, then nothing for weeks)
 * - Subscriptions cause churn: users subscribe, download, cancel
 * - Lifetime = one transaction, done. User feels ownership. We keep the money.
 * - $3.99 is the "charm price" sweet spot (perceived as "three dollars and change")
 * 
 * RevenueCat Project: meDownloader (proj88c8f9c7)
 * Entitlement: pro_features
 * Product: medownloader_pro_lifetime ($3.99 one-time)
 * 
 * Conversion Math (100k downloads @ 4.5% conversion):
 * - 4,500 users × $3.99 = ~$18,000 Year 1 Revenue
 */
interface PremiumRepository {
    
    /** Current premium status as a Flow. */
    val isPremium: StateFlow<Boolean>
    
    /** Initialize RevenueCat SDK. */
    fun initialize()
    
    /** Check if user has active premium entitlement. */
    suspend fun checkPremiumStatus(): Boolean
    
    /** Get the lifetime package for purchase. */
    suspend fun getLifetimePackage(): Result<Package>
    
    /** Purchase lifetime Pro. */
    suspend fun purchaseLifetime(activity: Activity): Result<PurchaseResult>
    
    /** Restore previous purchases. */
    suspend fun restorePurchases(): Result<RestoreResult>
    
    // ========================================================================
    // Tier Limits - These are actual limits (hard or soft depending on feature)
    // ========================================================================
    
    /** Max concurrent downloads (HARD LIMIT - blocks at limit) */
    fun getMaxConcurrentDownloads(): Int
    
    /** Max connections per file (affects speed) */
    fun getMaxConnectionsPerFile(): Int
    
    /** Max file segments (splits) */
    fun getMaxSplit(): Int
    
    /** Max items in download queue */
    fun getMaxQueueSize(): Int
    
    // ========================================================================
    // Power Features (HARD GATE - Premium only)
    // ========================================================================
    
    fun isTorrentEnabled(): Boolean
    fun isSchedulerEnabled(): Boolean
    fun isVideoSnifferEnabled(): Boolean
    fun isBatchImportEnabled(): Boolean
    fun isSpeedBoostEnabled(): Boolean
    fun isCustomThemesEnabled(): Boolean
    fun isDownloadAnalyticsEnabled(): Boolean
    fun isAutoExtractEnabled(): Boolean
    fun isBrowserIntegrationEnabled(): Boolean
    fun isDownloadCategoriesEnabled(): Boolean
    fun isSmartFilenameEnabled(): Boolean
    
    // ========================================================================
    // Upsell Triggers - When to suggest upgrade (non-blocking)
    // ========================================================================
    
    /** Should show "upgrade for more" when user hits concurrent limit */
    fun isAtConcurrentLimit(currentActive: Int): Boolean
    
    /** Should show "upgrade for faster" when download is slow */
    fun shouldSuggestSpeedBoost(downloadSpeedBytesPerSec: Long): Boolean
    
    /** Get customer info for debugging/support. */
    suspend fun getCustomerInfo(): Result<CustomerInfo>
    
    /** Log out (for testing). */
    suspend fun logOut(): Result<Unit>
}

data class PurchaseResult(
    val transaction: StoreTransaction,
    val customerInfo: CustomerInfo,
    val isPremiumNow: Boolean
)

data class RestoreResult(
    val customerInfo: CustomerInfo,
    val isPremiumNow: Boolean,
    val wasRestored: Boolean  // True if a purchase was actually restored
)

/**
 * Free Tier - The "Just Enough" Hook
 * 
 * Philosophy: Generous enough to feel complete. Limited enough to create natural desire.
 * 
 * Marketing Angle (Play Store):
 * ✅ "No ads, ever" (ADM has 15MB of ad SDKs - we have 0)
 * ✅ "No data harvesting" (We don't even have analytics)
 * ✅ "7× faster than your browser" (7 connections vs browser's 1)
 * ✅ "Download 4 files simultaneously"
 * ✅ "Pause & resume anytime"
 * ✅ "Works offline, no account needed"
 * ✅ "1/4 the size of ADM" (5MB vs 20MB)
 * 
 * Competitive Edge:
 * - We do 4 × 7 = 28 total streams
 * - Chrome does 6 × 1 = 6 streams
 * - We're objectively faster even on free tier
 */
object FreeTierLimits {
    // ===== CORE (Always Free, Always Works) =====
    // - HTTP/HTTPS/FTP: Works forever
    // - Pause/Resume: Always
    // - Share intent: Always
    // - Basic theme (Dark/Light): Always
    // - Download history: Unlimited
    
    // ===== The Sweet Spot Limits =====
    // These are HARD LIMITS but feel natural, not punishing
    
    /** 
     * 4 simultaneous downloads.
     * Why 4? Generous for casual use. Power users still hit this.
     * Browser comparison: Chrome does 6, but each is 1-connection (slow).
     * We do 4 × 7 connections = 28 total streams. Much faster overall.
     */
    const val MAX_CONCURRENT_DOWNLOADS = 4
    
    /**
     * 7 connections per file.
     * Why 7? 7× faster than browser's 1 connection - genuinely fast!
     * Premium gets 16 = still a nice 2× speed boost worth paying for.
     */
    const val CONNECTIONS_PER_FILE = 7
    
    /**
     * 7 file segments (splits).
     * Matches connections. More splits with same connections = no benefit.
     */
    const val MAX_SPLIT = 7
    
    /**
     * 10 downloads in queue.
     * Enough for most users. Data hoarders will want unlimited.
     */
    const val MAX_QUEUE_SIZE = 10
    
    // ===== Hard Gates (Premium Power Features) =====
    const val TORRENT_ENABLED = false           // P2P is power user territory
    const val SCHEDULER_ENABLED = false         // "Download at 3 AM" = power user
    const val VIDEO_SNIFFER_ENABLED = false     // Detect videos on pages
    const val BATCH_IMPORT_ENABLED = false       // Import 100 URLs at once
    const val SPEED_BOOST_ENABLED = false        // 16-connection turbo mode
    const val CUSTOM_THEMES_ENABLED = false      // AMOLED, custom colors
    const val DOWNLOAD_ANALYTICS_ENABLED = false // Speed graphs, history stats
    const val AUTO_EXTRACT_ENABLED = false       // Auto-unzip after download
    const val BROWSER_INTEGRATION_ENABLED = false// Catch browser downloads
    const val DOWNLOAD_CATEGORIES_ENABLED = false// Auto-sort by file type
    const val SMART_FILENAME_ENABLED = false     // Clean up ugly URLs
}

/**
 * Premium Tier - Power User Paradise ($3.99 Lifetime)
 * 
 * Value Proposition:
 * - 3× more simultaneous downloads (4 → 12)
 * - 2× faster per-file speed (7 → 16 connections)
 * - Unlimited queue (10 → ∞)
 * - BitTorrent/Magnet support
 * - Designer themes (Catppuccin, Gruvbox, Tokyo Night, Dracula, Nord)
 * - Schedule downloads for off-peak hours
 * - Auto-extract archives
 * 
 * Why $3.99?
 * - $3.99 = "Three dollars and change" (charm pricing)
 * - $4.99 = "Five dollars" (psychological cliff, 50% lower conversion)
 * - Lifetime = No churn, user feels ownership
 * 
 * The Theme Upsell:
 * Users will pay $4 just to make the app match their VS Code/Neovim setup.
 * Catppuccin Mocha in screenshots = instant "one of us" signal to power users.
 */
object PremiumTierLimits {
    // ===== Upgraded Core =====
    const val MAX_CONCURRENT_DOWNLOADS = 12      // 4× free tier
    const val CONNECTIONS_PER_FILE = 16          // 4× free tier, real speed boost
    const val MAX_SPLIT = 16                     // Matches connections
    const val MAX_QUEUE_SIZE = Int.MAX_VALUE     // Unlimited
    
    // ===== Power Features (Premium Exclusive) =====
    const val TORRENT_ENABLED = true
    const val SCHEDULER_ENABLED = true
    const val VIDEO_SNIFFER_ENABLED = true
    const val BATCH_IMPORT_ENABLED = true
    const val SPEED_BOOST_ENABLED = true
    const val CUSTOM_THEMES_ENABLED = true
    const val DOWNLOAD_ANALYTICS_ENABLED = true
    const val AUTO_EXTRACT_ENABLED = true
    const val BROWSER_INTEGRATION_ENABLED = true
    const val DOWNLOAD_CATEGORIES_ENABLED = true
    const val SMART_FILENAME_ENABLED = true
}

class PremiumRepositoryImpl(
    private val context: Context
) : PremiumRepository {
    
    companion object {
        private const val TAG = "PremiumRepository"
        
        /**
         * RevenueCat Public API Key (Test Store)
         * Project: meDownloader (proj88c8f9c7)
         * App: Test Store (app9a302f2c53)
         * 
         * IMPORTANT: Replace with production key before Play Store release.
         * Production key should come from a Play Store app type, not test_store.
         */
        private const val REVENUECAT_API_KEY = "test_jqiksbYDgSbIfWhCHJFbJdaROMA"
        
        /**
         * Entitlement ID that grants Pro features.
         * Single lifetime product maps to this entitlement.
         */
        private const val ENTITLEMENT_ID = "pro_features"
        
        /**
         * Product identifier - LIFETIME ONLY ($3.99)
         * No subscriptions. Users own it forever.
         */
        const val PRODUCT_LIFETIME = "medownloader_pro_lifetime"
    }
    
    private val _isPremium = MutableStateFlow(false)
    override val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()
    
    // Cached lifetime package for quick purchase
    private var cachedLifetimePackage: Package? = null

    override fun initialize() {
        try {
            Purchases.logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR
            
            Purchases.configure(
                PurchasesConfiguration.Builder(context, REVENUECAT_API_KEY)
                    .appUserID(null) // Anonymous user, RevenueCat generates ID
                    .observerMode(false) // We handle purchases
                    .diagnosticsEnabled(BuildConfig.DEBUG)
                    .build()
            )
            
            // Listen for real-time customer info updates
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
                updatePremiumState(info)
                Log.d(TAG, "Customer info updated: premium=${_isPremium.value}")
            }
            
            Log.i(TAG, "RevenueCat initialized (Lifetime-only model, \$3.99)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize RevenueCat", e)
        }
    }

    override suspend fun checkPremiumStatus(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            try {
                Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                    override fun onReceived(customerInfo: CustomerInfo) {
                        updatePremiumState(customerInfo)
                        continuation.resume(_isPremium.value)
                    }

                    override fun onError(error: PurchasesError) {
                        Log.e(TAG, "Failed to check premium: ${error.code} - ${error.message}")
                        // Don't change state on error - keep cached value
                        continuation.resume(_isPremium.value)
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Exception checking premium", e)
                continuation.resume(false)
            }
        }
    }

    override suspend fun getLifetimePackage(): Result<Package> {
        // Return cached if available
        cachedLifetimePackage?.let { return Result.success(it) }
        
        return try {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val lifetimePackage = offerings.current?.lifetime
            
            if (lifetimePackage != null) {
                cachedLifetimePackage = lifetimePackage
                Log.d(TAG, "Lifetime package: \$${lifetimePackage.product.price.formatted}")
                Result.success(lifetimePackage)
            } else {
                Log.e(TAG, "No lifetime package in current offering")
                Result.failure(Exception("Lifetime package not available"))
            }
        } catch (e: PurchasesException) {
            Log.e(TAG, "Failed to get offerings: ${e.code} - ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting offerings: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun purchaseLifetime(activity: Activity): Result<PurchaseResult> {
        val packageResult = getLifetimePackage()
        if (packageResult.isFailure) {
            return Result.failure(packageResult.exceptionOrNull() ?: Exception("Failed to get package"))
        }
        
        val pkg = packageResult.getOrThrow()
        
        return try {
            val purchaseParams = PurchaseParams.Builder(activity, pkg).build()
            val (transaction, customerInfo) = Purchases.sharedInstance.awaitPurchase(purchaseParams)
            
            updatePremiumState(customerInfo)
            
            val result = PurchaseResult(
                transaction = transaction,
                customerInfo = customerInfo,
                isPremiumNow = _isPremium.value
            )
            
            Log.i(TAG, "Lifetime purchase completed! Premium=${_isPremium.value}")
            Result.success(result)
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) {
                Log.d(TAG, "Purchase cancelled by user")
                Result.failure(PurchaseCancelledException())
            } else {
                Log.e(TAG, "Purchase failed: ${e.code} - ${e.message}")
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Purchase error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<RestoreResult> {
        return try {
            val wasPremiumBefore = _isPremium.value
            val customerInfo = Purchases.sharedInstance.awaitRestore()
            
            updatePremiumState(customerInfo)
            
            val result = RestoreResult(
                customerInfo = customerInfo,
                isPremiumNow = _isPremium.value,
                wasRestored = !wasPremiumBefore && _isPremium.value
            )
            
            Log.i(TAG, "Restore completed: wasRestored=${result.wasRestored}")
            Result.success(result)
        } catch (e: PurchasesException) {
            Log.e(TAG, "Restore failed: ${e.code} - ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Restore error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun getCustomerInfo(): Result<CustomerInfo> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            Result.success(customerInfo)
        } catch (e: PurchasesException) {
            Result.failure(e)
        }
    }

    override suspend fun logOut(): Result<Unit> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitLogOut()
            updatePremiumState(customerInfo)
            Result.success(Unit)
        } catch (e: PurchasesException) {
            Result.failure(e)
        }
    }

    // ========================================================================
    // Tier Limits - Hard limits that apply based on subscription status
    // ========================================================================

    override fun getMaxConcurrentDownloads(): Int {
        return if (_isPremium.value) PremiumTierLimits.MAX_CONCURRENT_DOWNLOADS 
               else FreeTierLimits.MAX_CONCURRENT_DOWNLOADS
    }

    override fun getMaxConnectionsPerFile(): Int {
        return if (_isPremium.value) PremiumTierLimits.CONNECTIONS_PER_FILE 
               else FreeTierLimits.CONNECTIONS_PER_FILE
    }
    
    override fun getMaxSplit(): Int {
        return if (_isPremium.value) PremiumTierLimits.MAX_SPLIT
               else FreeTierLimits.MAX_SPLIT
    }
    
    override fun getMaxQueueSize(): Int {
        return if (_isPremium.value) PremiumTierLimits.MAX_QUEUE_SIZE
               else FreeTierLimits.MAX_QUEUE_SIZE
    }

    // ========================================================================
    // Power Features (Premium Only)
    // ========================================================================

    override fun isTorrentEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.TORRENT_ENABLED 
               else FreeTierLimits.TORRENT_ENABLED
    }
    
    override fun isSchedulerEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.SCHEDULER_ENABLED
               else FreeTierLimits.SCHEDULER_ENABLED
    }
    
    override fun isVideoSnifferEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.VIDEO_SNIFFER_ENABLED
               else FreeTierLimits.VIDEO_SNIFFER_ENABLED
    }
    
    override fun isBatchImportEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.BATCH_IMPORT_ENABLED
               else FreeTierLimits.BATCH_IMPORT_ENABLED
    }
    
    override fun isSpeedBoostEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.SPEED_BOOST_ENABLED
               else FreeTierLimits.SPEED_BOOST_ENABLED
    }
    
    override fun isCustomThemesEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.CUSTOM_THEMES_ENABLED
               else FreeTierLimits.CUSTOM_THEMES_ENABLED
    }
    
    override fun isDownloadAnalyticsEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.DOWNLOAD_ANALYTICS_ENABLED
               else FreeTierLimits.DOWNLOAD_ANALYTICS_ENABLED
    }
    
    override fun isAutoExtractEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.AUTO_EXTRACT_ENABLED
               else FreeTierLimits.AUTO_EXTRACT_ENABLED
    }
    
    override fun isBrowserIntegrationEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.BROWSER_INTEGRATION_ENABLED
               else FreeTierLimits.BROWSER_INTEGRATION_ENABLED
    }
    
    override fun isDownloadCategoriesEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.DOWNLOAD_CATEGORIES_ENABLED
               else FreeTierLimits.DOWNLOAD_CATEGORIES_ENABLED
    }
    
    override fun isSmartFilenameEnabled(): Boolean {
        return if (_isPremium.value) PremiumTierLimits.SMART_FILENAME_ENABLED
               else FreeTierLimits.SMART_FILENAME_ENABLED
    }

    // ========================================================================
    // Upsell Triggers - Non-blocking suggestions
    // ========================================================================
    
    /**
     * Returns true when user is at their concurrent download limit.
     * Use this to show a friendly "Upgrade for more downloads" message.
     */
    override fun isAtConcurrentLimit(currentActive: Int): Boolean {
        if (_isPremium.value) return false  // Premium users have high limits
        return currentActive >= FreeTierLimits.MAX_CONCURRENT_DOWNLOADS
    }
    
    /**
     * Returns true when download speed is slow enough to suggest Speed Boost.
     * Threshold: < 1 MB/s on a file that could benefit from more connections.
     */
    override fun shouldSuggestSpeedBoost(downloadSpeedBytesPerSec: Long): Boolean {
        if (_isPremium.value) return false  // Already has speed boost
        // Suggest upgrade if download is slower than 1 MB/s
        return downloadSpeedBytesPerSec < 1_000_000L
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    private fun updatePremiumState(customerInfo: CustomerInfo) {
        val entitlement = customerInfo.entitlements[ENTITLEMENT_ID]
        _isPremium.value = entitlement?.isActive == true
        
        // Lifetime = no expiration, simple boolean state
        Log.d(TAG, "Premium state: ${_isPremium.value}")
    }
}

/**
 * Exception wrapper for RevenueCat errors.
 */
class RevenueCatException(
    val error: PurchasesError
) : Exception("RevenueCat Error ${error.code}: ${error.message}") {
    
    val isNetworkError: Boolean
        get() = error.code == PurchasesErrorCode.NetworkError
    
    val isStoreProblem: Boolean
        get() = error.code == PurchasesErrorCode.StoreProblemError
    
    val isReceiptAlreadyInUse: Boolean
        get() = error.code == PurchasesErrorCode.ReceiptAlreadyInUseError
    
    val isInvalidCredentials: Boolean
        get() = error.code == PurchasesErrorCode.InvalidCredentialsError
}

/**
 * Exception for user-cancelled purchases.
 */
class PurchaseCancelledException : Exception("Purchase was cancelled by user")
