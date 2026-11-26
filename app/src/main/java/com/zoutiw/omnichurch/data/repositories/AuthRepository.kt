package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.UserProfile
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Clean, modular Authentication & Session Repository for OmniChurch Pro.
 * Orchestrates cohesive domain UseCases (`MemberAuthUseCase`, `AdminChurchAuthUseCase`,
 * `AdminChurchRegistrationUseCase`, `AdminChurchSecurityUseCase`, `UserProfileSyncUseCase`)
 * without scattered free extension delegates.
 */
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

  private val userProfileSyncUseCase = UserProfileSyncUseCase(db, firebaseAuth)
  private val memberAuthUseCase = MemberAuthUseCase(db, churchRepository)
  private val adminChurchAuthUseCase =
    AdminChurchAuthUseCase(
      db = db,
      churchRepository = churchRepository,
      firebaseAuth = firebaseAuth,
      sessionLockManager = sessionLockManager,
      userProfileSyncUseCase = userProfileSyncUseCase,
    )
  private val adminRegistrationUseCase =
    AdminChurchRegistrationUseCase(
      db = db,
      firebaseAuth = firebaseAuth,
      sessionLockManager = sessionLockManager,
    )
  private val adminSecurityUseCase =
    AdminChurchSecurityUseCase(
      db = db,
      firebaseAuth = firebaseAuth,
      sessionLockManager = sessionLockManager,
      registrationUseCase = adminRegistrationUseCase,
      adminChurchAuthUseCase = adminChurchAuthUseCase,
    )

  val currentFirebaseUserFlow: Flow<FirebaseUser?> = callbackFlow {
    trySend(firebaseAuth.currentUser)
    val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
    firebaseAuth.addAuthStateListener(listener)
    awaitClose { firebaseAuth.removeAuthStateListener(listener) }
  }

  val lockedChurchesFlow: Flow<Map<String, Boolean>> = sessionLockManager.lockedChurchesFlow

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

  // 1. Member Authentication & Google Account Linking
  suspend fun authenticateMemberSmartWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<MemberAutoLoginResolution> =
    memberAuthUseCase.authenticateMemberSmartWithPin(selectedChurchId, idOrPhone, enteredPin)

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
    memberAuthUseCase.findMemberByLinkedGoogleEmail(selectedChurchId, googleEmailInput)

  suspend fun linkGoogleAccountToMember(
    member: Member,
    googleEmail: String,
    googleUid: String,
    googlePhotoUrl: String?,
  ): Result<Member> =
    memberAuthUseCase.linkGoogleAccountToMember(member, googleEmail, googleUid, googlePhotoUrl)

  // 2. Admin Authentication & Smart Owned-Church Resolution
  suspend fun authenticateAdminForChurch(
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ): Result<Church> =
    adminChurchAuthUseCase.authenticateAdminForChurch(
      selectedChurchId,
      enteredPasscode,
      googleEmail,
      googleUid,
    )

  suspend fun resolveAdminOwnedChurches(user: FirebaseUser): List<Church> =
    adminChurchAuthUseCase.resolveAdminOwnedChurches(user)

  suspend fun resolveAutoRebindChurchIfUniquelyOwned(failedChurchId: String?): Church? =
    adminChurchAuthUseCase.resolveAutoRebindChurchIfUniquelyOwned(failedChurchId)

  suspend fun refreshCurrentUserToken(): Boolean =
    FirestorePayloadSanitizer.refreshAuthToken(firebaseAuth.currentUser)

  suspend fun signInAdminSmartWithEmailAndPassword(
    email: String,
    password: String,
  ): Result<AdminAutoLoginResolution> =
    adminChurchAuthUseCase.signInAdminSmartWithEmailAndPassword(email, password)

  suspend fun finalizeAdminChurchSelection(church: Church): Result<Church> =
    adminChurchAuthUseCase.finalizeAdminChurchSelection(church)

  suspend fun signInAdminSmartWithCurrentGoogleUser(): Result<AdminAutoLoginResolution> =
    adminChurchAuthUseCase.signInAdminSmartWithCurrentGoogleUser()

  suspend fun signInAdminWithEmailAndPassword(
    email: String,
    password: String,
    selectedChurchId: String? = null,
  ): Result<Church> =
    adminChurchAuthUseCase.signInAdminWithEmailAndPasswordLegacy(email, password, selectedChurchId)

  // 3. Admin Account Registration & Church Creation
  suspend fun registerOrReuseAdminWithEmailPassword(
    email: String,
    password: String,
    displayName: String?,
    languageCode: String,
  ): Result<FirebaseUser> =
    adminRegistrationUseCase.registerOrReuseAdminWithEmailPassword(
      email,
      password,
      displayName,
      languageCode,
    )

  suspend fun createChurchWithEmailPassword(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> =
    adminRegistrationUseCase.createChurchWithEmailPassword(
      churchName,
      churchCode,
      churchAddress,
      email,
      password,
      languageCode,
    )

  suspend fun createChurchWithSignedInGoogleUser(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    languageCode: String,
  ): Result<Church> =
    adminRegistrationUseCase.createChurchWithSignedInGoogleUser(
      churchName,
      churchCode,
      churchAddress,
      languageCode,
    )

  // 4. Church Ownership Verification, Quick-PIN & Security Settings
  suspend fun signInAdminWithEmailAndPassword(
    church: Church,
    email: String,
    password: String,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.signInAdminWithEmailAndPasswordForChurch(church, email, password, languageCode)

  suspend fun verifySignedInAdminGoogleAccount(
    church: Church,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.verifySignedInAdminGoogleAccount(church, languageCode)

  suspend fun signInAdminWithGoogleIdToken(
    church: Church,
    idToken: String,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.signInAdminWithGoogleIdToken(church, idToken, languageCode)

  suspend fun verifyQuickPinAndUpgradeIfNeeded(
    church: Church,
    enteredPin: String,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.verifyQuickPinAndUpgradeIfNeeded(church, enteredPin, languageCode)

  suspend fun verifyLegacyPinStep(
    church: Church,
    enteredPin: String,
    languageCode: String,
  ): Result<Unit> =
    adminSecurityUseCase.verifyLegacyPinStep(church, enteredPin, languageCode)

  suspend fun migrateLegacyPinChurchToCloudOwner(
    church: Church,
    verifiedOldPin: String,
    adminEmail: String,
    newPassword: String,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.migrateLegacyPinChurchToCloudOwner(
      church,
      verifiedOldPin,
      adminEmail,
      newPassword,
      languageCode,
    )

  suspend fun updateChurchSecuritySettings(
    church: Church,
    quickPinEnabled: Boolean,
    newPinOrNull: String?,
    autoLockMinutes: Int,
    languageCode: String,
  ): Result<Church> =
    adminSecurityUseCase.updateChurchSecuritySettings(
      church,
      quickPinEnabled,
      newPinOrNull,
      autoLockMinutes,
      languageCode,
    )

  // 5. Password Reset & Session Lock / Sign-Out
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

  // 6. User Profile Synchronization & Observation
  internal suspend fun syncAdminUserProfileForChurch(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
    church: Church,
  ) = userProfileSyncUseCase.syncAdminUserProfileForChurch(uid, email, displayName, photoUrl, church)

  suspend fun syncDeaconUserProfile(member: Member, church: Church? = null): Result<Unit> =
    userProfileSyncUseCase.syncDeaconUserProfile(member, church)

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    userProfileSyncUseCase.observeUserProfile(userId)

  suspend fun ensureUserProfileInitialized(
    uid: String,
    email: String,
    displayName: String,
    photoUrl: String?,
  ): Result<UserProfile> =
    userProfileSyncUseCase.ensureUserProfileInitialized(uid, email, displayName, photoUrl)

  suspend fun syncUserChurchContext(
    churchId: String,
    churchName: String,
    memberId: String? = null,
  ) = userProfileSyncUseCase.syncUserChurchContext(churchId, churchName, memberId)
}
