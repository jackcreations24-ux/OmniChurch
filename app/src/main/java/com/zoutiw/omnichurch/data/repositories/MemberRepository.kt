package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MemberRepository(
  internal val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  internal val auth = Firebase.auth

  suspend fun fetchMembersForChurch(churchId: String, churchCode: String? = null): List<Member> {
    val cleanId = churchId.trim()
    val cleanCode = churchCode?.trim().orEmpty()
    if (cleanId.isEmpty() && cleanCode.isEmpty()) return emptyList()

    val matchIds = listOfNotNull(
      cleanId.takeIf { it.isNotBlank() },
      cleanCode.takeIf { it.isNotBlank() && !it.equals(cleanId, ignoreCase = true) }
    ).distinct()

    return try {
      val snap = if (matchIds.size > 1) {
        try {
          db.collection("members").whereIn("churchId", matchIds).get().awaitSafe()
        } catch (_: Exception) {
          db.collection("members").whereEqualTo("churchId", cleanId).get().awaitSafe()
        }
      } else {
        db.collection("members").whereEqualTo("churchId", matchIds.firstOrNull() ?: cleanId).get().awaitSafe()
      }
      snap.documents.mapNotNull { it.toMember() }.sortedBy { it.effectiveFullName.lowercase() }
    } catch (e: CancellationException) {
      currentCoroutineContext().ensureActive()
      emptyList()
    } catch (e: Exception) {
      Log.e("MemberRepository", "Error fetching members for church $cleanId: ${e.message}", e)
      emptyList()
    }
  }

  /**
   * Lightweight one-shot directory of public church display names for Deacon service creation.
   * Strips ALL sensitive PII, PINs, contact numbers, emails, legal docs, and barcodes so pure
   * Deacons never open a full `members` real-time listener or receive sensitive PII.
   */
  suspend fun fetchPublicMemberDisplayNames(
    churchId: String,
    churchCode: String? = null,
  ): List<Member> {
    val cleanId = churchId.trim()
    val cleanCode = churchCode?.trim().orEmpty()
    if (cleanId.isEmpty() && cleanCode.isEmpty()) return emptyList()

    val candidates = linkedMapOf<String, Member>()
    // 1. Check optional lightweight public_member_names / church display directory if present
    runCatching {
      val churchSnap = db.collection("churches").document(cleanId).get().awaitSafe()
      val rawNames = churchSnap.get("publicMemberNames") as? List<*>
      rawNames?.forEachIndexed { idx, entry ->
        when (entry) {
          is String -> {
            val cleanName = entry.trim()
            if (cleanName.isNotEmpty()) {
              candidates["pub_$idx"] = Member(id = "pub_$idx", churchId = cleanId, fullName = cleanName)
            }
          }
          is Map<*, *> -> {
            val id = (entry["id"] as? String)?.trim().orEmpty().ifBlank { "pub_$idx" }
            val name = (entry["fullName"] as? String)?.trim()
              ?: (entry["name"] as? String)?.trim().orEmpty()
            val photo = (entry["photoUrl"] as? String)?.trim()
              ?: (entry["avatarUrl"] as? String)?.trim()
            if (name.isNotEmpty()) {
              candidates[id] = Member(id = id, churchId = cleanId, fullName = name, photoUrl = photo)
            }
          }
        }
      }
    }
    if (candidates.isNotEmpty()) {
      return candidates.values.sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
    }

    // 2. Fallback: one-shot fetch (never real-time listener) immediately stripped to public display-only entries
    val rawMembers = runCatching { fetchMembersForChurch(cleanId, cleanCode) }.getOrDefault(emptyList())
    return rawMembers
      .filter { !it.id.startsWith("ADMIN-", ignoreCase = true) && it.effectiveFullName.isNotBlank() }
      .map { it.toPublicDisplayEntry() }
      .distinctBy { it.id.ifBlank { it.effectiveFullName } }
      .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
  }

  fun observeChurchMembersResult(
    churchId: String,
    churchCode: String? = null,
  ): Flow<Result<List<Member>>> = flow {
    val cleanId = churchId.trim()
    val cleanCode = churchCode?.trim().orEmpty()
    val primaryTargetId = cleanId.ifBlank { cleanCode }
    if (primaryTargetId.isEmpty()) {
      emit(Result.success(emptyList()))
      return@flow
    }

    val path = "members"
    val primaryFlow: Flow<Result<List<Member>>> = channelFlow {
      var hasEmittedConfirmedOrNonEmpty = false
      val query = db.collection("members").whereEqualTo("churchId", primaryTargetId)

      val serverWarmupJob = launch {
        try {
          val serverSnap = query.get(Source.SERVER).awaitSafe()
          val list =
            serverSnap.documents
              .mapNotNull { it.toMember() }
              .distinctBy { it.id.ifBlank { it.effectiveMatrikil } }
              .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
          hasEmittedConfirmedOrNonEmpty = true
          trySend(Result.success(list))
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          handleFirestoreError(e, OperationType.LIST, path)
          val cachedList =
            runCatching {
              query
                .get(Source.CACHE)
                .awaitSafe()
                .documents
                .mapNotNull { it.toMember() }
                .distinctBy { it.id.ifBlank { it.effectiveMatrikil } }
                .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
            }.getOrDefault(emptyList())
          if (cachedList.isNotEmpty()) {
            hasEmittedConfirmedOrNonEmpty = true
            trySend(Result.success(cachedList))
          } else if (!hasEmittedConfirmedOrNonEmpty) {
            trySend(Result.failure(e))
          }
        }
      }

      var attempt = 0
      try {
        while (currentCoroutineContext().isActive) {
          try {
            query.snapshots().collect { snapshot ->
              val list =
                snapshot.documents
                  .mapNotNull { it.toMember() }
                  .distinctBy { it.id.ifBlank { it.effectiveMatrikil } }
                  .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
              if (snapshot.metadata.isFromCache && list.isEmpty() && !hasEmittedConfirmedOrNonEmpty) {
                return@collect
              }
              attempt = 0
              hasEmittedConfirmedOrNonEmpty = true
              trySend(Result.success(list))
            }
            break
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            if (!hasEmittedConfirmedOrNonEmpty) {
              trySend(Result.failure(e))
            }
            val isPermissionError =
              (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            if (isPermissionError) break
            attempt += 1
            delay((1500L * attempt).coerceAtMost(8000L))
          }
        }
      } finally {
        serverWarmupJob.cancel()
      }
    }

    val hasDistinctLegacyCode =
      cleanId.isNotBlank() &&
        cleanCode.isNotBlank() &&
        !cleanCode.equals(cleanId, ignoreCase = true)

    if (!hasDistinctLegacyCode) {
      emitAll(primaryFlow)
    } else {
      val legacyCodeFlow: Flow<List<Member>> =
        db
          .collection("members")
          .whereEqualTo("churchId", cleanCode)
          .snapshots()
          .map { snapshot -> snapshot.documents.mapNotNull { it.toMember() } }
          .onStart { emit(emptyList()) }
          .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
          }

      emitAll(
        combine(primaryFlow, legacyCodeFlow) { primaryResult, legacyMembers ->
          primaryResult.fold(
            onSuccess = { primaryMembers ->
              val merged =
                (primaryMembers + legacyMembers)
                  .distinctBy { it.id.ifBlank { it.effectiveMatrikil } }
                  .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
              Result.success(merged)
            },
            onFailure = { error ->
              if (legacyMembers.isNotEmpty()) {
                val sortedLegacy =
                  legacyMembers
                    .distinctBy { it.id.ifBlank { it.effectiveMatrikil } }
                    .sortedBy { it.effectiveFullName.lowercase(Locale.getDefault()) }
                Result.success(sortedLegacy)
              } else {
                Result.failure(error)
              }
            },
          )
        }
      )
    }
  }

  fun observeChurchMembers(churchId: String, churchCode: String? = null): Flow<List<Member>> =
    observeChurchMembersResult(churchId, churchCode).map { it.getOrDefault(emptyList()) }

  fun observeCurrentMember(userId: String): Flow<Member?> = flow {
    val path = "members"
    emitAll(
      db
        .collection("members")
        .whereEqualTo("userId", userId)
        .limit(1)
        .snapshots()
        .map { snapshot -> snapshot.documents.firstOrNull()?.toMember() }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(null)
        }
    )
  }

  fun observeSessionMemberResult(
    churchId: String,
    memberDocId: String,
    memberMatrikil: String,
  ): Flow<Result<Member?>> =
    observeSessionMemberResultImpl(db, churchId, memberDocId, memberMatrikil)

  fun observeSessionMember(
    churchId: String,
    memberDocId: String,
    memberMatrikil: String,
  ): Flow<Member?> =
    observeSessionMemberResult(churchId, memberDocId, memberMatrikil).map { it.getOrNull() }

  suspend fun registerMemberWithMatrikilAndPin(
    church: Church,
    fullNameInput: String,
    customMatrikilInput: String?,
    pinInput: String,
    phoneInput: String?,
    departmentInput: String,
    ministryRoleInput: String,
  ): Result<Member> =
    registerMemberWithMatrikilAndPinImpl(
      church, fullNameInput, customMatrikilInput, pinInput, phoneInput, departmentInput, ministryRoleInput,
    )

  suspend fun updateMemberProfile(
    member: Member,
    fullName: String,
    department: String,
    ministryRole: String,
  ): Result<Unit> = updateMemberProfileImpl(member, fullName, department, ministryRole)

  suspend fun updateMemberProfilePhoto(member: Member, photoDataUrl: String): Result<Unit> =
    updateMemberProfilePhotoImpl(member, photoDataUrl)

  suspend fun updateMemberByAdmin(
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String? = null,
    titlePrefix: String? = null,
  ): Result<Unit> =
    updateMemberByAdminImpl(memberDocId, fullName, department, ministryRole, phone, titlePrefix)

  suspend fun bindNfcCardUidToMember(memberDocId: String, nfcCardUid: String): Result<Unit> =
    bindNfcCardUidToMemberImpl(memberDocId, nfcCardUid)

  suspend fun resolveMemberCardVerification(
    rawTokenOrCode: String,
    fallbackChurchId: String? = null,
  ): Result<PublicVerificationResolution> =
    resolveMemberCardVerificationImpl(db, rawTokenOrCode, fallbackChurchId)
}
