package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FieldValue
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal suspend fun AuthRepository.verifyQuickPinAndUpgradeIfNeededImpl(
  church: Church,
  enteredPin: String,
  languageCode: String,
): Result<Church> {
  val liveUser = firebaseAuth.currentUser
  if (!SessionLockManager.canQuickPinUnlock(church, liveUser)) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedQuickPinUnavailable(languageCode)))
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

internal suspend fun AuthRepository.migrateLegacyPinChurchToCloudOwnerImpl(
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
    registerOrReuseAdminWithEmailPasswordImpl(
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

internal suspend fun AuthRepository.updateChurchSecuritySettingsImpl(
  church: Church,
  quickPinEnabled: Boolean,
  newPinOrNull: String?,
  autoLockMinutes: Int,
  languageCode: String,
): Result<Church> {
  val activeUser =
    FirestorePayloadSanitizer.requireActiveAdminAuth(firebaseAuth, "modifye sekirite legliz la")
      .getOrElse { return Result.failure(OmniAuthException(it.localizedMessage ?: AuthErrorMessages.localizedNoSignedInUser(languageCode))) }

  val cleanNewPin = newPinOrNull?.trim()?.takeIf { it.isNotEmpty() }
  if (cleanNewPin != null && !cleanNewPin.matches(Regex("^\\d{4,6}$"))) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedPinFormatError(languageCode)))
  }
  if (quickPinEnabled && cleanNewPin == null && church.adminPasscode.isNullOrBlank()) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedQuickPinSetupRequired(languageCode)))
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
    Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan aktyalizasyon sekirite legliz la."))
  }
}
