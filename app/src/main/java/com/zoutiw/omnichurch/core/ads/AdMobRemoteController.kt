package com.zoutiw.omnichurch.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zoutiw.omnichurch.BuildConfig
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.presentation.components.RemoteShareAppConfig
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Real-Time Remote AdMob & Developer Engine Controller (`app_config/developer_engine`).
 *
 * Enforces:
 * 1. Real-time Firestore listener on `app_config/developer_engine` in the shared named database.
 * 2. Anti-Ban Lock: Forces Google Official Test Ad Unit IDs whenever `BuildConfig.DEBUG`,
 *    `environment == "test"`, or `mobileForceTestIds == true`.
 * 3. Pro Church License Ad-Free check (`adFreeForLicensedChurches` + active `licenses` entry).
 * 4. Non-intrusive Door Scanner Interstitial lifecycle (never interrupts active QR scanning;
 *    preloads in background and only triggers when closing the scanner screen if frequency and cooldown pass).
 * 5. Real-time `memberVerification` and `licenses` config sync for Official QR & Authority Verification.
 */
class AdMobRemoteController(
  private val firestore: FirebaseFirestore,
  private val isDebugBuild: Boolean = BuildConfig.DEBUG,
) {
  private val _state =
    MutableStateFlow(
      resolveEffectiveState(
        config = RemoteAdMobConfig(),
        licenses = emptyList(),
        activeChurchId = "",
        activeChurchCode = "",
        isDebugBuild = isDebugBuild,
      )
    )
  val state: StateFlow<EffectiveMobileAdMobState> = _state.asStateFlow()

  private val _memberVerificationState = MutableStateFlow(RemoteMemberVerificationConfig())
  val memberVerificationState: StateFlow<RemoteMemberVerificationConfig> =
    _memberVerificationState.asStateFlow()

  private val _licensesState = MutableStateFlow<List<RemoteChurchLicense>>(emptyList())
  val licensesState: StateFlow<List<RemoteChurchLicense>> = _licensesState.asStateFlow()

  private val _donationConfigState = MutableStateFlow(DeveloperDonationConfig())
  val donationConfigState: StateFlow<DeveloperDonationConfig> = _donationConfigState.asStateFlow()

  private val _shareAppConfigState = MutableStateFlow(RemoteShareAppConfig())
  val shareAppConfigState: StateFlow<RemoteShareAppConfig> = _shareAppConfigState.asStateFlow()

  private var listenerRegistration: ListenerRegistration? = null
  private var preloadedInterstitialAd: InterstitialAd? = null
  private var preloadedInterstitialUnitId: String? = null
  private var isPreloadingInterstitial: Boolean = false
  @Volatile private var isMobileAdsInitialized: Boolean = false
  private val mobileAdsInitLock = Any()

  init {
    startListening()
  }

  fun startListening() {
    if (listenerRegistration != null) return
    try {
      listenerRegistration =
        firestore
          .collection(COLLECTION_APP_CONFIG)
          .document(DOC_DEVELOPER_ENGINE)
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(
                TAG,
                "developer_engine listener error (${error.code}); preserving safe fallback config.",
              )
              return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
              _memberVerificationState.value = RemoteMemberVerificationConfig()
              _licensesState.value = emptyList()
              _donationConfigState.value = DeveloperDonationConfig()
              _shareAppConfigState.value = RemoteShareAppConfig()
              recomputeState(
                config = RemoteAdMobConfig(),
                licenses = emptyList(),
              )
              return@addSnapshotListener
            }

            val rawAdMobMap = snapshot.get("adMob") as? Map<*, *>
            val rawVerificationMap = snapshot.get("memberVerification") as? Map<*, *>
            val rawDonationsMap = snapshot.get("donations") as? Map<*, *>
            val rawShareAppMap =
              (snapshot.get("shareApp") as? Map<*, *>)
                ?: (snapshot.get("appShare") as? Map<*, *>)
                ?: (snapshot.get("shareConfig") as? Map<*, *>)
            val rawLicensesList = snapshot.get("licenses")
            val parsedConfig = RemoteAdMobConfig.fromMap(rawAdMobMap)
            val parsedVerification = RemoteMemberVerificationConfig.fromMap(rawVerificationMap)
            val parsedDonations = DeveloperDonationConfig.fromMap(rawDonationsMap)
            val parsedShareApp = RemoteShareAppConfig.fromMap(rawShareAppMap)
            val parsedLicenses = RemoteChurchLicense.fromList(rawLicensesList)

            _memberVerificationState.value = parsedVerification
            _licensesState.value = parsedLicenses
            _donationConfigState.value = parsedDonations
            _shareAppConfigState.value = parsedShareApp
            recomputeState(
              config = parsedConfig,
              licenses = parsedLicenses,
            )
          }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to attach developer_engine listener; using safe defaults.", e)
    }
  }

  fun isChurchLicensed(
    churchId: String?,
    churchCode: String? = null,
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cleanId = churchId?.trim().orEmpty()
    val cleanCode = churchCode?.trim().orEmpty()
    if (cleanId.isEmpty() && cleanCode.isEmpty()) return false
    return _licensesState.value.any { license ->
      license.isActiveOfficialLicenseFor(
        targetChurchId = cleanId,
        targetChurchCode = cleanCode,
        nowMillis = nowMillis,
      )
    }
  }

  fun stopListening() {
    listenerRegistration?.remove()
    listenerRegistration = null
  }

  fun updateActiveChurch(churchId: String?, churchCode: String? = null) {
    val cleanId = churchId?.trim().orEmpty()
    val cleanCode = churchCode?.trim().orEmpty()
    val current = _state.value
    if (current.activeChurchId == cleanId && current.activeChurchCode == cleanCode) return
    _state.update { prev ->
      resolveEffectiveState(
        config = prev.config,
        licenses = prev.licenses,
        activeChurchId = cleanId,
        activeChurchCode = cleanCode,
        isDebugBuild = isDebugBuild,
        sessionScanCount = prev.sessionScanCount,
        lastInterstitialShownAtMs = prev.lastInterstitialShownAtMs,
      )
    }
    if (!_state.value.shouldShowAds) {
      preloadedInterstitialAd = null
    }
  }

  private fun recomputeState(
    config: RemoteAdMobConfig,
    licenses: List<RemoteChurchLicense>,
  ) {
    _state.update { prev ->
      resolveEffectiveState(
        config = config,
        licenses = licenses,
        activeChurchId = prev.activeChurchId,
        activeChurchCode = prev.activeChurchCode,
        isDebugBuild = isDebugBuild,
        sessionScanCount = prev.sessionScanCount,
        lastInterstitialShownAtMs = prev.lastInterstitialShownAtMs,
      )
    }
    if (!_state.value.shouldShowAds) {
      preloadedInterstitialAd = null
    }
  }

  /**
   * Called after each successful QR scan at the church door.
   * NEVER shows an ad during scanning; only increments `sessionScanCount` and preloads in background.
   */
  fun onQrScanSuccess(context: Context? = null) {
    _state.update { prev -> prev.copy(sessionScanCount = prev.sessionScanCount + 1) }
    val current = _state.value
    if (
      context != null &&
        current.shouldShowAds &&
        current.config.enableInterstitialOnScan &&
        current.config.mobileEnableInterstitialAfterScanClose
    ) {
      preloadInterstitialIfNeeded(context.applicationContext, current.effectiveInterstitialId)
    }
  }

  /**
   * Evaluates whether the scanner-close interstitial gate is satisfied right now.
   */
  fun canShowInterstitialOnScannerClose(nowMs: Long = System.currentTimeMillis()): Boolean =
    canShowInterstitialOnScannerClose(_state.value, nowMs)

  /**
   * Called ONLY when the admin/usher exits/closes the QR Scanner screen.
   * Shows the preloaded interstitial if all frequency & cooldown rules pass, then resets `sessionScanCount`.
   */
  fun onScannerScreenClosed(
    activity: Activity?,
    nowMs: Long = System.currentTimeMillis(),
  ): Boolean {
    val current = _state.value
    if (!canShowInterstitialOnScannerClose(current, nowMs)) {
      return false
    }

    _state.update { prev ->
      prev.copy(
        sessionScanCount = 0,
        lastInterstitialShownAtMs = nowMs,
      )
    }

    val adToShow = preloadedInterstitialAd
    preloadedInterstitialAd = null
    if (adToShow != null && activity != null && !activity.isFinishing) {
      adToShow.fullScreenContentCallback =
        object : FullScreenContentCallback() {
          override fun onAdDismissedFullScreenContent() {
            preloadInterstitialIfNeeded(
              activity.applicationContext,
              _state.value.effectiveInterstitialId,
            )
          }

          override fun onAdFailedToShowFullScreenContent(adError: AdError) {
            Log.w(TAG, "Interstitial failed to show: ${adError.message}")
          }
        }
      runCatching { adToShow.show(activity) }
      return true
    } else if (activity != null) {
      preloadInterstitialIfNeeded(activity.applicationContext, current.effectiveInterstitialId)
    }
    return true
  }

  /**
   * Lazily initializes Google Mobile Ads SDK only when remote AdMob configuration actually enables
   * ads for a non-ad-free church, avoiding premature AdServices / Measurement service bindings at
   * cold start.
   */
  fun ensureMobileAdsInitialized(context: Context) {
    if (isMobileAdsInitialized || !_state.value.shouldShowAds) return
    synchronized(mobileAdsInitLock) {
      if (isMobileAdsInitialized || !_state.value.shouldShowAds) return
      isMobileAdsInitialized = true
    }
    val appContext = context.applicationContext
    Handler(Looper.getMainLooper()).post {
      runCatching { MobileAds.initialize(appContext) }
        .onFailure { error ->
          synchronized(mobileAdsInitLock) { isMobileAdsInitialized = false }
          Log.w(TAG, "MobileAds initialization unavailable in current environment", error)
        }
    }
  }

  fun preloadInterstitialIfNeeded(
    context: Context,
    adUnitId: String = _state.value.effectiveInterstitialId,
  ) {
    if (!_state.value.shouldShowAds) return
    if (adUnitId.isBlank()) return
    if (preloadedInterstitialAd != null && preloadedInterstitialUnitId == adUnitId) return
    if (isPreloadingInterstitial) return

    ensureMobileAdsInitialized(context)
    isPreloadingInterstitial = true
    val mainHandler = Handler(Looper.getMainLooper())
    mainHandler.post {
      try {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
          context.applicationContext,
          adUnitId,
          adRequest,
          object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
              isPreloadingInterstitial = false
              preloadedInterstitialAd = interstitialAd
              preloadedInterstitialUnitId = adUnitId
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
              isPreloadingInterstitial = false
              preloadedInterstitialAd = null
              Log.d(TAG, "Interstitial preload skipped/failed: ${loadAdError.message}")
            }
          },
        )
      } catch (e: Exception) {
        isPreloadingInterstitial = false
        Log.d(TAG, "Interstitial preload unavailable in current environment", e)
      }
    }
  }

  companion object {
    private const val TAG = "AdMobRemoteCtrl"
    const val NAMED_DATABASE_ID = "ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1"
    const val COLLECTION_APP_CONFIG = "app_config"
    const val DOC_DEVELOPER_ENGINE = "developer_engine"
    const val OFFICIAL_PLATFORM_VERIFICATION_URL = "https://verify.omnichurch.download"

    fun sanitizeVerificationBaseUrl(raw: String?): String {
      val trimmed = raw?.trim()?.trimEnd('/')?.trim().orEmpty()
      if (trimmed.isEmpty()) return OFFICIAL_PLATFORM_VERIFICATION_URL
      val lower = trimmed.lowercase(Locale.US)
      if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
        return OFFICIAL_PLATFORM_VERIFICATION_URL
      }
      if (
        lower.contains("capacitor://") ||
          lower.contains("ionic://") ||
          lower.contains("file://")
      ) {
        return OFFICIAL_PLATFORM_VERIFICATION_URL
      }
      val afterScheme = trimmed.substringAfter("://")
      val hostPort = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
      val host =
        if (hostPort.startsWith("[")) {
          hostPort.substringAfter("[").substringBefore("]")
        } else {
          hostPort.substringBefore(":")
        }.trim().lowercase(Locale.US)
      if (
        host.isEmpty() ||
          host == "localhost" ||
          host.endsWith(".localhost") ||
          host == "127.0.0.1" ||
          host == "0.0.0.0" ||
          host == "10.0.2.2" ||
          host == "::1" ||
          host.endsWith(".local") ||
          host.endsWith(".internal") ||
          host.startsWith("192.168.") ||
          host.startsWith("10.") ||
          !host.contains('.')
      ) {
        return OFFICIAL_PLATFORM_VERIFICATION_URL
      }
      return trimmed
    }

    // Google Official Test IDs (Mandatory Anti-Ban Fallbacks)
    const val TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"

    @Volatile private var INSTANCE: AdMobRemoteController? = null

    fun getInstance(context: Context): AdMobRemoteController =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: run {
              val db =
                try {
                  FirebaseFirestore.getInstance(
                    FirebaseApp.getInstance(),
                    NAMED_DATABASE_ID,
                  )
                } catch (_: Exception) {
                  FirestoreProvider.get(context.applicationContext)
                }
              AdMobRemoteController(firestore = db).also { INSTANCE = it }
            }
        }

    /**
     * Pure deterministic state evaluator for Anti-Ban Test Mode, Pro License Ad-Free status,
     * Master Kill-Switches, and Effective Ad Unit IDs.
     */
    fun resolveEffectiveState(
      config: RemoteAdMobConfig,
      licenses: List<RemoteChurchLicense>,
      activeChurchId: String,
      activeChurchCode: String = "",
      isDebugBuild: Boolean = BuildConfig.DEBUG,
      sessionScanCount: Int = 0,
      lastInterstitialShownAtMs: Long = 0L,
      nowMillis: Long = System.currentTimeMillis(),
    ): EffectiveMobileAdMobState {
      val isTestMode =
        isDebugBuild ||
          config.environment.trim().equals("test", ignoreCase = true) ||
          config.mobileForceTestIds

      val isChurchAdFree =
        config.adFreeForLicensedChurches &&
          licenses.any { license ->
            license.isActiveAndAdFreeFor(
              targetChurchId = activeChurchId,
              targetChurchCode = activeChurchCode,
              nowMillis = nowMillis,
            )
          }

      val shouldShowAds = config.enabled && config.mobileEnabled && !isChurchAdFree

      val effectiveAppId =
        if (isTestMode) TEST_APP_ID else config.adMobAppId.trim().ifBlank { TEST_APP_ID }
      val effectiveBannerId =
        if (isTestMode) TEST_BANNER_ID else config.bannerAdUnitId.trim().ifBlank { TEST_BANNER_ID }
      val effectiveInterstitialId =
        if (isTestMode) {
          TEST_INTERSTITIAL_ID
        } else {
          config.interstitialAdUnitId.trim().ifBlank { TEST_INTERSTITIAL_ID }
        }
      val effectiveRewardedId =
        if (isTestMode) {
          TEST_REWARDED_ID
        } else {
          config.rewardedAdUnitId.trim().ifBlank { TEST_REWARDED_ID }
        }
      val effectiveNativeId =
        if (isTestMode) {
          TEST_NATIVE_ID
        } else {
          config.nativeAdUnitId.trim().ifBlank { TEST_NATIVE_ID }
        }

      return EffectiveMobileAdMobState(
        config = config,
        licenses = licenses,
        activeChurchId = activeChurchId.trim(),
        activeChurchCode = activeChurchCode.trim(),
        isTestMode = isTestMode,
        isChurchAdFree = isChurchAdFree,
        shouldShowAds = shouldShowAds,
        effectiveAppId = effectiveAppId,
        effectiveBannerId = effectiveBannerId,
        effectiveInterstitialId = effectiveInterstitialId,
        effectiveRewardedId = effectiveRewardedId,
        effectiveNativeId = effectiveNativeId,
        sessionScanCount = sessionScanCount,
        lastInterstitialShownAtMs = lastInterstitialShownAtMs,
      )
    }

    /**
     * Pure evaluator for non-intrusive scanner-close interstitial eligibility.
     */
    fun canShowInterstitialOnScannerClose(
      state: EffectiveMobileAdMobState,
      nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
      val cfg = state.config
      if (!state.shouldShowAds) return false
      if (!cfg.enableInterstitialOnScan || !cfg.mobileEnableInterstitialAfterScanClose) return false
      val requiredScans = cfg.interstitialScanFrequency.coerceAtLeast(1)
      if (state.sessionScanCount < requiredScans) return false
      val cooldownMs = cfg.mobileInterstitialCooldownSeconds.coerceAtLeast(0) * 1000L
      if (
        state.lastInterstitialShownAtMs > 0L &&
          (nowMs - state.lastInterstitialShownAtMs) < cooldownMs
      ) {
        return false
      }
      return true
    }
  }
}
