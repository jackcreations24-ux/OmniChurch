package com.zoutiw.omnichurch

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.ads.RemoteMemberVerificationConfig
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.ChurchNotification
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.MemberAttendanceHistoryItem
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.PublicVerificationResolution
import com.zoutiw.omnichurch.data.models.ScannerValidationOutcome
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.presentation.components.GlassBottomNavigation
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.OmniNavTab
import com.zoutiw.omnichurch.presentation.navigation.AdminAuthViewModel
import com.zoutiw.omnichurch.presentation.navigation.OmniChurchViewModel
import com.zoutiw.omnichurch.presentation.screens.auth.AutoLockWarningDialog
import com.zoutiw.omnichurch.presentation.screens.auth.BlockingAdminAuthGateOverlay
import com.zoutiw.omnichurch.presentation.screens.auth.signOutOmniChurch
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import kotlinx.coroutines.CoroutineScope

internal data class WorkspaceNavContext(
  val context: Context,
  val scope: CoroutineScope,
  val navController: NavHostController,
  val viewModel: OmniChurchViewModel,
  val adminAuthViewModel: AdminAuthViewModel,
  val preferencesManager: OmniPreferencesManager,
  val adMobController: AdMobRemoteController,
  val verificationConfig: RemoteMemberVerificationConfig,
  val donationConfig: DeveloperDonationConfig,
  val currentLanguageCode: String,
  val sessionState: OmniSessionState,
  val activeChurch: Church?,
  val userProfile: UserProfile?,
  val member: Member?,
  val currentMemberState: UiState<Member?>,
  val servicesState: UiState<List<ChurchService>>,
  val availableServicesList: List<ChurchService>,
  val activeAttendanceServices: List<ChurchService>,
  val churchMembers: List<Member>,
  val servicePickerMembers: List<Member>,
  val churchMembersUiState: UiState<List<Member>>,
  val announcementsState: UiState<List<Announcement>>,
  val unreadAnnouncementIds: Set<String>,
  val unreadNotificationsCount: Int,
  val reactionSummaries: Map<String, TargetReactionSummary>,
  val commentCounts: Map<String, Int>,
  val notificationsState: UiState<List<ChurchNotification>>,
  val memberQrBitmap: Bitmap?,
  val scannerOutcome: ScannerValidationOutcome,
  val cardVerificationState: UiState<PublicVerificationResolution?>,
  val selectedScannerServiceId: String?,
  val memberAttendanceHistory: List<MemberAttendanceHistoryItem>,
  val isAdminSession: Boolean,
  val isActiveDeacon: Boolean,
  val canManageServices: Boolean,
  val canPublishAnnouncements: Boolean,
  val canScanAttendance: Boolean,
  val currentAuthUid: String,
  val resolvedProfileAvatarUrl: String?,
  val adminAuthBusy: Boolean,
  val resolvedChurchId: String,
  val resolvedChurchName: String,
  val resolvedChurchLogoUrl: String?,
  val resolvedAuthorName: String,
  val isChromeVisible: Boolean,
  val onChromeVisibilityChange: (Boolean) -> Unit,
  val navigateToTab: (String) -> Unit,
  val navigateBackOrHome: () -> Unit,
  val onOpenDonate: () -> Unit,
  val performFullSignOut: () -> Unit,
)

