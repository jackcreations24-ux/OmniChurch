package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoutiw.omnichurch.data.models.OmniSessionState
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

data class RememberedAdminCredentials(
  val rememberEnabled: Boolean = true,
  val email: String = "",
  val password: String = "",
)

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

  // Remember Me (Encrypted Admin Email + Password Prefill)
  private val keyRememberAdminEnabled = booleanPreferencesKey("remember_admin_credentials_enabled_v1")
  private val keyRememberedAdminEmail = stringPreferencesKey("remembered_admin_email_v1")
  private val keyRememberedAdminSecret = stringPreferencesKey("remembered_admin_secret_v1")

  private val safePreferencesFlow =
    context.omniSessionDataStore.data.catch { emit(emptyPreferences()) }

  val rememberedAdminCredentialsFlow: Flow<RememberedAdminCredentials> =
    safePreferencesFlow.map { prefs ->
      val enabled = prefs[keyRememberAdminEnabled] ?: true
      if (!enabled) {
        RememberedAdminCredentials(rememberEnabled = false)
      } else {
        RememberedAdminCredentials(
          rememberEnabled = true,
          email = prefs[keyRememberedAdminEmail].orEmpty(),
          password = CryptoSecurity.decryptLocalSecret(prefs[keyRememberedAdminSecret]),
        )
      }
    }

  val sessionStateFlow: Flow<OmniSessionState> =
    safePreferencesFlow.map { prefs ->
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
    safePreferencesFlow.map { prefs -> prefs[keyCachedQrToken] }

  val languageCodeFlow: Flow<String> =
    safePreferencesFlow.map { prefs ->
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

  suspend fun saveRememberedAdminCredentials(
    remember: Boolean,
    email: String,
    password: String,
  ) {
    val cleanEmail = email.trim().lowercase(Locale.US)
    context.omniSessionDataStore.edit { prefs ->
      prefs[keyRememberAdminEnabled] = remember
      if (remember && cleanEmail.isNotEmpty()) {
        prefs[keyRememberedAdminEmail] = cleanEmail
        val encrypted = CryptoSecurity.encryptLocalSecret(password)
        if (encrypted.isNotEmpty()) {
          prefs[keyRememberedAdminSecret] = encrypted
        } else {
          prefs.remove(keyRememberedAdminSecret)
        }
      } else {
        prefs.remove(keyRememberedAdminEmail)
        prefs.remove(keyRememberedAdminSecret)
      }
    }
  }

  suspend fun setRememberAdminEnabled(enabled: Boolean) {
    context.omniSessionDataStore.edit { prefs ->
      prefs[keyRememberAdminEnabled] = enabled
      if (!enabled) {
        prefs.remove(keyRememberedAdminEmail)
        prefs.remove(keyRememberedAdminSecret)
      }
    }
  }

  private fun sanitizeKeySegment(raw: String): String =
    raw.trim().lowercase().replace(Regex("[^a-z0-9_-]"), "_")

  suspend fun getSeenNotificationKeys(churchId: String, memberKey: String): Set<String> {
    if (churchId.isBlank() || memberKey.isBlank()) return emptySet()
    val prefKey =
      stringSetPreferencesKey(
        "seen_notif_keys_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    val prefs = safePreferencesFlow.first()
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
    val prefs = safePreferencesFlow.first()
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
      return flowOf(emptySet())
    }
    val prefKey =
      stringSetPreferencesKey(
        "read_announcement_ids_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    return safePreferencesFlow.map { prefs -> prefs[prefKey] ?: emptySet() }
  }

  suspend fun markAnnouncementRead(
    churchId: String,
    memberKey: String,
    announcementId: String,
  ) {
    markAnnouncementsRead(churchId, memberKey, listOf(announcementId))
  }

  suspend fun markAnnouncementsRead(
    churchId: String,
    memberKey: String,
    announcementIds: Collection<String>,
  ) {
    val cleanIds = announcementIds.map { it.trim() }.filter { it.isNotEmpty() }
    if (churchId.isBlank() || memberKey.isBlank() || cleanIds.isEmpty()) return
    val prefKey =
      stringSetPreferencesKey(
        "read_announcement_ids_${sanitizeKeySegment(churchId)}_${sanitizeKeySegment(memberKey)}"
      )
    context.omniSessionDataStore.edit { prefs ->
      val existing = prefs[prefKey] ?: emptySet()
      val merged = (existing + cleanIds).toList()
      prefs[prefKey] = merged.takeLast(1000).toSet()
    }
  }
}
