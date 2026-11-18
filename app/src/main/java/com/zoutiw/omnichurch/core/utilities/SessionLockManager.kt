package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoutiw.omnichurch.data.models.Church
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.omniAdminLockDataStore by
  preferencesDataStore(name = "omni_church_admin_lock_prefs")

enum class OwnershipEvaluationStatus {
  SUPER_DEVELOPER,
  MATCHED_UID,
  MATCHED_EMAIL_NEEDS_UID_HEAL,
  UNCLAIMED_LEGACY_CHURCH,
  NO_AUTH_USER,
  MISMATCHED_OWNER,
}

data class OwnershipEvaluationResult(
  val status: OwnershipEvaluationStatus,
  val isAuthorized: Boolean,
  val expectedOwnerEmail: String? = null,
)

class SessionLockManager(private val context: Context? = null) {
  private val keyLockedChurchesJson = stringPreferencesKey("locked_churches_json_v1")
  private val keyLastAdminBackgroundAtMs = longPreferencesKey("last_admin_background_at_ms_v1")
  private val _inMemoryLockedChurches = MutableStateFlow<Map<String, Boolean>>(emptyMap())
  private val _inMemoryTimestamp = MutableStateFlow<Long?>(null)

  val lockedChurchesFlow: Flow<Map<String, Boolean>> =
    if (context != null) {
      context.omniAdminLockDataStore.data.map { prefs ->
        parseLockedMap(prefs[keyLockedChurchesJson])
      }
    } else {
      _inMemoryLockedChurches
    }

  suspend fun isChurchAdminLocked(churchId: String): Boolean {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return false
    return lockedChurchesFlow.first()[cleanId] == true
  }

  suspend fun lockChurchAdminSession(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
        current[cleanId] = true
        prefs[keyLockedChurchesJson] = serializeLockedMap(current)
      }
    } else {
      _inMemoryLockedChurches.value = _inMemoryLockedChurches.value + (cleanId to true)
    }
  }

  suspend fun authorizeChurchAdminSession(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
        current[cleanId] = false
        prefs[keyLockedChurchesJson] = serializeLockedMap(current)
        prefs.remove(keyLastAdminBackgroundAtMs)
      }
    } else {
      _inMemoryLockedChurches.value = _inMemoryLockedChurches.value + (cleanId to false)
      _inMemoryTimestamp.value = null
    }
  }

  suspend fun clearChurchAdminLock(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
        current.remove(cleanId)
        prefs[keyLockedChurchesJson] = serializeLockedMap(current)
      }
    } else {
      _inMemoryLockedChurches.value = _inMemoryLockedChurches.value - cleanId
    }
  }

  suspend fun clearAllAdminLocks() {
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        prefs.remove(keyLockedChurchesJson)
        prefs.remove(keyLastAdminBackgroundAtMs)
      }
    } else {
      _inMemoryLockedChurches.value = emptyMap()
      _inMemoryTimestamp.value = null
    }
  }

  suspend fun recordAdminBackgroundTimestamp(epochMillis: Long = System.currentTimeMillis()) {
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        prefs[keyLastAdminBackgroundAtMs] = epochMillis
      }
    } else {
      _inMemoryTimestamp.value = epochMillis
    }
  }

  suspend fun getLastAdminBackgroundTimestamp(): Long? {
    return if (context != null) {
      context.omniAdminLockDataStore.data.map { prefs -> prefs[keyLastAdminBackgroundAtMs] }.first()
    } else {
      _inMemoryTimestamp.value
    }
  }

  suspend fun clearAdminBackgroundTimestamp() {
    if (context != null) {
      context.omniAdminLockDataStore.edit { prefs ->
        prefs.remove(keyLastAdminBackgroundAtMs)
      }
    } else {
      _inMemoryTimestamp.value = null
    }
  }

  private fun parseLockedMap(rawJson: String?): Map<String, Boolean> {
    if (rawJson.isNullOrBlank()) return emptyMap()
    return try {
      val json = JSONObject(rawJson)
      val map = mutableMapOf<String, Boolean>()
      val keys = json.keys()
      while (keys.hasNext()) {
        val k = keys.next()
        map[k] = json.optBoolean(k, false)
      }
      map
    } catch (_: Exception) {
      emptyMap()
    }
  }

  private fun serializeLockedMap(map: Map<String, Boolean>): String {
    val json = JSONObject()
    for ((k, v) in map) {
      json.put(k, v)
    }
    return json.toString()
  }

  companion object {
    fun evaluateOwnership(church: Church, user: FirebaseUser?): OwnershipEvaluationResult {
      if (user == null) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.NO_AUTH_USER,
          isAuthorized = false,
        )
      }
      val email = user.email?.trim()?.lowercase() ?: ""
      val uid = user.uid.trim()
      if (CryptoSecurity.isSuperDeveloperAccount(email)) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.SUPER_DEVELOPER,
          isAuthorized = true,
        )
      }
      val ownerUid = church.ownerId.trim()
      val adminGoogleUid = (church.adminGoogleUid ?: "").trim()
      val adminGoogleEmail = church.adminGoogleEmail.trim().lowercase()

      if (ownerUid.isNotEmpty() && ownerUid == uid) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.MATCHED_UID,
          isAuthorized = true,
        )
      }
      if (adminGoogleUid.isNotEmpty() && adminGoogleUid == uid) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.MATCHED_UID,
          isAuthorized = true,
        )
      }
      if (adminGoogleEmail.isNotEmpty() && email.isNotEmpty() && adminGoogleEmail == email) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.MATCHED_EMAIL_NEEDS_UID_HEAL,
          isAuthorized = true,
          expectedOwnerEmail = adminGoogleEmail,
        )
      }
      if (ownerUid.isEmpty() && adminGoogleUid.isEmpty() && adminGoogleEmail.isEmpty()) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.UNCLAIMED_LEGACY_CHURCH,
          isAuthorized = true,
        )
      }
      return OwnershipEvaluationResult(
        status = OwnershipEvaluationStatus.MISMATCHED_OWNER,
        isAuthorized = false,
        expectedOwnerEmail = adminGoogleEmail.ifEmpty { ownerUid },
      )
    }

    fun canQuickPinUnlock(church: Church, user: FirebaseUser?): Boolean {
      if (!church.quickPinEnabled) return false
      val eval = evaluateOwnership(church, user)
      return eval.isAuthorized
    }

    fun hasMatchingChurchCloudOwner(church: Church, user: FirebaseUser?): Boolean {
      return com.zoutiw.omnichurch.data.models.OmniAuthEvaluator.hasMatchingChurchCloudOwner(church, user)
    }

    fun isChurchAdminAuthorized(
      church: Church,
      firebaseUser: FirebaseUser?,
      lockedChurches: Map<String, Boolean>,
    ): Boolean {
      val isLocked = lockedChurches[church.id] == true
      if (isLocked) return false
      return evaluateOwnership(church, firebaseUser).isAuthorized
    }
  }
}
