package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zoutiw.omnichurch.base.FirestoreEmulatorTestBase
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniChurchAuthAndSecurityRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun authRepository_emailPasswordMultiChurchReuse_securitySettings_andQuickPinUpgrade() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val sessionLockManager = SessionLockManager(context)
      val preferencesManager = OmniPreferencesManager(context)
      val churchRepo = ChurchRepository(firestore)
      val authRepo =
        AuthRepository(
          db = firestore,
          churchRepository = churchRepo,
          firebaseAuth = auth,
          sessionLockManager = sessionLockManager,
          preferencesManager = preferencesManager,
        )

      val suffix = UUID.randomUUID().toString().take(6).uppercase()
      val ownerEmail = "owner_$suffix@omnichurch.org"
      val ownerPassword = "StrongPassword2026!"

      signInTestUser("probe_$suffix@omnichurch.org")
      auth.signOut()

      val church1 =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo
            .createChurchWithEmailPassword(
              churchName = "Legliz Premye $suffix",
              churchCode = "P$suffix",
              churchAddress = "Port-au-Prince",
              email = ownerEmail,
              password = ownerPassword,
              languageCode = "en",
            )
            .getOrThrow()
        }
      assertEquals(ownerEmail.lowercase(), church1.adminGoogleEmail)
      assertEquals("email_password", church1.adminAuthMethod)
      assertEquals(false, church1.effectiveQuickPinEnabled)
      assertEquals(5, church1.effectiveAutoLockMinutes)
      assertNotNull(church1.adminGoogleUid)

      val church2 =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo
            .createChurchWithEmailPassword(
              churchName = "Legliz Dezyem $suffix",
              churchCode = "D$suffix",
              churchAddress = "Cap-Haitien",
              email = ownerEmail,
              password = ownerPassword,
              languageCode = "en",
            )
            .getOrThrow()
        }
      assertEquals(church1.adminGoogleUid, church2.adminGoogleUid)

      val wrongCollision =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.createChurchWithEmailPassword(
            churchName = "Legliz Fail $suffix",
            churchCode = "F$suffix",
            churchAddress = "Delmas",
            email = ownerEmail,
            password = "WrongPassword999",
            languageCode = "en",
          )
        }
      assertTrue(wrongCollision.isFailure)

      val signedInChurch1 =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo
            .signInAdminWithEmailAndPassword(
              church = church1,
              email = ownerEmail,
              password = ownerPassword,
              languageCode = "en",
            )
            .getOrThrow()
        }
      val updatedChurch1 =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo
            .updateChurchSecuritySettings(
              church = signedInChurch1,
              quickPinEnabled = true,
              newPinOrNull = "4589",
              autoLockMinutes = 15,
              languageCode = "en",
            )
            .getOrThrow()
        }
      assertTrue(updatedChurch1.quickPinEnabled)
      assertEquals(15, updatedChurch1.autoLockMinutes)
      assertTrue(CryptoSecurity.isHashedPasscode(updatedChurch1.adminPasscode))

      val pinOk =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.verifyQuickPinAndUpgradeIfNeeded(updatedChurch1, "4589", "en")
        }
      assertTrue(pinOk.isSuccess)
      val pinBad =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.verifyQuickPinAndUpgradeIfNeeded(updatedChurch1, "0000", "en")
        }
      assertTrue(pinBad.isFailure)

      val legacyPlainChurch = updatedChurch1.copy(adminPasscode = "7391")
      firestore
        .collection("churches")
        .document(updatedChurch1.id)
        .update("adminPasscode", "7391")
        .await()
      val upgradedChurch =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.verifyQuickPinAndUpgradeIfNeeded(legacyPlainChurch, "7391", "en").getOrThrow()
        }
      assertTrue(CryptoSecurity.isHashedPasscode(upgradedChurch.adminPasscode))

      val intruderEmail = "intruder_$suffix@other.org"
      val intruderPassword = "IntruderPassword2026!"
      withTimeout(DEFAULT_TIMEOUT_MS) {
        authRepo.registerOrReuseAdminWithEmailPassword(intruderEmail, intruderPassword, "Intruder", "en")
      }
      val mismatchedSignIn =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.signInAdminWithEmailAndPassword(
            church = upgradedChurch,
            email = intruderEmail,
            password = intruderPassword,
            languageCode = "en",
          )
        }
      assertTrue(mismatchedSignIn.isFailure)
    }

  @Test
  fun remoteDeveloperEngine_realTimeAdMobAndLicenseSync_updatesEffectiveStateForAllClients() =
    runBlocking {
      signInVerifiedSuperAdmin("charlesjj3186@gmail.com")
      firestore
        .collection("app_config")
        .document("developer_engine")
        .set(
          mapOf(
            "adMob" to
              mapOf(
                "enabled" to true,
                "environment" to "production",
                "mobileEnabled" to true,
                "mobileForceTestIds" to false,
                "adMobAppId" to "ca-app-pub-9999999999999999~1111111111",
                "bannerAdUnitId" to "ca-app-pub-9999999999999999/2222222222",
                "interstitialAdUnitId" to "ca-app-pub-9999999999999999/3333333333",
                "adFreeForLicensedChurches" to true,
                "mobileShowBannerHome" to true,
                "mobileShowBannerAnnouncements" to true,
                "mobileShowBannerServices" to true,
                "enableInterstitialOnScan" to true,
                "mobileEnableInterstitialAfterScanClose" to true,
                "interstitialScanFrequency" to 2,
                "mobileInterstitialCooldownSeconds" to 120,
              ),
            "licenses" to
              listOf(
                mapOf(
                  "key" to "PRO-TABENAK-2026",
                  "churchId" to "CH_PRO_01",
                  "status" to "active",
                  "expiresAt" to "never",
                  "allowAdFree" to true,
                )
              ),
          )
        )
        .await()

      auth.signOut()
      val controller = AdMobRemoteController(firestore = firestore, isDebugBuild = false)
      try {
        val syncedState =
          withTimeout(FLOW_TIMEOUT_MS) {
            controller.state.first {
              it.config.bannerAdUnitId == "ca-app-pub-9999999999999999/2222222222"
            }
          }
        assertFalse(syncedState.isTestMode)
        assertEquals("ca-app-pub-9999999999999999/2222222222", syncedState.effectiveBannerId)
        assertTrue(syncedState.shouldShowAds)
        assertTrue(syncedState.shouldShowBanner(com.zoutiw.omnichurch.core.ads.OmniAdPlacement.SERVICES))

        controller.updateActiveChurch(churchId = "CH_PRO_01", churchCode = "CH_PRO_01")
        val licensedState =
          withTimeout(FLOW_TIMEOUT_MS) { controller.state.first { it.isChurchAdFree } }
        assertFalse(licensedState.shouldShowAds)
        assertFalse(licensedState.shouldShowBanner(com.zoutiw.omnichurch.core.ads.OmniAdPlacement.HOME))

        controller.updateActiveChurch(churchId = "CH_FREE_02", churchCode = "CH_FREE_02")
        val freeState =
          withTimeout(FLOW_TIMEOUT_MS) { controller.state.first { !it.isChurchAdFree && it.shouldShowAds } }
        assertTrue(freeState.shouldShowAds)

        controller.onQrScanSuccess()
        assertFalse(controller.canShowInterstitialOnScannerClose(nowMs = 500_000L))
        controller.onQrScanSuccess()
        assertTrue(controller.canShowInterstitialOnScannerClose(nowMs = 500_000L))
        assertTrue(controller.onScannerScreenClosed(activity = null, nowMs = 500_000L))
        assertEquals(0, controller.state.value.sessionScanCount)
        assertEquals(500_000L, controller.state.value.lastInterstitialShownAtMs)
      } finally {
        controller.stopListening()
      }
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 10000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
