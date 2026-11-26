package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.UserProfile
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await

private const val MAPPER_TAG = "FirestoreMapper"

suspend fun <T> Task<T>.awaitSafe(): T {
  return try {
    await()
  } catch (e: CancellationException) {
    currentCoroutineContext().ensureActive()
    throw e
  }
}

/**
 * Schema-resilient Firestore mapper that safely converts raw document maps (shared between
 * OmniChurch Web and Android) into strongly-typed Kotlin models without failing when documents
 * store `"id"` inside the payload, mix `Timestamp` / ISO-8601 `String` / epoch `Long` values,
 * or omit optional fields.
 */
object FirestoreDocumentMapper {

  fun mapChurch(documentId: String, data: Map<String, Any?>): Church {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val resolvedCode = data.optFirstNonBlank("code", "churchCode")
    val resolvedName =
      data.optFirstNonBlank("name", "churchName", "title").ifBlank {
        resolvedCode.ifBlank { resolvedId }
      }
    return Church(
      id = resolvedId,
      code = resolvedCode,
      name = resolvedName,
      pastor = data.optFirstNonBlank("pastor", "pastorName"),
      address = data.optString("address"),
      city = data.optFirstNonBlank("city", default = "Ayiti"),
      phone = data.optString("phone"),
      email = data.optString("email"),
      motto = data.optString("motto"),
      adminPasscode = data.optString("adminPasscode"),
      adminGoogleEmail = data.optString("adminGoogleEmail"),
      themeColor = data.optFirstNonBlank("themeColor", default = "indigo"),
      logoIcon = data.optFirstNonBlank("logoIcon", default = "Church"),
      currency = data.optFirstNonBlank("currency", default = "HTG"),
      status = data.optFirstNonBlank("status", default = "active"),
      memberCount = data.optInt("memberCount", 0),
      createdAt = data.optTimestampString("createdAt"),
      ownerId = data.optString("ownerId"),
      logoUrl = data.optNullableString("logoUrl"),
      bannerUrl = data.optNullableString("bannerUrl"),
      tagline = data.optString("tagline"),
      updatedAt = data.optTimestampString("updatedAt"),
      adminGoogleUid = data.optNullableString("adminGoogleUid"),
      adminGoogleName = data.optNullableString("adminGoogleName"),
      adminGooglePhoto = data.optNullableString("adminGooglePhoto"),
      adminAuthMethod = data.optNullableString("adminAuthMethod"),
      quickPinEnabled = data.optBoolean("quickPinEnabled", false),
      autoLockMinutes = data.optInt("autoLockMinutes", 0),
      adminPasscodeUpdatedAt = data.optNullableTimestampString("adminPasscodeUpdatedAt"),
    )
  }

  fun mapUserProfile(documentId: String, data: Map<String, Any?>): UserProfile {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    return UserProfile(
      id = resolvedId,
      userId = data.optFirstNonBlank("userId", default = resolvedId),
      email = data.optString("email"),
      displayName = data.optFirstNonBlank("displayName", "name"),
      photoUrl = data.optFirstNullableString("photoUrl", "avatarUrl", "photo", "avatar"),
      churchId = data.optNullableString("churchId"),
      churchName = data.optNullableString("churchName"),
      role = data.optFirstNonBlank("role", default = "member"),
      memberId = data.optNullableString("memberId"),
      language = data.optFirstNullableString("language") ?: "ht",
      createdAt = data.optTimestamp("createdAt"),
      updatedAt = data.optTimestamp("updatedAt"),
    )
  }

