package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
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
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.filterNonNullMap
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChurchRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  private val auth = Firebase.auth

  fun getChurchesFlow(): Flow<List<Church>> = observeChurches()

  fun observeChurchesResult(): Flow<Result<List<Church>>> = channelFlow {
    val path = "churches"
    var hasEmittedConfirmedOrNonEmpty = false

    val serverWarmupJob = launch {
      try {
        val serverSnap = db.collection("churches").get(Source.SERVER).awaitSafe()
        val serverChurches =
          serverSnap.documents
            .mapNotNull { it.toChurch() }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
        hasEmittedConfirmedOrNonEmpty = true
        trySend(Result.success(serverChurches))
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, path)
        Log.w("ChurchRepository", "Server warm-up fetch for churches failed: ${e.message}")
        val cachedChurches =
          runCatching {
            db.collection("churches")
              .get(Source.CACHE)
              .awaitSafe()
              .documents
              .mapNotNull { it.toChurch() }
              .sortedBy { it.name.lowercase(Locale.getDefault()) }
          }.getOrDefault(emptyList())
        if (cachedChurches.isNotEmpty()) {
          hasEmittedConfirmedOrNonEmpty = true
          trySend(Result.success(cachedChurches))
        } else if (!hasEmittedConfirmedOrNonEmpty) {
          trySend(Result.failure(e))
        }
      }
    }

    var attempt = 0
    try {
      while (currentCoroutineContext().isActive) {
        try {
          db.collection("churches")
            .snapshots()
            .collect { snapshot ->
              val mapped =
                snapshot.documents
                  .mapNotNull { it.toChurch() }
                  .sortedBy { it.name.lowercase(Locale.getDefault()) }
              // Avoid replacing Loading with an empty local offline cache before server responds.
              if (snapshot.metadata.isFromCache && mapped.isEmpty() && !hasEmittedConfirmedOrNonEmpty) {
                return@collect
              }
              attempt = 0
              hasEmittedConfirmedOrNonEmpty = true
              trySend(Result.success(mapped))
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

  fun observeChurches(): Flow<List<Church>> =
    observeChurchesResult().map { it.getOrDefault(emptyList()) }

  suspend fun fetchAllChurches(): List<Church> {
    return try {
      val snap =
        try {
          db.collection("churches").get(Source.SERVER).awaitSafe()
        } catch (e: CancellationException) {
          throw e
        } catch (_: Exception) {
          db.collection("churches").get().awaitSafe()
        }
      snap.documents.mapNotNull { it.toChurch() }.sortedBy { it.name.lowercase(Locale.getDefault()) }
    } catch (e: CancellationException) {
      currentCoroutineContext().ensureActive()
      emptyList()
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, "churches")
      Log.e("ChurchRepository", "Error fetching churches: ${e.message}", e)
      emptyList()
    }
  }

  fun observeChurch(churchId: String): Flow<Church?> = flow {
    val cleanId = churchId.trim()
    if (cleanId.isEmpty()) {
      emit(null)
      return@flow
    }
    val path = "churches/$cleanId"
    emitAll(
      db.collection("churches")
        .document(cleanId)
        .snapshots()
        .map { snap -> snap.toChurch() }
        .catch { error ->
          handleFirestoreError(error, OperationType.GET, path)
          emit(null)
        }
    )
  }

  suspend fun setupChurchAndMemberProfile(
    createNewChurch: Boolean,
    churchNameInput: String,
    existingChurchIdInput: String,
    churchAddressInput: String,
    fullNameInput: String,
    departmentInput: String,
    ministryRoleInput: String,
    photoUrl: String?,
    churchCodeInput: String = "",
    adminPasscodeInput: String = "",
    memberPinInput: String = "1234",
    memberPhoneInput: String = "",
  ): Result<Member> {
    val currentUser = auth.currentUser
    val actorUid = currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
    val cleanFullName = fullNameInput.trim().ifBlank { currentUser?.displayName ?: "Manm OmniChurch" }
    val cleanDepartment = departmentInput.trim().ifBlank { "Adorasyon" }
    val cleanRole = ministryRoleInput.trim().ifBlank { if (createNewChurch) "Admin" else "Manm" }
    val cleanPin = memberPinInput.trim().ifBlank { "1234" }

    return try {
      val targetChurch: Church
      if (createNewChurch) {
        val adminUser =
          FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "kreye yon nouvo legliz")
            .getOrElse { return Result.failure(it) }
        val churchRef = db.collection("churches").document()
        if (!FirestorePayloadSanitizer.isValidChurchId(churchRef.id)) {
          return Result.failure(IllegalArgumentException("ID legliz la pa valid."))
        }
        val cleanChurchName = churchNameInput.trim().ifBlank { "OmniChurch Pro" }
        val generatedCode =
          churchCodeInput.trim().uppercase().ifBlank {
            val letters = cleanChurchName.filter { it.isLetterOrDigit() }.take(3).uppercase().ifEmpty { "OC" }
            "$letters${(100..999).random()}"
          }
        val cleanPasscode = adminPasscodeInput.trim()
        val passcodeDigest =
          if (cleanPasscode.isNotEmpty()) {
            OmniAuthEvaluator.computeSaltedAdminPasscodeDigest(churchRef.id, cleanPasscode)
          } else {
            null
          }
        val adminEmail = adminUser.email?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotEmpty() }
        val adminGoogleUid = adminUser.uid.trim()

        val rawChurchPayload =
          hashMapOf<String, Any?>(
            "id" to churchRef.id,
            "name" to cleanChurchName,
            "code" to generatedCode,
            "address" to churchAddressInput.trim(),
            "ownerId" to adminGoogleUid,
            "adminGoogleEmail" to adminEmail,
            "adminGoogleUid" to adminGoogleUid,
            "adminGoogleName" to adminUser.displayName,
            "adminPasscode" to passcodeDigest,
            "adminAuthMethod" to if (adminEmail != null && passcodeDigest != null) "both" else if (adminEmail != null) "google" else "email_password",
            "status" to "active",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        val churchPayload =
          FirestorePayloadSanitizer.sanitizeChurchCreate(
            raw = rawChurchPayload,
            docId = churchRef.id,
            currentUser = adminUser,
          )
        churchRef.set(churchPayload).await()
        targetChurch =
          Church(
            id = churchRef.id,
            code = generatedCode,
            name = cleanChurchName,
            address = churchAddressInput.trim(),
            ownerId = adminGoogleUid,
            adminGoogleEmail = adminEmail.orEmpty(),
            adminGoogleUid = adminGoogleUid,
            adminPasscode = passcodeDigest.orEmpty(),
          )
      } else {
        val cleanExistingId = existingChurchIdInput.trim()
        val churches = fetchAllChurches()
        targetChurch =
          OmniAuthEvaluator.resolveChurchForMemberLogin(churches, cleanExistingId, null)
            ?: return Result.failure(IllegalArgumentException("Legliz \"$cleanExistingId\" pa jwenn nan sistèm nan."))
      }

      val churchPrefix = targetChurch.effectiveCode.uppercase()
      val initials =
        cleanFullName
          .split(" ")
          .mapNotNull { it.firstOrNull()?.uppercaseChar() }
          .joinToString("")
          .take(2)
          .ifEmpty { "MN" }
      val matrikil = "$churchPrefix-$initials${(1000..9999).random()}"

      val memberRef = db.collection("members").document()
      val barcodeToken =
        "OMNICHURCH_${targetChurch.id}_${memberRef.id}_${UUID.randomUUID().toString().take(8).uppercase()}"
      val joinDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

      val rawMemberPayload =
        hashMapOf<String, Any?>(
          "id" to memberRef.id,
          "userId" to actorUid,
          "churchId" to targetChurch.id,
          "churchName" to targetChurch.name,
          "fullName" to cleanFullName,
          "memberId" to matrikil,
          "memberCode" to matrikil,
          "phone" to memberPhoneInput.trim().takeIf { it.isNotEmpty() },
          "email" to currentUser?.email?.trim()?.takeIf { it.isNotEmpty() },
          "pin" to cleanPin,
          "securityPin" to cleanPin,
          "googleEmail" to currentUser?.email?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotEmpty() },
          "googleUid" to currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() },
          "photoUrl" to photoUrl?.trim()?.takeIf { it.isNotEmpty() },
          "department" to cleanDepartment,
          "role" to if (createNewChurch) cleanRole else "Manm",
          "status" to "active",
          "barcodeToken" to barcodeToken,
          "joinDateFormatted" to joinDateFormatted,
          "createdBy" to currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() },
          "updatedBy" to currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() },
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val memberPayload =
        FirestorePayloadSanitizer.sanitizeMemberCreate(
          raw = rawMemberPayload,
          docId = memberRef.id,
        )
      memberRef.set(memberPayload).await()

      if (currentUser != null) {
        val userRef = db.collection("users").document(currentUser.uid)
        val userSnap = runCatching { userRef.get().await() }.getOrNull()
        val targetUserRole =
          if (currentUser.email?.trim().equals(OmniAuthEvaluator.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
            "super_admin"
          } else if (createNewChurch) {
            "admin"
          } else {
            "member"
          }
        if (userSnap != null && userSnap.exists()) {
          runCatching {
            userRef.update(
              filterNonNullMap(
                "churchId" to targetChurch.id,
                "churchName" to targetChurch.name,
                "memberId" to memberRef.id,
                "role" to targetUserRole,
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            ).await()
          }
        }
      }

      val createdMember =
        runCatching { memberRef.get().await().toMember() }.getOrNull()
          ?: Member(
            id = memberRef.id,
            memberId = matrikil,
            userId = actorUid,
            churchId = targetChurch.id,
            churchName = targetChurch.name,
            fullName = cleanFullName,
            memberCode = matrikil,
            phone = memberPhoneInput.trim().takeIf { it.isNotEmpty() },
            pin = cleanPin,
            securityPin = cleanPin,
            department = cleanDepartment,
            role = cleanRole,
            status = "active",
            barcodeToken = barcodeToken,
            joinDateFormatted = joinDateFormatted,
          )
      Result.success(createdMember)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "churches")
      Result.failure(e)
    }
  }
}
