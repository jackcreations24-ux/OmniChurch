package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
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
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  private val auth = Firebase.auth

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
            if (isPermissionError) {
              break
            }
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

  fun observeSessionMember(
    churchId: String,
    memberDocId: String,
    memberMatrikil: String,
  ): Flow<Member?> = flow {
    val cleanDocId = memberDocId.trim()
    val cleanMatrikil = memberMatrikil.trim()
    if (cleanDocId.isNotEmpty()) {
      val path = "members/$cleanDocId"
      emitAll(
        db
          .collection("members")
          .document(cleanDocId)
          .snapshots()
          .map { snap -> if (snap.exists()) snap.toMember() else null }
          .catch { error ->
            handleFirestoreError(error, OperationType.GET, path)
            emit(null)
          }
      )
    } else if (cleanMatrikil.isNotEmpty()) {
      val path = "members"
      val query =
        if (churchId.isNotBlank()) {
          db.collection("members").whereEqualTo("churchId", churchId.trim())
        } else {
          db.collection("members")
        }
      emitAll(
        query
          .snapshots()
          .map { snapshot ->
            snapshot.documents
              .mapNotNull { it.toMember() }
              .firstOrNull { member ->
                val matchesChurch =
                  churchId.isBlank() ||
                    member.churchId.trim().equals(churchId.trim(), ignoreCase = true)
                val matchesId =
                  member.effectiveMatrikil.equals(cleanMatrikil, ignoreCase = true) ||
                    member.id.equals(cleanMatrikil, ignoreCase = true) ||
                    member.memberCode.equals(cleanMatrikil, ignoreCase = true)
                matchesChurch && matchesId
              }
          }
          .catch { error ->
            handleFirestoreError(error, OperationType.LIST, path)
            emit(null)
          }
      )
    } else {
      emit(null)
    }
  }

  suspend fun registerMemberWithMatrikilAndPin(
    church: Church,
    fullNameInput: String,
    customMatrikilInput: String?,
    pinInput: String,
    phoneInput: String?,
    departmentInput: String,
    ministryRoleInput: String,
  ): Result<Member> {
    val adminUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "anrejistre yon nouvo manm")
        .getOrElse { return Result.failure(it) }

    val cleanChurchId = church.id.trim()
    val cleanName = fullNameInput.trim().take(200)
    if (!FirestorePayloadSanitizer.isValidChurchId(cleanChurchId)) {
      return Result.failure(IllegalArgumentException("ID legliz la pa valid."))
    }
    if (cleanName.isEmpty()) {
      return Result.failure(IllegalArgumentException("Tanpri antre non konplè manm nan."))
    }
    val cleanPin = pinInput.trim().ifEmpty { "1234" }
    val cleanDept = departmentInput.trim().ifEmpty { "Adorasyon" }
    val cleanRole = ministryRoleInput.trim().ifEmpty { "Manm" }

    val churchPrefix = church.effectiveCode.uppercase()
    val initials =
      cleanName
        .split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .take(2)
        .ifEmpty { "MN" }
    val randomDigits = (1000..9999).random()
    val matrikil =
      customMatrikilInput?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
        ?: "$churchPrefix-$initials$randomDigits"

    val docRef = db.collection("members").document()
    val generatedUserId = "mem_uid_${docRef.id}"
    val barcodeToken =
      "OMNICHURCH_${cleanChurchId}_${docRef.id}_${UUID.randomUUID().toString().take(8).uppercase()}"
    val joinDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    val rawPayload =
      hashMapOf<String, Any?>(
        "id" to docRef.id,
        "userId" to generatedUserId,
        "memberId" to matrikil,
        "churchId" to cleanChurchId,
        "churchName" to church.name,
        "fullName" to cleanName,
        "memberCode" to matrikil,
        "phone" to phoneInput?.trim()?.takeIf { it.isNotEmpty() },
        "pin" to cleanPin,
        "securityPin" to cleanPin,
        "department" to cleanDept,
        "role" to cleanRole,
        "status" to "active",
        "barcodeToken" to barcodeToken,
        "joinDateFormatted" to joinDateFormatted,
        "createdBy" to adminUser.uid,
        "updatedBy" to adminUser.uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val payload = FirestorePayloadSanitizer.sanitizeMemberCreate(rawPayload, docRef.id)

    return try {
      docRef.set(payload).await()
      val created =
        docRef.get().await().toMember()
          ?: Member(
            id = docRef.id,
            memberId = matrikil,
            userId = generatedUserId,
            churchId = cleanChurchId,
            churchName = church.name,
            fullName = cleanName,
            memberCode = matrikil,
            phone = phoneInput?.trim(),
            pin = cleanPin,
            securityPin = cleanPin,
            department = cleanDept,
            role = cleanRole,
            status = "active",
            barcodeToken = barcodeToken,
            joinDateFormatted = joinDateFormatted,
          )
      Result.success(created)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateMemberProfile(
    member: Member,
    fullName: String,
    department: String,
    ministryRole: String,
  ): Result<Unit> {
    val cleanMemberId = member.id.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanMemberId)) {
      return Result.failure(IllegalArgumentException("ID manm nan pa valid."))
    }
    val memberRef = db.collection("members").document(cleanMemberId)
    val updates =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        hashMapOf(
          "fullName" to fullName.trim(),
          "department" to department.trim(),
          "role" to ministryRole.trim(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    return try {
      memberRef.update(updates).await()

      val currentUid = auth.currentUser?.uid
      if (!currentUid.isNullOrBlank() && currentUid == member.userId) {
        val userRef = db.collection("users").document(currentUid)
        val snap = userRef.get().await()
        if (snap.exists()) {
          userRef
            .update(
              filterNonNullMap(
                "displayName" to fullName.trim(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            )
            .await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateMemberProfilePhoto(
    member: Member,
    photoDataUrl: String,
  ): Result<Unit> {
    val cleanMemberId = member.id.trim()
    val cleanPhoto = photoDataUrl.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanMemberId)) {
      return Result.failure(IllegalArgumentException("ID manm nan pa valid."))
    }
    if (cleanPhoto.isEmpty()) {
      return Result.failure(IllegalArgumentException("Foto a pa valab."))
    }
    val memberRef = db.collection("members").document(cleanMemberId)
    val updates =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        hashMapOf(
          "avatarUrl" to cleanPhoto,
          "photoUrl" to cleanPhoto,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    return try {
      memberRef.update(updates).await()

      val currentUid = auth.currentUser?.uid
      if (!currentUid.isNullOrBlank()) {
        try {
          val userRef = db.collection("users").document(currentUid)
          val snap = userRef.get().await()
          if (snap.exists()) {
            userRef
              .update(
                filterNonNullMap(
                  "photoUrl" to cleanPhoto,
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          }
        } catch (userErr: Exception) {
          Log.w("MemberRepository", "Non-fatal: could not sync photoUrl: ${userErr.message}")
        }
      }
      return Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      return Result.failure(e)
    }
  }

  suspend fun updateMemberByAdmin(
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String? = null,
    titlePrefix: String? = null,
  ): Result<Unit> {
    val adminUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "modifye enfòmasyon manm nan")
        .getOrElse { return Result.failure(it) }

    val cleanDocId = memberDocId.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanDocId)) {
      return Result.failure(IllegalArgumentException("ID dokiman manm nan pa valid."))
    }
    val memberRef = db.collection("members").document(cleanDocId)
    val updates =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        hashMapOf(
          "fullName" to fullName.trim(),
          "department" to department.trim(),
          "role" to ministryRole.trim(),
          "phone" to phone?.trim()?.takeIf { it.isNotBlank() },
          "titlePrefix" to titlePrefix?.trim()?.takeIf { it.isNotBlank() },
          "updatedBy" to adminUser.uid,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    return try {
      memberRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      Result.failure(e)
    }
  }

  suspend fun bindNfcCardUidToMember(
    memberDocId: String,
    nfcCardUid: String,
  ): Result<Unit> {
    val adminUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "lye kat NFC manm nan")
        .getOrElse { return Result.failure(it) }

    val cleanDocId = memberDocId.trim()
    val cleanUid = nfcCardUid.trim().uppercase().removePrefix("NFC-UID:").trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanDocId) || cleanUid.isBlank()) {
      return Result.failure(IllegalArgumentException("ID manm nan oswa UID kat NFC a pa valid."))
    }
    val memberRef = db.collection("members").document(cleanDocId)
    val updates =
      FirestorePayloadSanitizer.sanitizeMemberUpdate(
        hashMapOf(
          "nfcCardUid" to cleanUid,
          "updatedBy" to adminUser.uid,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      )
    return try {
      memberRef.update(updates).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      Result.failure(e)
    }
  }

  suspend fun resolveMemberCardVerification(
    rawTokenOrCode: String,
    fallbackChurchId: String? = null,
  ): Result<PublicVerificationResolution> {
    val cleanedInput = rawTokenOrCode.trim()
    if (cleanedInput.isEmpty()) {
      return Result.failure(
        IllegalArgumentException("Tanpri eskane yon kòd QR, yon kat NFC, oswa antre yon Matrikil.")
      )
    }

    val parsedQr = OmniQrParser.parseQrToken(cleanedInput)
    var candidateChurchId = parsedQr.churchId?.trim()?.takeIf { it.isNotEmpty() }
    var candidateDocId: String? = null
    val targetMemberIdentifier = parsedQr.memberId.trim().ifEmpty { cleanedInput }

    // Support legacy/standard OMNICHURCH_<churchId>_<memberDocId>_<nonce> tokens
    if (cleanedInput.startsWith("OMNICHURCH_", ignoreCase = true)) {
      val parts = cleanedInput.split("_")
      if (parts.size >= 3) {
        if (candidateChurchId.isNullOrBlank()) {
          candidateChurchId = parts[1].trim().takeIf { it.isNotEmpty() }
        }
        candidateDocId = parts[2].trim().takeIf { it.isNotEmpty() }
      }
    }

    val fallbackClean = fallbackChurchId?.trim()?.takeIf { it.isNotEmpty() }
    val rawUpper = cleanedInput.uppercase(Locale.US)
    val targetUpper = targetMemberIdentifier.uppercase(Locale.US)
    val rawWithoutNfc = rawUpper.removePrefix("NFC-UID:").trim()
    val targetWithoutNfc = targetUpper.removePrefix("NFC-UID:").trim()

    fun Member.matchesVerificationQuery(): Boolean {
      val memberNfcUid = nfcCardUid.trim().uppercase(Locale.US).removePrefix("NFC-UID:").trim()
      return effectiveMatrikil.uppercase(Locale.US) == targetUpper ||
        id.trim().uppercase(Locale.US) == targetUpper ||
        memberCode.trim().uppercase(Locale.US) == targetUpper ||
        barcodeToken.trim().uppercase(Locale.US) == targetUpper ||
        qrCode.trim().uppercase(Locale.US) == targetUpper ||
        barcodeToken.trim().uppercase(Locale.US) == rawUpper ||
        qrCode.trim().uppercase(Locale.US) == rawUpper ||
        effectiveMatrikil.uppercase(Locale.US) == rawUpper ||
        id.trim().uppercase(Locale.US) == rawUpper ||
        (candidateDocId != null && id.trim().equals(candidateDocId, ignoreCase = true)) ||
        (memberNfcUid.isNotEmpty() && (memberNfcUid == rawWithoutNfc || memberNfcUid == targetWithoutNfc))
    }

    suspend fun loadChurchByIdOrCode(identifier: String): Church? {
      val clean = identifier.trim()
      if (clean.isEmpty()) return null
      val direct =
        runCatching {
          db.collection("churches").document(clean).get().awaitSafe().toChurch()
        }.getOrNull()
      if (direct != null) return direct
      val allChurches =
        runCatching {
          db.collection("churches").get().awaitSafe().documents.mapNotNull { it.toChurch() }
        }.getOrDefault(emptyList())
      return allChurches.firstOrNull { church ->
        church.id.equals(clean, ignoreCase = true) ||
          (church.code.isNotBlank() && church.code.equals(clean, ignoreCase = true))
      }
    }

    return try {
      // 1. Resolve target Church (explicit QR church -> Matrikil prefix -> fallback active church)
      var resolvedChurch: Church? =
        candidateChurchId?.let { loadChurchByIdOrCode(it) }
          ?: run {
            val matrikilPrefix =
              targetMemberIdentifier.substringBefore("-", "").trim().takeIf {
                targetMemberIdentifier.contains("-") && it.length in 2..16
              }
            matrikilPrefix?.let { loadChurchByIdOrCode(it) }
          }
          ?: fallbackClean?.let { loadChurchByIdOrCode(it) }

      // 2. Search members within resolved church (by canonical id and legacy code if distinct)
      var matchedMember: Member? = null
      if (resolvedChurch != null) {
        val searchKeys =
          listOfNotNull(
            resolvedChurch.id.trim().takeIf { it.isNotEmpty() },
            resolvedChurch.code.trim().takeIf {
              it.isNotEmpty() && !it.equals(resolvedChurch.id.trim(), ignoreCase = true)
            },
          )
        for (key in searchKeys) {
          val snap =
            runCatching {
              db.collection("members").whereEqualTo("churchId", key).get().awaitSafe()
            }.getOrNull()
          val found = snap?.documents?.mapNotNull { it.toMember() }?.firstOrNull { it.matchesVerificationQuery() }
          if (found != null) {
            matchedMember = found
            break
          }
        }
      }

      // 3. Direct member document lookup fallback if a doc ID is present
      if (matchedMember == null) {
        val directDocCandidates =
          listOfNotNull(
            candidateDocId,
            targetMemberIdentifier.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]{8,64}$")) },
          ).distinct()
        for (docId in directDocCandidates) {
          val doc =
            runCatching {
              db.collection("members").document(docId).get().awaitSafe().toMember()
            }.getOrNull()
          if (doc != null && doc.matchesVerificationQuery()) {
            matchedMember = doc
            break
          }
        }
      }

      // 4. Fallback to fallbackChurchId if QR church differed or wasn't found
      if (matchedMember == null && fallbackClean != null && fallbackClean != resolvedChurch?.id) {
        val fallbackChurch = loadChurchByIdOrCode(fallbackClean)
        if (fallbackChurch != null) {
          val snap =
            runCatching {
              db.collection("members").whereEqualTo("churchId", fallbackChurch.id).get().awaitSafe()
            }.getOrNull()
          val found = snap?.documents?.mapNotNull { it.toMember() }?.firstOrNull { it.matchesVerificationQuery() }
          if (found != null) {
            matchedMember = found
            resolvedChurch = fallbackChurch
          }
        }
      }

      if (matchedMember == null) {
        return Result.failure(
          IllegalStateException(
            "Kat sa a pa jwenn nan rejis ofisyèl la. Verifye kòd QR la, kat NFC a, oswa Matrikil la."
          )
        )
      }

      // Ensure we always return the member's true home church record
      val finalChurch =
        resolvedChurch?.takeIf {
          it.id.equals(matchedMember.churchId, ignoreCase = true) ||
            it.code.equals(matchedMember.churchId, ignoreCase = true)
        }
          ?: loadChurchByIdOrCode(matchedMember.churchId)
          ?: resolvedChurch
          ?: Church(
            id = matchedMember.churchId,
            name = matchedMember.churchName.ifBlank { "OmniChurch" },
          )

      Result.success(PublicVerificationResolution(church = finalChurch, member = matchedMember))
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, "members")
      Result.failure(
        IllegalStateException(
          e.localizedMessage ?: "Erè pandan verifikasyon kat la nan sèvè a."
        )
      )
    }
  }
}
