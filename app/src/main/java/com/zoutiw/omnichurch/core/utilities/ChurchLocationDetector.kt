package com.zoutiw.omnichurch.core.utilities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class DetectedChurchLocation(
  val countryCode: String,
  val countryName: String,
  val cityHint: String,
  val formattedDefaultCity: String,
  val source: String,
)

/**
 * Multi-layer, zero-crash church country & city detector (Offline IANA Timezone / Locale +
 * Background Geo-IP + On-Demand GPS / Network Reverse Geocoding) with 4-language (`ht`, `fr`, `en`, `es`)
 * localization and smart non-duplicating location formatting.
 */
object ChurchLocationDetector {

  private const val PREFS_NAME = "omnichurch_geo_location_prefs"
  private const val KEY_GEO_COUNTRY_V1 = "omnichurch_detected_geo_country_v1"
  private const val KEY_GEO_CITY_V1 = "omnichurch_detected_geo_city_v1"
  private const val KEY_GEO_TS_V1 = "omnichurch_detected_geo_ts_v1"
  private const val CACHE_TTL_MS = 24L * 60L * 60L * 1000L // 24 hours

  @Volatile private var memoryCachedCountryCode: String? = null
  @Volatile private var memoryCachedCityHint: String? = null
  @Volatile private var memoryCachedTimestampMs: Long = 0L

  private val TIMEZONE_TO_COUNTRY: Map<String, String> =
    mapOf(
      "America/Port-au-Prince" to "HT",
      "America/Santo_Domingo" to "DO",
      "America/New_York" to "US",
      "America/Miami" to "US",
      "America/Detroit" to "US",
      "America/Kentucky/Louisville" to "US",
      "America/Indiana/Indianapolis" to "US",
      "America/Chicago" to "US",
      "America/Denver" to "US",
      "America/Phoenix" to "US",
      "America/Los_Angeles" to "US",
      "America/Anchorage" to "US",
      "Pacific/Honolulu" to "US",
      "America/Toronto" to "CA",
      "America/Montreal" to "CA",
      "America/Vancouver" to "CA",
      "America/Halifax" to "CA",
      "America/Winnipeg" to "CA",
      "America/Edmonton" to "CA",
      "Europe/Paris" to "FR",
      "America/Martinique" to "MQ",
      "America/Guadeloupe" to "GP",
      "America/Cayenne" to "GF",
      "America/Santiago" to "CL",
      "America/Sao_Paulo" to "BR",
      "America/Fortaleza" to "BR",
      "America/Manaus" to "BR",
      "America/Mexico_City" to "MX",
      "America/Cancun" to "MX",
      "America/Tijuana" to "MX",
      "Europe/Madrid" to "ES",
      "America/Bogota" to "CO",
      "America/Lima" to "PE",
      "America/Panama" to "PA",
      "America/Jamaica" to "JM",
      "America/Nassau" to "BS",
      "America/Port_of_Spain" to "TT",
      "America/Havana" to "CU",
      "America/Puerto_Rico" to "PR",
      "Europe/London" to "GB",
      "Europe/Brussels" to "BE",
      "Europe/Zurich" to "CH",
    )

  private val KNOWN_COUNTRY_TOKENS: Set<String> =
    setOf(
      "ayiti",
      "haiti",
      "haïti",
      "haití",
      "repiblik dominiken",
      "republique dominicaine",
      "république dominicaine",
      "republica dominicana",
      "república dominicana",
      "dominican republic",
      "rep. dom",
      "rep dom",
      "r.d.",
      "usa",
      "u.s.a",
      "etazini",
      "etats-unis",
      "états-unis",
      "etats unis",
      "united states",
      "estados unidos",
      "ee.uu",
      "eeuu",
      "kanada",
      "canada",
      "canadá",
      "frans",
      "france",
      "francia",
      "espay",
      "espagne",
      "spain",
      "espana",
      "españa",
      "chili",
      "chile",
      "brezil",
      "bresil",
      "brésil",
      "brasil",
      "brazil",
      "meksik",
      "mexique",
      "mexico",
      "méxico",
      "gwadloup",
      "guadeloupe",
      "guadalupe",
      "matinik",
      "martinique",
      "martinica",
      "giyan fransez",
      "giyàn fransèz",
      "guyane francaise",
      "guyane française",
      "french guiana",
      "guayana francesa",
    )

