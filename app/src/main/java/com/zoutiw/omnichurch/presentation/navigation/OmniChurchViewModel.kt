package com.zoutiw.omnichurch.presentation.navigation

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.RememberedAdminCredentials
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
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.repositories.AuthRepository
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
import kotlinx.coroutines.flow.update
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

  val rememberedAdminCredentials: StateFlow<RememberedAdminCredentials> =
    preferencesManager.rememberedAdminCredentialsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = RememberedAdminCredentials(),
    )

  private val _pendingAdminOwnedChurches = MutableStateFlow<List<Church>>(emptyList())
  val pendingAdminOwnedChurches: StateFlow<List<Church>> = _pendingAdminOwnedChurches.asStateFlow()

  private val churchesRefreshTrigger = MutableStateFlow(0)

  fun retryLoadChurches() {
    churchesRefreshTrigger.value += 1
  }

  val churchesUiState: StateFlow<UiState<List<Church>>> =
    churchesRefreshTrigger
      .flatMapLatest {
        repository
          .observeChurchesResult()
          .map<Result<List<Church>>, UiState<List<Church>>> { result ->
            val list = result.getOrNull()
            if (list != null) {
              UiState.Success(list)
            } else {
              val err = result.exceptionOrNull()
              UiState.Error(
                err?.localizedMessage ?: "Erè pandan chajman lis legliz yo depi nan sèvè a."
              )
            }
          }
          .onStart { emit(UiState.Loading) }
      }
      .catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Erè pandan chajman lis legliz yo."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val churchesState: StateFlow<List<Church>> =
    churchesUiState
      .map { state -> (state as? UiState.Success)?.data ?: emptyList() }
      .stateIn(
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

  val memberAttendanceHistoryState: StateFlow<List<MemberAttendanceHistoryItem>> =
    currentMemberState.flatMapLatest { memberState ->
      val member = (memberState as? UiState.Success)?.data
      if (member == null || member.id.isBlank()) {
        flowOf(emptyList())
      } else {
        repository.attendanceRepository.observeMemberAttendanceHistory(member.churchId, member.id)
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
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

  private val memberRefreshTrigger = MutableStateFlow(0)

  fun retryLoadChurchMembers() {
    memberRefreshTrigger.value += 1
  }

  private val resolvedChurchQueryTargetFlow: Flow<Pair<String, String>> =
    combine(
      activeChurchIdFlow,
      activeChurchState,
      churchesState,
      firestoreUserProfileFlow,
    ) { sessionChurchId, activeChurch, allChurches, profile ->
      if (activeChurch != null && activeChurch.id.isNotBlank()) {
        return@combine activeChurch.id.trim() to activeChurch.code.trim()
      }
      val rawCandidate =
        sessionChurchId?.trim()?.takeIf { it.isNotBlank() }
          ?: profile?.churchId?.trim()?.takeIf { it.isNotBlank() }
          ?: ""
      if (rawCandidate.isEmpty()) {
        return@combine "" to ""
      }
      val matchedChurch =
        allChurches.firstOrNull { church ->
          church.id == rawCandidate ||
            church.id.equals(rawCandidate, ignoreCase = true) ||
            (church.code.isNotBlank() && church.code.equals(rawCandidate, ignoreCase = true))
        }
      if (matchedChurch != null) {
        matchedChurch.id.trim() to matchedChurch.code.trim()
      } else {
        rawCandidate to ""
      }
    }.distinctUntilChanged()

  val churchMembersUiState: StateFlow<UiState<List<Member>>> =
    combine(resolvedChurchQueryTargetFlow, memberRefreshTrigger) { target, _ -> target }
      .flatMapLatest { (churchId, churchCode) ->
        if (churchId.isBlank() && churchCode.isBlank()) {
          flowOf<UiState<List<Member>>>(UiState.Success(emptyList()))
        } else {
          repository
            .observeChurchMembersResult(churchId, churchCode)
            .map<Result<List<Member>>, UiState<List<Member>>> { result ->
              val members = result.getOrNull()
              if (members != null) {
                UiState.Success(members)
              } else {
                val err = result.exceptionOrNull()
                UiState.Error(
                  err?.localizedMessage ?: "Erè pandan chajman lis manm legliz la."
                )
              }
            }
            .onStart { emit(UiState.Loading) }
        }
      }
      .catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Erè pandan chajman lis manm legliz la."))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val churchMembersState: StateFlow<List<Member>> =
    churchMembersUiState
      .map { state -> (state as? UiState.Success)?.data ?: emptyList() }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList(),
      )

  private val announcementsRefreshTrigger = MutableStateFlow(0)

  fun retryLoadAnnouncements() {
    announcementsRefreshTrigger.value += 1
  }

  val announcementsState: StateFlow<UiState<List<Announcement>>> =
    combine(activeChurchIdFlow, announcementsRefreshTrigger) { churchId, _ -> churchId }
      .flatMapLatest { churchId ->
        if (churchId.isNullOrBlank()) {
          flowOf<UiState<List<Announcement>>>(UiState.Success(emptyList()))
        } else {
          repository
            .observeAnnouncementsResult(churchId)
            .map<Result<List<Announcement>>, UiState<List<Announcement>>> { result ->
              val list = result.getOrNull()
              if (list != null) {
                UiState.Success(list)
              } else {
                val err = result.exceptionOrNull()
                UiState.Error(
                  err?.localizedMessage ?: "Erè nan chajman anons yo"
                )
              }
            }
            .onStart { emit(UiState.Loading) }
        }
      }
      .catch { err ->
        emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman anons yo"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  private val optimisticReadAnnouncementIds =
    MutableStateFlow<Map<String, Set<String>>>(emptyMap())

  private fun readerScopeKey(churchId: String, readerKey: String): String =
    "${churchId.trim()}::${readerKey.trim()}"

  private val activeReaderKeyFlow: Flow<String> =
    combine(sessionState, activeAuthUidFlow) { session, authUid ->
      session.activeMemberDocId.trim().takeIf { it.isNotEmpty() }
        ?: session.activeMemberId.trim().takeIf { it.isNotEmpty() }
        ?: authUid.trim().takeIf { it.isNotEmpty() }
        ?: if (session.isAdminSession) "admin_session" else "guest_session"
    }.distinctUntilChanged()

  val readAnnouncementIdsState: StateFlow<Set<String>> =
    combine(activeChurchIdFlow, activeReaderKeyFlow) { churchId, readerKey ->
      (churchId?.trim().orEmpty()) to readerKey
    }.flatMapLatest { (churchId, readerKey) ->
      if (churchId.isEmpty() || readerKey.isEmpty()) {
        flowOf(emptySet())
      } else {
        val scopeKey = readerScopeKey(churchId, readerKey)
        combine(
          preferencesManager.observeReadAnnouncementIds(churchId, readerKey),
          optimisticReadAnnouncementIds,
        ) { persistedIds, optimisticMap ->
          persistedIds + (optimisticMap[scopeKey] ?: emptySet())
        }
      }
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptySet(),
    )

  val unreadAnnouncementIdsState: StateFlow<Set<String>> =
    combine(
      announcementsState,
      readAnnouncementIdsState,
      sessionState,
    ) { announcementsUi, readIds, session ->
      val list = (announcementsUi as? UiState.Success)?.data ?: return@combine emptySet()
      val isAdmin = session.isAdminSession
      val actorUid = effectiveActorUid()
      list
        .asSequence()
        .filter { ann -> isAdmin || ann.isPublished }
        .filter { ann ->
          // Don't mark an admin's own published announcements as unread for them
          !(isAdmin && actorUid.isNotEmpty() && ann.authorId.trim() == actorUid)
        }
        .map { it.id.trim() }
        .filter { id -> id.isNotEmpty() && id !in readIds }
        .toSet()
    }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptySet(),
    )

  private fun currentReaderIdentity(): Pair<String, String>? {
    val session = sessionState.value
    val churchId =
      resolveCanonicalChurchId(
        session.activeChurchId.trim().ifBlank {
          (currentMemberState.value as? UiState.Success)?.data?.churchId?.trim().orEmpty()
        }
      )
    if (churchId.isEmpty()) return null
    val readerKey =
      session.activeMemberDocId.trim().takeIf { it.isNotEmpty() }
        ?: session.activeMemberId.trim().takeIf { it.isNotEmpty() }
        ?: effectiveActorUid().takeIf { it.isNotEmpty() }
        ?: if (session.isAdminSession) "admin_session" else "guest_session"
    return churchId to readerKey
  }

  fun markAnnouncementAsRead(announcementId: String) {
    val cleanId = announcementId.trim()
    if (cleanId.isEmpty()) return
    val (churchId, readerKey) = currentReaderIdentity() ?: return
    val scopeKey = readerScopeKey(churchId, readerKey)
    optimisticReadAnnouncementIds.update { current ->
      current + (scopeKey to ((current[scopeKey] ?: emptySet()) + cleanId))
    }
    viewModelScope.launch {
      preferencesManager.markAnnouncementRead(
        churchId = churchId,
        memberKey = readerKey,
        announcementId = cleanId,
      )
    }
  }

  fun markAllAnnouncementsAsRead() {
    val announcements = (announcementsState.value as? UiState.Success)?.data ?: return
    val ids = announcements.map { it.id.trim() }.filter { it.isNotEmpty() }
    if (ids.isEmpty()) return
    val (churchId, readerKey) = currentReaderIdentity() ?: return
    val scopeKey = readerScopeKey(churchId, readerKey)
    optimisticReadAnnouncementIds.update { current ->
      current + (scopeKey to ((current[scopeKey] ?: emptySet()) + ids))
    }
    viewModelScope.launch {
      preferencesManager.markAnnouncementsRead(
        churchId = churchId,
        memberKey = readerKey,
        announcementIds = ids,
      )
    }
  }

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

  private val pendingOptimisticCommentDeltas = MutableStateFlow<Map<String, Int>>(emptyMap())

  val churchCommentsFlow: Flow<List<AnnouncementComment>> =
    activeChurchIdFlow.flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) flowOf(emptyList())
      else repository.observeChurchComments(churchId)
    }

  val commentCountsState: StateFlow<Map<String, Int>> =
    combine(churchCommentsFlow, pendingOptimisticCommentDeltas) { comments, optimisticDeltas ->
      val counts = mutableMapOf<String, Int>()
      for (comment in comments) {
        val cleanTargetId = comment.targetId.trim()
        if (cleanTargetId.isNotEmpty()) {
          counts[cleanTargetId] = (counts[cleanTargetId] ?: 0) + 1
        }
      }
      for ((targetId, delta) in optimisticDeltas) {
        if (delta > 0) {
          counts[targetId] = (counts[targetId] ?: 0) + delta
        }
      }
      counts
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

  private val _cardVerificationState =
    MutableStateFlow<UiState<PublicVerificationResolution?>>(UiState.Success(null))
  val cardVerificationState: StateFlow<UiState<PublicVerificationResolution?>> =
    _cardVerificationState.asStateFlow()

  private var lastCardVerificationQuery: Pair<String, String?>? = null

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
    // Auto-heal stale users/{uid}.churchId and auto-rebind cached churchId if the signed-in admin
    // uniquely owns a different church (preventing owner_mismatch / PERMISSION_DENIED).
    viewModelScope.launch {
      combine(activeAuthUidFlow, sessionState, activeChurchState, firestoreUserProfileFlow) { uid, session, church, profile ->
        listOf(uid, session, church, profile)
      }
        .distinctUntilChanged { old, new ->
          old[0] == new[0] &&
            (old[1] as OmniSessionState).activeChurchId == (new[1] as OmniSessionState).activeChurchId &&
            (old[1] as OmniSessionState).activeRole == (new[1] as OmniSessionState).activeRole &&
            (old[2] as? Church)?.id == (new[2] as? Church)?.id &&
            (old[3] as? UserProfile)?.churchId == (new[3] as? UserProfile)?.churchId
        }
        .collect { items ->
          val uid = items[0] as String
          val session = items[1] as OmniSessionState
          val church = items[2] as? Church
          val profile = items[3] as? UserProfile
          if (uid.isNotBlank() && session.isAdminSession && session.activeChurchId.isNotBlank()) {
            val activeUser = FirebaseAuth.getInstance().currentUser
            val eval =
              if (church != null && activeUser != null) {
                SessionLockManager.evaluateOwnership(church, activeUser)
              } else {
                null
              }
            if (eval != null && !eval.isAuthorized) {
              val rebound = repository.resolveAutoRebindChurchIfUniquelyOwned(session.activeChurchId)
              if (rebound != null) {
                preferencesManager.saveAuthenticatedSession(
                  activeChurchId = rebound.id,
                  activeRole = "admin",
                  activeMemberId = "",
                  activeMemberDocId = "",
                  memberName = rebound.name,
                )
                repository.syncUserChurchContext(rebound.id, rebound.name)
                return@collect
              }
            }
          }
          if (uid.isNotBlank() && church != null && church.id.isNotBlank()) {
            val profileChurchId = profile?.churchId?.trim().orEmpty()
            if (profile != null && profileChurchId != church.id.trim()) {
              repository.syncUserChurchContext(church.id.trim(), church.name.trim())
            }
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

  fun logoutCurrentSession(onSuccess: () -> Unit) {
    viewModelScope.launch {
      activeAuthUidFlow.value = ""
      _pendingAdminOwnedChurches.value = emptyList()
      optimisticReadAnnouncementIds.value = emptyMap()
      repository.authRepository.sessionLockManager.clearAllAdminLocks()
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

  fun setRememberAdminCredentialsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesManager.setRememberAdminEnabled(enabled)
    }
  }

  fun signInAdminSmartWithEmailAndPassword(
    email: String,
    password: String,
    rememberMe: Boolean,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
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
        .onFailure { err ->
          _pendingAdminOwnedChurches.value = emptyList()
          _isAuthenticating.value = false
          val msg = err.localizedMessage ?: "Imèl oswa modpas la pa kòrèk."
          _actionFeedbackMessage.value = msg
          onError(msg)
        }
    }
  }

  fun signInAdminSmartWithCurrentGoogleUser(
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
  ) {
    if (_isAuthenticating.value) return
    viewModelScope.launch {
      _isAuthenticating.value = true
      repository.signInAdminSmartWithCurrentGoogleUser()
        .onSuccess { resolution ->
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
        .onFailure { err ->
          _pendingAdminOwnedChurches.value = emptyList()
          _isAuthenticating.value = false
          val msg = err.localizedMessage ?: "Nou pa t kapab verifye kont Google Admin nan."
          _actionFeedbackMessage.value = msg
          onError(msg)
        }
    }
  }

  fun selectOwnedAdminChurch(
    church: Church,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
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

  fun cancelOwnedAdminChurchSelection() {
    _pendingAdminOwnedChurches.value = emptyList()
    viewModelScope.launch {
      repository.signOutFirebaseAuthOnly()
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

  private fun resolveCanonicalChurchId(rawChurchId: String): String {
    val candidate =
      rawChurchId.trim().ifBlank {
        activeChurchState.value?.id?.trim().orEmpty().ifBlank {
          sessionState.value.activeChurchId.trim()
        }
      }
    if (candidate.isEmpty()) return ""
    val active = activeChurchState.value
    if (active != null && (active.id.equals(candidate, ignoreCase = true) || active.code.equals(candidate, ignoreCase = true))) {
      return active.id.trim()
    }
    val matched =
      churchesState.value.firstOrNull { church ->
        church.id == candidate ||
          church.id.equals(candidate, ignoreCase = true) ||
          (church.code.isNotBlank() && church.code.equals(candidate, ignoreCase = true))
      }
    return matched?.id?.trim()?.takeIf { it.isNotEmpty() } ?: candidate
  }

  fun createService(
    churchId: String,
    churchName: String?,
    title: String,
    description: String? = null,
    dateFormatted: String = "",
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
    imageUrl: String? = null,
  ) {
    val cleanChurchId = resolveCanonicalChurchId(churchId)
    if (cleanChurchId.isEmpty()) {
      _actionFeedbackMessage.value = "Pa ka jwenn ID legliz la. Rekonekte epi eseye ankò."
      return
    }
    if (title.trim().isEmpty()) {
      _actionFeedbackMessage.value = "Tanpri antre tit sèvis la."
      return
    }
    viewModelScope.launch {
      val resolvedChurchName =
        churchName?.trim()?.takeIf { it.isNotBlank() }
          ?: activeChurchState.value?.name?.trim()?.takeIf { it.isNotBlank() }
          ?: "OmniChurch Pro"
      val effectiveDateFormatted =
        dateFormatted.trim().ifBlank {
          listOf(date.trim(), startTime.trim()).filter { it.isNotBlank() }.joinToString(" • ")
        }
      repository.createService(
        churchId = cleanChurchId,
        churchName = resolvedChurchName,
        title = title,
        description = description,
        dateFormatted = effectiveDateFormatted,
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
        imageUrl = imageUrl,
      ).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la kreye avèk siksè!"
      }.onFailure { err ->
        val isPermissionDenied =
          (err as? FirebaseFirestoreException)?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
            (err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true)
        if (isPermissionDenied) {
          val rebound = repository.resolveAutoRebindChurchIfUniquelyOwned(cleanChurchId)
          if (rebound != null) {
            preferencesManager.saveAuthenticatedSession(
              activeChurchId = rebound.id,
              activeRole = "admin",
              activeMemberId = "",
              activeMemberDocId = "",
              memberName = rebound.name,
            )
            repository.syncUserChurchContext(rebound.id, rebound.name)
            repository.createService(
              churchId = rebound.id,
              churchName = rebound.name.ifBlank { resolvedChurchName },
              title = title,
              description = description,
              dateFormatted = effectiveDateFormatted,
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
              imageUrl = imageUrl,
            ).onSuccess {
              _actionFeedbackMessage.value = "Sèvis la kreye avèk siksè!"
              return@launch
            }
          }
        }
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè kreyasyon sèvis."
      }
    }
  }

  fun updateService(
    serviceId: String,
    title: String,
    description: String?,
    dateFormatted: String = "",
    lateToleranceMinutes: Long,
    isOpenForAttendance: Boolean,
    isLive: Boolean = false,
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
  ) {
    viewModelScope.launch {
      val effectiveDateFormatted =
        dateFormatted.trim().ifBlank {
          listOf(date.trim(), startTime.trim()).filter { it.isNotBlank() }.joinToString(" • ")
        }
      repository.updateService(
        serviceId = serviceId,
        title = title,
        description = description,
        dateFormatted = effectiveDateFormatted,
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
        sendReminder24h = sendReminder24h,
        imageUrl = imageUrl,
      ).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la mete ajou avèk siksè!"
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan mizajou sèvis la."
      }
    }
  }

  fun toggleServiceAttendanceWindow(service: ChurchService, isOpen: Boolean) {
    viewModelScope.launch {
      repository.toggleServiceAttendanceStatus(service, isOpen).onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Pa ka chanje estati prezans lan."
      }
    }
  }

  fun deleteService(serviceId: String) {
    viewModelScope.launch {
      repository.deleteService(serviceId).onSuccess {
        _actionFeedbackMessage.value = "Sèvis la efase avèk siksè."
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan efasman sèvis la."
      }
    }
  }

  fun createAnnouncement(
    churchId: String,
    title: String,
    content: String,
    isPinned: Boolean,
    authorName: String,
    department: String = "Depatman Administrasyon",
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
    val cleanChurchId = resolveCanonicalChurchId(churchId)
    if (cleanChurchId.isEmpty()) {
      _actionFeedbackMessage.value = "Pa ka jwenn ID legliz la pou pibliye anons la."
      return
    }
    if (title.trim().isEmpty() || content.trim().isEmpty()) {
      _actionFeedbackMessage.value = "Tit ak kontni anons la obligatwa."
      return
    }
    viewModelScope.launch {
      repository.createAnnouncement(
        churchId = cleanChurchId,
        title = title,
        content = content,
        department = department.trim().ifBlank { "Depatman Administrasyon" },
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
      ).onSuccess { createdAnnouncementId ->
        markAnnouncementAsRead(createdAnnouncementId)
        _actionFeedbackMessage.value = "Anons la pibliye avèk siksè!"
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan piblikasyon anons la."
      }
    }
  }

  fun updateAnnouncement(
    announcementId: String,
    title: String,
    content: String,
    department: String = "Depatman Administrasyon",
    category: String = "anons",
    date: String? = null,
    targetAudience: String = "Tout moun",
    isPinned: Boolean = false,
    showToastOnLogin: Boolean = false,
    toastAlertLevel: String = "info",
    isScheduledNotification: Boolean = false,
    scheduledNotificationDate: String? = null,
    posterUrl: String? = null,
    serviceId: String? = null,
  ) {
    viewModelScope.launch {
      repository.updateAnnouncement(
        announcementId = announcementId,
        title = title,
        content = content,
        department = department,
        category = category,
        date = date,
        targetAudience = targetAudience,
        isPinned = isPinned,
        showToastOnLogin = showToastOnLogin,
        toastAlertLevel = toastAlertLevel,
        isScheduledNotification = isScheduledNotification,
        scheduledNotificationDate = scheduledNotificationDate,
        posterUrl = posterUrl,
        serviceId = serviceId,
      ).onSuccess {
        _actionFeedbackMessage.value = "Anons la mete ajou avèk siksè!"
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan mizajou anons la."
      }
    }
  }

  fun deleteAnnouncement(announcementId: String) {
    viewModelScope.launch {
      repository.deleteAnnouncement(announcementId).onSuccess {
        _actionFeedbackMessage.value = "Anons la efase avèk siksè."
      }.onFailure { err ->
        _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan efasman anons la."
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
    val currentMember = (currentMemberState.value as? UiState.Success)?.data
    val userName =
      currentUserProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentMember?.effectiveFullName?.takeIf { it.isNotBlank() }
        ?: if (sessionState.value.isAdminSession) "Admin" else "Manm OmniChurch"
    optimisticUserReactions.value = optimisticUserReactions.value + (targetId to reactionType)
    viewModelScope.launch {
      repository.reactToTarget(
        targetId = targetId,
        isServiceTarget = isServiceTarget,
        churchId = resolveCanonicalChurchId(churchId),
        userName = userName,
        reactionType = reactionType.key,
        actorId = effectiveActorUid(),
        isCommentTarget = isCommentTarget,
      )
    }
  }

  fun observeCommentsForTarget(churchId: String, targetId: String): Flow<List<AnnouncementComment>> =
    repository.observeComments(resolveCanonicalChurchId(churchId), targetId)

  fun observeAttendancesForService(churchId: String, serviceId: String): Flow<List<AttendanceRecord>> =
    repository.observeServiceAttendances(resolveCanonicalChurchId(churchId), serviceId)

  fun postComment(targetId: String, isServiceTarget: Boolean, content: String) {
    val cleanTargetId = targetId.trim()
    val currentUserProfile = (userProfileState.value as? UiState.Success)?.data
    val currentMember = (currentMemberState.value as? UiState.Success)?.data
    val churchId =
      resolveCanonicalChurchId(
        sessionState.value.activeChurchId.trim().ifBlank {
          currentMember?.churchId?.trim().orEmpty().ifBlank {
            currentUserProfile?.churchId?.trim().orEmpty()
          }
        }
      )
    if (cleanTargetId.isEmpty() || churchId.isEmpty() || content.isBlank()) return
    val authorName =
      currentUserProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentMember?.effectiveFullName?.takeIf { it.isNotBlank() }
        ?: if (sessionState.value.isAdminSession) "Admin" else "Manm OmniChurch"
    val authorPhoto = currentUserProfile?.photoUrl ?: currentMember?.effectivePhotoUrl
    pendingOptimisticCommentDeltas.update { current ->
      current + (cleanTargetId to ((current[cleanTargetId] ?: 0) + 1))
    }
    viewModelScope.launch {
      try {
        repository.addComment(
          targetId = cleanTargetId,
          isServiceTarget = isServiceTarget,
          churchId = churchId,
          userName = authorName,
          userPhotoUrl = authorPhoto,
          content = content,
          actorId = effectiveActorUid(),
        ).onFailure { err ->
          _actionFeedbackMessage.value = err.localizedMessage ?: "Pa ka poste kòmantè a."
        }
      } finally {
        pendingOptimisticCommentDeltas.update { current ->
          val remaining = (current[cleanTargetId] ?: 0) - 1
          if (remaining > 0) current + (cleanTargetId to remaining)
          else current - cleanTargetId
        }
      }
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

  fun verifyMemberCard(
    rawTokenOrCode: String,
    fallbackChurchId: String? = null,
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

  fun openDirectMemberVerification(
    member: Member,
    church: Church? = null,
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

  fun retryLastCardVerification() {
    val last = lastCardVerificationQuery ?: return
    verifyMemberCard(last.first, last.second)
  }

  fun clearCardVerificationState() {
    _cardVerificationState.value = UiState.Success(null)
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
