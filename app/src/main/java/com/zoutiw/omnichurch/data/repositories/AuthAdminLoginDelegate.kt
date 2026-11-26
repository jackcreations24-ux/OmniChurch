package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

internal suspend fun AuthRepository.authenticateAdminForChurchImpl(
  selectedChurchId: String,
  enteredPasscode: String?,
  googleEmail: String?,
  googleUid: String?,
): Result<Church> {
  val cleanChurchId = selectedChurchId.trim()
  if (cleanChurchId.isEmpty()) {
    return Result.failure(
      OmniAuthException("Tanpri chwazi legliz ou vle jere kòm Administratè a.")
    )
  }

  return try {
    val churches = churchRepository.fetchAllChurches()
    val church =
      OmniAuthEvaluator.resolveChurchForMemberLogin(churches, cleanChurchId, null)
        ?: return Result.failure(
          OmniAuthException("Legliz sa a pa egziste nan baz done Firestore la.")
        )

    val isPasscodeValid = enteredPasscode != null && OmniAuthEvaluator.verifyAdminPasscode(church, enteredPasscode)
    val isGoogleValid = googleEmail != null && OmniAuthEvaluator.isGoogleEmailAuthorizedForAdmin(church, googleEmail, googleUid)

    if (isPasscodeValid || isGoogleValid || CryptoSecurity.isSuperDeveloperAccount(googleEmail)) {
      sessionLockManager.authorizeChurchAdminSession(church.id)
      Result.success(church)
    } else {
      Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_WRONG_ADMIN_PASSCODE))
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.GET, "churches/$cleanChurchId")
    Result.failure(e)
  }
}

internal suspend fun AuthRepository.resolveAdminOwnedChurchesImpl(user: FirebaseUser): List<Church> {
  val uid = user.uid.trim()
  if (uid.isEmpty()) return emptyList()
  val results = linkedMapOf<String, Church>()

  runCatching {
    db.collection("churches").whereEqualTo("ownerId", uid).get().awaitSafe()
      .documents.mapNotNull { it.toChurch() }
      .forEach { results[it.id] = it }
  }
  runCatching {
    db.collection("churches").whereEqualTo("adminGoogleUid", uid).get().awaitSafe()
      .documents.mapNotNull { it.toChurch() }
      .forEach { results[it.id] = it }
  }
  val lowerEmail = user.email?.trim()?.lowercase(Locale.US).orEmpty()
  if (user.isEmailVerified && lowerEmail.isNotEmpty()) {
    runCatching {
      db.collection("churches").whereEqualTo("adminGoogleEmail", lowerEmail).get().awaitSafe()
        .documents.mapNotNull { it.toChurch() }
        .forEach { results[it.id] = it }
    }
  }
  return results.values.toList()
}

