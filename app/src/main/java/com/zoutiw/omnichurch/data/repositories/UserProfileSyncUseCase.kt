package com.zoutiw.omnichurch.data.repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Cohesive UseCase managing `users/{uid}` Firestore profile synchronization,
 * role promotion protection (never downgrading `admin` / `super_admin`),
 * Deacon (`deacon`) profile sync, and real-time `UserProfile` observation.
 */
internal class UserProfileSyncUseCase(
  private val db: FirebaseFirestore,
  private val firebaseAuth: FirebaseAuth,
) {
  suspend fun syncAdminUserProfileForChurch(
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
        // Respect custom or explicitly cleared ("") adminGooglePhoto and existing user photoUrl.
        val resolvedPhotoForSync: String? =
          when {
            church.adminGooglePhoto != null -> church.adminGooglePhoto
            existingSnap.contains("photoUrl") -> null
            else -> photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          }
        val updateMap =
          hashMapOf<String, Any>(
            "churchId" to church.id.trim(),
            "churchName" to church.name.trim().ifBlank { "OmniChurch Pro" },
            "role" to targetRole,
            "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
            "updatedAt" to FieldValue.serverTimestamp(),
          ).apply {
            if (resolvedPhotoForSync != null) {
              put("photoUrl", resolvedPhotoForSync)
            }
          }
        userRef.update(updateMap).await()
      } else {
        val initialPhoto =
          if (church.adminGooglePhoto != null) {
            church.adminGooglePhoto
          } else {
            photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          }
        userRef
          .set(
            filterNonNullMap(
              "userId" to uid,
              "email" to email.trim().ifBlank { "admin@omnichurch.org" },
              "displayName" to displayName.trim().ifBlank { "Admin OmniChurch" },
              "photoUrl" to initialPhoto,
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

  /**
   * Automatically ensures/syncs `users/{uid}` with `role: "deacon"`, `churchId`, and `memberId`
   * when an active Deacon (Dyak / Dyakonès) signs in with Firebase Auth.
   * NEVER downgrades an existing `admin` or `super_admin` user profile.
   */
  suspend fun syncDeaconUserProfile(member: Member, church: Church? = null): Result<Unit> {
    val currentUser = firebaseAuth.currentUser ?: return Result.success(Unit)
    val uid = currentUser.uid.trim()
    if (uid.isEmpty()) return Result.success(Unit)
    if (!OmniRoleEvaluator.isActiveDeacon(member)) return Result.success(Unit)

    val cleanChurchId = (church?.id?.trim()?.takeIf { it.isNotEmpty() } ?: member.churchId.trim())
    if (cleanChurchId.isEmpty() || member.id.isBlank()) return Result.success(Unit)
    val cleanChurchName =
      (church?.name?.trim()?.takeIf { it.isNotEmpty() } ?: member.churchName.trim()).ifBlank {
        "OmniChurch Pro"
      }
    val userRef = db.collection("users").document(uid)

    return try {
      val existingSnap = userRef.get().await()
      val emailLower = currentUser.email?.trim()?.lowercase(Locale.US).orEmpty()
      val isSuperAdminEmail = emailLower == OmniAuthEvaluator.SUPER_ADMIN_EMAIL.lowercase(Locale.US)

      if (existingSnap.exists()) {
        val existingRole = existingSnap.getString("role")?.trim()?.lowercase(Locale.US).orEmpty()
        if (existingRole == "admin" || existingRole == "super_admin" || isSuperAdminEmail) {
          return Result.success(Unit)
        }
        val alreadySynced =
          existingRole == "deacon" &&
            existingSnap.getString("churchId")?.trim() == cleanChurchId &&
            existingSnap.getString("memberId")?.trim() == member.id.trim()
        if (alreadySynced) return Result.success(Unit)

        userRef
          .update(
            filterNonNullMap(
              "role" to "deacon",
              "churchId" to cleanChurchId,
              "churchName" to cleanChurchName,
              "memberId" to member.id.trim(),
              "displayName" to member.fullName.trim().ifBlank { currentUser.displayName ?: "Dyak" },
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      } else {
        val initialRole = if (isSuperAdminEmail) "super_admin" else "deacon"
        val resolvedPhoto =
          member.effectivePhotoUrl?.takeIf { it.isNotBlank() }
            ?: if (member.avatarUrl == null && member.photoUrl == null && member.googlePhoto == null) {
              currentUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }
            } else {
              null
            }
        userRef
          .set(
            filterNonNullMap(
              "userId" to uid,
              "email" to
                (emailLower.ifBlank { member.email?.trim().orEmpty() }.ifBlank { "deacon@omnichurch.org" }),
              "displayName" to member.fullName.trim().ifBlank { currentUser.displayName ?: "Dyak" },
              "photoUrl" to resolvedPhoto,
              "churchId" to cleanChurchId,
              "churchName" to cleanChurchName,
              "memberId" to member.id.trim(),
              "role" to initialRole,
              "language" to "ht",
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, userRef.path)
      Log.w(TAG, "Deacon profile sync deferred: ${e.message}")
      Result.failure(e)
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
          if (snapshot.exists()) snapshot.toUserProfile() else null
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
        docRef.get().await().toUserProfile()
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

  companion object {
    private const val TAG = "UserProfileSyncUseCase"
  }
}