  fun mapMember(documentId: String, data: Map<String, Any?>): Member {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val firstName = data.optString("firstName")
    val lastName = data.optString("lastName")
    val fullName =
      data.optString("fullName")
        .ifBlank { "$firstName $lastName".trim() }
        .ifBlank { data.optFirstNonBlank("name", "displayName") }
    val nfcUid = data.optFirstNonBlank("nfcCardUid", "nfcTagUid").trim()
    val resolvedMemberId = data.optFirstNullableString("memberId", "matrikil")
    val resolvedMemberCode = data.optFirstNonBlank("memberCode", "memberId", "matrikil", "code")
    val resolvedChurchId = data.optFirstNonBlank("churchId", "church_id", "organizationId")
    return Member(
      id = resolvedId,
      memberId = resolvedMemberId,
      userId = data.optString("userId"),
      churchId = resolvedChurchId,
      churchName = data.optString("churchName"),
      churchLogoUrl = data.optNullableString("churchLogoUrl"),
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      memberCode = resolvedMemberCode,
      phone = data.optFirstNullableString("phone", "phoneNumber", "tel", "mobile"),
      email = data.optNullableString("email"),
      address = data.optString("address"),
      gender = data.optFirstNonBlank("gender", default = "other"),
      pin = data.optNullableString("pin"),
      securityPin = data.optNullableString("securityPin"),
      googleEmail = data.optNullableString("googleEmail"),
      googleUid = data.optNullableString("googleUid"),
      googlePhoto = data.optNullableString("googlePhoto"),
      photoUrl = data.optFirstNullableString("photoUrl", "photo", "imageUrl", "picture"),
      qrCode = data.optString("qrCode"),
      department = data.optFirstNonBlank("department", "ministry", "ministryDepartment"),
      role = data.optFirstNonBlank("role", "ministryRole", default = "member"),
      memberType = data.optFirstNonBlank("memberType", "type", default = "member"),
      status = data.optFirstNonBlank("status", default = "active"),
      barcodeToken = data.optString("barcodeToken"),
      nfcCardUid = nfcUid,
      joinedAt = data.optTimestampString("joinedAt"),
      joinDateFormatted = data.optNullableString("joinDateFormatted"),
      attendanceStreak = data.optInt("attendanceStreak", 0),
      totalAttendances = data.optInt("totalAttendances", 0),
      titlePrefix = data.optNullableString("titlePrefix"),
      avatarUrl = data.optFirstNullableString("avatarUrl", "avatar"),
      emergencyContact = data.optNullableString("emergencyContact"),
      joinedDate = data.optNullableTimestampString("joinedDate"),
      notes = data.optString("notes"),
      createdAt = data.optTimestamp("createdAt"),
      updatedAt = data.optTimestamp("updatedAt"),
    )
  }

  fun mapChurchService(documentId: String, data: Map<String, Any?>): ChurchService {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val resolvedCategory = data.optFirstNonBlank("category", "type", default = "dimanche_matin")
    val resolvedTitle = data.optFirstNonBlank("title", "name")
    val resolvedName = data.optFirstNonBlank("name", "title")
    return ChurchService(
      id = resolvedId,
      churchId = data.optString("churchId"),
      churchName = data.optString("churchName"),
      category = resolvedCategory,
      title = resolvedTitle,
      name = resolvedName,
      description = data.optNullableString("description"),
      imageUrl = data.optFirstNullableString("imageUrl", "posterUrl"),
      dateFormatted = data.optString("dateFormatted"),
      date = data.optString("date"),
      startTime = data.optString("startTime"),
      endTime = data.optString("endTime"),
      theme = data.optNullableString("theme"),
      passage = data.optNullableString("passage"),
      leader = data.optNullableString("leader"),
      preacher = data.optNullableString("preacher"),
      reader = data.optNullableString("reader"),
      musicians = data.optNullableString("musicians"),
      ushers = data.optNullableString("ushers"),
      sendReminder24h = data.optBoolean("sendReminder24h", false),
      type = data.optFirstNonBlank("type", "category", default = resolvedCategory),
      status = data.optFirstNonBlank("status", default = "active"),
      startTimeEpochMillis = data.optEpochMillis("startTimeEpochMillis", 0L),
      lateToleranceMinutes = data.optLong("lateToleranceMinutes", 15L),
      isOpenForAttendance = data.optBoolean("isOpenForAttendance", false) && !data.optBoolean("isPermanentlyEnded", false),
      wasOpenedForAttendance = data.optBoolean("wasOpenedForAttendance", false),
      isPermanentlyEnded = data.optBoolean("isPermanentlyEnded", false),
      isLive = data.optBoolean("isLive", false) && !data.optBoolean("isPermanentlyEnded", false),
      endedAt = data.optNullableTimestampString("endedAt"),
      reminderSentAt = data.optNullableTimestampString("reminderSentAt"),
      notes = data.optNullableString("notes"),
      authorId = data.optFirstNonBlank("authorId", "createdBy"),
      authorName = data.optString("authorName"),
      createdBy = data.optFirstNonBlank("createdBy", "authorId"),
      reactionsCount = data.optLong("reactionsCount", 0L),
      commentsCount = data.optLong("commentsCount", 0L),
      createdAt = data.optTimestamp("createdAt"),
      updatedAt = data.optTimestamp("updatedAt"),
    )
  }

