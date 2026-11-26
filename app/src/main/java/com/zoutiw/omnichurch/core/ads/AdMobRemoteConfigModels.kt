package com.zoutiw.omnichurch.core.ads

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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
