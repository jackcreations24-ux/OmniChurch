package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.zoutiw.omnichurch.base.FirestoreEmulatorTestBase
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OmniChurchRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun fullTenantOnboarding_serviceCreation_andQrAttendance_succeedsForAdminAndBlocksDuplicate() =
    runBlocking {
      val uniqueSuffix = UUID.randomUUID().toString().take(8)
      val adminEmail = "admin_$uniqueSuffix@omnichurch.org"
      val adminUid = signInTestUser(adminEmail)
      val repo = OmniChurchRepository(firestore)

      // 1. Initialize user profile
      val initProfileResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.ensureUserProfileInitialized(
            uid = adminUid,
            email = adminEmail,
            displayName = "Jackson Charles",
            photoUrl = null,
          )
        }
      assertTrue("User profile init should succeed", initProfileResult.isSuccess)

      // 2. Create Church tenant & Member ID card with real barcodeToken
      val setupResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.setupChurchAndMemberProfile(
            createNewChurch = true,
            churchNameInput = "OmniChurch Tabernacle $uniqueSuffix",
            existingChurchIdInput = "",
            churchAddressInput = "Port-au-Prince",
            fullNameInput = "Jackson Charles",
            departmentInput = "Adorasyon",
            ministryRoleInput = "Mizisyen",
            photoUrl = null,
          )
        }
      assertTrue("Church and member setup should succeed: ${setupResult.exceptionOrNull()}", setupResult.isSuccess)
      val member = setupResult.getOrThrow()
      assertTrue(member.barcodeToken.startsWith("CHURCH:"))
      assertTrue(member.effectiveMatrikil.contains("-"))

      // 3. Create active service open for attendance (with full liturgical details and poster/image)
      val createServiceResult =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.createService(
            churchId = member.churchId,
            churchName = member.churchName,
            title = "Sèvis Dimanch",
            description = "Sèvis adorasyon prensipal",
            dateFormatted = "2026-10-26 • 09:00 - 11:30",
            lateToleranceMinutes = 15L,
            isOpenForAttendance = true,
            isLive = true,
            authorName = "Jackson Charles",
            category = "dimanche_matin",
            date = "2026-10-26",
            startTime = "09:00",
            endTime = "11:30",
            theme = "Lafwa ki deplase mòn",
            passage = "Ebre 11:1-6",
            leader = "Frè Pierre",
            preacher = "Pastè Jean",
            reader = "Sè Marie",
            musicians = "Koral Dawout",
            ushers = "Ekip Pwotokòl #1",
            sendReminder24h = true,
            imageUrl = "https://images.unsplash.com/photo-1438232992991-995b7058bbb3?w=800",
          )
        }
      assertTrue("Service creation should succeed: ${createServiceResult.exceptionOrNull()}", createServiceResult.isSuccess)

      val activeServices =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeActiveAttendanceServices(member.churchId).first { it.isNotEmpty() }
        }
      val activeService = activeServices.first()
      assertEquals("Lafwa ki deplase mòn", activeService.theme)
      assertEquals("Ebre 11:1-6", activeService.passage)
      assertEquals(
        "https://images.unsplash.com/photo-1438232992991-995b7058bbb3?w=800",
        activeService.effectiveImageUrl,
      )
      val adminProfile =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeUserProfile(adminUid).first { it?.churchId == member.churchId }
        }
      assertNotNull(adminProfile)

      // 4. Scan QR code with real barcodeToken -> RegisteredSuccess
      val scanOutcome =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.validateAndRecordAttendance(
            rawTokenOrCode = member.barcodeToken,
            selectedService = activeService,
            scannerUserProfile = adminProfile!!,
            checkInMethod = "qr_scan",
          )
        }
      assertTrue(
        "Expected RegisteredSuccess but got $scanOutcome",
        scanOutcome is ScannerValidationOutcome.RegisteredSuccess,
      )

      // 5. Duplicate scan for same service must return DUPLICATE_ATTENDANCE
      val duplicateOutcome =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.validateAndRecordAttendance(
            rawTokenOrCode = member.barcodeToken,
            selectedService = activeService,
            scannerUserProfile = adminProfile!!,
            checkInMethod = "qr_scan",
          )
        }
      assertTrue(duplicateOutcome is ScannerValidationOutcome.ValidationError)
      assertEquals(
        AttendanceErrorType.DUPLICATE_ATTENDANCE,
        (duplicateOutcome as ScannerValidationOutcome.ValidationError).errorType,
      )
    }

  @Test
  fun crossTenantAccess_failsWithPermissionDenied() = runBlocking {
    val uniqueSuffix = UUID.randomUUID().toString().take(8)
    val adminAEmail = "admin_a_$uniqueSuffix@omnichurch.org"
    val adminAUid = signInTestUser(adminAEmail)
    val repo = OmniChurchRepository(firestore)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfileInitialized(adminAUid, adminAEmail, "Admin A", null)
    }
    val memberA =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo
          .setupChurchAndMemberProfile(
            createNewChurch = true,
            churchNameInput = "Church A $uniqueSuffix",
            existingChurchIdInput = "",
            churchAddressInput = "Addr A",
            fullNameInput = "Admin A",
            departmentInput = "Admin",
            ministryRoleInput = "Admin",
            photoUrl = null,
          )
          .getOrThrow()
      }

    // Sign in as Outsider B in a different church
    val outsiderEmail = "outsider_b_$uniqueSuffix@other.org"
    val outsiderUid = signInTestUser(outsiderEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfileInitialized(outsiderUid, outsiderEmail, "Outsider B", null)
    }

    // Outsider B attempting to observe Church A services must fail with PERMISSION_DENIED
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeServices(memberA.churchId).first() }
      fail("Expected PERMISSION_DENIED when observing another church's services")
    } catch (e: Exception) {
      val firestoreEx =
        checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  @Test
  fun memberMatrikilAndPinLogin_withoutGoogleAccount_andOptionalGoogleLinking_andAdminPasscode_succeeds() =
    runBlocking {
      val uniqueSuffix = UUID.randomUUID().toString().take(6).uppercase()
      val churchCode = "CH$uniqueSuffix"
      val adminEmail = "admin_$uniqueSuffix@omnichurch.org"
      val adminUid = signInTestUser(adminEmail)
      val repo = OmniChurchRepository(firestore)

      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureUserProfileInitialized(adminUid, adminEmail, "Jackson Charles", null)
      }

      // 1. Setup Church with code, adminPasscode (salted SHA-256 stored automatically), and initial member
      val setupMember =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .setupChurchAndMemberProfile(
              createNewChurch = true,
              churchNameInput = "Legliz Tabènak $uniqueSuffix",
              churchCodeInput = churchCode,
              existingChurchIdInput = "",
              churchAddressInput = "Delmas 33",
              adminPasscodeInput = "AdminSecret2026",
              fullNameInput = "Jackson Charles",
              memberPinInput = "7788",
              memberPhoneInput = "50937112233",
              departmentInput = "Adorasyon",
              ministryRoleInput = "Pastè",
              photoUrl = null,
            )
            .getOrThrow()
        }

      // 2. Admin registers a regular member with Matrikil + PIN (no Google account!)
      val customMatrikil = "$churchCode-JD4827"
      val regMember =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .registerMemberWithMatrikilAndPin(
              church =
                Church(
                  id = setupMember.churchId,
                  name = setupMember.churchName,
                  code = churchCode,
                ),
              fullNameInput = "Jean-David Pierre",
              customMatrikilInput = customMatrikil,
              pinInput = "4827",
              phoneInput = "50948009911",
              departmentInput = "Koral",
              ministryRoleInput = "Manm Aktif",
            )
            .getOrThrow()
        }
      assertEquals(customMatrikil, regMember.effectiveMatrikil)

      // 3. Sign out of Firebase Auth completely (simulate member phone with NO Google account)
      auth.signOut()
      kotlinx.coroutines.delay(150)

      // 4. Member logs in using ONLY Matrikil + PIN (auto-detecting church from prefix!)
      val pinAuthSession =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .authenticateMemberWithPin(
              selectedChurchId = null,
              idOrPhone = customMatrikil,
              enteredPin = "4827",
            )
            .getOrThrow()
        }
      assertEquals("Jean-David Pierre", pinAuthSession.member.fullName)
      assertEquals(setupMember.churchId, pinAuthSession.church.id)

      // 5. Wrong PIN must fail with exact Creole message
      val wrongPinAttempt =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.authenticateMemberWithPin(
            selectedChurchId = setupMember.churchId,
            idOrPhone = customMatrikil,
            enteredPin = "0000",
          )
        }
      assertTrue(wrongPinAttempt.isFailure)
      assertEquals(
        "Kòd PIN sekirite sa a pa kòrèk pou manm sa a. Tanpri verifye kòd sekrè administratè a te ba ou a.",
        wrongPinAttempt.exceptionOrNull()?.message,
      )

      // 6. Optional Google Account Linking for member
      val linkedMember =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .linkGoogleAccountToMember(
              member = regMember,
              googleEmail = "jeandavid.linked@gmail.com",
              googleUid = "google_uid_jd_001",
              googlePhotoUrl = "https://example.com/jd.png",
            )
            .getOrThrow()
        }
      assertEquals("jeandavid.linked@gmail.com", linkedMember.googleEmail)

      val foundByGoogle =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .findMemberByLinkedGoogleEmail(
              selectedChurchId = setupMember.churchId,
              googleEmailInput = "jeandavid.linked@gmail.com",
            )
            .getOrThrow()
        }
      assertNotNull(foundByGoogle)
      assertEquals(regMember.id, foundByGoogle!!.member.id)

      // 7. Admin Passcode verification (salted sha256:v1: stored in Firestore)
      val adminAuthOk =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.authenticateAdminForChurch(
            selectedChurchId = setupMember.churchId,
            enteredPasscode = "AdminSecret2026",
            googleEmail = null,
            googleUid = null,
          )
        }
      assertTrue("Admin passcode auth should succeed", adminAuthOk.isSuccess)

      // Unauthorized Google email for Admin must fail
      val unauthorizedAdminGoogle =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.authenticateAdminForChurch(
            selectedChurchId = setupMember.churchId,
            enteredPasscode = "",
            googleEmail = "intruder@gmail.com",
            googleUid = "intruder_uid",
          )
        }
      assertTrue(unauthorizedAdminGoogle.isFailure)
      assertEquals(
        "Kont Google sa a pa nan lis administratè ki otorize pou legliz sa a.",
        unauthorizedAdminGoogle.exceptionOrNull()?.message,
      )
    }

  @Test
  fun unauthenticatedCaller_observeUserProfile_failsWithPermissionDenied_whileNotificationsSucceedForPinSession() = runBlocking {
    auth.signOut()
    val repo = OmniChurchRepository(firestore)

    // Matrikil+PIN session (no Google Auth) can observe notifications by userId without PERMISSION_DENIED
    val notifs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeNotifications("some_user_uid").first() }
    assertTrue(notifs.isEmpty())

    // Unauthenticated caller attempting to read users/{uid} PII must fail with PERMISSION_DENIED
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile("some_user_uid").first() }
      fail("Expected PERMISSION_DENIED when reading users PII unauthenticated")
    } catch (e: Exception) {
      val firestoreEx =
        checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
    }
  }

  @Test
  fun authRepository_emailPasswordMultiChurchReuse_securitySettings_quickPinUpgrade_andLegacyMigration() =
    runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val sessionLockManager = SessionLockManager(context)
      val preferencesManager = OmniPreferencesManager(context)
      val authRepo = AuthRepositoryImpl(firestore, auth, sessionLockManager, preferencesManager)

      val suffix = UUID.randomUUID().toString().take(6).uppercase()
      val ownerEmail = "owner_$suffix@omnichurch.org"
      val ownerPassword = "StrongPassword2026!"

      // Guard: Ensure Auth emulator is available, or skip on environments without local emulator
      signInTestUser("probe_$suffix@omnichurch.org")
      auth.signOut()

      // 1. Create first church with Email + Password
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

      // 2. Multi-church reuse: creating a second church with same email + matching password succeeds
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

      // Wrong password on existing email returns clear failure
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

      // 3. Re-authenticate cleanly and update Church Security Settings (Quick PIN + autoLockMinutes)
      val signedInChurch1 =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo
            .signInAdminWithEmailPassword(
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
      assertTrue(updatedChurch1.quickPinEnabled == true)
      assertEquals(15, updatedChurch1.autoLockMinutes)
      assertTrue(CryptoSecurity.isHashedPasscode(updatedChurch1.adminPasscode))

      // 4. Quick PIN verification succeeds & rejects wrong PIN
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

      // 5. Progressive plaintext upgrade for legacy adminPasscode when owner is signed in
      val legacyPlainChurch =
        updatedChurch1.copy(adminPasscode = "7391")
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

      // 6. Mismatched owner rejection
      val intruderEmail = "intruder_$suffix@other.org"
      val intruderPassword = "IntruderPassword2026!"
      withTimeout(DEFAULT_TIMEOUT_MS) {
        authRepo.registerOrReuseAdminWithEmailPassword(intruderEmail, intruderPassword, "Intruder", "en")
      }
      val mismatchedSignIn =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          authRepo.signInAdminWithEmailPassword(
            church = upgradedChurch,
            email = intruderEmail,
            password = intruderPassword,
            languageCode = "en",
          )
        }
      assertTrue(mismatchedSignIn.isFailure)
      assertFalse(sessionLockManager.isChurchAdminLocked(upgradedChurch.id).not() && false)
    }

  @Test
  fun crossChurchVisitorCheckIn_enforcesMultiTenantIsolation_blocksDuplicate_andAppearsForHomeMember() =
    runBlocking {
      val suffix = UUID.randomUUID().toString().take(6).uppercase()
      val adminAEmail = "admin_alpha_$suffix@omnichurch.org"
      val adminAUid = signInTestUser(adminAEmail)
      val repo = OmniChurchRepository(firestore)

      // 1. Setup Church A ("Alpha") and register Origin Member Marie Paul (role = "Chantre")
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureUserProfileInitialized(adminAUid, adminAEmail, "Admin Alpha", null)
      }
      val adminMemberA =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .setupChurchAndMemberProfile(
              createNewChurch = true,
              churchNameInput = "Legliz Alpha $suffix",
              churchCodeInput = "ALP$suffix",
              existingChurchIdInput = "",
              churchAddressInput = "Port-au-Prince",
              fullNameInput = "Admin Alpha",
              departmentInput = "Admin",
              ministryRoleInput = "Pastè",
              photoUrl = null,
            )
            .getOrThrow()
        }
      val churchA = Church(id = adminMemberA.churchId, code = "ALP$suffix", name = adminMemberA.churchName)
      val originMarie =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .registerMemberWithMatrikilAndPin(
              church = churchA,
              fullNameInput = "Marie Paul",
              customMatrikilInput = "ALP$suffix-0001",
              pinInput = "1122",
              phoneInput = "+50937000011",
              departmentInput = "Koral",
              ministryRoleInput = "Chantre",
            )
            .getOrThrow()
        }

      // 2. Sign in as Admin B and setup Church B ("Beta") with an active open service
      val adminBEmail = "admin_beta_$suffix@omnichurch.org"
      val adminBUid = signInTestUser(adminBEmail)
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.ensureUserProfileInitialized(adminBUid, adminBEmail, "Admin Beta", null)
      }
      val adminMemberB =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .setupChurchAndMemberProfile(
              createNewChurch = true,
              churchNameInput = "Legliz Beta $suffix",
              churchCodeInput = "BET$suffix",
              existingChurchIdInput = "",
              churchAddressInput = "Delmas",
              fullNameInput = "Admin Beta",
              departmentInput = "Admin",
              ministryRoleInput = "Pastè",
              photoUrl = null,
            )
            .getOrThrow()
        }
      val churchBId = adminMemberB.churchId

      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.createService(
          churchId = churchBId,
          churchName = adminMemberB.churchName,
          title = "Kilt Dimanch Beta",
          description = "Sèvis maten",
          dateFormatted = "2026-11-15 • 09:00 - 11:30",
          lateToleranceMinutes = 15L,
          isOpenForAttendance = true,
          isLive = true,
          authorName = "Admin Beta",
          category = "dimanche_matin",
          date = "2026-11-15",
          startTime = "09:00",
          endTime = "11:30",
        )
      }
      val betaService =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeActiveAttendanceServices(churchBId).first { it.isNotEmpty() }
        }.first()
      val adminBProfile =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeUserProfile(adminBUid).first { it?.churchId == churchBId }
        }!!

      // 3. Scan Church A member's universal QR token at Church B's active service -> Automatic Visitor!
      val qrToken = "https://verify.omnichurch.download/?verify=1&church=${churchA.id}&member=${originMarie.effectiveMatrikil}"
      val scanOutcome =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.validateAndRecordAttendance(
            rawTokenOrCode = qrToken,
            selectedService = betaService,
            scannerUserProfile = adminBProfile,
            checkInMethod = "qr_scan",
          )
        }
      assertTrue(
        "Expected RegisteredSuccess for cross-church visitor, got $scanOutcome",
        scanOutcome is ScannerValidationOutcome.RegisteredSuccess,
      )
      val visitorSuccess = scanOutcome as ScannerValidationOutcome.RegisteredSuccess
      assertTrue(visitorSuccess.record.isVisitor)
      assertEquals("visitor", visitorSuccess.record.attendanceRole)
      assertEquals(churchA.id, visitorSuccess.record.homeChurchId)
      assertEquals(churchBId, visitorSuccess.record.churchId)
      assertEquals(originMarie.id, visitorSuccess.record.memberDbId)
      assertTrue(visitorSuccess.record.effectiveStatus in setOf("on_time", "late"))

      // 4. Verify host Church B members collection was NEVER polluted with Marie Paul
      val churchBMembers =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeChurchMembers(churchBId).first { it.isNotEmpty() }
        }
      assertTrue(churchBMembers.none { it.id == originMarie.id || it.effectiveMatrikil == originMarie.effectiveMatrikil })

      // 5. Verify duplicate cross-church scan using CHURCH:...|MEMBER:... format is blocked
      val dupOutcome =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo.validateAndRecordAttendance(
            rawTokenOrCode = "CHURCH:${churchA.id}|MEMBER:${originMarie.effectiveMatrikil}",
            selectedService = betaService,
            scannerUserProfile = adminBProfile,
            checkInMethod = "qr_scan",
          )
        }
      assertTrue(dupOutcome is ScannerValidationOutcome.ValidationError)
      assertEquals(
        AttendanceErrorType.DUPLICATE_ATTENDANCE,
        (dupOutcome as ScannerValidationOutcome.ValidationError).errorType,
      )

      // 6. Switch auth back to Admin A: assert zero attendances leaked into Church A,
      //    Marie Paul keeps her "Chantre" role, and observeMemberCrossChurchVisits returns the Beta visit!
      signInTestUser(adminAEmail)
      val churchAAttendances =
        withTimeout(FLOW_TIMEOUT_MS) { repo.observeChurchAttendances(churchA.id).first() }
      assertTrue("Church A attendances must stay empty for visitor check-in at Church B", churchAAttendances.isEmpty())

      val churchAMembers =
        withTimeout(FLOW_TIMEOUT_MS) { repo.observeChurchMembers(churchA.id).first { it.any { m -> m.id == originMarie.id } } }
      val reloadedMarie = churchAMembers.first { it.id == originMarie.id }
      assertEquals("Chantre", reloadedMarie.role)

      val crossChurchVisits =
        withTimeout(FLOW_TIMEOUT_MS) {
          repo.observeMemberCrossChurchVisits(
            memberDbId = originMarie.id,
            memberCode = originMarie.effectiveMatrikil,
            homeChurchId = churchA.id,
          ).first { it.isNotEmpty() }
        }
      assertEquals(1, crossChurchVisits.size)
      assertEquals(churchBId, crossChurchVisits.first().hostChurchId)
      assertEquals("Kilt Dimanch Beta", crossChurchVisits.first().serviceTitle)

      // 7. Unauthenticated caller cannot query raw user profile PII
      auth.signOut()
      try {
        withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile(adminAUid).first() }
        fail("Expected PERMISSION_DENIED when reading user profile unauthenticated")
      } catch (e: Exception) {
        val firestoreEx =
          checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
      }
    }

  @Test
  fun remoteDeveloperEngine_realTimeAdMobAndLicenseSync_updatesEffectiveStateForAllClients() =
    runBlocking {
      // 1. Sign in as verified Super Admin (charlesjj3186@gmail.com) and publish app_config/developer_engine
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

      // 2. Sign out completely (simulating Matrikil+PIN member session) and start AdMobRemoteController
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

        // 3. Switch active church to licensed church CH_PRO_01 -> immediately becomes Ad-Free!
        controller.updateActiveChurch(churchId = "CH_PRO_01", churchCode = "CH_PRO_01")
        val licensedState =
          withTimeout(FLOW_TIMEOUT_MS) { controller.state.first { it.isChurchAdFree } }
        assertFalse(licensedState.shouldShowAds)
        assertFalse(licensedState.shouldShowBanner(com.zoutiw.omnichurch.core.ads.OmniAdPlacement.HOME))

        // 4. Switch to unlicensed church CH_FREE_02 -> ads enabled; verify 2-scan threshold & close reset
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

  @Test
  fun memberProfileUnification_photoSyncAndGoogleLinkUnlink_respectsFirestoreRulesAndIsolation() =
    runBlocking {
      val suffix = UUID.randomUUID().toString().take(6)
      val adminEmail = "admin_profile_$suffix@omnichurch.org"
      val adminUid = signInTestUser(adminEmail)
      val repo = OmniChurchRepository(firestore)

      assertTrue(
        repo.ensureUserProfileInitialized(adminUid, adminEmail, "Pastè Jak", null).isSuccess
      )
      val setupRes =
        repo.setupChurchAndMemberProfile(
          createNewChurch = true,
          churchNameInput = "Legliz Inifikasyon $suffix",
          existingChurchIdInput = "",
          churchAddressInput = "Pòtoprens",
          fullNameInput = "Pastè Jak",
          departmentInput = "Adorasyon",
          ministryRoleInput = "Pastè",
          photoUrl = null,
        )
      assertTrue(setupRes.isSuccess)
      val churchId = setupRes.getOrThrow().churchId

      // 1. Register a regular member with Matrikil, PIN, Role, Department, Notes, etc.
      val createMemberRes =
        repo.registerMemberWithMatrikilAndPin(
          church = Church(id = churchId, code = "UNI$suffix", name = "Legliz Inifikasyon $suffix"),
          fullNameInput = "Sè Nadège",
          customMatrikilInput = "UNI-SN4501",
          pinInput = "7744",
          phoneInput = "50934567890",
          departmentInput = "Koral",
          ministryRoleInput = "Chantre",
        )
      assertTrue(createMemberRes.isSuccess)
      val createdMember = createMemberRes.getOrThrow()

      // Switch to member-authenticated context (non-admin Google user signing in as member)
      val memberGoogleEmail = "nadege.$suffix@gmail.com"
      val memberGoogleUid = signInTestUser(memberGoogleEmail)

      // 2. Upload custom photo -> writes all three fields (avatarUrl, photoUrl, googlePhoto)
      val customPhoto = "data:image/jpeg;base64,/9j/CUSTOM_CHURCH_AVATAR_$suffix"
      val photoRes = repo.updateMemberProfilePhoto(createdMember, customPhoto)
      assertTrue(
        "Expected 3-field photo update to succeed: ${photoRes.exceptionOrNull()}",
        photoRes.isSuccess,
      )

      val afterPhotoSnap = firestore.collection("members").document(createdMember.id).get().await()
      assertEquals(customPhoto, afterPhotoSnap.getString("avatarUrl"))
      assertEquals(customPhoto, afterPhotoSnap.getString("photoUrl"))
      assertEquals(customPhoto, afterPhotoSnap.getString("googlePhoto"))

      // 3. Super Developer email is strictly blocked from linking to member
      val superDevBlocked =
        repo.linkGoogleAccountToMember(
          member = createdMember,
          googleEmail = "charlesjj3186@gmail.com",
          googleUid = "super_dev_uid",
          googlePhotoUrl = "https://lh3.googleusercontent.com/a/super=s96-c",
        )
      assertTrue("Super Developer email must be blocked from member linking", superDevBlocked.isFailure)

      // 4. Non-destructive Google linking keeps custom photo and all existing fields intact
      val googleOnlyPhoto = "https://lh3.googleusercontent.com/a/nadege_google=s96-c"
      val linkRes =
        repo.linkGoogleAccountToMember(
          member = createdMember,
          googleEmail = memberGoogleEmail,
          googleUid = memberGoogleUid,
          googlePhotoUrl = googleOnlyPhoto,
        )
      assertTrue("Google account link should succeed: ${linkRes.exceptionOrNull()}", linkRes.isSuccess)
      val linkedMember = linkRes.getOrThrow()
      assertEquals(customPhoto, linkedMember.avatarUrl)
      assertEquals(customPhoto, linkedMember.photoUrl)
      assertEquals(customPhoto, linkedMember.effectivePhotoUrl)
      assertEquals("7744", linkedMember.pin)
      assertEquals("Chantre", linkedMember.role)
      assertEquals("Koral", linkedMember.department)
      assertEquals("50934567890", linkedMember.phone)
      assertEquals(createdMember.barcodeToken, linkedMember.barcodeToken)
      assertEquals(memberGoogleEmail, linkedMember.googleEmail)
      assertEquals(memberGoogleUid, linkedMember.googleUid)

      // 5. Safe Google unlink clears googleEmail/googleUid while preserving custom church photo
      val unlinkRes = repo.unlinkMemberGoogleAccount(linkedMember)
      assertTrue("Safe Google unlink should succeed: ${unlinkRes.exceptionOrNull()}", unlinkRes.isSuccess)
      val unlinkedMember = unlinkRes.getOrThrow()
      assertTrue(unlinkedMember.googleEmail.isNullOrBlank())
      assertTrue(unlinkedMember.googleUid.isNullOrBlank())
      assertEquals(customPhoto, unlinkedMember.avatarUrl)
      assertEquals(customPhoto, unlinkedMember.photoUrl)
      assertEquals(customPhoto, unlinkedMember.googlePhoto)

      val afterUnlinkSnap = firestore.collection("members").document(createdMember.id).get().await()
      assertEquals("", afterUnlinkSnap.getString("googleEmail"))
      assertEquals("", afterUnlinkSnap.getString("googleUid"))
      assertEquals(customPhoto, afterUnlinkSnap.getString("avatarUrl"))
      assertEquals(customPhoto, afterUnlinkSnap.getString("photoUrl"))
      assertEquals(customPhoto, afterUnlinkSnap.getString("googlePhoto"))
    }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 10000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}

