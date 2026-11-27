package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.ChurchLocationDetector
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Cohesive UseCase handling Church Admin registration (or multi-church account reuse)
 * and new Church creation via Email/Password or Google Sign-In.
 */
internal class AdminChurchRegistrationUseCase(
  private val db: FirebaseFirestore,
  private val firebaseAuth: FirebaseAuth,
  private val sessionLockManager: SessionLockManager,
) {
  suspend fun registerOrReuseAdminWithEmailPassword(
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
          ?: return Result.failure(
            OmniAuthException(AuthErrorMessages.localizedGenericAuthError(languageCode))
          )

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
            ?: return Result.failure(
              OmniAuthException(AuthErrorMessages.localizedEmailInUseWrongPassword(languageCode))
            )
        FirestorePayloadSanitizer.refreshAuthToken(existingUser)
        Result.success(existingUser)
      } catch (signInErr: CancellationException) {
        throw signInErr
      } catch (signInErr: Exception) {
        Log.w(TAG, "Multi-church owner reuse sign-in failed for $cleanEmail", signInErr)
        Result.failure(
          OmniAuthException(AuthErrorMessages.localizedEmailInUseWrongPassword(languageCode))
        )
      }
    } catch (e: Exception) {
      Result.failure(OmniAuthException(AuthErrorMessages.mapFirebaseAuthException(e, languageCode)))
    }
  }

  suspend fun createChurchWithEmailPassword(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> {
    val cleanName = churchName.trim()
    if (cleanName.length < 2) {
      return Result.failure(
        OmniAuthException(AuthErrorMessages.localizedInvalidChurchName(languageCode))
      )
    }
    if (CryptoSecurity.isSuperDeveloperAccount(email)) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
    }
    val userResult =
      registerOrReuseAdminWithEmailPassword(
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
      languageCode = languageCode,
    )
  }

  suspend fun createChurchWithSignedInGoogleUser(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    languageCode: String,
  ): Result<Church> {
    val cleanName = churchName.trim()
    if (cleanName.length < 2) {
      return Result.failure(
        OmniAuthException(AuthErrorMessages.localizedInvalidChurchName(languageCode))
      )
    }
    val user =
      firebaseAuth.currentUser
        ?: return Result.failure(
          OmniAuthException(AuthErrorMessages.localizedNoSignedInUser(languageCode))
        )
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
      languageCode = languageCode,
    )
  }

  private suspend fun persistNewChurchDocument(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    ownerEmail: String,
    ownerName: String?,
    ownerPhoto: String?,
    authMethod: String,
    languageCode: String,
  ): Result<Church> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(firebaseAuth, "kreye yon nouvo legliz")
        .getOrElse {
          return Result.failure(
            OmniAuthException(it.localizedMessage ?: "Sesyon Firebase ekspire.")
          )
        }

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
      val formattedCity =
        ChurchLocationDetector.formatChurchLocationWithDetectedCountry(
          rawCity = churchAddress,
          language = languageCode,
        )
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
          "city" to formattedCity,
          "address" to formattedCity,
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
          city = formattedCity,
          address = formattedCity,
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

  companion object {
    private const val TAG = "AdminChurchRegistrationUseCase"
  }
}
