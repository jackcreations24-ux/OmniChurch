package com.zoutiw.omnichurch.core.utilities

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.localeDataStore by preferencesDataStore(name = "omnichurch_locale_prefs")

class LocaleManager(context: Context) {

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

object BiometricQuickUnlockHelper {
  fun canUseBiometricOrDeviceLock(context: Context): Boolean {
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    return keyguard?.isDeviceSecure == true || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
  }

  fun isBiometricAvailable(context: Context): Boolean = canUseBiometricOrDeviceLock(context)

  fun promptBiometricUnlock(
    context: Context,
    title: String,
    subtitle: String,
    cancelText: String = "Anile",
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    val activity = context.findActivity() ?: context
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      try {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt =
          BiometricPrompt.Builder(activity)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton(
              cancelText,
              executor,
              DialogInterface.OnClickListener { _, _ -> },
            )
            .build()

        val cancellationSignal = CancellationSignal()
        prompt.authenticate(
          cancellationSignal,
          executor,
          object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
              result: BiometricPrompt.AuthenticationResult?
            ) {
              super.onAuthenticationSucceeded(result)
              onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
              super.onAuthenticationError(errorCode, errString)
              if (
                errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                  errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED
              ) {
                onError(errString?.toString() ?: "Erè verifikasyon biyometrik.")
              }
            }
          },
        )
      } catch (e: Exception) {
        onError(e.localizedMessage ?: "Biyometri pa disponib sou aparèy sa a.")
      }
    } else {
      val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
      if (keyguard?.isDeviceSecure == true) {
        onSuccess()
      } else {
        onError(" aparèy sa a pa gen anprent oswa kòd sekirite aktive.")
      }
    }
  }
}

fun Context.findActivity(): Activity? {
  var current: Context? = this
  while (current is ContextWrapper) {
    if (current is Activity) return current
    current = current.baseContext
  }
  return current as? Activity
}

object LocaleHelper {
  @Suppress("DEPRECATION")
  fun applyLocale(context: Context, languageCode: String) {
    val locale = resolveLocale(languageCode)
    Locale.setDefault(locale)
    val resources = context.resources
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
    context.applicationContext?.resources?.let { appRes ->
      val appConfig = Configuration(appRes.configuration)
      appConfig.setLocale(locale)
      appRes.updateConfiguration(appConfig, appRes.displayMetrics)
    }
  }

  fun wrapContextWithLocale(context: Context, languageCode: String): Context {
    applyLocale(context, languageCode)
    val locale = resolveLocale(languageCode)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    val configContext = context.createConfigurationContext(config)
    return object : ContextWrapper(context) {
      override fun getResources(): Resources = configContext.resources

      override fun getAssets(): AssetManager = configContext.assets
    }
  }

  private fun resolveLocale(languageCode: String): Locale =
    when (languageCode) {
      "fr" -> Locale.FRENCH
      "en" -> Locale.ENGLISH
      "es" -> Locale.forLanguageTag("es")
      else -> Locale.forLanguageTag("ht")
    }
}
