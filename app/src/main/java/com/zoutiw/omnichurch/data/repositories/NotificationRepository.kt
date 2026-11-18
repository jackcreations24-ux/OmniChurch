package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NotificationRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  fun observeNotifications(userId: String, churchId: String? = null): Flow<List<ChurchNotification>> = flow {
    val cleanUid = userId.trim()
    val cleanChurchId = churchId?.trim().orEmpty()
    emitAll(
      db.collection("notifications")
        .let { query ->
          if (cleanChurchId.isNotEmpty()) {
            query.whereEqualTo("churchId", cleanChurchId)
          } else {
            query.whereEqualTo("userId", cleanUid)
          }
        }
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { it.toChurchNotification() }
            .sortedByDescending { it.createdAt }
        }
        .catch { emit(emptyList()) }
    )
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> {
    val cleanId = notificationId.trim()
    if (cleanId.isEmpty()) return Result.success(Unit)
    return try {
      db.collection("notifications").document(cleanId).update("isRead", true).await()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