internal suspend fun AuthRepository.signInAdminSmartWithEmailAndPasswordImpl(
  email: String,
  password: String,
): Result<AuthRepository.AdminAutoLoginResolution> {
  val cleanEmail = email.trim().lowercase(Locale.US)
  if (cleanEmail.isEmpty()) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_EMAIL))
  }
  if (!cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_INVALID_EMAIL))
  }
  if (password.isBlank()) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_PASSWORD))
  }

  return try {
    val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
    val firebaseUser =
      authResult.user ?: return Result.failure(OmniAuthException("Otantifikasyon echwe."))

    FirestorePayloadSanitizer.refreshAuthToken(firebaseUser)

    val resolvedEmail = (firebaseUser.email ?: cleanEmail).trim().lowercase(Locale.US)
    val isSuperDev = CryptoSecurity.isSuperDeveloperAccount(resolvedEmail)

    val ownedChurches = linkedMapOf<String, Church>()
    resolveAdminOwnedChurchesImpl(firebaseUser).forEach { church ->
      ownedChurches[church.id] = church
    }

    val existingUserProfile =
      runCatching {
        db.collection("users")
          .document(firebaseUser.uid)
          .get()
          .await()
          .toUserProfile()
      }.getOrNull()

    if (isSuperDev) {
      churchRepository.fetchAllChurches().forEach { church ->
        ownedChurches[church.id] = church
      }
    } else {
      val profileChurchId = existingUserProfile?.churchId?.trim().orEmpty()
      val isProfileAdmin =
        existingUserProfile?.role?.trim()?.lowercase(Locale.US) in setOf("admin", "super_admin")
      if (profileChurchId.isNotEmpty() && isProfileAdmin && !ownedChurches.containsKey(profileChurchId)) {
        val profileChurch =
          runCatching {
            db.collection("churches").document(profileChurchId).get().awaitSafe().toChurch()
          }.getOrNull()
        if (profileChurch != null &&
          OmniAuthEvaluator.isChurchEmailAdminAuthorized(profileChurch, resolvedEmail, firebaseUser.uid)
        ) {
          ownedChurches[profileChurch.id] = profileChurch
        }
      }
      if (ownedChurches.isEmpty()) {
        val byContactEmail =
          runCatching {
            db.collection("churches").whereEqualTo("adminGoogleEmail", resolvedEmail).get().awaitSafe()
              .documents.mapNotNull { it.toChurch() }
          }.getOrDefault(emptyList())
        byContactEmail
          .filter { OmniAuthEvaluator.isChurchEmailAdminAuthorized(it, resolvedEmail, firebaseUser.uid) }
          .forEach { ownedChurches[it.id] = it }

        if (ownedChurches.isEmpty()) {
          val byLegacyEmail =
            runCatching {
              db.collection("churches").whereEqualTo("email", resolvedEmail).get().awaitSafe()
                .documents.mapNotNull { it.toChurch() }
            }.getOrDefault(emptyList())
          byLegacyEmail
            .filter { OmniAuthEvaluator.isChurchEmailAdminAuthorized(it, resolvedEmail, firebaseUser.uid) }
            .forEach { ownedChurches[it.id] = it }
        }
      }
    }

    val authorizedList =
      ownedChurches.values
        .filter { it.id.isNotBlank() }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }

    when {
      authorizedList.isEmpty() -> {
        runCatching { firebaseAuth.signOut() }
        Result.failure(
          OmniAuthException(
            "Okenn legliz pa lye oswa otorize pou imèl \"$resolvedEmail\". Verifye imèl kont administratè legliz ou a."
          )
        )
      }
      authorizedList.size == 1 -> {
        val targetChurch = authorizedList.first()
        finalizeAdminChurchSelectionImpl(targetChurch)
          .map { AuthRepository.AdminAutoLoginResolution.SingleChurchResolved(it) }
      }
      else -> {
        Result.success(AuthRepository.AdminAutoLoginResolution.MultipleOwnedChurches(authorizedList))
      }
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: FirebaseAuthInvalidUserException) {
    Result.failure(OmniAuthException("Kont imèl sa a pa egziste oswa li dezaktive. Verifye imèl la."))
  } catch (e: FirebaseAuthInvalidCredentialsException) {
    Result.failure(OmniAuthException("Imèl oswa modpas la pa kòrèk. Verifye enfòmasyon yo epi eseye ankò."))
  } catch (e: FirebaseTooManyRequestsException) {
    Result.failure(OmniAuthException("Twòp tantativ koneksyon echwe. Tanpri tann kèk minit anvan ou re-eseye."))
  } catch (e: FirebaseNetworkException) {
    Result.failure(OmniAuthException("Pa gen koneksyon entènèt. Verifye rezo ou epi eseye ankò."))
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.GET, "churches")
    Result.failure(OmniAuthException(e.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."))
  }
}

