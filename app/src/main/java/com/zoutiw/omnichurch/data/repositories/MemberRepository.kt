package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

  fun observeChurchMembers(churchId: String, churchCode: String? = null): Flow<List<Member>> = flow {
    val cleanId = churchId.trim()
    val cleanCode = churchCode?.trim().orEmpty()
    val path = "members"
    emitAll(
      db
        .collection("members")
        .snapshots()
        .map { snapshot ->
          val members = snapshot.documents.mapNotNull { it.toMember() }
          if (cleanId.isBlank() && cleanCode.isBlank()) {
            members.sortedBy { it.effectiveFullName.lowercase() }
          } else {
            members.filter { member ->
              val mChurchId = member.churchId.trim()
              mChurchId.isBlank() ||
                (cleanId.isNotBlank() && mChurchId.equals(cleanId, ignoreCase = true)) ||
                (cleanCode.isNotBlank() && mChurchId.equals(cleanCode, ignoreCase = true)) ||
                (cleanId.isNotBlank() && member.churchName.contains(cleanId, ignoreCase = true))
            }.sortedBy { it.effectiveFullName.lowercase() }
          }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

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
    val cleanName = fullNameInput.trim()
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
      "OMNICHURCH_${church.id}_${docRef.id}_${UUID.randomUUID().toString().take(8).uppercase()}"
    val joinDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

    val payload =
      filterNonNullMap(
        "userId" to generatedUserId,
        "memberId" to matrikil,
        "churchId" to church.id,
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
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      docRef.set(payload).await()
      val created =
        docRef.get().await().toMember()
          ?: Member(
            id = docRef.id,
            memberId = matrikil,
            userId = generatedUserId,
            churchId = church.id,
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
    val memberRef = db.collection("members").document(member.id)
    return try {
      memberRef
        .update(
          filterNonNullMap(
            "fullName" to fullName.trim(),
            "department" to department.trim(),
            "role" to ministryRole.trim(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

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
    val cleanPhoto = photoDataUrl.trim()
    if (cleanPhoto.isEmpty()) {
      return Result.failure(IllegalArgumentException("Foto a pa valab."))
    }
    val memberRef = db.collection("members").document(member.id)
    return try {
      memberRef
        .update(
          filterNonNullMap(
            "avatarUrl" to cleanPhoto,
            "photoUrl" to cleanPhoto,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

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
    if (memberDocId.isBlank()) {
      return Result.failure(IllegalArgumentException("ID dokiman manm nan vid."))
    }
    val memberRef = db.collection("members").document(memberDocId)
    return try {
      memberRef
        .update(
          filterNonNullMap(
            "fullName" to fullName.trim(),
            "department" to department.trim(),
            "role" to ministryRole.trim(),
            "phone" to phone?.trim()?.takeIf { it.isNotBlank() },
            "titlePrefix" to titlePrefix?.trim()?.takeIf { it.isNotBlank() },
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
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
    val cleanDocId = memberDocId.trim()
    val cleanUid = nfcCardUid.trim().uppercase().removePrefix("NFC-UID:").trim()
    if (cleanDocId.isBlank() || cleanUid.isBlank()) {
      return Result.failure(IllegalArgumentException("ID manm nan oswa UID kat NFC a pa valid."))
    }
    val memberRef = db.collection("members").document(cleanDocId)
    return try {
      memberRef
        .update(
          filterNonNullMap(
            "nfcCardUid" to cleanUid,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      Result.failure(e)
    }
  }
}
