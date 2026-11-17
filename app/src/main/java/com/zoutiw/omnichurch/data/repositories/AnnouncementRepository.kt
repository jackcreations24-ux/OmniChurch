package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
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

  private val auth = Firebase.auth

  private fun resolveActorId(preferredActorId: String? = null): String {
    val cleanPreferred = (preferredActorId ?: "").trim()
    if (cleanPreferred.isNotEmpty()) return cleanPreferred
    return auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
  }

  fun observeAnnouncements(churchId: String): Flow<List<Announcement>> = flow {
    val path = "announcements"
    emitAll(
      db
        .collection("announcements")
        .whereEqualTo("churchId", churchId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              Announcement::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedWith(
              compareByDescending<Announcement> { it.isPinned }
                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
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
    val uid = resolveActorId(actorId)
    val docRef = db.collection("announcements").document()
    val cleanDate = date?.trim()?.takeIf { it.isNotBlank() }
      ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val payload =
      filterNonNullMap(
        "churchId" to churchId,
        "title" to title.trim(),
        "content" to content.trim(),
        "category" to category.trim().ifBlank { "anons" },
        "department" to department.trim().ifBlank { "Depatman Administrasyon" },
        "date" to cleanDate,
        "targetAudience" to targetAudience.trim().ifBlank { "Tout moun" },
        "isPinned" to isPinned,
        "showToastOnLogin" to showToastOnLogin,
        "toastAlertLevel" to toastAlertLevel.trim().ifBlank { "info" },
        "isScheduledNotification" to isScheduledNotification,
        "scheduledNotificationDate" to scheduledNotificationDate?.trim()?.takeIf { it.isNotBlank() },
        "posterUrl" to posterUrl?.trim()?.takeIf { it.isNotBlank() },
        "imageUrl" to posterUrl?.trim()?.takeIf { it.isNotBlank() },
        "serviceId" to serviceId?.trim()?.takeIf { it.isNotBlank() },
        "authorId" to uid,
        "authorName" to authorName.ifBlank { "Admin" },
        "reactionsCount" to 0L,
        "commentsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      val notifRef = db.collection("notifications").document()
      val summaryMessage = if (content.length > 80) "${content.trim().take(80)}..." else content.trim()
      val notifPayload =
        filterNonNullMap(
          "churchId" to churchId,
          "title" to "Nouvo Anons Ofisyèl 📢",
          "message" to "${title.trim()}: $summaryMessage",
          "type" to "announcement",
          "isRead" to false,
          "createdAt" to FieldValue.serverTimestamp(),
        )
      runCatching { notifRef.set(notifPayload).await() }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    if (announcementId.isBlank()) {
      return Result.failure(IllegalArgumentException("ID anons la vid."))
    }
    val docRef = db.collection("announcements").document(announcementId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeReactions(churchId: String): Flow<List<AnnouncementReaction>> = flow {
    val path = "announcement_reactions"
    emitAll(
      db
        .collection("announcement_reactions")
        .whereEqualTo("churchId", churchId)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AnnouncementReaction::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  suspend fun reactToTarget(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    userName: String,
    reactionType: String = "pray",
    actorId: String? = null,
    isCommentTarget: Boolean = false,
  ): Result<Unit> {
    val uid = resolveActorId(actorId)
    val reactionDocId = "${targetId}_$uid"
    val reactionRef = db.collection("announcement_reactions").document(reactionDocId)
    val parentCollection = if (isServiceTarget) "services" else "announcements"
    val parentRef = db.collection(parentCollection).document(targetId)

    return try {
      val existingReaction = reactionRef.get().await()
      if (!existingReaction.exists()) {
        val payload =
          mapOf(
            "targetId" to targetId,
            "churchId" to churchId,
            "userId" to uid,
            "userName" to userName.ifBlank { "Manm" },
            "reactionType" to reactionType,
            "createdAt" to FieldValue.serverTimestamp(),
          )
        reactionRef.set(payload).await()
        if (!isCommentTarget) {
          runCatching {
            parentRef
              .update(
                mapOf(
                  "reactionsCount" to FieldValue.increment(1L),
                  "updatedAt" to FieldValue.serverTimestamp(),
                )
              )
              .await()
          }
        }
      } else {
        val previousReactionType = existingReaction.getString("reactionType")
        if (previousReactionType != reactionType) {
          reactionRef
            .update(
              mapOf(
                "reactionType" to reactionType,
                "userName" to userName.ifBlank { "Manm" },
              )
            )
            .await()
        }
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, reactionRef.path)
      Result.failure(e)
    }
  }

  fun observeComments(churchId: String, targetId: String): Flow<List<AnnouncementComment>> = flow {
    val path = "announcement_comments"
    val cleanChurchId = churchId.trim()
    val cleanTargetId = targetId.trim()
    emitAll(
      db
        .collection("announcement_comments")
        .whereEqualTo("targetId", cleanTargetId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              AnnouncementComment::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .filter { comment ->
              val cChurchId = comment.churchId.trim()
              cChurchId.isBlank() || cleanChurchId.isBlank() || cChurchId.equals(cleanChurchId, ignoreCase = true)
            }
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
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
    val uid = resolveActorId(actorId)
    val commentRef = db.collection("announcement_comments").document()
    val parentCollection = if (isServiceTarget) "services" else "announcements"
    val parentRef = db.collection(parentCollection).document(targetId)

    val payload =
      filterNonNullMap(
        "targetId" to targetId,
        "churchId" to churchId,
        "userId" to uid,
        "userName" to userName.ifBlank { "Manm" },
        "userPhotoUrl" to userPhotoUrl,
        "content" to content.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    return try {
      commentRef.set(payload).await()
      parentRef
        .update(
          mapOf(
            "commentsCount" to FieldValue.increment(1L),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(commentRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, commentRef.path)
      Result.failure(e)
    }
  }
}
