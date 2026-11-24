package com.zoutiw.omnichurch

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zoutiw.omnichurch.core.ads.AdMobRemoteController
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.NamedFirestoreProvider
import com.zoutiw.omnichurch.core.utilities.LocaleHelper
import com.zoutiw.omnichurch.core.utilities.NfcIntentBus
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.data.models.UserProfile
import com.zoutiw.omnichurch.data.repositories.OmniChurchRepository
import com.zoutiw.omnichurch.presentation.components.GlassBottomNavigation
import com.zoutiw.omnichurch.presentation.components.GlassLoadingIndicator
import com.zoutiw.omnichurch.presentation.components.OmniChurchSplashScreen
import com.zoutiw.omnichurch.presentation.components.OmniNavTab
import com.zoutiw.omnichurch.presentation.navigation.AdminAuthViewModel
import com.zoutiw.omnichurch.presentation.navigation.OmniChurchViewModel
import com.zoutiw.omnichurch.presentation.screens.auth.AutoLockWarningDialog
import com.zoutiw.omnichurch.presentation.screens.auth.BlockingAdminAuthGateOverlay
import com.zoutiw.omnichurch.presentation.screens.auth.OmniChurchAuthPortalScreen
import com.zoutiw.omnichurch.presentation.screens.auth.signOutOmniChurch
import com.zoutiw.omnichurch.presentation.screens.home.MemberHomeScreen
import com.zoutiw.omnichurch.presentation.screens.member.AdminMemberListScreen
import com.zoutiw.omnichurch.presentation.screens.member.DigitalMemberIdScreen
import com.zoutiw.omnichurch.presentation.screens.member.MemberQRGalleryScreen
import com.zoutiw.omnichurch.presentation.screens.scanner.AdminQRScannerScreen
import com.zoutiw.omnichurch.presentation.screens.services.NotificationsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ProfileAndSettingsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServiceDetailsScreen
import com.zoutiw.omnichurch.presentation.screens.services.ServicesScreen
import com.zoutiw.omnichurch.presentation.screens.verification.MemberPublicVerificationScreen
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.GlassCardBorderSubtle
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.OmniChurchTheme
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser) }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