internal suspend fun AuthRepository.finalizeAdminChurchSelectionImpl(church: Church): Result<Church> {
  val firebaseUser =
    firebaseAuth.currentUser
      ?: return Result.failure(OmniAuthException("Sesyon Firebase ou a ekspire. Tanpri rekonekte."))
  val resolvedEmail = (firebaseUser.email ?: "").trim().lowercase(Locale.US)
  val isAuthorized =
    CryptoSecurity.isSuperDeveloperAccount(resolvedEmail) ||
      OmniAuthEvaluator.isChurchEmailAdminAuthorized(church, resolvedEmail, firebaseUser.uid)
  if (!isAuthorized) {
    return Result.failure(
      OmniAuthException("Kont ou a pa gen otorizasyon administratè pou legliz ${church.name}.")
    )
  }
  syncAdminUserProfileForChurch(
    uid = firebaseUser.uid,
    email = resolvedEmail,
    displayName =
      firebaseUser.displayName?.takeIf { it.isNotBlank() }
        ?: church.pastor.takeIf { it.isNotBlank() }
        ?: church.name.ifBlank { "Admin OmniChurch" },
    photoUrl = firebaseUser.photoUrl?.toString(),
    church = church,
  )
  sessionLockManager.authorizeChurchAdminSession(church.id)
  return Result.success(church)
}

internal suspend fun AuthRepository.signInAdminSmartWithCurrentGoogleUserImpl():
  Result<AuthRepository.AdminAutoLoginResolution> {
  val firebaseUser =
    firebaseAuth.currentUser
      ?: return Result.failure(
        OmniAuthException("Koneksyon Google la pa konplete ak Firebase. Tanpri eseye ankò.")
      )
  return try {
    FirestorePayloadSanitizer.refreshAuthToken(firebaseUser)
    val resolvedEmail = (firebaseUser.email ?: "").trim().lowercase(Locale.US)
    val isSuperDev = CryptoSecurity.isSuperDeveloperAccount(resolvedEmail)

    val ownedChurches = linkedMapOf<String, Church>()
    resolveAdminOwnedChurchesImpl(firebaseUser).forEach { church ->
      ownedChurches[church.id] = church
    }

    if (isSuperDev) {
      churchRepository.fetchAllChurches().forEach { church ->
        ownedChurches[church.id] = church
      }
    } else {
      val existingUserProfile =
        runCatching {
          db.collection("users").document(firebaseUser.uid).get().await().toUserProfile()
        }.getOrNull()
      val profileChurchId = existingUserProfile?.churchId?.trim().orEmpty()
      val isProfileAdmin =
        existingUserProfile?.role?.trim()?.lowercase(Locale.US) in setOf("admin", "super_admin")
      if (profileChurchId.isNotEmpty() && isProfileAdmin && !ownedChurches.containsKey(profileChurchId)) {
        val profileChurch =
          runCatching {
            db.collection("churches").document(profileChurchId).get().awaitSafe().toChurch()
          }.getOrNull()
        if (profileChurch != null &&
          OmniAuthEvaluator.isChurchEmailAdminAuthorized(profileChurch, resolvedEmail, firebaseUser.uid)
        ) {
          ownedChurches[profileChurch.id] = profileChurch
        }
      }
      if (ownedChurches.isEmpty() && resolvedEmail.isNotEmpty()) {
        val byContactEmail =
          runCatching {
            db.collection("churches").whereEqualTo("adminGoogleEmail", resolvedEmail).get().awaitSafe()
              .documents.mapNotNull { it.toChurch() }
          }.getOrDefault(emptyList())
        byContactEmail
          .filter { OmniAuthEvaluator.isChurchEmailAdminAuthorized(it, resolvedEmail, firebaseUser.uid) }
          .forEach { ownedChurches[it.id] = it }
      }
    }

    val authorizedList =
      ownedChurches.values
        .filter { it.id.isNotBlank() }
        .sortedBy { it.name.lowercase(Locale.getDefault()) }

    when {
      authorizedList.isEmpty() -> {
        runCatching { firebaseAuth.signOut() }
        Result.failure(
          OmniAuthException(
            "Okenn legliz pa lye ak kont Google \"$resolvedEmail\". Verifye kont Google administratè legliz ou a."
          )
        )
      }
      authorizedList.size == 1 -> {
        finalizeAdminChurchSelectionImpl(authorizedList.first())
          .map { AuthRepository.AdminAutoLoginResolution.SingleChurchResolved(it) }
      }
      else -> {
        Result.success(AuthRepository.AdminAutoLoginResolution.MultipleOwnedChurches(authorizedList))
      }
    }
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.GET, "churches")
    Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan verifikasyon kont Google Admin nan."))
  }
}

