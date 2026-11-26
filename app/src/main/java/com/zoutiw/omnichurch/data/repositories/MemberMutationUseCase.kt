package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Clean Architecture UseCase encapsulating all member registration, profile updates,
 * profile photo sync, admin member mutations, and NFC card UID binding.
 */
class MemberMutationUseCase(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth,
) {
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
            .update(filterNonNullMap("displayName" to fullName.trim(), "updatedAt" to FieldValue.serverTimestamp()))
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
    val memberRef = db.collection("members").document(cleanMemberId)
    val rawUpdates =
      hashMapOf<String, Any?>(
        "avatarUrl" to cleanPhoto,
        "photoUrl" to cleanPhoto,
        "googlePhoto" to cleanPhoto,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val updates = FirestorePayloadSanitizer.sanitizeMemberUpdate(rawUpdates)
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
                mapOf(
                  "photoUrl" to cleanPhoto,
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          }
        } catch (userErr: Exception) {
          Log.w("MemberMutationUseCase", "Non-fatal: could not sync photoUrl: ${userErr.message}")
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

  suspend fun updateMemberByAdmin(
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String?,
    titlePrefix: String?,
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
}
