package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OwnershipEvaluationStatus
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

private const val ADMIN_SEC_TAG = "AuthRepository"

internal suspend fun AuthRepository.registerOrReuseAdminWithEmailPasswordImpl(
  email: String,
  password: String,
  displayName: String?,
  languageCode: String,
): Result<FirebaseUser> {
  val cleanEmail = email.trim().lowercase(Locale.US)
  if (!AuthErrorMessages.isValidEmail(cleanEmail)) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidEmail(languageCode)))
  }
  if (password.length < 6) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedWeakPassword(languageCode)))
  }

  return try {
    val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
    val user =
      authResult.user
        ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode)))

    val cleanDisplay = displayName?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanDisplay != null) {
      runCatching {
        user.updateProfile(
          UserProfileChangeRequest.Builder().setDisplayName(cleanDisplay).build()
        ).await()
      }
    }
    runCatching { user.sendEmailVerification() }
    FirestorePayloadSanitizer.refreshAuthToken(user)
    Result.success(user)
  } catch (e: CancellationException) {
    throw e
  } catch (collision: FirebaseAuthUserCollisionException) {
    try {
      val signInResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
      val existingUser =
        signInResult.user
          ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedEmailInUseWrongPassword(languageCode)))
      FirestorePayloadSanitizer.refreshAuthToken(existingUser)
      Result.success(existingUser)
    } catch (signInErr: CancellationException) {
      throw signInErr
    } catch (signInErr: Exception) {
      Log.w(ADMIN_SEC_TAG, "Multi-church owner reuse sign-in failed for $cleanEmail", signInErr)
      Result.failure(OmniAuthException(AuthErrorMessages.localizedEmailInUseWrongPassword(languageCode)))
    }
  } catch (e: Exception) {
    Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
  }
}

internal suspend fun AuthRepository.createChurchWithEmailPasswordImpl(
  churchName: String,
  churchCode: String,
  churchAddress: String,
  email: String,
  password: String,
  languageCode: String,
): Result<Church> {
  val cleanName = churchName.trim()
  if (cleanName.length < 2) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidChurchName(languageCode)))
  }
  if (CryptoSecurity.isSuperDeveloperAccount(email)) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
  }
  val userResult =
    registerOrReuseAdminWithEmailPasswordImpl(
      email = email,
      password = password,
      displayName = cleanName,
      languageCode = languageCode,
    )
  val user = userResult.getOrElse { return Result.failure(it) }

  return persistNewChurchDocument(
    churchName = cleanName,
    churchCode = churchCode,
    churchAddress = churchAddress,
    ownerEmail = (user.email ?: email).trim().lowercase(Locale.US),
    ownerName = user.displayName?.takeIf { it.isNotBlank() },
    ownerPhoto = user.photoUrl?.toString(),
    authMethod = "email_password",
  )
}

internal suspend fun AuthRepository.createChurchWithSignedInGoogleUserImpl(
  churchName: String,
  churchCode: String,
  churchAddress: String,
  languageCode: String,
): Result<Church> {
  val cleanName = churchName.trim()
  if (cleanName.length < 2) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidChurchName(languageCode)))
  }
  val user =
    firebaseAuth.currentUser
      ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedNoSignedInUser(languageCode)))
  val cleanEmail = user.email?.trim()?.lowercase(Locale.US).orEmpty()
  if (cleanEmail.isEmpty()) {
    return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidEmail(languageCode)))
  }
  if (CryptoSecurity.isSuperDeveloperAccount(cleanEmail)) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
  }

  return persistNewChurchDocument(
    churchName = cleanName,
    churchCode = churchCode,
    churchAddress = churchAddress,
    ownerEmail = cleanEmail,
    ownerName = user.displayName?.takeIf { it.isNotBlank() },
    ownerPhoto = user.photoUrl?.toString(),
    authMethod = "google",
  )
}

