package com.zoutiw.omnichurch.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.repositories.AuthRepository
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.launch

internal fun OmniChurchViewModel.performLoginMemberWithPin(
  selectedChurchId: String?,
  idOrPhone: String,
  enteredPin: String,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.authenticateMemberSmartWithPin(selectedChurchId, idOrPhone, enteredPin)
      .onSuccess { resolution ->
        when (resolution) {
          is AuthRepository.MemberAutoLoginResolution.SingleMemberResolved -> {
            _pendingMemberMatchedSessions.value = emptyList()
            val session = resolution.session
            preferencesManager.saveMemberSession(
              churchId = session.church.id,
              memberId = session.member.effectiveMatrikil,
              memberDocId = session.member.id,
              memberName = session.member.effectiveFullName,
              barcodeToken = session.member.effectiveBarcodeToken,
            )
            _isAuthenticating.value = false
            onSuccess()
          }
          is AuthRepository.MemberAutoLoginResolution.MultipleMemberChurches -> {
            _pendingMemberMatchedSessions.value = resolution.sessions
            _isAuthenticating.value = false
          }
        }
      }
      .onFailure { err ->
        _pendingMemberMatchedSessions.value = emptyList()
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Erè otantikasyon."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performSelectMatchedMemberSession(
  session: AuthenticatedMemberSession,
  onSuccess: () -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    _pendingMemberMatchedSessions.value = emptyList()
    preferencesManager.saveMemberSession(
      churchId = session.church.id,
      memberId = session.member.effectiveMatrikil,
      memberDocId = session.member.id,
      memberName = session.member.effectiveFullName,
      barcodeToken = session.member.effectiveBarcodeToken,
    )
    _isAuthenticating.value = false
    onSuccess()
  }
}

internal fun OmniChurchViewModel.performUnlockSavedBiometricSession(onSuccess: () -> Unit) {
  viewModelScope.launch {
    val state = sessionState.value
    if (state.biometricEnabled && state.biometricChurchId.isNotBlank() && state.biometricMemberDocId.isNotBlank()) {
      preferencesManager.saveMemberSession(
        churchId = state.biometricChurchId,
        memberId = state.biometricMemberId,
        memberDocId = state.biometricMemberDocId,
        memberName = state.biometricMemberName,
      )
      onSuccess()
    }
  }
}

internal fun OmniChurchViewModel.performToggleBiometricQuickUnlock(enabled: Boolean) {
  viewModelScope.launch {
    val state = sessionState.value
    if (enabled) {
      if (state.activeChurchId.isNotBlank() && state.activeMemberDocId.isNotBlank()) {
        preferencesManager.setBiometricQuickUnlock(
          enabled = true,
          churchId = state.activeChurchId,
          memberId = state.activeMemberId,
          memberDocId = state.activeMemberDocId,
          memberName = "",
        )
        _actionFeedbackMessage.value = "Debloke biyometrik aktive avèk siksè!"
      }
    } else {
      preferencesManager.setBiometricQuickUnlock(
        enabled = false,
        churchId = "",
        memberId = "",
        memberDocId = "",
        memberName = "",
      )
      _actionFeedbackMessage.value = "Debloke biyometrik dezaktive."
    }
  }
}

internal fun OmniChurchViewModel.performOptionalGoogleMemberLogin(
  selectedChurchId: String?,
  googleEmail: String,
  onDirectMemberLoginSuccess: () -> Unit,
  onNeedsPinVerificationPrompt: () -> Unit,
  onError: (String) -> Unit,
) {
  viewModelScope.launch {
    repository.findMemberByLinkedGoogleEmail(selectedChurchId, googleEmail)
      .onSuccess { session ->
        if (session != null) {
          preferencesManager.saveMemberSession(
            churchId = session.church.id,
            memberId = session.member.effectiveMatrikil,
            memberDocId = session.member.id,
            memberName = session.member.effectiveFullName,
            barcodeToken = session.member.effectiveBarcodeToken,
          )
          onDirectMemberLoginSuccess()
        } else {
          onNeedsPinVerificationPrompt()
        }
      }
      .onFailure { err ->
        onError(err.localizedMessage ?: "Erè rechèch Google Email.")
      }
  }
}

