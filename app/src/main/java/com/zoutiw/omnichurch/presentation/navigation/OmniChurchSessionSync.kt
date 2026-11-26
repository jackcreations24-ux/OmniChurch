package com.zoutiw.omnichurch.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.zoutiw.omnichurch.core.utilities.QrCodeGenerator
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.AnnouncementReaction
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

internal fun OmniChurchViewModel.effectiveActorUid(): String =
  activeAuthUidFlow.value.ifBlank { currentUserId }.trim()

internal fun readerScopeKey(churchId: String, readerKey: String): String =
  "${churchId.trim()}::${readerKey.trim()}"

internal fun OmniChurchViewModel.currentReaderIdentity(): Pair<String, String>? {
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

internal fun OmniChurchViewModel.resolveCanonicalChurchId(rawChurchId: String): String {
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

internal fun OmniChurchViewModel.buildResolvedChurchQueryTargetFlow(): Flow<Pair<String, String>> =
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

internal fun OmniChurchViewModel.buildUnreadAnnouncementIdsFlow(): Flow<Set<String>> =
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
  }

internal fun OmniChurchViewModel.buildReactionSummariesFlow(): Flow<Map<String, TargetReactionSummary>> =
  combine(churchReactionsFlow, optimisticUserReactions) { reactions: List<AnnouncementReaction>, optimisticMap ->
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
  }

internal fun OmniChurchViewModel.buildCommentCountsFlow(): Flow<Map<String, Int>> =
  combine(churchCommentsFlow, pendingOptimisticCommentDeltas) { comments: List<AnnouncementComment>, optimisticDeltas ->
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
  }

internal fun OmniChurchViewModel.initUserProfileAndQrSync() {
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
      member?.standardizedQrPayload
        ?.takeIf { it.isNotBlank() }
        ?: member?.effectiveBarcodeToken
          ?.takeIf { it.isNotBlank() }
        ?: offlineToken
          ?.takeIf { !it.isNullOrBlank() }
          .orEmpty()
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

internal fun OmniChurchViewModel.initAdminChurchAutoRebind() {
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

internal fun OmniChurchViewModel.initDeaconUserProfileSync() {
  viewModelScope.launch {
    combine(activeAuthUidFlow, currentMemberState, activeChurchState) { uid, memberUi, church ->
      Triple(uid.trim(), (memberUi as? UiState.Success)?.data, church)
    }
      .distinctUntilChanged { old, new ->
        old.first == new.first &&
          old.second?.id == new.second?.id &&
          old.second?.role == new.second?.role &&
          old.second?.status == new.second?.status &&
          old.second?.memberType == new.second?.memberType &&
          old.third?.id == new.third?.id
      }
      .collect { (uid, member, church) ->
        if (uid.isNotEmpty() && member != null && com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.isActiveDeacon(member)) {
          repository.syncDeaconUserProfile(member = member, church = church)
        }
      }
  }
}
