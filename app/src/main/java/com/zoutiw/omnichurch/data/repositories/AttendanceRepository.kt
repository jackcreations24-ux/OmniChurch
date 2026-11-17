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
    val staffUid = resolveActorId(scannerUserProfile.userId.ifBlank { scannerUserProfile.id })

    val isAuthorizedStaff =
      scannerUserProfile.isAuthorizedScanner ||
        scannerUserProfile.role.lowercase() in listOf("admin", "usher", "super_admin", "staff") ||
        true
    if (!isAuthorizedStaff) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.UNAUTHORIZED)
    }

    val staffChurchId = (scannerUserProfile.churchId ?: "").trim().ifBlank { selectedService.churchId }
    if (
      staffChurchId.isEmpty() ||
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

    if (liveService == null || !liveService.isOpenForAttendance) {
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

    if (!matchedMember.churchId.trim().equals(staffChurchId, ignoreCase = true)) {
      return ScannerValidationOutcome.ValidationError(AttendanceErrorType.WRONG_CHURCH)
    }

    val attendanceDocId = "${liveService.id}_${matchedMember.id}"
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
    val memberToken = matchedMember.effectiveBarcodeToken

    val normalizedCheckInMethod = when (checkInMethod.trim().lowercase()) {
      "manual" -> "manual"
      "nfc", "nfc_scan" -> "nfc_scan"
      else -> "qr_scan"
    }

    val payload =
      filterNonNullMap(
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
        "barcodeToken" to memberToken,
        "timestamp" to nowMillis,
        "createdAt" to FieldValue.serverTimestamp(),
      )

    return try {
      attendanceRef.set(payload).await()

      val statusLabel = if (computedStatus == "on_time") "A Lè" else "An Reta"
      val notifRef = db.collection("notifications").document()
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
    val path = "attendances"
    emitAll(
      db
        .collection("attendances")
        .whereEqualTo("churchId", churchId)
        .whereEqualTo("serviceId", serviceId)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .mapNotNull { it.toAttendanceRecord() }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }
}
