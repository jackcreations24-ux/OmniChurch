package com.zoutiw.omnichurch.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

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
    if (isPermanentlyLocked(now) || hasPassedEndTime(now)) return true
    val startMs = scheduledStartMillis() ?: return false
    return now > (startMs + 3_600_000L * 3)
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

sealed interface ScannerValidationOutcome {
  data object Idle : ScannerValidationOutcome

  data object Processing : ScannerValidationOutcome

  data class RegisteredSuccess(
    val record: AttendanceRecord,
    val member: Member,
    val service: ChurchService,
    val formattedTime: String,
  ) : ScannerValidationOutcome

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

fun ChurchService.isServiceEnded(nowMillis: Long = System.currentTimeMillis()): Boolean {
  if (isPermanentlyLocked(nowMillis) || status.equals("ended", ignoreCase = true) || status.equals("closed", ignoreCase = true)) {
    return true
  }
  if (hasPassedEndTime(nowMillis)) return true
  val startMs = scheduledStartMillis() ?: return false
  return nowMillis > (startMs + 4 * 3_600_000L)
}

fun parseServiceDateCalendar(rawDate: String): java.util.Calendar? {
  val trimmed = rawDate.trim()
  if (trimmed.isBlank()) return null
  val cleaned = trimmed.substringAfter(":", trimmed).trim().ifBlank { trimmed }
  val inputsToTry = listOf(trimmed, cleaned).distinct()
  val patterns =
    listOf(
      "yyyy-MM-dd",
      "dd/MM/yyyy",
      "dd-MM-yyyy",
      "dd:MM:yyyy",
      "yyyy/MM/dd",
      "dd MMM yyyy",
      "MMM dd, yyyy",
      "d MMM yyyy",
      "d MMMM yyyy",
      "MMMM d, yyyy",
    )
  val locales = listOf(java.util.Locale.US, java.util.Locale.FRENCH, java.util.Locale.getDefault())
  for (input in inputsToTry) {
    for (pattern in patterns) {
      for (locale in locales) {
        try {
          val sdf = java.text.SimpleDateFormat(pattern, locale).apply { isLenient = false }
          val parsed = sdf.parse(input)
          if (parsed != null) {
            return java.util.Calendar.getInstance().apply { time = parsed }
          }
        } catch (_: Exception) {
          // Try next pattern
        }
      }
    }
  }
  return null
}

fun parseClockHourMinute24(rawTime: String): Pair<Int, Int>? {
  val trimmed = rawTime.trim()
  if (trimmed.isBlank()) return null
  val amPmMatch =
    Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(AM|PM)$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
  if (amPmMatch != null) {
    val h12 = amPmMatch.groupValues[1].toIntOrNull() ?: return null
    val m = amPmMatch.groupValues[2].toIntOrNull() ?: return null
    val amPm = amPmMatch.groupValues[3].uppercase(java.util.Locale.US)
    if (h12 !in 1..12 || m !in 0..59) return null
    val h24 =
      when {
        amPm == "AM" && h12 == 12 -> 0
        amPm == "PM" && h12 < 12 -> h12 + 12
        else -> h12
      }
    return h24 to m
  }
  val match24 = Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$").matchEntire(trimmed) ?: return null
  val h24 = match24.groupValues[1].toIntOrNull() ?: return null
  val m = match24.groupValues[2].toIntOrNull() ?: return null
  if (h24 !in 0..23 || m !in 0..59) return null
  return h24 to m
}

/**
 * Formats any 24h or 12h clock string (or time range like "19:00 - 21:00") with explicit
 * 12h AM/PM precision without leading zero on single-digit hours (e.g. "19:00" -> "7:00 PM").
 */
fun formatServiceClockAmPm(rawTime: String): String {
  val trimmed = rawTime.trim()
  if (trimmed.isBlank()) return ""
  if (trimmed.contains(" - ")) {
    val parts = trimmed.split(" - ").map { formatServiceClockAmPm(it) }.filter { it.isNotBlank() }
    return parts.joinToString(" - ")
  }
  val parsed = parseClockHourMinute24(trimmed) ?: return trimmed
  val (hour24, minute) = parsed
  val amPm = if (hour24 >= 12) "PM" else "AM"
  val hour12 =
    when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
  return "$hour12:${minute.toString().padStart(2, '0')} $amPm"
}

/**
 * Formats a calendar date with its localized full weekday, day number, month name, and year:
 * - HT: "lendi 23 novanm 2026"
 * - FR: "lundi 23 novembre 2026"
 * - ES: "lunes 23 de noviembre 2026"
 * - EN: "Monday, November 23, 2026"
 */
fun formatLocalizedCalendarDate(cal: java.util.Calendar, languageCode: String = "ht"): String {
  val lang = languageCode.trim().lowercase(java.util.Locale.US)
  val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
  val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
  val monthIdx = cal.get(java.util.Calendar.MONTH)
  val year = cal.get(java.util.Calendar.YEAR)

  return when (lang) {
    "en" -> {
      val weekday =
        when (dow) {
          java.util.Calendar.SUNDAY -> "Sunday"
          java.util.Calendar.MONDAY -> "Monday"
          java.util.Calendar.TUESDAY -> "Tuesday"
          java.util.Calendar.WEDNESDAY -> "Wednesday"
          java.util.Calendar.THURSDAY -> "Thursday"
          java.util.Calendar.FRIDAY -> "Friday"
          java.util.Calendar.SATURDAY -> "Saturday"
          else -> ""
        }
      val month =
        listOf(
          "January", "February", "March", "April", "May", "June",
          "July", "August", "September", "October", "November", "December",
        ).getOrElse(monthIdx) { "" }
      "$weekday, $month $day, $year"
    }
    "fr" -> {
      val weekday =
        when (dow) {
          java.util.Calendar.SUNDAY -> "dimanche"
          java.util.Calendar.MONDAY -> "lundi"
          java.util.Calendar.TUESDAY -> "mardi"
          java.util.Calendar.WEDNESDAY -> "mercredi"
          java.util.Calendar.THURSDAY -> "jeudi"
          java.util.Calendar.FRIDAY -> "vendredi"
          java.util.Calendar.SATURDAY -> "samedi"
          else -> ""
        }
      val month =
        listOf(
          "janvier", "février", "mars", "avril", "mai", "juin",
          "juillet", "août", "septembre", "octobre", "novembre", "décembre",
        ).getOrElse(monthIdx) { "" }
      "$weekday $day $month $year"
    }
    "es" -> {
      val weekday =
        when (dow) {
          java.util.Calendar.SUNDAY -> "domingo"
          java.util.Calendar.MONDAY -> "lunes"
          java.util.Calendar.TUESDAY -> "martes"
          java.util.Calendar.WEDNESDAY -> "miércoles"
          java.util.Calendar.THURSDAY -> "jueves"
          java.util.Calendar.FRIDAY -> "viernes"
          java.util.Calendar.SATURDAY -> "sábado"
          else -> ""
        }
      val month =
        listOf(
          "enero", "febrero", "marzo", "abril", "mayo", "junio",
          "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        ).getOrElse(monthIdx) { "" }
      "$weekday $day de $month $year"
    }
    else -> {
      val weekday =
        when (dow) {
          java.util.Calendar.SUNDAY -> "dimanch"
          java.util.Calendar.MONDAY -> "lendi"
          java.util.Calendar.TUESDAY -> "madi"
          java.util.Calendar.WEDNESDAY -> "mèkredi"
          java.util.Calendar.THURSDAY -> "jedi"
          java.util.Calendar.FRIDAY -> "vandredi"
          java.util.Calendar.SATURDAY -> "samdi"
          else -> ""
        }
      val month =
        listOf(
          "janvye", "fevriye", "mas", "avril", "me", "jen",
          "jiyè", "out", "septanm", "oktòb", "novanm", "desanm",
        ).getOrElse(monthIdx) { "" }
      "$weekday $day $month $year"
    }
  }
}

fun formatLocalizedServiceDate(isoDate: String, languageCode: String = "ht"): String {
  val trimmed = isoDate.trim()
  if (trimmed.isBlank()) return ""
  val cal = parseServiceDateCalendar(trimmed) ?: return trimmed
  return formatLocalizedCalendarDate(cal, languageCode)
}

fun formatCreoleDateWithDay(isoDate: String): String =
  formatLocalizedServiceDate(isoDate, "ht").ifBlank { "Dat Sèvis" }

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

data class MemberAttendanceHistoryItem(
  val id: String = "",
  val dateLabel: String = "",
  val timeLabel: String = "",
  val serviceTitle: String = "",
  val status: MemberAttendanceStatus = MemberAttendanceStatus.PRESENT,
  val churchId: String = "",
)

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
