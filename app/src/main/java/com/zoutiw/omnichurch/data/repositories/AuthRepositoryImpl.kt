package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.OwnershipEvaluationStatus
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Production AuthRepositoryImpl mirroring the Web/PC OmniChurch Pro authentication & multi-tenant
 * ownership rules using the explicit named Firestore database, FirebaseAuth Email+Password, Google
 * Sign-In credentials, SHA-256:v1 Quick PINs, and SessionLockManager anti-bypass state.
 */
class AuthRepositoryImpl(
  private val db: FirebaseFirestore,
  private val firebaseAuth: FirebaseAuth,
  private val sessionLockManager: SessionLockManager,
  private val preferencesManager: OmniPreferencesManager,
) : AuthRepository {

  override val currentFirebaseUserFlow: Flow<FirebaseUser?> = callbackFlow {
    trySend(firebaseAuth.currentUser)
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    firebaseAuth.addAuthStateListener(listener)
    awaitClose { firebaseAuth.removeAuthStateListener(listener) }
  }

  override val lockedChurchesFlow: Flow<Map<String, Boolean>> =
    sessionLockManager.lockedChurchesFlow

  override fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

  override suspend fun registerOrReuseAdminWithEmailPassword(
    email: String,
    password: String,
    displayName: String?,
    languageCode: String,
  ): Result<FirebaseUser> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (!isValidEmail(cleanEmail)) {
      return Result.failure(OmniAuthException(localizedInvalidEmail(languageCode)))
    }
    if (password.length < 6) {
      return Result.failure(OmniAuthException(localizedWeakPassword(languageCode)))
    }

    return try {
      val authResult = firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).await()
      val user =
        authResult.user
          ?: return Result.failure(OmniAuthException(localizedGenericAuthError(languageCode)))

      val cleanDisplay = displayName?.trim()?.takeIf { it.isNotEmpty() }
      if (cleanDisplay != null) {
        runCatching {
          user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(cleanDisplay).build()
          ).await()
        }
      }
      // Non-blocking verification email
      runCatching { user.sendEmailVerification() }
      Result.success(user)
    } catch (e: CancellationException) {
      throw e
    } catch (collision: FirebaseAuthUserCollisionException) {
      // Multi-Church Owner Reuse Rule: attempt sign-in with the supplied password
      try {
        val signInResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
        val existingUser =
          signInResult.user
            ?: return Result.failure(OmniAuthException(localizedEmailInUseWrongPassword(languageCode)))
        Result.success(existingUser)
      } catch (signInErr: CancellationException) {
        throw signInErr
      } catch (signInErr: Exception) {
        Log.w(TAG, "Multi-church owner reuse sign-in failed for $cleanEmail", signInErr)
        Result.failure(OmniAuthException(localizedEmailInUseWrongPassword(languageCode)))
      }
    } catch (e: Exception) {
      Result.failure(OmniAuthException(mapFirebaseAuthException(e, languageCode)))
    }
  }

  override suspend fun createChurchWithEmailPassword(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> {
    val cleanName = churchName.trim()
    if (cleanName.length < 2) {
      return Result.failure(OmniAuthException(localizedInvalidChurchName(languageCode)))
    }
    if (CryptoSecurity.isSuperDeveloperAccount(email)) {
      return Result.failure(OmniAuthException(com.zoutiw.omnichurch.data.models.OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
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
      ownerUid = user.uid,
      ownerEmail = (user.email ?: email).trim().lowercase(Locale.US),
      ownerName = user.displayName?.takeIf { it.isNotBlank() },
      ownerPhoto = user.photoUrl?.toString(),
      authMethod = "email_password",
    )
  }

  override suspend fun createChurchWithSignedInGoogleUser(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    languageCode: String,
  ): Result<Church> {
    val cleanName = churchName.trim()
    if (cleanName.length < 2) {
      return Result.failure(OmniAuthException(localizedInvalidChurchName(languageCode)))
    }
    val user =
      firebaseAuth.currentUser
        ?: return Result.failure(OmniAuthException(localizedNoSignedInUser(languageCode)))
    val cleanEmail = user.email?.trim()?.lowercase(Locale.US).orEmpty()
    if (cleanEmail.isEmpty()) {
      return Result.failure(OmniAuthException(localizedInvalidEmail(languageCode)))
    }
    if (CryptoSecurity.isSuperDeveloperAccount(cleanEmail)) {
      return Result.failure(OmniAuthException(com.zoutiw.omnichurch.data.models.OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
    }

    return persistNewChurchDocument(
      churchName = cleanName,
      churchCode = churchCode,
      churchAddress = churchAddress,
      ownerUid = user.uid,
      ownerEmail = cleanEmail,
      ownerName = user.displayName?.takeIf { it.isNotBlank() },
      ownerPhoto = user.photoUrl?.toString(),
      authMethod = "google",
    )
  }

  private suspend fun persistNewChurchDocument(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    ownerUid: String,
    ownerEmail: String,
    ownerName: String?,
    ownerPhoto: String?,
    authMethod: String,
  ): Result<Church> {
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

      val payload =
        filterNonNullMap(
          "code" to cleanCode,
          "name" to churchName,
          "ownerId" to ownerUid,
          "adminGoogleEmail" to ownerEmail.lowercase(Locale.US),
          "adminGoogleUid" to ownerUid,
          "adminGoogleName" to ownerName,
          "adminGooglePhoto" to ownerPhoto,
          "adminAuthMethod" to authMethod,
          "quickPinEnabled" to false,
          "autoLockMinutes" to 5,
          "address" to churchAddress.trim().takeIf { it.isNotBlank() },
          "tagline" to "Legliz la nan poch ou",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      targetRef.set(payload).await()
      sessionLockManager.authorizeChurchAdminSession(targetRef.id)

      val createdChurch =
        Church(
          id = targetRef.id,
          code = cleanCode,
          name = churchName,
          address = churchAddress.trim(),
          ownerId = ownerUid,
          adminGoogleEmail = ownerEmail.lowercase(Locale.US),
          adminGoogleUid = ownerUid,
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

  override suspend fun signInAdminWithEmailPassword(
    church: Church,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (!isValidEmail(cleanEmail)) {
      return Result.failure(OmniAuthException(localizedInvalidEmail(languageCode)))
    }
    if (password.isBlank()) {
      return Result.failure(OmniAuthException(localizedEmptyPassword(languageCode)))
    }

    return try {
      val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
      val user =
        authResult.user
          ?: return Result.failure(OmniAuthException(localizedGenericAuthError(languageCode)))
      verifyAndHealChurchOwnership(
        church = church,
        user = user,
        authMethodUsed = "email_password",
        languageCode = languageCode,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(OmniAuthException(mapFirebaseAuthException(e, languageCode)))
    }
  }

  override suspend fun verifySignedInAdminGoogleAccount(
    church: Church,
    languageCode: String,
  ): Result<Church> {
    val user =
      firebaseAuth.currentUser
        ?: return Result.failure(OmniAuthException(localizedNoSignedInUser(languageCode)))
    return verifyAndHealChurchOwnership(
      church = church,
      user = user,
      authMethodUsed = "google",
      languageCode = languageCode,
    )
  }

  override suspend fun signInAdminWithGoogleIdToken(
    church: Church,
    idToken: String,
    languageCode: String,
  ): Result<Church> {
    return try {
      val cred = GoogleAuthProvider.getCredential(idToken, null)
      val authResult = firebaseAuth.signInWithCredential(cred).await()
      val user =
        authResult.user
          ?: return Result.failure(OmniAuthException(localizedGenericAuthError(languageCode)))
      verifyAndHealChurchOwnership(
        church = church,
        user = user,
        authMethodUsed = "google",
        languageCode = languageCode,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(OmniAuthException(mapFirebaseAuthException(e, languageCode)))
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
        // Auto-heal blank adminGoogleUid in Firestore
        if (church.adminGoogleUid.isNullOrBlank()) {
          val resolvedMethod = mergeAuthMethods(church.adminAuthMethod, authMethodUsed)
          val updates =
            filterNonNullMap(
              "adminGoogleUid" to user.uid,
              "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
              "adminGooglePhoto" to user.photoUrl?.toString()?.takeIf { it.isNotEmpty() },
              "adminAuthMethod" to resolvedMethod,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          try {
            db.collection("churches").document(church.id).update(updates).await()
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            // Keep proceeding if only metadata heal failed, or fail cleanly? Rules allow MATCHED_EMAIL via adminGoogleEmail.
            Log.w(TAG, "Auto-heal UID update warning for ${church.id}: ${e.message}")
          }
          sessionLockManager.authorizeChurchAdminSession(church.id)
          Result.success(
            church.copy(
              adminGoogleUid = user.uid,
              adminGoogleName = user.displayName ?: church.adminGoogleName,
              adminGooglePhoto = user.photoUrl?.toString() ?: church.adminGooglePhoto,
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
        val updates =
          filterNonNullMap(
            "adminGoogleEmail" to cleanEmail.takeIf { it.isNotEmpty() },
            "adminGoogleUid" to user.uid,
            "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
            "adminGooglePhoto" to user.photoUrl?.toString()?.takeIf { it.isNotEmpty() },
            "adminAuthMethod" to authMethodUsed,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        try {
          db.collection("churches").document(church.id).update(updates).await()
          sessionLockManager.authorizeChurchAdminSession(church.id)
          Result.success(
            church.copy(
              adminGoogleEmail = cleanEmail.ifEmpty { church.adminGoogleEmail },
              adminGoogleUid = user.uid,
              adminGoogleName = user.displayName ?: church.adminGoogleName,
              adminGooglePhoto = user.photoUrl?.toString() ?: church.adminGooglePhoto,
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
        Result.failure(
          OmniAuthException(
            localizedMismatchedOwnerError(
              languageCode = languageCode,
              churchName = church.name.ifBlank { church.effectiveCode },
              expectedEmail = eval.expectedOwnerEmail,
              currentEmail = user.email,
            )
          )
        )
      }
      OwnershipEvaluationStatus.NO_AUTH_USER -> {
        Result.failure(OmniAuthException(localizedNoSignedInUser(languageCode)))
      }
    }
  }

  override suspend fun sendPasswordResetEmail(
    email: String,
    languageCode: String,
  ): Result<Unit> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (!isValidEmail(cleanEmail)) {
      return Result.failure(OmniAuthException(localizedInvalidEmail(languageCode)))
    }
    return try {
      firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Result.failure(OmniAuthException(mapFirebaseAuthException(e, languageCode)))
    }
  }

  override suspend fun verifyQuickPinAndUpgradeIfNeeded(
    church: Church,
    enteredPin: String,
    languageCode: String,
  ): Result<Church> {
    val liveUser = firebaseAuth.currentUser
    if (!SessionLockManager.canQuickPinUnlock(church, liveUser)) {
      return Result.failure(OmniAuthException(localizedQuickPinUnavailable(languageCode)))
    }

    val verification =
      CryptoSecurity.verifyAdminPasscode(
        entered = enteredPin,
        stored = church.adminPasscode,
        churchId = church.id,
        fallbackSalt = church.code,
      )
    if (!verification.matches) {
      return Result.failure(OmniAuthException(localizedWrongPin(languageCode)))
    }

    var updatedChurch = church
    if (verification.needsUpgrade) {
      val upgradedHash = CryptoSecurity.hashAdminPasscode(enteredPin, church.id)
      val upgradedAt = CryptoSecurity.isoTimestampNow()
      try {
        db
          .collection("churches")
          .document(church.id)
          .update(
            filterNonNullMap(
              "adminPasscode" to upgradedHash,
              "adminPasscodeUpdatedAt" to upgradedAt,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
        updatedChurch =
          church.copy(
            adminPasscode = upgradedHash,
            adminPasscodeUpdatedAt = upgradedAt,
          )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "churches/${church.id}")
      }
    }

    sessionLockManager.authorizeChurchAdminSession(church.id)
    return Result.success(updatedChurch)
  }

  override suspend fun verifyLegacyPinStep(
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
      Result.failure(OmniAuthException(localizedWrongPin(languageCode)))
    }
  }

  override suspend fun migrateLegacyPinChurchToCloudOwner(
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
      return Result.failure(OmniAuthException(localizedWrongPin(languageCode)))
    }

    val userResult =
      registerOrReuseAdminWithEmailPassword(
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
      filterNonNullMap(
        "adminGoogleEmail" to cleanEmail,
        "adminGoogleUid" to user.uid,
        "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
        "adminAuthMethod" to "email_password",
        "adminPasscode" to upgradedHash,
        "adminPasscodeUpdatedAt" to upgradedAt,
        "updatedAt" to FieldValue.serverTimestamp(),
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

  override suspend fun updateChurchSecuritySettings(
    church: Church,
    quickPinEnabled: Boolean,
    newPinOrNull: String?,
    autoLockMinutes: Int,
    languageCode: String,
  ): Result<Church> {
    val cleanNewPin = newPinOrNull?.trim()?.takeIf { it.isNotEmpty() }
    if (cleanNewPin != null && !cleanNewPin.matches(Regex("^\\d{4,6}$"))) {
      return Result.failure(OmniAuthException(localizedPinFormatError(languageCode)))
    }
    if (quickPinEnabled && cleanNewPin == null && church.adminPasscode.isNullOrBlank()) {
      return Result.failure(OmniAuthException(localizedQuickPinSetupRequired(languageCode)))
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
      filterNonNullMap(
        "quickPinEnabled" to quickPinEnabled,
        "autoLockMinutes" to safeAutoLock,
        "adminPasscode" to nextHash,
        "adminPasscodeUpdatedAt" to nextUpdatedAt,
        "updatedAt" to FieldValue.serverTimestamp(),
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

  override suspend fun lockChurchAdminSession(churchId: String) {
    sessionLockManager.lockChurchAdminSession(churchId)
  }

  override suspend fun authorizeChurchAdminSession(churchId: String) {
    sessionLockManager.authorizeChurchAdminSession(churchId)
  }

  override suspend fun signOutAdmin(credentialManager: CredentialManager?) {
    if (credentialManager != null) {
      runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
    runCatching { firebaseAuth.signOut() }
    sessionLockManager.clearAllAdminLocks()
    preferencesManager.clearActiveSession()
  }

  private fun mergeAuthMethods(existing: String?, current: String): String {
    val cleanExisting = existing?.trim()?.lowercase().orEmpty()
    return when {
      cleanExisting.isEmpty() || cleanExisting == "legacy_pin" -> current
      cleanExisting == current -> current
      cleanExisting == "both" -> "both"
      (cleanExisting == "email_password" && current == "google") ||
        (cleanExisting == "google" && current == "email_password") -> "both"
      else -> current
    }
  }

  private fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() &&
      email.length <= 200 &&
      Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email)
  }

  private fun mapFirebaseAuthException(error: Throwable, lang: String): String {
    val code =
      when (error) {
        is FirebaseAuthException -> error.errorCode
        is FirebaseException -> error.message.orEmpty()
        else -> ""
      }
    val rawMsg = (error.message ?: "").lowercase(Locale.US)
    return when {
      error is FirebaseTooManyRequestsException || code == "ERROR_TOO_MANY_REQUESTS" ->
        when (lang) {
          "fr" -> "Trop de tentatives. Veuillez patienter quelques minutes avant de réessayer."
          "en" -> "Too many attempts. Please wait a few minutes before trying again."
          "es" -> "Demasiados intentos. Espere unos minutos antes de volver a intentarlo."
          else -> "Twòp esè ki echwe. Tanpri tann kèk minit anvan ou eseye ankò."
        }
      error is FirebaseAuthWeakPasswordException || code == "ERROR_WEAK_PASSWORD" ->
        localizedWeakPassword(lang)
      error is FirebaseAuthUserCollisionException || code == "ERROR_EMAIL_ALREADY_IN_USE" ->
        localizedEmailInUseWrongPassword(lang)
      error is FirebaseAuthInvalidUserException || code == "ERROR_USER_NOT_FOUND" || code == "ERROR_USER_DISABLED" ->
        when (lang) {
          "fr" -> "Aucun compte administrateur trouvé avec cet e-mail."
          "en" -> "No admin account found for this email address."
          "es" -> "No se encontró ninguna cuenta de administrador para este correo."
          else -> "Pa gen okenn kont administratè ki anrejistre ak imèl sa a."
        }
      error is FirebaseAuthInvalidCredentialsException ||
        code == "ERROR_WRONG_PASSWORD" ||
        code == "ERROR_INVALID_CREDENTIAL" ||
        code == "ERROR_INVALID_EMAIL" ->
        when (lang) {
          "fr" -> "E-mail ou mot de passe incorrect. Vérifiez vos identifiants."
          "en" -> "Incorrect email or password. Please check your credentials."
          "es" -> "Correo o contraseña incorrectos. Verifique sus credenciales."
          else -> "Imèl la oswa modpas la pa kòrèk. Tanpri verifye enfòmasyon ou yo."
        }
      code == "ERROR_OPERATION_NOT_ALLOWED" || rawMsg.contains("operation-not-allowed") || rawMsg.contains("operation_not_allowed") ->
        when (lang) {
          "fr" -> "La connexion par e-mail/mot de passe n'est pas encore activée dans Firebase Console."
          "en" -> "Email/Password sign-in is not yet enabled in Firebase Console."
          "es" -> "El acceso con correo y contraseña aún no está habilitado en Firebase Console."
          else -> "Koneksyon Imèl + Modpas poko aktive nan Firebase Console pwojè sa a."
        }
      else -> error.localizedMessage ?: localizedGenericAuthError(lang)
    }
  }

  private fun localizedInvalidEmail(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir une adresse e-mail valide."
      "en" -> "Please enter a valid email address."
      "es" -> "Por favor, ingrese una dirección de correo válida."
      else -> "Tanpri antre yon adrès imèl ki valid."
    }

  private fun localizedWeakPassword(lang: String): String =
    when (lang) {
      "fr" -> "Le mot de passe doit contenir au moins 6 caractères."
      "en" -> "Password must be at least 6 characters long."
      "es" -> "La contraseña debe tener al menos 6 caracteres."
      else -> "Modpas la dwe gen omwen 6 karaktè."
    }

  private fun localizedEmptyPassword(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir votre mot de passe."
      "en" -> "Please enter your password."
      "es" -> "Por favor, ingrese su contraseña."
      else -> "Tanpri antre modpas ou a."
    }

  private fun localizedEmailInUseWrongPassword(lang: String): String =
    when (lang) {
      "fr" -> "Cet e-mail possède déjà un compte OmniChurch avec un mot de passe différent."
      "en" -> "This email already owns an account with a different password."
      "es" -> "Este correo ya tiene una cuenta con una contraseña diferente."
      else -> "Imèl sa a deja gen yon kont ak yon lòt modpas. Antre modpas ki kòrèk la."
    }

  private fun localizedInvalidChurchName(lang: String): String =
    when (lang) {
      "fr" -> "Veuillez saisir le nom officiel de votre église."
      "en" -> "Please enter your church's official name."
      "es" -> "Por favor, ingrese el nombre oficial de su iglesia."
      else -> "Tanpri antre non ofisyèl legliz la."
    }

  private fun localizedNoSignedInUser(lang: String): String =
    when (lang) {
      "fr" -> "Session expirée. Veuillez vous reconnecter avec votre e-mail ou Google."
      "en" -> "Please sign in with your Email & Password or Google account first."
      "es" -> "Inicie sesión primero con su correo y contraseña o cuenta de Google."
      else -> "Tanpri konekte ak Imèl + Modpas ou oswa Kont Google ou anvan."
    }

  private fun localizedQuickPinUnavailable(lang: String): String =
    when (lang) {
      "fr" -> "Le déverrouillage rapide par PIN n'est disponible qu'après une connexion complète du propriétaire."
      "en" -> "Quick PIN resume requires an active signed-in church owner session."
      "es" -> "El PIN rápido requiere una sesión activa del propietario de la iglesia."
      else -> "Deblokaj ak PIN rapid disponib sèlman lè pwopriyetè legliz la deja konekte sou aparèy la."
    }

  private fun localizedWrongPin(lang: String): String =
    when (lang) {
      "fr" -> "Code PIN administrateur incorrect."
      "en" -> "Incorrect Admin PIN."
      "es" -> "PIN de administrador incorrecto."
      else -> "Kòd PIN Administratè a pa kòrèk."
    }

  private fun localizedPinFormatError(lang: String): String =
    when (lang) {
      "fr" -> "Le code PIN rapide doit contenir entre 4 et 6 chiffres."
      "en" -> "Quick PIN must be 4 to 6 numeric digits."
      "es" -> "El PIN rápido debe tener de 4 a 6 dígitos numéricos."
      else -> "Kòd PIN rapid la dwe gen ant 4 ak 6 chif sèlman."
    }

  private fun localizedQuickPinSetupRequired(lang: String): String =
    when (lang) {
      "fr" -> "Définissez un code PIN de 4 à 6 chiffres pour activer le déverrouillage rapide."
      "en" -> "Please set a 4 to 6 digit numeric PIN to enable Quick PIN Unlock."
      "es" -> "Configure un PIN numérico de 4 a 6 dígitos para activar el desbloqueo rápido."
      else -> "Tanpri kreye yon PIN 4 a 6 chif pou aktive Deblokaj Rapid ak PIN."
    }

  private fun localizedMismatchedOwnerError(
    languageCode: String,
    churchName: String,
    expectedEmail: String?,
    currentEmail: String?,
  ): String {
    val ownerHint = expectedEmail?.takeIf { it.isNotBlank() } ?: churchName
    val usedHint = currentEmail?.takeIf { it.isNotBlank() } ?: "account"
    return when (languageCode) {
      "fr" ->
        "Ce compte ($usedHint) n'est pas le propriétaire autorisé pour $churchName. Connectez-vous avec : $ownerHint"
      "en" ->
        "Account ($usedHint) is not the authorized owner for $churchName. Please sign in with: $ownerHint"
      "es" ->
        "La cuenta ($usedHint) no es el propietario autorizado de $churchName. Inicie sesión con: $ownerHint"
      else ->
        "Kont sa a ($usedHint) pa pwopriyetè otorize pou legliz $churchName. Tanpri konekte ak: $ownerHint"
    }
  }

  private fun localizedGenericAuthError(lang: String): String =
    when (lang) {
      "fr" -> "Échec d'authentification. Veuillez réessayer."
      "en" -> "Authentication failed. Please try again."
      "es" -> "Error de autenticación. Inténtelo de nuevo."
      else -> "Otantifikasyon an echwe. Tanpri eseye ankò."
    }

  private companion object {
    const val TAG = "AuthRepositoryImpl"
  }
}
