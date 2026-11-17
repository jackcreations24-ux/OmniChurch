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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.zoutiw.omnichurch.presentation.components.OmniNavTab
import com.zoutiw.omnichurch.presentation.navigation.OmniChurchViewModel
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
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    setTheme(R.style.Theme_MyApplication)
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
            val repository = OmniChurchRepository(firestore)
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

  LaunchedEffect(currentUser?.uid) {
    viewModel.syncAuthIdentity(currentUser?.uid.orEmpty())
  }

  val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
  val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
  val actionFeedbackMessage by viewModel.actionFeedbackMessage.collectAsStateWithLifecycle()

  val churches by viewModel.churchesState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  if (!sessionState.isInitialized) {
    Box(
      modifier = Modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
      contentAlignment = Alignment.Center,
    ) {
      GlassLoadingIndicator()
    }
  } else if (!sessionState.isAuthenticated) {
    OmniChurchAuthPortalScreen(
      churches = churches,
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
      preferencesManager = preferencesManager,
      currentLanguageCode = currentLanguageCode,
    )
  }
}

@Composable
private fun AuthenticatedOmniChurchWorkspace(
  viewModel: OmniChurchViewModel,
  preferencesManager: OmniPreferencesManager,
  currentLanguageCode: String,
) {
  val context = LocalContext.current
  val activityContext = remember(context) { context.findActivity() ?: context }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }

  val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val currentMemberState by viewModel.currentMemberState.collectAsStateWithLifecycle()
  val servicesState by viewModel.servicesState.collectAsStateWithLifecycle()
  val activeAttendanceServices by viewModel.activeAttendanceServicesState.collectAsStateWithLifecycle()
  val churchMembers by viewModel.churchMembersState.collectAsStateWithLifecycle()
  val announcementsState by viewModel.announcementsState.collectAsStateWithLifecycle()
  val reactionSummaries by viewModel.reactionSummariesState.collectAsStateWithLifecycle()
  val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
  val memberQrBitmap by viewModel.memberQrBitmap.collectAsStateWithLifecycle()
  val scannerOutcome by viewModel.scannerOutcome.collectAsStateWithLifecycle()
  val selectedScannerServiceId by viewModel.selectedScannerServiceId.collectAsStateWithLifecycle()
  val actionFeedbackMessage by viewModel.actionFeedbackMessage.collectAsStateWithLifecycle()

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

  val unreadNotificationsCount =
    when (val notifUi = notificationsState) {
      is UiState.Success -> notifUi.data.count { !it.isRead }
      else -> 0
    }

  val performFullSignOut: () -> Unit = {
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
        Box(modifier = Modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient)) {
          NavHost(
            navController = navController,
            startDestination = OmniRoutes.HOME,
            modifier = Modifier.fillMaxSize(),
          ) {
            composable(OmniRoutes.HOME) {
              MemberHomeScreen(
                userProfile = userProfile,
                member = member,
                isAdminSession = isAdminSession,
                servicesState = servicesState,
                announcementsState = announcementsState,
                unreadNotificationsCount = unreadNotificationsCount,
                onOpenMemberCard = { navController.navigate(OmniRoutes.MEMBER_ID_CARD) },
                onOpenMemberList = { navController.navigate(OmniRoutes.ADMIN_MEMBER_LIST) },
                onSelectServiceCard = { serviceId -> navController.navigate("${OmniRoutes.SERVICE_DETAILS}/$serviceId") },
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
                onReactToItem = { targetId, isService, reactionType ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  viewModel.reactToFeedItem(
                    targetId = targetId,
                    isServiceTarget = isService,
                    churchId = targetChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                  )
                },
                observeComments = { targetId ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  viewModel.observeCommentsForTarget(targetChurchId, targetId)
                },
                onPostComment = { targetId, isService, content ->
                  viewModel.postComment(targetId, isService, content)
                },
                onCreateAnnouncement = { title, content, department, isPinned ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  val author = userProfile?.displayName ?: member?.effectiveFullName ?: "Admin"
                  viewModel.createAnnouncement(
                    churchId = targetChurchId,
                    title = title,
                    content = content,
                    isPinned = isPinned,
                    authorName = author,
                  )
                },
                onCreateService = { title, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers, isOpen, sendReminder24h, tol, isLive ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  val churchName = member?.churchName ?: userProfile?.churchName ?: "OmniChurch Pro"
                  val author = userProfile?.displayName ?: member?.effectiveFullName ?: "Admin"
                  viewModel.createService(
                    churchId = targetChurchId,
                    churchName = churchName,
                    title = title,
                    lateToleranceMinutes = tol,
                    isOpenForAttendance = isOpen,
                    isLive = isLive,
                    authorName = author,
                    date = date,
                    category = category,
                    startTime = startTime,
                    endTime = endTime,
                    theme = theme,
                    passage = passage,
                    leader = leader,
                    preacher = preacher,
                    reader = reader,
                    musicians = musicians,
                    ushers = ushers,
                    sendReminder24h = sendReminder24h
                  )
                },
                reactionSummaries = reactionSummaries,
                onReactToComment = { commentId, reactionType ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  viewModel.reactToFeedItem(
                    targetId = commentId,
                    isServiceTarget = false,
                    churchId = targetChurchId,
                    reactionType = OmniReactionType.fromKey(reactionType),
                    isCommentTarget = true,
                  )
                },
                onDeleteService = { serviceId -> viewModel.deleteService(serviceId) },
                onDeleteAnnouncement = { announcementId ->
                  viewModel.deleteAnnouncement(announcementId)
                },
                onUpdateService = { serviceId, title, description, dateFormatted, lateToleranceMinutes, isOpen, isLive, category, date, startTime, endTime, theme, passage, leader, preacher, reader, musicians, ushers ->
                  viewModel.updateService(
                    serviceId = serviceId,
                    title = title,
                    description = description,
                    lateToleranceMinutes = lateToleranceMinutes,
                    isOpenForAttendance = isOpen,
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
                  )
                },
              )
            }

            composable("${OmniRoutes.SERVICE_DETAILS}/{serviceId}") { backStackEntry ->
              val serviceId = backStackEntry.arguments?.getString("serviceId").orEmpty()
              val servicesList = (servicesState as? UiState.Success)?.data ?: emptyList()
              val service = servicesList.firstOrNull { it.id == serviceId } ?: servicesList.firstOrNull()
              if (service != null) {
                ServiceDetailsScreen(
                  service = service,
                  reactionSummary = reactionSummaries[service.id],
                  onReactClick = {
                    val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                      ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                      ?: sessionState.activeChurchId
                    viewModel.reactToFeedItem(
                      targetId = service.id,
                      isServiceTarget = true,
                      churchId = targetChurchId,
                    )
                  },
                  onCommentClick = {
                    navController.navigate(OmniRoutes.HOME)
                  },
                  onBackClick = { navController.popBackStack() },
                )
              }
            }

            composable(OmniRoutes.SERVICES) {
              ServicesScreen(
                userProfile = userProfile,
                servicesState = servicesState,
                onCreateService = { title, description, dateFormatted, lateToleranceMinutes, isOpen, isLive ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  val churchName = member?.churchName ?: userProfile?.churchName ?: "OmniChurch Pro"
                  val author = userProfile?.displayName ?: member?.effectiveFullName ?: "Admin"
                  viewModel.createService(
                    churchId = targetChurchId,
                    churchName = churchName,
                    title = title,
                    lateToleranceMinutes = lateToleranceMinutes,
                    isOpenForAttendance = isOpen,
                    isLive = isLive,
                    authorName = author,
                    date = dateFormatted,
                  )
                },
                onToggleAttendanceWindow = { service, isOpen ->
                  viewModel.toggleServiceAttendanceWindow(service, isOpen)
                },
                observeAttendances = { serviceId ->
                  val targetChurchId = member?.churchId?.takeIf { it.isNotBlank() }
                  ?: userProfile?.churchId?.takeIf { it.isNotBlank() }
                  ?: sessionState.activeChurchId
                  viewModel.observeAttendancesForService(targetChurchId, serviceId)
                },
                onOpenScannerForService = { serviceId ->
                  viewModel.selectScannerService(serviceId)
                  navController.navigate(OmniRoutes.ADMIN_QR_SCANNER)
                },
                onBackClick = { navController.popBackStack() },
                onDeleteService = { serviceId -> viewModel.deleteService(serviceId) },
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
                onClose = { navController.popBackStack() },
              )
            }

            composable(OmniRoutes.ADMIN_QR_SCANNER) {
              val effectiveProfile = userProfile ?: UserProfile(
                role = if (isAdminSession) "admin" else "usher",
                churchId = member?.churchId ?: userProfile?.churchId.orEmpty().ifBlank { sessionState.activeChurchId }
              )
              AdminQRScannerScreen(
                userProfile = effectiveProfile,
                activeServices = activeAttendanceServices,
                selectedServiceId = selectedScannerServiceId,
                validationOutcome = scannerOutcome,
                churchMembers = churchMembers,
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
                onResetOutcome = { viewModel.resetScannerOutcome() },
                onOpenServicesManagement = { navController.navigate(OmniRoutes.SERVICES) },
                onBackClick = { navController.popBackStack() },
              )
            }

            composable(OmniRoutes.ADMIN_MEMBER_LIST) {
              AdminMemberListScreen(
                userProfile = userProfile,
                isAdminSession = isAdminSession,
                churchMembers = churchMembers,
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
                  val targetChurch = Church(id = sessionState.activeChurchId, name = member?.churchName ?: "OmniChurch Pro")
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
                      val targetChurch = Church(id = sessionState.activeChurchId, name = member?.churchName ?: "OmniChurch Pro")
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
              )
            }
          }

          val showBottomBar =
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

            GlassBottomNavigation(
              selectedTab = selectedTab,
              unreadNotificationsCount = unreadNotificationsCount,
              isAdmin = isAdminSession,
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
              modifier = Modifier.align(Alignment.BottomCenter),
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
