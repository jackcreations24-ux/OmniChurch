package com.zoutiw.omnichurch.core.utilities

import android.app.Activity
import android.app.KeyguardManager
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.omniSessionDataStore by
  preferencesDataStore(name = "omni_church_session_prefs")

class OmniPreferencesManager(private val context: Context) {
  // Persistent Session Keys (omni_church_session_prefs)
  private val keyActiveChurchId = stringPreferencesKey("activeChurchId")
  private val keyActiveRole = stringPreferencesKey("activeRole")
  private val keyActiveMemberId = stringPreferencesKey("activeMemberId")
  private val keyActiveMemberDocId = stringPreferencesKey("activeMemberDocId")

  // Biometric Quick Unlock Keys
  private val keyBiometricEnabled = booleanPreferencesKey("biometricEnabled")
  private val keyBiometricChurchId = stringPreferencesKey("biometricChurchId")
  private val keyBiometricMemberId = stringPreferencesKey("biometricMemberId")
  private val keyBiometricMemberDocId = stringPreferencesKey("biometricMemberDocId")
  private val keyBiometricMemberName = stringPreferencesKey("biometricMemberName")

  // Offline QR Cache & Localization Keys
  private val keyCachedQrToken = stringPreferencesKey("cached_barcode_token")
  private val keyCachedMemberCode = stringPreferencesKey("cached_member_code")
  private val keyCachedMemberName = stringPreferencesKey("cached_member_name")
  private val keyLanguageCode = stringPreferencesKey("omnichurch_lang_v1")

  val sessionStateFlow: Flow<OmniSessionState> =
    context.omniSessionDataStore.data.map { prefs ->
      OmniSessionState(
        isInitialized = true,
        activeChurchId = prefs[keyActiveChurchId].orEmpty(),
        activeRole = prefs[keyActiveRole] ?: "member",
        activeMemberId = prefs[keyActiveMemberId].orEmpty(),
        activeMemberDocId = prefs[keyActiveMemberDocId].orEmpty(),
        biometricEnabled = prefs[keyBiometricEnabled] ?: false,
        biometricChurchId = prefs[keyBiometricChurchId].orEmpty(),
        biometricMemberId = prefs[keyBiometricMemberId].orEmpty(),
        biometricMemberDocId = prefs[keyBiometricMemberDocId].orEmpty(),
        biometricMemberName = prefs[keyBiometricMemberName].orEmpty(),
      )
    }

  val cachedQrTokenFlow: Flow<String?> =
    context.omniSessionDataStore.data.map { prefs -> prefs[keyCachedQrToken] }

  val languageCodeFlow: Flow<String> =
    context.omniSessionDataStore.data.map { prefs ->
      prefs[keyLanguageCode]
        ?: try {
          context.getSharedPreferences("omnichurch_prefs", Context.MODE_PRIVATE)
            .getString("omnichurch_lang_v1", null)
        } catch (_: Exception) {
          null
        }
        ?: "ht"
    }

  suspend fun saveAuthenticatedSession(
    activeChurchId: String,
    activeRole: String,
    activeMemberId: String,
    activeMemberDocId: String,
    memberName: String = "",
    barcodeToken: String = "",
  ) {
    context.omniSessionDataStore.edit { prefs ->
      prefs[keyActiveChurchId] = activeChurchId.trim()
      prefs[keyActiveRole] = if (activeRole == "admin") "admin" else "member"
      prefs[keyActiveMemberId] = activeMemberId.trim()
      prefs[keyActiveMemberDocId] = activeMemberDocId.trim()
      if (barcodeToken.isNotBlank()) {
        prefs[keyCachedQrToken] = barcodeToken.trim()
        prefs[keyCachedMemberCode] = activeMemberId.trim()
        prefs[keyCachedMemberName] = memberName.trim()
      }
      // Keep biometric target synced if biometric quick unlock is already enabled
      if (prefs[keyBiometricEnabled] == true && activeMemberDocId.isNotBlank()) {
        prefs[keyBiometricChurchId] = activeChurchId.trim()
        prefs[keyBiometricMemberId] = activeMemberId.trim()
        prefs[keyBiometricMemberDocId] = activeMemberDocId.trim()
        if (memberName.isNotBlank()) {
          prefs[keyBiometricMemberName] = memberName.trim()
        }
      }
    }
  }