internal fun OmniChurchViewModel.performVerifyPinAndLinkGoogleAccount(
  selectedChurchId: String?,
  idOrPhone: String,
  enteredPin: String,
  googleEmail: String,
  googleUid: String,
  googlePhotoUrl: String?,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  viewModelScope.launch {
    repository.authenticateMemberWithPin(selectedChurchId, idOrPhone, enteredPin)
      .onSuccess { session ->
        repository.linkGoogleAccountToMember(
          member = session.member,
          googleEmail = googleEmail,
          googleUid = googleUid,
          googlePhotoUrl = googlePhotoUrl,
        ).onSuccess { updatedMember ->
          preferencesManager.saveMemberSession(
            churchId = session.church.id,
            memberId = updatedMember.effectiveMatrikil,
            memberDocId = updatedMember.id,
            memberName = updatedMember.effectiveFullName,
            barcodeToken = updatedMember.effectiveBarcodeToken,
          )
          onSuccess()
        }.onFailure { err ->
          onError(err.localizedMessage ?: "Erè lye kont Google la.")
        }
      }
      .onFailure { err ->
        onError(err.localizedMessage ?: "PIN an pa kòrèk.")
      }
  }
}

internal fun OmniChurchViewModel.performLinkGoogleToCurrentMember(
  googleEmail: String,
  googleUid: String,
  googlePhotoUrl: String?,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  val currentMember = (currentMemberState.value as? UiState.Success)?.data ?: return
  viewModelScope.launch {
    repository.linkGoogleAccountToMember(currentMember, googleEmail, googleUid, googlePhotoUrl)
      .onSuccess { onSuccess() }
      .onFailure { err -> onError(err.localizedMessage ?: "Erè lye kont Google.") }
  }
}

