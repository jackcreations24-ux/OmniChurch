package com.zoutiw.omnichurch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zoutiw.omnichurch.core.utilities.LocaleManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.screens.donate.DonateScreen
import com.zoutiw.omnichurch.presentation.screens.member.AdminMemberListScreen
import com.zoutiw.omnichurch.presentation.screens.member.DigitalMemberIdScreen
import com.zoutiw.omnichurch.presentation.screens.member.MemberQRGalleryScreen
import com.zoutiw.omnichurch.presentation.screens.scanner.AdminQRScannerScreen
import com.zoutiw.omnichurch.presentation.screens.services.NotificationsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ProfileAndSettingsScreen
import com.zoutiw.omnichurch.presentation.screens.verification.MemberPublicVerificationScreen
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.registerSecondaryWorkspaceRoutes(ctx: WorkspaceNavContext) {
  composable(OmniRoutes.MEMBER_ID_CARD) {
    DigitalMemberIdScreen(
      memberState = ctx.currentMemberState,
      qrBitmap = ctx.memberQrBitmap,
      onBackClick = { ctx.navController.popBackStack() },
      onOpenFullScreenQrGallery = {
        ctx.navController.navigate(OmniRoutes.MEMBER_QR_GALLERY)
      },
      onOpenProfileSettings = { ctx.navController.navigate(OmniRoutes.PROFILE_SETTINGS) },
      onUpdateMemberPhoto = { newPhotoUrl ->
        ctx.viewModel.updateMemberPhoto(
          photoDataUrl = newPhotoUrl,
          onSuccess = {
            ctx.viewModel.setFeedbackMessage(
              if (newPhotoUrl.isBlank()) "Foto pwofil ou retire avèk siksè!"
              else "Foto pwofil ou mete ajou avèk siksè!"
            )
          },
          onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
        )
      },
      currentLanguageCode = ctx.currentLanguageCode,
    )
  }

  composable(OmniRoutes.MEMBER_QR_GALLERY) {
    MemberQRGalleryScreen(
      member = ctx.member,
      qrBitmap = ctx.memberQrBitmap,
      attendanceHistory = ctx.memberAttendanceHistory,
      currentLanguageCode = ctx.currentLanguageCode,
      effectiveVerificationBaseUrl = ctx.verificationConfig.effectiveVerificationBaseUrl,
      onOpenVerificationCertificate = { targetMember ->
        ctx.viewModel.openDirectMemberVerification(targetMember, ctx.activeChurch)
        ctx.navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
      },
      onClose = { ctx.navController.popBackStack() },
    )
  }

  composable(OmniRoutes.ADMIN_QR_SCANNER) {
    if (!ctx.canScanAttendance) {
      androidx.compose.runtime.LaunchedEffect(Unit) { ctx.navigateBackOrHome() }
      return@composable
    }
    val effectiveProfile = ctx.userProfile ?: UserProfile(
      role = if (ctx.isAdminSession) "admin" else "usher",
      churchId = ctx.resolvedChurchId,
    )
    AdminQRScannerScreen(
      userProfile = effectiveProfile,
      activeServices = ctx.activeAttendanceServices,
      selectedServiceId = ctx.selectedScannerServiceId,
      validationOutcome = ctx.scannerOutcome,
      churchMembers = ctx.churchMembers,
      allServices = ctx.availableServicesList,
      currentLanguageCode = ctx.currentLanguageCode,
      observeServiceAttendances = { serviceId ->
        ctx.viewModel.observeAttendancesForService(ctx.resolvedChurchId, serviceId)
      },
      onSelectService = { serviceId -> ctx.viewModel.selectScannerService(serviceId) },
      onScanOrSubmitCode = { rawTokenOrCode, service, method ->
        ctx.viewModel.processAttendanceScanOrCode(rawTokenOrCode, service, method)
      },
      onBindNfcCardToMember = { memberDocId, nfcCardUid, onLinked ->
        ctx.viewModel.bindNfcCardToMember(
          memberDocId = memberDocId,
          nfcCardUid = nfcCardUid,
          languageCode = ctx.currentLanguageCode,
          onSuccess = onLinked,
        )
      },
      onVerifyCard = { rawTokenOrCode ->
        ctx.viewModel.verifyMemberCard(rawTokenOrCode, ctx.resolvedChurchId)
        ctx.navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
      },
      onOpenMemberVerification = { targetMember ->
        ctx.viewModel.openDirectMemberVerification(targetMember, ctx.activeChurch)
        ctx.navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
      },
      onResetOutcome = { ctx.viewModel.resetScannerOutcome() },
      onOpenServicesManagement = { ctx.navController.navigate(OmniRoutes.SERVICES) },
      onBackClick = { ctx.navController.popBackStack() },
    )
  }

  composable(OmniRoutes.MEMBER_VERIFICATION) {
    MemberPublicVerificationScreen(
      verificationState = ctx.cardVerificationState,
      verificationConfig = ctx.verificationConfig,
      isChurchLicensed = { cId, cCode -> ctx.adMobController.isChurchLicensed(cId, cCode) },
      initialLanguageCode = ctx.currentLanguageCode,
      onRetry = { ctx.viewModel.retryLastCardVerification() },
      onClose = {
        ctx.viewModel.clearCardVerificationState()
        ctx.navController.popBackStack()
      },
    )
  }

  composable(OmniRoutes.ADMIN_MEMBER_LIST) {
    if (!ctx.isAdminSession) {
      androidx.compose.runtime.LaunchedEffect(Unit) { ctx.navigateBackOrHome() }
      return@composable
    }
    AdminMemberListScreen(
      userProfile = ctx.userProfile,
      isAdminSession = ctx.isAdminSession,
      churchMembers = ctx.churchMembers,
      churchMembersUiState = ctx.churchMembersUiState,
      onRetryLoadMembers = { ctx.viewModel.retryLoadChurchMembers() },
      onBackClick = { ctx.navController.popBackStack() },
      onAdminUpdateMember = { memberDocId, fullName, department, ministryRole, phone, titlePrefix ->
        ctx.viewModel.updateChurchMemberByAdmin(
          memberDocId = memberDocId,
          fullName = fullName,
          department = department,
          ministryRole = ministryRole,
          phone = phone,
          titlePrefix = titlePrefix,
        )
      },
      onRegisterNewMember = { fullName, customMatrikil, pin, phone, department, ministryRole ->
        val targetChurch = ctx.activeChurch ?: Church(id = ctx.resolvedChurchId, name = ctx.resolvedChurchName)
        ctx.viewModel.registerNewMemberByAdmin(
          church = targetChurch,
          fullName = fullName,
          customMatrikil = customMatrikil,
          pin = pin,
          phone = phone,
          department = department,
          ministryRole = ministryRole,
          onSuccess = { newMem ->
            ctx.viewModel.setFeedbackMessage(
              "Manm ${newMem.fullName} anrejistre! ID Matrikil: ${newMem.effectiveMatrikil}"
            )
          },
          onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
        )
      },
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
        ctx.member?.let {
          com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.defaultDeaconAuthorName(it)
        },
      canPublishAnnouncement = ctx.canPublishAnnouncements,
      canEditAnnouncement = ctx.canPublishAnnouncements,
      availableServices = ctx.availableServicesList,
      onCreateAnnouncement =
        if (ctx.canPublishAnnouncements) {
          {
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
          }
        } else null,
      onUpdateAnnouncement =
        if (ctx.canPublishAnnouncements) {
          {
            existingAnnouncement,
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
            ctx.viewModel.updateAnnouncement(
              announcementId = existingAnnouncement.id,
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
        } else null,
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

  composable(OmniRoutes.PROFILE_SETTINGS) {
    val activeChurch = ctx.activeChurch
    ProfileAndSettingsScreen(
      userProfile = ctx.userProfile,
      member = ctx.member,
      currentLanguageCode = ctx.currentLanguageCode,
      onLanguageChange = { newLang ->
        ctx.scope.launch {
          ctx.preferencesManager.setLanguageCode(newLang)
          LocaleManager(ctx.context.applicationContext).savePreferredLanguage(newLang)
        }
      },
      onSaveMemberChanges = { fullName, department, role ->
        ctx.viewModel.updateMemberProfileInfo(fullName, department, role) {}
      },
      onSignOut = ctx.performFullSignOut,
      onBackClick = ctx.navigateBackOrHome,
      biometricEnabled = ctx.sessionState.biometricEnabled,
      onToggleBiometric = { enabled -> ctx.viewModel.toggleBiometricQuickUnlock(enabled) },
      onLinkGoogleAccount = { payload ->
        ctx.viewModel.linkGoogleToCurrentMember(
          googleEmail = payload.email,
          googleUid = payload.uid,
          googlePhotoUrl = payload.photoUrl,
          onSuccess = {
            ctx.viewModel.setFeedbackMessage(
              when (ctx.currentLanguageCode.lowercase()) {
                "fr" -> "Compte Google (${payload.email}) lié avec succès !"
                "en" -> "Google account (${payload.email}) linked successfully!"
                "es" -> "¡Cuenta de Google (${payload.email}) vinculada con éxito!"
                else -> "Kont Google (${payload.email}) lye avèk siksè!"
              }
            )
          },
          onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
        )
      },
      onUpdateMemberPhoto =
        if (ctx.member != null) {
          { newPhotoUrl ->
            ctx.viewModel.updateMemberPhoto(
              photoDataUrl = newPhotoUrl,
              onSuccess = {
                val cleared = newPhotoUrl.isBlank()
                ctx.viewModel.setFeedbackMessage(
                  when (ctx.currentLanguageCode.lowercase()) {
                    "fr" -> if (cleared) "Photo de profil supprimée !" else "Photo de profil mise à jour avec succès !"
                    "en" -> if (cleared) "Profile photo removed!" else "Profile photo updated successfully!"
                    "es" -> if (cleared) "¡Foto de perfil eliminada!" else "¡Foto de perfil actualizada con éxito!"
                    else -> if (cleared) "Foto pwofil ou retire avèk siksè!" else "Foto pwofil ou mete ajou avèk siksè!"
                  }
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      onUpdateAdminPhoto =
        if (ctx.isAdminSession && activeChurch != null) {
          { newPhotoUrl ->
            ctx.viewModel.updateAdminPhoto(
              photoDataUrl = newPhotoUrl,
              onSuccess = {
                val cleared = newPhotoUrl.isBlank()
                ctx.viewModel.setFeedbackMessage(
                  when (ctx.currentLanguageCode.lowercase()) {
                    "fr" -> if (cleared) "Photo administrateur supprimée !" else "Photo administrateur mise à jour !"
                    "en" -> if (cleared) "Admin photo removed!" else "Admin photo updated!"
                    "es" -> if (cleared) "¡Foto de administrador eliminada!" else "¡Foto de administrador actualizada!"
                    else -> if (cleared) "Foto administratè a retire avèk siksè!" else "Foto administratè a mete ajou avèk siksè!"
                  }
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      onUpdateChurchLogo =
        if (ctx.isAdminSession && activeChurch != null) {
          { newLogoUrl ->
            ctx.viewModel.updateChurchLogo(
              logoDataUrl = newLogoUrl,
              onSuccess = {
                val cleared = newLogoUrl.isBlank()
                ctx.viewModel.setFeedbackMessage(
                  when (ctx.currentLanguageCode.lowercase()) {
                    "fr" -> if (cleared) "Logo de l'église supprimé !" else "Logo de l'église mis à jour !"
                    "en" -> if (cleared) "Church logo removed!" else "Church logo updated!"
                    "es" -> if (cleared) "¡Logo de la iglesia eliminado!" else "¡Logo de la iglesia actualizado!"
                    else -> if (cleared) "Logo legliz la retire avèk siksè!" else "Logo legliz la mete ajou avèk siksè!"
                  }
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      onRegisterNewMember =
        if (ctx.isAdminSession) {
          { fullName, customMatrikil, pin, phone, department, ministryRole ->
            val targetChurch = activeChurch ?: Church(id = ctx.resolvedChurchId, name = ctx.resolvedChurchName)
            ctx.viewModel.registerNewMemberByAdmin(
              church = targetChurch,
              fullName = fullName,
              customMatrikil = customMatrikil,
              pin = pin,
              phone = phone,
              department = department,
              ministryRole = ministryRole,
              onSuccess = { newMem ->
                ctx.viewModel.setFeedbackMessage(
                  when (ctx.currentLanguageCode.lowercase()) {
                    "fr" -> "Membre ${newMem.fullName} enregistré ! ID Matricule : ${newMem.effectiveMatrikil}"
                    "en" -> "Member ${newMem.fullName} registered! Member ID: ${newMem.effectiveMatrikil}"
                    "es" -> "¡Miembro ${newMem.fullName} registrado! ID Matrícula: ${newMem.effectiveMatrikil}"
                    else -> "Manm ${newMem.fullName} anrejistre! ID Matrikil: ${newMem.effectiveMatrikil}"
                  }
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      churchMembers = if (ctx.isAdminSession) ctx.churchMembers else emptyList(),
      onAdminUpdateMember =
        if (ctx.isAdminSession) {
          { memberDocId, fullName, department, ministryRole, phone, titlePrefix ->
            ctx.viewModel.updateChurchMemberByAdmin(
              memberDocId = memberDocId,
              fullName = fullName,
              department = department,
              ministryRole = ministryRole,
              phone = phone,
              titlePrefix = titlePrefix,
            )
          }
        } else null,
      activeChurch = activeChurch,
      isAdminSession = ctx.isAdminSession,
      isAdminSecurityBusy = ctx.adminAuthBusy,
      onSaveChurchSecuritySettings =
        if (ctx.isAdminSession && activeChurch != null) {
          { quickPinEnabled, newPinOrNull, autoLockMinutes ->
            ctx.adminAuthViewModel.updateSecuritySettings(
              church = activeChurch,
              quickPinEnabled = quickPinEnabled,
              newPinOrNull = newPinOrNull,
              autoLockMinutes = autoLockMinutes,
              languageCode = ctx.currentLanguageCode,
              onSuccess = {
                ctx.viewModel.setFeedbackMessage(
                  when (ctx.currentLanguageCode.lowercase()) {
                    "fr" -> "Paramètres de sécurité enregistrés !"
                    "en" -> "Security settings saved!"
                    "es" -> "¡Configuración de seguridad guardada!"
                    else -> "Paramèt sekirite ak PIN rapid yo anrejistre!"
                  }
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      onLockAdminNow =
        if (ctx.isAdminSession && activeChurch != null) {
          { ctx.adminAuthViewModel.lockChurchNow(activeChurch.id) }
        } else null,
      unreadNotificationsCount = ctx.unreadNotificationsCount,
      onOpenNotifications = { ctx.navigateToTab(OmniRoutes.NOTIFICATIONS) },
    )
  }

  composable(OmniRoutes.DONATE) {
    DonateScreen(
      donationConfig = ctx.donationConfig,
      currentLanguageCode = ctx.currentLanguageCode,
      onBackClick = { ctx.navController.popBackStack() },
    )
  }
}
