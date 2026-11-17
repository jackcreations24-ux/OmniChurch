package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuthRepository(
  private val db: FirebaseFirestore,
  private val churchRepository: ChurchRepository,
) {
  constructor(context: Context) : this(
    db = FirestoreProvider.get(context),
    churchRepository = ChurchRepository(context),
  )

  private val auth = Firebase.auth

  fun resolveActorId(preferredActorId: String? = null): String {
    val cleanPreferred = (preferredActorId ?: "").trim()
    if (cleanPreferred.isNotEmpty()) return cleanPreferred
    return auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
  }

  suspend fun authenticateMemberWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthenticatedMemberSession> {
    val cleanInput = idOrPhone.trim()
    val cleanPin = enteredPin.trim()

    if (cleanInput.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_ID))
    }
    if (cleanPin.isEmpty()) {
      return Result.failure(OmniAuthException(OmniAuthEvaluator.ERROR_EMPTY_PIN))
    }

    return try {
      val churches = churchRepository.fetchAllChurches()
      val resolvedChurch =
        OmniAuthEvaluator.resolveChurchForMemberLogin(
          churches = churches,
          selectedChurchId = selectedChurchId,
          idOrPhoneInput = cleanInput,
        )
          ?: return Result.failure(
            OmniAuthException(OmniAuthEvaluator.ERROR_NO_CHURCH_SELECTED)
          )

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
      val allMembers = membersSnap.documents.mapNotNull { it.toMember() }

      OmniAuthEvaluator.authenticateMemberFromList(
        church = resolvedChurch,
        churchMembers = allMembers,
        idOrPhone = cleanInput,
        enteredPin = cleanPin,
      )
    } catch (e: CancellationException) {
      currentCoroutineContext().ensureActive()
      Result.failure(OmniAuthException(" koneksyon an te entèwonp, tanpri eseye ankò."))
    } catch (e: OmniAuthException) {
      Result.failure(e)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "members")
      Result.failure(
        OmniAuthException(e.localizedMessage ?: "Erè koneksyon ak baz done Firestore la.")
      )
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
      filterNonNullMap(
        "googleEmail" to cleanGoogleEmail,
        "googleUid" to cleanGoogleUid,
        "googlePhoto" to googlePhotoUrl?.trim()?.takeIf { it.isNotEmpty() },
        "email" to preservedEmail,
        "updatedAt" to FieldValue.serverTimestamp(),
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

      val middleware = com.zoutiw.omnichurch.core.security.AdminAuthorizationMiddleware(db)
      val authResult = middleware.authorizeAdminAction(
        churchId = cleanChurchId,
        enteredPasscode = enteredPasscode,
        googleEmail = googleEmail,
        googleUid = googleUid,
        church = church
      )
      
      if (authResult.isSuccess) {
        Result.success(church)
      } else {
        Result.failure(authResult.exceptionOrNull() ?: OmniAuthException("Erè otantifikasyon."))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "churches/$cleanChurchId")
      Result.failure(e)
    }
  }

  suspend fun signInAdminWithEmailAndPassword(
    email: String,
    passcode: String,
  ): Result<Church> {
    val cleanEmail = email.trim().lowercase()
    val cleanPasscode = passcode.trim()
    if (cleanEmail.isEmpty() || cleanPasscode.isEmpty()) {
      return Result.failure(OmniAuthException("Tanpri antre imèl ak modpas ou."))
    }
    return try {
      val authResult = auth.signInWithEmailAndPassword(cleanEmail, cleanPasscode).await()
      val firebaseUser = authResult.user ?: return Result.failure(OmniAuthException("Otantifikasyon echwe."))

      val churches = churchRepository.fetchAllChurches()
      val matchedChurch = churches.firstOrNull { church ->
        church.adminGoogleEmail.trim().lowercase() == cleanEmail ||
          church.ownerId == firebaseUser.uid
      } ?: churches.firstOrNull()

      if (matchedChurch == null) {
        return Result.failure(OmniAuthException("Okenn legliz pa jwenn pou imèl sa a."))
      }
      Result.success(matchedChurch)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "churches")
      Result.failure(OmniAuthException(e.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."))
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
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
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
        val existingProfile =
          existingSnap.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
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
          .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
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
    val uid = auth.currentUser?.uid?.trim().orEmpty()
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
}
