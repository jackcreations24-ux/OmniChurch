package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Member
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Clean Architecture UseCase managing real-time and server-confirmed member session observation
 * with cache fallback and automatic retry on transient network errors.
 */
class MemberSessionObservationUseCase(
  private val db: FirebaseFirestore,
) {
  private suspend fun fetchConfirmedMemberFromFirestore(
    cleanChurchId: String,
    cleanDocId: String,
    cleanMatrikil: String,
    source: Source,
  ): Member? {
  if (cleanDocId.isNotEmpty()) {
    val docSnap = db.collection("members").document(cleanDocId).get(source).awaitSafe()
    if (docSnap.exists()) {
      docSnap.toMember()?.let { return it }
    }
  }

  if (cleanMatrikil.isEmpty()) return null

  fun matchesTarget(member: Member, enforceChurch: Boolean): Boolean {
    val matchesChurch =
      !enforceChurch ||
        cleanChurchId.isEmpty() ||
        member.churchId.trim().equals(cleanChurchId, ignoreCase = true)
    if (!matchesChurch) return false
    val upper = cleanMatrikil.uppercase(Locale.US)
    val digitsOnly = cleanMatrikil.replace(Regex("[^0-9]"), "")
    val memberPhoneDigits = (member.phone ?: "").replace(Regex("[^0-9]"), "")
    return member.effectiveMatrikil.equals(cleanMatrikil, ignoreCase = true) ||
      member.id.equals(cleanMatrikil, ignoreCase = true) ||
      member.memberCode.equals(cleanMatrikil, ignoreCase = true) ||
      member.memberId.equals(upper, ignoreCase = true) ||
      (digitsOnly.length >= 7 && memberPhoneDigits == digitsOnly)
  }

  val candidateQueries =
    buildList {
      add(db.collection("members").whereEqualTo("memberId", cleanMatrikil).limit(10))
      val upper = cleanMatrikil.uppercase(Locale.US)
      if (upper != cleanMatrikil) {
        add(db.collection("members").whereEqualTo("memberId", upper).limit(10))
      }
      add(db.collection("members").whereEqualTo("memberCode", cleanMatrikil).limit(10))
      if (upper != cleanMatrikil) {
        add(db.collection("members").whereEqualTo("memberCode", upper).limit(10))
      }
      if (cleanChurchId.isNotEmpty()) {
        add(db.collection("members").whereEqualTo("churchId", cleanChurchId))
      }
    }

  for (q in candidateQueries) {
    val snap = q.get(source).awaitSafe()
    val matched = snap.documents.mapNotNull { it.toMember() }.firstOrNull { matchesTarget(it, enforceChurch = true) }
    if (matched != null) return matched
  }

  val allSnap = db.collection("members").get(source).awaitSafe()
  val allMembers = allSnap.documents.mapNotNull { it.toMember() }
  return allMembers.firstOrNull { matchesTarget(it, enforceChurch = true) }
    ?: allMembers.firstOrNull { matchesTarget(it, enforceChurch = false) }
}

  fun observeSessionMemberResult(
    churchId: String,
    memberDocId: String,
    memberMatrikil: String,
  ): Flow<Result<Member?>> = flow {
    val cleanChurchId = churchId.trim()
    val cleanDocId = memberDocId.trim()
    val cleanMatrikil = memberMatrikil.trim()
    if (cleanDocId.isEmpty() && cleanMatrikil.isEmpty()) {
      emit(Result.success(null))
      return@flow
    }

    emitAll(
      channelFlow {
        var hasServerConfirmation = false
        var latestConfirmedMember: Member? = null

        val serverWarmupJob = launch {
          try {
            val serverMember =
              fetchConfirmedMemberFromFirestore(
                cleanChurchId = cleanChurchId,
                cleanDocId = cleanDocId,
                cleanMatrikil = cleanMatrikil,
                source = Source.SERVER,
              )
            hasServerConfirmation = true
            latestConfirmedMember = serverMember
            trySend(Result.success(serverMember))
          } catch (e: CancellationException) {
            throw e
          } catch (e: Exception) {
            val opType = if (cleanDocId.isNotEmpty()) OperationType.GET else OperationType.LIST
            val path = if (cleanDocId.isNotEmpty()) "members/$cleanDocId" else "members"
            handleFirestoreError(e, opType, path)
            val cachedMember =
              runCatching {
                fetchConfirmedMemberFromFirestore(
                  cleanChurchId = cleanChurchId,
                  cleanDocId = cleanDocId,
                  cleanMatrikil = cleanMatrikil,
                  source = Source.CACHE,
                )
              }.getOrNull()
          if (cachedMember != null) {
            latestConfirmedMember = cachedMember
            trySend(Result.success(cachedMember))
          } else if (latestConfirmedMember == null && !hasServerConfirmation) {
            trySend(Result.failure(e))
          }
        }
      }

      try {
        if (cleanDocId.isNotEmpty()) {
          val path = "members/$cleanDocId"
          var attempt = 0
          while (currentCoroutineContext().isActive) {
            try {
              db.collection("members").document(cleanDocId).snapshots().collect { snap ->
                val liveMember = if (snap.exists()) snap.toMember() else null
                if (liveMember != null) {
                  attempt = 0
                  hasServerConfirmation = true
                  latestConfirmedMember = liveMember
                  trySend(Result.success(liveMember))
                } else if (!snap.metadata.isFromCache && hasServerConfirmation && latestConfirmedMember == null) {
                  attempt = 0
                  trySend(Result.success(null))
                }
              }
              break
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              handleFirestoreError(e, OperationType.GET, path)
              if (latestConfirmedMember == null && !hasServerConfirmation) {
                trySend(Result.failure(e))
              }
              val isPermissionError =
                (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
              if (isPermissionError) break
              attempt += 1
              delay((1500L * attempt).coerceAtMost(8000L))
            }
          }
        } else {
          val path = "members"
          val query =
            if (cleanChurchId.isNotEmpty()) {
              db.collection("members").whereEqualTo("churchId", cleanChurchId)
            } else {
              db.collection("members")
            }
          var attempt = 0
          while (currentCoroutineContext().isActive) {
            try {
              query.snapshots().collect { snapshot ->
                val liveMember =
                  snapshot.documents
                    .mapNotNull { it.toMember() }
                    .firstOrNull { member ->
                      val matchesChurch =
                        cleanChurchId.isEmpty() ||
                          member.churchId.trim().equals(cleanChurchId, ignoreCase = true)
                      val matchesId =
                        member.effectiveMatrikil.equals(cleanMatrikil, ignoreCase = true) ||
                          member.id.equals(cleanMatrikil, ignoreCase = true) ||
                          member.memberCode.equals(cleanMatrikil, ignoreCase = true)
                      matchesChurch && matchesId
                    }
                if (liveMember != null) {
                  attempt = 0
                  hasServerConfirmation = true
                  latestConfirmedMember = liveMember
                  trySend(Result.success(liveMember))
                } else if (!snapshot.metadata.isFromCache && hasServerConfirmation && latestConfirmedMember == null) {
                  attempt = 0
                  trySend(Result.success(null))
                }
              }
              break
            } catch (e: CancellationException) {
              throw e
            } catch (e: Exception) {
              handleFirestoreError(e, OperationType.LIST, path)
              if (latestConfirmedMember == null && !hasServerConfirmation) {
                trySend(Result.failure(e))
              }
              val isPermissionError =
                (e as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
              if (isPermissionError) break
              attempt += 1
              delay((1500L * attempt).coerceAtMost(8000L))
            }
          }
        }
      } finally {
        serverWarmupJob.cancel()
      }
    }
  )
}
}
