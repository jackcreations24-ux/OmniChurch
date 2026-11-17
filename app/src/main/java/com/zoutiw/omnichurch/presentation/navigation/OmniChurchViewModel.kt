package com.zoutiw.omnichurch.presentation.navigation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.repositories.OmniChurchRepository
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OmniChurchViewModel(
  val repository: OmniChurchRepository,
  val preferencesManager: OmniPreferencesManager,
  val currentUserId: String = "",
  val currentUserEmail: String = "",
  val currentUserDisplayName: String = "",
  val currentUserPhotoUrl: String? = null,
) : ViewModel() {

  constructor(
    context: Context,
    currentUserId: String = "",
    currentUserEmail: String = "",
    currentUserDisplayName: String = "",
    currentUserPhotoUrl: String? = null,
  ) : this(
    repository = OmniChurchRepository(context),
    preferencesManager = OmniPreferencesManager(context),
    currentUserId = currentUserId,
    currentUserEmail = currentUserEmail,
    currentUserDisplayName = currentUserDisplayName,
    currentUserPhotoUrl = currentUserPhotoUrl,
  )

  private val activeAuthUidFlow = MutableStateFlow(currentUserId.trim())

  fun syncAuthIdentity(uid: String) {
    val cleanUid = uid.trim()
    if (activeAuthUidFlow.value != cleanUid) {
      activeAuthUidFlow.value = cleanUid
    }
  }

  private fun effectiveActorUid(): String =
    activeAuthUidFlow.value.ifBlank { currentUserId }.trim()

  val sessionState: StateFlow<OmniSessionState> =
    preferencesManager.sessionStateFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = OmniSessionState(),
    )

  val churchesState: StateFlow<List<Church>> =
    repository.observeChurches().stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val firestoreUserProfileFlow: Flow<UserProfile?> =
    activeAuthUidFlow.flatMapLatest { uid ->
      if (uid.isBlank()) flowOf(null) else repository.observeUserProfile(uid)
    }

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    firestoreUserProfileFlow.map { profile ->
      UiState.Success(profile) as UiState<UserProfile?>
    }.catch { err ->
      emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman pwofil la"))
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val activeChurchIdFlow: Flow<String?> =
    sessionState.map { state ->
      state.activeChurchId.trim().ifEmpty { null }
    }

  val activeChurchState: StateFlow<Church?> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(null)
      else repository.observeChurch(churchId)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  val currentMemberState: StateFlow<UiState<Member?>> =
    sessionState.flatMapLatest { state ->
      val churchId = state.activeChurchId
      val docId = state.activeMemberDocId
      val memberId = state.activeMemberId
      if (docId.isBlank() && memberId.isBlank()) {
        flowOf(UiState.Success(null))
      } else {
        repository.observeSessionMember(churchId, docId, memberId).map { member ->
          UiState.Success(member) as UiState<Member?>
        }
      }
    }.catch { err ->
      emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman manm nan"))
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val servicesState: StateFlow<UiState<List<ChurchService>>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
      else repository.observeServices(churchId).map { UiState.Success(it) as UiState<List<ChurchService>> }
    }.catch { err ->
      emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman sèvis yo"))
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val activeAttendanceServicesState: StateFlow<List<ChurchService>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList())
      else repository.observeActiveAttendanceServices(churchId)
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val churchMembersState: StateFlow<List<Member>> =
    combine(activeChurchIdFlow, activeChurchState) { churchId, church ->
      val cleanId = churchId.orEmpty().trim()
      val cleanCode = church?.code.orEmpty().trim()
      cleanId to cleanCode
    }.flatMapLatest { (churchId, churchCode) ->
      if (churchId.isBlank() && churchCode.isBlank()) flowOf(emptyList())
      else repository.observeChurchMembers(churchId, churchCode)
    }.catch { _ ->
      emit(emptyList())
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  val announcementsState: StateFlow<UiState<List<Announcement>>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
      else repository.observeAnnouncements(churchId).map { UiState.Success(it) as UiState<List<Announcement>> }
    }.catch { err ->
      emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman anons yo"))
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading,
    )

  val optimisticUserReactions = MutableStateFlow<Map<String, OmniReactionType>>(emptyMap())

  val churchReactionsFlow: Flow<List<AnnouncementReaction>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList())
      else repository.observeReactions(churchId)
    }

  val reactionSummariesState: StateFlow<Map<String, TargetReactionSummary>> =
    combine(churchReactionsFlow, optimisticUserReactions) { reactions, optimisticMap ->
      val grouped = reactions.groupBy { it.targetId }
      val result = mutableMapOf<String, TargetReactionSummary>()
      for ((targetId, targetReactions) in grouped) {
        val totalCount = targetReactions.size
        val countsByType = targetReactions.groupBy { OmniReactionType.fromKey(it.reactionType) }
          .mapValues { it.value.size }
        val topTypes = countsByType.entries.sortedByDescending { it.value }.map { it.key }.take(3)
        val myReactionObj = targetReactions.firstOrNull { it.userId == effectiveActorUid() }
        val myType = optimisticMap[targetId] ?: myReactionObj?.let { OmniReactionType.fromKey(it.reactionType) }
        result[targetId] = TargetReactionSummary(
          totalCount = totalCount,
          topReactions = topTypes,
          currentUserReaction = myType,
        )
      }
      for ((tId, optType) in optimisticMap) {
        if (!result.containsKey(tId)) {
          result[tId] = TargetReactionSummary(
            totalCount = 1,
            topReactions = listOf(optType),
            currentUserReaction = optType,
          )
        }
      }
      result
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyMap(),
    )

  val notificationsState: StateFlow<UiState<List<ChurchNotification>>> =
    combine(activeChurchIdFlow, activeAuthUidFlow) { churchId, uid -> churchId to uid }
      .flatMapLatest { (churchId, uid) ->
        repository.observeNotifications(uid, churchId)
          .map { UiState.Success(it) as UiState<List<ChurchNotification>> }
      }.catch { _ ->
        emit(UiState.Success(emptyList()))
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val cachedOfflineQrToken: StateFlow<String?> =
    preferencesManager.cachedQrTokenFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = null,
    )

  private val _memberQrBitmap = MutableStateFlow<Bitmap?>(null)
  val memberQrBitmap: StateFlow<Bitmap?> = _memberQrBitmap.asStateFlow()

  private val _scannerOutcome = MutableStateFlow<ScannerValidationOutcome>(ScannerValidationOutcome.Idle)
  val scannerOutcome: StateFlow<ScannerValidationOutcome> = _scannerOutcome.asStateFlow()

  private val _selectedScannerServiceId = MutableStateFlow<String?>(null)
  val selectedScannerServiceId: StateFlow<String?> = _selectedScannerServiceId.asStateFlow()

  private val _actionFeedbackMessage = MutableStateFlow<String?>(null)
  val actionFeedbackMessage: StateFlow<String?> = _actionFeedbackMessage.asStateFlow()

  private val _isSubmittingSetup = MutableStateFlow(false)
  val isSubmittingSetup: StateFlow<Boolean> = _isSubmittingSetup.asStateFlow()

  private val _isAuthenticating = MutableStateFlow(false)
  val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

  init {
    if (currentUserId.isNotBlank()) {
      viewModelScope.launch {
        repository.ensureUserProfileInitialized(
          uid = currentUserId,
          email = currentUserEmail,
          displayName = currentUserDisplayName,
          photoUrl = currentUserPhotoUrl,
        )
      }
    }
    viewModelScope.launch {
      combine(currentMemberState, cachedOfflineQrToken) { memberState, offlineToken ->
        val member = (memberState as? UiState.Success)?.data
        val tokenToUse = member?.standardizedQrPayload
          ?.takeIf { it.isNotBlank() }
          ?: member?.effectiveBarcodeToken
          ?.takeIf { it.isNotBlank() }
          ?: offlineToken
          ?.takeIf { !it.isNullOrBlank() }
          .orEmpty()
        tokenToUse
      }.collect { token ->
        if (token.isNotBlank()) {
          val bitmap = QrCodeGenerator.generateQrBitmap(token, sizePx = 768, includeCenterEmblem = true)
          _memberQrBitmap.value = bitmap
        } else {
          _memberQrBitmap.value = null
        }
      }
    }
  }

  fun clearFeedbackMessage() {
    _actionFeedbackMessage.value = null
  }

  fun setFeedbackMessage(msg: String?) {
    _actionFeedbackMessage.value = msg
  }

  fun selectScannerService(serviceId: String) {
    _selectedScannerServiceId.value = serviceId
  }

  fun resetScannerOutcome() {
    _scannerOutcome.value = ScannerValidationOutcome.Idle
  }

  fun loginMemberWithPin(
    selectedChurchId: String?,
    idOrPhone: String,
    enteredPin: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
  ) {
    if (_isAuthenticating.value) return
    viewModelScope.launch {
      _isAuthenticating.value = true
      repository.authenticateMemberWithPin(selectedChurchId, idOrPhone, enteredPin)
        .onSuccess { session ->
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
        .onFailure { err ->
          _isAuthenticating.value = false
          val msg = err.localizedMessage ?: "Erè otantikasyon."
          _actionFeedbackMessage.value = msg
          onError(msg)
        }
    }
  }

  fun unlockSavedBiometricSession(onSuccess: () -> Unit) {
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

  fun toggleBiometricQuickUnlock(enabled: Boolean) {
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

  fun handleOptionalGoogleMemberLogin(
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

  fun verifyPinAndLinkGoogleAccount(
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

  fun linkGoogleToCurrentMember(
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

  fun loginAdminForChurch(
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
          preferencesManager.saveAuthenticatedSession(
            activeChurchId = church.id,
            activeRole = "admin",
            activeMemberId = "",
            activeMemberDocId = "",
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

  fun logoutCurrentSession(onSuccess: () -> Unit) {
    viewModelScope.launch {
      activeAuthUidFlow.value = ""
      preferencesManager.clearActiveSession()
      onSuccess()
    }
  }

  fun completeChurchAndMemberSetup(
    createNewChurch: Boolean,
    churchNameInput: String,
    existingChurchIdInput: String,
    churchAddressInput: String,
    fullNameInput: String,
    departmentInput: String,
    ministryRoleInput: String,
    photoUrl: String?,
    churchCodeInput: String = "",
    adminPasscodeInput: String = "",
    memberPinInput: String = "1234",
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

  fun signInAdminWithEmailAndPassword(
    email: String,
    password: String,
    selectedChurchId: String? = null,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
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

  fun sendAdminPasswordResetEmail(
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

  fun registerNewMemberByAdmin(
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

  fun createService(
    churchId: String,
    churchName: String?,
    title: String,
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean,
    authorName: String,
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
  ) {
    viewModelScope.launch {
      val dateFormatted = "$date • $startTime"
      repository.createService(
        churchId = churchId,
        churchName = churchName ?: "OmniChurch Pro",
        title = title,
        description = null,
        dateFormatted = dateFormatted,
        lateToleranceMinutes = lateToleranceMinutes,
        isOpenForAttendance = isOpenForAttendance,
        isLive = isLive,
        authorName = authorName,
        actorId = effectiveActorUid(),
        category = category,
        date = date,
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
      ).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la kreye avèk siksè!"
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè kreyasyon sèvis."
      }
    }
  }

  fun updateService(
    serviceId: String,
    title: String,
    description: String?,
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
  ) {
    viewModelScope.launch {
      val dateFormatted = "$date • $startTime"
      repository.updateService(
        serviceId = serviceId,
        title = title,
        description = description,
        dateFormatted = dateFormatted,
        lateToleranceMinutes = lateToleranceMinutes,
        isOpenForAttendance = isOpenForAttendance,
        isLive = isLive,
        category = category,
        date = date,
        startTime = startTime,
        endTime = endTime,
        theme = theme,
        passage = passage,
        leader = leader,
        preacher = preacher,
        reader = reader,
        musicians = musicians,
        ushers = ushers,
      ).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la mete ajou avèk siksè!"
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan mizajou sèvis la."
      }
    }
  }

  fun toggleServiceAttendanceWindow(service: ChurchService, isOpen: Boolean) {
    viewModelScope.launch {
      repository.toggleServiceAttendanceStatus(service, isOpen)
    }
  }

  fun deleteService(serviceId: String) {
    viewModelScope.launch {
      repository.deleteService(serviceId).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la efase avèk siksè."
      }
    }
  }

  fun createAnnouncement(
    churchId: String,
    title: String,
    content: String,
    isPinned: Boolean,
    authorName: String,
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ) {
    viewModelScope.launch {
      repository.createAnnouncement(
        churchId = churchId,
        title = title,
        content = content,
        department = "Depatman Administrasyon",
        isPinned = isPinned,
        authorName = authorName,
        actorId = effectiveActorUid(),
        category = category,
        date = date,
        targetAudience = targetAudience,
        showToastOnLogin = showToastOnLogin,
        toastAlertLevel = toastAlertLevel,
        isScheduledNotification = isScheduledNotification,
        scheduledNotificationDate = scheduledNotificationDate,
        posterUrl = posterUrl,
        serviceId = serviceId,
      ).onSuccess {
        _actionFeedbackMessage.value = "Anons la pibliye avèk siksè!"
      }
    }
  }

  fun deleteAnnouncement(announcementId: String) {
    viewModelScope.launch {
      repository.deleteAnnouncement(announcementId).onSuccess {
        _actionFeedbackMessage.value = "Anons la efase avèk siksè."
      }
    }
  }

  fun reactToFeedItem(
    targetId: String,
    isServiceTarget: Boolean,
    churchId: String,
    reactionType: OmniReactionType = OmniReactionType.PRAY,
    isCommentTarget: Boolean = false,
  ) {
    val currentUserProfile = (userProfileState.value as? UiState.Success)?.data
    val userName = currentUserProfile?.displayName ?: "Manm OmniChurch"
    optimisticUserReactions.value = optimisticUserReactions.value + (targetId to reactionType)
    viewModelScope.launch {
      repository.reactToTarget(
        targetId = targetId,
        isServiceTarget = isServiceTarget,
        churchId = churchId,
        userName = userName,
        reactionType = reactionType.key,
        actorId = effectiveActorUid(),
        isCommentTarget = isCommentTarget,
      )
    }
  }

  fun observeCommentsForTarget(churchId: String, targetId: String): Flow<List<AnnouncementComment>> =
    repository.observeComments(churchId, targetId)

  fun observeAttendancesForService(churchId: String, serviceId: String): Flow<List<AttendanceRecord>> =
    repository.observeServiceAttendances(churchId, serviceId)

  fun postComment(targetId: String, isServiceTarget: Boolean, content: String) {
    val currentUserProfile = (userProfileState.value as? UiState.Success)?.data ?: return
    val churchId = currentUserProfile.churchId ?: return
    viewModelScope.launch {
      repository.addComment(
        targetId = targetId,
        isServiceTarget = isServiceTarget,
        churchId = churchId,
        userName = currentUserProfile.displayName,
        userPhotoUrl = currentUserProfile.photoUrl,
        content = content,
        actorId = effectiveActorUid(),
      )
    }
  }

  fun processAttendanceScanOrCode(
    rawTokenOrCode: String,
    selectedService: ChurchService,
    checkInMethod: String = "qr_scanner",
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

  fun bindNfcCardToMember(
    memberDocId: String,
    nfcCardUid: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
  ) {
    viewModelScope.launch {
      repository.bindNfcCardUidToMember(memberDocId, nfcCardUid)
        .onSuccess {
          _actionFeedbackMessage.value = "Kat NFC la lye ak manm nan avèk siksè!"
          onSuccess()
        }
        .onFailure { err ->
          val msg = err.localizedMessage ?: "Erè pandan lyezon kat NFC a."
          _actionFeedbackMessage.value = msg
          onError(msg)
        }
    }
  }

  fun markNotificationAsRead(notificationId: String) {
    viewModelScope.launch {
      repository.markNotificationRead(notificationId)
    }
  }

  fun markAllNotificationsRead(notifications: List<ChurchNotification>) {
    viewModelScope.launch {
      notifications.filter { !it.isRead }.forEach { notif ->
        repository.markNotificationRead(notif.id)
      }
    }
  }

  fun updateMemberProfileInfo(
    fullName: String,
    department: String,
    ministryRole: String,
    onSuccess: () -> Unit = {},
  ) {
    val member = (currentMemberState.value as? UiState.Success)?.data ?: return
    viewModelScope.launch {
      repository.updateMemberProfile(member, fullName, department, ministryRole).onSuccess {
        _actionFeedbackMessage.value = "Profil la ajou avèk siksè."
        onSuccess()
      }
    }
  }

  fun updateChurchMemberByAdmin(
    memberDocId: String,
    fullName: String,
    department: String,
    ministryRole: String,
    phone: String? = null,
    titlePrefix: String? = null,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
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

  fun updateMemberPhoto(
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

  companion object {
    private const val TAG = "OmniChurchViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
