package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FieldValue
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.formatServiceClockAmPm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean Architecture UseCase encapsulating service schedule validation, start/end window checks,
 * and sanitized Firestore create/update payload construction.
 */
class ServiceScheduleAndPayloadUseCase {

  fun parseDateTimeMillis(date: String, time: String): Long? {
    val cleanDate = date.trim()
    if (cleanDate.isEmpty()) return null
    val cleanTime = time.trim().replace("h", ":", ignoreCase = true)
    val candidates =
      if (cleanTime.contains(":")) {
        listOf(
          "yyyy-MM-dd hh:mm a" to "$cleanDate ${cleanTime.uppercase(Locale.US)}",
          "yyyy-MM-dd HH:mm" to "$cleanDate $cleanTime",
        )
      } else {
        listOf("yyyy-MM-dd" to cleanDate)
      }
    for ((pattern, value) in candidates) {
      try {
        val parsed = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)?.time
        if (parsed != null) return parsed
      } catch (_: Exception) {
        // Try next pattern
      }
    }
    return null
  }

  fun isAlreadyEndedInPast(
    date: String,
    startTime: String,
    endTime: String,
    now: Long = System.currentTimeMillis(),
  ): Boolean {
    val cleanDate = date.trim()
    if (cleanDate.isEmpty()) return false
    val endMillis =
      if (endTime.trim().isNotEmpty()) {
        parseDateTimeMillis(cleanDate, endTime)
      } else if (startTime.trim().isNotEmpty()) {
        parseDateTimeMillis(cleanDate, startTime)?.plus(3 * 3_600_000L)
      } else {
        parseDateTimeMillis(cleanDate, "23:59")
      }
    return endMillis != null && endMillis < now
  }

  fun computeStartEpochMillis(
    date: String,
    startTime: String,
    fallbackNow: Long = System.currentTimeMillis(),
  ): Long {
    val parsedMillis = parseDateTimeMillis(date, startTime)
    if (parsedMillis != null && parsedMillis > 0L) return parsedMillis
    return fallbackNow
  }

  fun isBeforeScheduledStart(
    date: String,
    startTime: String,
    now: Long = System.currentTimeMillis(),
  ): Boolean {
    val startMillis = parseDateTimeMillis(date, startTime) ?: return false
    return now < startMillis
  }

  fun validateAttendanceToggle(
    liveService: ChurchService,
    isOpen: Boolean,
    now: Long = System.currentTimeMillis(),
  ): Result<Unit> {
    if (liveService.isPermanentlyLocked(now)) {
      return Result.failure(
        IllegalStateException("Sèvis sa a deja fini nèt — prezans lan pa ka re-ouvri ankò.")
      )
    }
    if (isOpen && liveService.isBeforeStart(now)) {
      val clockStr = formatServiceClockAmPm(liveService.startTime).ifBlank { liveService.startTime }
      return Result.failure(
        IllegalStateException("Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr).")
      )
    }
    if (isOpen && liveService.hasPassedEndTime(now)) {
      return Result.failure(
        IllegalStateException("Sèvis sa a deja depase lè fini li — prezans lan pa ka louvri.")
      )
    }
    return Result.success(Unit)
  }

  fun buildSanitizedCreatePayload(
    docId: String,
    uid: String,
    churchId: String,
    churchName: String,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    authorName: String,
    category: String,
    cleanDate: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
    sendReminder24h: Boolean,
    imageUrl: String?,
  ): Pair<Map<String, Any>, String> {
    val cleanChurchName = churchName.trim().ifBlank { "OmniChurch Pro" }.take(200)
    val cleanAuthorName = authorName.trim().ifBlank { "Admin" }.take(200)
    val cleanCategory = category.trim().ifBlank { "dimanche_matin" }
    val cleanDateFormatted =
      dateFormatted.trim().ifBlank {
        val formattedTime = formatServiceClockAmPm(startTime)
        if (formattedTime.isNotBlank()) "$cleanDate • $formattedTime" else cleanDate
      }.take(150)
    val cleanImageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() && it.length <= 600_000 }
    val startEpochMillis = computeStartEpochMillis(cleanDate, startTime)
    val beforeStart = isBeforeScheduledStart(cleanDate, startTime)
    val effectiveIsOpen = isOpenForAttendance && !beforeStart && !isAlreadyEndedInPast(cleanDate, startTime, endTime)
    val effectiveIsLive = isLive && effectiveIsOpen

    val rawPayload =
      hashMapOf<String, Any?>(
        "id" to docId,
        "churchId" to churchId,
        "churchName" to cleanChurchName,
        "category" to cleanCategory,
        "title" to title,
        "name" to title,
        "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(3000),
        "dateFormatted" to cleanDateFormatted,
        "date" to cleanDate,
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "sendReminder24h" to sendReminder24h,
        "reminderSentAt" to if (sendReminder24h) FirestorePayloadSanitizer.isoUtcNow() else null,
        "imageUrl" to cleanImageUrl,
        "type" to cleanCategory,
        "status" to "active",
        "startTimeEpochMillis" to startEpochMillis,
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 360L),
        "isOpenForAttendance" to effectiveIsOpen,
        "wasOpenedForAttendance" to effectiveIsOpen,
        "isPermanentlyEnded" to false,
        "isLive" to effectiveIsLive,
        "authorId" to uid,
        "authorName" to cleanAuthorName,
        "createdBy" to uid,
        "updatedBy" to uid,
        "reactionsCount" to 0L,
        "commentsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return FirestorePayloadSanitizer.sanitizeServiceCreate(rawPayload) to cleanDateFormatted
  }

  fun buildSanitizedUpdatePayload(
    uid: String,
    cleanTitle: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    category: String,
    cleanDate: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
    sendReminder24h: Boolean?,
    imageUrl: String?,
    notes: String?,
    reminderSentAt: String?,
  ): Map<String, Any?> {
    val cleanCategory = category.trim().ifBlank { "dimanche_matin" }
    val beforeStart = isBeforeScheduledStart(cleanDate, startTime)
    val endedInPast = isAlreadyEndedInPast(cleanDate, startTime, endTime)
    val effectiveIsOpen = isOpenForAttendance && !beforeStart && !endedInPast
    val effectiveIsLive = isLive && effectiveIsOpen
    val updatedStartEpochMillis = computeStartEpochMillis(cleanDate, startTime)

    val cleanDateFormatted =
      dateFormatted.trim().ifBlank {
        val formattedTime = formatServiceClockAmPm(startTime)
        if (formattedTime.isNotBlank()) "$cleanDate • $formattedTime" else cleanDate
      }.take(150)

    val rawPayload =
      hashMapOf<String, Any?>(
        "title" to cleanTitle,
        "name" to cleanTitle,
        "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(3000),
        "dateFormatted" to cleanDateFormatted,
        "date" to cleanDate,
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "startTimeEpochMillis" to updatedStartEpochMillis,
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "category" to cleanCategory,
        "type" to cleanCategory,
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 360L),
        "isOpenForAttendance" to effectiveIsOpen,
        "isLive" to effectiveIsLive,
        "updatedBy" to uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (effectiveIsOpen) {
      rawPayload["wasOpenedForAttendance"] = true
    }
    if (sendReminder24h != null) {
      rawPayload["sendReminder24h"] = sendReminder24h
    }
    if (reminderSentAt != null) {
      rawPayload["reminderSentAt"] = reminderSentAt.trim().takeIf { it.isNotBlank() }
    }
    if (notes != null) {
      rawPayload["notes"] = notes.trim()
    }
    if (imageUrl != null) {
      rawPayload["imageUrl"] = imageUrl.trim().takeIf { it.isNotBlank() && it.length <= 600_000 }
    }

    return FirestorePayloadSanitizer.sanitizeServiceUpdate(
      raw = rawPayload,
      isPermanentlyEnded = false,
    )
  }
}
