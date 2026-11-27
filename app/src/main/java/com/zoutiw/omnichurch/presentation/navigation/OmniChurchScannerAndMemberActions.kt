package com.zoutiw.omnichurch.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.launch

internal fun OmniChurchViewModel.performProcessAttendanceScanOrCode(
  rawTokenOrCode: String,
  selectedService: ChurchService,
  checkInMethod: String,
) {
  val session = sessionState.value
  val profile =
    (userProfileState.value as? UiState.Success)?.data
      ?: run {
        if (!session.isAuthenticated) return
        UserProfile(
          userId = effectiveActorUid().ifBlank { "matrikil_pin_session" },
          role = session.activeRole.ifBlank { "admin" },
          churchId = session.activeChurchId.ifBlank { selectedService.churchId },
          displayName = "Admin Scanner",
        )
      }
  if (_scannerOutcome.value is ScannerValidationOutcome.Processing) return
  val nowMillis = System.currentTimeMillis()
  if (selectedService.isBeforeStart(nowMillis)) {
    val startLabel = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(selectedService.startTime)
    _scannerOutcome.value =
      ScannerValidationOutcome.ValidationError(
        com.zoutiw.omnichurch.data.models.AttendanceErrorType.SERVICE_CLOSED,
        if (startLabel.isNotBlank()) {
          "Sèvis sa a poko kòmanse (kòmanse a $startLabel). Eskane prezans bloke anvan lè a."
        } else {
          "Sèvis sa a poko kòmanse. Eskane prezans bloke anvan lè a."
        },
      )
    return
  }
  if (selectedService.isPermanentlyEnded || selectedService.hasPassedEndTime(nowMillis)) {
    _scannerOutcome.value =
      ScannerValidationOutcome.ValidationError(
        com.zoutiw.omnichurch.data.models.AttendanceErrorType.SERVICE_CLOSED,
        "Sèvis sa a deja fini. Eskane prezans fèmen.",
      )
    return
  }
  _scannerOutcome.value = ScannerValidationOutcome.Processing
  viewModelScope.launch {
    val outcome = repository.validateAndRecordAttendance(
      rawTokenOrCode = rawTokenOrCode,
      selectedService = selectedService,
      scannerUserProfile = profile,
      checkInMethod = checkInMethod,
    )
    _scannerOutcome.value = outcome
  }
}

internal fun OmniChurchViewModel.performVerifyMemberCard(
  rawTokenOrCode: String,
  fallbackChurchId: String?,
) {
  val cleanQuery = rawTokenOrCode.trim()
  if (cleanQuery.isEmpty()) return
  val effectiveFallbackChurch =
    fallbackChurchId?.trim()?.takeIf { it.isNotEmpty() }
      ?: activeChurchState.value?.id?.trim()?.takeIf { it.isNotEmpty() }
      ?: sessionState.value.activeChurchId.trim().takeIf { it.isNotEmpty() }
  lastCardVerificationQuery = cleanQuery to effectiveFallbackChurch
  _cardVerificationState.value = UiState.Loading
  viewModelScope.launch {
    repository
      .resolveMemberCardVerification(cleanQuery, effectiveFallbackChurch)
      .onSuccess { resolution ->
        _cardVerificationState.value = UiState.Success(resolution)
      }
      .onFailure { err ->
        _cardVerificationState.value =
          UiState.Error(
            err.localizedMessage
              ?: "Kat sa a pa jwenn nan rejis ofisyèl la. Verifye kòd QR, kat NFC, oswa Matrikil la."
          )
      }
  }
}

internal fun OmniChurchViewModel.performOpenDirectMemberVerification(
  member: Member,
  church: Church?,
) {
  val resolvedChurch =
    church?.takeIf { it.id.isNotBlank() }
      ?: activeChurchState.value?.takeIf {
        it.id.equals(member.churchId, ignoreCase = true) ||
          it.code.equals(member.churchId, ignoreCase = true)
      }
      ?: churchesState.value.firstOrNull {
        it.id.equals(member.churchId, ignoreCase = true) ||
          it.code.equals(member.churchId, ignoreCase = true)
      }
  if (resolvedChurch != null) {
    lastCardVerificationQuery = member.effectiveMatrikil to resolvedChurch.id
    _cardVerificationState.value =
      UiState.Success(PublicVerificationResolution(church = resolvedChurch, member = member))
  } else {
    verifyMemberCard(
      rawTokenOrCode = member.effectiveMatrikil.ifBlank { member.id },
      fallbackChurchId = member.churchId,
    )
  }
}

