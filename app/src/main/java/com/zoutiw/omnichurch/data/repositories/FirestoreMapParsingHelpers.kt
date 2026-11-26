package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object FirestoreMapParsingHelpers {
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

  fun formatIsoUtc(date: Date): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(date)
  }

  fun parseIsoToDate(raw: String): Date? {
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

  fun mapToTimestamp(map: Map<*, *>): Timestamp? {
    val rawSeconds = map["seconds"] ?: map["_seconds"] ?: return null
    val seconds = (rawSeconds as? Number)?.toLong() ?: rawSeconds.toString().toLongOrNull() ?: return null
    val rawNanos = map["nanoseconds"] ?: map["_nanoseconds"] ?: 0
    val nanos = (rawNanos as? Number)?.toInt() ?: rawNanos.toString().toIntOrNull() ?: 0
    return Timestamp(seconds, nanos.coerceIn(0, 999_999_999))
  }
}

internal fun Map<String, Any?>.optString(key: String, default: String = ""): String {
  val value = this[key] ?: return default
  return when (value) {
    is String -> value.ifEmpty { default }
    is DocumentReference -> value.id.ifEmpty { default }
    is Number,
    is Boolean -> value.toString()
    else -> default
  }
}

internal fun Map<String, Any?>.optFirstNonBlank(vararg keys: String, default: String = ""): String {
  for (key in keys) {
    val candidate = optString(key, "").trim()
    if (candidate.isNotEmpty()) return candidate
  }
  return default
}

internal fun Map<String, Any?>.optNullableString(key: String): String? {
  val value = this[key] ?: return null
  return when (value) {
    is String -> value.takeIf { it.isNotEmpty() }
    is DocumentReference -> value.id.takeIf { it.isNotEmpty() }
    is Number,
    is Boolean -> value.toString()
    else -> null
  }
}

internal fun Map<String, Any?>.optFirstNullableString(vararg keys: String): String? {
  for (key in keys) {
    val candidate = optNullableString(key)?.trim()
    if (!candidate.isNullOrEmpty()) return candidate
  }
  return null
}

internal fun Map<String, Any?>.optBoolean(key: String, default: Boolean = false): Boolean {
  val value = this[key] ?: return default
  return when (value) {
    is Boolean -> value
    is Number -> value.toLong() != 0L
    is String ->
      when (value.trim().lowercase()) {
        "true", "1", "yes", "wi" -> true
        "false", "0", "no", "non" -> false
        else -> default
      }
    else -> default
  }
}

internal fun Map<String, Any?>.optInt(key: String, default: Int = 0): Int {
  val value = this[key] ?: return default
  return when (value) {
    is Number -> value.toInt()
    is String -> value.trim().toIntOrNull() ?: value.trim().toDoubleOrNull()?.toInt() ?: default
    else -> default
  }
}

internal fun Map<String, Any?>.optLong(key: String, default: Long = 0L): Long {
  val value = this[key] ?: return default
  return when (value) {
    is Number -> value.toLong()
    is String -> value.trim().toLongOrNull() ?: value.trim().toDoubleOrNull()?.toLong() ?: default
    else -> default
  }
}

internal fun Map<String, Any?>.optTimestamp(key: String): Timestamp? {
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
    is Map<*, *> -> FirestoreMapParsingHelpers.mapToTimestamp(value)
    is String -> FirestoreMapParsingHelpers.parseIsoToDate(value)?.let { Timestamp(it) }
    else -> null
  }
}

internal fun Map<String, Any?>.optTimestampString(key: String): String {
  val value = this[key] ?: return ""
  return when (value) {
    is String -> value.trim()
    is Timestamp -> FirestoreMapParsingHelpers.formatIsoUtc(value.toDate())
    is Date -> FirestoreMapParsingHelpers.formatIsoUtc(value)
    is Number -> {
      val raw = value.toLong()
      if (raw <= 0L) "" else FirestoreMapParsingHelpers.formatIsoUtc(Date(if (raw < 10_000_000_000L) raw * 1000L else raw))
    }
    is Map<*, *> -> FirestoreMapParsingHelpers.mapToTimestamp(value)?.toDate()?.let { FirestoreMapParsingHelpers.formatIsoUtc(it) }.orEmpty()
    else -> ""
  }
}

internal fun Map<String, Any?>.optNullableTimestampString(key: String): String? =
  optTimestampString(key).takeIf { it.isNotEmpty() }

internal fun Map<String, Any?>.optEpochMillisOrNull(key: String): Long? {
  val value = this[key] ?: return null
  return when (value) {
    is Number -> {
      val raw = value.toLong()
      if (raw <= 0L) null else if (raw < 10_000_000_000L) raw * 1000L else raw
    }
    is Timestamp -> value.toDate().time
    is Date -> value.time
    is Map<*, *> -> FirestoreMapParsingHelpers.mapToTimestamp(value)?.toDate()?.time
    is String -> FirestoreMapParsingHelpers.parseIsoToDate(value)?.time
    else -> null
  }
}

internal fun Map<String, Any?>.optEpochMillis(key: String, default: Long): Long =
  optEpochMillisOrNull(key) ?: default
