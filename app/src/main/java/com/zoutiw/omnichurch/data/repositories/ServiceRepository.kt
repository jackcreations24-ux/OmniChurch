package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.filterNonNullMap
import com.zoutiw.omnichurch.data.models.formatServiceClockAmPm
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
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
  private val scheduleAndPayloadUseCase = ServiceScheduleAndPayloadUseCase()

  /**
   * In-flight create guard keyed by `serviceId` to prevent `update()` racing ahead of `set()`
   * and triggering PERMISSION_DENIED on non-existent documents.
   */
  private val inFlightCreates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

  private suspend fun awaitInFlightCreate(serviceId: String) {
    val deferred = inFlightCreates[serviceId] ?: return
    runCatching { deferred.await() }
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
          val now = System.currentTimeMillis()
          snapshot.documents
            .mapNotNull { it.toChurchService() }
            .filter { it.isLiveBadgeActive(now) }
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
    imageUrl: String? = null,
  ): Result<String> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "kreye sèvis la")
        .getOrElse { return Result.failure(it) }

    val cleanChurchId = churchId.trim()
    val cleanTitle = title.trim().take(200)
    if (!FirestorePayloadSanitizer.isValidChurchId(cleanChurchId)) {
      return Result.failure(IllegalArgumentException("ID legliz la pa valid pou kreye sèvis la."))
    }
    if (cleanTitle.isEmpty()) {
      return Result.failure(IllegalArgumentException("Tanpri antre tit sèvis la."))
    }

    val cleanDate = date.trim().ifBlank {
      val parsed = dateFormatted.trim().substringBefore("•").trim()
      parsed.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    }
    if (isOpenForAttendance && scheduleAndPayloadUseCase.isAlreadyEndedInPast(cleanDate, startTime, endTime)) {
      return Result.failure(
        IllegalArgumentException("Ou pa ka kreye yon sèvis ki deja fini nan pase a ak prezans ouvè.")
      )
    }
    val beforeStart = scheduleAndPayloadUseCase.isBeforeScheduledStart(cleanDate, startTime)
    if (isOpenForAttendance && beforeStart) {
      val clockStr = formatServiceClockAmPm(startTime).ifBlank { startTime }
      return Result.failure(
        IllegalArgumentException("Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr).")
      )
    }

    val uid = activeUser.uid.take(128)
    val docRef = db.collection("services").document()
    val (sanitizedPayload, cleanDateFormatted) =
      scheduleAndPayloadUseCase.buildSanitizedCreatePayload(
        docId = docRef.id,
        uid = uid,
        churchId = cleanChurchId,
        churchName = churchName,
        title = cleanTitle,
        description = description,
        dateFormatted = dateFormatted,
        lateToleranceMinutes = lateToleranceMinutes,
        isOpenForAttendance = isOpenForAttendance,
        isLive = isLive,
        authorName = authorName,
        category = category,
        cleanDate = cleanDate,
        startTime = startTime,
        endTime = endTime,
        theme = theme,
        passage = passage,
        leader = leader,
        preacher = preacher,
        reader = reader,
        musicians = musicians,
        ushers = ushers,
        sendReminder24h = sendReminder24h,
        imageUrl = imageUrl,
      )

    val createGuard = CompletableDeferred<Unit>()
    inFlightCreates[docRef.id] = createGuard
    return try {
      docRef.set(sanitizedPayload).await()
      val notifRef = db.collection("notifications").document()
      val notifPayload =
        filterNonNullMap(
          "userId" to "all",
          "churchId" to cleanChurchId,
          "title" to "Nouvo Sèvis Pibliye ⛪",
          "message" to "$cleanTitle • $cleanDateFormatted".take(2000),
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
    } finally {
      createGuard.complete(Unit)
      inFlightCreates.remove(docRef.id, createGuard)
    }
  }

  suspend fun toggleServiceAttendanceStatus(
    service: ChurchService,
    isOpen: Boolean,
  ): Result<Unit> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "chanje estati prezans sèvis la")
        .getOrElse { return Result.failure(it) }

    val cleanId = service.id.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanId)) {
      return Result.failure(IllegalArgumentException("ID sèvis la pa valid."))
    }

    awaitInFlightCreate(cleanId)

    val docRef = db.collection("services").document(cleanId)
    val liveService = runCatching { docRef.get().await().toChurchService() }.getOrNull() ?: service
    scheduleAndPayloadUseCase.validateAttendanceToggle(liveService, isOpen)
      .onFailure { return Result.failure(it) }

    val rawUpdates =
      hashMapOf<String, Any?>(
        "isOpenForAttendance" to isOpen,
        "isLive" to isOpen,
        "updatedBy" to activeUser.uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (isOpen || liveService.wasOpenedForAttendance || liveService.isOpenForAttendance) {
      rawUpdates["wasOpenedForAttendance"] = true
    }

    val sanitized =
      FirestorePayloadSanitizer.sanitizeServiceUpdate(
        raw = rawUpdates,
        isPermanentlyEnded = false,
      )
    return try {
      docRef.update(sanitized).await()
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
    sendReminder24h: Boolean? = null,
    imageUrl: String? = null,
    notes: String? = null,
    reminderSentAt: String? = null,
  ): Result<Unit> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "modifye sèvis la")
        .getOrElse { return Result.failure(it) }

    val cleanId = serviceId.trim()
    val cleanTitle = title.trim().take(200)
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanId)) {
      return Result.failure(IllegalArgumentException("ID sèvis la pa valid."))
    }
    if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("Tit sèvis la vid."))

    awaitInFlightCreate(cleanId)

    val docRef = db.collection("services").document(cleanId)
    val liveService = runCatching { docRef.get().await().toChurchService() }.getOrNull()
    val permanentlyEnded = liveService?.isPermanentlyEnded == true

    if (permanentlyEnded) {
      if (isOpenForAttendance) {
        return Result.failure(
          IllegalStateException("Sèvis sa a deja fini nèt — li pa ka re-ouvri pou prezans.")
        )
      }
      val endedRaw =
        hashMapOf<String, Any?>(
          "notes" to notes?.trim(),
          "sendReminder24h" to sendReminder24h,
          "reminderSentAt" to reminderSentAt?.trim()?.takeIf { it.isNotBlank() },
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      val endedSanitized =
        FirestorePayloadSanitizer.sanitizeServiceUpdate(
          raw = endedRaw,
          isPermanentlyEnded = true,
        )
      if (endedSanitized.keys == setOf("updatedAt")) {
        return Result.failure(
          IllegalStateException(
            "Sèvis sa a deja fini nèt — tit ak orè li pa ka modifye ankò (sèlman nòt oswa rapèl)."
          )
        )
      }
      return try {
        docRef.update(endedSanitized).await()
        Result.success(Unit)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, docRef.path)
        Result.failure(e)
      }
    }

    val cleanDate = date.trim().ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    if (isOpenForAttendance && scheduleAndPayloadUseCase.isBeforeScheduledStart(cleanDate, startTime)) {
      val clockStr = formatServiceClockAmPm(startTime).ifBlank { startTime }
      return Result.failure(
        IllegalStateException("Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr).")
      )
    }

    val sanitized =
      scheduleAndPayloadUseCase.buildSanitizedUpdatePayload(
        uid = activeUser.uid,
        cleanTitle = cleanTitle,
        description = description,
        dateFormatted = dateFormatted,
        lateToleranceMinutes = lateToleranceMinutes,
        isOpenForAttendance = isOpenForAttendance,
        isLive = isLive,
        category = category,
        cleanDate = cleanDate,
        startTime = startTime,
        endTime = endTime,
        theme = theme,
        passage = passage,
        leader = leader,
        preacher = preacher,
        reader = reader,
        musicians = musicians,
        ushers = ushers,
        sendReminder24h = sendReminder24h,
        imageUrl = imageUrl,
        notes = notes,
        reminderSentAt = reminderSentAt,
      )
    return try {
      docRef.update(sanitized).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteService(serviceId: String, requireDeaconOwnership: Boolean = false): Result<Unit> {
    val activeUser =
      FirestorePayloadSanitizer.requireActiveAdminAuth(auth, "efase sèvis la")
        .getOrElse { return Result.failure(it) }

    val cleanId = serviceId.trim()
    if (!FirestorePayloadSanitizer.isValidDocumentId(cleanId)) {
      return Result.failure(IllegalArgumentException("ID sèvis la pa valid."))
    }

    awaitInFlightCreate(cleanId)

    val docRef = db.collection("services").document(cleanId)
    if (requireDeaconOwnership) {
      val liveService = runCatching { docRef.get().await().toChurchService() }.getOrNull()
      if (liveService != null) {
        if (liveService.isPermanentlyLocked()) {
          return Result.failure(
            IllegalStateException("Ou pa ka efase yon sèvis ki deja fini nèt.")
          )
        }
        val uid = activeUser.uid.trim()
        if (liveService.authorId.trim() != uid && liveService.createdBy.trim() != uid) {
          return Result.failure(
            IllegalStateException("Ou kapab efase sèlman sèvis ou te kreye tèt ou yo.")
          )
        }
      }
    }
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
