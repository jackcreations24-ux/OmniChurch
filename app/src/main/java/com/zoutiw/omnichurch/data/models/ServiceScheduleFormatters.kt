package com.zoutiw.omnichurch.data.models

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val ClockAmPmRegex =
  Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?\\s*(AM|PM)$", RegexOption.IGNORE_CASE)
private val Clock24Regex =
  Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$")

private val ServiceDatePatterns =
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

private val MonthsEn =
  listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December",
  )

private val MonthsFr =
  listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
  )

private val MonthsEs =
  listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
  )

private val MonthsHt =
  listOf(
    "janvye", "fevriye", "mas", "avril", "me", "jen",
    "jiyè", "out", "septanm", "oktòb", "novanm", "desanm",
  )

fun parseServiceDateCalendar(rawDate: String): Calendar? {
  val trimmed = rawDate.trim()
  if (trimmed.isBlank()) return null
  val cleaned = trimmed.substringAfter(":", trimmed).trim().ifBlank { trimmed }
  val inputsToTry = listOf(trimmed, cleaned).distinct()
  val locales = listOf(Locale.US, Locale.FRENCH, Locale.getDefault())
  for (input in inputsToTry) {
    for (pattern in ServiceDatePatterns) {
      for (locale in locales) {
        try {
          val sdf = SimpleDateFormat(pattern, locale).apply { isLenient = false }
          val parsed = sdf.parse(input)
          if (parsed != null) {
            return Calendar.getInstance().apply { time = parsed }
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
  val amPmMatch = ClockAmPmRegex.matchEntire(trimmed)
  if (amPmMatch != null) {
    val h12 = amPmMatch.groupValues[1].toIntOrNull() ?: return null
    val m = amPmMatch.groupValues[2].toIntOrNull() ?: return null
    val amPm = amPmMatch.groupValues[3].uppercase(Locale.US)
    if (h12 !in 1..12 || m !in 0..59) return null
    val h24 =
      when {
        amPm == "AM" && h12 == 12 -> 0
        amPm == "PM" && h12 < 12 -> h12 + 12
        else -> h12
      }
    return h24 to m
  }
  val match24 = Clock24Regex.matchEntire(trimmed) ?: return null
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
fun formatLocalizedCalendarDate(cal: Calendar, languageCode: String = "ht"): String {
  val lang = languageCode.trim().lowercase(Locale.US)
  val dow = cal.get(Calendar.DAY_OF_WEEK)
  val day = cal.get(Calendar.DAY_OF_MONTH)
  val monthIdx = cal.get(Calendar.MONTH)
  val year = cal.get(Calendar.YEAR)

  return when (lang) {
    "en" -> {
      val weekday =
        when (dow) {
          Calendar.SUNDAY -> "Sunday"
          Calendar.MONDAY -> "Monday"
          Calendar.TUESDAY -> "Tuesday"
          Calendar.WEDNESDAY -> "Wednesday"
          Calendar.THURSDAY -> "Thursday"
          Calendar.FRIDAY -> "Friday"
          Calendar.SATURDAY -> "Saturday"
          else -> ""
        }
      val month = MonthsEn.getOrElse(monthIdx) { "" }
      "$weekday, $month $day, $year"
    }
    "fr" -> {
      val weekday =
        when (dow) {
          Calendar.SUNDAY -> "dimanche"
          Calendar.MONDAY -> "lundi"
          Calendar.TUESDAY -> "mardi"
          Calendar.WEDNESDAY -> "mercredi"
          Calendar.THURSDAY -> "jeudi"
          Calendar.FRIDAY -> "vendredi"
          Calendar.SATURDAY -> "samedi"
          else -> ""
        }
      val month = MonthsFr.getOrElse(monthIdx) { "" }
      "$weekday $day $month $year"
    }
    "es" -> {
      val weekday =
        when (dow) {
          Calendar.SUNDAY -> "domingo"
          Calendar.MONDAY -> "lunes"
          Calendar.TUESDAY -> "martes"
          Calendar.WEDNESDAY -> "miércoles"
          Calendar.THURSDAY -> "jueves"
          Calendar.FRIDAY -> "viernes"
          Calendar.SATURDAY -> "sábado"
          else -> ""
        }
      val month = MonthsEs.getOrElse(monthIdx) { "" }
      "$weekday $day de $month $year"
    }
    else -> {
      val weekday =
        when (dow) {
          Calendar.SUNDAY -> "dimanch"
          Calendar.MONDAY -> "lendi"
          Calendar.TUESDAY -> "madi"
          Calendar.WEDNESDAY -> "mèkredi"
          Calendar.THURSDAY -> "jedi"
          Calendar.FRIDAY -> "vandredi"
          Calendar.SATURDAY -> "samdi"
          else -> ""
        }
      val month = MonthsHt.getOrElse(monthIdx) { "" }
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
