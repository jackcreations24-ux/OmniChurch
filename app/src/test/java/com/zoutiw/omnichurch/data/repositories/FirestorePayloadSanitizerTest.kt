package com.zoutiw.omnichurch.data.repositories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestorePayloadSanitizerTest {

  @Test
  fun documentIdAndChurchIdValidationEnforcesStrictRegexAndLengths() {
    assertTrue(FirestorePayloadSanitizer.isValidDocumentId("a"))
    assertTrue(FirestorePayloadSanitizer.isValidDocumentId("service_123-ABC"))
    assertFalse(FirestorePayloadSanitizer.isValidDocumentId(""))
    assertFalse(FirestorePayloadSanitizer.isValidDocumentId("bad/id"))
    assertFalse(FirestorePayloadSanitizer.isValidDocumentId("bad id"))
    assertFalse(FirestorePayloadSanitizer.isValidDocumentId("a".repeat(129)))

    assertFalse(FirestorePayloadSanitizer.isValidChurchId("a"))
    assertTrue(FirestorePayloadSanitizer.isValidChurchId("CH01"))
    assertFalse(FirestorePayloadSanitizer.isValidChurchId("church/invalid"))
  }

  @Test
  fun churchSanitizerStripsUnknownAndRestrictedVerificationFields() {
    val raw =
      mapOf(
        "code" to "EGD1",
        "name" to "Legliz Gras Bondye",
        "adminVerified" to true,
        "adminVerifiedAt" to "2026-01-01T00:00:00Z",
        "adminVerifiedBy" to "attacker",
        "unknownField" to "must_be_stripped",
        "adminAuthMethod" to "invalid_method",
        "status" to "deleted",
        "autoLockMinutes" to 999,
      )
    val sanitized = FirestorePayloadSanitizer.sanitizeChurchCreate(raw, "EGD1", currentUser = null)

    assertEquals("EGD1", sanitized["id"])
    assertEquals("email_password", sanitized["adminAuthMethod"])
    assertEquals("active", sanitized["status"])
    assertEquals(120, sanitized["autoLockMinutes"])
    assertFalse(sanitized.containsKey("adminVerified"))
    assertFalse(sanitized.containsKey("adminVerifiedAt"))
    assertFalse(sanitized.containsKey("adminVerifiedBy"))
    assertFalse(sanitized.containsKey("unknownField"))
    assertTrue(sanitized.keys.all { it in FirestorePayloadSanitizer.CHURCH_ALLOWED_KEYS })
  }

  @Test
  fun memberSanitizerEnforcesIdMatchAndImmutableChurchIdOnUpdate() {
    val createPayload =
      FirestorePayloadSanitizer.sanitizeMemberCreate(
        raw =
          mapOf(
            "id" to "wrong_id",
            "churchId" to "CH001",
            "fullName" to "  Jean Baptiste  ",
            "status" to "bogus_status",
            "extraUnauthorized" to 42,
          ),
        docId = "member_doc_1",
      )
    assertEquals("member_doc_1", createPayload["id"])
    assertEquals("Jean Baptiste", createPayload["fullName"])
    assertEquals("active", createPayload["status"])
    assertFalse(createPayload.containsKey("extraUnauthorized"))

    val updatePayload =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        raw =
          mapOf(
            "churchId" to "OTHER_CHURCH",
            "createdAt" to "tampered",
            "fullName" to "Jean B.",
            "status" to "suspended",
          )
      )
    assertFalse(updatePayload.containsKey("churchId"))
    assertFalse(updatePayload.containsKey("createdAt"))
    assertEquals("suspended", updatePayload["status"])
  }

  @Test
  fun serviceSanitizerEnforcesTerminalLockAndLateToleranceRange() {
    val endedUpdate =
      FirestorePayloadSanitizer.sanitizeServiceUpdate(
        raw =
          mapOf(
            "title" to "Attempted Title Edit",
            "isOpenForAttendance" to true,
            "date" to "2030-01-01",
            "notes" to "Post-service notes",
            "sendReminder24h" to false,
            "reminderSentAt" to "2026-11-22T12:00:00.000Z",
            "updatedAt" to "ts",
          ),
        isPermanentlyEnded = true,
      )
    assertFalse(endedUpdate.containsKey("title"))
    assertFalse(endedUpdate.containsKey("isOpenForAttendance"))
    assertFalse(endedUpdate.containsKey("date"))
    assertEquals("Post-service notes", endedUpdate["notes"])
    assertTrue(endedUpdate.keys.all { it in FirestorePayloadSanitizer.ENDED_SERVICE_UPDATE_ALLOWED_KEYS })

    val createdService =
      FirestorePayloadSanitizer.sanitizeServiceCreate(
        raw =
          mapOf(
            "churchId" to "CH001",
            "title" to "Sunday Worship",
            "lateToleranceMinutes" to 500L,
            "isPermanentlyEnded" to true,
            "isOpenForAttendance" to true,
            "unknownKey" to "strip",
          )
      )
    assertEquals(360L, createdService["lateToleranceMinutes"])
    assertEquals(false, createdService["isOpenForAttendance"])
    assertFalse(createdService.containsKey("unknownKey"))
  }

  @Test
  fun attendanceSanitizerEnforcesAllowedEnumsAndKeys() {
    val sanitized =
      FirestorePayloadSanitizer.sanitizeRegularAttendanceCreate(
        raw =
          mapOf(
            "churchId" to "CH001",
            "serviceId" to "svc_1",
            "memberDocId" to "mem_1",
            "memberId" to "CH001-JB1234",
            "recordedByUid" to "uid_admin",
            "checkInMethod" to "invalid_method",
            "status" to "unknown_status",
            "attendanceRole" to "member",
            "isVisitor" to false,
            "extraField" to "nope",
          )
      )
    assertEquals("qr_scan", sanitized["checkInMethod"])
    assertEquals("on_time", sanitized["status"])
    assertEquals("member", sanitized["attendanceRole"])
    assertFalse(sanitized.containsKey("extraField"))
    assertTrue(sanitized.keys.all { it in FirestorePayloadSanitizer.REGULAR_ATTENDANCE_ALLOWED_KEYS })
  }
}
