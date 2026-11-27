package com.zoutiw.omnichurch.data.models

import androidx.compose.runtime.Immutable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@Immutable
@IgnoreExtraProperties
data class ChurchService(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val churchName: String = "",
  val category: String = "dimanche_matin",
  val title: String = "",
  val name: String = "",
  val description: String? = null,
  val imageUrl: String? = null,
  val dateFormatted: String = "",
  val date: String = "",
  val startTime: String = "",
  val endTime: String = "",
  val theme: String? = null,
  val passage: String? = null,
  val leader: String? = null,
  val preacher: String? = null,
  val reader: String? = null,
  val musicians: String? = null,
  val ushers: String? = null,
  val sendReminder24h: Boolean = false,
  val type: String = "dimanche_matin",
  val status: String = "active",
  val startTimeEpochMillis: Long = 0L,
  val lateToleranceMinutes: Long = 15L,
  @JvmField @field:PropertyName("isOpenForAttendance") val isOpenForAttendance: Boolean = false,
  @JvmField @field:PropertyName("wasOpenedForAttendance") val wasOpenedForAttendance: Boolean = false,
  @JvmField @field:PropertyName("isPermanentlyEnded") val isPermanentlyEnded: Boolean = false,
  @JvmField @field:PropertyName("isLive") val isLive: Boolean = false,
  val endedAt: String? = null,
  val reminderSentAt: String? = null,
  val notes: String? = null,
  val authorId: String = "",
  val authorName: String = "",
  val createdBy: String = "",
  val reactionsCount: Long = 0L,
  val commentsCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val effectiveTitle: String
    get() = title.ifBlank { name }

  val effectiveDateOnly: String
    get() = date.ifBlank { dateFormatted }

  val effectiveImageUrl: String?
    get() = imageUrl?.trim()?.takeIf { it.isNotEmpty() }

  val effectiveAuthorUid: String
    get() = authorId.trim().ifBlank { createdBy.trim() }

  fun isPermanentlyLocked(now: Long = System.currentTimeMillis()): Boolean =
    isPermanentlyEnded || status.equals("locked", ignoreCase = true) || status.equals("archived", ignoreCase = true)

  fun scheduledStartMillis(): Long? {
    val dateCal = resolveScheduleCalendar()
    val parsedStart = parseClockHourMinute24(startTime)
    if (dateCal != null && parsedStart != null) {
      return (dateCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, parsedStart.first)
        set(java.util.Calendar.MINUTE, parsedStart.second)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }.timeInMillis
    }
    if (startTimeEpochMillis > 0L) return startTimeEpochMillis
    return null
  }

  fun scheduledEndMillis(): Long? {
    val dateCal = resolveScheduleCalendar() ?: return null
    val parsedEnd = parseClockHourMinute24(endTime) ?: return null
    val endCal =
      (dateCal.clone() as java.util.Calendar).apply {
        set(java.util.Calendar.HOUR_OF_DAY, parsedEnd.first)
        set(java.util.Calendar.MINUTE, parsedEnd.second)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
      }
    val startMs = scheduledStartMillis()
    if (startMs != null && endCal.timeInMillis <= startMs) {
      endCal.add(java.util.Calendar.DAY_OF_YEAR, 1)
    }
    return endCal.timeInMillis
  }

  fun isBeforeStart(now: Long = System.currentTimeMillis()): Boolean {
    val startMs = scheduledStartMillis() ?: return false
    return now < startMs
  }

  fun hasPassedEndTime(now: Long = System.currentTimeMillis()): Boolean {
    val endMs = scheduledEndMillis() ?: return false
    return now > endMs
  }

  fun isLiveBadgeActive(now: Long = System.currentTimeMillis()): Boolean =
    isOpenForAttendance &&
      !isPermanentlyLocked(now) &&
      !hasPassedEndTime(now) &&
      !isBeforeStart(now)

  fun isTimerTicking(now: Long = System.currentTimeMillis()): Boolean =
    isLiveBadgeActive(now)

  fun isServiceEnded(nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (isPermanentlyLocked(nowMillis) || status.equals("ended", ignoreCase = true) || status.equals("closed", ignoreCase = true)) {
      return true
    }
    if (hasPassedEndTime(nowMillis)) return true
    val startMs = scheduledStartMillis() ?: return false
    return nowMillis > (startMs + 4 * 3_600_000L)
  }

  fun isFinishedForAbsence(now: Long = System.currentTimeMillis()): Boolean {
    if (isBeforeStart(now)) return false
    if (
      isPermanentlyLocked(now) ||
        status.equals("ended", ignoreCase = true) ||
        status.equals("closed", ignoreCase = true) ||
        hasPassedEndTime(now)
    ) {
      return true
    }
    val startMs = scheduledStartMillis() ?: return false
    return (!isOpenForAttendance && now > (startMs + 3_600_000L * 3)) ||
      now > (startMs + 3_600_000L * 4)
  }

  val elapsedTimerLabel: String
    get() = elapsedTimerLabel(System.currentTimeMillis())

  fun elapsedTimerLabel(now: Long = System.currentTimeMillis()): String {
    if (isLiveBadgeActive(now)) {
      val startMs = scheduledStartMillis()
      if (startMs != null && now >= startMs) {
        val elapsedSec = ((now - startMs) / 1000L).coerceAtLeast(0L)
        val hrs = elapsedSec / 3600L
        val mins = (elapsedSec % 3600L) / 60L
        val secs = elapsedSec % 60L
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
      }
      return "LIVE"
    }
    if (isBeforeStart(now)) {
      val startClock = formatServiceClockAmPm(startTime)
      if (startClock.isNotBlank()) return startClock
    }
    return dateFormatted.ifBlank { "Sèvis" }
  }

  val effectiveScheduleLabel: String
    get() = formattedScheduleWithDay("ht")

  fun effectiveScheduleLabel(now: Long = System.currentTimeMillis()): String = formattedScheduleWithDay("ht")

  fun formattedTimeRangeAmPm(): String {
    val startFormatted = formatServiceClockAmPm(startTime)
    val endFormatted = formatServiceClockAmPm(endTime)
    return when {
      startFormatted.isNotBlank() && endFormatted.isNotBlank() && !endFormatted.equals(startFormatted, ignoreCase = true) ->
        "$startFormatted - $endFormatted"
      startFormatted.isNotBlank() -> startFormatted
      startTimeEpochMillis > 0L ->
        formatServiceClockAmPm(
          java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(startTimeEpochMillis))
        )
      else -> ""
    }
  }

  fun formattedScheduleWithDay(languageCode: String = "ht"): String {
    val cal = resolveScheduleCalendar()
    val datePart =
      if (cal != null) {
        formatLocalizedCalendarDate(cal, languageCode)
      } else {
        dateFormatted.ifBlank { date }.trim()
      }

    val timePart = formattedTimeRangeAmPm()

    return when {
      datePart.isNotBlank() && timePart.isNotBlank() -> "$datePart • $timePart"
      datePart.isNotBlank() -> datePart
      timePart.isNotBlank() -> timePart
      else -> dateFormatted.ifBlank { date }
    }
  }

  private fun resolveScheduleCalendar(): java.util.Calendar? {
    val candidates =
      listOf(
        date.trim(),
        dateFormatted.substringBefore("•").trim(),
        dateFormatted.trim(),
      ).filter { it.isNotBlank() }.distinct()

    for (raw in candidates) {
      parseServiceDateCalendar(raw)?.let { return it }
    }

    if (startTimeEpochMillis > 0L) {
      return java.util.Calendar.getInstance().apply { timeInMillis = startTimeEpochMillis }
    }
    return null
  }

  val categoryLabelHt: String
    get() = when (category.trim().lowercase()) {
      "dimanche_matin", "sunday_morning", "dimanch maten" -> "Dimanch Maten"
      "jeudi_soir", "thursday_night", "jedi swè" -> "Jedi Swè"
      "jeunesse", "youth", "jennès" -> "Jèn yo"
      "priere", "prayer", "lapriyè" -> "Reinyon Lapriyè"
      "jeune", "fasting", "jen" -> "Jèn ak Lapriyè"
      else -> category.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
    }
}

