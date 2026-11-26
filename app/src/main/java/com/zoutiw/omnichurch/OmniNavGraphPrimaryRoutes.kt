package com.zoutiw.omnichurch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniRoleEvaluator
import com.zoutiw.omnichurch.presentation.screens.home.MemberHomeScreen
import com.zoutiw.omnichurch.presentation.screens.services.NotificationsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServiceDetailsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServicesScreen

internal fun WorkspaceNavContext.createAnnouncementCallback(): (
  String,
  String,
  String,
  String,
  String,
  String,
  Boolean,
  Boolean,
  String,
  Boolean,
  String?,
  String?,
  String?,
) -> Unit = { title, content, department, category, date, targetAudience, isPinned, showToastOnLogin, toastAlertLevel, isScheduledNotification, scheduledNotificationDate, posterUrl, serviceId ->
  viewModel.createAnnouncement(
    churchId = resolvedChurchId,
    title = title,
    content = content,
    isPinned = isPinned,
    authorName = resolvedAuthorName,
    department = department,
    category = category,
    date = date,
    targetAudience = targetAudience,
    showToastOnLogin = showToastOnLogin,
    toastAlertLevel = toastAlertLevel,
    isScheduledNotification = isScheduledNotification,
    scheduledNotificationDate = scheduledNotificationDate,
    posterUrl = posterUrl,
    serviceId = serviceId,
  )
}

internal fun WorkspaceNavContext.updateAnnouncementCallback(): (
  Announcement,
  String,
  String,
  String,
  String,
  String,
  String,
  Boolean,
  Boolean,
  String,
  Boolean,
  String?,
  String?,
  String?,
) -> Unit = { existing, title, content, department, category, date, targetAudience, isPinned, showToastOnLogin, toastAlertLevel, isScheduledNotification, scheduledNotificationDate, posterUrl, serviceId ->
  viewModel.updateAnnouncement(
    announcementId = existing.id,
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
  )
}

private fun WorkspaceNavContext.createServiceCallback(): (
  String,
  String?,
  String,
  Long,
  Boolean,
  Boolean,
  String,
  String,
  String,
  String,
  String?,
  String?,
  String?,
  String?,
  String?,
  String?,
  String?,
  Boolean,
  String?,
) -> Unit = { title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
  viewModel.createService(
    churchId = resolvedChurchId,
    churchName = resolvedChurchName,
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
    sendReminder24h = sendReminder24h,
    imageUrl = imageUrl,
    authorName = resolvedAuthorName,
  )
}

private fun WorkspaceNavContext.updateServiceCallback(): (
  ChurchService,
  String,
  String?,
  String,
  Long,
  Boolean,
  String,
  String,
  String,
  String,
  String?,
  String?,
  String?,
  String?,
  String?,
  String?,
  String?,
  Boolean,
  String?,
) -> Unit = { existingService, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
  viewModel.updateService(
    serviceId = existingService.id,
    title = title,
    description = description,
    dateFormatted = dateFormatted,
    lateToleranceMinutes = lateToleranceMinutes,
    isOpenForAttendance = isOpenForAttendance,
    isLive = existingService.isLive,
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
  )
}