  fun normalizeLanguage(language: String?): String {
    val clean = language?.trim()?.lowercase(Locale.US).orEmpty()
    return when {
      clean.startsWith("fr") -> "fr"
      clean.startsWith("en") -> "en"
      clean.startsWith("es") -> "es"
      else -> "ht"
    }
  }

  /**
   * Maps 2-letter ISO country code to localized country name in `ht`, `fr`, `en`, `es`.
   */
  fun getCountryNameFromCode(countryCode: String?, language: String = "ht"): String {
    val lang = normalizeLanguage(language)
    val code = countryCode?.trim()?.uppercase(Locale.US).orEmpty().ifBlank { "HT" }
    return when (code) {
      "HT" ->
        when (lang) {
          "fr" -> "Haïti"
          "en" -> "Haiti"
          "es" -> "Haití"
          else -> "Ayiti"
        }
      "DO" ->
        when (lang) {
          "fr" -> "République Dominicaine"
          "en" -> "Dominican Republic"
          "es" -> "República Dominicana"
          else -> "Repiblik Dominiken"
        }
      "US" ->
        when (lang) {
          "fr" -> "États-Unis (USA)"
          "en" -> "United States (USA)"
          "es" -> "Estados Unidos (EE.UU.)"
          else -> "Etazini (USA)"
        }
      "CA" ->
        when (lang) {
          "fr" -> "Canada"
          "en" -> "Canada"
          "es" -> "Canadá"
          else -> "Kanada"
        }
      "FR" ->
        when (lang) {
          "fr" -> "France"
          "en" -> "France"
          "es" -> "Francia"
          else -> "Frans"
        }
      "ES" ->
        when (lang) {
          "fr" -> "Espagne"
          "en" -> "Spain"
          "es" -> "España"
          else -> "Espay"
        }
      "CL" ->
        when (lang) {
          "fr" -> "Chili"
          "en" -> "Chile"
          "es" -> "Chile"
          else -> "Chili"
        }
      "BR" ->
        when (lang) {
          "fr" -> "Brésil"
          "en" -> "Brazil"
          "es" -> "Brasil"
          else -> "Brezil"
        }
      "MX" ->
        when (lang) {
          "fr" -> "Mexique"
          "en" -> "Mexico"
          "es" -> "México"
          else -> "Meksik"
        }
      "GP" ->
        when (lang) {
          "fr" -> "Guadeloupe"
          "en" -> "Guadeloupe"
          "es" -> "Guadalupe"
          else -> "Gwadloup"
        }
      "MQ" ->
        when (lang) {
          "fr" -> "Martinique"
          "en" -> "Martinique"
          "es" -> "Martinica"
          else -> "Matinik"
        }
      "GF" ->
        when (lang) {
          "fr" -> "Guyane Française"
          "en" -> "French Guiana"
          "es" -> "Guayana Francesa"
          else -> "Giyàn Fransèz"
        }
      else -> {
        if (code.length == 2 && code.all { it in 'A'..'Z' }) {
          val targetLocale =
            when (lang) {
              "fr" -> Locale.FRENCH
              "en" -> Locale.ENGLISH
              "es" -> Locale.forLanguageTag("es")
              else -> Locale.forLanguageTag("fr")
            }
          val display = Locale.forLanguageTag("und-$code").getDisplayCountry(targetLocale).trim()
          if (display.isNotBlank() && !display.equals(code, ignoreCase = true)) {
            display
          } else {
            getCountryNameFromCode("HT", lang)
          }
        } else {
          getCountryNameFromCode("HT", lang)
        }
      }
    }
  }

