package com.zoutiw.omnichurch.core.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.zoutiw.omnichurch.BuildConfig
import com.zoutiw.omnichurch.core.network.FirestoreProvider
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class OmniAdPlacement {
  HOME,
  ANNOUNCEMENTS,
  SERVICES,
}

/**
 * Exact `adMob` map schema inside `app_config/developer_engine` in the shared
 * OmniChurch Pro named Firestore database (`ai-studio-applegizplatfmmu-438b9782-c8b8-4863-805f-d40924b9f1d1`).
 */
data class RemoteAdMobConfig(
  val enabled: Boolean = false,
  val environment: String = "test",
  val mobileEnabled: Boolean = false,
  val mobileForceTestIds: Boolean = false,
  val adMobAppId: String = AdMobRemoteController.TEST_APP_ID,
  val bannerAdUnitId: String = AdMobRemoteController.TEST_BANNER_ID,
  val interstitialAdUnitId: String = AdMobRemoteController.TEST_INTERSTITIAL_ID,
  val rewardedAdUnitId: String = AdMobRemoteController.TEST_REWARDED_ID,
  val nativeAdUnitId: String = AdMobRemoteController.TEST_NATIVE_ID,
  val adFreeForLicensedChurches: Boolean = true,
  val mobileShowBannerHome: Boolean = true,
  val mobileShowBannerAnnouncements: Boolean = true,
  val mobileShowBannerServices: Boolean = false,
  val enableInterstitialOnScan: Boolean = true,
  val mobileEnableInterstitialAfterScanClose: Boolean = true,
  val interstitialScanFrequency: Int = 4,
  val mobileInterstitialCooldownSeconds: Int = 180,
  val mobileRewardedEnabled: Boolean = false,
  val mobileNativeFeedEnabled: Boolean = true,
  val lastCloudPushAt: String? = null,
) {
  companion object {
    fun fromMap(rawMap: Map<*, *>?): RemoteAdMobConfig {
      if (rawMap == null) return RemoteAdMobConfig()

      fun bool(key: String, default: Boolean): Boolean =
        when (val v = rawMap[key]) {
          is Boolean -> v
          is String -> v.trim().equals("true", ignoreCase = true)
          is Number -> v.toInt() != 0
          else -> default
        }

      fun str(key: String, default: String): String =
        (rawMap[key] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: default

      fun int(key: String, default: Int): Int =
        when (val v = rawMap[key]) {
          is Number -> v.toInt()
          is String -> v.trim().toIntOrNull() ?: default
          else -> default
        }

      return RemoteAdMobConfig(
        enabled = bool("enabled", false),
        environment = str("environment", "test"),
        mobileEnabled = bool("mobileEnabled", false),
        mobileForceTestIds = bool("mobileForceTestIds", false),
        adMobAppId = str("adMobAppId", AdMobRemoteController.TEST_APP_ID),
        bannerAdUnitId = str("bannerAdUnitId", AdMobRemoteController.TEST_BANNER_ID),
        interstitialAdUnitId = str("interstitialAdUnitId", AdMobRemoteController.TEST_INTERSTITIAL_ID),
        rewardedAdUnitId = str("rewardedAdUnitId", AdMobRemoteController.TEST_REWARDED_ID),
        nativeAdUnitId = str("nativeAdUnitId", AdMobRemoteController.TEST_NATIVE_ID),
        adFreeForLicensedChurches = bool("adFreeForLicensedChurches", true),
        mobileShowBannerHome = bool("mobileShowBannerHome", true),
        mobileShowBannerAnnouncements = bool("mobileShowBannerAnnouncements", true),
        mobileShowBannerServices = bool("mobileShowBannerServices", false),
        enableInterstitialOnScan = bool("enableInterstitialOnScan", true),
        mobileEnableInterstitialAfterScanClose =
          bool("mobileEnableInterstitialAfterScanClose", true),
        interstitialScanFrequency = int("interstitialScanFrequency", 4).coerceAtLeast(1),
        mobileInterstitialCooldownSeconds =
          int("mobileInterstitialCooldownSeconds", 180).coerceAtLeast(0),
        mobileRewardedEnabled = bool("mobileRewardedEnabled", false),
        mobileNativeFeedEnabled = bool("mobileNativeFeedEnabled", true),
        lastCloudPushAt = (rawMap["lastCloudPushAt"] as? String)?.trim()?.takeIf { it.isNotEmpty() },
      )
    }
  }
}

/**
 * Exact `licenses` list entry inside `app_config/developer_engine`.
 */
data class RemoteChurchLicense(
  val key: String = "",
  val churchId: String = "",
  val status: String = "pending",
  val expiresAt: String = "never",
  val allowAdFree: Boolean = false,
) {
  fun isActiveAndAdFreeFor(
    targetChurchId: String,
    targetChurchCode: String = "",
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cleanTargetId = targetChurchId.trim()
    val cleanTargetCode = targetChurchCode.trim()
    if (cleanTargetId.isEmpty() && cleanTargetCode.isEmpty()) return false

    val cleanLicenseChurch = churchId.trim()
    val matchesChurch =
      cleanLicenseChurch.equals("GLOBAL", ignoreCase = true) ||
        (cleanTargetId.isNotEmpty() && cleanLicenseChurch.equals(cleanTargetId, ignoreCase = true)) ||
        (cleanTargetCode.isNotEmpty() &&
          cleanLicenseChurch.equals(cleanTargetCode, ignoreCase = true))

    if (!matchesChurch) return false
    if (!status.trim().equals("active", ignoreCase = true)) return false
    if (!allowAdFree) return false

    val cleanExpires = expiresAt.trim()
    if (cleanExpires.isEmpty() || cleanExpires.equals("never", ignoreCase = true)) {
      return true
    }
    val parsedExpiry = parseIsoOrDateMillis(cleanExpires) ?: return true
    return parsedExpiry > nowMillis
  }

  fun isActiveOfficialLicenseFor(
    targetChurchId: String,
    targetChurchCode: String = "",
    nowMillis: Long = System.currentTimeMillis(),
  ): Boolean {
    val cleanTargetId = targetChurchId.trim()
    val cleanTargetCode = targetChurchCode.trim()
    if (cleanTargetId.isEmpty() && cleanTargetCode.isEmpty()) return false

    val cleanLicenseChurch = churchId.trim()
    val matchesChurch =
      cleanLicenseChurch.equals("GLOBAL", ignoreCase = true) ||
        (cleanTargetId.isNotEmpty() && cleanLicenseChurch.equals(cleanTargetId, ignoreCase = true)) ||
        (cleanTargetCode.isNotEmpty() &&
          cleanLicenseChurch.equals(cleanTargetCode, ignoreCase = true))

    if (!matchesChurch) return false
    if (!status.trim().equals("active", ignoreCase = true)) return false

    val cleanExpires = expiresAt.trim()
    if (cleanExpires.isEmpty() || cleanExpires.equals("never", ignoreCase = true)) {
      return true
    }
    val parsedExpiry = parseIsoOrDateMillis(cleanExpires) ?: return true
    return parsedExpiry > nowMillis
  }

  companion object {
    fun fromList(rawList: Any?): List<RemoteChurchLicense> {
      val list = rawList as? List<*> ?: return emptyList()
      return list.mapNotNull { item ->
        val map = item as? Map<*, *> ?: return@mapNotNull null
        val key = (map["key"] as? String)?.trim().orEmpty()
        val churchId = (map["churchId"] as? String)?.trim().orEmpty()
        val status = (map["status"] as? String)?.trim()?.ifEmpty { "pending" } ?: "pending"
        val expiresAt = (map["expiresAt"] as? String)?.trim()?.ifEmpty { "never" } ?: "never"
        val allowAdFree =
          when (val v = map["allowAdFree"]) {
            is Boolean -> v
            is String -> v.trim().equals("true", ignoreCase = true)
            is Number -> v.toInt() != 0
            else -> false
          }
        RemoteChurchLicense(
          key = key,
          churchId = churchId,
          status = status,
          expiresAt = expiresAt,
          allowAdFree = allowAdFree,
        )
      }
    }

    private fun parseIsoOrDateMillis(dateStr: String): Long? {
      val patterns =
        listOf(
          "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
          "yyyy-MM-dd'T'HH:mm:ss'Z'",
          "yyyy-MM-dd'T'HH:mm:ss",
          "yyyy-MM-dd",
        )
      for (pattern in patterns) {
        try {
          val sdf = SimpleDateFormat(pattern, Locale.US)
          if (pattern.endsWith("'Z'")) {
            sdf.timeZone = TimeZone.getTimeZone("UTC")
          }
          val parsed = sdf.parse(dateStr)
          if (parsed != null) return parsed.time
        } catch (_: Exception) {
          // try next format
        }
      }
      return null
    }
  }
}

/**
 * Exact `memberVerification` object schema inside `app_config/developer_engine` shared with Web.
 */
data class RemoteMemberVerificationConfig(
  val enabled: Boolean = true,
  val disabledChurchIds: List<String> = emptyList(),
  val enforceActiveStatusCheck: Boolean = true,
  val showMemberPhoto: Boolean = true,
  val showLegalDocuments: Boolean = true,
  val maskLegalDocumentNumber: Boolean = true,
  val showChurchContactInfo: Boolean = true,
  val showChurchRncBadge: Boolean = true,
  val allowPrintAndShare: Boolean = true,
  val customAuthorityNotice: String = "",
  val verificationBaseUrl: String = AdMobRemoteController.OFFICIAL_PLATFORM_VERIFICATION_URL,
) {
  val effectiveVerificationBaseUrl: String
    get() = AdMobRemoteController.sanitizeVerificationBaseUrl(verificationBaseUrl)

  fun isEnabledForChurch(churchId: String?, churchCode: String? = null): Boolean {
    if (!enabled) return false
    val cleanId = churchId?.trim().orEmpty()
    val cleanCode = churchCode?.trim().orEmpty()
    if (disabledChurchIds.isEmpty()) return true
    return disabledChurchIds.none { disabled ->
      val cleanDisabled = disabled.trim()
      cleanDisabled.isNotEmpty() &&
        ((cleanId.isNotEmpty() && cleanDisabled.equals(cleanId, ignoreCase = true)) ||
          (cleanCode.isNotEmpty() && cleanDisabled.equals(cleanCode, ignoreCase = true)))
    }
  }

  companion object {
    fun fromMap(rawMap: Map<*, *>?): RemoteMemberVerificationConfig {
      if (rawMap == null) return RemoteMemberVerificationConfig()

      fun bool(key: String, default: Boolean): Boolean =
        when (val v = rawMap[key]) {
          is Boolean -> v
          is String -> v.trim().equals("true", ignoreCase = true)
          is Number -> v.toInt() != 0
          else -> default
        }

      fun str(key: String, default: String = ""): String =
        (rawMap[key] as? String)?.trim() ?: default

      val disabledIds =
        (rawMap["disabledChurchIds"] as? List<*>)
          ?.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
          .orEmpty()

      return RemoteMemberVerificationConfig(
        enabled = bool("enabled", true),
        disabledChurchIds = disabledIds,
        enforceActiveStatusCheck = bool("enforceActiveStatusCheck", true),
        showMemberPhoto = bool("showMemberPhoto", true),
        showLegalDocuments = bool("showLegalDocuments", true),
        maskLegalDocumentNumber = bool("maskLegalDocumentNumber", true),
        showChurchContactInfo = bool("showChurchContactInfo", true),
        showChurchRncBadge = bool("showChurchRncBadge", true),
        allowPrintAndShare = bool("allowPrintAndShare", true),
        customAuthorityNotice = str("customAuthorityNotice", ""),
        verificationBaseUrl =
          AdMobRemoteController.sanitizeVerificationBaseUrl(str("verificationBaseUrl")),
      )
    }
  }
}

/**
 * Real-time resolved mobile AdMob state exposed to Jetpack Compose UI and ViewModels.
 */
data class EffectiveMobileAdMobState(
  val config: RemoteAdMobConfig = RemoteAdMobConfig(),
  val licenses: List<RemoteChurchLicense> = emptyList(),
  val activeChurchId: String = "",
  val activeChurchCode: String = "",
  val isTestMode: Boolean = true,
  val isChurchAdFree: Boolean = false,
  val shouldShowAds: Boolean = false,
  val effectiveAppId: String = AdMobRemoteController.TEST_APP_ID,
  val effectiveBannerId: String = AdMobRemoteController.TEST_BANNER_ID,
  val effectiveInterstitialId: String = AdMobRemoteController.TEST_INTERSTITIAL_ID,
  val effectiveRewardedId: String = AdMobRemoteController.TEST_REWARDED_ID,
  val effectiveNativeId: String = AdMobRemoteController.TEST_NATIVE_ID,
  val sessionScanCount: Int = 0,
  val lastInterstitialShownAtMs: Long = 0L,
) {
  fun shouldShowBanner(placement: OmniAdPlacement): Boolean {
    if (!shouldShowAds || !config.enabled || !config.mobileEnabled || isChurchAdFree) return false
    return when (placement) {
      OmniAdPlacement.HOME -> config.mobileShowBannerHome
      OmniAdPlacement.ANNOUNCEMENTS -> config.mobileShowBannerAnnouncements
      OmniAdPlacement.SERVICES -> config.mobileShowBannerServices
    }
  }
}

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
              recomputeState(
                config = RemoteAdMobConfig(),
                licenses = emptyList(),
              )
              return@addSnapshotListener
            }

            val rawAdMobMap = snapshot.get("adMob") as? Map<*, *>
            val rawVerificationMap = snapshot.get("memberVerification") as? Map<*, *>
            val rawLicensesList = snapshot.get("licenses")
            val parsedConfig = RemoteAdMobConfig.fromMap(rawAdMobMap)
            val parsedVerification = RemoteMemberVerificationConfig.fromMap(rawVerificationMap)
            val parsedLicenses = RemoteChurchLicense.fromList(rawLicensesList)

            _memberVerificationState.value = parsedVerification
            _licensesState.value = parsedLicenses
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

    // Consume the scan counter and record cooldown timestamp when threshold is reached on close
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

  fun preloadInterstitialIfNeeded(context: Context, adUnitId: String = _state.value.effectiveInterstitialId) {
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
