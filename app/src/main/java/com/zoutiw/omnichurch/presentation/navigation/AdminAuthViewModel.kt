package com.zoutiw.omnichurch.presentation.navigation

import android.os.SystemClock
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.repositories.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Dedicated ViewModel managing Cloud Admin Authentication (Email+Password, Google Credential Manager,
 * Password Reset, Legacy PIN Migration, Quick PIN Resume, Multi-Church Creation, Security Settings,
 * and Inactivity Auto-Lock with 30s warning countdown + anti-bypass state).
 */
class AdminAuthViewModel(
  private val authRepository: AuthRepository,
  private val sessionLockManager: SessionLockManager,
) : ViewModel() {

  val firebaseUser: StateFlow<FirebaseUser?> =
    authRepository.currentFirebaseUserFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = authRepository.currentUser(),
    )

  val lockedChurches: StateFlow<Map<String, Boolean>> =
    authRepository.lockedChurchesFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyMap(),
    )

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _authError = MutableStateFlow<String?>(null)
  val authError: StateFlow<String?> = _authError.asStateFlow()

  private val _infoMessage = MutableStateFlow<String?>(null)
  val infoMessage: StateFlow<String?> = _infoMessage.asStateFlow()

  private val _legacyVerifiedChurchId = MutableStateFlow<String?>(null)
  val legacyVerifiedChurchId: StateFlow<String?> = _legacyVerifiedChurchId.asStateFlow()

  private val _legacyVerifiedPin = MutableStateFlow<String?>(null)
  val legacyVerifiedPin: StateFlow<String?> = _legacyVerifiedPin.asStateFlow()

  private val _autoLockWarningSeconds = MutableStateFlow<Int?>(null)
  val autoLockWarningSeconds: StateFlow<Int?> = _autoLockWarningSeconds.asStateFlow()

  private var lastActivityElapsedMs: Long = SystemClock.elapsedRealtime()
  private var inactivityWatcherJob: Job? = null

  fun clearMessages() {
    _authError.value = null
    _infoMessage.value = null
  }

  fun setAuthError(msg: String?) {
    _authError.value = msg
  }

  fun resetLegacyMigrationStep() {
    _legacyVerifiedChurchId.value = null
    _legacyVerifiedPin.value = null
  }

  fun canQuickPinUnlock(church: Church?): Boolean {
    if (church == null) return false
    return SessionLockManager.canQuickPinUnlock(church, firebaseUser.value)
  }

  fun isChurchAdminAuthorized(church: Church?): Boolean {
    if (church == null) return false
    return SessionLockManager.isChurchAdminAuthorized(
      church = church,
      firebaseUser = firebaseUser.value,
      lockedChurches = lockedChurches.value,
    )
  }

  fun signInAdminWithEmailPassword(
    church: Church?,
    email: String,
    password: String,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value) return
    val safeChurch = church ?: run {
      _authError.value = AdminAuthLocalizedMessages.selectChurchFirst(languageCode)
      return
    }
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res = authRepository.signInAdminWithEmailAndPassword(safeChurch, email, password, languageCode)
      _isBusy.value = false
      res
        .onSuccess { authorizedChurch ->
          recordUserInteraction()
          onSuccess(authorizedChurch)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun verifySignedInAdminGoogleAccount(
    church: Church?,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value) return
    if (church == null) {
      _authError.value = AdminAuthLocalizedMessages.selectChurchFirst(languageCode)
      return
    }
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res = authRepository.verifySignedInAdminGoogleAccount(church, languageCode)
      _isBusy.value = false
      res
        .onSuccess { authorizedChurch ->
          recordUserInteraction()
          onSuccess(authorizedChurch)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun sendPasswordResetEmail(
    email: String,
    languageCode: String,
  ) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res = authRepository.sendPasswordResetEmail(email, languageCode)
      _isBusy.value = false
      res
        .onSuccess {
          _infoMessage.value = AdminAuthLocalizedMessages.passwordResetSent(languageCode, email)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun unlockWithQuickPin(
    church: Church?,
    pin: String,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value) return
    if (church == null) {
      _authError.value = AdminAuthLocalizedMessages.selectChurchFirst(languageCode)
      return
    }
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res = authRepository.verifyQuickPinAndUpgradeIfNeeded(church, pin, languageCode)
      _isBusy.value = false
      res
        .onSuccess { updatedChurch ->
          recordUserInteraction()
          onSuccess(updatedChurch)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun authorizeViaBiometric(
    church: Church?,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (church == null || !SessionLockManager.hasMatchingChurchCloudOwner(church, firebaseUser.value)) {
      _authError.value = AdminAuthLocalizedMessages.selectChurchFirst(languageCode)
      return
    }
    viewModelScope.launch {
      authRepository.authorizeChurchAdminSession(church.id)
      recordUserInteraction()
      onSuccess(church)
    }
  }

  fun verifyLegacyPinStep(
    church: Church?,
    pin: String,
    languageCode: String,
  ) {
    if (_isBusy.value || church == null) return
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      val res = authRepository.verifyLegacyPinStep(church, pin, languageCode)
      _isBusy.value = false
      res
        .onSuccess {
          _legacyVerifiedChurchId.value = church.id
          _legacyVerifiedPin.value = pin.trim()
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun completeLegacyMigration(
    church: Church?,
    adminEmail: String,
    newPassword: String,
    confirmPassword: String,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value || church == null) return
    val oldPin = _legacyVerifiedPin.value
    if (oldPin.isNullOrBlank() || _legacyVerifiedChurchId.value != church.id) {
      _authError.value = "Verifye ansyen kòd PIN lan anvan."
      return
    }
    if (newPassword != confirmPassword) {
      _authError.value = AdminAuthLocalizedMessages.passwordMismatch(languageCode)
      return
    }
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      val res =
        authRepository.migrateLegacyPinChurchToCloudOwner(
          church = church,
          verifiedOldPin = oldPin,
          adminEmail = adminEmail,
          newPassword = newPassword,
          languageCode = languageCode,
        )
      _isBusy.value = false
      res
        .onSuccess { migratedChurch ->
          resetLegacyMigrationStep()
          recordUserInteraction()
          onSuccess(migratedChurch)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun createChurchWithEmailPassword(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    confirmPassword: String,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value) return
    if (password != confirmPassword) {
      _authError.value = AdminAuthLocalizedMessages.passwordMismatch(languageCode)
      return
    }
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res =
        authRepository.createChurchWithEmailPassword(
          churchName = churchName,
          churchCode = churchCode,
          churchAddress = churchAddress,
          email = email,
          password = password,
          languageCode = languageCode,
        )
      _isBusy.value = false
      res
        .onSuccess { created ->
          recordUserInteraction()
          onSuccess(created)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun createChurchWithSignedInGoogleUser(
    churchName: String,
    churchCode: String,
    churchAddress: String,
    languageCode: String,
    onSuccess: (Church) -> Unit,
  ) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    _infoMessage.value = null
    viewModelScope.launch {
      val res =
        authRepository.createChurchWithSignedInGoogleUser(
          churchName = churchName,
          churchCode = churchCode,
          churchAddress = churchAddress,
          languageCode = languageCode,
        )
      _isBusy.value = false
      res
        .onSuccess { created ->
          recordUserInteraction()
          onSuccess(created)
        }
        .onFailure { err -> _authError.value = err.message }
    }
  }

  fun updateSecuritySettings(
    church: Church,
    quickPinEnabled: Boolean,
    newPinOrNull: String?,
    autoLockMinutes: Int,
    languageCode: String,
    onSuccess: (Church) -> Unit = {},
    onError: (String) -> Unit = {},
  ) {
    if (_isBusy.value) return
    _isBusy.value = true
    _authError.value = null
    viewModelScope.launch {
      val res =
        authRepository.updateChurchSecuritySettings(
          church = church,
          quickPinEnabled = quickPinEnabled,
          newPinOrNull = newPinOrNull,
          autoLockMinutes = autoLockMinutes,
          languageCode = languageCode,
        )
      _isBusy.value = false
      res
        .onSuccess { updated ->
          recordUserInteraction()
          onSuccess(updated)
        }
        .onFailure { err ->
          val msg = err.message ?: "Erè pandan aktyalizasyon sekirite."
          _authError.value = msg
          onError(msg)
        }
    }
  }

  fun recordUserInteraction() {
    lastActivityElapsedMs = SystemClock.elapsedRealtime()
    if (_autoLockWarningSeconds.value != null) {
      _autoLockWarningSeconds.value = null
    }
  }

  fun lockChurchNow(churchId: String) {
    _autoLockWarningSeconds.value = null
    inactivityWatcherJob?.cancel()
    if (churchId.isBlank()) return
    viewModelScope.launch {
      authRepository.lockChurchAdminSession(churchId)
    }
  }

  fun onAppStopped(isAdminSession: Boolean) {
    if (!isAdminSession) return
    viewModelScope.launch {
      sessionLockManager.recordAdminBackgroundTimestamp(System.currentTimeMillis())
    }
  }

  fun onAppStarted(isAdminSession: Boolean, church: Church?) {
    if (!isAdminSession || church == null) return
    val minutes = church.effectiveAutoLockMinutes
    if (minutes <= 0) {
      viewModelScope.launch { sessionLockManager.clearAdminBackgroundTimestamp() }
      return
    }
    viewModelScope.launch {
      val stoppedAt = sessionLockManager.getLastAdminBackgroundTimestamp()
      sessionLockManager.clearAdminBackgroundTimestamp()
      if (stoppedAt != null) {
        val elapsedMs = System.currentTimeMillis() - stoppedAt
        if (elapsedMs >= minutes * 60_000L) {
          authRepository.lockChurchAdminSession(church.id)
          _autoLockWarningSeconds.value = null
        }
      }
    }
  }

  fun updateInactivityWatcher(
    isAdminSession: Boolean,
    isAdminUnlocked: Boolean,
    church: Church?,
  ) {
    inactivityWatcherJob?.cancel()
    if (!isAdminSession || !isAdminUnlocked || church == null) {
      _autoLockWarningSeconds.value = null
      return
    }
    val autoLockMins = church.effectiveAutoLockMinutes
    if (autoLockMins <= 0) {
      _autoLockWarningSeconds.value = null
      return
    }

    val timeoutMs = autoLockMins * 60_000L
    val warningWindowMs = minOf(WARNING_COUNTDOWN_MS, timeoutMs / 2)
    lastActivityElapsedMs = SystemClock.elapsedRealtime()

    inactivityWatcherJob =
      viewModelScope.launch {
        while (true) {
          delay(1000L)
          val elapsed = SystemClock.elapsedRealtime() - lastActivityElapsedMs
          val remaining = timeoutMs - elapsed
          when {
            remaining <= 0L -> {
              _autoLockWarningSeconds.value = null
              authRepository.lockChurchAdminSession(church.id)
              break
            }
            remaining <= warningWindowMs -> {
              _autoLockWarningSeconds.value = ((remaining + 999L) / 1000L).toInt().coerceAtLeast(1)
            }
            else -> {
              if (_autoLockWarningSeconds.value != null) {
                _autoLockWarningSeconds.value = null
              }
            }
          }
        }
      }
  }

  fun signOutAdmin(
    credentialManager: CredentialManager?,
    onComplete: () -> Unit = {},
  ) {
    inactivityWatcherJob?.cancel()
    _autoLockWarningSeconds.value = null
    resetLegacyMigrationStep()
    viewModelScope.launch {
      authRepository.signOutAdmin(credentialManager)
      onComplete()
    }
  }

  override fun onCleared() {
    super.onCleared()
    inactivityWatcherJob?.cancel()
  }

  private companion object {
    const val WARNING_COUNTDOWN_MS = 30_000L
  }
}
