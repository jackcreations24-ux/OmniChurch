package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.localeDataStore by preferencesDataStore(name = "omnichurch_locale_prefs")

class LocaleManager(private val context: Context) {

  private val appContext = context.applicationContext

  companion object {
    private val LANGUAGE_KEY = stringPreferencesKey("app_language_code")
    const val DEFAULT_LANGUAGE = "ht"
    val SUPPORTED_LANGUAGES = listOf("ht", "fr", "en", "es")
  }

  val preferredLanguageFlow: Flow<String> =
    appContext.localeDataStore.data.map { prefs ->
      prefs[LANGUAGE_KEY]?.takeIf { it in SUPPORTED_LANGUAGES } ?: DEFAULT_LANGUAGE
    }

  suspend fun savePreferredLanguage(languageCode: String) {
    val clean = if (languageCode in SUPPORTED_LANGUAGES) languageCode else DEFAULT_LANGUAGE
    appContext.localeDataStore.edit { prefs ->
      prefs[LANGUAGE_KEY] = clean
    }
  }

  fun updateRuntimeConfiguration(targetContext: Context, languageCode: String): Context {
    val clean = if (languageCode in SUPPORTED_LANGUAGES) languageCode else DEFAULT_LANGUAGE
    LocaleHelper.applyLocale(targetContext, clean)
    return LocaleHelper.wrapContextWithLocale(targetContext, clean)
  }

  fun resolveLocale(languageCode: String): Locale =
    when (languageCode.trim().lowercase()) {
      "fr" -> Locale.FRENCH
      "en" -> Locale.ENGLISH
      "es" -> Locale.forLanguageTag("es")
      else -> Locale.forLanguageTag("ht")
    }
}
