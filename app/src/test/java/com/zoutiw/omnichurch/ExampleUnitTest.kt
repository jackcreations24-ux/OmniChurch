package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.ads.RemoteChurchLicense
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.data.models.computeDeterministicVerificationSeal
import com.zoutiw.omnichurch.data.models.maskLegalDocumentNumber
import com.zoutiw.omnichurch.presentation.components.toDrawableRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  private val sampleChurches =
    listOf(
      Church(
        id = "church_ch001",
        name = "Legliz Tabènak",
        code = "CH001",
        ownerId = "owner_1",
        adminGoogleEmail = "pastor@tabenak.org",
        adminPasscode =
          OmniAuthEvaluator.computeSaltedAdminPasscodeDigest(
            salt = "church_ch001",
            enteredPasscode = "SecretAdmin2026",
          ),
      ),
      Church(
        id = "church_etg01",
        name = "Eglise Evangelique",
        code = "ETG01",
        ownerId = "owner_2",
        adminGoogleEmail = "admin@etg.org",
        adminPasscode =
          OmniAuthEvaluator.computeSaltedAdminPasscodeDigest(
            salt = "ETG01",
            enteredPasscode = "EtgPass99",
          ),
      ),
    )

  private val sampleMembers =
    listOf(
      Member(
        id = "doc_jd_01",
        churchId = "church_ch001",
        churchName = "Legliz Tabènak",
        fullName = "Jean-David Pierre",
        memberId = "CH001-JD4827",
        memberCode = "CH001-JD4827",
        phone = "+509 3700-1234",
        email = "jd@tabenak.org",
        pin = "9055",
        barcodeToken = "OMNICHURCH_church_ch001_JD4827",
      ),
      Member(
        id = "doc_mp_02",
        churchId = "ETG01",
        churchName = "Eglise Evangelique",
        fullName = "Marie-Pauline Joseph",
        memberId = "ETG01-MP9201",
        memberCode = "ETG01-MP9201",
        phone = "50948112233",
        pin = null,
        securityPin = null,
        barcodeToken = "OMNICHURCH_church_etg01_MP9201",
      ),
    )

  @Test
  fun `user profile role helpers validate permanent admin scanner vs temporary usher and member roles`() {
    val adminProfile = UserProfile(id = "u1", email = "admin@church.org", role = "admin")
    val usherProfile = UserProfile(id = "u2", email = "usher@church.org", role = "usher")
    val memberProfile = UserProfile(id = "u3", email = "member@church.org", role = "member")

    assertTrue(adminProfile.isAuthorizedScanner)
    assertTrue(adminProfile.isAdminOrSuperAdmin)
    assertTrue(usherProfile.isAuthorizedScanner)
    assertFalse(usherProfile.isAdminOrSuperAdmin)
    assertFalse(memberProfile.isAuthorizedScanner)
    assertFalse(memberProfile.isAdminOrSuperAdmin)
  }

  @Test
  fun `usher role detection handles Creole French and English labels accurately`() {
    val usherMember = Member(id = "m_akey", fullName = "Claire Joseph", role = "Akèy")
    val accueilMember = Member(id = "m_accueil", fullName = "Marc Louis", role = "Accueil")
    val regularMember = Member(id = "m_manm", fullName = "Luc Pierre", role = "Manm")

    assertTrue(usherMember.isUsherRole)
    assertTrue(accueilMember.isUsherRole)
    assertFalse(regularMember.isUsherRole)
  }

  @Test
  fun `home feed excludes ended services while preserving active and upcoming services`() {
    val nowMs = 1_760_000_000_000L
    val activeService =
      ChurchService(
        id = "srv_live",
        churchId = "church_1",
        title = "Sèvis Maten",
        status = "active",
        isOpenForAttendance = true,
        startTimeEpochMillis = nowMs - 30 * 60_000L,
      )
    val endedByFlag = activeService.copy(id = "srv_done_flag", isPermanentlyEnded = true, isOpenForAttendance = false)
    val endedByStatus = activeService.copy(id = "srv_done_status", status = "ended", isOpenForAttendance = false)
    val futureService =
      ChurchService(
        id = "srv_future",
        churchId = "church_1",
        title = "Sèvis Pwochenn",
        status = "scheduled",
        isOpenForAttendance = false,
        startTimeEpochMillis = nowMs + 24 * 3_600_000L,
      )

    val allServices = listOf(activeService, endedByFlag, endedByStatus, futureService)
    val homeFeedServices = allServices.filter { !it.isServiceEnded(nowMs) }

    assertEquals(listOf("srv_live", "srv_future"), homeFeedServices.map { it.id })
    assertEquals(4, allServices.size)
  }

  @Test
  fun `punctuality calculation marks on_time within tolerance and late after tolerance`() {
    val serviceStartMillis = 1_700_000_000_000L
    val service =
      ChurchService(
        id = "srv_1",
        churchId = "church_1",
        title = "Sèvis Dimanch",
        startTimeEpochMillis = serviceStartMillis,
        lateToleranceMinutes = 15L,
        isOpenForAttendance = true,
      )

    val toleranceDeadline = service.startTimeEpochMillis + service.lateToleranceMinutes * 60_000L
    val arrivalOnTime = serviceStartMillis + 10 * 60_000L
    val arrivalLate = serviceStartMillis + 20 * 60_000L

    assertEquals("on_time", if (arrivalOnTime <= toleranceDeadline) "on_time" else "late")
    assertEquals("late", if (arrivalLate <= toleranceDeadline) "on_time" else "late")
  }

  @Test
  fun `member login with Matrikil and dedicated PIN succeeds without Google account`() {
    val church = sampleChurches[0]
    val result =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "ch001-jd4827",
        enteredPin = "9055",
      )
    assertTrue(result.isSuccess)
    val session = result.getOrThrow()
    assertEquals("CH001-JD4827", session.member.effectiveMatrikil)
    assertEquals("Jean-David Pierre", session.member.fullName)
  }

  @Test
  fun `auto-detect church from Matrikil prefix and verify legacy fallback PIN from last 4 chars`() {
    val resolvedChurch =
      OmniAuthEvaluator.resolveChurchForMemberLogin(
        churches = sampleChurches,
        selectedChurchId = null,
        idOrPhoneInput = "ETG01-MP9201",
      )
    assertNotNull(resolvedChurch)
    assertEquals("church_etg01", resolvedChurch!!.id)

    val result =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = resolvedChurch,
        churchMembers = listOf(sampleMembers[1]),
        idOrPhone = "ETG01-MP9201",
        enteredPin = "9201",
      )
    assertTrue(result.isSuccess)
    assertEquals("Marie-Pauline Joseph", result.getOrThrow().member.fullName)
  }

  @Test
  fun `member login by phone number and wrong PIN or empty fields return exact Creole errors`() {
    val church = sampleChurches[0]
    val phoneResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "50937001234",
        enteredPin = "9055",
      )
    assertTrue(phoneResult.isSuccess)

    val wrongPinResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "CH001-JD4827",
        enteredPin = "0000",
      )
    assertTrue(wrongPinResult.isFailure)
    assertEquals(
      OmniAuthEvaluator.ERROR_WRONG_PIN,
      (wrongPinResult.exceptionOrNull() as OmniAuthException).message,
    )

    val emptyIdResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "   ",
        enteredPin = "9055",
      )
    assertEquals(
      OmniAuthEvaluator.ERROR_EMPTY_ID,
      (emptyIdResult.exceptionOrNull() as OmniAuthException).message,
    )
  }

  @Test
  fun `authenticateMemberAutoResolving detects member own church without selecting church list`() {
    val autoById =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = sampleMembers,
        idOrPhone = "ETG01-MP9201",
        enteredPin = "9201",
        selectedChurchId = null,
      )
    assertTrue(autoById.isSuccess)
    assertEquals("church_etg01", autoById.getOrThrow().first().church.id)

    val autoByPhone =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = sampleMembers,
        idOrPhone = "50937001234",
        enteredPin = "9055",
        selectedChurchId = null,
      )
    assertTrue(autoByPhone.isSuccess)
    assertEquals("church_ch001", autoByPhone.getOrThrow().first().church.id)

    val sharedPhoneOtherChurch =
      sampleMembers[1].copy(
        id = "doc_shared",
        churchId = "church_etg01",
        phone = "50937001234",
        pin = "9055",
      )
    val multiMatch =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = listOf(sampleMembers[0], sharedPhoneOtherChurch),
        idOrPhone = "50937001234",
        enteredPin = "9055",
        selectedChurchId = null,
      )
    assertTrue(multiMatch.isSuccess)
    assertEquals(2, multiMatch.getOrThrow().size)
  }

  @Test
  fun `salted SHA-256 admin passcode verification and admin Google email authorization work accurately`() {
    val churchA = sampleChurches[0]
    val churchB = sampleChurches[1]

    assertTrue(OmniAuthEvaluator.verifyAdminPasscode(churchA, "SecretAdmin2026"))
    assertFalse(OmniAuthEvaluator.verifyAdminPasscode(churchA, "WrongPass"))
    assertTrue(OmniAuthEvaluator.verifyAdminPasscode(churchB, "EtgPass99"))
    assertFalse(OmniAuthEvaluator.verifyAdminPasscode(churchB, "BadPass"))

    assertTrue(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "pastor@tabenak.org"))
    assertTrue(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "charlesjj3186@gmail.com"))
    assertFalse(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "stranger@gmail.com"))
    assertTrue(CryptoSecurity.isSuperDeveloperAccount("  CharlesJJ3186@Gmail.com "))
    assertFalse(CryptoSecurity.isSuperDeveloperAccount("member@omnichurch.org"))
  }

  @Test
  fun `OmniQrParser correctly decodes universal verification URL internal token and raw member ID`() {
    val url = "https://verify.omnichurch.download/?verify=1&church=CH001&member=MB-7842"
    val resUrl = OmniQrParser.parseQrToken(url)
    assertEquals("CH001", resUrl.churchId)
    assertEquals("MB-7842", resUrl.memberId)

    val token = "OMNICHURCH|CHURCH:ETG01|MEMBER:MEM-001"
    val resToken = OmniQrParser.parseQrToken(token)
    assertEquals("ETG01", resToken.churchId)
    assertEquals("MEM-001", resToken.memberId)

    val raw = "MB-9999"
    val resRaw = OmniQrParser.parseQrToken(raw)
    assertNull(resRaw.churchId)
    assertEquals("MB-9999", resRaw.memberId)
  }

  @Test
  fun `AttendanceRecord visitor flags and member verificationUrl resolve accurately`() {
    val member =
      Member(
        id = "m1",
        churchId = "CH001",
        memberId = "MB-1234",
        fullName = "Jackson Charles",
        titlePrefix = "Pastè",
        avatarUrl = "data:image/jpeg;base64,samplephoto",
        photoUrl = "https://example.com/old.jpg",
      )
    assertEquals(
      "https://verify.omnichurch.download/?verify=1&church=CH001&member=MB-1234",
      member.verificationUrl,
    )
    assertEquals("data:image/jpeg;base64,samplephoto", member.effectivePhotoUrl)

    val visitorRec =
      AttendanceRecord(
        id = "srv_1_mem_1",
        churchId = "church_b",
        serviceId = "srv_1",
        serviceName = "Sèvis Dimanch",
        memberId = "ALP-0001",
        memberDocId = "mem_1",
        status = "late",
        attendanceRole = "visitor",
        isVisitor = true,
        homeChurchId = "church_a",
        homeChurchName = "Legliz Alpha",
      )
    assertTrue(visitorRec.isCrossChurchVisitor)
    assertEquals("late", visitorRec.effectiveStatus)
    assertEquals("mem_1", visitorRec.effectiveMemberDbId)
  }

  @Test
  fun `seven 3D reactions map accurately to keys and independent drawable resources`() {
    val allReactions = OmniReactionType.entries
    assertEquals(7, allReactions.size)
    assertEquals(listOf("like", "love", "pray", "haha", "wow", "sad", "clap"), allReactions.map { it.key })
    assertEquals(OmniReactionType.PRAY, OmniReactionType.fromKey("amen"))
    assertEquals(OmniReactionType.CLAP, OmniReactionType.fromKey("CLAP"))
    assertEquals(7, allReactions.map { it.toDrawableRes() }.toSet().size)
  }

  @Test
  fun `remoteVerificationConfig licenses documentMasking and seal match spec`() {
    val cfg =
      RemoteMemberVerificationConfig.fromMap(
        mapOf(
          "enabled" to true,
          "disabledChurchIds" to listOf("CH_DISABLED", "bad_code"),
          "enforceActiveStatusCheck" to true,
          "showMemberPhoto" to true,
          "showLegalDocuments" to true,
          "maskLegalDocumentNumber" to true,
          "showChurchContactInfo" to false,
          "showChurchRncBadge" to true,
          "allowPrintAndShare" to true,
          "customAuthorityNotice" to "Valid pou ane 2026",
          "verificationBaseUrl" to "http://127.0.0.1:4000",
        )
      )
    assertTrue(cfg.isEnabledForChurch("CH001", "CH001"))
    assertFalse(cfg.isEnabledForChurch("CH_DISABLED", null))
    assertFalse(cfg.isEnabledForChurch("other", "BAD_CODE"))
    assertEquals("https://verify.omnichurch.download", cfg.effectiveVerificationBaseUrl)
    assertEquals("Valid pou ane 2026", cfg.customAuthorityNotice)

    val activeLicense =
      RemoteChurchLicense(
        key = "LIC-01",
        churchId = "CH001",
        status = "active",
        expiresAt = "never",
        allowAdFree = false,
      )
    assertTrue(activeLicense.isActiveOfficialLicenseFor("CH001"))
    assertFalse(activeLicense.isActiveAndAdFreeFor("CH001"))

    val expiredLicense =
      RemoteChurchLicense(
        key = "LIC-02",
        churchId = "CH001",
        status = "active",
        expiresAt = "2020-01-01",
        allowAdFree = true,
      )
    assertFalse(expiredLicense.isActiveOfficialLicenseFor("CH001"))

    assertEquals("••••••••4567", maskLegalDocumentNumber("001-123-4567", shouldMask = true))
    assertEquals("001-123-4567", maskLegalDocumentNumber("001-123-4567", shouldMask = false))
    assertEquals("1234", maskLegalDocumentNumber("1234", shouldMask = true))

    val seal1 = computeDeterministicVerificationSeal("ch001", "doc_1", "evg-0042", true)
    val seal2 = computeDeterministicVerificationSeal("ch001", "doc_1", "evg-0042", true)
    assertEquals(seal1, seal2)
    assertTrue(seal1.startsWith("OC-"))
    assertEquals(
      "https://verify.omnichurch.download/?verify=1&church=CH001&member=EVG-0042",
      buildCanonicalQrPayload("CH001", "EVG-0042", "https://verify.omnichurch.download/"),
    )
  }
}