  suspend fun saveMemberSession(
    churchId: String,
    memberId: String,
    memberDocId: String,
    memberName: String = "",
    barcodeToken: String = "",
  ) {
    saveAuthenticatedSession(
      activeChurchId = churchId,
      activeRole = "member",
      activeMemberId = memberId,
      activeMemberDocId = memberDocId,
      memberName = memberName,
      barcodeToken = barcodeToken,
    )
  }

  suspend fun setBiometricQuickUnlock(
    enabled: Boolean,
    churchId: String,
    memberId: String,
    memberDocId: String,
    memberName: String,
  ) {
    context.omniSessionDataStore.edit { prefs ->
      prefs[keyBiometricEnabled] = enabled
      if (enabled) {
        prefs[keyBiometricChurchId] = churchId.trim()
        prefs[keyBiometricMemberId] = memberId.trim()
        prefs[keyBiometricMemberDocId] = memberDocId.trim()
        prefs[keyBiometricMemberName] = memberName.trim()
      } else {
        prefs.remove(keyBiometricChurchId)
        prefs.remove(keyBiometricMemberId)
        prefs.remove(keyBiometricMemberDocId)
        prefs.remove(keyBiometricMemberName)
      }
    }
  }

  suspend fun clearActiveSession() {
    context.omniSessionDataStore.edit { prefs ->
      prefs.remove(keyActiveChurchId)
      prefs.remove(keyActiveRole)
      prefs.remove(keyActiveMemberId)
      prefs.remove(keyActiveMemberDocId)
    }
  }

  suspend fun cacheVerifiedMemberQr(barcodeToken: String, memberCode: String, fullName: String) {
    if (barcodeToken.isBlank()) return
    context.omniSessionDataStore.edit { prefs ->
      prefs[keyCachedQrToken] = barcodeToken
      prefs[keyCachedMemberCode] = memberCode
      prefs[keyCachedMemberName] = fullName
    }
  }

  suspend fun clearCachedMemberQr() {
    context.omniSessionDataStore.edit { prefs ->
      prefs.remove(keyCachedQrToken)
      prefs.remove(keyCachedMemberCode)
      prefs.remove(keyCachedMemberName)
    }
  }

  suspend fun setLanguageCode(langCode: String) {
    context.omniSessionDataStore.edit { prefs -> prefs[keyLanguageCode] = langCode }
    try {
      context.getSharedPreferences("omnichurch_prefs", Context.MODE_PRIVATE)
        .edit().putString("omnichurch_lang_v1", langCode).apply()
    } catch (_: Exception) {}
  }

  private fun sanitizeKeySegment(raw: String): String =
    raw.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")