internal fun NavGraphBuilder.registerPrimaryWorkspaceRoutes(ctx: WorkspaceNavContext) {
  composable(OmniRoutes.HOME) {
    MemberHomeScreen(
      userProfile = ctx.userProfile,
      member = ctx.member,
      servicesState = ctx.servicesState,
      announcementsState = ctx.announcementsState,
      unreadNotificationsCount = ctx.unreadNotificationsCount,
      onOpenMemberCard = { ctx.navController.navigate(OmniRoutes.MEMBER_ID_CARD) },
      onOpenScannerOrGallery = {
        if (ctx.canScanAttendance) {
          ctx.activeAttendanceServices.firstOrNull()?.let {
            ctx.viewModel.selectScannerService(it.id)
          }
          ctx.navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
        } else {
          ctx.navController.navigate(OmniRoutes.MEMBER_QR_GALLERY)
        }
      },
      onOpenServices = { ctx.navigateToTab(OmniRoutes.SERVICES) },
      onOpenNotifications = { ctx.navigateToTab(OmniRoutes.NOTIFICATIONS) },
      onOpenProfile = { ctx.navigateToTab(OmniRoutes.PROFILE_SETTINGS) },
      onOpenMemberList = {
        ctx.navController.navigate(OmniRoutes.ADMIN_MEMBER_LIST) {
          launchSingleTop = true
        }
      },
      onReactToItem = { targetId, isService, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = targetId,
          isServiceTarget = isService,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
        )
      },
      observeComments = { targetId ->
        ctx.viewModel.observeCommentsForTarget(ctx.resolvedChurchId, targetId)
      },
      onPostComment = { targetId, isService, content ->
        ctx.viewModel.postComment(targetId, isService, content)
      },
      onCreateAnnouncement = ctx.createAnnouncementCallback(),
      onCreateService = ctx.createServiceCallback(),
      reactionSummaries = ctx.reactionSummaries,
      commentCounts = ctx.commentCounts,
      onReactToComment = { commentId, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = commentId,
          isServiceTarget = false,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
          isCommentTarget = true,
        )
      },
      onDeleteService = { serviceId -> ctx.viewModel.deleteService(serviceId) },
      onDeleteAnnouncement = { announcementId ->
        ctx.viewModel.deleteAnnouncement(announcementId)
      },
      isAdminSession = ctx.isAdminSession,
      isDeaconSession = ctx.isActiveDeacon,
      currentAuthUid = ctx.currentAuthUid,
      headerPhotoUrl = ctx.resolvedProfileAvatarUrl,
      canCreateService = ctx.canManageServices,
      canEditService = ctx.canManageServices,
      onUpdateService = ctx.updateServiceCallback(),
      churchMembers = ctx.servicePickerMembers,
      activeChurchName = ctx.resolvedChurchName,
      activeChurchLogoUrl = ctx.resolvedChurchLogoUrl,
      currentLanguageCode = ctx.currentLanguageCode,
      isChromeVisible = ctx.isChromeVisible,
      onChromeVisibilityChange = ctx.onChromeVisibilityChange,
      donationConfig = ctx.donationConfig,
      onOpenDonate = ctx.onOpenDonate,
    )
  }

  composable("${OmniRoutes.SERVICE_DETAILS}/{serviceId}") { backStackEntry ->
    val serviceId = backStackEntry.arguments?.getString("serviceId").orEmpty()
    ServiceDetailsScreen(
      serviceId = serviceId,
      onBack = { ctx.navController.popBackStack() },
    )
  }

  composable(OmniRoutes.SERVICES) {
    ServicesScreen(
      userProfile = ctx.userProfile,
      servicesState = ctx.servicesState,
      onCreateService = ctx.createServiceCallback(),
      onToggleAttendanceWindow = { service, isOpen ->
        ctx.viewModel.toggleServiceAttendanceWindow(service, isOpen)
      },
      observeAttendances = { serviceId ->
        ctx.viewModel.observeAttendancesForService(ctx.resolvedChurchId, serviceId)
      },
      onOpenScannerForService = { serviceId ->
        ctx.viewModel.selectScannerService(serviceId)
        ctx.navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
      },
      onBackClick = ctx.navigateBackOrHome,
      onDeleteService = { serviceId -> ctx.viewModel.deleteService(serviceId) },
      isAdminSession = ctx.isAdminSession,
      isDeaconSession = ctx.isActiveDeacon,
      canScanAttendance = ctx.canScanAttendance,
      currentAuthUid = ctx.currentAuthUid,
      canCreateService = ctx.canManageServices,
      canEditService = ctx.canManageServices,
      onUpdateService = ctx.updateServiceCallback(),
      reactionSummaries = ctx.reactionSummaries,
      commentCounts = ctx.commentCounts,
      onReactToService = { serviceId, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = serviceId,
          isServiceTarget = true,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
        )
      },
      observeComments = { targetId ->
        ctx.viewModel.observeCommentsForTarget(ctx.resolvedChurchId, targetId)
      },
      onPostComment = { serviceId, content ->
        ctx.viewModel.postComment(serviceId, true, content)
      },
      onReactToComment = { commentId, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = commentId,
          isServiceTarget = false,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
          isCommentTarget = true,
        )
      },
      churchMembers = ctx.servicePickerMembers,
      activeChurchName = ctx.resolvedChurchName,
      activeChurchLogoUrl = ctx.resolvedChurchLogoUrl,
      currentLanguageCode = ctx.currentLanguageCode,
      isChromeVisible = ctx.isChromeVisible,
      onChromeVisibilityChange = ctx.onChromeVisibilityChange,
    )
  }

  composable(OmniRoutes.NOTIFICATIONS) {
    NotificationsScreen(
      notificationsState = ctx.notificationsState,
      onMarkRead = { notifId -> ctx.viewModel.markNotificationAsRead(notifId) },
      onMarkAllRead = { list -> ctx.viewModel.markAllNotificationsRead(list) },
      onBackClick = ctx.navigateBackOrHome,
      isAdmin = ctx.isAdminSession,
      isDeaconSession = ctx.isActiveDeacon,
      currentAuthUid = ctx.currentAuthUid,
      deaconAuthorLabel =
        ctx.member?.let { OmniRoleEvaluator.defaultDeaconAuthorName(it) },
      canPublishAnnouncement = ctx.canPublishAnnouncements,
      canEditAnnouncement = ctx.canPublishAnnouncements,
      availableServices = ctx.availableServicesList,
      onCreateAnnouncement =
        if (ctx.canPublishAnnouncements) ctx.createAnnouncementCallback() else null,
      onUpdateAnnouncement =
        if (ctx.canPublishAnnouncements) ctx.updateAnnouncementCallback() else null,
      announcementsState = ctx.announcementsState,
      unreadAnnouncementIds = ctx.unreadAnnouncementIds,
      onMarkAnnouncementRead = { announcementId ->
        ctx.viewModel.markAnnouncementAsRead(announcementId)
      },
      onMarkAllAnnouncementsRead = {
        ctx.viewModel.markAllAnnouncementsAsRead()
      },
      onRetryLoadAnnouncements = {
        ctx.viewModel.retryLoadAnnouncements()
      },
      currentLanguageCode = ctx.currentLanguageCode,
      reactionSummaries = ctx.reactionSummaries,
      commentCounts = ctx.commentCounts,
      onReactToAnnouncement = { announcementId, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = announcementId,
          isServiceTarget = false,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
        )
      },
      observeComments = { targetId ->
        ctx.viewModel.observeCommentsForTarget(ctx.resolvedChurchId, targetId)
      },
      onPostComment = { targetId, content ->
        ctx.viewModel.postComment(targetId, false, content)
      },
      onReactToComment = { commentId, reactionType ->
        ctx.viewModel.reactToFeedItem(
          targetId = commentId,
          isServiceTarget = false,
          churchId = ctx.resolvedChurchId,
          reactionType = OmniReactionType.fromKey(reactionType),
          isCommentTarget = true,
        )
      },
      onDeleteAnnouncement = { announcementId ->
        ctx.viewModel.deleteAnnouncement(announcementId)
      },
      isChromeVisible = ctx.isChromeVisible,
      onChromeVisibilityChange = ctx.onChromeVisibilityChange,
    )
  }
}
