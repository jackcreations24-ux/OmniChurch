package com.zoutiw.omnichurch.core.utilities

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.zoutiw.omnichurch.data.models.Church
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow
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

/**
 * Manages persisted per-church Admin lock state, inactivity tracking, and strict multi-tenant
 * ownership evaluation so locked or mismatched admin sessions can never bypass re-authentication.
 */
class SessionLockManager(private val context: Context) {
  private val keyLockedChurchesJson = stringPreferencesKey("locked_churches_json_v1")
  private val keyLastAdminBackgroundAtMs = longPreferencesKey("last_admin_background_at_ms_v1")

  val lockedChurchesFlow: Flow<Map<String, Boolean>> =
    context.omniAdminLockDataStore.data.map { prefs ->
      parseLockedMap(prefs[keyLockedChurchesJson])
    }

  suspend fun isChurchAdminLocked(churchId: String): Boolean {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return false
    return lockedChurchesFlow.first()[cleanId] == true
  }

  suspend fun lockChurchAdminSession(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    context.omniAdminLockDataStore.edit { prefs ->
      val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
      current[cleanId] = true
      prefs[keyLockedChurchesJson] = serializeLockedMap(current)
    }
  }

  suspend fun authorizeChurchAdminSession(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    context.omniAdminLockDataStore.edit { prefs ->
      val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
      current[cleanId] = false
      prefs[keyLockedChurchesJson] = serializeLockedMap(current)
      prefs.remove(keyLastAdminBackgroundAtMs)
    }
  }