internal fun OmniChurchViewModel.performLoginAdminForChurch(
  selectedChurchId: String,
  enteredPasscode: String?,
  googleEmail: String?,
  googleUid: String?,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.authenticateAdminForChurch(selectedChurchId, enteredPasscode, googleEmail, googleUid)
      .onSuccess { church ->
        val signedInUid = repository.currentAuthUid()
        if (signedInUid.isNotBlank()) {
          activeAuthUidFlow.value = signedInUid
          repository.syncUserChurchContext(church.id, church.name)
        }
        preferencesManager.saveAuthenticatedSession(
          activeChurchId = church.id,
          activeRole = "admin",
          activeMemberId = "",
          activeMemberDocId = "",
          memberName = church.name,
        )
        _isAuthenticating.value = false
        onSuccess()
      }
      .onFailure { err ->
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Kòd admin an pa kòrèk."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performLogoutCurrentSession(onSuccess: () -> Unit) {
  viewModelScope.launch {
    activeAuthUidFlow.value = ""
    _pendingAdminOwnedChurches.value = emptyList()
    _pendingMemberMatchedSessions.value = emptyList()
    optimisticReadAnnouncementIds.value = emptyMap()
    repository.authRepository.sessionLockManager.clearAllAdminLocks()
    preferencesManager.clearActiveSession()
    onSuccess()
  }
}

internal fun OmniChurchViewModel.performCompleteChurchAndMemberSetup(
  createNewChurch: Boolean,
  churchNameInput: String,
  existingChurchIdInput: String,
  churchAddressInput: String,
  fullNameInput: String,
  departmentInput: String,
  ministryRoleInput: String,
  photoUrl: String?,
  churchCodeInput: String,
  adminPasscodeInput: String,
  memberPinInput: String,
  onSuccess: () -> Unit,
) {
  if (_isSubmittingSetup.value) return
  viewModelScope.launch {
    _isSubmittingSetup.value = true
    repository.setupChurchAndMemberProfile(
      createNewChurch = createNewChurch,
      churchNameInput = churchNameInput,
      existingChurchIdInput = existingChurchIdInput,
      churchAddressInput = churchAddressInput,
      fullNameInput = fullNameInput,
      departmentInput = departmentInput,
      ministryRoleInput = ministryRoleInput,
      photoUrl = photoUrl,
      churchCodeInput = churchCodeInput,
      adminPasscodeInput = adminPasscodeInput,
      memberPinInput = memberPinInput,
    ).onSuccess { member ->
      preferencesManager.saveMemberSession(
        churchId = member.churchId,
        memberId = member.effectiveMatrikil,
        memberDocId = member.id,
        memberName = member.effectiveFullName,
        barcodeToken = member.effectiveBarcodeToken,
      )
      _isSubmittingSetup.value = false
      onSuccess()
    }.onFailure { err ->
      _isSubmittingSetup.value = false
      _actionFeedbackMessage.value = err.localizedMessage ?: "Erè konfigirasyon profil."
    }
  }
}

internal fun OmniChurchViewModel.performSignInAdminSmartWithEmailAndPassword(
  email: String,
  password: String,
  rememberMe: Boolean,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.signInAdminSmartWithEmailAndPassword(
      email = email,
      password = password,
    )
      .onSuccess { resolution ->
        preferencesManager.saveRememberedAdminCredentials(
          remember = rememberMe,
          email = email,
          password = password,
        )
        handleAdminAutoLoginResolution(resolution, onSuccess)
      }
      .onFailure { err ->
        _pendingAdminOwnedChurches.value = emptyList()
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performSignInAdminSmartWithCurrentGoogleUser(
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.signInAdminSmartWithCurrentGoogleUser()
      .onSuccess { resolution ->
        handleAdminAutoLoginResolution(resolution, onSuccess)
      }
      .onFailure { err ->
        _pendingAdminOwnedChurches.value = emptyList()
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Nou pa t kapab verifye kont Google Admin nan."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

private suspend fun OmniChurchViewModel.handleAdminAutoLoginResolution(
  resolution: AuthRepository.AdminAutoLoginResolution,
  onSuccess: () -> Unit,
) {
  when (resolution) {
    is AuthRepository.AdminAutoLoginResolution.SingleChurchResolved -> {
      _pendingAdminOwnedChurches.value = emptyList()
      val church = resolution.church
      val signedInUid = repository.currentAuthUid()
      if (signedInUid.isNotBlank()) {
        activeAuthUidFlow.value = signedInUid
      }
      preferencesManager.saveAuthenticatedSession(
        activeChurchId = church.id,
        activeRole = "admin",
        activeMemberId = "",
        activeMemberDocId = "",
        memberName = church.name,
      )
      _isAuthenticating.value = false
      onSuccess()
    }
    is AuthRepository.AdminAutoLoginResolution.MultipleOwnedChurches -> {
      _pendingAdminOwnedChurches.value = resolution.churches
      _isAuthenticating.value = false
    }
  }
}

internal fun OmniChurchViewModel.performSelectOwnedAdminChurch(
  church: Church,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.finalizeAdminChurchSelection(church)
      .onSuccess { finalizedChurch ->
        _pendingAdminOwnedChurches.value = emptyList()
        val signedInUid = repository.currentAuthUid()
        if (signedInUid.isNotBlank()) {
          activeAuthUidFlow.value = signedInUid
        }
        preferencesManager.saveAuthenticatedSession(
          activeChurchId = finalizedChurch.id,
          activeRole = "admin",
          activeMemberId = "",
          activeMemberDocId = "",
          memberName = finalizedChurch.name,
        )
        _isAuthenticating.value = false
        onSuccess()
      }
      .onFailure { err ->
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Nou pa t kapab ouvri legliz sa a."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performSignInAdminWithEmailAndPassword(
  email: String,
  password: String,
  selectedChurchId: String?,
  onSuccess: () -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.signInAdminWithEmailAndPassword(
      email = email,
      password = password,
      selectedChurchId = selectedChurchId,
    )
      .onSuccess { church ->
        val signedInUid = repository.currentAuthUid()
        if (signedInUid.isNotBlank()) {
          activeAuthUidFlow.value = signedInUid
        }
        preferencesManager.saveAuthenticatedSession(
          activeChurchId = church.id,
          activeRole = "admin",
          activeMemberId = "",
          activeMemberDocId = "",
          memberName = church.name,
        )
        _isAuthenticating.value = false
        onSuccess()
      }
      .onFailure { err ->
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performSendAdminPasswordResetEmail(
  email: String,
  onSuccess: (String) -> Unit,
  onError: (String) -> Unit,
) {
  if (_isAuthenticating.value) return
  viewModelScope.launch {
    _isAuthenticating.value = true
    repository.sendAdminPasswordResetEmail(email)
      .onSuccess {
        _isAuthenticating.value = false
        onSuccess(
          "Nou voye yon lyen pou reyinisyalize modpas la nan imèl ${email.trim().lowercase()}."
        )
      }
      .onFailure { err ->
        _isAuthenticating.value = false
        val msg = err.localizedMessage ?: "Nou pa t kapab voye lyen an."
        _actionFeedbackMessage.value = msg
        onError(msg)
      }
  }
}

internal fun OmniChurchViewModel.performRegisterNewMemberByAdmin(
  church: Church,
  fullName: String,
  customMatrikil: String?,
  pin: String,
  phone: String?,
  department: String,
  ministryRole: String,
  onSuccess: (Member) -> Unit,
  onError: (String) -> Unit,
) {
  viewModelScope.launch {
    repository.registerMemberWithMatrikilAndPin(
      church = church,
      fullNameInput = fullName,
      customMatrikilInput = customMatrikil,
      pinInput = pin,
      phoneInput = phone,
      departmentInput = department,
      ministryRoleInput = ministryRole,
    ).onSuccess { member ->
      _actionFeedbackMessage.value = "Manm an anrejistre avèk siksè: ${member.effectiveMatrikil}"
      onSuccess(member)
    }.onFailure { err ->
      val msg = err.localizedMessage ?: "Erè anrejistreman manm."
      _actionFeedbackMessage.value = msg
      onError(msg)
    }
  }
}
