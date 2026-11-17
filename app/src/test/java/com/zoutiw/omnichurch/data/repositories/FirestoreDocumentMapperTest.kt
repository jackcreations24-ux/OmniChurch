package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.Timestamp
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreDocumentMapperTest {

  @Test
  fun `mapChurch succeeds when document contains id field, Timestamp createdAt and updatedAt, Double counts, and missing optionals`() {
    val rawDoc =
      mapOf<String, Any?>(
        "id" to "church_etg01",
        "code" to "ETG01",
        "name" to "Legliz Tabènak la Gras",
        "email" to "contact@etg01.org",
        "adminGoogleEmail" to "admin@etg01.org",
        "ownerId" to "uid_owner_99",
        "memberCount" to 145.0,
        "autoLockMinutes" to 10L,
        "quickPinEnabled" to "true",
        "createdAt" to Timestamp(1_760_000_000L, 0),
        "updatedAt" to Date(1_760_005_000_000L),
        "adminPasscodeUpdatedAt" to Timestamp(1_760_001_000L, 0),
      )

    val church = FirestoreDocumentMapper.mapChurch("church_etg01", rawDoc)

    assertEquals("church_etg01", church.id)
    assertEquals("ETG01", church.code)
    assertEquals("Legliz Tabènak la Gras", church.name)
    assertEquals("contact@etg01.org", church.email)
    assertEquals("admin@etg01.org", church.adminGoogleEmail)
    assertEquals(145, church.memberCount)
    assertEquals(10, church.autoLockMinutes)
    assertTrue(church.quickPinEnabled)
    assertTrue(church.createdAt.isNotBlank())
    assertTrue(church.updatedAt.isNotBlank())
    assertNotNull(church.adminPasscodeUpdatedAt)
    assertEquals("Ayiti", church.city)
    assertEquals("HTG", church.currency)
  }

  @Test
  fun `mapChurch falls back cleanly to churchName and code aliases`() {
    val rawDoc =
      mapOf<String, Any?>(
        "churchCode" to "CH777",
        "churchName" to "Legliz Betèl",
        "pastorName" to "Pastè Jean",
        "createdAt" to "2026-03-10T14:30:00Z",
      )

    val church = FirestoreDocumentMapper.mapChurch("doc_777", rawDoc)

    assertEquals("doc_777", church.id)
    assertEquals("CH777", church.code)
    assertEquals("Legliz Betèl", church.name)
    assertEquals("Pastè Jean", church.pastor)
    assertEquals("2026-03-10T14:30:00Z", church.createdAt)
  }

  @Test
  fun `mapMember handles id in body, ISO timestamps, nfcTagUid alias, and avatar fallback`() {
    val rawDoc =
      mapOf<String, Any?>(
        "id" to "mem_doc_1",
        "churchId" to "church_etg01",
        "firstName" to "Jean",
        "lastName" to "Baptiste",
        "memberId" to "ETG01-JB1001",
        "memberCode" to "ETG01-JB1001",
        "nfcTagUid" to "04AABBCCDD",
        "avatar" to "https://cdn.example.com/jb.jpg",
        "attendanceStreak" to 4.0,
        "totalAttendances" to "19",
        "createdAt" to "2026-02-01T10:00:00.000Z",
        "updatedAt" to 1_760_000_000_000L,
      )

    val member = FirestoreDocumentMapper.mapMember("mem_doc_1", rawDoc)

    assertEquals("mem_doc_1", member.id)
    assertEquals("Jean Baptiste", member.effectiveFullName)
    assertEquals("04AABBCCDD", member.nfcCardUid)
    assertEquals("https://cdn.example.com/jb.jpg", member.effectivePhotoUrl)
    assertEquals(4, member.attendanceStreak)
    assertEquals(19, member.totalAttendances)
    assertNotNull(member.createdAt)
    assertNotNull(member.updatedAt)
  }

  @Test
  fun `mapAttendanceRecord handles visitor records with id field, ISO timestamp string, and web aliases`() {
    val rawVisitorDoc =
      mapOf<String, Any?>(
        "id" to "srv_1_mem_99",
        "churchId" to "host_church_1",
        "serviceId" to "srv_1",
        "serviceTitle" to "Sèvis Dimanch Maten",
        "memberId" to "VIS-0042",
        "memberDbId" to "mem_99",
        "memberName" to "Marie Claire",
        "recordedBy" to "usher_uid_1",
        "attendanceStatus" to "late",
        "checkInMethod" to "nfc_scan",
        "timestamp" to "2026-04-12T13:15:30Z",
      )

    val record = FirestoreDocumentMapper.mapAttendanceRecord("srv_1_mem_99", rawVisitorDoc)

    assertEquals("srv_1_mem_99", record.id)
    assertEquals("Sèvis Dimanch Maten", record.serviceName)
    assertEquals("mem_99", record.memberDocId)
    assertEquals("VIS-0042", record.memberCode)
    assertEquals("usher_uid_1", record.recordedByUid)
    assertEquals("late", record.status)
    assertEquals("nfc_scan", record.checkInMethod)
    assertTrue(record.timestamp > 0L)
    assertNotNull(record.createdAt)
  }

  @Test
  fun `mapChurchService, mapAnnouncement, mapChurchNotification, and mapUserProfile handle mixed web schemas`() {
    val service =
      FirestoreDocumentMapper.mapChurchService(
        "srv_2",
        mapOf(
          "id" to "srv_2",
          "churchId" to "ch_1",
          "name" to "Kilt Swè",
          "posterUrl" to "https://cdn.example.com/poster.png",
          "lateToleranceMinutes" to 20.0,
          "isOpenForAttendance" to true,
          "createdAt" to "2026-05-01T18:00:00Z",
        ),
      )
    assertEquals("Kilt Swè", service.effectiveTitle)
    assertEquals("https://cdn.example.com/poster.png", service.imageUrl)
    assertEquals(20L, service.lateToleranceMinutes)
    assertTrue(service.isOpenForAttendance)
    assertNotNull(service.createdAt)

    val announcement =
      FirestoreDocumentMapper.mapAnnouncement(
        "ann_1",
        mapOf(
          "id" to "ann_1",
          "churchId" to "ch_1",
          "title" to "Reyinyon Jèn",
          "message" to "Samdi a 4è PM",
          "isPinned" to true,
          "createdAt" to 1_760_000_000L,
        ),
      )
    assertEquals("Samdi a 4è PM", announcement.content)
    assertTrue(announcement.isPinned)
    assertNotNull(announcement.createdAt)

    val notification =
      FirestoreDocumentMapper.mapChurchNotification(
        "notif_1",
        mapOf(
          "id" to "notif_1",
          "churchId" to "ch_1",
          "userId" to "all",
          "title" to "Avètisman",
          "message" to "Sèvis la louvri",
          "read" to false,
          "createdAt" to "2026-05-01T08:00:00Z",
        ),
      )
    assertFalse(notification.isRead)
    assertNotNull(notification.createdAt)

    val userProfile =
      FirestoreDocumentMapper.mapUserProfile(
        "uid_123",
        mapOf(
          "id" to "uid_123",
          "email" to "admin@omnichurch.org",
          "name" to "Pastè Marc",
          "role" to "admin",
          "createdAt" to "2026-05-01T08:00:00Z",
        ),
      )
    assertEquals("uid_123", userProfile.userId)
    assertEquals("Pastè Marc", userProfile.displayName)
    assertTrue(userProfile.isAdminOrSuperAdmin)
    assertNotNull(userProfile.createdAt)
  }
}