  fun mapAnnouncement(documentId: String, data: Map<String, Any?>): Announcement {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val resolvedPoster = data.optNullableString("posterUrl")
    val resolvedImage = data.optFirstNullableString("imageUrl", "posterUrl") ?: resolvedPoster
    val resolvedPinned = data.optBoolean("isPinned", data.optBoolean("pinned", false))
    return Announcement(
      id = resolvedId,
      churchId = data.optString("churchId"),
      title = data.optString("title"),
      content = data.optFirstNonBlank("content", "message", "body"),
      category = data.optFirstNonBlank("category", default = "anons"),
      department = data.optString("department"),
      date = data.optNullableString("date"),
      time = data.optNullableString("time"),
      targetAudience = data.optFirstNonBlank("targetAudience", "audience", default = "Tout moun"),
      priority = data.optFirstNonBlank("priority", default = "normal"),
      expiresAt = data.optNullableTimestampString("expiresAt"),
      isPinned = resolvedPinned,
      isPublished = data.optBoolean("isPublished", true),
      showToastOnLogin = data.optBoolean("showToastOnLogin", false),
      toastAlertLevel = data.optFirstNonBlank("toastAlertLevel", default = "info"),
      isScheduledNotification = data.optBoolean("isScheduledNotification", false),
      scheduledNotificationDate = data.optNullableTimestampString("scheduledNotificationDate"),
      posterUrl = resolvedPoster,
      imageUrl = resolvedImage,
      imageCaption = data.optNullableString("imageCaption"),
      serviceId = data.optNullableString("serviceId"),
      authorId = data.optFirstNonBlank("authorId", "createdBy"),
      authorName = data.optFirstNonBlank("authorName", "author"),
      authorPhotoUrl = data.optNullableString("authorPhotoUrl"),
      titleHt = data.optNullableString("title_ht"),
      titleFr = data.optNullableString("title_fr"),
      titleEn = data.optNullableString("title_en"),
      titleEs = data.optNullableString("title_es"),
      contentHt = data.optNullableString("content_ht"),
      contentFr = data.optNullableString("content_fr"),
      contentEn = data.optNullableString("content_en"),
      contentEs = data.optNullableString("content_es"),
      reactionsCount = data.optLong("reactionsCount", 0L),
      commentsCount = data.optLong("commentsCount", 0L),
      createdAt = data.optTimestamp("createdAt"),
      updatedAt = data.optTimestamp("updatedAt"),
    )
  }

  fun mapAnnouncementReaction(documentId: String, data: Map<String, Any?>): AnnouncementReaction {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    return AnnouncementReaction(
      id = resolvedId,
      targetId = data.optString("targetId"),
      churchId = data.optString("churchId"),
      userId = data.optString("userId"),
      userName = data.optString("userName"),
      reactionType = data.optFirstNonBlank("reactionType", default = "pray"),
      createdAt = data.optTimestamp("createdAt"),
    )
  }

  fun mapAnnouncementComment(documentId: String, data: Map<String, Any?>): AnnouncementComment {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    return AnnouncementComment(
      id = resolvedId,
      targetId = data.optString("targetId"),
      churchId = data.optString("churchId"),
      userId = data.optString("userId"),
      userName = data.optString("userName"),
      userPhotoUrl = data.optNullableString("userPhotoUrl"),
      content = data.optString("content"),
      createdAt = data.optTimestamp("createdAt"),
    )
  }

