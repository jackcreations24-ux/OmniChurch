package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.core.ads.RemoteAdMobConfig
import com.zoutiw.omnichurch.core.ads.RemoteChurchLicense
import com.zoutiw.omnichurch.presentation.components.isPlacementBannerEnabled
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobControllerUnitTest {

  @Test
  fun `AdMobRemoteController enforces Anti-Ban test IDs in test mode and resolves custom IDs only in production`() {
    val customProdMap =
      mapOf(
        "enabled" to true,
        "environment" to "production",
        "mobileEnabled" to true,
        "mobileForceTestIds" to true,
        "adMobAppId" to "ca-app-pub-1111111111111111~2222222222",
        "bannerAdUnitId" to "ca-app-pub-1111111111111111/3333333333",
        "interstitialAdUnitId" to "ca-app-pub-1111111111111111/4444444444",
        "rewardedAdUnitId" to "ca-app-pub-1111111111111111/5555555555",
        "nativeAdUnitId" to "ca-app-pub-1111111111111111/6666666666",
        "mobileShowBannerHome" to true,
        "mobileShowBannerAnnouncements" to true,
        "mobileShowBannerServices" to false,
      )
    val configLocked = RemoteAdMobConfig.fromMap(customProdMap)

    val stateLocked =
      AdMobRemoteController.resolveEffectiveState(
        config = configLocked,
        licenses = emptyList(),
        activeChurchId = "church_ch001",
        isDebugBuild = false,
      )
    assertTrue(stateLocked.isTestMode)
    assertEquals(AdMobRemoteController.TEST_BANNER_ID, stateLocked.effectiveBannerId)
    assertEquals(AdMobRemoteController.TEST_INTERSTITIAL_ID, stateLocked.effectiveInterstitialId)
    assertEquals(AdMobRemoteController.TEST_REWARDED_ID, stateLocked.effectiveRewardedId)
    assertEquals(AdMobRemoteController.TEST_NATIVE_ID, stateLocked.effectiveNativeId)
    assertTrue(stateLocked.shouldShowBanner(OmniAdPlacement.HOME))
    assertTrue(stateLocked.shouldShowBanner(OmniAdPlacement.ANNOUNCEMENTS))
    assertFalse(stateLocked.shouldShowBanner(OmniAdPlacement.SERVICES))

    val configUnlocked = configLocked.copy(mobileForceTestIds = false)
    val stateProd =
      AdMobRemoteController.resolveEffectiveState(
        config = configUnlocked,
        licenses = emptyList(),
        activeChurchId = "church_ch001",
        isDebugBuild = false,
      )
    assertFalse(stateProd.isTestMode)
    assertEquals("ca-app-pub-1111111111111111/3333333333", stateProd.effectiveBannerId)
    assertEquals("ca-app-pub-1111111111111111/4444444444", stateProd.effectiveInterstitialId)
    assertEquals("ca-app-pub-1111111111111111/5555555555", stateProd.effectiveRewardedId)
    assertEquals("ca-app-pub-1111111111111111/6666666666", stateProd.effectiveNativeId)
  }

  @Test
  fun `AdMobRemoteController disables all ads for active Pro Licensed church and respects scanner close frequency and cooldown`() {
    val rawLicenses =
      listOf(
        mapOf(
          "key" to "OMNI-PRO-CH001",
          "churchId" to "CH001",
          "status" to "active",
          "expiresAt" to "never",
          "allowAdFree" to true,
        ),
        mapOf(
          "key" to "OMNI-EXPIRED-ETG",
          "churchId" to "ETG01",
          "status" to "expired",
          "expiresAt" to "2020-01-01",
          "allowAdFree" to true,
        ),
      )
    val licenses = RemoteChurchLicense.fromList(rawLicenses)
    val baseConfig = RemoteAdMobConfig(enabled = true, mobileEnabled = true)

    val licensedState =
      AdMobRemoteController.resolveEffectiveState(
        config = baseConfig,
        licenses = licenses,
        activeChurchId = "church_ch001",
        activeChurchCode = "CH001",
        isDebugBuild = true,
      )
    assertTrue(licensedState.isChurchAdFree)
    assertFalse(licensedState.shouldShowAds)
    assertFalse(licensedState.shouldShowBanner(OmniAdPlacement.HOME))

    val unlicensedState =
      AdMobRemoteController.resolveEffectiveState(
        config = baseConfig,
        licenses = licenses,
        activeChurchId = "church_etg01",
        activeChurchCode = "ETG01",
        isDebugBuild = true,
        sessionScanCount = 3,
      )
    assertFalse(unlicensedState.isChurchAdFree)
    assertTrue(unlicensedState.shouldShowAds)

    val nowMs = 1_750_000_000_000L
    assertFalse(AdMobRemoteController.canShowInterstitialOnScannerClose(unlicensedState, nowMs))

    val readyState = unlicensedState.copy(sessionScanCount = 4, lastInterstitialShownAtMs = 0L)
    assertTrue(AdMobRemoteController.canShowInterstitialOnScannerClose(readyState, nowMs))

    val cooldownBlockedState =
      unlicensedState.copy(
        sessionScanCount = 5,
        lastInterstitialShownAtMs = nowMs - 60_000L,
      )
    assertFalse(AdMobRemoteController.canShowInterstitialOnScannerClose(cooldownBlockedState, nowMs))

    val cooldownElapsedState =
      unlicensedState.copy(
        sessionScanCount = 4,
        lastInterstitialShownAtMs = nowMs - 185_000L,
      )
    assertTrue(AdMobRemoteController.canShowInterstitialOnScannerClose(cooldownElapsedState, nowMs))
  }

  @Test
  fun `OmniAdMobBanner isPlacementBannerEnabled respects HOME ANNOUNCEMENTS SERVICES flags and master kill switches`() {
    val rawDefaultConfig = RemoteAdMobConfig()
    assertFalse(rawDefaultConfig.enabled)
    assertEquals("test", rawDefaultConfig.environment)
    assertFalse(rawDefaultConfig.mobileEnabled)
    assertFalse(rawDefaultConfig.mobileForceTestIds)
    val initialState =
      AdMobRemoteController.resolveEffectiveState(
        config = rawDefaultConfig,
        licenses = emptyList(),
        activeChurchId = "church_free",
        isDebugBuild = false,
      )
    assertTrue(initialState.isTestMode)
    assertFalse(initialState.shouldShowAds)
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.HOME, initialState))

    val defaultState =
      AdMobRemoteController.resolveEffectiveState(
        config = RemoteAdMobConfig(enabled = true, mobileEnabled = true),
        licenses = emptyList(),
        activeChurchId = "church_free",
      )
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.HOME, defaultState))
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.ANNOUNCEMENTS, defaultState))
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.SERVICES, defaultState))

    val customPlacementState =
      AdMobRemoteController.resolveEffectiveState(
        config =
          RemoteAdMobConfig(
            enabled = true,
            mobileEnabled = true,
            mobileShowBannerHome = false,
            mobileShowBannerAnnouncements = true,
            mobileShowBannerServices = true,
          ),
        licenses = emptyList(),
        activeChurchId = "church_free",
      )
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.HOME, customPlacementState))
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.ANNOUNCEMENTS, customPlacementState))
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.SERVICES, customPlacementState))

    val mobileKilledState =
      AdMobRemoteController.resolveEffectiveState(
        config =
          RemoteAdMobConfig(
            enabled = true,
            mobileEnabled = false,
            mobileShowBannerHome = true,
            mobileShowBannerAnnouncements = true,
            mobileShowBannerServices = true,
          ),
        licenses = emptyList(),
        activeChurchId = "church_free",
      )
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.HOME, mobileKilledState))
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.ANNOUNCEMENTS, mobileKilledState))
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.SERVICES, mobileKilledState))
  }

  @Test
  fun `verificationUrlSanitization rejects internal hosts and preserves valid https domains`() {
    val defaultUrl = AdMobRemoteController.OFFICIAL_PLATFORM_VERIFICATION_URL
    assertEquals("https://verify.omnichurch.download", defaultUrl)
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://localhost:3000"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://127.0.0.1:8080/"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://10.0.2.2:5173"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("https://192.168.1.42"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("capacitor://localhost"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("   "))
    assertEquals(
      "https://verify.omnichurch.download",
      AdMobRemoteController.sanitizeVerificationBaseUrl("  https://verify.omnichurch.download/  "),
    )
    assertEquals(
      "https://id.omnichurch.org",
      AdMobRemoteController.sanitizeVerificationBaseUrl("https://id.omnichurch.org///"),
    )
  }
}