internal fun OmniChurchViewModel.performBindNfcCardToMember(
  memberDocId: String,
  nfcCardUid: String,
  languageCode: String = "ht",
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val copy = com.zoutiw.omnichurch.presentation.screens.scanner.ScannerStrings.forLanguage(languageCode)
  viewModelScope.launch {
    repository.bindNfcCardUidToMember(memberDocId, nfcCardUid)
      .onSuccess {
        _actionFeedbackMessage.value = copy.nfcBindSuccessMsg
        onSuccess()
      }
      .onFailure { err ->
        val msg = err.localizedMessage ?: copy.nfcBindErrorMsg
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performMarkNotificationAsRead(notificationId: String) {
  viewModelScope.launch {
    repository.markNotificationRead(notificationId)
  }
}

internal fun OmniChurchViewModel.performMarkAllNotificationsRead(
  notifications: List<ChurchNotification>,
) {
  viewModelScope.launch {
    notifications.filter { !it.isRead }.forEach { notif ->
      repository.markNotificationRead(notif.id)
    }
  }
}

internal fun OmniChurchViewModel.performUpdateMemberProfileInfo(
  fullName: String,
  department: String,
  ministryRole: String,
  onSuccess: () -> Unit,
) {
  val member = (currentMemberState.value as? UiState.Success)?.data ?: return
  viewModelScope.launch {
    repository.updateMemberProfile(member, fullName, department, ministryRole).onSuccess {
      _actionFeedbackMessage.value = "Profil la ajou avèk siksè."
      onSuccess()
    }
  }
}

internal fun OmniChurchViewModel.performUpdateChurchMemberByAdmin(
  memberDocId: String,
  fullName: String,
  department: String,
  ministryRole: String,
  phone: String?,
  titlePrefix: String?,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  viewModelScope.launch {
    repository.updateMemberByAdmin(memberDocId, fullName, department, ministryRole, phone, titlePrefix)
      .onSuccess {
        _actionFeedbackMessage.value = "Profil manm lan ajou avèk siksè."
        onSuccess()
      }
      .onFailure { err ->
        val msg = err.localizedMessage ?: "Erè ajou manm."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performUpdateMemberPhoto(
  photoDataUrl: String,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val member = (currentMemberState.value as? UiState.Success)?.data ?: return
  viewModelScope.launch {
    repository.updateMemberProfilePhoto(member, photoDataUrl)
      .onSuccess { onSuccess() }
      .onFailure { err -> onError(err.localizedMessage ?: "Erè foto pwofil.") }
  }
}

internal fun OmniChurchViewModel.performUpdateAdminPhoto(
  photoDataUrl: String,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val churchId = resolveCanonicalChurchId("")
  if (churchId.isBlank()) {
    onError("Pa ka jwenn legliz aktif la.")
    return
  }
  viewModelScope.launch {
    repository.updateChurchAdminPhoto(churchId, photoDataUrl)
      .onSuccess { onSuccess() }
      .onFailure { err -> onError(err.localizedMessage ?: "Erè pandan mizajou foto administratè a.") }
  }
}

internal fun OmniChurchViewModel.performUpdateChurchLogo(
  logoDataUrl: String,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val churchId = resolveCanonicalChurchId("")
  if (churchId.isBlank()) {
    onError("Pa ka jwenn legliz aktif la.")
    return
  }
  viewModelScope.launch {
    repository.updateChurchLogo(churchId, logoDataUrl)
      .onSuccess { onSuccess() }
      .onFailure { err -> onError(err.localizedMessage ?: "Erè pandan mizajou logo legliz la.") }
  }
}

internal fun OmniChurchViewModel.performUpdateChurchLocation(
  rawCity: String,
  languageCode: String = "ht",
  onSuccess: (String) -> Unit,
  onError: (String) -> Unit,
) {
  val churchId = resolveCanonicalChurchId("")
  if (churchId.isBlank()) {
    onError("Pa ka jwenn legliz aktif la.")
    return
  }
  viewModelScope.launch {
    repository.updateChurchLocation(churchId, rawCity, languageCode)
      .onSuccess { formatted -> onSuccess(formatted) }
      .onFailure { err -> onError(err.localizedMessage ?: "Erè pandan mizajou peyi / vil legliz la.") }
  }
}