  suspend fun getSeenNotificationKeys(churchId: String, memberKey: String): Set<String> {
    if (churchId.isBlank() || memberKey.isBlank()) return emptySet()
    val prefKey =
      stringSetPreferencesKey(
        "seen_notif_keys_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    val prefs = context.omniSessionDataStore.data.first()
    return prefs[prefKey] ?: emptySet()
  }

  suspend fun markNotificationKeysSeen(
    churchId: String,
    memberKey: String,
    keys: Collection<String>,
  ) {
    if (churchId.isBlank() || memberKey.isBlank() || keys.isEmpty()) return
    val prefKey =
      stringSetPreferencesKey(
        "seen_notif_keys_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    context.omniSessionDataStore.edit { prefs ->
      val existing = prefs[prefKey] ?: emptySet()
      prefs[prefKey] = (existing + keys).toList().takeLast(500).toSet()
    }
  }

  suspend fun isFeedBaselineSeeded(churchId: String, memberKey: String): Boolean {
    if (churchId.isBlank() || memberKey.isBlank()) return false
    val prefKey =
      booleanPreferencesKey(
        "feed_baseline_seeded_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    val prefs = context.omniSessionDataStore.data.first()
    return prefs[prefKey] == true
  }

  suspend fun markFeedBaselineSeeded(churchId: String, memberKey: String) {
    if (churchId.isBlank() || memberKey.isBlank()) return
    val prefKey =
      booleanPreferencesKey(
        "feed_baseline_seeded_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    context.omniSessionDataStore.edit { prefs -> prefs[prefKey] = true }
  }

  fun observeReadAnnouncementIds(churchId: String, memberKey: String): Flow<Set<String>> {
    if (churchId.isBlank() || memberKey.isBlank()) {
      return kotlinx.coroutines.flow.flowOf(emptySet())
    }
    val prefKey =
      stringSetPreferencesKey(
        "read_announcement_ids_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    return context.omniSessionDataStore.data.map { prefs -> prefs[prefKey] ?: emptySet() }
  }

  suspend fun markAnnouncementRead(
    churchId: String,
    memberKey: String,
    announcementId: String,
  ) {
    if (churchId.isBlank() || memberKey.isBlank() || announcementId.isBlank()) return
    val prefKey =
      stringSetPreferencesKey(
        "read_announcement_ids_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    context.omniSessionDataStore.edit { prefs ->
      val existing = prefs[prefKey] ?: emptySet()
      prefs[prefKey] = (existing + announcementId.trim()).toList().takeLast(500).toSet()
    }
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

object QrCodeGenerator {
  /**
   * Generates a high-contrast pure white background QR code with dark modules and an optional
   * subtle center OmniChurch flame emblem (using High error correction level H so scannability is
   * 100% preserved under all lighting conditions).
   */
  suspend fun generateQrBitmap(
    token: String,
    sizePx: Int = 768,
    includeCenterEmblem: Boolean = true,
  ): Bitmap? =
    withContext(Dispatchers.Default) {
      if (token.isBlank()) return@withContext null
      try {
        val hints =
          mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 2,
            EncodeHintType.CHARACTER_SET to "UTF-8",
          )
        val bitMatrix = QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val darkModuleColor = Color.rgb(7, 21, 47) // Deep OmniChurch Navy #07152F for crisp contrast
        for (y in 0 until height) {
          val offset = y * width
          for (x in 0 until width) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) darkModuleColor else Color.WHITE
          }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        if (includeCenterEmblem) {
          drawCenterFlameBadge(bitmap)
        }
        bitmap
      } catch (e: Exception) {
        null
      }
    }

  private fun drawCenterFlameBadge(bitmap: Bitmap) {
    val canvas = Canvas(bitmap)
    val cx = bitmap.width / 2f
    val cy = bitmap.height / 2f
    val badgeRadius = bitmap.width * 0.105f

    val bgPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
      }
    val borderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 174, 239)
        style = Paint.Style.STROKE
        strokeWidth = bitmap.width * 0.008f
      }
    val rect = RectF(cx - badgeRadius, cy - badgeRadius, cx + badgeRadius, cy + badgeRadius)
    val corner = badgeRadius * 0.35f
    canvas.drawRoundRect(rect, corner, corner, bgPaint)
    canvas.drawRoundRect(rect, corner, corner, borderPaint)

    // Left orange-gold flame wing
    val flamePaintLeft =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx - badgeRadius * 0.55f,
            cy - badgeRadius * 0.65f,
            cx,
            cy + badgeRadius * 0.65f,
            Color.rgb(255, 194, 71),
            Color.rgb(255, 140, 0),
            Shader.TileMode.CLAMP,
          )
        style = Paint.Style.FILL
      }
    val leftPath =
      Path().apply {
        moveTo(cx, cy - badgeRadius * 0.65f)
        cubicTo(
          cx - badgeRadius * 0.55f,
          cy - badgeRadius * 0.15f,
          cx - badgeRadius * 0.55f,
          cy + badgeRadius * 0.45f,
          cx - badgeRadius * 0.04f,
          cy + badgeRadius * 0.62f,
        )
        cubicTo(
          cx - badgeRadius * 0.25f,
          cy + badgeRadius * 0.25f,
          cx - badgeRadius * 0.15f,
          cy - badgeRadius * 0.15f,
          cx,
          cy - badgeRadius * 0.65f,
        )
        close()
      }
    canvas.drawPath(leftPath, flamePaintLeft)

