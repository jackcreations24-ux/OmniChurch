package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.data.models.formatLocalizedServiceDate
import com.zoutiw.omnichurch.data.models.formatServiceClockAmPm
import com.zoutiw.omnichurch.data.repositories.ServiceScheduleAndPayloadUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceAttendanceAndRbacTest {

  private val scheduleUseCase = ServiceScheduleAndPayloadUseCase()

  @Test
  fun `formatLocalizedServiceDate renders full weekday, day, month, and year across ht, fr, es, en`() {
    val isoDate = "2026-11-23" // Monday, November 23, 2026
    assertEquals("lendi 23 novanm 2026", formatLocalizedServiceDate(isoDate, "ht"))
    assertEquals("lundi 23 novembre 2026", formatLocalizedServiceDate(isoDate, "fr"))
    assertEquals("lunes 23 de noviembre 2026", formatLocalizedServiceDate(isoDate, "es"))
    assertEquals("Monday, November 23, 2026", formatLocalizedServiceDate(isoDate, "en"))
  }

  @Test
  fun `formatServiceClockAmPm converts 24h times and time ranges to 12h AM PM precision`() {
    assertEquals("7:00 PM", formatServiceClockAmPm("19:00"))
    assertEquals("8:30 AM", formatServiceClockAmPm("08:30"))
    assertEquals("12:00 AM", formatServiceClockAmPm("00:00"))
    assertEquals("12:15 PM", formatServiceClockAmPm("12:15"))
    assertEquals("7:00 PM - 9:00 PM", formatServiceClockAmPm("19:00 - 21:00"))
  }

  @Test
  fun `strict LIVE badge and attendance toggle reject pre-start, post-end, and permanently ended services`() {
    val startMs = scheduleUseCase.parseDateTimeMillis("2026-11-23", "19:00")!!
    val beforeStartNow = startMs - 30 * 60_000L
    val duringServiceNow = startMs + 30 * 60_000L
    val afterEndNow = startMs + 3 * 3600_000L

    val service =
      ChurchService(
        id = "srv_001",
        churchId = "ch_001",
        title = "Sèvis Adorasyon",
        date = "2026-11-23",
        startTime = "19:00",
        endTime = "21:00",
        startTimeEpochMillis = startMs,
        isOpenForAttendance = true,
        isPermanentlyEnded = false,
      )

    // Before scheduled start: LIVE badge must be false and opening attendance must be rejected
    assertFalse(service.isLiveBadgeActive(beforeStartNow))
    assertTrue(scheduleUseCase.validateAttendanceToggle(service, isOpen = true, now = beforeStartNow).isFailure)

    // During scheduled window: LIVE badge is active and toggle is allowed
    assertTrue(service.isLiveBadgeActive(duringServiceNow))
    assertTrue(scheduleUseCase.validateAttendanceToggle(service, isOpen = true, now = duringServiceNow).isSuccess)

    // After end time: LIVE badge is false and opening attendance is rejected
    assertFalse(service.isLiveBadgeActive(afterEndNow))
    assertTrue(scheduleUseCase.validateAttendanceToggle(service, isOpen = true, now = afterEndNow).isFailure)

    // Permanently ended service: cannot be re-opened even during time window
    val endedService = service.copy(isPermanentlyEnded = true)
    assertFalse(endedService.isLiveBadgeActive(duringServiceNow))
    assertTrue(scheduleUseCase.validateAttendanceToggle(endedService, isOpen = true, now = duringServiceNow).isFailure)
  }

  @Test
  fun `OmniRoleEvaluator enforces strict Deacon word-token RBAC and prevents Reception privilege escalation`() {
    val activeDeacon =
      Member(
        id = "mem_deacon",
        fullName = "Jean Paul",
        role = "Dyak",
        department = "Adorasyon",
        status = "active",
        memberType = "member",
      )
    val departmentOnlyNotDeacon =
      Member(
        id = "mem_dept_only",
        fullName = "Marc Antoine",
        role = "Manm",
        department = "Dyakonat",
        status = "active",
        memberType = "member",
      )
    val pureReception =
      Member(
        id = "mem_reception",
        fullName = "Marie Claire",
        role = "Resepsyon",
        department = "Akèy",
        status = "active",
        memberType = "member",
      )
    val inactiveDeacon = activeDeacon.copy(status = "inactive")
    val visitorDeacon = activeDeacon.copy(memberType = "visitor")

    // Active Deacon can manage services & announcements, but pure Deacon cannot scan attendance
    assertTrue(OmniRoleEvaluator.isActiveDeacon(activeDeacon))
    assertTrue(OmniRoleEvaluator.isPureDeacon(activeDeacon, isAdmin = false))
    assertTrue(OmniRoleEvaluator.canManageServices(activeDeacon, isAdmin = false))
    assertTrue(OmniRoleEvaluator.canPublishAnnouncements(activeDeacon, isAdmin = false))
    assertFalse(OmniRoleEvaluator.canScanAttendance(activeDeacon, isAdmin = false))

    // Department name alone never grants Deacon rights
    assertFalse(OmniRoleEvaluator.isActiveDeacon(departmentOnlyNotDeacon))
    assertFalse(OmniRoleEvaluator.canManageServices(departmentOnlyNotDeacon, isAdmin = false))

    // Pure Reception can scan attendance but cannot manage services or publish announcements
    assertTrue(OmniRoleEvaluator.isActiveReception(pureReception))
    assertTrue(OmniRoleEvaluator.canScanAttendance(pureReception, isAdmin = false))
    assertFalse(OmniRoleEvaluator.canManageServices(pureReception, isAdmin = false))
    assertFalse(OmniRoleEvaluator.canPublishAnnouncements(pureReception, isAdmin = false))

    // Inactive or visitor members cannot exercise Deacon privileges
    assertFalse(OmniRoleEvaluator.isActiveDeacon(inactiveDeacon))
    assertFalse(OmniRoleEvaluator.isActiveDeacon(visitorDeacon))
  }

  @Test
  fun `Deacon author-only delete and announcement edit rules and PII stripping on public display entries`() {
    val ownService =
      ChurchService(
        id = "srv_own",
        authorId = "uid_deacon_1",
        createdBy = "uid_deacon_1",
        isPermanentlyEnded = false,
      )
    val otherService =
      ChurchService(
        id = "srv_other",
        authorId = "uid_admin_9",
        createdBy = "uid_admin_9",
        isPermanentlyEnded = false,
      )
    val lockedOwnService = ownService.copy(isPermanentlyEnded = true)

    assertTrue(
      OmniRoleEvaluator.canDeleteService(
        service = ownService,
        isAdmin = false,
        isDeacon = true,
        currentAuthUid = "uid_deacon_1",
      )
    )
    assertFalse(
      OmniRoleEvaluator.canDeleteService(
        service = otherService,
        isAdmin = false,
        isDeacon = true,
        currentAuthUid = "uid_deacon_1",
      )
    )
    assertFalse(
      OmniRoleEvaluator.canDeleteService(
        service = lockedOwnService,
        isAdmin = false,
        isDeacon = true,
        currentAuthUid = "uid_deacon_1",
      )
    )

    val ownAnnouncement = Announcement(id = "ann_1", authorId = "uid_deacon_1")
    val otherAnnouncement = Announcement(id = "ann_2", authorId = "uid_other")
    assertTrue(
      OmniRoleEvaluator.canEditOrDeleteAnnouncement(
        announcement = ownAnnouncement,
        isAdmin = false,
        isDeacon = true,
        currentAuthUid = "uid_deacon_1",
      )
    )
    assertFalse(
      OmniRoleEvaluator.canEditOrDeleteAnnouncement(
        announcement = otherAnnouncement,
        isAdmin = false,
        isDeacon = true,
        currentAuthUid = "uid_deacon_1",
      )
    )

    // Verify PII stripping on Member.toPublicDisplayEntry() for Deacon dropdowns
    val fullMemberWithPii =
      Member(
        id = "mem_007",
        memberId = "CH01-JP1234",
        churchId = "ch_01",
        fullName = "Frè Jean Pierre",
        phone = "+509 3711-2233",
        email = "jp@secret.org",
        pin = "9876",
        securityPin = "9876",
        documentNumber = "001-999-888-7",
        barcodeToken = "OMNICHURCH_SECRET_TOKEN",
        nfcCardUid = "04AABBCCDD",
      )
    val stripped = fullMemberWithPii.toPublicDisplayEntry()
    assertEquals("Frè Jean Pierre", stripped.fullName)
    assertNull(stripped.phone)
    assertNull(stripped.email)
    assertNull(stripped.pin)
    assertNull(stripped.securityPin)
    assertNull(stripped.documentNumber)
    assertEquals("", stripped.barcodeToken)
    assertEquals("", stripped.nfcCardUid)
  }
}
