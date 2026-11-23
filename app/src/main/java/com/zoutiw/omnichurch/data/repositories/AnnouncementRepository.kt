package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.filterNonNullMap
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
    if (cleanPreferred.isNotEmpty()) return cleanPreferred.take(128)
    return auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() }?.take(128)
      ?: "matrikil_pin_session"
  }

  fun observeAnnouncementsResult(churchId: String): Flow<Result<List<Announcement>>> = flow {
    val cleanChurchId = churchId.trim()
    if (cleanChurchId.isEmpty()) {
      emit(Result.success(emptyList()))
      return@flow
    }
    val path = "announcements"
    emitAll(
      db.collection("announcements")
        .whereEqualTo("churchId", cleanChurchId)
        .snapshots()
        .map { snapshot ->
          val sorted =
            snapshot.documents
              .mapNotNull { it.toAnnouncement() }
              .sortedWith(
                compareByDescending<Announcement> { it.isPinned }
                  .thenByDescending { it.sortTimestampMillis }
                  .thenByDescending { it.id }
              )
          Result.success(sorted)
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(Result.failure(error))
        }
    )
  }

  fun observeAnnouncements(churchId: String): Flow<List<Announcement>> =
    observeAnnouncementsResult(churchId).map { result -> result.getOrElse { emptyList() } }

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
    val adminUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "pibliye anons la")
        .getOrElse { return Result.failure(it) }

    val cleanChurchId = churchId.trim()
    val cleanTitle = title.trim().take(300)
    val cleanContent = content.trim().take(5000)
    if (!FirestorePayloadSanitizer.isValidChurchId(cleanChurchId)) {
      return Result.failure(IllegalArgumentException("ID legliz la pa valid pou pibliye anons la."))
    }
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Tanpri antre tit anons la."))
    }
    if (cleanContent.isEmpty()) {
      return Result.failure(IllegalArgumentException("Tanpri antre mesaj anons la."))
    }

    val cleanDepartment = department.trim().ifBlank { "Depatman Administrasyon" }.take(200)
    val cleanAuthorName = authorName.trim().ifBlank { "Admin" }.take(200)
    val cleanAuthorId = adminUser.uid.take(128)
    val cleanPosterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() && it.length <= 600_000 }
    val cleanServiceId = serviceId?.trim()?.takeIf { FirestorePayloadSanitizer.isValidDocumentId(it) }
    val docRef = db.collection("announcements").document()

    val rawPayload = hashMapOf<String, Any?>(
      "id" to docRef.id,
      "churchId" to cleanChurchId,
      "title" to cleanTitle,
      "content" to cleanContent,
      "department" to cleanDepartment,
      "isPinned" to isPinned,
      "authorName" to cleanAuthorName,
      "authorId" to cleanAuthorId,
      "createdBy" to cleanAuthorId,
      "updatedBy" to cleanAuthorId,
      "category" to category.trim().ifBlank { "anons" },
      "date" to date?.trim()?.takeIf { it.isNotBlank() },
      "targetAudience" to targetAudience.trim().ifBlank { "Tout moun" },
      "showToastOnLogin" to showToastOnLogin,
      "toastAlertLevel" to toastAlertLevel.trim().ifBlank { "info" },
      "isScheduledNotification" to isScheduledNotification,
      "scheduledNotificationDate" to scheduledNotificationDate?.trim()?.takeIf { it.isNotBlank() },
      "posterUrl" to cleanPosterUrl,
      "imageUrl" to cleanPosterUrl,
      "serviceId" to cleanServiceId,
      "reactionsCount" to 0L,
      "commentsCount" to 0L,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val payload = FirestorePayloadSanitizer.sanitizeAnnouncementCreate(rawPayload)
    return try {
      docRef.set(payload).await()
      val notifRef = db.collection("notifications").document()
      val notifPayload = filterNonNullMap(
        "userId" to "all",
        "churchId" to cleanChurchId,
        "title" to "Nouvo Anons: $cleanTitle".take(200),
        "message" to cleanContent.take(2000),
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

  suspend fun updateAnnouncement(
    announcementId: String,
    title: String,
    content: String,
    department: String,
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    isPinned: Boolean = false,
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ): Result<Unit> {
    val adminUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "modifye anons la")
        .getOrElse { return Result.failure(it) }

    val cleanId = announcementId.trim()
    val cleanTitle = title.trim().take(300)
    val cleanContent = content.trim().take(5000)
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanId)) {
      return Result.failure(IllegalArgumentException("ID anons la pa valid."))
    }
    if (cleanTitle.isEmpty() || cleanContent.isEmpty()) {
      return Result.failure(IllegalArgumentException("Tit ak mesaj anons la obligatwa."))
    }
    val cleanPosterUrl = posterUrl?.trim()?.takeIf { it.isNotBlank() && it.length <= 600_000 }
    val docRef = db.collection("announcements").document(cleanId)
    val rawPayload = hashMapOf<String, Any?>(
      "title" to cleanTitle,
      "content" to cleanContent,
      "department" to department.trim().ifBlank { "Depatman Administrasyon" }.take(200),
      "category" to category.trim().ifBlank { "anons" },
      "date" to date?.trim()?.takeIf { it.isNotBlank() },
      "targetAudience" to targetAudience.trim().ifBlank { "Tout moun" },
      "isPinned" to isPinned,
      "showToastOnLogin" to showToastOnLogin,
      "toastAlertLevel" to toastAlertLevel.trim().ifBlank { "info" },
      "isScheduledNotification" to isScheduledNotification,
      "scheduledNotificationDate" to scheduledNotificationDate?.trim()?.takeIf { it.isNotBlank() },
      "posterUrl" to cleanPosterUrl,
      "imageUrl" to cleanPosterUrl,
      "serviceId" to serviceId?.trim()?.takeIf { FirestorePayloadSanitizer.isValidDocumentId(it) },
      "updatedBy" to adminUser.uid,
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    val payload = FirestorePayloadSanitizer.sanitizeAnnouncementUpdate(rawPayload)
    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteAnnouncement(announcementId: String): Result<Unit> {
    FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "efase anons la")
      .getOrElse { return Result.failure(it) }

    val cleanId = announcementId.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanId)) return Result.success(Unit)
    return try {
      db.collection("announcements").document(cleanId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
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
    val uid = resolveActorId(actorId)
    val cleanTargetId = targetId.trim()
    val cleanChurchId = churchId.trim()
    if (cleanTargetId.isEmpty() || cleanChurchId.isEmpty()) {
      return Result.failure(IllegalArgumentException("Sib oswa ID legliz la vid."))
    }
    val docId = "${cleanTargetId}_$uid"
    val docRef = db.collection("announcement_reactions").document(docId)
    val payload = filterNonNullMap(
      "targetId" to cleanTargetId,
      "churchId" to cleanChurchId,
      "userId" to uid,
      "userName" to userName.trim().ifBlank { "Manm" }.take(120),
      "reactionType" to reactionType.trim().ifBlank { "pray" },
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeChurchComments(churchId: String): Flow<List<AnnouncementComment>> = flow {
    val cleanChurchId = churchId.trim()
    if (cleanChurchId.isEmpty()) {
      emit(emptyList())
      return@flow
    }
    emitAll(
      db.collection("announcement_comments")
        .whereEqualTo("churchId", cleanChurchId)
        .snapshots()
        .map { snapshot -> snapshot.documents.mapNotNull { it.toAnnouncementComment() } }
        .catch { emit(emptyList()) }
    )
  }

  fun observeComments(churchId: String, targetId: String): Flow<List<AnnouncementComment>> = flow {
    val cleanChurchId = churchId.trim()
    val cleanTargetId = targetId.trim()
    if (cleanTargetId.isEmpty()) {
      emit(emptyList())
      return@flow
    }
    val query =
      if (cleanChurchId.isNotEmpty()) {
        db.collection("announcement_comments").whereEqualTo("churchId", cleanChurchId)
      } else {
        db.collection("announcement_comments").whereEqualTo("targetId", cleanTargetId)
      }
    emitAll(
      query
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toAnnouncementComment() }
            .filter { it.targetId.trim() == cleanTargetId }
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
    val cleanTargetId = targetId.trim()
    val cleanChurchId = churchId.trim()
    val cleanContent = content.trim().take(1000)
    if (cleanTargetId.isEmpty() || cleanChurchId.isEmpty() || cleanContent.isEmpty()) {
      return Result.failure(IllegalArgumentException("Kòmantè a pa konplè."))
    }
    val docRef = db.collection("announcement_comments").document()
    val uid = resolveActorId(actorId)
    val payload = filterNonNullMap(
      "targetId" to cleanTargetId,
      "churchId" to cleanChurchId,
      "userId" to uid,
      "userName" to userName.trim().ifBlank { "Manm" }.take(120),
      "userPhotoUrl" to userPhotoUrl?.trim()?.takeIf { it.isNotBlank() },
      "content" to cleanContent,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    return try {
      docRef.set(payload).await()
      val parentCollection = if (isServiceTarget) "services" else "announcements"
      runCatching {
        db.collection(parentCollection)
          .document(cleanTargetId)
          .update(
            mapOf(
              "commentsCount" to FieldValue.increment(1L),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
