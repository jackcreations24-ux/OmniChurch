package com.zoutiw.omnichurch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.presentation.screens.home.MemberHomeScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServiceDetailsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServicesScreen

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
      onCreateAnnouncement = {
        title,
        content,
        department,
        category,
        date,
        targetAudience,
        isPinned,
        showToastOnLogin,
        toastAlertLevel,
        isScheduledNotification,
        scheduledNotificationDate,
        posterUrl,
        serviceId ->
        ctx.viewModel.createAnnouncement(
          churchId = ctx.resolvedChurchId,
          title = title,
          content = content,
          isPinned = isPinned,
          authorName = ctx.resolvedAuthorName,
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
      },
      onCreateService = { title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
        ctx.viewModel.createService(
          churchId = ctx.resolvedChurchId,
          churchName = ctx.resolvedChurchName,
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
          authorName = ctx.resolvedAuthorName,
        )
      },
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
      onUpdateService = { existingService, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
        ctx.viewModel.updateService(
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
      },
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
      onCreateService = { title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
        ctx.viewModel.createService(
          churchId = ctx.resolvedChurchId,
          churchName = ctx.resolvedChurchName,
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
          authorName = ctx.resolvedAuthorName,
        )
      },
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
      onUpdateService = { existingService, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
        ctx.viewModel.updateService(
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
      },
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
}
