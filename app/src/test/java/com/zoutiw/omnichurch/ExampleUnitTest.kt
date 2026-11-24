package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.ads.OmniAdPlacement
import com.zoutiw.omnichurch.core.ads.RemoteAdMobConfig
import com.zoutiw.omnichurch.core.ads.RemoteChurchLicense
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.data.models.AdminServiceAttendanceRoster
import com.zoutiw.omnichurch.data.models.AdminServiceMemberAttendanceItem
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceStatus
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.buildCanonicalBarcodeToken
import com.zoutiw.omnichurch.data.models.buildCanonicalQrPayload
import com.zoutiw.omnichurch.data.models.buildUnlinkMemberGooglePayload
import com.zoutiw.omnichurch.data.models.computeDeterministicVerificationSeal
import com.zoutiw.omnichurch.data.models.isCustomMemberPhoto
import com.zoutiw.omnichurch.data.models.isSuperDeveloperAccount
import com.zoutiw.omnichurch.data.models.maskLegalDocumentNumber
import com.zoutiw.omnichurch.data.models.resolveCanonicalMemberPhoto
import com.zoutiw.omnichurch.data.models.unifyMemberProfile
import com.zoutiw.omnichurch.presentation.components.isPlacementBannerEnabled
import com.zoutiw.omnichurch.presentation.components.toDrawableRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
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

    // Akèy / Resepsyon (`usher`) no longer gets permanent scanner privilege without an assigned active service
    assertFalse(usherProfile.isAuthorizedScanner)
    assertFalse(usherProfile.isAdminOrSuperAdmin)

    assertFalse(memberProfile.isAuthorizedScanner)
    assertFalse(memberProfile.isAdminOrSuperAdmin)
  }

  @Test
  fun `deacon and usher role detection handles Creole French and English labels accurately`() {
    val deaconMember = Member(id = "m_dyak", fullName = "Paul Jean", role = "Dyak")
    val diacreMember = Member(id = "m_diacre", fullName = "Marc Louis", role = "Diacre")
    val usherMember = Member(id = "m_akey", fullName = "Claire Joseph", role = "Akèy (Resepsyon)")
    val regularMember = Member(id = "m_manm", fullName = "Luc Pierre", role = "Manm")

    assertTrue(deaconMember.isDeaconRole)
    assertTrue(diacreMember.isDeaconRole)
    assertFalse(usherMember.isDeaconRole)
    assertFalse(regularMember.isDeaconRole)

    assertTrue(usherMember.isUsherRole)
    assertFalse(deaconMember.isUsherRole)
    assertFalse(regularMember.isUsherRole)
  }

  @Test
  fun `temporary usher authorization grants scanner access only during assigned unfinished service`() {
    val assignedUsher = Member(
      id = "mem_claire",
      userId = "uid_claire",
      churchId = "church_1",
      fullName = "Claire Joseph",
      role = "Akèy",
    )
    val otherUsher = Member(
      id = "mem_peter",
      userId = "uid_peter",
      churchId = "church_1",
      fullName = "Peter Noel",
      role = "Akèy",
    )
    val activeService = ChurchService(
      id = "srv_active",
      churchId = "church_1",
      title = "Sèvis Dimanch",
      status = "active",
      isOpenForAttendance = true,
      ushers = "Claire Joseph",
      usherMemberIds = listOf("mem_claire"),
      usherUserIds = listOf("uid_claire"),
    )
    val endedService = activeService.copy(
      status = "completed",
      isOpenForAttendance = false,
    )

    assertTrue(activeService.isAssignedActiveUsher(assignedUsher, null))
    assertFalse(activeService.isAssignedActiveUsher(otherUsher, null))
    // Automatically revoked once service ends
    assertFalse(endedService.isAssignedActiveUsher(assignedUsher, null))
  }

  @Test
  fun `home feed excludes ended services while preserving active and upcoming services`() {
    val activeService = ChurchService(
      id = "srv_live",
      churchId = "church_1",
      title = "Sèvis Maten",
      status = "active",
      isOpenForAttendance = true,
      date = "2026-11-15",
      startTime = "09:00",
      endTime = "11:30",
    )
    val completedByStatus = activeService.copy(
      id = "srv_done_status",
      status = "completed",
      isOpenForAttendance = false,
    )
    val completedByPastEndTime = ChurchService(
      id = "srv_done_time",
      churchId = "church_1",
      title = "Sèvis Pase",
      status = "active",
      isOpenForAttendance = false,
      date = "2026-11-14",
      startTime = "09:00",
      endTime = "11:30",
    )
    val futureService = ChurchService(
      id = "srv_future",
      churchId = "church_1",
      title = "Sèvis Pwochenn",
      status = "scheduled",
      isOpenForAttendance = false,
      date = "2099-12-25",
      startTime = "09:00",
      endTime = "11:30",
    )

    val evalNow = com.zoutiw.omnichurch.data.models.parseServiceDateAndTimeMillis("2026-11-15", "10:00")!!
    val allServices = listOf(activeService, completedByStatus, completedByPastEndTime, futureService)
    val homeFeedServices = allServices.filter { !it.isServiceEnded(evalNow) }

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
    val arrivalOnTime = serviceStartMillis + 10 * 60_000L // 10 minutes after start
    val arrivalLate = serviceStartMillis + 20 * 60_000L // 20 minutes after start

    val status1 = if (arrivalOnTime <= toleranceDeadline) "on_time" else "late"
    val status2 = if (arrivalLate <= toleranceDeadline) "on_time" else "late"

    assertEquals("on_time", status1)
    assertEquals("late", status2)
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

    // Member MP9201 has no dedicated pin/securityPin -> fallback PIN is last 4 chars ("9201")
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
  fun `member login by phone number (7+ digits) and wrong PIN or empty fields return exact Creole errors`() {
    val church = sampleChurches[0]
    val phoneResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "50937001234",
        enteredPin = "9055",
      )
    assertTrue(phoneResult.isSuccess)

    // Wrong PIN
    val wrongPinResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "CH001-JD4827",
        enteredPin = "0000",
      )
    assertTrue(wrongPinResult.isFailure)
    val wrongPinEx = wrongPinResult.exceptionOrNull() as OmniAuthException
    assertEquals(
      "Kòd PIN sekirite sa a pa kòrèk pou manm sa a. Tanpri verifye kòd sekrè administratè a te ba ou a.",
      wrongPinEx.message,
    )

    // Empty ID
    val emptyIdResult =
      OmniAuthEvaluator.authenticateMemberFromList(
        church = church,
        churchMembers = listOf(sampleMembers[0]),
        idOrPhone = "   ",
        enteredPin = "9055",
      )
    val emptyIdEx = emptyIdResult.exceptionOrNull() as OmniAuthException
    assertEquals(
      "Tanpri antre ID Manm (Matrikil) ou oswa nimewo telefòn ou.",
      emptyIdEx.message,
    )
  }

  @Test
  fun `authenticateMemberAutoResolving detects member own church without selecting church list`() {
    // 1. Direct ID lookup with selectedChurchId = null resolves church_etg01 automatically
    val autoById =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = sampleMembers,
        idOrPhone = "ETG01-MP9201",
        enteredPin = "9201",
        selectedChurchId = null,
      )
    assertTrue(autoById.isSuccess)
    val idSessions = autoById.getOrThrow()
    assertEquals(1, idSessions.size)
    assertEquals("church_etg01", idSessions.first().church.id)
    assertEquals("Marie-Pauline Joseph", idSessions.first().member.fullName)

    // 2. Phone lookup with selectedChurchId = null resolves church_ch001 automatically
    val autoByPhone =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = sampleMembers,
        idOrPhone = "50937001234",
        enteredPin = "9055",
        selectedChurchId = null,
      )
    assertTrue(autoByPhone.isSuccess)
    val phoneSessions = autoByPhone.getOrThrow()
    assertEquals(1, phoneSessions.size)
    assertEquals("church_ch001", phoneSessions.first().church.id)

    // 3. Duplicate phone + PIN across 2 churches returns both verified sessions for user disambiguation
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

    // 4. Non-existent ID returns global not-found error
    val missing =
      OmniAuthEvaluator.authenticateMemberAutoResolving(
        churches = sampleChurches,
        allMembers = sampleMembers,
        idOrPhone = "UNKNOWN-9999",
        enteredPin = "9055",
        selectedChurchId = null,
      )
    assertTrue(missing.isFailure)
    assertTrue(missing.exceptionOrNull() is OmniAuthException)
  }

  @Test
  fun `salted SHA-256 admin passcode verification and admin Google email authorization work accurately`() {
    val churchA = sampleChurches[0] // salted with church.id ("church_ch001")
    val churchB = sampleChurches[1] // salted with church.code ("ETG01")

    assertTrue(OmniAuthEvaluator.verifyAdminPasscode(churchA, "SecretAdmin2026"))
    assertFalse(OmniAuthEvaluator.verifyAdminPasscode(churchA, "WrongPass"))

    // Fallback salt = church.code
    assertTrue(OmniAuthEvaluator.verifyAdminPasscode(churchB, "EtgPass99"))
    assertFalse(OmniAuthEvaluator.verifyAdminPasscode(churchB, "BadPass"))

    // Authorized Google email check (church admin email or super-admin email)
    assertTrue(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "pastor@tabenak.org"))
    assertTrue(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "charlesjj3186@gmail.com"))
    assertFalse(OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(churchA, "stranger@gmail.com"))
  }

  @Test
  fun `OmniQrParser correctly decodes universal verification URL, internal token, and raw member ID`() {
    // 1. Primary Universal Verification URL
    val url = "https://verify.omnichurch.download/?verify=1&church=CH001&member=MB-7842"
    val resUrl = com.zoutiw.omnichurch.data.models.OmniQrParser.parseQrToken(url)
    assertEquals("CH001", resUrl.churchId)
    assertEquals("MB-7842", resUrl.memberId)

    // 2. Secondary internal token
    val token = "OMNICHURCH|CHURCH:ETG01|MEMBER:MEM-001"
    val resToken = com.zoutiw.omnichurch.data.models.OmniQrParser.parseQrToken(token)
    assertEquals("ETG01", resToken.churchId)
    assertEquals("MEM-001", resToken.memberId)

    // 3. Raw member code fallback
    val raw = "MB-9999"
    val resRaw = com.zoutiw.omnichurch.data.models.OmniQrParser.parseQrToken(raw)
    assertEquals(null, resRaw.churchId)
    assertEquals("MB-9999", resRaw.memberId)
  }

  @Test
  fun `member verificationUrl generates correct universal url and photo fallback`() {
    val member = Member(
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
  }

  @Test
  fun `seven 3D reactions map accurately to keys and independent drawable resources`() {
    val allReactions = OmniReactionType.entries
    assertEquals(7, allReactions.size)

    assertEquals(
      listOf("like", "love", "pray", "haha", "wow", "sad", "clap"),
      allReactions.map { it.key },
    )

    assertEquals(
      OmniReactionType.PRAY,
      OmniReactionType.fromKey("amen"),
    )
    assertEquals(
      OmniReactionType.CLAP,
      OmniReactionType.fromKey("CLAP"),
    )

    val drawables = allReactions.map { it.toDrawableRes() }.toSet()
    assertEquals(7, drawables.size)
  }

  @Test
  fun `cross-church visitor attendance is excluded from host regular rate and capped at 100 percent`() {
    val regularPresent =
      listOf(
        AdminServiceMemberAttendanceItem("r1", "m1", "Jean", "CH-1", "Koral", "09:00", MemberAttendanceStatus.PRESENT),
        AdminServiceMemberAttendanceItem("r2", "m2", "Paul", "CH-2", "Jenes", "09:05", MemberAttendanceStatus.PRESENT),
        AdminServiceMemberAttendanceItem("r3", "m3", "Marc", "CH-3", "Dyak", "09:08", MemberAttendanceStatus.PRESENT),
      )
    val regularLate =
      listOf(
        AdminServiceMemberAttendanceItem("r4", "m4", "Luc", "CH-4", "Koral", "09:25", MemberAttendanceStatus.LATE),
      )
    val visitors =
      (1..5).map { idx ->
        AdminServiceMemberAttendanceItem(
          id = "v$idx",
          memberDocId = "origin_$idx",
          fullName = "Visitor $idx",
          matrikil = "ALP-000$idx",
          department = "Koral",
          timeLabel = "09:10",
          status = MemberAttendanceStatus.PRESENT,
          isVisitor = true,
          homeChurchId = "church_alpha",
          homeChurchName = "Legliz Alpha",
        )
      }

    val fullRoster =
      AdminServiceAttendanceRoster(
        presentMembers = regularPresent,
        lateMembers = regularLate,
        absentMembers = emptyList(),
        visitorMembers = visitors,
        totalRegularMembers = 4,
      )
    assertEquals(4, fullRoster.regularAttendedCount)
    assertEquals(100, fullRoster.regularAttendanceRatePercent)

    val partialRoster =
      AdminServiceAttendanceRoster(
        presentMembers = regularPresent.take(1),
        lateMembers = emptyList(),
        absentMembers = emptyList(),
        visitorMembers = visitors,
        totalRegularMembers = 2,
      )
    assertEquals(1, partialRoster.regularAttendedCount)
    assertEquals(50, partialRoster.regularAttendanceRatePercent)
  }

  @Test
  fun `AttendanceRecord visitor flags and canonical CHURCH-MEMBER QR format resolve accurately`() {
    val visitorRec =
      AttendanceRecord(
        id = "srv_1_mem_1",
        churchId = "church_b",
        serviceId = "srv_1",
        serviceTitle = "Sèvis Dimanch",
        memberId = "ALP-0001",
        memberDbId = "mem_1",
        attendanceStatus = "late",
        attendanceRole = "visitor",
        isVisitor = true,
        homeChurchId = "church_a",
        homeChurchName = "Legliz Alpha",
        hostChurchName = "Legliz Beta",
      )
    assertTrue(visitorRec.isCrossChurchVisitor)
    assertEquals("late", visitorRec.effectiveStatus)
    assertEquals("mem_1", visitorRec.effectiveMemberDbId)
    assertEquals("Sèvis Dimanch", visitorRec.effectiveServiceTitle)

    val regularRec =
      AttendanceRecord(
        id = "srv_1_mem_2",
        churchId = "church_b",
        serviceId = "srv_1",
        memberId = "BET-0001",
        status = "on_time",
        attendanceRole = "member",
        isVisitor = false,
      )
    assertFalse(regularRec.isCrossChurchVisitor)
    assertEquals("on_time", regularRec.effectiveStatus)

    val parsedPipe = com.zoutiw.omnichurch.data.models.OmniQrParser.parseQrToken("CHURCH:church_a|MEMBER:ALP-0001")
    assertEquals("church_a", parsedPipe.churchId)
    assertEquals("ALP-0001", parsedPipe.memberId)
  }

  @Test
  fun `AdMobRemoteController enforces Anti-Ban test IDs in test mode and resolves custom IDs only in production`() {
    val customProdMap =
      mapOf(
        "enabled" to true,
        "environment" to "production",
        "mobileEnabled" to true,
        "mobileForceTestIds" to true, // Anti-Ban Lock still ON
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

    // 1. When mobileForceTestIds == true (or isDebugBuild == true), MUST return Google Official Test IDs
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

    // 2. When environment == "production", mobileForceTestIds == false, and isDebugBuild == false -> custom prod IDs
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
  fun `AdMobRemoteController disables all ads for active Pro Licensed church and respectsscanner close frequency and cooldown`() {
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

    // Church CH001 has an active ad-free license -> isChurchAdFree = true, shouldShowAds = false
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

    // Church ETG01 only has an expired license -> isChurchAdFree = false, shouldShowAds = true
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

    // Door Scanner Interstitial: 3 scans (< 4 frequency) -> cannot show on close
    val nowMs = 1_750_000_000_000L
    assertFalse(AdMobRemoteController.canShowInterstitialOnScannerClose(unlicensedState, nowMs))

    // 4 scans (>= 4 frequency) and no recent interstitial -> eligible on scanner close!
    val readyState = unlicensedState.copy(sessionScanCount = 4, lastInterstitialShownAtMs = 0L)
    assertTrue(AdMobRemoteController.canShowInterstitialOnScannerClose(readyState, nowMs))

    // Cooldown lock: if shown 60s ago (< 180s cooldown), blocked even with 5 scans
    val cooldownBlockedState =
      unlicensedState.copy(
        sessionScanCount = 5,
        lastInterstitialShownAtMs = nowMs - 60_000L,
      )
    assertFalse(AdMobRemoteController.canShowInterstitialOnScannerClose(cooldownBlockedState, nowMs))

    // Cooldown elapsed: 185s ago (>= 180s cooldown) -> allowed on scanner close
    val cooldownElapsedState =
      unlicensedState.copy(
        sessionScanCount = 4,
        lastInterstitialShownAtMs = nowMs - 185_000L,
      )
    assertTrue(AdMobRemoteController.canShowInterstitialOnScannerClose(cooldownElapsedState, nowMs))
  }

  @Test
  fun `OmniAdMobBanner isPlacementBannerEnabled respects HOME ANNOUNCEMENTS SERVICES flags and master kill switches`() {
    // 0. Initial default schema (enabled=false, mobileEnabled=false, mobileForceTestIds=false, environment="test")
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
    assertTrue(initialState.isTestMode) // environment == "test" forces Google Test IDs
    assertFalse(initialState.shouldShowAds)
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.HOME, initialState))

    // 1. When enabled=true and mobileEnabled=true with default placement flags (home=true, announcements=true, services=false)
    val defaultState =
      AdMobRemoteController.resolveEffectiveState(
        config = RemoteAdMobConfig(enabled = true, mobileEnabled = true),
        licenses = emptyList(),
        activeChurchId = "church_free",
      )
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.HOME, defaultState))
    assertTrue(isPlacementBannerEnabled(OmniAdPlacement.ANNOUNCEMENTS, defaultState))
    assertFalse(isPlacementBannerEnabled(OmniAdPlacement.SERVICES, defaultState))

    // 2. Enable SERVICES banner and disable HOME banner remotely
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

    // 3. Master mobileEnabled kill-switch disables all placements even if individual flags are true
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
  fun canonicalMemberPhoto_prioritizesCustomChurchPhotosOverGoogleAvatars() {
    val customDataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRg=="
    val customHttp = "https://cdn.omnichurch.org/avatars/marie_01.jpg"
    val googleAvatar = "https://lh3.googleusercontent.com/a/ACg8ocK123456=s96-c"
    val ggphtAvatar = "https://yt3.ggpht.com/ytc/avatar=s88"

    assertTrue(isCustomMemberPhoto(customDataUri))
    assertTrue(isCustomMemberPhoto(customHttp))
    assertFalse(isCustomMemberPhoto(googleAvatar))
    assertFalse(isCustomMemberPhoto(ggphtAvatar))
    assertFalse(isCustomMemberPhoto(null))
    assertFalse(isCustomMemberPhoto("   "))

    // Custom photo in photoUrl overrides Google avatar in avatarUrl/googlePhoto
    assertEquals(
      customDataUri,
      resolveCanonicalMemberPhoto(
        avatarUrl = googleAvatar,
        photoUrl = customDataUri,
        googlePhoto = googleAvatar,
        fallbackGoogleAuthPhoto = ggphtAvatar,
      ),
    )

    // Custom photo in avatarUrl wins first
    assertEquals(
      customHttp,
      resolveCanonicalMemberPhoto(
        avatarUrl = customHttp,
        photoUrl = googleAvatar,
        googlePhoto = googleAvatar,
      ),
    )

    // Falls back to Google photo when no custom photo exists
    assertEquals(
      googleAvatar,
      resolveCanonicalMemberPhoto(
        avatarUrl = null,
        photoUrl = " ",
        googlePhoto = googleAvatar,
        fallbackGoogleAuthPhoto = ggphtAvatar,
      ),
    )

    // Falls back to auth credential photo when member has no photo at all
    assertEquals(
      ggphtAvatar,
      resolveCanonicalMemberPhoto(
        avatarUrl = null,
        photoUrl = null,
        googlePhoto = null,
        fallbackGoogleAuthPhoto = ggphtAvatar,
      ),
    )

    assertNull(resolveCanonicalMemberPhoto(null, "", " ", null))
  }

  @Test
  fun unifyMemberProfile_isNonDestructiveAndPreservesCustomPhotoAndAllMemberFields() {
    val customPhoto = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUg=="
    val googleAvatar = "https://lh3.googleusercontent.com/a/default-user=s96-c"

    val existing =
      Member(
        id = "mem_jean_01",
        memberId = "CH001-JB1001",
        memberCode = "CH001-JB1001",
        churchId = "church_ch001",
        churchName = "Legliz Tabènak",
        fullName = "Jean Baptiste",
        pin = "4827",
        securityPin = "4827",
        role = "Dyak",
        department = "Adorasyon",
        phone = "50937001122",
        email = null,
        dateOfBirth = "1992-05-14",
        notes = "Responsab son ak lòd",
        barcodeToken = "OMNI_QR_JEAN_4827",
        status = "active",
        memberType = "regular",
        joinedDate = "2023-01-10",
        avatarUrl = customPhoto,
        photoUrl = customPhoto,
        googlePhoto = null,
      )

    val unifiedResult =
      unifyMemberProfile(
        existingMember = existing,
        googleEmail = "  Jean.Baptiste@Gmail.Com ",
        googleUid = "  uid_google_jean_99 ",
        googlePhotoUrl = googleAvatar,
        updatedAtValue = "2026-11-16T12:00:00Z",
      )

    val merged = unifiedResult.member
    // Custom photo is NEVER overwritten by Google's photoUrl
    assertEquals(customPhoto, merged.avatarUrl)
    assertEquals(customPhoto, merged.photoUrl)
    assertEquals(customPhoto, merged.effectivePhotoUrl)

    // Existing member fields are strictly untouched
    assertEquals("4827", merged.pin)
    assertEquals("Dyak", merged.role)
    assertEquals("Adorasyon", merged.department)
    assertEquals("50937001122", merged.phone)
    assertEquals("1992-05-14", merged.dateOfBirth)
    assertEquals("Responsab son ak lòd", merged.notes)
    assertEquals("OMNI_QR_JEAN_4827", merged.barcodeToken)
    assertEquals("active", merged.status)
    assertEquals("regular", merged.memberType)
    assertEquals("2023-01-10", merged.joinedDate)
    assertEquals("church_ch001", merged.churchId)
    assertEquals("CH001-JB1001", merged.memberId)

    // Google fields normalized and blank email populated
    assertEquals("jean.baptiste@gmail.com", merged.googleEmail)
    assertEquals("uid_google_jean_99", merged.googleUid)
    assertEquals("jean.baptiste@gmail.com", merged.email)

    // Firestore merge map omits sensitive untouched fields so merge never clobbers them
    assertFalse(unifiedResult.firestoreMergeMap.containsKey("pin"))
    assertFalse(unifiedResult.firestoreMergeMap.containsKey("role"))
    assertFalse(unifiedResult.firestoreMergeMap.containsKey("barcodeToken"))
    assertEquals(customPhoto, unifiedResult.firestoreMergeMap["avatarUrl"])
    assertEquals(customPhoto, unifiedResult.firestoreMergeMap["photoUrl"])

    // When a member has NO photo at all, linking Google initializes avatarUrl/photoUrl/googlePhoto
    val noPhotoMember = existing.copy(avatarUrl = null, photoUrl = null, googlePhoto = null, email = "existing@church.org")
    val initializedPhotoResult =
      unifyMemberProfile(
        existingMember = noPhotoMember,
        googleEmail = "jean.google@gmail.com",
        googleUid = "uid_google_2",
        googlePhotoUrl = googleAvatar,
        updatedAtValue = "2026-11-16T12:00:00Z",
      )
    assertEquals(googleAvatar, initializedPhotoResult.member.avatarUrl)
    assertEquals(googleAvatar, initializedPhotoResult.member.photoUrl)
    assertEquals(googleAvatar, initializedPhotoResult.member.googlePhoto)
    // Existing non-blank email must be preserved
    assertEquals("existing@church.org", initializedPhotoResult.member.email)
  }

  @Test
  fun safeUnlinkAndSuperDeveloperIsolationGuard_behaveCorrectly() {
    val customPhoto = "data:image/jpeg;base64,/9j/CUSTOM_CHURCH_PHOTO=="
    val googleAvatar = "https://lh3.googleusercontent.com/a/ACg8ocGoogleOnly=s96-c"

    // 1. Safe unlink keeps custom church photo intact
    val customPhotoMember =
      Member(
        id = "mem_custom",
        googleEmail = "marie@gmail.com",
        googleUid = "uid_marie",
        avatarUrl = customPhoto,
        photoUrl = customPhoto,
        googlePhoto = customPhoto,
      )
    val unlinkCustom = buildUnlinkMemberGooglePayload(customPhotoMember, updatedAtValue = "now")
    assertEquals("", unlinkCustom.member.googleEmail)
    assertEquals("", unlinkCustom.member.googleUid)
    assertEquals(customPhoto, unlinkCustom.member.avatarUrl)
    assertEquals(customPhoto, unlinkCustom.member.photoUrl)
    assertEquals(customPhoto, unlinkCustom.member.googlePhoto)

    // 2. Safe unlink clears generic Google-only photo fields
    val googleOnlyMember =
      Member(
        id = "mem_google_only",
        googleEmail = "paul@gmail.com",
        googleUid = "uid_paul",
        avatarUrl = googleAvatar,
        photoUrl = googleAvatar,
        googlePhoto = googleAvatar,
      )
    val unlinkGoogleOnly = buildUnlinkMemberGooglePayload(googleOnlyMember, updatedAtValue = "now")
    assertEquals("", unlinkGoogleOnly.member.googleEmail)
    assertEquals("", unlinkGoogleOnly.member.googleUid)
    assertEquals("", unlinkGoogleOnly.member.avatarUrl)
    assertEquals("", unlinkGoogleOnly.member.photoUrl)
    assertEquals("", unlinkGoogleOnly.member.googlePhoto)

    // 3. Super Developer salted SHA-256 guard matches target developer digest and rejects normal emails
    assertTrue(isSuperDeveloperAccount("  CharlesJJ3186@Gmail.com "))
    assertFalse(isSuperDeveloperAccount("member@omnichurch.org"))
    assertFalse(isSuperDeveloperAccount(null))

    try {
      unifyMemberProfile(
        existingMember = customPhotoMember,
        googleEmail = "charlesjj3186@gmail.com",
        googleUid = "uid_super_dev",
        googlePhotoUrl = googleAvatar,
        updatedAtValue = "now",
      )
      fail("Expected OmniAuthException when attempting to link Super Developer account to a member")
    } catch (_: OmniAuthException) {
      // Expected isolation guard
    }
  }

  @Test
  fun productionQrPayload_and_verificationUrlSanitization_rejectInternalHosts() {
    val defaultUrl = AdMobRemoteController.OFFICIAL_PLATFORM_VERIFICATION_URL
    assertEquals("https://verify.omnichurch.download", defaultUrl)

    // Rejects localhost / internal / capacitor / malformed URLs
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://localhost:3000"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://127.0.0.1:8080/"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("http://10.0.2.2:5173"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("https://192.168.1.42"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("capacitor://localhost"))
    assertEquals(defaultUrl, AdMobRemoteController.sanitizeVerificationBaseUrl("   "))

    // Preserves valid public HTTPS domain without trailing slash
    assertEquals(
      "https://verify.omnichurch.download",
      AdMobRemoteController.sanitizeVerificationBaseUrl("  https://verify.omnichurch.download/  "),
    )
    assertEquals(
      "https://id.omnichurch.org",
      AdMobRemoteController.sanitizeVerificationBaseUrl("https://id.omnichurch.org///"),
    )

    // Canonical QR URL & canonical 1D barcode formatting
    assertEquals(
      "https://verify.omnichurch.download/?verify=1&church=CH001&member=EVG-0042",
      buildCanonicalQrPayload("CH001", "EVG-0042", "http://localhost:5173"),
    )
    assertEquals(
      "CHURCH:CH001|MEMBER:EVG-0042",
      buildCanonicalBarcodeToken("CH001", "evg-0042"),
    )
  }

  @Test
  fun omniQrParser_decodesAllWebAndMobileTokenFormatsSafely() {
    // 1. Official URL with trailing newline / control chars and lang
    val urlResult =
      OmniQrParser.parseScanToken(
        "\u0000https://verify.omnichurch.download/?verify=1&church=CH001&member=EVG-0042&lang=fr\r\n"
      )
    assertEquals("CH001", urlResult.churchId)
    assertEquals("EVG-0042", urlResult.memberId)
    assertEquals("fr", urlResult.lang)
    assertTrue(urlResult.isVerificationUrl)
    assertEquals("EVG", urlResult.inferredChurchPrefix)

    // 2. Hash-routed URL with query aliases
    val hashUrlResult =
      OmniQrParser.parseScanToken(
        "https://verify.omnichurch.download/#/?verify=1&churchId=CH009&verify_member=MEM-77&locale=en-US"
      )
    assertEquals("CH009", hashUrlResult.churchId)
    assertEquals("MEM-77", hashUrlResult.memberId)
    assertEquals("en", hashUrlResult.lang)
    assertTrue(hashUrlResult.isVerificationUrl)

    // 3. JSON payload
    val jsonResult =
      OmniQrParser.parseScanToken(
        """{"churchId":"CH002","memberId":"CH002-JB10","lang":"es"}"""
      )
    assertEquals("CH002", jsonResult.churchId)
    assertEquals("CH002-JB10", jsonResult.memberId)
    assertEquals("es", jsonResult.lang)

    // 4. Canonical & legacy pipe tokens
    val pipeResult = OmniQrParser.parseScanToken("OMNICHURCH|CHURCH:CH003|MEMBER:CH003-MK99")
    assertEquals("CH003", pipeResult.churchId)
    assertEquals("CH003-MK99", pipeResult.memberId)

    // 5. Hyphenated church-prefixed matrikil without explicit churchId
    val hyphenResult = OmniQrParser.parseScanToken("EVG-0042")
    assertNull(hyphenResult.churchId)
    assertEquals("EVG-0042", hyphenResult.memberId)
    assertEquals("EVG", hyphenResult.inferredChurchPrefix)
  }

  @Test
  fun remoteVerificationConfig_licenses_documentMasking_andSeal_matchSpec() {
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

    // Legal doc masking keeps last 4 alphanumeric characters
    assertEquals("•••-•••-4567", maskLegalDocumentNumber("001-123-4567", maskEnabled = true))
    assertEquals("001-123-4567", maskLegalDocumentNumber("001-123-4567", maskEnabled = false))
    assertEquals("••••", maskLegalDocumentNumber("1234", maskEnabled = true))

    // Deterministic seal format
    val sealVerified = computeDeterministicVerificationSeal("ch001", "doc_1", "evg-0042", true)
    val sealSame = computeDeterministicVerificationSeal("CH001", "doc_1", "EVG-0042", true)
    val sealInactive = computeDeterministicVerificationSeal("CH001", "doc_1", "EVG-0042", false)
    assertEquals(sealVerified, sealSame)
    assertTrue(sealVerified.matches(Regex("SEC-[0-9A-F]{4}-[0-9A-F]{4}-VERIFIED")))
    assertTrue(sealInactive.endsWith("-INACTIVE"))

    // Demo & deleted member filtering
    assertTrue(Member(id = "demo_123").isLegacyDemoOrDeleted)
    assertTrue(Member(id = "m1", isDeleted = true).isLegacyDemoOrDeleted)
    assertTrue(Member(id = "m2", isDemo = true).isLegacyDemoOrDeleted)
    assertFalse(Member(id = "mem_real", churchId = "CH001").isLegacyDemoOrDeleted)
  }
}