  /**
   * Extracts a localized city hint from an IANA timezone ID when available.
   */
  fun extractCityHintFromTimezone(
    timezoneId: String = TimeZone.getDefault().id.orEmpty(),
    language: String = "ht",
  ): String {
    val lang = normalizeLanguage(language)
    val tz = timezoneId.trim()
    if (tz.isBlank() || tz.equals("UTC", ignoreCase = true) || tz.startsWith("GMT", ignoreCase = true) || tz.startsWith("Etc/", ignoreCase = true)) {
      return ""
    }
    return when (tz) {
      "America/Port-au-Prince" ->
        when (lang) {
          "ht", "es" -> "Pòtoprens"
          else -> "Port-au-Prince"
        }
      "America/Santo_Domingo" -> "Santo Domingo"
      "America/New_York" -> "New York"
      "America/Miami" -> "Miami"
      "America/Chicago" -> "Chicago"
      "America/Los_Angeles" -> "Los Angeles"
      "America/Toronto" -> "Toronto"
      "America/Montreal" ->
        when (lang) {
          "fr" -> "Montréal"
          else -> "Montreal"
        }
      "Europe/Paris" -> "Paris"
      "America/Martinique" -> "Fort-de-France"
      "America/Guadeloupe" -> "Pointe-à-Pitre"
      "America/Cayenne" -> "Cayenne"
      "America/Santiago" -> "Santiago"
      "America/Sao_Paulo" -> "São Paulo"
      "America/Mexico_City" ->
        when (lang) {
          "es" -> "Ciudad de México"
          else -> "Mexico City"
        }
      "Europe/Madrid" -> "Madrid"
      else -> {
        val slashIndex = tz.lastIndexOf('/')
        if (slashIndex in 0 until tz.length - 1) {
          tz.substring(slashIndex + 1).replace('_', ' ').trim()
        } else {
          ""
        }
      }
    }
  }

  /**
   * Layer 1 — Instant Offline Timezone & Locale Detection.
   * Zero network wait, safe on main thread.
   */
  fun getDetectedChurchCountrySync(
    language: String = "ht",
    context: Context? = null,
    timezoneIdOverride: String? = null,
  ): DetectedChurchLocation {
    val lang = normalizeLanguage(language)
    val tzId = timezoneIdOverride ?: runCatching { TimeZone.getDefault().id.orEmpty() }.getOrDefault("")
    val tzCountry = TIMEZONE_TO_COUNTRY[tzId]
    val cachedCountry = readCachedCountryCode(context)
    val cachedCity = readCachedCityHint(context)
    val localeCountry =
      runCatching { Locale.getDefault().country.trim().uppercase(Locale.US) }
        .getOrDefault("")
        .takeIf { it.length == 2 && it != "US" || tzCountry == "US" }

    val resolvedCountryCode =
      when {
        !cachedCountry.isNullOrBlank() -> cachedCountry
        !tzCountry.isNullOrBlank() -> tzCountry
        !localeCountry.isNullOrBlank() -> localeCountry
        else -> {
          val fallbackLocale =
            runCatching { Locale.getDefault().country.trim().uppercase(Locale.US) }.getOrDefault("")
          fallbackLocale.ifBlank { "HT" }
        }
      }

    val tzCityHint = extractCityHintFromTimezone(tzId, lang)
    val resolvedCityHint =
      when {
        !cachedCity.isNullOrBlank() -> cachedCity
        // Only use timezone city hint if it matches the resolved country or no cached override disagrees
        tzCountry == null || tzCountry == resolvedCountryCode -> tzCityHint
        else -> ""
      }

    val countryName = getCountryNameFromCode(resolvedCountryCode, lang)
    val formattedDefaultCity =
      if (resolvedCityHint.isNotBlank() && !containsKnownCountryName(resolvedCityHint, countryName)) {
        "$resolvedCityHint, $countryName"
      } else if (resolvedCityHint.isNotBlank()) {
        resolvedCityHint
      } else {
        countryName
      }

    val source =
      when {
        !cachedCountry.isNullOrBlank() -> "cache"
        !tzCountry.isNullOrBlank() -> "timezone"
        else -> "locale"
      }

    return DetectedChurchLocation(
      countryCode = resolvedCountryCode,
      countryName = countryName,
      cityHint = resolvedCityHint,
      formattedDefaultCity = formattedDefaultCity,
      source = source,
    )
  }

  /**
   * Layer 2 — Background Network IP Country Detection (`https://api.country.is`).
   * Queries with 2.5s timeout on `Dispatchers.IO`, caches for 24h in SharedPreferences, and never throws.
   */
  suspend fun detectGeoCountryAsync(context: Context? = null): String? =
    withContext(Dispatchers.IO) {
      val cached = readCachedCountryCode(context)
      if (!cached.isNullOrBlank()) {
        return@withContext cached
      }
      runCatching {
        val url = URL("https://api.country.is")
        val conn = (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          connectTimeout = 2500
          readTimeout = 2500
          setRequestProperty("Accept", "application/json")
        }
        try {
          if (conn.responseCode in 200..299) {
            val responseText =
              BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(responseText)
            val countryCode =
              json.optString("country", "").trim().uppercase(Locale.US).takeIf {
                it.length == 2 && it.all { ch -> ch in 'A'..'Z' }
              }
            if (!countryCode.isNullOrBlank()) {
              saveCachedLocation(context, countryCode = countryCode, cityHint = null)
              return@withContext countryCode
            }
          }
        } finally {
          conn.disconnect()
        }
        null
      }.getOrNull()
    }

