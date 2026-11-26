package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OwnershipEvaluationStatus
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Cohesive UseCase consolidating church ownership verification, UID auto-healing,
 * quick-PIN verification & hash upgrade, legacy PIN cloud-owner migration,
 * and church security settings updates.
 */
internal class AdminChurchSecurityUseCase(
  private val db: FirebaseFirestore,
  private val firebaseAuth: FirebaseAuth,
  private val sessionLockManager: SessionLockManager,
  private val registrationUseCase: AdminChurchRegistrationUseCase,
  private val adminChurchAuthUseCase: AdminChurchAuthUseCase,
) {
  suspend fun signInAdminWithEmailAndPasswordForChurch(
    church: Church,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (!AuthErrorMessages.isValidEmail(cleanEmail)) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidEmail(languageCode)))
    }
    if (password.isBlank()) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedEmptyPassword(languageCode)))
    }

    return try {
      val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
      val user =
        authResult.user
          ?: return Result.failure(
            OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode))
          )
      FirestorePayloadSanitizer.refreshAuthToken(user)
      verifyAndHealChurchOwnership(church, user, "email_password", languageCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
    }
  }

  suspend fun verifySignedInAdminGoogleAccount(
    church: Church,
    languageCode: String,
  ): Result<Church> {
    val user =
      firebaseAuth.currentUser
        ?: return Result.failure(
          OmniAuthException(AuthErrorMessages.localizedNoSignedInUser(languageCode))
        )
    FirestorePayloadSanitizer.refreshAuthToken(user)
    return verifyAndHealChurchOwnership(church, user, "google", languageCode)
  }

  suspend fun signInAdminWithGoogleIdToken(
    church: Church,
    idToken: String,
    languageCode: String,
  ): Result<Church> {
    return try {
      val cred = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = firebaseAuth.signInWithCredential(cred).await()
      val user =
        authResult.user
          ?: return Result.failure(
            OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode))
          )
      FirestorePayloadSanitizer.refreshAuthToken(user)
      verifyAndHealChurchOwnership(church, user, "google", languageCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
    }
  }

  suspend fun verifyQuickPinAndUpgradeIfNeeded(
    church: Church,
    enteredPin: String,
    languageCode: String,
  ): Result<Church> {
    val liveUser = firebaseAuth.currentUser
    if (!SessionLockManager.canQuickPinUnlock(church, liveUser)) {
      return Result.failure(
        OmniAuthException(AuthErrorMessages.localizedQuickPinUnavailable(languageCode))
      )
    }

    val verification =
      CryptoSecurity.verifyAdminPasscode(
        entered = enteredPin,
        stored = church.adminPasscode,
        churchId = church.id,
        fallbackSalt = church.code,
      )
    if (!verification.matches) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedWrongPin(languageCode)))
    }

    var updatedChurch = church
    if (verification.needsUpgrade && liveUser != null) {
      val upgradedHash = CryptoSecurity.hashAdminPasscode(enteredPin, church.id)
      val upgradedAt = CryptoSecurity.isoTimestampNow()
      val sanitizedUpgrade =
        FirestorePayloadSanitizer.sanitizeChurchUpdate(
          raw =
            hashMapOf(
              "adminPasscode" to upgradedHash,
              "adminPasscodeUpdatedAt" to upgradedAt,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          currentUser = liveUser,
        )
      try {
        db.collection("churches").document(church.id).update(sanitizedUpgrade).await()
        updatedChurch = church.copy(adminPasscode = upgradedHash, adminPasscodeUpdatedAt = upgradedAt)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "churches/${church.id}")
      }
    }

    if (liveUser != null) {
      FirestorePayloadSanitizer.refreshAuthToken(liveUser)
    }
    sessionLockManager.authorizeChurchAdminSession(church.id)
    return Result.success(updatedChurch)
  }

  fun verifyLegacyPinStep(
    church: Church,
    enteredPin: String,
    languageCode: String,
  ): Result<Unit> {
    val verification =
      CryptoSecurity.verifyAdminPasscode(
        entered = enteredPin,
        stored = church.adminPasscode,
        churchId = church.id,
        fallbackSalt = church.code,
      )
    return if (verification.matches) {
      Result.success(Unit)
    } else {
      Result.failure(OmniAuthException(AuthErrorMessages.localizedWrongPin(languageCode)))
    }
  }

  suspend fun migrateLegacyPinChurchToCloudOwner(
    church: Church,
    verifiedOldPin: String,
    adminEmail: String,
    newPassword: String,
    languageCode: String,
  ): Result<Church> {
    val verification =
      CryptoSecurity.verifyAdminPasscode(
        entered = verifiedOldPin,
        stored = church.adminPasscode,
        churchId = church.id,
        fallbackSalt = church.code,
      )
    if (!verification.matches) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedWrongPin(languageCode)))
    }

    val userResult =
      registrationUseCase.registerOrReuseAdminWithEmailPassword(
        email = adminEmail,
        password = newPassword,
        displayName = church.pastor.takeIf { it.isNotBlank() } ?: church.name,
        languageCode = languageCode,
      )
    val user = userResult.getOrElse { return Result.failure(it) }

    val cleanEmail = (user.email ?: adminEmail).trim().lowercase(Locale.US)
    val upgradedHash =
      if (verification.needsUpgrade) {
        CryptoSecurity.hashAdminPasscode(verifiedOldPin, church.id)
      } else {
        church.adminPasscode
      }
    val upgradedAt =
      if (verification.needsUpgrade) CryptoSecurity.isoTimestampNow() else church.adminPasscodeUpdatedAt

    val updates =
      FirestorePayloadSanitizer.sanitizeChurchUpdate(
        raw =
          hashMapOf(
            "adminGoogleEmail" to cleanEmail,
            "adminGoogleUid" to user.uid,
            "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
            "adminAuthMethod" to "email_password",
            "adminPasscode" to upgradedHash,
            "adminPasscodeUpdatedAt" to upgradedAt,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        currentUser = user,
      )
    val docPath = "churches/${church.id}"
    return try {
      db.collection("churches").document(church.id).update(updates).await()
      sessionLockManager.authorizeChurchAdminSession(church.id)
      Result.success(
        church.copy(
          adminGoogleEmail = cleanEmail,
          adminGoogleUid = user.uid,
          adminGoogleName = user.displayName ?: church.adminGoogleName,
          adminAuthMethod = "email_password",
          adminPasscode = upgradedHash,
          adminPasscodeUpdatedAt = upgradedAt,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan migrasyon kont legliz la."))
    }
  }

  suspend fun updateChurchSecuritySettings(
    church: Church,
    quickPinEnabled: Boolean,
    newPinOrNull: String?,
    autoLockMinutes: Int,
    languageCode: String,
  ): Result<Church> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(firebaseAuth, "modifye sekirite legliz la")
        .getOrElse {
          return Result.failure(
            OmniAuthException(
              it.localizedMessage ?: AuthErrorMessages.localizedNoSignedInUser(languageCode)
            )
          )
        }

    val cleanNewPin = newPinOrNull?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanNewPin != null && !cleanNewPin.matches(Regex("^\\d{4,6}$"))) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedPinFormatError(languageCode)))
    }
    if (quickPinEnabled && cleanNewPin == null && church.adminPasscode.isNullOrBlank()) {
      return Result.failure(
        OmniAuthException(AuthErrorMessages.localizedQuickPinSetupRequired(languageCode))
      )
    }

    val safeAutoLock = autoLockMinutes.coerceIn(0, 120)
    val nextHash =
      if (cleanNewPin != null) {
        CryptoSecurity.hashAdminPasscode(cleanNewPin, church.id)
      } else {
        church.adminPasscode
      }
    val nextUpdatedAt =
      if (cleanNewPin != null) CryptoSecurity.isoTimestampNow() else church.adminPasscodeUpdatedAt

    val updates =
      FirestorePayloadSanitizer.sanitizeChurchUpdate(
        raw =
          hashMapOf(
            "quickPinEnabled" to quickPinEnabled,
            "autoLockMinutes" to safeAutoLock,
            "adminPasscode" to nextHash,
            "adminPasscodeUpdatedAt" to nextUpdatedAt,
            "updatedAt" to FieldValue.serverTimestamp(),
          ),
        currentUser = activeUser,
      )
    val docPath = "churches/${church.id}"
    return try {
      db.collection("churches").document(church.id).update(updates).await()
      Result.success(
        church.copy(
          quickPinEnabled = quickPinEnabled,
          autoLockMinutes = safeAutoLock,
          adminPasscode = nextHash,
          adminPasscodeUpdatedAt = nextUpdatedAt,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docPath)
      Result.failure(
        OmniAuthException(e.localizedMessage ?: "Erè pandan aktyalizasyon sekirite legliz la.")
      )
    }
  }

  private suspend fun verifyAndHealChurchOwnership(
    church: Church,
    user: FirebaseUser,
    authMethodUsed: String,
    languageCode: String,
  ): Result<Church> {
    val eval = SessionLockManager.evaluateOwnership(church, user)
    val docPath = "churches/${church.id}"
    return when (eval.status) {
      OwnershipEvaluationStatus.SUPER_DEVELOPER,
      OwnershipEvaluationStatus.MATCHED_UID -> {
        sessionLockManager.authorizeChurchAdminSession(church.id)
        Result.success(church)
      }
      OwnershipEvaluationStatus.MATCHED_EMAIL_NEEDS_UID_HEAL -> {
        if (church.adminGoogleUid.isNullOrBlank()) {
          val resolvedMethod = AuthErrorMessages.mergeAuthMethods(church.adminAuthMethod, authMethodUsed)
          // Preserve custom or explicitly cleared ("") adminGooglePhoto; only seed Google photo when null.
          val resolvedPhotoForHeal =
            if (church.adminGooglePhoto == null) user.photoUrl?.toString()?.takeIf { it.isNotEmpty() } else null
          val rawUpdates =
            hashMapOf<String, Any?>(
              "adminGoogleUid" to user.uid,
              "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
              "adminAuthMethod" to resolvedMethod,
              "updatedAt" to FieldValue.serverTimestamp(),
            ).apply {
              if (resolvedPhotoForHeal != null) {
                put("adminGooglePhoto", resolvedPhotoForHeal)
              }
            }
          val updates =
            FirestorePayloadSanitizer.sanitizeChurchUpdate(
              raw = rawUpdates,
              currentUser = user,
            )
          try {
            db.collection("churches").document(church.id).update(updates).await()
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Log.w(TAG, "Auto-heal UID update warning for ${church.id}: ${e.message}")
          }
          sessionLockManager.authorizeChurchAdminSession(church.id)
          Result.success(
            church.copy(
              adminGoogleUid = user.uid,
              adminGoogleName = user.displayName ?: church.adminGoogleName,
              adminGooglePhoto = church.adminGooglePhoto ?: user.photoUrl?.toString(),
              adminAuthMethod = resolvedMethod,
            )
          )
        } else {
          sessionLockManager.authorizeChurchAdminSession(church.id)
          Result.success(church)
        }
      }
      OwnershipEvaluationStatus.UNCLAIMED_LEGACY_CHURCH -> {
        val cleanEmail = (user.email ?: "").trim().lowercase(Locale.US)
        val resolvedPhotoForClaim =
          if (church.adminGooglePhoto == null) user.photoUrl?.toString()?.takeIf { it.isNotEmpty() } else null
        val rawUpdates =
          hashMapOf<String, Any?>(
            "adminGoogleEmail" to cleanEmail.takeIf { it.isNotEmpty() },
            "adminGoogleUid" to user.uid,
            "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
            "adminAuthMethod" to authMethodUsed,
            "updatedAt" to FieldValue.serverTimestamp(),
          ).apply {
            if (resolvedPhotoForClaim != null) {
              put("adminGooglePhoto", resolvedPhotoForClaim)
            }
          }
        val updates =
          FirestorePayloadSanitizer.sanitizeChurchUpdate(
            raw = rawUpdates,
            currentUser = user,
          )
        try {
          db.collection("churches").document(church.id).update(updates).await()
          sessionLockManager.authorizeChurchAdminSession(church.id)
          Result.success(
            church.copy(
              adminGoogleEmail = cleanEmail.ifEmpty { church.adminGoogleEmail },
              adminGoogleUid = user.uid,
              adminGoogleName = user.displayName ?: church.adminGoogleName,
              adminGooglePhoto = church.adminGooglePhoto ?: user.photoUrl?.toString(),
              adminAuthMethod = authMethodUsed,
            )
          )
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.UPDATE, docPath)
          Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan reklamasyon legliz la."))
        }
      }
      OwnershipEvaluationStatus.MISMATCHED_OWNER -> {
        val reboundChurch = adminChurchAuthUseCase.resolveAutoRebindChurchIfUniquelyOwned(church.id)
        if (reboundChurch != null) {
          sessionLockManager.authorizeChurchAdminSession(reboundChurch.id)
          Result.success(reboundChurch)
        } else {
          Result.failure(
            OmniAuthException(
              AuthErrorMessages.localizedMismatchedOwnerError(
                languageCode = languageCode,
                churchName = church.name.ifBlank { church.effectiveCode },
                expectedEmail = eval.expectedOwnerEmail,
                currentEmail = user.email,
              )
            )
          )
        }
      }
      OwnershipEvaluationStatus.NO_AUTH_USER -> {
        Result.failure(OmniAuthException(AuthErrorMessages.localizedNoSignedInUser(languageCode)))
      }
    }
  }

  companion object {
    private const val TAG = "AdminChurchSecurityUseCase"
  }
}
