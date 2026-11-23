package com.zoutiw.omnichurch.data.repositories

import android.content.Context
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.OperationType
import com.zoutiw.omnichurch.core.network.handleFirestoreError
import com.zoutiw.omnichurch.data.models.AttendanceErrorType
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniQrParser
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
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

class AttendanceRepository(
  private val db: FirebaseFirestore,
) {
  constructor(context: Context) : this(FirestoreProvider.get(context))

  private val auth = Firebase.auth

  private fun resolveActorId(preferredActorId: String? = null): String {
    val cleanPreferred = (preferredActorId ?: "").trim()
    if (cleanPreferred.isNotEmpty()) return cleanPreferred
    return auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() } ?: "matrikil_pin_session"
  }

  suspend fun validateAndRecordAttendance(
    rawTokenOrCode: String,
    selectedService: ChurchService,
    scannerUserProfile: UserProfile,
    checkInMethod: String,
  ): ScannerValidationOutcome {
    val staffUid =
      auth.currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() }
        ?: resolveActorId(scannerUserProfile.userId.ifBlank { scannerUserProfile.id })

    val isAuthorizedStaff =
      scannerUserProfile.isAuthorizedScanner ||
        scannerUserProfile.role.lowercase() in listOf("admin", "usher", "super_admin", "staff")
    if (!isAuthorizedStaff) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.UNAUTHORIZED)
    }

    val staffChurchId = (scannerUserProfile.churchId ?: "").trim().ifBlank { selectedService.churchId }
    if (
      !FirestorePayloadSanitizer.isValidChurchId(staffChurchId) ||
        !FirestorePayloadSanitizer.isValidDocumentId(selectedService.id) ||
        !selectedService.churchId.trim().equals(staffChurchId, ignoreCase = true)
    ) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.WRONG_CHURCH)
    }

    val servicePath = "services/${selectedService.id}"
    val liveService =
      try {
        db.collection("services").document(selectedService.id).get().await().toChurchService()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, servicePath)
        return ScannerValidationOutcome.ValidationError(
          AttendanceErrorType.NETWORK_ERROR,
          e.localizedMessage,
        )
      }

    if (liveService == null || !liveService.isOpenForAttendance || liveService.isPermanentlyEnded) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.SERVICE_CLOSED)
    }
    if (!liveService.churchId.trim().equals(staffChurchId, ignoreCase = true)) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.WRONG_CHURCH)
    }

    val cleanedInput = rawTokenOrCode.trim()
    if (cleanedInput.isEmpty()) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.INVALID_QR)
    }

    val parsedQr = OmniQrParser.parseQrToken(cleanedInput)
    val targetChurchId = parsedQr.churchId
    val targetMemberIdentifier = parsedQr.memberId

    if (targetChurchId != null && !targetChurchId.equals(staffChurchId, true)) {
      return ScannerValidationOutcome.ValidationError(
        AttendanceErrorType.WRONG_CHURCH,
        "SEKIRITE: Kòd sa a fè pati yon lòt legliz!",
      )
    }

    val targetUpper = targetMemberIdentifier.uppercase()
    val rawUpper = cleanedInput.uppercase()

    val matchedMember: Member? =
      try {
        val byChurchSnap =
          db.collection("members").whereEqualTo("churchId", staffChurchId).get().await()
        val churchMembers = byChurchSnap.documents.mapNotNull { it.toMember() }
        val rawWithoutNfcPrefix = rawUpper.removePrefix("NFC-UID:").trim()
        val targetWithoutNfcPrefix = targetUpper.removePrefix("NFC-UID:").trim()
        churchMembers.firstOrNull { member ->
          val memberNfcUid = member.nfcCardUid.trim().uppercase().removePrefix("NFC-UID:")
          member.effectiveMatrikil.uppercase() == targetUpper ||
            member.id.trim().uppercase() == targetUpper ||
            member.memberCode.trim().uppercase() == targetUpper ||
            member.barcodeToken.trim().uppercase() == targetUpper ||
            member.qrCode.trim().uppercase() == targetUpper ||
            member.barcodeToken.trim().uppercase() == rawUpper ||
            member.qrCode.trim().uppercase() == rawUpper ||
            member.effectiveMatrikil.uppercase() == rawUpper ||
            member.id.trim().uppercase() == rawUpper ||
            (memberNfcUid.isNotEmpty() && (memberNfcUid == rawWithoutNfcPrefix || memberNfcUid == targetWithoutNfcPrefix))
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "members")
        return ScannerValidationOutcome.ValidationError(
          AttendanceErrorType.NETWORK_ERROR,
          e.localizedMessage,
        )
      }

    if (matchedMember == null || matchedMember.effectiveBarcodeToken.isBlank()) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.INVALID_QR)
    }

    if (matchedMember.effectiveStatus.lowercase(Locale.US) in setOf("suspended", "inactive")) {
      return ScannerValidationOutcome.ValidationError(
        AttendanceErrorType.UNAUTHORIZED,
        "Manm sa a pa aktif nan rejis legliz la (${matchedMember.effectiveStatus}).",
      )
    }

    if (!matchedMember.churchId.trim().equals(staffChurchId, ignoreCase = true)) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.WRONG_CHURCH)
    }

    val attendanceDocId = "${liveService.id}_${matchedMember.id}"
    if (!FirestorePayloadSanitizer.isValidDocumentId(attendanceDocId)) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.INVALID_QR)
    }
    val attendanceRef = db.collection("attendances").document(attendanceDocId)

    try {
      val existingAttendance = attendanceRef.get().await()
      if (existingAttendance.exists()) {
        return ScannerValidationOutcome.ValidationError(AttendanceErrorType.DUPLICATE_ATTENDANCE)
      }

      val crossDeviceSnap =
        db.collection("attendances")
          .whereEqualTo("churchId", staffChurchId)
          .whereEqualTo("serviceId", liveService.id)
          .whereEqualTo("memberDocId", matchedMember.id)
          .get()
          .await()
      if (!crossDeviceSnap.isEmpty) {
        return ScannerValidationOutcome.ValidationError(AttendanceErrorType.DUPLICATE_ATTENDANCE)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, attendanceRef.path)
      return ScannerValidationOutcome.ValidationError(
        AttendanceErrorType.NETWORK_ERROR,
        e.localizedMessage,
      )
    }

    val nowMillis = System.currentTimeMillis()
    val deadlineMillis =
      liveService.startTimeEpochMillis + (liveService.lateToleranceMinutes * 60_000L)
    val computedStatus =
      if (liveService.startTimeEpochMillis <= 0L || nowMillis <= deadlineMillis) {
        "on_time"
      } else {
        "late"
      }

    val memberMatrikil = matchedMember.effectiveMatrikil
    val memberUserId = matchedMember.userId.trim().ifEmpty { matchedMember.id }
    val memberToken = matchedMember.barcodeToken.trim().takeIf { it.isNotEmpty() } ?: matchedMember.effectiveBarcodeToken

    val normalizedCheckInMethod = when (checkInMethod.trim().lowercase(Locale.US)) {
      "manual" -> "manual"
      "nfc", "nfc_scan" -> "nfc_scan"
      "qr_scanner" -> "qr_scanner"
      "barcode" -> "barcode"
      "kiosk" -> "kiosk"
      else -> "qr_scan"
    }

    val rawPayload =
      hashMapOf<String, Any?>(
        "churchId" to staffChurchId,
        "serviceId" to liveService.id,
        "serviceName" to liveService.title,
        "memberDocId" to matchedMember.id,
        "memberId" to memberMatrikil,
        "memberCode" to memberMatrikil,
        "memberName" to matchedMember.fullName,
        "memberPhotoUrl" to matchedMember.effectivePhotoUrl,
        "userId" to memberUserId,
        "recordedByUid" to staffUid,
        "status" to computedStatus,
        "checkInMethod" to normalizedCheckInMethod,
        "barcodeToken" to memberToken.takeIf { it.isNotBlank() },
        "attendanceRole" to "member",
        "isVisitor" to false,
        "timestamp" to nowMillis,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    val payload = FirestorePayloadSanitizer.sanitizeRegularAttendanceCreate(rawPayload)

    return try {
      attendanceRef.set(payload).await()

      val statusLabel = if (computedStatus == "on_time") "A Lè" else "An Reta"
      val notifRef = db.collection("notifications").document()
      runCatching {
        notifRef
          .set(
            filterNonNullMap(
              "userId" to memberUserId,
              "churchId" to staffChurchId,
              "title" to "Prezans Anrejistre • ${liveService.title}",
              "message" to "Prezans ou pou ${liveService.title} anrejistre kòm: $statusLabel.",
              "type" to "attendance",
              "isRead" to false,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }

      val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(nowMillis))
      val record =
        AttendanceRecord(
          id = attendanceDocId,
          churchId = staffChurchId,
          serviceId = liveService.id,
          serviceName = liveService.title,
          memberDocId = matchedMember.id,
          memberCode = memberMatrikil,
          memberName = matchedMember.fullName,
          memberPhotoUrl = matchedMember.effectivePhotoUrl,
          userId = memberUserId,
          recordedByUid = staffUid,
          status = computedStatus,
          checkInMethod = normalizedCheckInMethod,
          barcodeToken = memberToken,
          createdAt = Timestamp.now(),
        )
      ScannerValidationOutcome.RegisteredSuccess(
        record = record,
        member = matchedMember,
        service = liveService,
        formattedTime = formattedTime,
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, attendanceRef.path)
      ScannerValidationOutcome.ValidationError(
        AttendanceErrorType.NETWORK_ERROR,
        e.localizedMessage,
      )
    }
  }

  fun observeServiceAttendances(
    churchId: String,
    serviceId: String,
  ): Flow<List<AttendanceRecord>> = flow {
    val cleanChurchId = churchId.trim()
    val cleanServiceId = serviceId.trim()
    if (cleanChurchId.isEmpty() || cleanServiceId.isEmpty()) {
      emit(emptyList())
      return@flow
    }
    val path = "attendances"
    emitAll(
      db
        .collection("attendances")
        .whereEqualTo("churchId", cleanChurchId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toAttendanceRecord() }
            .filter { it.serviceId.trim() == cleanServiceId }
            .sortedByDescending { it.createdAt ?: Timestamp(Date(it.timestamp)) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          emit(emptyList())
        }
    )
  }

  fun observeMemberAttendanceHistory(
    churchId: String,
    memberDocId: String,
  ): Flow<List<com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem>> = flow {
    val path = "attendances"
    emitAll(
      db
        .collection("attendances")
        .whereEqualTo("churchId", churchId)
        .whereEqualTo("memberDocId", memberDocId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents.mapNotNull { doc ->
            val record = doc.toAttendanceRecord() ?: return@mapNotNull null
            val timeMillis = record.timestamp ?: record.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            val dateLabel = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timeMillis))
            val timeLabel = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timeMillis))
            val statusEnum = when (record.status.lowercase()) {
              "late", "an_reta" -> com.zoutiw.omnichurch.data.models.MemberAttendanceStatus.LATE
              "absent", "absan" -> com.zoutiw.omnichurch.data.models.MemberAttendanceStatus.ABSENT
              "excused", "ekskize" -> com.zoutiw.omnichurch.data.models.MemberAttendanceStatus.EXCUSED
              else -> com.zoutiw.omnichurch.data.models.MemberAttendanceStatus.PRESENT
            }
            com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem(
              id = record.id.ifBlank { doc.id },
              dateLabel = dateLabel,
              timeLabel = timeLabel,
              serviceTitle = record.serviceName.ifBlank { "Sèvis Adorasyon" },
              status = statusEnum,
              churchId = record.churchId,
            )
          }.sortedByDescending { it.dateLabel }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }
}