@Immutable
@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val id: String = "",
  val churchId: String = "",
  val serviceId: String = "",
  val serviceName: String = "",
  val memberId: String = "",
  val memberDocId: String = "",
  val memberCode: String = "",
  val memberName: String = "",
  val memberPhotoUrl: String? = null,
  val userId: String = "",
  val recordedByUid: String = "",
  val status: String = "on_time",
  val checkInMethod: String = "qr_scanner",
  val barcodeToken: String = "",
  val attendanceRole: String = "member",
  @JvmField @field:PropertyName("isVisitor") val isVisitor: Boolean = false,
  val homeChurchId: String? = null,
  val homeChurchName: String? = null,
  val timestamp: Long = System.currentTimeMillis(),
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  val effectiveStatus: String
    get() = status.trim().ifEmpty { "on_time" }

  val isCrossChurchVisitor: Boolean
    get() =
      isVisitor ||
        attendanceRole.equals("visitor", ignoreCase = true) ||
        (!homeChurchId.isNullOrBlank() &&
          churchId.isNotBlank() &&
          !homeChurchId.trim().equals(churchId.trim(), ignoreCase = true))

  val effectiveMemberDbId: String
    get() = memberDocId.trim().ifEmpty { memberId.trim().ifEmpty { id } }

  val effectiveMemberCode: String
    get() = memberCode.trim().ifEmpty { memberId }
}

@Immutable
sealed interface ScannerValidationOutcome {
  data object Idle : ScannerValidationOutcome

  data object Processing : ScannerValidationOutcome

  @Immutable
  data class RegisteredSuccess(
    val record: AttendanceRecord,
    val member: Member,
    val service: ChurchService,
    val formattedTime: String,
  ) : ScannerValidationOutcome

  @Immutable
  data class ValidationError(val errorType: AttendanceErrorType, val details: String? = null) :
    ScannerValidationOutcome
}

enum class AttendanceErrorType {
  INVALID_QR,
  WRONG_CHURCH,
  DUPLICATE_ATTENDANCE,
  SERVICE_CLOSED,
  UNAUTHORIZED,
  NETWORK_ERROR,
}

enum class MemberAttendanceStatus {
  PRESENT, LATE, ABSENT, EXCUSED;

  val labelHt: String
    get() = when (this) {
      PRESENT -> "Prezan"
      LATE -> "An Retè"
      ABSENT -> "Absan"
      EXCUSED -> "Ekskize"
    }
}

@Immutable
data class MemberAttendanceHistoryItem(
  val id: String = "",
  val dateLabel: String = "",
  val timeLabel: String = "",
  val serviceTitle: String = "",
  val status: MemberAttendanceStatus = MemberAttendanceStatus.PRESENT,
  val churchId: String = "",
)

@Immutable
data class CrossChurchVisitItem(
  val id: String = "",
  val hostChurchName: String = "",
  val dateLabel: String = "",
  val timeLabel: String = "",
  val serviceTitle: String = "",
  val churchId: String = "",
)

fun ChurchService.isFinishedForAbsence(): Boolean =
  isFinishedForAbsence(System.currentTimeMillis())

