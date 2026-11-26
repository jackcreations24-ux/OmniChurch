package com.zoutiw.omnichurch.presentation.navigation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.RememberedAdminCredentials
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.AttendanceRecord
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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

  internal val activeAuthUidFlow = MutableStateFlow(currentUserId.trim())
  val currentAuthUid: StateFlow<String> = activeAuthUidFlow.asStateFlow()

  fun syncAuthIdentity(uid: String) {
    val cleanUid = uid.trim()
    if (activeAuthUidFlow.value != cleanUid) {
      activeAuthUidFlow.value = cleanUid
    }
  }

  val sessionState: StateFlow<OmniSessionState> =
    preferencesManager.sessionStateFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = OmniSessionState(),
    )

  val rememberedAdminCredentials: StateFlow<RememberedAdminCredentials> =
    preferencesManager.rememberedAdminCredentialsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = RememberedAdminCredentials(),
    )

  internal val _pendingAdminOwnedChurches = MutableStateFlow<List<Church>>(emptyList())
  val pendingAdminOwnedChurches: StateFlow<List<Church>> = _pendingAdminOwnedChurches.asStateFlow()

  internal val _pendingMemberMatchedSessions =
    MutableStateFlow<List<AuthenticatedMemberSession>>(emptyList())
  val pendingMemberMatchedSessions: StateFlow<List<AuthenticatedMemberSession>> =
    _pendingMemberMatchedSessions.asStateFlow()

  private val churchesRefreshTrigger = MutableStateFlow(0)

  fun retryLoadChurches() {
    churchesRefreshTrigger.value += 1
  }

  val churchesUiState: StateFlow<UiState<List<Church>>> =
    buildChurchesUiStateFlow(churchesRefreshTrigger)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val churchesState: StateFlow<List<Church>> =
    churchesUiState
      .map { state -> (state as? UiState.Success)?.data ?: emptyList() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val firestoreUserProfileFlow: Flow<UserProfile?> =
    activeAuthUidFlow.flatMapLatest { uid ->
      if (uid.isBlank()) flowOf(null) else repository.observeUserProfile(uid)
    }

  val userProfileState: StateFlow<UiState<UserProfile?>> =
    firestoreUserProfileFlow
      .map { profile -> UiState.Success(profile) as UiState<UserProfile?> }
      .catch { err -> emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman pwofil la")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val activeChurchIdFlow: Flow<String?> =
    sessionState.map { state -> state.activeChurchId.trim().ifEmpty { null } }

  val activeChurchState: StateFlow<Church?> =
    activeChurchIdFlow
      .flatMapLatest { churchId ->
        if (churchId.isNullOrBlank()) flowOf(null) else repository.observeChurch(churchId)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  val currentMemberState: StateFlow<UiState<Member?>> =
    buildCurrentMemberUiStateFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val memberAttendanceHistoryState: StateFlow<List<MemberAttendanceHistoryItem>> =
    currentMemberState.flatMapLatest { memberState ->
      val member = (memberState as? UiState.Success)?.data
      if (member == null || member.id.isBlank()) flowOf(emptyList())
      else repository.attendanceRepository.observeMemberAttendanceHistory(member.churchId, member.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val servicesState: StateFlow<UiState<List<ChurchService>>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(UiState.Success(emptyList()))
      else repository.observeServices(churchId).map { UiState.Success(it) as UiState<List<ChurchService>> }
    }
      .catch { err -> emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman sèvis yo")) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val activeAttendanceServicesState: StateFlow<List<ChurchService>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList())
      else repository.observeActiveAttendanceServices(churchId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val memberRefreshTrigger = MutableStateFlow(0)

  fun retryLoadChurchMembers() {
    memberRefreshTrigger.value += 1
  }

  val churchMembersUiState: StateFlow<UiState<List<Member>>> =
    buildChurchMembersUiStateFlow(memberRefreshTrigger)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val churchMembersState: StateFlow<List<Member>> =
    churchMembersUiState
      .map { state -> (state as? UiState.Success)?.data ?: emptyList() }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  val publicMemberNamesState: StateFlow<List<Member>> =
    buildPublicMemberDisplayNamesFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())

  private val announcementsRefreshTrigger = MutableStateFlow(0)

  fun retryLoadAnnouncements() {
    announcementsRefreshTrigger.value += 1
  }

  val announcementsState: StateFlow<UiState<List<Announcement>>> =
    buildAnnouncementsUiStateFlow(announcementsRefreshTrigger)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  internal val optimisticReadAnnouncementIds =
    MutableStateFlow<Map<String, Set<String>>>(emptyMap())

  private val activeReaderKeyFlow: Flow<String> =
    combine(sessionState, activeAuthUidFlow) { session, authUid ->
      session.activeMemberDocId.trim().takeIf { it.isNotEmpty() }
        ?: session.activeMemberId.trim().takeIf { it.isNotEmpty() }
        ?: authUid.trim().takeIf { it.isNotEmpty() }
        ?: if (session.isAdminSession) "admin_session" else "guest_session"
    }.distinctUntilChanged()

  val readAnnouncementIdsState: StateFlow<Set<String>> =
    buildReadAnnouncementIdsFlow(activeReaderKeyFlow)
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  val unreadAnnouncementIdsState: StateFlow<Set<String>> =
    buildUnreadAnnouncementIdsFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptySet())

  fun markAnnouncementAsRead(announcementId: String) = performMarkAnnouncementAsRead(announcementId)
  fun markAllAnnouncementsAsRead() = performMarkAllAnnouncementsAsRead()

  val optimisticUserReactions = MutableStateFlow<Map<String, OmniReactionType>>(emptyMap())

  val churchReactionsFlow: Flow<List<AnnouncementReaction>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList()) else repository.observeReactions(churchId)
    }

  val reactionSummariesState: StateFlow<Map<String, TargetReactionSummary>> =
    buildReactionSummariesFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

  internal val pendingOptimisticCommentDeltas = MutableStateFlow<Map<String, Int>>(emptyMap())

  val churchCommentsFlow: Flow<List<AnnouncementComment>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList()) else repository.observeChurchComments(churchId)
    }

  val commentCountsState: StateFlow<Map<String, Int>> =
    buildCommentCountsFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyMap())

  val notificationsState: StateFlow<UiState<List<ChurchNotification>>> =
    buildNotificationsUiStateFlow()
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UiState.Loading)

  val cachedOfflineQrToken: StateFlow<String?> =
    preferencesManager.cachedQrTokenFlow
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  internal val _memberQrBitmap = MutableStateFlow<Bitmap?>(null)
  val memberQrBitmap: StateFlow<Bitmap?> = _memberQrBitmap.asStateFlow()

  internal val _scannerOutcome = MutableStateFlow<ScannerValidationOutcome>(ScannerValidationOutcome.Idle)
  val scannerOutcome: StateFlow<ScannerValidationOutcome> = _scannerOutcome.asStateFlow()

  internal val _cardVerificationState =
    MutableStateFlow<UiState<PublicVerificationResolution?>>(UiState.Success(null))
  val cardVerificationState: StateFlow<UiState<PublicVerificationResolution?>> =
    _cardVerificationState.asStateFlow()

  internal var lastCardVerificationQuery: Pair<String, String?>? = null

  internal val _selectedScannerServiceId = MutableStateFlow<String?>(null)
  val selectedScannerServiceId: StateFlow<String?> = _selectedScannerServiceId.asStateFlow()

  internal val _actionFeedbackMessage = MutableStateFlow<String?>(null)
  val actionFeedbackMessage: StateFlow<String?> = _actionFeedbackMessage.asStateFlow()

  internal val _isSubmittingSetup = MutableStateFlow(false)
  val isSubmittingSetup: StateFlow<Boolean> = _isSubmittingSetup.asStateFlow()

  internal val _isAuthenticating = MutableStateFlow(false)
  val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

  init {
    initUserProfileAndQrSync()
    initAdminChurchAutoRebind()
    initDeaconUserProfileSync()
  }

  fun clearFeedbackMessage() { _actionFeedbackMessage.value = null }
  fun setFeedbackMessage(msg: String?) { _actionFeedbackMessage.value = msg }
  fun selectScannerService(serviceId: String) { _selectedScannerServiceId.value = serviceId }
  fun resetScannerOutcome() { _scannerOutcome.value = ScannerValidationOutcome.Idle }

  fun loginMemberWithPin(
    selectedChurchId: String?, idOrPhone: String, enteredPin: String,
    onSuccess: () -> Unit, onError: (String) -> Unit,
  ) = performLoginMemberWithPin(selectedChurchId, idOrPhone, enteredPin, onSuccess, onError)

  fun selectMatchedMemberSession(session: AuthenticatedMemberSession, onSuccess: () -> Unit = {}) =
    performSelectMatchedMemberSession(session, onSuccess)

  fun cancelMatchedMemberChurchSelection() { _pendingMemberMatchedSessions.value = emptyList() }
  fun unlockSavedBiometricSession(onSuccess: () -> Unit) = performUnlockSavedBiometricSession(onSuccess)
  fun toggleBiometricQuickUnlock(enabled: Boolean) = performToggleBiometricQuickUnlock(enabled)

  fun handleOptionalGoogleMemberLogin(
    selectedChurchId: String?, googleEmail: String,
    onDirectMemberLoginSuccess: () -> Unit, onNeedsPinVerificationPrompt: () -> Unit, onError: (String) -> Unit,
  ) = performOptionalGoogleMemberLogin(selectedChurchId, googleEmail, onDirectMemberLoginSuccess, onNeedsPinVerificationPrompt, onError)

  fun verifyPinAndLinkGoogleAccount(
    selectedChurchId: String?, idOrPhone: String, enteredPin: String,
    googleEmail: String, googleUid: String, googlePhotoUrl: String?,
    onSuccess: () -> Unit, onError: (String) -> Unit,
  ) = performVerifyPinAndLinkGoogleAccount(selectedChurchId, idOrPhone, enteredPin, googleEmail, googleUid, googlePhotoUrl, onSuccess, onError)

  fun linkGoogleToCurrentMember(
    googleEmail: String, googleUid: String, googlePhotoUrl: String?,
    onSuccess: () -> Unit, onError: (String) -> Unit,
  ) = performLinkGoogleToCurrentMember(googleEmail, googleUid, googlePhotoUrl, onSuccess, onError)

  fun loginAdminForChurch(
    selectedChurchId: String, enteredPasscode: String?, googleEmail: String?, googleUid: String?,
    onSuccess: () -> Unit, onError: (String) -> Unit,
  ) = performLoginAdminForChurch(selectedChurchId, enteredPasscode, googleEmail, googleUid, onSuccess, onError)

  fun logoutCurrentSession(onSuccess: () -> Unit) = performLogoutCurrentSession(onSuccess)

  fun completeChurchAndMemberSetup(
    createNewChurch: Boolean, churchNameInput: String, existingChurchIdInput: String,
    churchAddressInput: String, fullNameInput: String, departmentInput: String,
    ministryRoleInput: String, photoUrl: String?, churchCodeInput: String = "",
    adminPasscodeInput: String = "", memberPinInput: String = "1234", onSuccess: () -> Unit,
  ) = performCompleteChurchAndMemberSetup(
    createNewChurch, churchNameInput, existingChurchIdInput, churchAddressInput, fullNameInput,
    departmentInput, ministryRoleInput, photoUrl, churchCodeInput, adminPasscodeInput, memberPinInput, onSuccess,
  )

  fun setRememberAdminCredentialsEnabled(enabled: Boolean) {
    viewModelScope.launch { preferencesManager.setRememberAdminEnabled(enabled) }
  }

  fun signInAdminSmartWithEmailAndPassword(
    email: String, password: String, rememberMe: Boolean,
    onSuccess: () -> Unit = {}, onError: (String) -> Unit = {},
  ) = performSignInAdminSmartWithEmailAndPassword(email, password, rememberMe, onSuccess, onError)

  fun signInAdminSmartWithCurrentGoogleUser(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
    performSignInAdminSmartWithCurrentGoogleUser(onSuccess, onError)

  fun selectOwnedAdminChurch(church: Church, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) =
    performSelectOwnedAdminChurch(church, onSuccess, onError)

  fun cancelOwnedAdminChurchSelection() {
    _pendingAdminOwnedChurches.value = emptyList()
    viewModelScope.launch { repository.signOutFirebaseAuthOnly() }
  }

  fun signInAdminWithEmailAndPassword(
    email: String, password: String, selectedChurchId: String? = null,
    onSuccess: () -> Unit = {}, onError: (String) -> Unit = {},
  ) = performSignInAdminWithEmailAndPassword(email, password, selectedChurchId, onSuccess, onError)

  fun sendAdminPasswordResetEmail(email: String, onSuccess: (String) -> Unit, onError: (String) -> Unit) =
    performSendAdminPasswordResetEmail(email, onSuccess, onError)

  fun registerNewMemberByAdmin(
    church: Church, fullName: String, customMatrikil: String?, pin: String, phone: String?,
    department: String, ministryRole: String, onSuccess: (Member) -> Unit, onError: (String) -> Unit,
  ) = performRegisterNewMemberByAdmin(church, fullName, customMatrikil, pin, phone, department, ministryRole, onSuccess, onError)

  fun createService(
    churchId: String, churchName: String?, title: String, description: String? = null,
    dateFormatted: String = "", lateToleranceMinutes: Long, isOpenForAttendance: Boolean,
    isLive: Boolean, authorName: String, category: String = "dimanche_matin", date: String = "",
    startTime: String = "", endTime: String = "", theme: String? = null, passage: String? = null,
    leader: String? = null, preacher: String? = null, reader: String? = null,
    musicians: String? = null, ushers: String? = null, sendReminder24h: Boolean = false,
    imageUrl: String? = null,
  ) = performCreateService(
    churchId, churchName, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance,
    isLive, authorName, category, date, startTime, endTime, theme, passage, leader, preacher, reader,
    musicians, ushers, sendReminder24h, imageUrl,
  )

  fun updateService(
    serviceId: String, title: String, description: String?, dateFormatted: String = "",
    lateToleranceMinutes: Long, isOpenForAttendance: Boolean, isLive: Boolean = false,
    category: String = "dimanche_matin", date: String = "", startTime: String = "",
    endTime: String = "", theme: String? = null, passage: String? = null, leader: String? = null,
    preacher: String? = null, reader: String? = null, musicians: String? = null,
    ushers: String? = null, sendReminder24h: Boolean = false, imageUrl: String? = null,
  ) = performUpdateService(
    serviceId, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive,
    category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers,
    sendReminder24h, imageUrl,
  )

  fun toggleServiceAttendanceWindow(service: ChurchService, isOpen: Boolean) =
    performToggleServiceAttendanceWindow(service, isOpen)

  fun deleteService(serviceId: String) = performDeleteService(serviceId)

  fun createAnnouncement(
    churchId: String, title: String, content: String, isPinned: Boolean, authorName: String,
    department: String = "Depatman Administrasyon", category: String = "anons", date: String? = null,
    targetAudience: String = "Tout moun", showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info", isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null, posterUrl: String? = null, serviceId: String? = null,
  ) = performCreateAnnouncement(
    churchId, title, content, isPinned, authorName, department, category, date, targetAudience,
    showToastOnLogin, toastAlertLevel, isScheduledNotification, scheduledNotificationDate, posterUrl, serviceId,
  )

  fun updateAnnouncement(
    announcementId: String, title: String, content: String,
    department: String = "Depatman Administrasyon", category: String = "anons", date: String? = null,
    targetAudience: String = "Tout moun", isPinned: Boolean = false, showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info", isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null, posterUrl: String? = null, serviceId: String? = null,
  ) = performUpdateAnnouncement(
    announcementId, title, content, department, category, date, targetAudience, isPinned,
    showToastOnLogin, toastAlertLevel, isScheduledNotification, scheduledNotificationDate, posterUrl, serviceId,
  )

  fun deleteAnnouncement(announcementId: String) = performDeleteAnnouncement(announcementId)

  fun reactToFeedItem(
    targetId: String, isServiceTarget: Boolean, churchId: String,
    reactionType: OmniReactionType = OmniReactionType.PRAY, isCommentTarget: Boolean = false,
  ) = performReactToFeedItem(targetId, isServiceTarget, churchId, reactionType, isCommentTarget)

  fun observeCommentsForTarget(churchId: String, targetId: String): Flow<List<AnnouncementComment>> =
    repository.observeComments(resolveCanonicalChurchId(churchId), targetId)

  fun observeAttendancesForService(churchId: String, serviceId: String): Flow<List<AttendanceRecord>> =
    repository.observeServiceAttendances(resolveCanonicalChurchId(churchId), serviceId)

  fun postComment(targetId: String, isServiceTarget: Boolean, content: String) =
    performPostComment(targetId, isServiceTarget, content)

  fun processAttendanceScanOrCode(
    rawTokenOrCode: String, selectedService: ChurchService, checkInMethod: String = "qr_scanner",
  ) = performProcessAttendanceScanOrCode(rawTokenOrCode, selectedService, checkInMethod)

  fun verifyMemberCard(rawTokenOrCode: String, fallbackChurchId: String? = null) =
    performVerifyMemberCard(rawTokenOrCode, fallbackChurchId)

  fun openDirectMemberVerification(member: Member, church: Church? = null) =
    performOpenDirectMemberVerification(member, church)

  fun retryLastCardVerification() {
    val last = lastCardVerificationQuery ?: return
    verifyMemberCard(last.first, last.second)
  }

  fun clearCardVerificationState() { _cardVerificationState.value = UiState.Success(null) }

  fun bindNfcCardToMember(
    memberDocId: String,
    nfcCardUid: String,
    languageCode: String = "ht",
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
  ) = performBindNfcCardToMember(memberDocId, nfcCardUid, languageCode, onSuccess, onError)

  fun markNotificationAsRead(notificationId: String) = performMarkNotificationAsRead(notificationId)
  fun markAllNotificationsRead(notifications: List<ChurchNotification>) = performMarkAllNotificationsRead(notifications)

  fun updateMemberProfileInfo(
    fullName: String, department: String, ministryRole: String, onSuccess: () -> Unit = {},
  ) = performUpdateMemberProfileInfo(fullName, department, ministryRole, onSuccess)

  fun updateChurchMemberByAdmin(
    memberDocId: String, fullName: String, department: String, ministryRole: String,
    phone: String? = null, titlePrefix: String? = null, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {},
  ) = performUpdateChurchMemberByAdmin(memberDocId, fullName, department, ministryRole, phone, titlePrefix, onSuccess, onError)

  fun updateMemberPhoto(photoDataUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
    performUpdateMemberPhoto(photoDataUrl, onSuccess, onError)

  fun updateAdminPhoto(photoDataUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
    performUpdateAdminPhoto(photoDataUrl, onSuccess, onError)

  fun updateChurchLogo(logoDataUrl: String, onSuccess: () -> Unit, onError: (String) -> Unit) =
    performUpdateChurchLogo(logoDataUrl, onSuccess, onError)

  companion object {
    private const val TAG = "OmniChurchViewModel"
    private const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
