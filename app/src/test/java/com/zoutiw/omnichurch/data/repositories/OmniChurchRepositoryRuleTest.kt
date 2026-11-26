package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FirebaseFirestoreException
import com.zoutiw.omnichurch.base.FirestoreEmulatorTestBase
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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

    val outsiderEmail = "outsider_b_$uniqueSuffix@other.org"
    val outsiderUid = signInTestUser(outsiderEmail)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      repo.ensureUserProfileInitialized(outsiderUid, outsiderEmail, "Outsider B", null)
    }

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

      val customMatrikil = "$churchCode-JD4827"
      val regMember =
        withTimeout(DEFAULT_TIMEOUT_MS) {
          repo
            .registerMemberWithMatrikilAndPin(
              church = Church(id = setupMember.churchId, name = setupMember.churchName, code = churchCode),
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

      auth.signOut()
      kotlinx.coroutines.delay(150)

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
  fun unauthenticatedCaller_observeUserProfile_failsWithPermissionDenied_whileNotificationsSucceedForPinSession() =
    runBlocking {
      signInTestUser("probe_unauth_${UUID.randomUUID().toString().take(6)}@omnichurch.org")
      auth.signOut()
      val repo = OmniChurchRepository(firestore)

      val notifs = withTimeout(FLOW_TIMEOUT_MS) { repo.observeNotifications("some_user_uid").first() }
      assertTrue(notifs.isEmpty())

      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          firestore.collection("users").document("some_user_uid").get().await()
        }
        fail("Expected PERMISSION_DENIED when reading users PII unauthenticated")
      } catch (e: Exception) {
        val firestoreEx =
          checkNotNull(e.unwrapFirestoreException()) { "Expected FirebaseFirestoreException, got $e" }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx.code)
      }
    }

  @Test
  fun memberProfilePhotoUpdate_writesCanonicalPhotoFields() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val adminEmail = "admin_profile_$suffix@omnichurch.org"
    val adminUid = signInTestUser(adminEmail)
    val repo = OmniChurchRepository(firestore)

    assertTrue(repo.ensureUserProfileInitialized(adminUid, adminEmail, "Pastè Jak", null).isSuccess)
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

    val customPhoto = "data:image/jpeg;base64,/9j/CUSTOM_CHURCH_AVATAR_$suffix"
    val photoRes = repo.updateMemberProfilePhoto(createdMember, customPhoto)
    assertTrue("Expected photo update to succeed: ${photoRes.exceptionOrNull()}", photoRes.isSuccess)

    val afterPhotoSnap = firestore.collection("members").document(createdMember.id).get().await()
    assertEquals(customPhoto, afterPhotoSnap.getString("avatarUrl"))
    assertEquals(customPhoto, afterPhotoSnap.getString("photoUrl"))
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 10000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
