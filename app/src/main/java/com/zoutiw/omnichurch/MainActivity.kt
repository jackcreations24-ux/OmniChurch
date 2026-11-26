package com.zoutiw.omnichurch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.zoutiw.omnichurch.core.network.FirestoreProvider
import com.zoutiw.omnichurch.core.network.NamedFirestoreProvider
import com.zoutiw.omnichurch.core.utilities.LocaleManager
import com.zoutiw.omnichurch.core.utilities.NfcIntentBus
import com.zoutiw.omnichurch.core.utilities.OmniPreferencesManager
import com.zoutiw.omnichurch.data.repositories.OmniChurchRepository
import com.zoutiw.omnichurch.presentation.components.OmniChurchSplashScreen
import com.zoutiw.omnichurch.presentation.navigation.AdminAuthViewModel
import com.zoutiw.omnichurch.presentation.navigation.OmniChurchViewModel
import com.zoutiw.omnichurch.presentation.screens.auth.OmniChurchAuthPortalScreen
import com.zoutiw.omnichurch.ui.theme.OmniChurchTheme
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
  const val DONATE = "donate"
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
      val localeManager =
        remember(activityContext) { LocaleManager(activityContext.applicationContext) }
      val initialSavedLang =
        remember(activityContext) {
          try {
            activityContext.applicationContext
              .getSharedPreferences("omnichurch_prefs", Context.MODE_PRIVATE)
              .getString("omnichurch_lang_v1", LocaleManager.DEFAULT_LANGUAGE)
              ?.takeIf { it in LocaleManager.SUPPORTED_LANGUAGES }
              ?: LocaleManager.DEFAULT_LANGUAGE
          } catch (_: Exception) {
            LocaleManager.DEFAULT_LANGUAGE
          }
        }
      val languageCode by
        preferencesManager.languageCodeFlow.collectAsStateWithLifecycle(
          initialValue = initialSavedLang
        )

      val localizedContext =
        remember(activityContext, languageCode) {
          localeManager.updateRuntimeConfiguration(activityContext, languageCode)
        }

      CompositionLocalProvider(LocalContext provides localizedContext) {
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
