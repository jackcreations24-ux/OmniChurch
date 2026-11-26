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

  /**
   * In-flight create guard keyed by `serviceId` to prevent `update()` racing ahead of `set()`
   * and triggering PERMISSION_DENIED on non-existent documents.
   */
  private val inFlightCreates = ConcurrentHashMap<String, CompletableDeferred<Unit>>()

  private suspend fun awaitInFlightCreate(serviceId: String) {
    val deferred = inFlightCreates[serviceId] ?: return
    runCatching { deferred.await() }
  }

  private fun parseDateTimeMillis(date: String, time: String): Long? {
    val cleanDate = date.trim()
    if (cleanDate.isEmpty()) return null
    val cleanTime = time.trim().replace("h", ":", ignoreCase = true)
    val candidates =
      if (cleanTime.contains(":")) {
        listOf(
          "yyyy-MM-dd hh:mm a" to "$cleanDate ${cleanTime.uppercase(Locale.US)}",
          "yyyy-MM-dd HH:mm" to "$cleanDate $cleanTime",
        )
      } else {
        listOf("yyyy-MM-dd" to cleanDate)
      }
    for ((pattern, value) in candidates) {
      try {
        val parsed = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)?.time
        if (parsed != null) return parsed
      } catch (_: Exception) {
        // Try next pattern
      }
    }
    return null
  }

  private fun isAlreadyEndedInPast(date: String, startTime: String, endTime: String): Boolean {
    val cleanDate = date.trim()
    if (cleanDate.isEmpty()) return false
    val now = System.currentTimeMillis()
    val endMillis =
      if (endTime.trim().isNotEmpty()) {
        parseDateTimeMillis(cleanDate, endTime)
      } else if (startTime.trim().isNotEmpty()) {
        parseDateTimeMillis(cleanDate, startTime)?.plus(3 * 3_600_000L)
      } else {
        // Date-only: consider ended only if the entire day (23:59) has passed
        parseDateTimeMillis(cleanDate, "23:59")
      }
    return endMillis != null && endMillis < now
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

  private fun computeStartEpochMillis(date: String, startTime: String): Long {
    val parsedMillis = parseDateTimeMillis(date, startTime)
    if (parsedMillis != null && parsedMillis > 0L) return parsedMillis
    return System.currentTimeMillis()
  }

  private fun isBeforeScheduledStart(date: String, startTime: String, now: Long = System.currentTimeMillis()): Boolean {
    val startMillis = parseDateTimeMillis(date, startTime) ?: return false
    return now < startMillis
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
    if (isOpenForAttendance && isAlreadyEndedInPast(cleanDate, startTime, endTime)) {
      return Result.failure(
        IllegalArgumentException(
          "Ou pa ka kreye yon sèvis ki deja fini nan pase a ak prezans ouvè."
        )
      )
    }
    val beforeStart = isBeforeScheduledStart(cleanDate, startTime)
    if (isOpenForAttendance && beforeStart) {
      val clockStr = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(startTime).ifBlank { startTime }
      return Result.failure(
        IllegalArgumentException(
          "Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr)."
        )
      )
    }

    val uid = activeUser.uid.take(128)
    val docRef = db.collection("services").document()
    val cleanChurchName = churchName.trim().ifBlank { "OmniChurch Pro" }.take(200)
    val cleanAuthorName = authorName.trim().ifBlank { "Admin" }.take(200)
    val cleanCategory = category.trim().ifBlank { "dimanche_matin" }
    val cleanDateFormatted =
      dateFormatted.trim().ifBlank {
        val formattedTime = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(startTime)
        if (formattedTime.isNotBlank()) "$cleanDate • $formattedTime" else cleanDate
      }.take(150)
    val cleanImageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() && it.length <= 600_000 }
    val startEpochMillis = computeStartEpochMillis(cleanDate, startTime)
    val effectiveIsOpen = isOpenForAttendance && !beforeStart && !isAlreadyEndedInPast(cleanDate, startTime, endTime)
    val effectiveIsLive = isLive && effectiveIsOpen

    val rawPayload =
      hashMapOf<String, Any?>(
        "id" to docRef.id,
        "churchId" to cleanChurchId,
        "churchName" to cleanChurchName,
        "category" to cleanCategory,
        "title" to cleanTitle,
        "name" to cleanTitle,
        "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(3000),
        "dateFormatted" to cleanDateFormatted,
        "date" to cleanDate,
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "sendReminder24h" to sendReminder24h,
        "reminderSentAt" to if (sendReminder24h) FirestorePayloadSanitizer.isoUtcNow() else null,
        "imageUrl" to cleanImageUrl,
        "type" to cleanCategory,
        "status" to "active",
        "startTimeEpochMillis" to startEpochMillis,
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 360L),
        "isOpenForAttendance" to effectiveIsOpen,
        "wasOpenedForAttendance" to effectiveIsOpen,
        "isPermanentlyEnded" to false,
        "isLive" to effectiveIsLive,
        "authorId" to uid,
        "authorName" to cleanAuthorName,
        "createdBy" to uid,
        "updatedBy" to uid,
        "reactionsCount" to 0L,
        "commentsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    val sanitizedPayload = FirestorePayloadSanitizer.sanitizeServiceCreate(rawPayload)

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
    val now = System.currentTimeMillis()
    if (liveService.isPermanentlyLocked(now)) {
      return Result.failure(
        IllegalStateException(
          "Sèvis sa a deja fini nèt — prezans lan pa ka re-ouvri ankò."
        )
      )
    }
    if (isOpen && liveService.isBeforeStart(now)) {
      val clockStr = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(liveService.startTime).ifBlank { liveService.startTime }
      return Result.failure(
        IllegalStateException(
          "Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr)."
        )
      )
    }
    if (isOpen && liveService.hasPassedEndTime(now)) {
      return Result.failure(
        IllegalStateException(
          "Sèvis sa a deja depase lè fini li — prezans lan pa ka louvri."
        )
      )
    }

    val rawUpdates =
      hashMapOf<String, Any?>(
        "isOpenForAttendance" to isOpen,
        "isLive" to isOpen,
        "updatedBy" to activeUser.uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (isOpen) {
      rawUpdates["wasOpenedForAttendance"] = true
    } else if (liveService.wasOpenedForAttendance || liveService.isOpenForAttendance) {
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
          IllegalStateException(
            "Sèvis sa a deja fini nèt — li pa ka re-ouvri pou prezans."
          )
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

    val cleanCategory = category.trim().ifBlank { "dimanche_matin" }
    val cleanDate = date.trim().ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val beforeStart = isBeforeScheduledStart(cleanDate, startTime)
    if (isOpenForAttendance && beforeStart) {
      val clockStr = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(startTime).ifBlank { startTime }
      return Result.failure(
        IllegalStateException(
          "Prezans pa ka louvri anvan lè sèvis la kòmanse ($clockStr)."
        )
      )
    }
    val endedInPast = isAlreadyEndedInPast(cleanDate, startTime, endTime)
    val effectiveIsOpen = isOpenForAttendance && !beforeStart && !endedInPast
    val effectiveIsLive = isLive && effectiveIsOpen
    val updatedStartEpochMillis = computeStartEpochMillis(cleanDate, startTime)

    val cleanDateFormatted =
      dateFormatted.trim().ifBlank {
        val formattedTime = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(startTime)
        if (formattedTime.isNotBlank()) "$cleanDate • $formattedTime" else cleanDate
      }.take(150)

    val rawPayload =
      hashMapOf<String, Any?>(
        "title" to cleanTitle,
        "name" to cleanTitle,
        "description" to description?.trim()?.takeIf { it.isNotBlank() }?.take(3000),
        "dateFormatted" to cleanDateFormatted,
        "date" to cleanDate,
        "startTime" to startTime.trim().takeIf { it.isNotBlank() },
        "endTime" to endTime.trim().takeIf { it.isNotBlank() },
        "startTimeEpochMillis" to updatedStartEpochMillis,
        "theme" to theme?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "passage" to passage?.trim()?.takeIf { it.isNotBlank() }?.take(500),
        "leader" to leader?.trim()?.takeIf { it.isNotBlank() },
        "preacher" to preacher?.trim()?.takeIf { it.isNotBlank() },
        "reader" to reader?.trim()?.takeIf { it.isNotBlank() },
        "musicians" to musicians?.trim()?.takeIf { it.isNotBlank() },
        "ushers" to ushers?.trim()?.takeIf { it.isNotBlank() },
        "category" to cleanCategory,
        "type" to cleanCategory,
        "lateToleranceMinutes" to lateToleranceMinutes.coerceIn(0L, 360L),
        "isOpenForAttendance" to effectiveIsOpen,
        "isLive" to effectiveIsLive,
        "updatedBy" to activeUser.uid,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    if (effectiveIsOpen) {
      rawPayload["wasOpenedForAttendance"] = true
    }
    if (sendReminder24h != null) {
      rawPayload["sendReminder24h"] = sendReminder24h
    }
    if (reminderSentAt != null) {
      rawPayload["reminderSentAt"] = reminderSentAt.trim().takeIf { it.isNotBlank() }
    }
    if (notes != null) {
      rawPayload["notes"] = notes.trim()
    }
    if (imageUrl != null) {
      rawPayload["imageUrl"] = imageUrl.trim().takeIf { it.isNotBlank() && it.length <= 600_000 }
    }

    val sanitized =
      FirestorePayloadSanitizer.sanitizeServiceUpdate(
        raw = rawPayload,
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
