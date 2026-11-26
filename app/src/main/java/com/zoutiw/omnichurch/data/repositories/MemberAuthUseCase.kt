package com.zoutiw.omnichurch.data.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniAuthException
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await

/**
 * Cohesive UseCase handling Member Matrikil/PIN auto-resolving login,
 * Google account lookup by linked email, and linking a Google account to a Member profile.
 */
internal class MemberAuthUseCase(
  private val db: FirebaseFirestore,
  private val churchRepository: ChurchRepository,
) {
  suspend fun authenticateMemberSmartWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
  ): Result<AuthRepository.MemberAutoLoginResolution> {
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
          AuthRepository.MemberAutoLoginResolution.SingleMemberResolved(enriched.first())
        } else {
          AuthRepository.MemberAutoLoginResolution.MultipleMemberChurches(enriched)
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

      val targetedCandidates = linkedMapOf<String, Member>()
      runCatching {
        db.collection("members")
          .whereEqualTo("googleEmail", cleanEmail)
          .limit(10)
          .get(Source.SERVER)
          .awaitSafe()
          .documents
          .mapNotNull { it.toMember() }
          .forEach { targetedCandidates[it.id] = it }
      }
      if (targetedCandidates.isEmpty()) {
        runCatching {
          db.collection("members")
            .whereEqualTo("email", cleanEmail)
            .limit(10)
            .get(Source.SERVER)
            .awaitSafe()
            .documents
            .mapNotNull { it.toMember() }
            .forEach { targetedCandidates[it.id] = it }
        }
      }

      val allMembers =
        if (targetedCandidates.isNotEmpty()) {
          targetedCandidates.values.toList()
        } else {
          val membersSnap =
            runCatching { db.collection("members").get(Source.SERVER).awaitSafe() }
              .getOrElse { db.collection("members").get().awaitSafe() }
          membersSnap.documents.mapNotNull { it.toMember() }
        }

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
          ?: runCatching {
            val cid = matchedMember.churchId.trim()
            if (cid.isNotEmpty()) {
              db.collection("churches").document(cid).get(Source.SERVER).awaitSafe().toChurch()
                ?: db
                  .collection("churches")
                  .whereEqualTo("code", cid)
                  .limit(1)
                  .get(Source.SERVER)
                  .awaitSafe()
                  .documents
                  .firstOrNull()
                  ?.toChurch()
            } else null
          }.getOrNull()
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

    val hasExistingPhotoPreference =
      member.avatarUrl != null || member.photoUrl != null || member.googlePhoto != null
    val rawUpdates =
      hashMapOf<String, Any?>(
        "googleEmail" to cleanGoogleEmail.lowercase(Locale.US),
        "googleUid" to cleanGoogleUid,
        "email" to preservedEmail,
        "updatedAt" to FieldValue.serverTimestamp(),
      ).apply {
        if (!hasExistingPhotoPreference) {
          val candidatePhoto = googlePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
          if (candidatePhoto != null) {
            put("googlePhoto", candidatePhoto)
          }
        }
      }

    val updates = FirestorePayloadSanitizer.sanitizeMemberUpdate(rawUpdates)

    return try {
      docRef.update(updates).await()
      val updatedMember =
        docRef.get().await().toMember()
          ?: member.copy(
            googleEmail = cleanGoogleEmail,
            googleUid = cleanGoogleUid,
            googlePhoto = if (hasExistingPhotoPreference) member.googlePhoto else googlePhotoUrl,
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
      val matchedKnown =
        knownChurches.firstOrNull { OmniAuthEvaluator.doesMemberBelongToChurch(session.member, it) }
      if (matchedKnown != null) {
        session.copy(church = matchedKnown)
      } else {
        val memberChurchId = session.member.churchId.trim()
        val fetchedChurch =
          if (memberChurchId.isNotEmpty()) {
            runCatching {
              db.collection("churches").document(memberChurchId).get().awaitSafe().toChurch()
                ?: db
                  .collection("churches")
                  .whereEqualTo("code", memberChurchId)
                  .limit(1)
                  .get()
                  .awaitSafe()
                  .documents
                  .firstOrNull()
                  ?.toChurch()
            }.getOrNull()
          } else null
        if (fetchedChurch != null) session.copy(church = fetchedChurch) else session
      }
    }
  }
}
