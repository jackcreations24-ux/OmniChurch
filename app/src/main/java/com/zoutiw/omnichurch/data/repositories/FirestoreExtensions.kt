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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

  private val isoReadPatterns =
    listOf(
      "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
      "yyyy-MM-dd'T'HH:mm:ssZ",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd",
    )

  private fun formatIsoUtc(date: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
  }

  private fun parseIsoToDate(raw: String): Date? {
    val clean = raw.trim()
    if (clean.isEmpty()) return null
    clean.toLongOrNull()?.let { num ->
      val millis = if (num in 1L..9_999_999_999L) num * 1000L else num
      return Date(millis)
    }
    for (pattern in isoReadPatterns) {
      try {
        val sdf = SimpleDateFormat(pattern, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        sdf.isLenient = true
        val parsed = sdf.parse(clean)
        if (parsed != null) return parsed
      } catch (_: Exception) {}
    }
    return null
  }

  private fun mapToTimestamp(map: Map<*, *>): Timestamp? {
    val rawSeconds = map["seconds"] ?: map["_seconds"] ?: return null
    val seconds = (rawSeconds as? Number)?.toLong() ?: rawSeconds.toString().toLongOrNull() ?: return null
    val rawNanos = map["nanoseconds"] ?: map["_nanoseconds"] ?: 0
    val nanos = (rawNanos as? Number)?.toInt() ?: rawNanos.toString().toIntOrNull() ?: 0
    return Timestamp(seconds, nanos.coerceIn(0, 999_999_999))
  }

  private fun Map<String, Any?>.optString(key: String, default: String = ""): String {
    val value = this[key] ?: return default
    return when (value) {
      is String -> value.ifEmpty { default }
      is Number,
      is Boolean -> value.toString()
      else -> default
    }
  }

  private fun Map<String, Any?>.optFirstNonBlank(vararg keys: String, default: String = ""): String {
    for (key in keys) {
      val candidate = optString(key, "").trim()
      if (candidate.isNotEmpty()) return candidate
    }
    return default
  }

  private fun Map<String, Any?>.optNullableString(key: String): String? {
    val value = this[key] ?: return null
    return when (value) {
      is String -> value.takeIf { it.isNotEmpty() }
      is Number,
      is Boolean -> value.toString()
      else -> null
    }
  }

  private fun Map<String, Any?>.optFirstNullableString(vararg keys: String): String? {
    for (key in keys) {
      val candidate = optNullableString(key)?.trim()
      if (!candidate.isNullOrEmpty()) return candidate
    }
    return null
  }

  private fun Map<String, Any?>.optBoolean(key: String, default: Boolean = false): Boolean {
    val value = this[key] ?: return default
    return when (value) {
      is Boolean -> value
      is Number -> value.toLong() != 0L
      is String ->
        when (value.trim().lowercase()) {
          "true",
          "1",
          "yes",
          "wi" -> true
          "false",
          "0",
          "no",
          "non" -> false
          else -> default
        }
      else -> default
    }
  }

  private fun Map<String, Any?>.optInt(key: String, default: Int = 0): Int {
    val value = this[key] ?: return default
    return when (value) {
      is Number -> value.toInt()
      is String -> value.trim().toIntOrNull() ?: value.trim().toDoubleOrNull()?.toInt() ?: default
      else -> default
    }
  }

  private fun Map<String, Any?>.optLong(key: String, default: Long = 0L): Long {
    val value = this[key] ?: return default
    return when (value) {
      is Number -> value.toLong()
      is String -> value.trim().toLongOrNull() ?: value.trim().toDoubleOrNull()?.toLong() ?: default
      else -> default
    }
  }

  private fun Map<String, Any?>.optTimestamp(key: String): Timestamp? {
    val value = this[key] ?: return null
    return when (value) {
      is Timestamp -> value
      is Date -> Timestamp(value)
      is Number -> {
        val raw = value.toLong()
        if (raw <= 0L) null
        else {
          val millis = if (raw < 10_000_000_000L) raw * 1000L else raw
          Timestamp(Date(millis))
        }
      }
      is Map<*, *> -> mapToTimestamp(value)
      is String -> parseIsoToDate(value)?.let { Timestamp(it) }
      else -> null
    }
  }

  private fun Map<String, Any?>.optTimestampString(key: String): String {
    val value = this[key] ?: return ""
    return when (value) {
      is String -> value.trim()
      is Timestamp -> formatIsoUtc(value.toDate())
      is Date -> formatIsoUtc(value)
      is Number -> {
        val raw = value.toLong()
        if (raw <= 0L) "" else formatIsoUtc(Date(if (raw < 10_000_000_000L) raw * 1000L else raw))
      }
      is Map<*, *> -> mapToTimestamp(value)?.toDate()?.let { formatIsoUtc(it) }.orEmpty()
      else -> ""
    }
  }

  private fun Map<String, Any?>.optNullableTimestampString(key: String): String? {
    return optTimestampString(key).takeIf { it.isNotEmpty() }
  }

  private fun Map<String, Any?>.optEpochMillisOrNull(key: String): Long? {
    val value = this[key] ?: return null
    return when (value) {
      is Number -> {
        val raw = value.toLong()
        if (raw <= 0L) null else if (raw < 10_000_000_000L) raw * 1000L else raw
      }
      is Timestamp -> value.toDate().time
      is Date -> value.time
      is Map<*, *> -> mapToTimestamp(value)?.toDate()?.time
      is String -> parseIsoToDate(value)?.time
      else -> null
    }
  }

  private fun Map<String, Any?>.optEpochMillis(key: String, default: Long): Long {
    return optEpochMillisOrNull(key) ?: default
  }

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
      autoLockMinutes = data.optInt("autoLockMinutes", 5),
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
      data.optString("fullName").ifBlank { "$firstName $lastName".trim() }
    val nfcUid = data.optFirstNonBlank("nfcCardUid", "nfcTagUid").trim()
    return Member(
      id = resolvedId,
      memberId = data.optNullableString("memberId"),
      userId = data.optString("userId"),
      churchId = data.optString("churchId"),
      churchName = data.optString("churchName"),
      churchLogoUrl = data.optNullableString("churchLogoUrl"),
      firstName = firstName,
      lastName = lastName,
      fullName = fullName,
      memberCode = data.optString("memberCode"),
      phone = data.optNullableString("phone"),
      email = data.optNullableString("email"),
      address = data.optString("address"),
      gender = data.optFirstNonBlank("gender", default = "other"),
      pin = data.optNullableString("pin"),
      securityPin = data.optNullableString("securityPin"),
      googleEmail = data.optNullableString("googleEmail"),
      googleUid = data.optNullableString("googleUid"),
      googlePhoto = data.optNullableString("googlePhoto"),
      photoUrl = data.optFirstNullableString("photoUrl", "photo"),
      qrCode = data.optString("qrCode"),
      department = data.optString("department"),
      role = data.optFirstNonBlank("role", default = "member"),
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
      isOpenForAttendance = data.optBoolean("isOpenForAttendance", false),
      isLive = data.optBoolean("isLive", false),
      authorId = data.optString("authorId"),
      authorName = data.optString("authorName"),
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
    return Announcement(
      id = resolvedId,
      churchId = data.optString("churchId"),
      title = data.optString("title"),
      content = data.optFirstNonBlank("content", "message", "body"),
      category = data.optFirstNonBlank("category", default = "anons"),
      department = data.optString("department"),
      date = data.optNullableString("date"),
      targetAudience = data.optFirstNonBlank("targetAudience", default = "Tout moun"),
      isPinned = data.optBoolean("isPinned", false),
      showToastOnLogin = data.optBoolean("showToastOnLogin", false),
      toastAlertLevel = data.optFirstNonBlank("toastAlertLevel", default = "info"),
      isScheduledNotification = data.optBoolean("isScheduledNotification", false),
      scheduledNotificationDate = data.optNullableTimestampString("scheduledNotificationDate"),
      posterUrl = resolvedPoster,
      imageUrl = resolvedImage,
      serviceId = data.optNullableString("serviceId"),
      authorId = data.optString("authorId"),
      authorName = data.optString("authorName"),
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
