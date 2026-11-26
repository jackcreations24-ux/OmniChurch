package com.zoutiw.omnichurch.presentation.navigation

import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal fun OmniChurchViewModel.buildChurchesUiStateFlow(
  refreshTrigger: Flow<Int>,
): Flow<UiState<List<Church>>> =
  refreshTrigger
    .flatMapLatest {
      repository.observeChurchesResult()
        .map<Result<List<Church>>, UiState<List<Church>>> { res ->
          res.getOrNull()?.let { UiState.Success(it) }
            ?: UiState.Error(
              res.exceptionOrNull()?.localizedMessage
                ?: "Erè pandan chajman lis legliz yo depi nan sèvè a."
            )
        }
        .onStart { emit(UiState.Loading) }
    }
    .catch { err -> emit(UiState.Error(err.localizedMessage ?: "Erè pandan chajman lis legliz yo.")) }

internal fun OmniChurchViewModel.buildCurrentMemberUiStateFlow(): Flow<UiState<Member?>> =
  sessionState
    .flatMapLatest { state ->
      val docId = state.activeMemberDocId
      val memberId = state.activeMemberId
      if (docId.isBlank() && memberId.isBlank()) {
        flowOf(UiState.Success(null))
      } else {
        repository.observeSessionMember(state.activeChurchId, docId, memberId)
          .map { member -> UiState.Success(member) as UiState<Member?> }
      }
    }
    .catch { err -> emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman manm nan")) }

internal fun OmniChurchViewModel.buildChurchMembersUiStateFlow(
  memberRefreshTrigger: Flow<Int>,
): Flow<UiState<List<Member>>> =
  combine(
    buildResolvedChurchQueryTargetFlow(),
    memberRefreshTrigger,
    sessionState,
    firestoreUserProfileFlow,
    currentMemberState,
  ) { target, _, session, profile, memberState ->
    val currentMember = (memberState as? UiState.Success)?.data
    val isAdmin = session.isAdminSession || profile?.isAdminOrSuperAdmin == true
    val isReception =
      com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.isActiveReception(currentMember) ||
        profile?.role?.trim()?.equals("usher", ignoreCase = true) == true
    Triple(target.first, target.second, isAdmin || isReception)
  }
    .flatMapLatest { (churchId, churchCode, canSubscribeFullMembers) ->
      // Strict PII Isolation: Pure Deacons and regular members NEVER open a full members real-time listener.
      if (!canSubscribeFullMembers || (churchId.isBlank() && churchCode.isBlank())) {
        flowOf<UiState<List<Member>>>(UiState.Success(emptyList()))
      } else {
        repository.observeChurchMembersResult(churchId, churchCode)
          .map<Result<List<Member>>, UiState<List<Member>>> { res ->
            res.getOrNull()?.let { UiState.Success(it) }
              ?: UiState.Error(
                res.exceptionOrNull()?.localizedMessage ?: "Erè pandan chajman lis manm legliz la."
              )
          }
          .onStart { emit(UiState.Loading) }
      }
    }
    .catch { err ->
      emit(UiState.Error(err.localizedMessage ?: "Erè pandan chajman lis manm legliz la."))
    }

internal fun OmniChurchViewModel.buildPublicMemberDisplayNamesFlow(): Flow<List<Member>> =
  combine(
    buildResolvedChurchQueryTargetFlow(),
    sessionState,
    firestoreUserProfileFlow,
    currentMemberState,
  ) { target, session, profile, memberState ->
    val currentMember = (memberState as? UiState.Success)?.data
    val isAdmin = session.isAdminSession || profile?.isAdminOrSuperAdmin == true
    val isActiveDeacon = com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.isActiveDeacon(currentMember)
    Triple(target.first, target.second, !isAdmin && isActiveDeacon)
  }
    .flatMapLatest { (churchId, churchCode, needsDeaconPickerNames) ->
      if (!needsDeaconPickerNames || (churchId.isBlank() && churchCode.isBlank())) {
        flowOf(emptyList())
      } else {
        kotlinx.coroutines.flow.flow {
          emit(repository.fetchPublicMemberDisplayNames(churchId, churchCode))
        }
      }
    }
    .catch { emit(emptyList()) }

internal fun OmniChurchViewModel.buildAnnouncementsUiStateFlow(
  announcementsRefreshTrigger: Flow<Int>,
): Flow<UiState<List<Announcement>>> =
  combine(activeChurchIdFlow, announcementsRefreshTrigger) { churchId, _ -> churchId }
    .flatMapLatest { churchId ->
      if (churchId.isNullOrBlank()) {
        flowOf<UiState<List<Announcement>>>(UiState.Success(emptyList()))
      } else {
        repository.observeAnnouncementsResult(churchId)
          .map<Result<List<Announcement>>, UiState<List<Announcement>>> { res ->
            res.getOrNull()?.let { UiState.Success(it) }
              ?: UiState.Error(res.exceptionOrNull()?.localizedMessage ?: "Erè nan chajman anons yo")
          }
          .onStart { emit(UiState.Loading) }
      }
    }
    .catch { err -> emit(UiState.Error(err.localizedMessage ?: "Erè nan chajman anons yo")) }

internal fun OmniChurchViewModel.buildReadAnnouncementIdsFlow(
  activeReaderKeyFlow: Flow<String>,
): Flow<Set<String>> =
  combine(activeChurchIdFlow, activeReaderKeyFlow) { churchId, readerKey ->
    churchId?.trim().orEmpty() to readerKey
  }.flatMapLatest { (churchId, readerKey) ->
    if (churchId.isEmpty() || readerKey.isEmpty()) flowOf(emptySet())
    else {
      val scopeKey = readerScopeKey(churchId, readerKey)
      combine(
        preferencesManager.observeReadAnnouncementIds(churchId, readerKey),
        optimisticReadAnnouncementIds,
      ) { persistedIds, optimisticMap -> persistedIds + (optimisticMap[scopeKey] ?: emptySet()) }
    }
  }

internal fun OmniChurchViewModel.buildNotificationsUiStateFlow(): Flow<UiState<List<ChurchNotification>>> =
  combine(activeChurchIdFlow, activeAuthUidFlow) { churchId, uid -> churchId to uid }
    .flatMapLatest { (churchId, uid) ->
      repository.observeNotifications(uid, churchId)
        .map { UiState.Success(it) as UiState<List<ChurchNotification>> }
    }
    .catch { emit(UiState.Success(emptyList())) }
