package com.zoutiw.omnichurch.core.legal

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Official external legal links fetched read-only from Firestore:
 * `app_config/developer_engine` -> `legalConfig`.
 */
data class LegalLinksConfig(
  val officialPrivacyUrl: String = "",
  val officialTermsUrl: String = "",
)

/**
 * Security-hardened HTTPS URL validator and native Android launcher
 * (Chrome Custom Tabs styled with Royal Navy `#07152F` + `ACTION_VIEW` fallback).
 */
object LegalUrlOpener {
  private const val TAG = "LegalUrlOpener"
  private const val ROYAL_NAVY_TOOLBAR_COLOR = 0xFF07152F.toInt()

  /**
   * Trims whitespace and ONLY accepts valid `https://` URLs with a non-blank host.
   * Rejects `http://`, `javascript:`, `file:`, `content:`, `intent:`, `data:`,
   * control/whitespace characters, embedded userInfo, or malformed URLs.
   */
  fun normalizeOfficialHttpsUrl(raw: String?): String? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty()) return null
    if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return null

    val uri =
      try {
        Uri.parse(trimmed)
      } catch (_: Exception) {
        return null
      }

    val scheme = uri.scheme?.lowercase() ?: return null
    if (scheme != "https") return null

    val host = uri.host?.trim().orEmpty()
    if (host.isEmpty() || !host.contains('.') || host.startsWith(".") || host.endsWith(".")) {
      return null
    }
    if (!uri.userInfo.isNullOrEmpty()) return null

    return uri.toString()
  }

  /**
   * Safely opens an official HTTPS legal URL using Android Chrome Custom Tabs,
   * automatically falling back to `Intent.ACTION_VIEW` if Custom Tabs is unavailable.
   * Calls [onUnavailable] if the URL is invalid/empty or no browser can handle it.
   */
  fun openOfficialUrl(
    context: Context,
    rawUrl: String?,
    onUnavailable: () -> Unit,
  ): Boolean {
    val validUrl = normalizeOfficialHttpsUrl(rawUrl)
    if (validUrl == null) {
      onUnavailable()
      return false
    }

    val uri = Uri.parse(validUrl)

    // 1. Primary path: Chrome Custom Tabs styled with OmniChurch Royal Navy (#07152F)
    try {
      val colorSchemeParams =
        CustomTabColorSchemeParams.Builder()
          .setToolbarColor(ROYAL_NAVY_TOOLBAR_COLOR)
          .build()
      val customTabsIntent =
        CustomTabsIntent.Builder()
          .setDefaultColorSchemeParams(colorSchemeParams)
          .setShowTitle(true)
          .build()
      if (context !is Activity) {
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      customTabsIntent.launchUrl(context, uri)
      return true
    } catch (e: Exception) {
      Log.w(TAG, "Custom Tabs unavailable, falling back to ACTION_VIEW: ${e.message}")
    }

    // 2. Graceful fallback: standard system browser Intent.ACTION_VIEW
    return try {
      val fallbackIntent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          addCategory(Intent.CATEGORY_BROWSABLE)
          if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
        }
      context.startActivity(fallbackIntent)
      true
    } catch (e: ActivityNotFoundException) {
      Log.w(TAG, "No browser Activity found to open legal URL: ${e.message}")
      onUnavailable()
      false
    } catch (e: Exception) {
      Log.w(TAG, "Unable to open official legal URL: ${e.message}")
      onUnavailable()
      false
    }
  }
}

/**
 * Read-only real-time repository observing `app_config/developer_engine` (`legalConfig`)
 * on the named OmniChurch Firestore database with instant SharedPreferences cold-start cache.
 */
class LegalLinksRepository private constructor(
  private val firestore: FirebaseFirestore,
  private val prefs: SharedPreferences,
) {
  private val _legalLinksState = MutableStateFlow(readCachedConfig())
  val legalLinksState: StateFlow<LegalLinksConfig> = _legalLinksState.asStateFlow()

  private var listenerRegistration: ListenerRegistration? = null

  init {
    startObserving()
  }

  fun startObserving() {
    if (listenerRegistration != null) return
    try {
      listenerRegistration =
        firestore
          .collection(COLLECTION_APP_CONFIG)
          .document(DOC_DEVELOPER_ENGINE)
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(
                TAG,
                "legalConfig listener error (${error.code}); preserving cached official links.",
              )
              return@addSnapshotListener
            }
            if (snapshot == null || !snapshot.exists()) {
              return@addSnapshotListener
            }

            val rawLegalMap = snapshot.get(FIELD_LEGAL_CONFIG) as? Map<*, *>
            val privacyUrl =
              LegalUrlOpener.normalizeOfficialHttpsUrl(
                rawLegalMap?.get(FIELD_OFFICIAL_PRIVACY_URL) as? String
              ).orEmpty()
            val termsUrl =
              LegalUrlOpener.normalizeOfficialHttpsUrl(
                rawLegalMap?.get(FIELD_OFFICIAL_TERMS_URL) as? String
              ).orEmpty()

            val updated =
              LegalLinksConfig(
                officialPrivacyUrl = privacyUrl,
                officialTermsUrl = termsUrl,
              )
            _legalLinksState.value = updated
            writeCachedConfig(updated)
          }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to attach legalConfig listener; using local cache.", e)
    }
  }

  private fun readCachedConfig(): LegalLinksConfig {
    val cachedPrivacy =
      LegalUrlOpener.normalizeOfficialHttpsUrl(
        prefs.getString(KEY_CACHED_PRIVACY_URL, null)
      ).orEmpty()
    val cachedTerms =
      LegalUrlOpener.normalizeOfficialHttpsUrl(
        prefs.getString(KEY_CACHED_TERMS_URL, null)
      ).orEmpty()
    return LegalLinksConfig(
      officialPrivacyUrl = cachedPrivacy,
      officialTermsUrl = cachedTerms,
    )
  }

  private fun writeCachedConfig(config: LegalLinksConfig) {
    prefs.edit().apply {
      if (config.officialPrivacyUrl.isNotEmpty()) {
        putString(KEY_CACHED_PRIVACY_URL, config.officialPrivacyUrl)
      } else {
        remove(KEY_CACHED_PRIVACY_URL)
      }
      if (config.officialTermsUrl.isNotEmpty()) {
        putString(KEY_CACHED_TERMS_URL, config.officialTermsUrl)
      } else {
        remove(KEY_CACHED_TERMS_URL)
      }
    }.apply()
  }

  companion object {
    private const val TAG = "LegalLinksRepo"
    private const val COLLECTION_APP_CONFIG = "app_config"
    private const val DOC_DEVELOPER_ENGINE = "developer_engine"
    private const val FIELD_LEGAL_CONFIG = "legalConfig"
    private const val FIELD_OFFICIAL_PRIVACY_URL = "officialPrivacyUrl"
    private const val FIELD_OFFICIAL_TERMS_URL = "officialTermsUrl"
    private const val PREFS_NAME = "omnichurch_official_legal_links_cache"
    private const val KEY_CACHED_PRIVACY_URL = "cached_official_privacy_url"
    private const val KEY_CACHED_TERMS_URL = "cached_official_terms_url"

    @Volatile private var INSTANCE: LegalLinksRepository? = null

    fun getInstance(context: Context): LegalLinksRepository =
      INSTANCE
        ?: synchronized(this) {
          INSTANCE
            ?: run {
              val appCtx = context.applicationContext
              val db = FirestoreProvider.get(appCtx)
              val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
              LegalLinksRepository(firestore = db, prefs = prefs).also { INSTANCE = it }
            }
        }
  }
}