  suspend fun clearChurchAdminLock(churchId: String) {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) return
    context.omniAdminLockDataStore.edit { prefs ->
      val current = parseLockedMap(prefs[keyLockedChurchesJson]).toMutableMap()
      current.remove(cleanId)
      prefs[keyLockedChurchesJson] = serializeLockedMap(current)
    }
  }

  suspend fun clearAllAdminLocks() {
    context.omniAdminLockDataStore.edit { prefs ->
      prefs.remove(keyLockedChurchesJson)
      prefs.remove(keyLastAdminBackgroundAtMs)
    }
  }

  suspend fun recordAdminBackgroundTimestamp(epochMillis: Long = System.currentTimeMillis()) {
    context.omniAdminLockDataStore.edit { prefs ->
      prefs[keyLastAdminBackgroundAtMs] = epochMillis
    }
  }

  suspend fun getLastAdminBackgroundTimestamp(): Long? {
    return context.omniAdminLockDataStore.data.map { prefs -> prefs[keyLastAdminBackgroundAtMs] }.first()
  }

  suspend fun clearAdminBackgroundTimestamp() {
    context.omniAdminLockDataStore.edit { prefs ->
      prefs.remove(keyLastAdminBackgroundAtMs)
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
    map.forEach { (k, v) ->
      if (k.isNotBlank()) json.put(k, v)
    }
    return json.toString()
  }

  companion object {
    fun evaluateOwnership(
      church: Church,
      firebaseUser: FirebaseUser?,
    ): OwnershipEvaluationResult =
      evaluateOwnership(
        church = church,
        currentUserUid = firebaseUser?.uid,
        currentUserEmail = firebaseUser?.email,
      )

    fun evaluateOwnership(
      church: Church,
      currentUserUid: String?,
      currentUserEmail: String?,
    ): OwnershipEvaluationResult {
      val uid = currentUserUid?.trim().orEmpty()
      val email = currentUserEmail?.trim().orEmpty()
      if (uid.isEmpty() && email.isEmpty()) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.NO_AUTH_USER,
          isAuthorized = false,
          expectedOwnerEmail = church.adminGoogleEmail?.trim()?.takeIf { it.isNotEmpty() },
        )
      }

      if (CryptoSecurity.isSuperDeveloperAccount(email)) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.SUPER_DEVELOPER,
          isAuthorized = true,
        )
      }

      val churchAdminUid = church.adminGoogleUid?.trim().orEmpty()
      val churchAdminEmail = church.adminGoogleEmail?.trim().orEmpty()

      if (churchAdminUid.isNotEmpty() && uid.isNotEmpty() && churchAdminUid == uid) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.MATCHED_UID,
          isAuthorized = true,
        )
      }

      if (
        churchAdminEmail.isNotEmpty() &&
          email.isNotEmpty() &&
          churchAdminEmail.equals(email, ignoreCase = true)
      ) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.MATCHED_EMAIL_NEEDS_UID_HEAL,
          isAuthorized = true,
        )
      }

      if (churchAdminUid.isEmpty() && churchAdminEmail.isEmpty()) {
        return OwnershipEvaluationResult(
          status = OwnershipEvaluationStatus.UNCLAIMED_LEGACY_CHURCH,
          isAuthorized = true,
        )
      }

      return OwnershipEvaluationResult(
        status = OwnershipEvaluationStatus.MISMATCHED_OWNER,
        isAuthorized = false,
        expectedOwnerEmail = churchAdminEmail.takeIf { it.isNotEmpty() },
      )
    }

    /**
     * Checks strict cloud owner identity match for `church`:
     * Super Developer OR `adminGoogleUid == uid` OR `adminGoogleEmail == email`.
     * Does NOT treat an unclaimed legacy church as already matching a cloud owner.
     */
    fun hasMatchingChurchCloudOwner(church: Church, firebaseUser: FirebaseUser?): Boolean =
      hasMatchingChurchCloudOwner(church, firebaseUser?.uid, firebaseUser?.email)

    fun hasMatchingChurchCloudOwner(
      church: Church,
      currentUserUid: String?,
      currentUserEmail: String?,
    ): Boolean {
      val status = evaluateOwnership(church, currentUserUid, currentUserEmail).status
      return status == OwnershipEvaluationStatus.SUPER_DEVELOPER ||
        status == OwnershipEvaluationStatus.MATCHED_UID ||
        status == OwnershipEvaluationStatus.MATCHED_EMAIL_NEEDS_UID_HEAL
    }

    /**
     * Quick PIN Resume is ONLY allowed when:
     * 1. `church.quickPinEnabled == true`
     * 2. `!church.adminPasscode.isNullOrBlank()`
     * 3. Firebase Auth currently has a live user matching the church's cloud owner (or Super Developer).
     */
    fun canQuickPinUnlock(church: Church, firebaseUser: FirebaseUser?): Boolean =
      canQuickPinUnlock(church, firebaseUser?.uid, firebaseUser?.email)

    fun canQuickPinUnlock(
      church: Church,
      currentUserUid: String?,
      currentUserEmail: String?,
    ): Boolean {
      return church.quickPinEnabled == true &&
        !church.adminPasscode.isNullOrBlank() &&
        hasMatchingChurchCloudOwner(church, currentUserUid, currentUserEmail)
    }

    /**
     * Anti-bypass guard: returns true ONLY when the church is NOT locked in `lockedChurches`
     * AND the active session satisfies cloud ownership (or legacy PIN-only church with explicit unlock).
     */
    fun isChurchAdminAuthorized(
      church: Church,
      firebaseUser: FirebaseUser?,
      lockedChurches: Map<String, Boolean>,
    ): Boolean =
      isChurchAdminAuthorized(
        church = church,
        currentUserUid = firebaseUser?.uid,
        currentUserEmail = firebaseUser?.email,
        lockedChurches = lockedChurches,
      )

    fun isChurchAdminAuthorized(
      church: Church,
      currentUserUid: String?,
      currentUserEmail: String?,
      lockedChurches: Map<String, Boolean>,
    ): Boolean {
      if (church.id.isBlank()) return false
      if (lockedChurches[church.id] == true) return false
      return if (church.hasCloudOwner) {
        hasMatchingChurchCloudOwner(church, currentUserUid, currentUserEmail)
      } else {
        // Legacy PIN-only church without cloud owner yet requires explicit non-locked session
        lockedChurches[church.id] == false
      }
    }
  }
}
