package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
  internal val db: FirebaseFirestore,
  internal val churchRepository: ChurchRepository,
  internal val firebaseAuth: FirebaseAuth = Firebase.auth,
  val sessionLockManager: SessionLockManager = SessionLockManager(),
  private val preferencesManager: OmniPreferencesManager? = null,
) {
  constructor(context: Context) : this(
    db = FirestoreProvider.get(context),
    churchRepository = ChurchRepository(context),
    sessionLockManager = SessionLockManager(context),
    preferencesManager = OmniPreferencesManager(context),
  )

  val currentFirebaseUserFlow: Flow<FirebaseUser?> = callbackFlow {
    trySend(firebaseAuth.currentUser)
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    firebaseAuth.addAuthStateListener(listener)
    awaitClose { firebaseAuth.removeAuthStateListener(listener) }
  }

  val lockedChurchesFlow: Flow<Map<String, Boolean>> =
    sessionLockManager.lockedChurchesFlow

  fun currentUser(): FirebaseUser? = firebaseAuth.currentUser

  fun currentAuthUid(): String = firebaseAuth.currentUser?.uid?.trim().orEmpty()

  fun resolveActorId(preferredActorId: String? = null): String {
    val cleanPreferred = (preferredActorId ?: "").trim()
    if (cleanPreferred.isNotEmpty()) return cleanPreferred
    return firebaseAuth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
  }

  sealed interface MemberAutoLoginResolution {
    data class SingleMemberResolved(val session: AuthenticatedMemberSession) : MemberAutoLoginResolution
    data class MultipleMemberChurches(val sessions: List<AuthenticatedMemberSession>) : MemberAutoLoginResolution
  }

  sealed class AdminAutoLoginResolution {
    data class SingleChurchResolved(val church: Church) : AdminAutoLoginResolution()
    data class MultipleOwnedChurches(val churches: List<Church>) : AdminAutoLoginResolution()
  }

  suspend fun authenticateMemberSmartWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<MemberAutoLoginResolution> =
    authenticateMemberSmartWithPinImpl(selectedChurchId, idOrPhone, enteredPin)

  suspend fun authenticateMemberWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthenticatedMemberSession> =
    authenticateMemberSmartWithPin(selectedChurchId, idOrPhone, enteredPin).map { resolution ->
      when (resolution) {
        is MemberAutoLoginResolution.SingleMemberResolved -> resolution.session
        is MemberAutoLoginResolution.MultipleMemberChurches -> resolution.sessions.first()
      }
    }

  suspend fun findMemberByLinkedGoogleEmail(
    selectedChurchId: String?,
    googleEmailInput: String,
  ): Result<AuthenticatedMemberSession?> =
    findMemberByLinkedGoogleEmailImpl(selectedChurchId, googleEmailInput)

  suspend fun linkGoogleAccountToMember(
    member: Member,
    googleEmail: String,
    googleUid: String,
    googlePhotoUrl: String?,
  ): Result<Member> =
    linkGoogleAccountToMemberImpl(member, googleEmail, googleUid, googlePhotoUrl)

  suspend fun authenticateAdminForChurch(
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ): Result<Church> =
    authenticateAdminForChurchImpl(selectedChurchId, enteredPasscode, googleEmail, googleUid)

  suspend fun registerOrReuseAdminWithEmailPassword(
    email: String,
    password: String,
    displayName: String?,
    languageCode: String,
  ): Result<FirebaseUser> =
    registerOrReuseAdminWithEmailPasswordImpl(email, password, displayName, languageCode)

  suspend fun createChurchWithEmailPassword(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> =
    createChurchWithEmailPasswordImpl(churchName, churchCode, churchAddress, email, password, languageCode)

  suspend fun createChurchWithSignedInGoogleUser(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    languageCode: String,
  ): Result<Church> =
    createChurchWithSignedInGoogleUserImpl(churchName, churchCode, churchAddress, languageCode)

  suspend fun resolveAdminOwnedChurches(user: FirebaseUser): List<Church> =
    resolveAdminOwnedChurchesImpl(user)

  suspend fun resolveAutoRebindChurchIfUniquelyOwned(failedChurchId: String?): Church? {
    val user = firebaseAuth.currentUser ?: return null
    FirestorePayloadSanitizer.refreshAuthToken(user)
    val owned = resolveAdminOwnedChurches(user)
    if (owned.size != 1) return null
    val uniqueChurch = owned.first()
    val cleanFailed = failedChurchId?.trim().orEmpty()
    return if (uniqueChurch.id.isNotBlank() && !uniqueChurch.id.equals(cleanFailed, ignoreCase = true)) {
      uniqueChurch
    } else {
      null
    }
  }

  suspend fun refreshCurrentUserToken(): Boolean =
    FirestorePayloadSanitizer.refreshAuthToken(firebaseAuth.currentUser)

  suspend fun signInAdminSmartWithEmailAndPassword(
    email: String,
    password: String,
  ): Result<AdminAutoLoginResolution> =
    signInAdminSmartWithEmailAndPasswordImpl(email, password)

  suspend fun finalizeAdminChurchSelection(church: Church): Result<Church> =
    finalizeAdminChurchSelectionImpl(church)

  suspend fun signInAdminSmartWithCurrentGoogleUser(): Result<AdminAutoLoginResolution> =
    signInAdminSmartWithCurrentGoogleUserImpl()

  suspend fun signInAdminWithEmailAndPassword(
    email: String,
    password: String,
    selectedChurchId: String? = null,
  ): Result<Church> =
    signInAdminWithEmailAndPasswordLegacyImpl(email, password, selectedChurchId)

  suspend fun signInAdminWithEmailAndPassword(
    church: Church,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> =
    signInAdminWithEmailAndPasswordChurchImpl(church, email, password, languageCode)

  suspend fun verifySignedInAdminGoogleAccount(
    church: Church,
    languageCode: String,
  ): Result<Church> =
    verifySignedInAdminGoogleAccountImpl(church, languageCode)

  suspend fun signInAdminWithGoogleIdToken(
    church: Church,
    idToken: String,
    languageCode: String,
  ): Result<Church> =
    signInAdminWithGoogleIdTokenImpl(church, idToken, languageCode)

  suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (cleanEmail.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_EMAIL))
    }
    if (!cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".")) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_INVALID_EMAIL))
    }
    return try {
      firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseAuthInvalidUserException) {
      Result.failure(OmniAuthException("Okenn kont imèl pa jwenn pou adrès sa a."))
    } catch (e: FirebaseNetworkException) {
      Result.failure(OmniAuthException("Pa gen koneksyon entènèt pou voye imèl la."))
    } catch (e: Exception) {
      Result.failure(
        OmniAuthException(
          e.localizedMessage ?: "Nou pa t kapab voye lyen pou chanje modpas la. Eseye ankò."
        )
      )
    }
  }

  suspend fun sendAdminPasswordResetEmail(email: String): Result<Unit> = sendPasswordResetEmail(email)

  suspend fun sendPasswordResetEmail(email: String, languageCode: String): Result<Unit> {
    val cleanEmail = email.trim().lowercase(Locale.US)
    if (!AuthErrorMessages.isValidEmail(cleanEmail)) {
      return Result.failure(OmniAuthException(AuthErrorMessages.localizedInvalidEmail(languageCode)))
    }
    return try {
      firebaseAuth.sendPasswordResetEmail(cleanEmail).await()
      Result.success(Unit)
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
  ): Result<Church> =
    verifyQuickPinAndUpgradeIfNeededImpl(church, enteredPin, languageCode)

  suspend fun verifyLegacyPinStep(
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
  ): Result<Church> =
    migrateLegacyPinChurchToCloudOwnerImpl(church, verifiedOldPin, adminEmail, newPassword, languageCode)

  suspend fun updateChurchSecuritySettings(
    church: Church,
    quickPinEnabled: Boolean,
    newPinOrNull: String?,
    autoLockMinutes: Int,
    languageCode: String,
  ): Result<Church> =
    updateChurchSecuritySettingsImpl(church, quickPinEnabled, newPinOrNull, autoLockMinutes, languageCode)

  suspend fun lockChurchAdminSession(churchId: String) {
    sessionLockManager.lockChurchAdminSession(churchId)
  }

  suspend fun authorizeChurchAdminSession(churchId: String) {
    sessionLockManager.authorizeChurchAdminSession(churchId)
  }

  suspend fun signOutAdmin(credentialManager: CredentialManager? = null) {
    if (credentialManager != null) {
      runCatching { credentialManager.clearCredentialState(ClearCredentialStateRequest()) }
    }
    runCatching { firebaseAuth.signOut() }
    sessionLockManager.clearAllAdminLocks()
    preferencesManager?.clearActiveSession()
  }

  internal suspend fun syncAdminUserProfileForChurch(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
    church: Church,
  ) {
    if (uid.isBlank() || church.id.isBlank()) return
    val userRef = db.collection("users").document(uid)
    val targetRole =
      if (email.trim().equals(OmniAuthEvaluator.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
        "super_admin"
      } else {
        "admin"
      }
    runCatching {
      val existingSnap = userRef.get().await()
      if (existingSnap.exists()) {
        // Respect custom or explicitly cleared ("") adminGooglePhoto and existing user photoUrl.
        val resolvedPhotoForSync: String? =
          when {
            church.adminGooglePhoto != null -> church.adminGooglePhoto
            existingSnap.contains("photoUrl") -> null
            else -> photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          }
        val updateMap =
          hashMapOf<String, Any>(
            "churchId" to church.id.trim(),
            "churchName" to church.name.trim().ifBlank { "OmniChurch Pro" },
            "role" to targetRole,
            "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
            "updatedAt" to FieldValue.serverTimestamp(),
          ).apply {
            if (resolvedPhotoForSync != null) {
              put("photoUrl", resolvedPhotoForSync)
            }
          }
        userRef.update(updateMap).await()
      } else {
        val initialPhoto =
          if (church.adminGooglePhoto != null) {
            church.adminGooglePhoto
          } else {
            photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          }
        userRef
          .set(
            filterNonNullMap(
              "userId" to uid,
              "email" to email.trim().ifBlank { "admin@omnichurch.org" },
              "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
              "photoUrl" to initialPhoto,
              "churchId" to church.id.trim(),
              "churchName" to church.name.trim().ifBlank { "OmniChurch Pro" },
              "role" to targetRole,
              "language" to "ht",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    }.onFailure { err ->
      Log.w(TAG, "Admin profile sync deferred: ${err.message}")
    }
  }

  /**
   * Automatically ensures/syncs `users/{uid}` with `role: "deacon"`, `churchId`, and `memberId`
   * when an active Deacon (Dyak / Dyakonès) signs in with Firebase Auth.
   * NEVER downgrades an existing `admin` or `super_admin` user profile.
   */
  suspend fun syncDeaconUserProfile(member: Member, church: Church? = null): Result<Unit> {
    val currentUser = firebaseAuth.currentUser ?: return Result.success(Unit)
    val uid = currentUser.uid.trim()
    if (uid.isEmpty()) return Result.success(Unit)
    if (!OmniRoleEvaluator.isActiveDeacon(member)) return Result.success(Unit)

    val cleanChurchId = (church?.id?.trim()?.takeIf { it.isNotEmpty() } ?: member.churchId.trim())
    if (cleanChurchId.isEmpty() || member.id.isBlank()) return Result.success(Unit)
    val cleanChurchName =
      (church?.name?.trim()?.takeIf { it.isNotEmpty() } ?: member.churchName.trim()).ifBlank { "OmniChurch Pro" }
    val userRef = db.collection("users").document(uid)

    return try {
      val existingSnap = userRef.get().await()
      val emailLower = currentUser.email?.trim()?.lowercase(Locale.US).orEmpty()
      val isSuperAdminEmail = emailLower == OmniAuthEvaluator.SUPER_ADMIN_EMAIL.lowercase(Locale.US)

      if (existingSnap.exists()) {
        val existingRole = existingSnap.getString("role")?.trim()?.lowercase(Locale.US).orEmpty()
        // NEVER downgrade an existing admin or super_admin profile.
        if (existingRole == "admin" || existingRole == "super_admin" || isSuperAdminEmail) {
          return Result.success(Unit)
        }
        val alreadySynced =
          existingRole == "deacon" &&
            existingSnap.getString("churchId")?.trim() == cleanChurchId &&
            existingSnap.getString("memberId")?.trim() == member.id.trim()
        if (alreadySynced) return Result.success(Unit)

        userRef
          .update(
            filterNonNullMap(
              "role" to "deacon",
              "churchId" to cleanChurchId,
              "churchName" to cleanChurchName,
              "memberId" to member.id.trim(),
              "displayName" to member.fullName.trim().ifBlank { currentUser.displayName ?: "Dyak" },
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val initialRole = if (isSuperAdminEmail) "super_admin" else "deacon"
        val resolvedPhoto =
          member.effectivePhotoUrl?.takeIf { it.isNotBlank() }
            ?: if (member.avatarUrl == null && member.photoUrl == null && member.googlePhoto == null) {
              currentUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
            } else {
              null
            }
        userRef
          .set(
            filterNonNullMap(
              "userId" to uid,
              "email" to (emailLower.ifBlank { member.email?.trim().orEmpty() }.ifBlank { "deacon@omnichurch.org" }),
              "displayName" to member.fullName.trim().ifBlank { currentUser.displayName ?: "Dyak" },
              "photoUrl" to resolvedPhoto,
              "churchId" to cleanChurchId,
              "churchName" to cleanChurchName,
              "memberId" to member.id.trim(),
              "role" to initialRole,
              "language" to "ht",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      Log.w(TAG, "Deacon profile sync deferred: ${e.message}")
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    if (userId.isBlank()) {
      emit(null)
      return@flow
    }
    val path = "users/$userId"
    emitAll(
      db
        .collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) snapshot.toUserProfile() else null
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.GET, path)
          emit(null)
        }
    )
  }

  suspend fun ensureUserProfileInitialized(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
  ): Result<UserProfile> {
    if (uid.isBlank()) {
      return Result.failure(IllegalArgumentException("UID is blank"))
    }
    val docRef = db.collection("users").document(uid)
    return try {
      val existingSnap = docRef.get().await()
      if (existingSnap.exists()) {
        val existingProfile = existingSnap.toUserProfile()
        if (existingProfile != null) {
          return Result.success(existingProfile)
        }
      }

      val memberQuery =
        db.collection("members").whereEqualTo("userId", uid).limit(1).get().await()
      val matchedMember = memberQuery.documents.firstOrNull()?.toMember()

      val initialRole =
        if (email.trim().equals(OmniAuthEvaluator.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
          "super_admin"
        } else {
          "member"
        }

      val payload =
        filterNonNullMap(
          "userId" to uid,
          "email" to email.trim().ifBlank { "member@omnichurch.org" },
          "displayName" to displayName.trim().ifBlank { matchedMember?.fullName ?: "Manm OmniChurch" },
          "photoUrl" to (photoUrl ?: matchedMember?.effectivePhotoUrl),
          "churchId" to matchedMember?.churchId,
          "churchName" to matchedMember?.churchName,
          "memberId" to matchedMember?.id,
          "role" to initialRole,
          "language" to "ht",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      val fresh =
        docRef.get().await().toUserProfile()
          ?: UserProfile(
            id = uid,
            userId = uid,
            email = email,
            displayName = displayName,
            role = initialRole,
          )
      Result.success(fresh)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun syncUserChurchContext(
    churchId: String,
    churchName: String,
    memberId: String? = null,
  ) {
    val uid = firebaseAuth.currentUser?.uid?.trim().orEmpty()
    if (uid.isBlank() || churchId.isBlank()) return
    val userRef = db.collection("users").document(uid)
    runCatching {
      val snap = userRef.get().await()
      if (snap.exists()) {
        userRef
          .update(
            filterNonNullMap(
              "churchId" to churchId.trim(),
              "churchName" to churchName.trim().ifBlank { "OmniChurch Pro" },
              "memberId" to memberId?.trim()?.takeIf { it.isNotBlank() },
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
    }
  }

  companion object {
    private const val TAG = "AuthRepository"
  }
}