@Composable
internal fun AuthenticatedOmniChurchWorkspace(
  viewModel: OmniChurchViewModel,
  adminAuthViewModel: AdminAuthViewModel,
  preferencesManager: OmniPreferencesManager,
  currentLanguageCode: String,
) {
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }

  val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
  val activeChurch by viewModel.activeChurchState.collectAsStateWithLifecycle()
  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val currentMemberState by viewModel.currentMemberState.collectAsStateWithLifecycle()
  val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
  val activeAttendanceServices by viewModel.activeAttendanceServicesState.collectAsStateWithLifecycle()
  val churchMembers by viewModel.churchMembersState.collectAsStateWithLifecycle()
  val publicMemberNames by viewModel.publicMemberNamesState.collectAsStateWithLifecycle()
  val churchMembersUiState by viewModel.churchMembersUiState.collectAsStateWithLifecycle()
  val currentAuthUid by viewModel.currentAuthUid.collectAsStateWithLifecycle()
  val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
  val unreadAnnouncementIds by viewModel.unreadAnnouncementIdsState.collectAsStateWithLifecycle()
  val reactionSummaries by viewModel.reactionSummariesState.collectAsStateWithLifecycle()
  val commentCounts by viewModel.commentCountsState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val memberQrBitmap by viewModel.memberQrBitmap.collectAsStateWithLifecycle()
  val scannerOutcome by viewModel.scannerOutcome.collectAsStateWithLifecycle()
  val cardVerificationState by viewModel.cardVerificationState.collectAsStateWithLifecycle()
  val selectedScannerServiceId by viewModel.selectedScannerServiceId.collectAsStateWithLifecycle()
  val memberAttendanceHistory by viewModel.memberAttendanceHistoryState.collectAsStateWithLifecycle()
  val actionFeedbackMessage by viewModel.actionFeedbackMessage.collectAsStateWithLifecycle()
  val lockedChurches by adminAuthViewModel.lockedChurches.collectAsStateWithLifecycle()
  val adminFirebaseUser by adminAuthViewModel.firebaseUser.collectAsStateWithLifecycle()
  val adminAuthBusy by adminAuthViewModel.isBusy.collectAsStateWithLifecycle()
  val adminAuthError by adminAuthViewModel.authError.collectAsStateWithLifecycle()
  val adminAuthInfo by adminAuthViewModel.infoMessage.collectAsStateWithLifecycle()
  val autoLockWarningSeconds by adminAuthViewModel.autoLockWarningSeconds.collectAsStateWithLifecycle()
  val legacyVerifiedChurchId by adminAuthViewModel.legacyVerifiedChurchId.collectAsStateWithLifecycle()
  val legacyVerifiedPin by adminAuthViewModel.legacyVerifiedPin.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  val adMobController = remember(context) { AdMobRemoteController.getInstance(context) }
  val verificationConfig by adMobController.memberVerificationState.collectAsStateWithLifecycle()
  val donationConfig by adMobController.donationConfigState.collectAsStateWithLifecycle()

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: OmniRoutes.HOME
  var isChromeVisible by remember { mutableStateOf(true) }

  LaunchedEffect(currentRoute) {
    isChromeVisible = true
  }

  val navigateToTab: (String) -> Unit = { targetRoute ->
    isChromeVisible = true
    if (currentRoute != targetRoute) {
      if (targetRoute == OmniRoutes.HOME) {
        val poppedToHome = navController.popBackStack(OmniRoutes.HOME, inclusive = false)
        if (!poppedToHome) {
          navController.navigate(OmniRoutes.HOME) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
          }
        }
      } else {
        navController.navigate(targetRoute) {
          popUpTo(navController.graph.startDestinationId) { saveState = true }
          launchSingleTop = true
          restoreState = true
        }
      }
    }
  }

  val navigateBackOrHome: () -> Unit = {
    isChromeVisible = true
    if (!navController.popBackStack()) {
      navigateToTab(OmniRoutes.HOME)
    }
  }

  LaunchedEffect(actionFeedbackMessage) {
    val msg = actionFeedbackMessage
    if (!msg.isNullOrBlank()) {
      Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
      viewModel.clearFeedbackMessage()
    }
  }

  val userProfile = (userProfileState as? UiState.Success)?.data
  val member = (currentMemberState as? UiState.Success)?.data
  val isAdminSession = sessionState.isAdminSession || userProfile?.isAdminOrSuperAdmin == true
  val isActiveDeacon =
    !isAdminSession && com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.isActiveDeacon(member)
  val canManageServices =
    com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.canManageServices(
      member = member,
      isAdmin = isAdminSession,
    )
  val canPublishAnnouncements =
    com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.canPublishAnnouncements(
      member = member,
      isAdmin = isAdminSession,
    )
  val canScanAttendance =
    com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.canScanAttendance(
      member = member,
      isAdmin = isAdminSession,
    ) || userProfile?.isAuthorizedScanner == true
  val servicePickerMembers =
    if (isAdminSession || com.zoutiw.omnichurch.data.models.OmniRoleEvaluator.isActiveReception(member)) {
      churchMembers
    } else {
      publicMemberNames
    }
  val currentChurch = activeChurch
  val isAdminLocked =
    isAdminSession &&
      currentChurch != null &&
      (
        lockedChurches[currentChurch.id] == true ||
          (adminFirebaseUser == null && currentChurch.hasCloudOwner)
      )

  DisposableEffect(lifecycleOwner, isAdminSession, currentChurch?.id, currentChurch?.effectiveAutoLockMinutes) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_STOP -> adminAuthViewModel.onAppStopped(isAdminSession)
          Lifecycle.Event.ON_START -> adminAuthViewModel.onAppStarted(isAdminSession, currentChurch)
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(isAdminSession, isAdminLocked, currentChurch?.id, currentChurch?.effectiveAutoLockMinutes) {
    adminAuthViewModel.updateInactivityWatcher(
      isAdminSession = isAdminSession,
      isAdminUnlocked = !isAdminLocked,
      church = currentChurch,
    )
  }

  val resolvedChurchId =
    currentChurch?.id?.trim()?.takeIf { it.isNotBlank() }
      ?: sessionState.activeChurchId.trim().takeIf { it.isNotBlank() }
      ?: member?.churchId?.trim()?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchId?.trim().orEmpty()
  val resolvedChurchName =
    currentChurch?.name?.trim()?.takeIf { it.isNotBlank() }
      ?: member?.churchName?.trim()?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.trim()?.takeIf { it.isNotBlank() }
      ?: "OmniChurch Pro"
  val resolvedChurchLogoUrl =
    currentChurch?.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
      ?: member?.churchLogoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val resolvedAuthorName =
    userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
      ?: member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
      ?: currentChurch?.pastor?.trim()?.takeIf { it.isNotBlank() }
      ?: "Admin"
  val availableServicesList = (servicesState as? UiState.Success)?.data ?: emptyList()
  val unreadNotificationsCount = unreadAnnouncementIds.size

  val performFullSignOut: () -> Unit = {
    adminAuthViewModel.signOutAdmin(credentialManager)
    viewModel.logoutCurrentSession {}
    signOutOmniChurch(credentialManager, scope)
  }

  val resolvedBottomNavAvatarUrl: String? =
    if (member != null) {
      val hasExplicitMemberPhotoState =
        member.avatarUrl != null || member.photoUrl != null || member.googlePhoto != null
      member.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
        ?: if (hasExplicitMemberPhotoState) {
          null
        } else {
          userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
        }
    } else if (isAdminSession) {
      if (currentChurch?.adminGooglePhoto != null) {
        currentChurch.adminGooglePhoto.trim().takeIf { it.isNotEmpty() }
      } else {
        userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
          ?: adminFirebaseUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }
      }
    } else {
      userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
    }

  val navCtx = WorkspaceNavContext(
    context = context,
    scope = scope,
    navController = navController,
    viewModel = viewModel,
    adminAuthViewModel = adminAuthViewModel,
    preferencesManager = preferencesManager,
    adMobController = adMobController,
    verificationConfig = verificationConfig,
    donationConfig = donationConfig,
    currentLanguageCode = currentLanguageCode,
    sessionState = sessionState,
    activeChurch = currentChurch,
    userProfile = userProfile,
    member = member,
    currentMemberState = currentMemberState,
    servicesState = servicesState,
    availableServicesList = availableServicesList,
    activeAttendanceServices = activeAttendanceServices,
    churchMembers = churchMembers,
    servicePickerMembers = servicePickerMembers,
    churchMembersUiState = churchMembersUiState,
    announcementsState = announcementsState,
    unreadAnnouncementIds = unreadAnnouncementIds,
    unreadNotificationsCount = unreadNotificationsCount,
    reactionSummaries = reactionSummaries,
    commentCounts = commentCounts,
    notificationsState = notificationsState,
    memberQrBitmap = memberQrBitmap,
    scannerOutcome = scannerOutcome,
    cardVerificationState = cardVerificationState,
    selectedScannerServiceId = selectedScannerServiceId,
    memberAttendanceHistory = memberAttendanceHistory,
    isAdminSession = isAdminSession,
    isActiveDeacon = isActiveDeacon,
    canManageServices = canManageServices,
    canPublishAnnouncements = canPublishAnnouncements,
    canScanAttendance = canScanAttendance,
    currentAuthUid = currentAuthUid,
    resolvedProfileAvatarUrl = resolvedBottomNavAvatarUrl,
    adminAuthBusy = adminAuthBusy,
    resolvedChurchId = resolvedChurchId,
    resolvedChurchName = resolvedChurchName,
    resolvedChurchLogoUrl = resolvedChurchLogoUrl,
    resolvedAuthorName = resolvedAuthorName,
    isChromeVisible = isChromeVisible,
    onChromeVisibilityChange = { visible -> isChromeVisible = visible },
    navigateToTab = navigateToTab,
    navigateBackOrHome = navigateBackOrHome,
    onOpenDonate = {
      isChromeVisible = true
      navController.navigate(OmniRoutes.DONATE) {
        launchSingleTop = true
      }
    },
    performFullSignOut = performFullSignOut,
  )

  if (currentMemberState is UiState.Loading && member == null && !isAdminSession && sessionState.activeMemberDocId.isBlank() && sessionState.activeMemberId.isBlank()) {
    Box(
      modifier = Modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
      contentAlignment = Alignment.Center,
    ) {
      GlassLoadingIndicator()
    }
  } else {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(OmniBrushes.DeepBackgroundGradient)
          .pointerInput(isAdminSession, isAdminLocked) {
            awaitPointerEventScope {
              while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                if (isAdminSession && !isAdminLocked) {
                  adminAuthViewModel.recordUserInteraction()
                }
              }
            }
          },
    ) {
      NavHost(
        navController = navController,
        startDestination = OmniRoutes.HOME,
        modifier = Modifier.fillMaxSize(),
      ) {
        registerPrimaryWorkspaceRoutes(navCtx)
        registerSecondaryWorkspaceRoutes(navCtx)
      }

      val showBottomBar =
        !isAdminLocked &&
          currentRoute in
          setOf(
            OmniRoutes.HOME,
            OmniRoutes.SERVICES,
            OmniRoutes.NOTIFICATIONS,
            OmniRoutes.PROFILE_SETTINGS,
          )

      val selectedTab =
        when (currentRoute) {
          OmniRoutes.HOME -> OmniNavTab.HOME
          OmniRoutes.SERVICES -> OmniNavTab.SERVICES
          OmniRoutes.NOTIFICATIONS -> OmniNavTab.NOTIFICATIONS
          OmniRoutes.PROFILE_SETTINGS -> OmniNavTab.PROFILE
          else -> OmniNavTab.MEMBER_CARD_OR_SCANNER
        }
      val resolvedBottomNavDisplayName =
        member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
          ?: userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
          ?: currentChurch?.adminGoogleName?.trim()?.takeIf { it.isNotBlank() }
          ?: resolvedAuthorName
      val isScannerOrbMode = canScanAttendance

      AnimatedVisibility(
        visible = showBottomBar && isChromeVisible,
        enter = slideInVertically(
          initialOffsetY = { it },
          animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        ) + fadeIn(animationSpec = tween(durationMillis = 120)),
        exit = slideOutVertically(
          targetOffsetY = { it },
          animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
        ) + fadeOut(animationSpec = tween(durationMillis = 110)),
        modifier = Modifier.align(Alignment.BottomCenter),
      ) {
        GlassBottomNavigation(
          selectedTab = selectedTab,
          unreadNotificationsCount = unreadNotificationsCount,
          onTabSelected = { tab ->
            val targetRoute =
              when (tab) {
                OmniNavTab.HOME -> OmniRoutes.HOME
                OmniNavTab.SERVICES -> OmniRoutes.SERVICES
                OmniNavTab.MEMBER_CARD_OR_SCANNER -> OmniRoutes.MEMBER_ID_CARD
                OmniNavTab.NOTIFICATIONS -> OmniRoutes.NOTIFICATIONS
                OmniNavTab.PROFILE -> OmniRoutes.PROFILE_SETTINGS
              }
            navigateToTab(targetRoute)
          },
          onCenterQrOrbClick = {
            isChromeVisible = true
            if (isScannerOrbMode) {
              activeAttendanceServices.firstOrNull()?.let {
                viewModel.selectScannerService(it.id)
              }
              navController.navigate(OmniRoutes.ADMIN_QR_SCANNER) {
                launchSingleTop = true
              }
            } else {
              navController.navigate(OmniRoutes.MEMBER_QR_GALLERY) {
                launchSingleTop = true
              }
            }
          },
          userAvatarUrl = resolvedBottomNavAvatarUrl,
          userDisplayName = resolvedBottomNavDisplayName,
          currentLanguageCode = currentLanguageCode,
          isScannerMode = isScannerOrbMode,
        )
      }

      val warningSecs = autoLockWarningSeconds
      if (isAdminSession && !isAdminLocked && warningSecs != null && currentChurch != null) {
        AutoLockWarningDialog(
          secondsRemaining = warningSecs,
          currentLanguageCode = currentLanguageCode,
          onStayActive = { adminAuthViewModel.recordUserInteraction() },
          onLockNow = { adminAuthViewModel.lockChurchNow(currentChurch.id) },
        )
      }

      if (isAdminLocked && currentChurch != null) {
        BlockingAdminAuthGateOverlay(
          church = currentChurch,
          firebaseUser = adminFirebaseUser,
          isBusy = adminAuthBusy,
          errorMessage = adminAuthError,
          infoMessage = adminAuthInfo,
          legacyVerifiedChurchId = legacyVerifiedChurchId,
          legacyVerifiedPin = legacyVerifiedPin,
          currentLanguageCode = currentLanguageCode,
          onClearMessages = { adminAuthViewModel.clearMessages() },
          onSignInEmailPassword = { email, password ->
            adminAuthViewModel.signInAdminWithEmailPassword(
              church = currentChurch,
              email = email,
              password = password,
              languageCode = currentLanguageCode,
              onSuccess = {},
            )
          },
          onSendPasswordReset = { email ->
            adminAuthViewModel.sendPasswordResetEmail(email, currentLanguageCode)
          },
          onGoogleVerified = {
            adminAuthViewModel.verifySignedInAdminGoogleAccount(
              church = currentChurch,
              languageCode = currentLanguageCode,
              onSuccess = {},
            )
          },
          onQuickPinSubmit = { enteredPin ->
            adminAuthViewModel.unlockWithQuickPin(
              church = currentChurch,
              pin = enteredPin,
              languageCode = currentLanguageCode,
              onSuccess = {},
            )
          },
          onBiometricSuccess = {
            adminAuthViewModel.authorizeViaBiometric(
              church = currentChurch,
              languageCode = currentLanguageCode,
              onSuccess = {},
            )
          },
          onVerifyLegacyPin = { pin ->
            adminAuthViewModel.verifyLegacyPinStep(currentChurch, pin, currentLanguageCode)
          },
          onCompleteLegacyMigration = { email, newPass, confirmPass ->
            adminAuthViewModel.completeLegacyMigration(
              church = currentChurch,
              adminEmail = email,
              newPassword = newPass,
              confirmPassword = confirmPass,
              languageCode = currentLanguageCode,
              onSuccess = {},
            )
          },
          onResetLegacyMigration = { adminAuthViewModel.resetLegacyMigrationStep() },
          onLocalError = { msg -> adminAuthViewModel.setAuthError(msg) },
          onExitToAuthPortal = performFullSignOut,
        )
      }
    }
  }
}