object OmniRoutes {
  const val HOME = "home"
  const val SERVICES = "services"
  const val MEMBER_ID_CARD = "member_id_card"
  const val MEMBER_QR_GALLERY = "member_qr_gallery"
  const val ADMIN_QR_SCANNER = "admin_qr_scanner"
  const val ADMIN_MEMBER_LIST = "admin_member_list"
  const val NOTIFICATIONS = "notifications"
  const val PROFILE_SETTINGS = "profile_settings"
  const val SERVICE_DETAILS = "service_details"
  const val MEMBER_VERIFICATION = "member_verification"
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    FirestoreProvider.ensureFirebaseInitialized(applicationContext)
    NfcIntentBus.dispatch(intent)
    enableEdgeToEdge()
    setContent {
      val activityContext = this@MainActivity
      val preferencesManager =
        remember(activityContext) { OmniPreferencesManager(activityContext.applicationContext) }
      val languageCode by
        preferencesManager.languageCodeFlow.collectAsStateWithLifecycle(
          initialValue = "ht"
        )

      remember(languageCode) {
        LocaleHelper.applyLocale(activityContext, languageCode)
        languageCode
      }

      key(languageCode) {
        OmniChurchTheme {
          OmniChurchRootHost(
            preferencesManager = preferencesManager,
            currentLanguageCode = languageCode,
          )
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    NfcIntentBus.dispatch(intent)
  }
}

@Composable
fun OmniChurchRootHost(
  preferencesManager: OmniPreferencesManager,
  currentLanguageCode: String,
) {
  val currentUser by
    remember { Firebase.auth.authStateFlow() }
      .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

  val viewModel: OmniChurchViewModel =
    viewModel(
      factory =
        viewModelFactory {
          initializer {
            val app = checkNotNull(this[APPLICATION_KEY])
            val firestore = NamedFirestoreProvider.getInstance(app)
            val repository = OmniChurchRepository(db = firestore, context = app.applicationContext)
            val user = currentUser
            OmniChurchViewModel(
              repository = repository,
              preferencesManager = preferencesManager,
              currentUserId = user?.uid.orEmpty(),
              currentUserEmail = user?.email.orEmpty(),
              currentUserDisplayName =
                user?.displayName?.takeIf { it.isNotBlank() }
                  ?: user?.email?.substringBefore("@").orEmpty(),
              currentUserPhotoUrl = user?.photoUrl?.toString(),
            )
          }
        }
    )

  val adminAuthViewModel: AdminAuthViewModel =
    viewModel(
      factory =
        viewModelFactory {
          initializer {
            AdminAuthViewModel(
              authRepository = viewModel.repository.authRepository,
              sessionLockManager = viewModel.repository.authRepository.sessionLockManager,
            )
          }
        }
    )

  LaunchedEffect(currentUser?.uid) {
    viewModel.syncAuthIdentity(currentUser?.uid.orEmpty())
  }

  val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
  val rememberedAdminCredentials by viewModel.rememberedAdminCredentials.collectAsStateWithLifecycle()
  val pendingAdminOwnedChurches by viewModel.pendingAdminOwnedChurches.collectAsStateWithLifecycle()
  val pendingMemberMatchedSessions by viewModel.pendingMemberMatchedSessions.collectAsStateWithLifecycle()
  val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
  val actionFeedbackMessage by viewModel.actionFeedbackMessage.collectAsStateWithLifecycle()

  val churches by viewModel.churchesState.collectAsStateWithLifecycle()
  val churchesUiState by viewModel.churchesUiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  if (!sessionState.isInitialized) {
    OmniChurchSplashScreen()
  } else if (!sessionState.isAuthenticated) {
    OmniChurchAuthPortalScreen(
      churches = churches,
      churchesUiState = churchesUiState,
      onRetryLoadChurches = { viewModel.retryLoadChurches() },
      sessionState = sessionState,
      isAuthenticating = isAuthenticating,
      currentLanguageCode = currentLanguageCode,
      onSelectLanguage = { code ->
        scope.launch { preferencesManager.setLanguageCode(code) }
      },
      errorFeedback = actionFeedbackMessage,
      onClearFeedback = { viewModel.clearFeedbackMessage() },
      onLoginMemberWithPin = { selectedChurchId, idOrPhone, pin ->
        viewModel.loginMemberWithPin(
          selectedChurchId = selectedChurchId,
          idOrPhone = idOrPhone,
          enteredPin = pin,
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onUnlockBiometricSession = { viewModel.unlockSavedBiometricSession {} },
      onOptionalGoogleMemberSignIn = { selectedChurchId, googlePayload, onNeedsLinking ->
        viewModel.handleOptionalGoogleMemberLogin(
          selectedChurchId = selectedChurchId,
          googleEmail = googlePayload.email,
          onDirectMemberLoginSuccess = {},
          onNeedsPinVerificationPrompt = onNeedsLinking,
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onLinkGoogleWithMatrikilPin = { selectedChurchId, idOrPhone, pin, googlePayload, onLinked ->
        viewModel.verifyPinAndLinkGoogleAccount(
          selectedChurchId = selectedChurchId,
          idOrPhone = idOrPhone,
          enteredPin = pin,
          googleEmail = googlePayload.email,
          googleUid = googlePayload.uid,
          googlePhotoUrl = googlePayload.photoUrl,
          onSuccess = onLinked,
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onLoginAdmin = { selectedChurchId, enteredPasscode, googleEmail, googleUid ->
        viewModel.loginAdminForChurch(
          selectedChurchId = selectedChurchId,
          enteredPasscode = enteredPasscode,
          googleEmail = googleEmail,
          googleUid = googleUid,
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onLoginAdminWithEmailAndPassword = { selectedChurchId, email, password ->
        viewModel.signInAdminWithEmailAndPassword(
          email = email,
          password = password,
          selectedChurchId = selectedChurchId,
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      rememberedAdminCredentials = rememberedAdminCredentials,
      pendingAdminOwnedChurches = pendingAdminOwnedChurches,
      onToggleRememberAdmin = { enabled ->
        viewModel.setRememberAdminCredentialsEnabled(enabled)
      },
      onLoginAdminSmartWithEmailAndPassword = { email, password, rememberMe ->
        viewModel.signInAdminSmartWithEmailAndPassword(
          email = email,
          password = password,
          rememberMe = rememberMe,
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onLoginAdminSmartWithGoogle = {
        viewModel.signInAdminSmartWithCurrentGoogleUser(
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onSelectOwnedAdminChurch = { ownedChurch ->
        viewModel.selectOwnedAdminChurch(
          church = ownedChurch,
          onSuccess = {},
          onError = { msg -> viewModel.setFeedbackMessage(msg) },
        )
      },
      onCancelOwnedAdminChurchSelection = {
        viewModel.cancelOwnedAdminChurchSelection()
      },
      pendingMemberMatchedSessions = pendingMemberMatchedSessions,
      onSelectMatchedMemberSession = { matchedSession ->
        viewModel.selectMatchedMemberSession(matchedSession)
      },
      onCancelMatchedMemberChurchSelection = {
        viewModel.cancelMatchedMemberChurchSelection()
      },
      onSendAdminPasswordReset = { email, onSent, onError ->
        viewModel.sendAdminPasswordResetEmail(
          email = email,
          onSuccess = onSent,
          onError = onError,
        )
      },
    )
  } else {
    AuthenticatedOmniChurchWorkspace(
      viewModel = viewModel,
      adminAuthViewModel = adminAuthViewModel,
      preferencesManager = preferencesManager,
      currentLanguageCode = currentLanguageCode,
    )
  }
}

@Composable
private fun AuthenticatedOmniChurchWorkspace(
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
  val churchMembersUiState by viewModel.churchMembersUiState.collectAsStateWithLifecycle()
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

  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route ?: OmniRoutes.HOME

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
  val isAdminLocked =
    isAdminSession &&
      activeChurch != null &&
      (
        lockedChurches[activeChurch!!.id] == true ||
          (adminFirebaseUser == null && activeChurch!!.hasCloudOwner)
      )

  DisposableEffect(lifecycleOwner, isAdminSession, activeChurch?.id, activeChurch?.effectiveAutoLockMinutes) {
    val observer =
      LifecycleEventObserver { _, event ->
        when (event) {
          Lifecycle.Event.ON_STOP -> adminAuthViewModel.onAppStopped(isAdminSession)
          Lifecycle.Event.ON_START -> adminAuthViewModel.onAppStarted(isAdminSession, activeChurch)
          else -> Unit
        }
      }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  LaunchedEffect(isAdminSession, isAdminLocked, activeChurch?.id, activeChurch?.effectiveAutoLockMinutes) {
    adminAuthViewModel.updateInactivityWatcher(
      isAdminSession = isAdminSession,
      isAdminUnlocked = !isAdminLocked,
      church = activeChurch,
    )
  }

  val resolvedChurchId =
    activeChurch?.id?.trim()?.takeIf { it.isNotBlank() }
      ?: sessionState.activeChurchId.trim().takeIf { it.isNotBlank() }
      ?: member?.churchId?.trim()?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchId?.trim().orEmpty()
  val resolvedChurchName =
    activeChurch?.name?.trim()?.takeIf { it.isNotBlank() }
      ?: member?.churchName?.trim()?.takeIf { it.isNotBlank() }
      ?: userProfile?.churchName?.trim()?.takeIf { it.isNotBlank() }
      ?: "OmniChurch Pro"
  val resolvedChurchLogoUrl =
    activeChurch?.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
      ?: member?.churchLogoUrl?.trim()?.takeIf { it.isNotEmpty() }
  val resolvedAuthorName =
    userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
      ?: member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
      ?: activeChurch?.pastor?.trim()?.takeIf { it.isNotBlank() }
      ?: "Admin"
  val availableServicesList = (servicesState as? UiState.Success)?.data ?: emptyList()

  val unreadNotificationsCount = unreadAnnouncementIds.size

  val performFullSignOut: () -> Unit = {
    adminAuthViewModel.signOutAdmin(credentialManager)
    viewModel.logoutCurrentSession {}
    signOutOmniChurch(credentialManager, scope)
  }

  when (val memberUi = currentMemberState) {
    is UiState.Loading -> {
      Box(
        modifier = Modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
        contentAlignment = Alignment.Center,
      ) {
        GlassLoadingIndicator()
      }
    }
    is UiState.Success -> {
      if (memberUi.data == null && !isAdminSession) {
        UnlinkedMemberAccountView(
          title = "Pwofil Manm Pa Jwenn",
          message = "Kont sa a pa asosye ak okenn manm nan legliz sa a. Pou sekirite, kreyasyon manm fèt sèlman sou platfòm Web OmniChurch la oswa pa administratè legliz la. Tanpri konekte ak ID Matrikil + PIN ou.",
          onSignOut = performFullSignOut,
        )
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
            composable(OmniRoutes.HOME) {
              MemberHomeScreen(
                userProfile = userProfile,
                member = member,
                servicesState = servicesState,
                announcementsState = announcementsState,
                unreadNotificationsCount = unreadNotificationsCount,
                onOpenMemberCard = { navController.navigate(OmniRoutes.MEMBER_ID_CARD) },
                onOpenScannerOrGallery = {
                  if (userProfile?.isAuthorizedScanner == true || isAdminSession) {
                    activeAttendanceServices.firstOrNull()?.let {
                      viewModel.selectScannerService(it.id)
                    }
                    navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
                  } else {
                    navController.navigate(OmniRoutes.MEMBER_QR_GALLERY)
                  }
                },
                onOpenServices = { navController.navigate(OmniRoutes.SERVICES) },
                onOpenNotifications = { navController.navigate(OmniRoutes.NOTIFICATIONS) },
                onOpenProfile = { navController.navigate(OmniRoutes.PROFILE_SETTINGS) },
                onOpenMemberList = { navController.navigate(OmniRoutes.ADMIN_MEMBER_LIST) },
                onReactToItem = { targetId, isService, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = targetId,
                    isServiceTarget = isService,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                  )
                },
                observeComments = { targetId ->
                  viewModel.observeCommentsForTarget(resolvedChurchId, targetId)
                },
                onPostComment = { targetId, isService, content ->
                  viewModel.postComment(targetId, isService, content)
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
                },
                onCreateService = { title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
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
                },
                reactionSummaries = reactionSummaries,
                commentCounts = commentCounts,
                onReactToComment = { commentId, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = commentId,
                    isServiceTarget = false,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                    isCommentTarget = true,
                  )
                },
                onDeleteService = { serviceId -> viewModel.deleteService(serviceId) },
                onDeleteAnnouncement = { announcementId ->
                  viewModel.deleteAnnouncement(announcementId)
                },
                isAdminSession = isAdminSession,
                canCreateService = isAdminSession,
                canEditService = isAdminSession,
                onUpdateService = { existingService, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
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
                },
                churchMembers = churchMembers,
                activeChurchName = resolvedChurchName,
                activeChurchLogoUrl = resolvedChurchLogoUrl,
                currentLanguageCode = currentLanguageCode,
              )
            }

            composable("${OmniRoutes.SERVICE_DETAILS}/{serviceId}") { backStackEntry ->
              val serviceId = backStackEntry.arguments?.getString("serviceId").orEmpty()
              ServiceDetailsScreen(
                serviceId = serviceId,
                onBack = { navController.popBackStack() },
              )
            }

            composable(OmniRoutes.SERVICES) {
              ServicesScreen(
                userProfile = userProfile,
                servicesState = servicesState,
                onCreateService = { title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
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
                },
                onToggleAttendanceWindow = { service, isOpen ->
                  viewModel.toggleServiceAttendanceWindow(service, isOpen)
                },
                observeAttendances = { serviceId ->
                  viewModel.observeAttendancesForService(resolvedChurchId, serviceId)
                },
                onOpenScannerForService = { serviceId ->
                  viewModel.selectScannerService(serviceId)
                  navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
                },
                onBackClick = { navController.popBackStack() },
                onDeleteService = { serviceId -> viewModel.deleteService(serviceId) },
                isAdminSession = isAdminSession,
                canCreateService = isAdminSession,
                canEditService = isAdminSession,
                onUpdateService = { existingService, title, description, dateFormatted, lateToleranceMinutes, isOpenForAttendance, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, sendReminder24h, imageUrl ->
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
                },
                reactionSummaries = reactionSummaries,
                commentCounts = commentCounts,
                onReactToService = { serviceId, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = serviceId,
                    isServiceTarget = true,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                  )
                },
                observeComments = { targetId ->
                  viewModel.observeCommentsForTarget(resolvedChurchId, targetId)
                },
                onPostComment = { serviceId, content ->
                  viewModel.postComment(serviceId, true, content)
                },
                onReactToComment = { commentId, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = commentId,
                    isServiceTarget = false,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                    isCommentTarget = true,
                  )
                },
                churchMembers = churchMembers,
                activeChurchLogoUrl = resolvedChurchLogoUrl,
                currentLanguageCode = currentLanguageCode,
              )
            }

            composable(OmniRoutes.MEMBER_ID_CARD) {
              DigitalMemberIdScreen(
                memberState = currentMemberState,
                qrBitmap = memberQrBitmap,
                onBackClick = { navController.popBackStack() },
                onOpenFullScreenQrGallery = {
                  navController.navigate(OmniRoutes.MEMBER_QR_GALLERY)
                },
                onOpenProfileSettings = { navController.navigate(OmniRoutes.PROFILE_SETTINGS) },
                onUpdateMemberPhoto = { newPhotoUrl ->
                  viewModel.updateMemberPhoto(
                    photoDataUrl = newPhotoUrl,
                    onSuccess = {
                      viewModel.setFeedbackMessage("Foto pwofil ou mete ajou avèk siksè!")
                    },
                    onError = { err -> viewModel.setFeedbackMessage(err) },
                  )
                },
              )
            }

            composable(OmniRoutes.MEMBER_QR_GALLERY) {
              MemberQRGalleryScreen(
                member = member,
                qrBitmap = memberQrBitmap,
                attendanceHistory = memberAttendanceHistory,
                effectiveVerificationBaseUrl = verificationConfig.effectiveVerificationBaseUrl,
                onOpenVerificationCertificate = { targetMember ->
                  viewModel.openDirectMemberVerification(targetMember, activeChurch)
                  navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
                },
                onClose = { navController.popBackStack() },
              )
            }

            composable(OmniRoutes.ADMIN_QR_SCANNER) {
              val effectiveProfile = userProfile ?: UserProfile(
                role = if (isAdminSession) "admin" else "usher",
                churchId = resolvedChurchId,
              )
              AdminQRScannerScreen(
                userProfile = effectiveProfile,
                activeServices = activeAttendanceServices,
                selectedServiceId = selectedScannerServiceId,
                validationOutcome = scannerOutcome,
                churchMembers = churchMembers,
                allServices = availableServicesList,
                currentLanguageCode = currentLanguageCode,
                observeServiceAttendances = { serviceId ->
                  viewModel.observeAttendancesForService(resolvedChurchId, serviceId)
                },
                onSelectService = { serviceId -> viewModel.selectScannerService(serviceId) },
                onScanOrSubmitCode = { rawTokenOrCode, service, method ->
                  viewModel.processAttendanceScanOrCode(rawTokenOrCode, service, method)
                },
                onBindNfcCardToMember = { memberDocId, nfcCardUid, onLinked ->
                  viewModel.bindNfcCardToMember(
                    memberDocId = memberDocId,
                    nfcCardUid = nfcCardUid,
                    onSuccess = onLinked,
                  )
                },
                onVerifyCard = { rawTokenOrCode ->
                  viewModel.verifyMemberCard(rawTokenOrCode, resolvedChurchId)
                  navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
                },
                onOpenMemberVerification = { targetMember ->
                  viewModel.openDirectMemberVerification(targetMember, activeChurch)
                  navController.navigate(OmniRoutes.MEMBER_VERIFICATION)
                },
                onResetOutcome = { viewModel.resetScannerOutcome() },
                onOpenServicesManagement = { navController.navigate(OmniRoutes.SERVICES) },
                onBackClick = { navController.popBackStack() },
              )
            }

            composable(OmniRoutes.MEMBER_VERIFICATION) {
              MemberPublicVerificationScreen(
                verificationState = cardVerificationState,
                verificationConfig = verificationConfig,
                isChurchLicensed = { cId, cCode -> adMobController.isChurchLicensed(cId, cCode) },
                initialLanguageCode = currentLanguageCode,
                onRetry = { viewModel.retryLastCardVerification() },
                onClose = {
                  viewModel.clearCardVerificationState()
                  navController.popBackStack()
                },
              )
            }

            composable(OmniRoutes.ADMIN_MEMBER_LIST) {
              AdminMemberListScreen(
                userProfile = userProfile,
                isAdminSession = isAdminSession,
                churchMembers = churchMembers,
                churchMembersUiState = churchMembersUiState,
                onRetryLoadMembers = { viewModel.retryLoadChurchMembers() },
                onBackClick = { navController.popBackStack() },
                onAdminUpdateMember = { memberDocId, fullName, department, ministryRole, phone, titlePrefix ->
                  viewModel.updateChurchMemberByAdmin(
                    memberDocId = memberDocId,
                    fullName = fullName,
                    department = department,
                    ministryRole = ministryRole,
                    phone = phone,
                    titlePrefix = titlePrefix,
                  )
                },
                onRegisterNewMember = { fullName, customMatrikil, pin, phone, department, ministryRole ->
                  val targetChurch = activeChurch ?: Church(id = resolvedChurchId, name = resolvedChurchName)
                  viewModel.registerNewMemberByAdmin(
                    church = targetChurch,
                    fullName = fullName,
                    customMatrikil = customMatrikil,
                    pin = pin,
                    phone = phone,
                    department = department,
                    ministryRole = ministryRole,
                    onSuccess = { newMem ->
                      viewModel.setFeedbackMessage(
                        "Manm ${newMem.fullName} anrejistre! ID Matrikil: ${newMem.effectiveMatrikil}"
                      )
                    },
                    onError = { err -> viewModel.setFeedbackMessage(err) },
                  )
                },
              )
            }

            composable(OmniRoutes.NOTIFICATIONS) {
              NotificationsScreen(
                notificationsState = notificationsState,
                onMarkRead = { notifId -> viewModel.markNotificationAsRead(notifId) },
                onMarkAllRead = { list -> viewModel.markAllNotificationsRead(list) },
                onBackClick = { navController.popBackStack() },
                isAdmin = isAdminSession,
                canPublishAnnouncement = isAdminSession,
                canEditAnnouncement = isAdminSession,
                availableServices = availableServicesList,
                onCreateAnnouncement =
                  if (isAdminSession) {
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
                  } else null,
                onUpdateAnnouncement =
                  if (isAdminSession) {
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
                      viewModel.updateAnnouncement(
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
                announcementsState = announcementsState,
                unreadAnnouncementIds = unreadAnnouncementIds,
                onMarkAnnouncementRead = { announcementId ->
                  viewModel.markAnnouncementAsRead(announcementId)
                },
                onMarkAllAnnouncementsRead = {
                  viewModel.markAllAnnouncementsAsRead()
                },
                onRetryLoadAnnouncements = {
                  viewModel.retryLoadAnnouncements()
                },
                currentLanguageCode = currentLanguageCode,
                reactionSummaries = reactionSummaries,
                commentCounts = commentCounts,
                onReactToAnnouncement = { announcementId, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = announcementId,
                    isServiceTarget = false,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                  )
                },
                observeComments = { targetId ->
                  viewModel.observeCommentsForTarget(resolvedChurchId, targetId)
                },
                onPostComment = { targetId, content ->
                  viewModel.postComment(targetId, false, content)
                },
                onReactToComment = { commentId, reactionType ->
                  viewModel.reactToFeedItem(
                    targetId = commentId,
                    isServiceTarget = false,
                    churchId = resolvedChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                    isCommentTarget = true,
                  )
                },
                onDeleteAnnouncement = { announcementId ->
                  viewModel.deleteAnnouncement(announcementId)
                },
              )
            }

            composable(OmniRoutes.PROFILE_SETTINGS) {
              ProfileAndSettingsScreen(
                userProfile = userProfile,
                member = member,
                currentLanguageCode = currentLanguageCode,
                onLanguageChange = { newLang ->
                  scope.launch { preferencesManager.setLanguageCode(newLang) }
                },
                onSaveMemberChanges = { fullName, department, role ->
                  viewModel.updateMemberProfileInfo(fullName, department, role) {}
                },
                onSignOut = performFullSignOut,
                onBackClick = { navController.popBackStack() },
                biometricEnabled = sessionState.biometricEnabled,
                onToggleBiometric = { enabled -> viewModel.toggleBiometricQuickUnlock(enabled) },
                onLinkGoogleAccount = { payload ->
                  viewModel.linkGoogleToCurrentMember(
                    googleEmail = payload.email,
                    googleUid = payload.uid,
                    googlePhotoUrl = payload.photoUrl,
                    onSuccess = {
                      viewModel.setFeedbackMessage("Kont Google (${payload.email}) lye avèk siksè!")
                    },
                    onError = { err -> viewModel.setFeedbackMessage(err) },
                  )
                },
                onRegisterNewMember =
                  if (isAdminSession) {
                    { fullName, customMatrikil, pin, phone, department, ministryRole ->
                      val targetChurch = activeChurch ?: Church(id = resolvedChurchId, name = resolvedChurchName)
                      viewModel.registerNewMemberByAdmin(
                        church = targetChurch,
                        fullName = fullName,
                        customMatrikil = customMatrikil,
                        pin = pin,
                        phone = phone,
                        department = department,
                        ministryRole = ministryRole,
                        onSuccess = { newMem ->
                          viewModel.setFeedbackMessage(
                            "Manm ${newMem.fullName} anrejistre! ID Matrikil: ${newMem.effectiveMatrikil}"
                          )
                        },
                        onError = { err -> viewModel.setFeedbackMessage(err) },
                      )
                    }
                  } else null,
                churchMembers = churchMembers,
                onAdminUpdateMember =
                  if (isAdminSession) {
                    { memberDocId, fullName, department, ministryRole, phone, titlePrefix ->
                      viewModel.updateChurchMemberByAdmin(
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
                isAdminSession = isAdminSession,
                isAdminSecurityBusy = adminAuthBusy,
                onSaveChurchSecuritySettings =
                  if (isAdminSession && activeChurch != null) {
                    { quickPinEnabled, newPinOrNull, autoLockMinutes ->
                      adminAuthViewModel.updateSecuritySettings(
                        church = activeChurch!!,
                        quickPinEnabled = quickPinEnabled,
                        newPinOrNull = newPinOrNull,
                        autoLockMinutes = autoLockMinutes,
                        languageCode = currentLanguageCode,
                        onSuccess = {
                          viewModel.setFeedbackMessage(
                            when (currentLanguageCode.lowercase()) {
                              "fr" -> "Paramètres de sécurité enregistrés !"
                              "en" -> "Security settings saved!"
                              "es" -> "¡Configuración de seguridad guardada!"
                              else -> "Paramèt sekirite ak PIN rapid yo anrejistre!"
                            }
                          )
                        },
                        onError = { err -> viewModel.setFeedbackMessage(err) },
                      )
                    }
                  } else null,
                onLockAdminNow =
                  if (isAdminSession && activeChurch != null) {
                    { adminAuthViewModel.lockChurchNow(activeChurch!!.id) }
                  } else null,
              )
            }
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

          if (showBottomBar) {
            val selectedTab =
              when (currentRoute) {
                OmniRoutes.HOME -> OmniNavTab.HOME
                OmniRoutes.SERVICES -> OmniNavTab.SERVICES
                OmniRoutes.NOTIFICATIONS -> OmniNavTab.NOTIFICATIONS
                OmniRoutes.PROFILE_SETTINGS -> OmniNavTab.PROFILE
                else -> OmniNavTab.MEMBER_CARD_OR_SCANNER
              }
            val resolvedBottomNavAvatarUrl =
              member?.effectivePhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
                ?: userProfile?.photoUrl?.trim()?.takeIf { it.isNotEmpty() }
                ?: if (isAdminSession) {
                  activeChurch?.adminGooglePhoto?.trim()?.takeIf { it.isNotEmpty() }
                    ?: adminFirebaseUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    ?: activeChurch?.logoUrl?.trim()?.takeIf { it.isNotEmpty() }
                } else {
                  adminFirebaseUser?.photoUrl?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                }
            val resolvedBottomNavDisplayName =
              member?.effectiveFullName?.trim()?.takeIf { it.isNotBlank() }
                ?: userProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: activeChurch?.adminGoogleName?.trim()?.takeIf { it.isNotBlank() }
                ?: resolvedAuthorName

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
                navController.navigate(targetRoute) {
                  popUpTo(OmniRoutes.HOME) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              onCenterQrOrbClick = {
                if (userProfile?.isAuthorizedScanner == true || isAdminSession) {
                  activeAttendanceServices.firstOrNull()?.let {
                    viewModel.selectScannerService(it.id)
                  }
                  navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
                } else {
                  navController.navigate(OmniRoutes.MEMBER_QR_GALLERY)
                }
              },
              userAvatarUrl = resolvedBottomNavAvatarUrl,
              userDisplayName = resolvedBottomNavDisplayName,
              modifier = Modifier.align(Alignment.BottomCenter),
            )
          }

          if (isAdminSession && !isAdminLocked && autoLockWarningSeconds != null && activeChurch != null) {
            AutoLockWarningDialog(
              secondsRemaining = autoLockWarningSeconds!!,
              currentLanguageCode = currentLanguageCode,
              onStayActive = { adminAuthViewModel.recordUserInteraction() },
              onLockNow = { adminAuthViewModel.lockChurchNow(activeChurch!!.id) },
            )
          }

          if (isAdminLocked && activeChurch != null) {
            BlockingAdminAuthGateOverlay(
              church = activeChurch!!,
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
                  church = activeChurch,
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
                  church = activeChurch,
                  languageCode = currentLanguageCode,
                  onSuccess = {},
                )
              },
              onQuickPinSubmit = { enteredPin ->
                adminAuthViewModel.unlockWithQuickPin(
                  church = activeChurch,
                  pin = enteredPin,
                  languageCode = currentLanguageCode,
                  onSuccess = {},
                )
              },
              onBiometricSuccess = {
                adminAuthViewModel.authorizeViaBiometric(
                  church = activeChurch,
                  languageCode = currentLanguageCode,
                  onSuccess = {},
                )
              },
              onVerifyLegacyPin = { pin ->
                adminAuthViewModel.verifyLegacyPinStep(activeChurch, pin, currentLanguageCode)
              },
              onCompleteLegacyMigration = { email, newPass, confirmPass ->
                adminAuthViewModel.completeLegacyMigration(
                  church = activeChurch,
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
    is UiState.Error -> {
      UnlinkedMemberAccountView(
        title = "Erè nan Chajman",
        message = memberUi.message.ifBlank { "Gen yon erè ki pase pandan n ap chaje done yo. Tanpri verifye koneksyon entènèt ou epi re-eseye." },
        onSignOut = performFullSignOut,
      )
    }
  }
}

@Composable
private fun UnlinkedMemberAccountView(
  title: String,
  message: String,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.padding(24.dp).fillMaxWidth(0.92f),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = DeepNavy.copy(alpha = 0.95f)),
      border = BorderStroke(1.dp, GlassCardBorderSubtle),
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(54.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          ),
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MutedGlassText,
            lineHeight = 22.sp,
          ),
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
          onClick = onSignOut,
          modifier = Modifier.fillMaxWidth().height(52.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandGold),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = DeepNavy)
          Spacer(modifier = Modifier.size(8.dp))
          Text("Dekonekte", color = DeepNavy, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
