package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
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

class ServiceRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  private val auth = Firebase.auth

  private fun resolveActorId(preferredActorId: String? = null): String {
    val cleanPreferred = (preferredActorId ?: "").trim()
    if (cleanPreferred.isNotEmpty()) return cleanPreferred
    return auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
  }

  fun observeServices(churchId: String): Flow<List<ChurchService>> = flow {
    val path = "services"
    emitAll(
      db
        .collection("services")
        .whereEqualTo("churchId", churchId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toChurchService() }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  fun observeActiveAttendanceServices(churchId: String): Flow<List<ChurchService>> = flow {
    val path = "services"
    emitAll(
      db
        .collection("services")
        .whereEqualTo("churchId", churchId)
        .whereEqualTo("isOpenForAttendance", true)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toChurchService() }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  suspend fun createService(
    churchId: String,
    churchName: String,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    authorName: String,
    actorId: String? = null,
    category: String = "dimanche_matin",
    date: String = "",
    startTime: String = "",
    endTime: String = "",
    theme: String? = null,
    passage: String? = null,
    leader: String? = null,
    preacher: String? = null,
    reader: String? = null,
    musicians: String? = null,
    ushers: String? = null,
    sendReminder24h: Boolean = false,
  ): Result<String> {
    val uid = resolveActorId(actorId)
    val docRef = db.collection("services").document()
    val cleanDate = date.trim().ifBlank {
      val parsed = dateFormatted.trim().substringBefore("•").trim()
      parsed.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    }
    val payload =
      filterNonNullMap(
        "churchId" to churchId,
        "churchName" to churchName,
        "category" to category.trim().ifBlank { "dimanche_matin" },
        "title" to title.trim(),
        "name" to title.trim(),
        "description" to description?.trim()?.takeIf { it.isNotBlank() },
        "dateFormatted" to dateFormatted.trim(),
        "date" to cleanDate,
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() },
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() },
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "sendReminder24h" to sendReminder24h,
        "type" to category.trim().ifBlank { "dimanche_matin" },
        "status" to "active",
        "startTimeEpochMillis" to System.currentTimeMillis(),
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 240L),
        "isOpenForAttendance" to isOpenForAttendance,
        "isLive" to isLive,
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
      val notifPayload =
        filterNonNullMap(
          "userId" to "all",
          "churchId" to churchId,
          "title" to "Nouvo Sèvis Pibliye ⛪",
          "message" to "${title.trim()} • ${dateFormatted.trim()}",
          "type" to "service",
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

  suspend fun toggleServiceAttendanceStatus(
    service: ChurchService,
    isOpen: Boolean,
  ): Result<Unit> {
    val docRef = db.collection("services").document(service.id)
    return try {
      docRef
        .update(
          mapOf(
            "isOpenForAttendance" to isOpen,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun updateService(
    serviceId: String,
    title: String,
    description: String?,
    dateFormatted: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    category: String,
    date: String,
    startTime: String,
    endTime: String,
    theme: String?,
    passage: String?,
    leader: String?,
    preacher: String?,
    reader: String?,
    musicians: String?,
    ushers: String?,
  ): Result<Unit> {
    if (serviceId.isBlank()) return Result.failure(IllegalArgumentException("ID sèvis la vid."))
    val docRef = db.collection("services").document(serviceId)
    val payload =
      filterNonNullMap(
        "title" to title.trim(),
        "name" to title.trim(),
        "description" to description?.trim()?.takeIf { it.isNotBlank() },
        "dateFormatted" to dateFormatted.trim(),
        "date" to date.trim(),
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() },
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() },
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "category" to category.trim(),
        "type" to category.trim(),
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 240L),
        "isOpenForAttendance" to isOpenForAttendance,
        "isLive" to isLive,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
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

  suspend fun deleteService(serviceId: String): Result<Unit> {
    if (serviceId.isBlank()) {
      return Result.failure(IllegalArgumentException("ID sèvis la vid."))
    }
    val docRef = db.collection("services").document(serviceId)
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
}
