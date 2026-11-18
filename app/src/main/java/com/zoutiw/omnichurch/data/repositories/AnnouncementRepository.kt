package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AnnouncementRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  fun observeAnnouncements(churchId: String): Flow<List<Announcement>> = flow {
    val cleanChurchId = churchId.trim()
    val path = "announcements"
    emitAll(
      db.collection("announcements")
        .whereEqualTo("churchId", cleanChurchId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { it.toAnnouncement() }
            .sortedByDescending { it.createdAt }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  suspend fun createAnnouncement(
    churchId: String,
    title: String,
    content: String,
    department: String,
    isPinned: Boolean,
    authorName: String,
    actorId: String? = null,
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ): Result<String> {
    val docRef = db.collection("announcements").document()
    val payload = filterNonNullMap(
      "churchId" to churchId.trim(),
      "title" to title.trim(),
      "content" to content.trim(),
      "department" to department.trim(),
      "isPinned" to isPinned,
      "authorName" to authorName.trim(),
      "authorId" to actorId?.trim().orEmpty(),
      "category" to category,
      "date" to date,
      "targetAudience" to targetAudience,
      "showToastOnLogin" to showToastOnLogin,
      "toastAlertLevel" to toastAlertLevel,
      "isScheduledNotification" to isScheduledNotification,
      "scheduledNotificationDate" to scheduledNotificationDate,
      "posterUrl" to posterUrl,
      "serviceId" to serviceId,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, "announcements")
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    val cleanId = announcementId.trim()
    if (cleanId.isEmpty()) return Result.success(Unit)
    return try {
      db.collection("announcements").document(cleanId).delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, "announcements/$cleanId")
      Result.failure(e)
    }
  }

  fun observeReactions(churchId: String): Flow<List<AnnouncementReaction>> = flow {
    val cleanChurchId = churchId.trim()
    emitAll(
      db.collection("announcement_reactions")
        .whereEqualTo("churchId", cleanChurchId)
        .snapshots()
        .map { snapshot -> snapshot.documents.mapNotNull { it.toAnnouncementReaction() } }
        .catch { emit(emptyList()) }
    )
  }

  suspend fun reactToTarget(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    userName: String,
    reactionType: String,
    actorId: String? = null,
    isCommentTarget: Boolean = false,
  ): Result<Unit> {
    val uid = actorId?.trim().orEmpty().ifBlank { "anon_user" }
    val docId = "${targetId}_$uid"
    val docRef = db.collection("announcement_reactions").document(docId)
    val payload = filterNonNullMap(
      "targetId" to targetId,
      "isServiceTarget" to isServiceTarget,
      "isCommentTarget" to isCommentTarget,
      "churchId" to churchId,
      "userId" to uid,
      "userName" to userName,
      "reactionType" to reactionType,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  fun observeComments(churchId: String, targetId: String): Flow<List<AnnouncementComment>> = flow {
    val cleanTargetId = targetId.trim()
    emitAll(
      db.collection("announcement_comments")
        .whereEqualTo("targetId", cleanTargetId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { it.toAnnouncementComment() }
            .sortedBy { it.createdAt }
        }
        .catch { emit(emptyList()) }
    )
  }

  suspend fun addComment(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    userName: String,
    userPhotoUrl: String?,
    content: String,
    actorId: String? = null,
  ): Result<String> {
    val docRef = db.collection("announcement_comments").document()
    val uid = actorId?.trim().orEmpty().ifBlank { "anon_user" }
    val payload = filterNonNullMap(
      "targetId" to targetId,
      "isServiceTarget" to isServiceTarget,
      "churchId" to churchId,
      "userId" to uid,
      "userName" to userName,
      "userPhotoUrl" to userPhotoUrl,
      "content" to content.trim(),
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