  /**
   * Layer 3 — On-Demand GPS / Network Reverse Geocoding.
   * Uses granted location permissions + Geocoder (or BigDataCloud reverse-geocode fallback),
   * and falls back smoothly to Layer 2 + Layer 1 without crashing if permission is denied or offline.
   */
  @SuppressLint("MissingPermission")
  suspend fun detectChurchLocationWithGpsOrNetworkAsync(
    context: Context,
    language: String = "ht",
  ): DetectedChurchLocation =
    withContext(Dispatchers.IO) {
      val lang = normalizeLanguage(language)

      // 1. If Coarse or Fine Location permission is granted, attempt last known GPS/Network location
      if (hasLocationPermission(context)) {
        val loc = getBestLastKnownLocation(context)
        if (loc != null) {
          val geocoded =
            reverseGeocodeWithAndroidGeocoder(context, loc.latitude, loc.longitude, lang)
              ?: reverseGeocodeWithBigDataCloud(loc.latitude, loc.longitude, lang)
          if (geocoded != null) {
            val (city, countryCode) = geocoded
            saveCachedLocation(context, countryCode = countryCode, cityHint = city)
            val countryName = getCountryNameFromCode(countryCode, lang)
            val formatted =
              if (city.isNotBlank()) {
                formatChurchLocationWithDetectedCountry(city, lang, countryName, context)
              } else {
                countryName
              }
            return@withContext DetectedChurchLocation(
              countryCode = countryCode,
              countryName = countryName,
              cityHint = city,
              formattedDefaultCity = formatted,
              source = "gps",
            )
          }
        }
      }

      // 2. Fallback to BigDataCloud IP-based client reverse geocode (works without GPS coordinates)
      val ipReverse = reverseGeocodeIpWithBigDataCloud(lang)
      if (ipReverse != null) {
        val (city, countryCode) = ipReverse
        saveCachedLocation(context, countryCode = countryCode, cityHint = city)
        val countryName = getCountryNameFromCode(countryCode, lang)
        val formatted =
          if (city.isNotBlank()) {
            formatChurchLocationWithDetectedCountry(city, lang, countryName, context)
          } else {
            countryName
          }
        return@withContext DetectedChurchLocation(
          countryCode = countryCode,
          countryName = countryName,
          cityHint = city,
          formattedDefaultCity = formatted,
          source = "network_geocode",
        )
      }

      // 3. Fallback to Layer 2 (api.country.is) + Layer 1 (Timezone & Locale)
      detectGeoCountryAsync(context)
      getDetectedChurchCountrySync(language = lang, context = context)
    }

  /**
   * Smart non-duplicating formatter for church `city` ("Vil / Depatman / Peyi").
   * - If `rawCity` is null or blank, returns `"$cityHint, $countryName"` (or `countryName` if `cityHint` is blank).
   * - If `rawCity` already contains the detected country name or any known country token, returns `rawCity.trim()` unchanged.
   * - Otherwise appends `", $countryName"`.
   */
  fun formatChurchLocationWithDetectedCountry(
    rawCity: String?,
    language: String = "ht",
    explicitCountry: String? = null,
    context: Context? = null,
  ): String {
    val lang = normalizeLanguage(language)
    val detected = getDetectedChurchCountrySync(language = lang, context = context)
    val countryName =
      explicitCountry?.trim()?.takeIf { it.isNotBlank() }?.let { rawExplicit ->
        if (rawExplicit.length == 2 && rawExplicit.all { it.isLetter() }) {
          getCountryNameFromCode(rawExplicit, lang)
        } else {
          rawExplicit
        }
      } ?: detected.countryName

    val cleanedCity = rawCity?.trim().orEmpty()
    if (cleanedCity.isBlank()) {
      val cityHint = detected.cityHint.trim()
      return if (cityHint.isNotBlank() && !containsKnownCountryName(cityHint, countryName)) {
        "$cityHint, $countryName"
      } else if (cityHint.isNotBlank()) {
        cityHint
      } else {
        countryName
      }
    }

    if (containsKnownCountryName(cleanedCity, countryName)) {
      return cleanedCity
    }

    return "$cleanedCity, $countryName"
  }

