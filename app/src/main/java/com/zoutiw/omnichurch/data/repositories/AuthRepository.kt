package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.core.utilities.CryptoSecurity
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.OwnershipEvaluationStatus
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
  private val db: FirebaseFirestore,
  private val churchRepository: ChurchRepository,
  private val firebaseAuth: FirebaseAuth = Firebase.auth,
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

  private suspend fun queryCandidateMembersForAutoAuth(
    resolvedChurch: Church?,
    cleanInput: String,
  ): List<Member> {
    if (resolvedChurch != null) {
      val churchId = resolvedChurch.id.trim()
      val churchCode = (resolvedChurch.code ?: "").trim()
      val membersSnap =
        try {
          if (churchCode.isNotEmpty() && !churchCode.equals(churchId, ignoreCase = true)) {
            try {
              db.collection("members").whereIn("churchId", listOf(churchId, churchCode)).get().awaitSafe()
            } catch (_: Exception) {
              db.collection("members").whereEqualTo("churchId", churchId).get().awaitSafe()
            }
          } else {
            db.collection("members").whereEqualTo("churchId", churchId).get().awaitSafe()
          }
        } catch (e: CancellationException) {
          currentCoroutineContext().ensureActive()
          delay(100)
          db.collection("members").whereEqualTo("churchId", churchId).get().await()
        } catch (_: Exception) {
          db.collection("members").get().awaitSafe()
        }
      return membersSnap.documents.mapNotNull { it.toMember() }
    }

    // Church-agnostic member lookup: run targeted indexed queries first, then fallback safely
    val collected = linkedMapOf<String, Member>()
    suspend fun collectQuery(field: String, value: String) {
      if (value.isBlank()) return
      runCatching {
        val snap = db.collection("members").whereEqualTo(field, value).limit(20).get().awaitSafe()
        snap.documents.mapNotNull { it.toMember() }.forEach { collected[it.id] = it }
      }
    }

    val cleanUpper = cleanInput.uppercase(Locale.US)
    val cleanLower = cleanInput.lowercase(Locale.US)
    val digitsOnly = cleanInput.replace(Regex("[^0-9]"), "")

    collectQuery("memberId", cleanInput)
    if (cleanUpper != cleanInput) collectQuery("memberId", cleanUpper)
    collectQuery("memberCode", cleanInput)
    if (cleanUpper != cleanInput) collectQuery("memberCode", cleanUpper)
    if (digitsOnly.length >= 7) {
      collectQuery("phone", cleanInput)
      if (digitsOnly != cleanInput) collectQuery("phone", digitsOnly)
    }
    if (cleanInput.contains("@")) {
      collectQuery("email", cleanLower)
      collectQuery("googleEmail", cleanLower)
    }
    if (!cleanInput.contains("/") && cleanInput.length in 2..128) {
      runCatching {
        val doc = db.collection("members").document(cleanInput).get().awaitSafe()
        doc.toMember()?.let { collected[it.id] = it }
      }
    }

    if (collected.isNotEmpty()) {
      return collected.values.toList()
    }

    val fullSnap =
      try {
        db.collection("members").get().awaitSafe()
      } catch (e: CancellationException) {
        currentCoroutineContext().ensureActive()
        delay(100)
        db.collection("members").get().await()
      }
    return fullSnap.documents.mapNotNull { it.toMember() }
  }

  private suspend fun enrichMissingChurches(
    sessions: List<AuthenticatedMemberSession>,
    knownChurches: List<Church>,
  ): List<AuthenticatedMemberSession> {
    if (sessions.isEmpty()) return sessions
    return sessions.map { session ->
      val matchedKnown = knownChurches.firstOrNull { OmniAuthEvaluator.doesMemberBelongToChurch(session.member, it) }
      if (matchedKnown != null) {
        session.copy(church = matchedKnown)
      } else {
        val memberChurchId = session.member.churchId.trim()
        val fetchedChurch =
          if (memberChurchId.isNotEmpty()) {
            runCatching {
              db.collection("churches").document(memberChurchId).get().awaitSafe().toChurch()
                ?: db.collection("churches").whereEqualTo("code", memberChurchId).limit(1).get().awaitSafe()
                  .documents.firstOrNull()?.toChurch()
            }.getOrNull()
          } else null
        if (fetchedChurch != null) session.copy(church = fetchedChurch) else session
      }
    }
  }

  suspend fun authenticateMemberSmartWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<MemberAutoLoginResolution> {
    val cleanInput = idOrPhone.trim()
    val cleanPin = enteredPin.trim()

    if (cleanInput.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_ID))
    }
    if (cleanPin.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_PIN))
    }

    return try {
      val churches = runCatching { churchRepository.fetchAllChurches() }.getOrElse { emptyList() }
      val resolvedExplicitOrPrefixedChurch =
        OmniAuthEvaluator.resolveChurchForMemberLogin(
          churches = churches,
          selectedChurchId = selectedChurchId,
          idOrPhoneInput = cleanInput,
        )

      if (!selectedChurchId.isNullOrBlank() && resolvedExplicitOrPrefixedChurch == null) {
        return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_NO_CHURCH_SELECTED))
      }

      var candidateMembers =
        queryCandidateMembersForAutoAuth(
          resolvedChurch = resolvedExplicitOrPrefixedChurch,
          cleanInput = cleanInput,
        )

      var evalResult =
        OmniAuthEvaluator.authenticateMemberAutoResolving(
          churches = churches,
          allMembers = candidateMembers,
          idOrPhone = cleanInput,
          enteredPin = cleanPin,
          selectedChurchId = selectedChurchId,
        )

      // If targeted query returned an unrelated candidate or none verified, try full normalized lookup once
      if (evalResult.isFailure && resolvedExplicitOrPrefixedChurch == null) {
        val fullMembersSnap =
          runCatching { db.collection("members").get().awaitSafe() }.getOrNull()
        val allMembers = fullMembersSnap?.documents?.mapNotNull { it.toMember() }.orEmpty()
        if (allMembers.isNotEmpty() && allMembers.size > candidateMembers.size) {
          candidateMembers = allMembers
          evalResult =
            OmniAuthEvaluator.authenticateMemberAutoResolving(
              churches = churches,
              allMembers = candidateMembers,
              idOrPhone = cleanInput,
              enteredPin = cleanPin,
              selectedChurchId = null,
            )
        }
      }

      evalResult.map { rawSessions ->
        val enriched = enrichMissingChurches(rawSessions, churches)
        if (enriched.size == 1) {
          MemberAutoLoginResolution.SingleMemberResolved(enriched.first())
        } else {
          MemberAutoLoginResolution.MultipleMemberChurches(enriched)
        }
      }
    } catch (e: CancellationException) {
      currentCoroutineContext().ensureActive()
      Result.failure(OmniAuthException("Koneksyon an te entèwonp, tanpri eseye ankò."))
    } catch (e: OmniAuthException) {
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "members")
      Result.failure(
        OmniAuthException(e.localizedMessage ?: "Erè koneksyon ak baz done Firestore la.")
      )
    }
  }

  suspend fun authenticateMemberWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthenticatedMemberSession> {
    return authenticateMemberSmartWithPin(
      selectedChurchId = selectedChurchId,
      idOrPhone = idOrPhone,
      enteredPin = enteredPin,
    ).map { resolution ->
      when (resolution) {
        is MemberAutoLoginResolution.SingleMemberResolved -> resolution.session
        is MemberAutoLoginResolution.MultipleMemberChurches -> resolution.sessions.first()
      }
    }
  }

  suspend fun findMemberByLinkedGoogleEmail(
    selectedChurchId: String?,
    googleEmailInput: String,
  ): Result<AuthenticatedMemberSession?> {
    val cleanEmail = googleEmailInput.trim().lowercase()
    if (cleanEmail.isEmpty()) return Result.success(null)

    return try {
      val churches = churchRepository.fetchAllChurches()
      val selectedChurch =
        if (!selectedChurchId.isNullOrBlank()) {
          OmniAuthEvaluator.resolveChurchForMemberLogin(churches, selectedChurchId, null)
        } else {
          null
        }

      val membersSnap = db.collection("members").get().await()
      val allMembers = membersSnap.documents.mapNotNull { it.toMember() }

      val candidateMembers =
        if (selectedChurch != null) {
          allMembers.filter { OmniAuthEvaluator.doesMemberBelongToChurch(it, selectedChurch) }
        } else {
          allMembers
        }

      val matchedMember =
        candidateMembers.firstOrNull { member ->
          val linkedGoogle = (member.googleEmail ?: "").trim().lowercase()
          val memberEmail = (member.email ?: "").trim().lowercase()
          (linkedGoogle.isNotEmpty() && linkedGoogle == cleanEmail) ||
            (memberEmail.isNotEmpty() && memberEmail == cleanEmail)
        } ?: return Result.success(null)

      val memberChurch =
        selectedChurch
          ?: churches.firstOrNull { OmniAuthEvaluator.doesMemberBelongToChurch(matchedMember, it) }
          ?: Church(
            id = matchedMember.churchId,
            code = matchedMember.churchId,
            name = matchedMember.churchName.ifBlank { "OmniChurch Pro" },
          )

      Result.success(AuthenticatedMemberSession(church = memberChurch, member = matchedMember))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "members")
      Result.failure(e)
    }
  }

  suspend fun linkGoogleAccountToMember(
    member: Member,
    googleEmail: String,
    googleUid: String,
    googlePhotoUrl: String?,
  ): Result<Member> {
    val cleanGoogleEmail = googleEmail.trim()
    val cleanGoogleUid = googleUid.trim()
    val docRef = db.collection("members").document(member.id)

    val preservedEmail =
      member.email?.trim()?.takeIf { it.isNotEmpty() } ?: cleanGoogleEmail

    val updates =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        hashMapOf(
          "googleEmail" to cleanGoogleEmail.lowercase(Locale.US),
          "googleUid" to cleanGoogleUid,
          "googlePhoto" to googlePhotoUrl?.trim()?.takeIf { it.isNotEmpty() },
          "email" to preservedEmail,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )

    return try {
      docRef.update(updates).await()
      val updatedMember =
        docRef.get().await().toMember()
          ?: member.copy(
            googleEmail = cleanGoogleEmail,
            googleUid = cleanGoogleUid,
            googlePhoto = googlePhotoUrl,
            email = preservedEmail,
          )
      Result.success(updatedMember)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun authenticateAdminForChurch(
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

  suspend fun registerOrReuseAdminWithEmailPassword(
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
            ?: return Result.failure(OmniAuthException(localizedEmailInUseWrongPassword(languageCode)))
        FirestorePayloadSanitizer.refreshAuthToken(existingUser)
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
      return Result.failure(OmniAuthException(localizedInvalidChurchName(languageCode)))
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
      ownerUid = user.uid,
      ownerEmail = (user.email ?: email).trim().lowercase(Locale.US),
      ownerName = user.displayName?.takeIf { it.isNotBlank() },
      ownerPhoto = user.photoUrl?.toString(),
      authMethod = "email_password",
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
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_SUPER_DEV_ISOLATION))
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

  /**
   * Resolves all churches owned by the currently authenticated Firebase Admin user by querying
   * `ownerId == uid`, `adminGoogleUid == uid`, and (when `emailVerified == true`)
   * `adminGoogleEmail == email.lowercase()`.
   */
  suspend fun resolveAdminOwnedChurches(user: FirebaseUser): List<Church> {
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

  /**
   * If a cached churchId triggers `owner_mismatch` / `PERMISSION_DENIED` while the signed-in user
   * uniquely owns a single church in Firestore, returns that church so the client can cleanly
   * auto-rebind without manual intervention.
   */
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

  sealed class AdminAutoLoginResolution {
    data class SingleChurchResolved(val church: Church) : AdminAutoLoginResolution()
    data class MultipleOwnedChurches(val churches: List<Church>) : AdminAutoLoginResolution()
  }

  /**
   * Smart Expert Admin Sign-In: authenticates with Firebase Email + Password, refreshes the ID
   * token, and resolves ONLY the churches strictly owned/authorized for that admin without ever
   * exposing other tenants' churches.
   *
   * - 1 owned church -> immediately syncs profile & returns [AdminAutoLoginResolution.SingleChurchResolved]
   * - >1 owned churches -> returns [AdminAutoLoginResolution.MultipleOwnedChurches] containing strictly owned churches
   * - 0 owned churches -> signs out cleanly & returns an error
   */
  suspend fun signInAdminSmartWithEmailAndPassword(
    email: String,
    password: String,
  ): Result<AdminAutoLoginResolution> {
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

      // Refresh ID token immediately so `email_verified` claims are current before Firestore queries/writes.
      FirestorePayloadSanitizer.refreshAuthToken(firebaseUser)

      val resolvedEmail = (firebaseUser.email ?: cleanEmail).trim().lowercase(Locale.US)
      val isSuperDev = CryptoSecurity.isSuperDeveloperAccount(resolvedEmail)

      val ownedChurches = linkedMapOf<String, Church>()
      // 1. Strictly query churches directly owned by this Firebase user
      resolveAdminOwnedChurches(firebaseUser).forEach { church ->
        ownedChurches[church.id] = church
      }

      // 2. Also include church linked on users/{uid} ONLY if admin/super_admin and strictly authorized
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
        // Fallback check for unverified email case where church stores matching email/adminGoogleEmail
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
          finalizeAdminChurchSelection(targetChurch)
            .map { AdminAutoLoginResolution.SingleChurchResolved(it) }
        }
        else -> {
          Result.success(AdminAutoLoginResolution.MultipleOwnedChurches(authorizedList))
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: FirebaseAuthInvalidUserException) {
      Result.failure(
        OmniAuthException("Kont imèl sa a pa egziste oswa li dezaktive. Verifye imèl la.")
      )
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Result.failure(
        OmniAuthException("Imèl oswa modpas la pa kòrèk. Verifye enfòmasyon yo epi eseye ankò.")
      )
    } catch (e: FirebaseTooManyRequestsException) {
      Result.failure(
        OmniAuthException("Twòp tantativ koneksyon echwe. Tanpri tann kèk minit anvan ou re-eseye.")
      )
    } catch (e: FirebaseNetworkException) {
      Result.failure(
        OmniAuthException("Pa gen koneksyon entènèt. Verifye rezo ou epi eseye ankò.")
      )
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "churches")
      Result.failure(OmniAuthException(e.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."))
    }
  }

  /**
   * Finalizes an authenticated admin's selection of one of their own churches (binding `users/{uid}`
   * and authorizing the local session lock).
   */
  suspend fun finalizeAdminChurchSelection(church: Church): Result<Church> {
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

  /**
   * Smart Expert Admin Sign-In for Google Sign-In: uses the currently authenticated Firebase
   * Google user, refreshes the ID token, and resolves ONLY their owned/authorized churches
   * without exposing any other church on the platform.
   */
  suspend fun signInAdminSmartWithCurrentGoogleUser(): Result<AdminAutoLoginResolution> {
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
      resolveAdminOwnedChurches(firebaseUser).forEach { church ->
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
          finalizeAdminChurchSelection(authorizedList.first())
            .map { AdminAutoLoginResolution.SingleChurchResolved(it) }
        }
        else -> {
          Result.success(AdminAutoLoginResolution.MultipleOwnedChurches(authorizedList))
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "churches")
      Result.failure(OmniAuthException(e.localizedMessage ?: "Erè pandan verifikasyon kont Google Admin nan."))
    }
  }

  suspend fun signInAdminWithEmailAndPassword(
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

      // Immediately refresh ID token so updated `email_verified` claims propagate before writes.
      FirestorePayloadSanitizer.refreshAuthToken(firebaseUser)

      val existingUserProfile =
        runCatching {
          db.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()
            .toUserProfile()
        }.getOrNull()

      // First resolve churches directly owned by this Firebase user (ownerId, adminGoogleUid, verified email)
      val directOwnedChurches = resolveAdminOwnedChurches(firebaseUser)
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
        // Auto-rebind when cached/selected church mismatches but user uniquely owns one church
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
      Result.failure(
        OmniAuthException("Kont imèl sa a pa egziste oswa li dezaktive. Verifye imèl la.")
      )
    } catch (e: FirebaseAuthInvalidCredentialsException) {
      Result.failure(
        OmniAuthException("Imèl oswa modpas la pa kòrèk. Verifye enfòmasyon yo epi eseye ankò.")
      )
    } catch (e: FirebaseTooManyRequestsException) {
      Result.failure(
        OmniAuthException("Twòp tantativ koneksyon echwe. Tanpri tann kèk minit anvan ou re-eseye.")
      )
    } catch (e: FirebaseNetworkException) {
      Result.failure(
        OmniAuthException("Pa gen koneksyon entènèt. Verifye rezo ou epi eseye ankò.")
      )
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "churches")
      Result.failure(OmniAuthException(e.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."))
    }
  }

  suspend fun signInAdminWithEmailAndPassword(
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
      FirestorePayloadSanitizer.refreshAuthToken(user)
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

  suspend fun verifySignedInAdminGoogleAccount(
    church: Church,
    languageCode: String,
  ): Result<Church> {
    val user =
      firebaseAuth.currentUser
        ?: return Result.failure(OmniAuthException(localizedNoSignedInUser(languageCode)))
    FirestorePayloadSanitizer.refreshAuthToken(user)
    return verifyAndHealChurchOwnership(
      church = church,
      user = user,
      authMethodUsed = "google",
      languageCode = languageCode,
    )
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
          ?: return Result.failure(OmniAuthException(localizedGenericAuthError(languageCode)))
      FirestorePayloadSanitizer.refreshAuthToken(user)
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
        if (church.adminGoogleUid.isNullOrBlank()) {
          val resolvedMethod = mergeAuthMethods(church.adminAuthMethod, authMethodUsed)
          val updates =
            FirestorePayloadSanitizer.sanitizeChurchUpdate(
              raw =
                hashMapOf(
                  "adminGoogleUid" to user.uid,
                  "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
                  "adminGooglePhoto" to user.photoUrl?.toString()?.takeIf { it.isNotEmpty() },
                  "adminAuthMethod" to resolvedMethod,
                  "updatedAt" to FieldValue.serverTimestamp(),
                ),
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
          FirestorePayloadSanitizer.sanitizeChurchUpdate(
            raw =
              hashMapOf(
                "adminGoogleEmail" to cleanEmail.takeIf { it.isNotEmpty() },
                "adminGoogleUid" to user.uid,
                "adminGoogleName" to user.displayName?.trim()?.takeIf { it.isNotEmpty() },
                "adminGooglePhoto" to user.photoUrl?.toString()?.takeIf { it.isNotEmpty() },
                "adminAuthMethod" to authMethodUsed,
                "updatedAt" to FieldValue.serverTimestamp(),
              ),
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
        val reboundChurch = resolveAutoRebindChurchIfUniquelyOwned(church.id)
        if (reboundChurch != null) {
          sessionLockManager.authorizeChurchAdminSession(reboundChurch.id)
          Result.success(reboundChurch)
        } else {
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
      }
      OwnershipEvaluationStatus.NO_AUTH_USER -> {
        Result.failure(OmniAuthException(localizedNoSignedInUser(languageCode)))
      }
    }
  }

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

  suspend fun verifyQuickPinAndUpgradeIfNeeded(
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

    if (liveUser != null) {
      FirestorePayloadSanitizer.refreshAuthToken(liveUser)
    }
    sessionLockManager.authorizeChurchAdminSession(church.id)
    return Result.success(updatedChurch)
  }

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
      Result.failure(OmniAuthException(localizedWrongPin(languageCode)))
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
        .getOrElse { return Result.failure(OmniAuthException(it.localizedMessage ?: localizedNoSignedInUser(languageCode))) }

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

  private suspend fun syncAdminUserProfileForChurch(
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
        userRef
          .update(
            filterNonNullMap(
              "churchId" to church.id.trim(),
              "churchName" to church.name.trim().ifBlank { "OmniChurch Pro" },
              "role" to targetRole,
              "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
              "photoUrl" to photoUrl?.trim()?.takeIf { it.isNotEmpty() },
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        userRef
          .set(
            filterNonNullMap(
              "userId" to uid,
              "email" to email.trim().ifBlank { "admin@omnichurch.org" },
              "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
              "photoUrl" to photoUrl?.trim()?.takeIf { it.isNotEmpty() },
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
          if (snapshot.exists()) {
            snapshot.toUserProfile()
          } else {
            null
          }
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
        docRef
          .get()
          .await()
          .toUserProfile()
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

  companion object {
    private const val TAG = "AuthRepository"
  }
}
