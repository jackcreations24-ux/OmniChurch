package com.zoutiw.omnichurch

import com.zoutiw.omnichurch.core.utilities.OfflineAttendanceQueueManager
import com.zoutiw.omnichurch.core.utilities.OwnershipEvaluationStatus
import com.zoutiw.omnichurch.core.utilities.PendingOfflineAttendance
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.repositories.ServiceScheduleAndPayloadUseCase
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineQueueAndUseCasesTest {

  private val scheduleUseCase = ServiceScheduleAndPayloadUseCase()

  @Test
  fun `PendingOfflineAttendance serializes and deserializes member and visitor payloads accurately`() {
    val visitorItem =
      PendingOfflineAttendance(
        attendanceDocId = "srv_101_mem_202",
        churchId = "host_church_1",
        serviceId = "srv_101",
        serviceTitle = "Kilt Adorasyon Dimanch",
        serviceCategory = "dimanche_matin",
        serviceDate = "2026-11-29",
        memberDocId = "mem_202",
        memberId = "OM-88412",
        memberName = "Marie Carmel Joseph",
        memberPhone = "+50937001122",
        memberDepartment = "Koral",
        memberPhotoUrl = null,
        userId = "uid_marie_202",
        recordedBy = "usher_uid_1",
        attendanceStatus = "late",
        lateMinutes = 22,
        lateToleranceMinutes = 15,
        checkInMethod = "qr_scan",
        barcodeToken = "OMNICHURCH_home_church_9_OM-88412",
        timestampMillis = 1764423000000L,
        timestampIso = "2026-11-29T13:30:00Z",
        isVisitor = true,
        homeChurchId = "home_church_9",
        homeChurchName = "Legliz Batis Betèl",
        hostChurchName = "Legliz Evanjelik Salem",
      )

    val serializedMap = visitorItem.toMap()
    val restored = PendingOfflineAttendance.fromMap(serializedMap)

    assertNotNull(restored)
    assertEquals(visitorItem.attendanceDocId, restored!!.attendanceDocId)
    assertEquals(visitorItem.churchId, restored.churchId)
    assertEquals(visitorItem.serviceId, restored.serviceId)
    assertEquals(visitorItem.memberDocId, restored.memberDocId)
    assertEquals(visitorItem.memberName, restored.memberName)
    assertEquals("late", restored.attendanceStatus)
    assertEquals(22, restored.lateMinutes)
    assertTrue(restored.isVisitor)
    assertEquals("home_church_9", restored.homeChurchId)
    assertEquals("Legliz Batis Betèl", restored.homeChurchName)

    val malformed = mapOf("attendanceDocId" to "only_doc_id")
    assertNull(PendingOfflineAttendance.fromMap(malformed))
  }

  @Test
  fun `OfflineAttendanceQueueManager enqueues deduplicates and persists across restarts`() {
    var backingDiskJson: String? = null
    val queueManager1 =
      OfflineAttendanceQueueManager(
        readRawJson = { backingDiskJson },
        writeRawJson = { backingDiskJson = it },
      )
    assertEquals(0, queueManager1.pendingCountState.value)

    val item1 =
      PendingOfflineAttendance(
        attendanceDocId = "srv_01_mem_01",
        churchId = "ch_01",
        serviceId = "srv_01",
        serviceTitle = "Kilt Dimanch",
        serviceCategory = "dimanche_matin",
        serviceDate = "2026-11-29",
        memberDocId = "mem_01",
        memberId = "OM-1001",
        memberName = "Jean Baptiste",
        memberPhone = null,
        memberDepartment = "Jèn",
        memberPhotoUrl = null,
        userId = "mem_01",
        recordedBy = "usher_1",
        attendanceStatus = "on_time",
        lateMinutes = 0,
        lateToleranceMinutes = 15,
        checkInMethod = "qr_scan",
        barcodeToken = "OMNICHURCH_ch_01_OM-1001",
        timestampMillis = 1764410000000L,
        timestampIso = "2026-11-29T10:00:00Z",
        isVisitor = false,
        homeChurchId = null,
        homeChurchName = null,
        hostChurchName = null,
      )

    val item2 = item1.copy(attendanceDocId = "srv_01_mem_02", memberDocId = "mem_02", memberId = "OM-1002")

    // First enqueue succeeds
    assertTrue(queueManager1.enqueue(item1))
    assertTrue(queueManager1.containsAttendance("srv_01_mem_01"))
    assertEquals(1, queueManager1.pendingCountState.value)

    // Duplicate scan of same member for same service is rejected
    assertFalse(queueManager1.enqueue(item1))
    assertEquals(1, queueManager1.pendingCountState.value)

    // Second distinct member scan succeeds
    assertTrue(queueManager1.enqueue(item2))
    assertEquals(2, queueManager1.pendingCountState.value)

    // Re-instantiating OfflineAttendanceQueueManager reloads persisted queue from disk without data loss
    val queueManagerReloaded =
      OfflineAttendanceQueueManager(
        readRawJson = { backingDiskJson },
        writeRawJson = { backingDiskJson = it },
      )
    assertEquals(2, queueManagerReloaded.pendingCountState.value)
    assertTrue(queueManagerReloaded.containsAttendance("srv_01_mem_01"))
    assertTrue(queueManagerReloaded.containsAttendance("srv_01_mem_02"))
  }

  @Test
  fun `ServiceScheduleAndPayloadUseCase parses 24h 12h and French h clock formats and validates windows`() {
    val millis24 = scheduleUseCase.parseDateTimeMillis("2026-12-06", "09:30")
    val millisAmPm = scheduleUseCase.parseDateTimeMillis("2026-12-06", "09:30 AM")
    val millisFrenchH = scheduleUseCase.parseDateTimeMillis("2026-12-06", "09h30")

    assertNotNull(millis24)
    assertEquals(millis24, millisAmPm)
    assertEquals(millis24, millisFrenchH)

    val startEpoch = millis24!!
    val beforeStartNow = startEpoch - 30 * 60_000L
    val duringServiceNow = startEpoch + 30 * 60_000L
    val afterServiceNow = startEpoch + 4 * 3_600_000L

    assertTrue(scheduleUseCase.isBeforeScheduledStart("2026-12-06", "09:30", beforeStartNow))
    assertFalse(scheduleUseCase.isBeforeScheduledStart("2026-12-06", "09:30", duringServiceNow))
    assertTrue(scheduleUseCase.isAlreadyEndedInPast("2026-12-06", "09:30", "11:30", afterServiceNow))

    val activeService =
      ChurchService(
        id = "srv_dec6",
        churchId = "ch_1",
        title = "Kilt Adorasyon",
        date = "2026-12-06",
        startTime = "09:30",
        endTime = "11:30",
        startTimeEpochMillis = startEpoch,
        status = "active",
      )

    // Cannot open attendance before service start time
    val beforeResult = scheduleUseCase.validateAttendanceToggle(activeService, isOpen = true, now = beforeStartNow)
    assertTrue(beforeResult.isFailure)

    // Can open attendance during active service window
    val duringResult = scheduleUseCase.validateAttendanceToggle(activeService, isOpen = true, now = duringServiceNow)
    assertTrue(duringResult.isSuccess)

    // Cannot open attendance after service end time
    val afterResult = scheduleUseCase.validateAttendanceToggle(activeService, isOpen = true, now = afterServiceNow)
    assertTrue(afterResult.isFailure)
  }

  @Test
  fun `ServiceScheduleAndPayloadUseCase sanitizes create payload and clamps lateToleranceMinutes`() {
    val (payload, formattedDate) =
      scheduleUseCase.buildSanitizedCreatePayload(
        docId = "srv_future_1",
        uid = "admin_uid_1",
        churchId = "ch_1",
        churchName = "  Legliz Salem  ",
        title = "Jeûne et Prière",
        description = "Sèvis lapriyè",
        dateFormatted = "",
        lateToleranceMinutes = 999L, // Should clamp to 360L
        isOpenForAttendance = true,
        isLive = true,
        authorName = "Pastè Jean",
        category = "jeune_priere",
        cleanDate = "2099-01-15",
        startTime = "18:00",
        endTime = "20:00",
        theme = "La Foi",
        passage = "Hébreux 11:1",
        leader = "Frè Marc",
        preacher = "Pastè Jean",
        reader = null,
        musicians = null,
        ushers = null,
        sendReminder24h = true,
        imageUrl = null,
      )

    assertEquals("Legliz Salem", payload["churchName"])
    assertEquals(360L, payload["lateToleranceMinutes"])
    // Future service (year 2099) cannot start already open for attendance or live
    assertEquals(false, payload["isOpenForAttendance"])
    assertEquals(false, payload["isLive"])
    assertTrue(formattedDate.contains("2099-01-15"))
  }

  @Test
  fun `SessionLockManager manages per-church lock state and background timestamps`() = runBlocking {
    val lockManager = SessionLockManager(context = null)

    assertFalse(lockManager.isChurchAdminLocked("ch_alpha"))
    lockManager.lockChurchAdminSession("ch_alpha")
    assertTrue(lockManager.isChurchAdminLocked("ch_alpha"))
    assertFalse(lockManager.isChurchAdminLocked("ch_beta"))

    lockManager.recordAdminBackgroundTimestamp(1764000000000L)
    assertEquals(1764000000000L, lockManager.getLastAdminBackgroundTimestamp())

    lockManager.authorizeChurchAdminSession("ch_alpha")
    assertFalse(lockManager.isChurchAdminLocked("ch_alpha"))
    assertNull(lockManager.getLastAdminBackgroundTimestamp())

    val unclaimedChurch = Church(id = "ch_alpha", name = "Legliz Alpha")
    val noUserEval = SessionLockManager.evaluateOwnership(unclaimedChurch, user = null)
    assertEquals(OwnershipEvaluationStatus.NO_AUTH_USER, noUserEval.status)
    assertFalse(noUserEval.isAuthorized)
  }
}