  /**
   * Resolves the best raw city/location string from a `Church` model where `city` defaults to `"Ayiti"`
   * and `address` may hold the specific city/department entered during registration.
   */
  fun resolveRawChurchCityOrAddress(city: String?, address: String?): String {
    val cleanCity = city?.trim().orEmpty()
    val cleanAddress = address?.trim().orEmpty()
    return when {
      cleanCity.isNotEmpty() && !cleanCity.equals("Ayiti", ignoreCase = true) -> cleanCity
      cleanAddress.isNotEmpty() -> cleanAddress
      else -> cleanCity
    }
  }

  /**
   * Formats a church's `city` + `address` pair with the localized country name without duplication.
   */
  fun formatChurchModelLocation(
    city: String?,
    address: String?,
    language: String = "ht",
    explicitCountry: String? = null,
    context: Context? = null,
  ): String {
    val raw = resolveRawChurchCityOrAddress(city, address)
    val effectiveExplicit =
      explicitCountry?.trim()?.takeIf { it.isNotBlank() && !it.equals("Ayiti", ignoreCase = true) }
    return formatChurchLocationWithDetectedCountry(
      rawCity = raw,
      language = language,
      explicitCountry = effectiveExplicit,
      context = context,
    )
  }

  internal fun containsKnownCountryName(text: String, targetCountryName: String): Boolean {
    val normalizedText = stripAccents(text).lowercase(Locale.US)
    val normalizedTarget = stripAccents(targetCountryName).lowercase(Locale.US)
    if (normalizedTarget.isNotBlank() && normalizedText.contains(normalizedTarget)) {
      return true
    }
    // Check base target name without parenthetical suffix like "Etazini (USA)" -> "etazini"
    val targetBeforeParen = normalizedTarget.substringBefore("(").trim()
    if (targetBeforeParen.length >= 3 && normalizedText.contains(targetBeforeParen)) {
      return true
    }
    for (token in KNOWN_COUNTRY_TOKENS) {
      val normToken = stripAccents(token).lowercase(Locale.US)
      if (normToken.isNotBlank() && containsTokenOnBoundary(normalizedText, normToken)) {
        return true
      }
    }
    return false
  }

  private fun containsTokenOnBoundary(normalizedText: String, normalizedToken: String): Boolean {
    var startIndex = normalizedText.indexOf(normalizedToken)
    while (startIndex >= 0) {
      val endIndex = startIndex + normalizedToken.length
      val validStart =
        startIndex == 0 || !normalizedText[startIndex - 1].isLetterOrDigit()
      val validEnd =
        endIndex >= normalizedText.length || !normalizedText[endIndex].isLetterOrDigit()
      if (validStart && validEnd) {
        return true
      }
      startIndex = normalizedText.indexOf(normalizedToken, startIndex + 1)
    }
    return false
  }

  private fun stripAccents(input: String): String {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
      .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
  }

  private fun hasLocationPermission(context: Context): Boolean {
    val fine =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    val coarse =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  @SuppressLint("MissingPermission")
  private fun getBestLastKnownLocation(context: Context): Location? {
    return runCatching {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
      val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
      )
      var bestLocation: Location? = null
      for (provider in providers) {
        val loc = runCatching { lm.getLastKnownLocation(provider) }.getOrNull() ?: continue
        if (bestLocation == null || loc.time > bestLocation.time) {
          bestLocation = loc
        }
      }
      bestLocation
    }.getOrNull()
  }

  @Suppress("DEPRECATION")
  private fun reverseGeocodeWithAndroidGeocoder(
    context: Context,
    latitude: Double,
    longitude: Double,
    language: String,
  ): Pair<String, String>? {
    return runCatching {
      if (!Geocoder.isPresent()) return null
      val locale =
        when (language) {
          "fr" -> Locale.FRENCH
          "en" -> Locale.ENGLISH
          "es" -> Locale.forLanguageTag("es")
          else -> Locale.forLanguageTag("fr")
        }
      val geocoder = Geocoder(context, locale)
      val addresses = geocoder.getFromLocation(latitude, longitude, 1)
      val first = addresses?.firstOrNull() ?: return null
      val countryCode = first.countryCode?.trim()?.uppercase(Locale.US).orEmpty()
      if (countryCode.length != 2) return null
      val city =
        first.locality?.trim()?.takeIf { it.isNotBlank() }
          ?: first.subAdminArea?.trim()?.takeIf { it.isNotBlank() }
          ?: first.adminArea?.trim().orEmpty()
      city to countryCode
    }.getOrNull()
  }