internal suspend fun AuthRepository.signInAdminWithEmailAndPasswordLegacyImpl(
  email: String,
  password: String,
  selectedChurchId: String? = null,
): Result<Church> {
  val cleanEmail = email.trim().lowercase(Locale.US)
  if (cleanEmail.isEmpty()) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_EMAIL))
  }
  if (!cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_INVALID_EMAIL))
  }
  if (password.isBlank()) {
    return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_PASSWORD))
  }

  return try {
    val authResult = firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).await()
    val firebaseUser =
      authResult.user ?: return Result.failure(OmniAuthException("Otantifikasyon echwe."))

    FirestorePayloadSanitizer.refreshAuthToken(firebaseUser)

    val existingUserProfile =
      runCatching {
        db.collection("users").document(firebaseUser.uid).get().await().toUserProfile()
      }.getOrNull()

    val directOwnedChurches = resolveAdminOwnedChurchesImpl(firebaseUser)
    val allChurches =
      if (directOwnedChurches.isNotEmpty() && selectedChurchId.isNullOrBlank()) {
        directOwnedChurches
      } else {
        (directOwnedChurches + churchRepository.fetchAllChurches()).distinctBy { it.id }
      }

    val resolutionResult =
      OmniAuthEvaluator.resolveChurchForEmailPasswordLogin(
        churches = allChurches,
        email = firebaseUser.email ?: cleanEmail,
        uid = firebaseUser.uid,
        selectedChurchId = selectedChurchId,
        profileChurchId = existingUserProfile?.churchId,
        profileRole = existingUserProfile?.role,
      )

    var matchedChurch = resolutionResult.getOrNull()
    if (matchedChurch == null && directOwnedChurches.size == 1) {
      matchedChurch = directOwnedChurches.first()
    }
    if (matchedChurch == null) {
      runCatching { firebaseAuth.signOut() }
      return Result.failure(
        resolutionResult.exceptionOrNull()
          ?: OmniAuthException("Okenn legliz pa jwenn pou imèl sa a.")
      )
    }

    syncAdminUserProfileForChurch(
      uid = firebaseUser.uid,
      email = firebaseUser.email ?: cleanEmail,
      displayName =
        firebaseUser.displayName?.takeIf { it.isNotBlank() }
          ?: matchedChurch.pastor.takeIf { it.isNotBlank() }
          ?: matchedChurch.name.ifBlank { "Admin OmniChurch" },
      photoUrl = firebaseUser.photoUrl?.toString(),
      church = matchedChurch,
    )

    Result.success(matchedChurch)
  } catch (e: CancellationException) {
    throw e
  } catch (e: FirebaseAuthInvalidUserException) {
    Result.failure(OmniAuthException("Kont imèl sa a pa egziste oswa li dezaktive. Verifye imèl la."))
  } catch (e: FirebaseAuthInvalidCredentialsException) {
    Result.failure(OmniAuthException("Imèl oswa modpas la pa kòrèk. Verifye enfòmasyon yo epi eseye ankò."))
  } catch (e: FirebaseTooManyRequestsException) {
    Result.failure(OmniAuthException("Twòp tantativ koneksyon echwe. Tanpri tann kèk minit anvan ou re-eseye."))
  } catch (e: FirebaseNetworkException) {
    Result.failure(OmniAuthException("Pa gen koneksyon entènèt. Verifye rezo ou epi eseye ankò."))
  } catch (e: Exception) {
    handleFirestoreError(e, OperationType.GET, "churches")
    Result.failure(OmniAuthException(e.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."))
  }
}