private suspend fun AuthRepository.persistNewChurchDocument(
  churchName: String,
  churchCode: String,
  churchAddress: String,
  ownerEmail: String,
  ownerName: String?,
  ownerPhoto: String?,
  authMethod: String,
): Result<Church> {
  val activeUser =
    FirestorePayloadSanitizer.requireActiveAdminAuth(firebaseAuth, "kreye yon nouvo legliz")
      .getOrElse { return Result.failure(OmniAuthException(it.localizedMessage ?: "Sesyon Firebase ekspire.")) }

  val cleanCode =
    churchCode
      .trim()
      .uppercase(Locale.US)
      .replace(Regex("[^A-Z0-9]"), "")
      .takeIf { it.length >= 3 }
      ?: "CH${(kotlin.math.abs(churchName.hashCode()) % 900 + 100)}"
  val docRef = db.collection("churches").document(cleanCode)
  return try {
    val existingSnap = docRef.get().await()
    val targetRef =
      if (existingSnap.exists()) db.collection("churches").document() else docRef
    if (!FirestorePayloadSanitizer.isValidChurchId(targetRef.id)) {
      return Result.failure(OmniAuthException("ID legliz la pa valid."))
    }

    val resolvedUid = activeUser.uid
    val resolvedEmail = (activeUser.email ?: ownerEmail).trim().lowercase(Locale.US)
    val rawPayload =
      hashMapOf<String, Any?>(
        "id" to targetRef.id,
        "code" to cleanCode,
        "name" to churchName,
        "ownerId" to resolvedUid,
        "adminGoogleEmail" to resolvedEmail,
        "adminGoogleUid" to resolvedUid,
        "adminGoogleName" to ownerName,
        "adminGooglePhoto" to ownerPhoto,
        "adminAuthMethod" to authMethod,
        "quickPinEnabled" to false,
        "autoLockMinutes" to 5,
        "address" to churchAddress.trim().takeIf { it.isNotBlank() },
        "tagline" to "Legliz la nan poch ou",
        "status" to "active",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val payload =
      FirestorePayloadSanitizer.sanitizeChurchCreate(
        raw = rawPayload,
        docId = targetRef.id,
        currentUser = activeUser,
      )
    targetRef.set(payload).await()
    sessionLockManager.authorizeChurchAdminSession(targetRef.id)

    val createdChurch =
      Church(
        id = targetRef.id,
        code = cleanCode,
        name = churchName,
        address = churchAddress.trim(),
        ownerId = resolvedUid,
        adminGoogleEmail = resolvedEmail,
        adminGoogleUid = resolvedUid,
        adminGoogleName = ownerName,
        adminGooglePhoto = ownerPhoto,
        adminAuthMethod = authMethod,
        quickPinEnabled = false,
        autoLockMinutes = 5,
      )
    Result.success(createdChurch)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.CREATE, docRef.path)
    Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan kreyasyon legliz la."))
  }
}

internal suspend fun AuthRepository.signInAdminWithEmailAndPasswordChurchImpl(
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
        ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode)))
    FirestorePayloadSanitizer.refreshAuthToken(user)
    verifyAndHealChurchOwnership(church, user, "email_password", languageCode)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
  }
}

internal suspend fun AuthRepository.verifySignedInAdminGoogleAccountImpl(
  church: Church,
  languageCode: String,
): Result<Church> {
  val user =
    firebaseAuth.currentUser
      ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedNoSignedInUser(languageCode)))
  FirestorePayloadSanitizer.refreshAuthToken(user)
  return verifyAndHealChurchOwnership(church, user, "google", languageCode)
}

internal suspend fun AuthRepository.signInAdminWithGoogleIdTokenImpl(
  church: Church,
  idToken: String,
  languageCode: String,
): Result<Church> {
  return try {
    val cred = GoogleAuthProvider.getCredential(idToken, null)
    val authResult = firebaseAuth.signInWithCredential(cred).await()
    val user =
      authResult.user
        ?: return Result.failure(OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode)))
    FirestorePayloadSanitizer.refreshAuthToken(user)
    verifyAndHealChurchOwnership(church, user, "google", languageCode)
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
  }
}

private suspend fun AuthRepository.verifyAndHealChurchOwnership(
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
          Log.w(ADMIN_SEC_TAG, "Auto-heal UID update warning for ${church.id}: ${e.message}")
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
      val reboundChurch = resolveAutoRebindChurchIfUniquelyOwned(church.id)
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