  private fun reverseGeocodeWithBigDataCloud(
    latitude: Double,
    longitude: Double,
    language: String,
  ): Pair<String, String>? {
    val queryLang = if (language == "ht") "fr" else language
    val urlStr =
      "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$latitude&longitude=$longitude&localityLanguage=$queryLang"
    return fetchBigDataCloudCityAndCountry(urlStr)
  }

  private fun reverseGeocodeIpWithBigDataCloud(language: String): Pair<String, String>? {
    val queryLang = if (language == "ht") "fr" else language
    val urlStr =
      "https://api.bigdatacloud.net/data/reverse-geocode-client?localityLanguage=$queryLang"
    return fetchBigDataCloudCityAndCountry(urlStr)
  }

  private fun fetchBigDataCloudCityAndCountry(urlStr: String): Pair<String, String>? {
    return runCatching {
      val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 2500
        readTimeout = 2500
        setRequestProperty("Accept", "application/json")
      }
      try {
        if (conn.responseCode in 200..299) {
          val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
          val json = JSONObject(body)
          val countryCode =
            json.optString("countryCode", "").trim().uppercase(Locale.US).takeIf {
              it.length == 2 && it.all { ch -> ch in 'A'..'Z' }
            } ?: return null
          val city =
            json.optString("city", "").trim().ifBlank {
              json.optString("locality", "").trim().ifBlank {
                json.optString("principalSubdivision", "").trim()
              }
            }
          city to countryCode
        } else {
          null
        }
      } finally {
        conn.disconnect()
      }
    }.getOrNull()
  }

  private fun readCachedCountryCode(context: Context?): String? {
    val now = System.currentTimeMillis()
    if (!memoryCachedCountryCode.isNullOrBlank() && (now - memoryCachedTimestampMs) in 0..CACHE_TTL_MS) {
      return memoryCachedCountryCode
    }
    if (context == null) return null
    return runCatching {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val ts = prefs.getLong(KEY_GEO_TS_V1, 0L)
      val code = prefs.getString(KEY_GEO_COUNTRY_V1, null)?.trim()?.uppercase(Locale.US)
      val city = prefs.getString(KEY_GEO_CITY_V1, null)?.trim()
      if (!code.isNullOrBlank() && code.length == 2 && (now - ts) in 0..CACHE_TTL_MS) {
        memoryCachedCountryCode = code
        memoryCachedCityHint = city
        memoryCachedTimestampMs = ts
        code
      } else {
        null
      }
    }.getOrNull()
  }

  private fun readCachedCityHint(context: Context?): String? {
    val now = System.currentTimeMillis()
    if (!memoryCachedCityHint.isNullOrBlank() && (now - memoryCachedTimestampMs) in 0..CACHE_TTL_MS) {
      return memoryCachedCityHint
    }
    if (context == null) return null
    return runCatching {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      val ts = prefs.getLong(KEY_GEO_TS_V1, 0L)
      val city = prefs.getString(KEY_GEO_CITY_V1, null)?.trim()
      if (!city.isNullOrBlank() && (now - ts) in 0..CACHE_TTL_MS) {
        city
      } else {
        null
      }
    }.getOrNull()
  }

  private fun saveCachedLocation(context: Context?, countryCode: String, cityHint: String?) {
    val now = System.currentTimeMillis()
    val cleanCode = countryCode.trim().uppercase(Locale.US)
    memoryCachedCountryCode = cleanCode
    if (!cityHint.isNullOrBlank()) {
      memoryCachedCityHint = cityHint.trim()
    }
    memoryCachedTimestampMs = now
    if (context == null) return
    runCatching {
      val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      prefs.edit().apply {
        putString(KEY_GEO_COUNTRY_V1, cleanCode)
        if (!cityHint.isNullOrBlank()) {
          putString(KEY_GEO_CITY_V1, cityHint.trim())
        }
        putLong(KEY_GEO_TS_V1, now)
        apply()
      }
    }
  }
}
