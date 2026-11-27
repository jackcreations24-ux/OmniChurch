package com.zoutiw.omnichurch

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.zoutiw.omnichurch.core.utilities.LocaleManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.screens.donate.DonateScreen
import com.zoutiw.omnichurch.presentation.screens.member.AdminMemberListScreen
import com.zoutiw.omnichurch.presentation.screens.member.DigitalMemberIdScreen
import com.zoutiw.omnichurch.presentation.screens.member.MemberQRGalleryScreen
import com.zoutiw.omnichurch.presentation.screens.scanner.AdminQRScannerScreen
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
              WorkspaceFeedbackMessages.googleLinked(ctx.currentLanguageCode, payload.email)
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
                ctx.viewModel.setFeedbackMessage(
                  WorkspaceFeedbackMessages.memberPhotoUpdated(
                    ctx.currentLanguageCode,
                    newPhotoUrl.isBlank(),
                  )
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
                ctx.viewModel.setFeedbackMessage(
                  WorkspaceFeedbackMessages.adminPhotoUpdated(
                    ctx.currentLanguageCode,
                    newPhotoUrl.isBlank(),
                  )
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
                ctx.viewModel.setFeedbackMessage(
                  WorkspaceFeedbackMessages.churchLogoUpdated(
                    ctx.currentLanguageCode,
                    newLogoUrl.isBlank(),
                  )
                )
              },
              onError = { err -> ctx.viewModel.setFeedbackMessage(err) },
            )
          }
        } else null,
      onUpdateChurchLocation =
        if (ctx.isAdminSession && activeChurch != null) {
          { rawCity ->
            ctx.viewModel.updateChurchLocation(
              rawCity = rawCity,
              languageCode = ctx.currentLanguageCode,
              onSuccess = { formatted ->
                val msg =
                  when (ctx.currentLanguageCode.trim().lowercase()) {
                    "fr" -> "Localisation de l'église enregistrée : $formatted"
                    "en" -> "Church location saved: $formatted"
                    "es" -> "Ubicación de la iglesia guardada: $formatted"
                    else -> "Pozisyon legliz la anrejistre: $formatted"
                  }
                ctx.viewModel.setFeedbackMessage(msg)
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
                  WorkspaceFeedbackMessages.memberRegistered(
                    ctx.currentLanguageCode,
                    newMem.fullName,
                    newMem.effectiveMatrikil,
                  )
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
                  WorkspaceFeedbackMessages.securitySettingsSaved(ctx.currentLanguageCode)
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
