package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
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
    val cleanUserId = userId.trim()
    val cleanChurchId = churchId?.trim().orEmpty()
    val path = "notifications"
    emitAll(
      db
        .collection("notifications")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              ChurchNotification::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .filter { notif ->
              val matchesUser = cleanUserId.isNotBlank() && notif.userId.equals(cleanUserId, ignoreCase = true)
              val matchesChurch = cleanChurchId.isNotBlank() && notif.churchId.equals(cleanChurchId, ignoreCase = true)
              val isGlobalChurchNotif = notif.userId.isBlank() && notif.churchId.isNotBlank() && (cleanChurchId.isBlank() || notif.churchId.equals(cleanChurchId, ignoreCase = true))
              matchesUser || matchesChurch || isGlobalChurchNotif
            }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  suspend fun markNotificationRead(notificationId: String): Result<Unit> {
    val docRef = db.collection("notifications").document(notificationId)
    return try {
      docRef.update("isRead", true).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }
}
