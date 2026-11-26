package com.zoutiw.omnichurch.presentation.navigation

import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private fun OmniChurchViewModel.isCurrentSessionAdmin(): Boolean {
  val profile = (userProfileState.value as? UiState.Success)?.data
  return sessionState.value.isAdminSession || profile?.isAdminOrSuperAdmin == true
}

private fun OmniChurchViewModel.activeDeaconMember(): com.zoutiw.omnichurch.data.models.Member? {
  if (isCurrentSessionAdmin()) return null
  val member = (currentMemberState.value as? UiState.Success)?.data ?: return null
  return member.takeIf { OmniRoleEvaluator.isActiveDeacon(it) }
}

internal fun OmniChurchViewModel.performMarkAnnouncementAsRead(announcementId: String) {
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

internal fun OmniChurchViewModel.performMarkAllAnnouncementsAsRead() {
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

internal fun OmniChurchViewModel.performCreateService(
  churchId: String,
  churchName: String?,
  title: String,
  description: String?,
  dateFormatted: String,
  lateToleranceMinutes: Long,
  isOpenForAttendance: Boolean,
  isLive: Boolean,
  authorName: String,
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
  sendReminder24h: Boolean,
  imageUrl: String?,
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
    val deaconMember = activeDeaconMember()
    val effectiveAuthorName =
      if (deaconMember != null) {
        OmniRoleEvaluator.defaultDeaconAuthorName(deaconMember)
      } else {
        authorName
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
      authorName = effectiveAuthorName,
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

internal fun OmniChurchViewModel.performUpdateService(
  serviceId: String,
  title: String,
  description: String?,
  dateFormatted: String,
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
  sendReminder24h: Boolean,
  imageUrl: String?,
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

internal fun OmniChurchViewModel.performToggleServiceAttendanceWindow(
  service: ChurchService,
  isOpen: Boolean,
) {
  viewModelScope.launch {
    repository.toggleServiceAttendanceStatus(service, isOpen).onFailure { err ->
      _actionFeedbackMessage.value = err.localizedMessage ?: "Pa ka chanje estati prezans lan."
    }
  }
}

internal fun OmniChurchViewModel.performDeleteService(serviceId: String) {
  val requireDeaconOwnership = !isCurrentSessionAdmin()
  viewModelScope.launch {
    repository.deleteService(serviceId, requireDeaconOwnership = requireDeaconOwnership).onSuccess {
      _actionFeedbackMessage.value = "Sèvis la efase avèk siksè."
    }.onFailure { err ->
      _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan efasman sèvis la."
    }
  }
}

internal fun OmniChurchViewModel.performCreateAnnouncement(
  churchId: String,
  title: String,
  content: String,
  isPinned: Boolean,
  authorName: String,
  department: String,
  category: String,
  date: String?,
  targetAudience: String,
  showToastOnLogin: Boolean,
  toastAlertLevel: String,
  isScheduledNotification: Boolean,
  scheduledNotificationDate: String?,
  posterUrl: String?,
  serviceId: String?,
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
    val deaconMember = activeDeaconMember()
    val effectiveAuthorName =
      if (deaconMember != null) {
        OmniRoleEvaluator.defaultDeaconAuthorName(deaconMember)
      } else {
        authorName
      }
    val effectiveDepartment =
      if (deaconMember != null) {
        OmniRoleEvaluator.DEFAULT_DEACON_DEPARTMENT
      } else {
        department.trim().ifBlank { "Depatman Administrasyon" }
      }
    repository.createAnnouncement(
      churchId = cleanChurchId,
      title = title,
      content = content,
      department = effectiveDepartment,
      isPinned = isPinned,
      authorName = effectiveAuthorName,
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

internal fun OmniChurchViewModel.performUpdateAnnouncement(
  announcementId: String,
  title: String,
  content: String,
  department: String,
  category: String,
  date: String?,
  targetAudience: String,
  isPinned: Boolean,
  showToastOnLogin: Boolean,
  toastAlertLevel: String,
  isScheduledNotification: Boolean,
  scheduledNotificationDate: String?,
  posterUrl: String?,
  serviceId: String?,
) {
  val requireDeaconOwnership = !isCurrentSessionAdmin()
  val effectiveDepartment =
    if (activeDeaconMember() != null) OmniRoleEvaluator.DEFAULT_DEACON_DEPARTMENT else department
  viewModelScope.launch {
    repository.updateAnnouncement(
      announcementId = announcementId,
      title = title,
      content = content,
      department = effectiveDepartment,
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
      requireDeaconOwnership = requireDeaconOwnership,
    ).onSuccess {
      _actionFeedbackMessage.value = "Anons la mete ajou avèk siksè!"
    }.onFailure { err ->
      _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan mizajou anons la."
    }
  }
}

internal fun OmniChurchViewModel.performDeleteAnnouncement(announcementId: String) {
  val requireDeaconOwnership = !isCurrentSessionAdmin()
  viewModelScope.launch {
    repository.deleteAnnouncement(announcementId, requireDeaconOwnership = requireDeaconOwnership).onSuccess {
      _actionFeedbackMessage.value = "Anons la efase avèk siksè."
    }.onFailure { err ->
      _actionFeedbackMessage.value = err.localizedMessage ?: "Erè pandan efasman anons la."
    }
  }
}

internal fun OmniChurchViewModel.performReactToFeedItem(
  targetId: String,
  isServiceTarget: Boolean,
  churchId: String,
  reactionType: OmniReactionType,
  isCommentTarget: Boolean,
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

internal fun OmniChurchViewModel.performPostComment(
  targetId: String,
  isServiceTarget: Boolean,
  content: String,
) {
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