  fun mapAttendanceRecord(documentId: String, data: Map<String, Any?>): AttendanceRecord {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val createdTimestamp = data.optTimestamp("createdAt")
    val parsedEpoch =
      data.optEpochMillisOrNull("timestamp")
        ?: createdTimestamp?.toDate()?.time
        ?: System.currentTimeMillis()
    val effectiveCreatedAt = createdTimestamp ?: Timestamp(Date(parsedEpoch))

    val resolvedRole = data.optFirstNonBlank("attendanceRole", default = "member")
    val resolvedIsVisitor =
      data.optBoolean("isVisitor", false) || resolvedRole.equals("visitor", ignoreCase = true)

    return AttendanceRecord(
      id = resolvedId,
      churchId = data.optString("churchId"),
      serviceId = data.optString("serviceId"),
      serviceName = data.optFirstNonBlank("serviceName", "serviceTitle"),
      memberId = data.optString("memberId"),
      memberDocId = data.optFirstNonBlank("memberDocId", "memberDbId"),
      memberCode = data.optFirstNonBlank("memberCode", "memberId"),
      memberName = data.optString("memberName"),
      memberPhotoUrl = data.optNullableString("memberPhotoUrl"),
      userId = data.optString("userId"),
      recordedByUid = data.optFirstNonBlank("recordedByUid", "recordedBy"),
      status = data.optFirstNonBlank("status", "attendanceStatus", default = "on_time"),
      checkInMethod = data.optFirstNonBlank("checkInMethod", default = "qr_scanner"),
      barcodeToken = data.optString("barcodeToken"),
      attendanceRole = resolvedRole,
      isVisitor = resolvedIsVisitor,
      homeChurchId = data.optNullableString("homeChurchId"),
      homeChurchName = data.optNullableString("homeChurchName"),
      timestamp = parsedEpoch,
      createdAt = effectiveCreatedAt,
    )
  }

  fun mapChurchNotification(documentId: String, data: Map<String, Any?>): ChurchNotification {
    val resolvedId = documentId.trim().ifEmpty { data.optString("id").trim() }
    val readState =
      when {
        data.containsKey("isRead") -> data.optBoolean("isRead", false)
        data.containsKey("read") -> data.optBoolean("read", false)
        else -> false
      }
    return ChurchNotification(
      id = resolvedId,
      userId = data.optString("userId"),
      churchId = data.optString("churchId"),
      title = data.optString("title"),
      message = data.optString("message"),
      type = data.optFirstNonBlank("type", default = "system"),
      isRead = readState,
      createdAt = data.optTimestamp("createdAt"),
    )
  }
}

private inline fun <T> DocumentSnapshot.mapSafely(
  entityName: String,
  crossinline block: (Map<String, Any?>) -> T,
): T? {
  if (!exists()) return null
  val rawData =
    try {
      getData(DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: Exception) {
      Log.e(MAPPER_TAG, "Failed reading $entityName data for $id: ${e.message}", e)
      null
    } ?: return null
  return try {
    block(rawData)
  } catch (e: Exception) {
    Log.e(MAPPER_TAG, "Failed mapping $entityName document $id: ${e.message}", e)
    null
  }
}

fun DocumentSnapshot.toChurch(): Church? =
  mapSafely("Church") { FirestoreDocumentMapper.mapChurch(id, it) }

fun DocumentSnapshot.toUserProfile(): UserProfile? =
  mapSafely("UserProfile") { FirestoreDocumentMapper.mapUserProfile(id, it) }

fun DocumentSnapshot.toMember(): Member? =
  mapSafely("Member") { FirestoreDocumentMapper.mapMember(id, it) }

fun DocumentSnapshot.toChurchService(): ChurchService? =
  mapSafely("ChurchService") { FirestoreDocumentMapper.mapChurchService(id, it) }

fun DocumentSnapshot.toAnnouncement(): Announcement? =
  mapSafely("Announcement") { FirestoreDocumentMapper.mapAnnouncement(id, it) }

fun DocumentSnapshot.toAnnouncementReaction(): AnnouncementReaction? =
  mapSafely("AnnouncementReaction") { FirestoreDocumentMapper.mapAnnouncementReaction(id, it) }

fun DocumentSnapshot.toAnnouncementComment(): AnnouncementComment? =
  mapSafely("AnnouncementComment") { FirestoreDocumentMapper.mapAnnouncementComment(id, it) }

fun DocumentSnapshot.toAttendanceRecord(): AttendanceRecord? =
  mapSafely("AttendanceRecord") { FirestoreDocumentMapper.mapAttendanceRecord(id, it) }

fun DocumentSnapshot.toChurchNotification(): ChurchNotification? =
  mapSafely("ChurchNotification") { FirestoreDocumentMapper.mapChurchNotification(id, it) }