    // Right electric-blue flame wing
    val flamePaintRight =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            cx,
            cy - badgeRadius * 0.45f,
            cx + badgeRadius * 0.55f,
            cy + badgeRadius * 0.65f,
            Color.rgb(0, 200, 255),
            Color.rgb(8, 123, 255),
            Shader.TileMode.CLAMP,
          )
        style = Paint.Style.FILL
      }
    val rightPath =
      Path().apply {
        moveTo(cx + badgeRadius * 0.08f, cy - badgeRadius * 0.42f)
        cubicTo(
          cx + badgeRadius * 0.55f,
          cy - badgeRadius * 0.05f,
          cx + badgeRadius * 0.55f,
          cy + badgeRadius * 0.48f,
          cx + badgeRadius * 0.04f,
          cy + badgeRadius * 0.62f,
        )
        cubicTo(
          cx + badgeRadius * 0.24f,
          cy + badgeRadius * 0.25f,
          cx + badgeRadius * 0.18f,
          cy - badgeRadius * 0.1f,
          cx + badgeRadius * 0.08f,
          cy - badgeRadius * 0.42f,
        )
        close()
      }
    canvas.drawPath(rightPath, flamePaintRight)
  }

  /**
   * Saves the member QR bitmap to the Android Gallery using MediaStore without requiring broad
   * storage permissions.
   */
  suspend fun saveQrToGallery(
    context: Context,
    bitmap: Bitmap,
    memberCode: String,
  ): Result<String> =
    withContext(Dispatchers.IO) {
      try {
        val filename = "OmniChurch_QR_${memberCode}_${System.currentTimeMillis()}.png"
        val resolver = context.contentResolver
        val contentValues =
          ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/OmniChurch",
              )
              put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
          }

        val imageUri =
          resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext Result.failure(IllegalStateException("MediaStore URI null"))

        resolver.openOutputStream(imageUri)?.use { outStream ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          contentValues.clear()
          contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
          resolver.update(imageUri, contentValues, null, null)
        }
        Result.success(filename)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }

  /** Shares the QR bitmap and optional canonical verification link via Android FileProvider chooser. */
  suspend fun shareQrCardImage(
    context: Context,
    bitmap: Bitmap,
    memberName: String,
    memberCode: String,
    churchName: String,
    verificationUrl: String? = null,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      try {
        val cacheDir = File(context.cacheDir, "shared_qr").apply { mkdirs() }
        val file = File(cacheDir, "OmniChurch_ID_${memberCode}.png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val uri =
          FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        withContext(Dispatchers.Main) {
          val shareText =
            if (!verificationUrl.isNullOrBlank()) {
              "$churchName • $memberName (ID: $memberCode)\n$verificationUrl"
            } else {
              "$churchName • $memberName (ID: $memberCode)"
            }
          val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
              type = "image/png"
              putExtra(Intent.EXTRA_STREAM, uri)
              putExtra(Intent.EXTRA_TEXT, shareText)
              addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
          val chooser =
            Intent.createChooser(shareIntent, churchName).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
          context.startActivity(chooser)
        }
        Result.success(Unit)
      } catch (e: Exception) {
        Result.failure(e)
      }
    }
}
