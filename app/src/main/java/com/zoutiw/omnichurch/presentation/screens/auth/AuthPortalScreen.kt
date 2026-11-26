package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.core.legal.LegalLinksRepository
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.RememberedAdminCredentials
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.presentation.components.AuthLegalFooter
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.OmniChurchProBadge
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun OmniChurchAuthPortalScreen(
  churches: List<Church>,
  churchesUiState: UiState<List<Church>> = UiState.Success(churches),
  onRetryLoadChurches: () -> Unit = {},
  sessionState: OmniSessionState,
  isAuthenticating: Boolean,
  errorFeedback: String?,
  onClearFeedback: () -> Unit,
  onLoginMemberWithPin: (selectedChurchId: String?, idOrPhone: String, pin: String) -> Unit,
  onUnlockBiometricSession: () -> Unit,
  onOptionalGoogleMemberSignIn: (
    selectedChurchId: String?,
    googlePayload: GoogleAccountPayload,
    onNeedsLinking: () -> Unit,
  ) -> Unit,
  onLinkGoogleWithMatrikilPin: (
    selectedChurchId: String?,
    idOrPhone: String,
    pin: String,
    googlePayload: GoogleAccountPayload,
    onLinked: () -> Unit,
  ) -> Unit,
  onLoginAdmin: (
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ) -> Unit,
  onLoginAdminWithEmailAndPassword: (
    selectedChurchId: String?,
    email: String,
    password: String,
  ) -> Unit = { _, _, _ -> },
  rememberedAdminCredentials: RememberedAdminCredentials = RememberedAdminCredentials(),
  pendingAdminOwnedChurches: List<Church> = emptyList(),
  onToggleRememberAdmin: (Boolean) -> Unit = {},
  onLoginAdminSmartWithEmailAndPassword: (
    email: String,
    password: String,
    rememberMe: Boolean,
  ) -> Unit = { email, password, _ ->
    onLoginAdminWithEmailAndPassword(null, email, password)
  },
  onLoginAdminSmartWithGoogle: () -> Unit = {},
  onSelectOwnedAdminChurch: (Church) -> Unit = {},
  onCancelOwnedAdminChurchSelection: () -> Unit = {},
  pendingMemberMatchedSessions: List<AuthenticatedMemberSession> = emptyList(),
  onSelectMatchedMemberSession: (AuthenticatedMemberSession) -> Unit = {},
  onCancelMatchedMemberChurchSelection: () -> Unit = {},
  onSendAdminPasswordReset: (
    email: String,
    onSent: (String) -> Unit,
    onError: (String) -> Unit,
  ) -> Unit = { _, _, _ -> },
  currentLanguageCode: String = "ht",
  onSelectLanguage: (String) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val rawContext = LocalContext.current
  val context = remember(rawContext) { rawContext.findActivity() ?: rawContext }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  val legalLinksRepository = remember(rawContext) { LegalLinksRepository.getInstance(rawContext) }
  val legalLinks by legalLinksRepository.legalLinksState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  var isMemberMode by remember { mutableStateOf(true) }
  var isAdminEmailMode by remember { mutableStateOf(true) }
  var selectedChurchId by remember { mutableStateOf<String?>(null) }
  var showChurchSelectorSheet by remember { mutableStateOf(false) }

  var matrikilOrPhoneInput by remember { mutableStateOf("") }
  var memberPinInput by remember { mutableStateOf("") }

  var adminEmailInput by remember { mutableStateOf(rememberedAdminCredentials.email) }
  var adminPasswordInput by remember { mutableStateOf(rememberedAdminCredentials.password) }
  var rememberAdminMe by remember { mutableStateOf(rememberedAdminCredentials.rememberEnabled) }
  var hasPrefilledAdminCreds by remember {
    mutableStateOf(
      rememberedAdminCredentials.email.isNotBlank() ||
        rememberedAdminCredentials.password.isNotBlank()
    )
  }
  var hasUserEditedAdminCreds by remember { mutableStateOf(false) }
  var isAdminPasswordVisible by remember { mutableStateOf(false) }

  var adminPasscodeInput by remember { mutableStateOf("") }
  var isAdminPasscodeVisible by remember { mutableStateOf(false) }
  var showLegacyAdminCodeFallback by remember { mutableStateOf(false) }
  var adminGooglePayload by remember { mutableStateOf<GoogleAccountPayload?>(null) }

  var pendingGoogleLinkPayload by remember { mutableStateOf<GoogleAccountPayload?>(null) }
  var linkMatrikilInput by remember { mutableStateOf("") }
  var linkPinInput by remember { mutableStateOf("") }

  var localError by remember { mutableStateOf<String?>(null) }
  var resetFeedbackMessage by remember { mutableStateOf<String?>(null) }
  val displayedError = localError ?: errorFeedback

  val normalizedLang = currentLanguageCode.lowercase()
  val copy = remember(normalizedLang) { AuthPortalCopy.forLanguage(normalizedLang) }

  LaunchedEffect(rememberedAdminCredentials) {
    rememberAdminMe = rememberedAdminCredentials.rememberEnabled
    if (!hasUserEditedAdminCreds && !hasPrefilledAdminCreds) {
      if (rememberedAdminCredentials.email.isNotBlank() || rememberedAdminCredentials.password.isNotBlank()) {
        adminEmailInput = rememberedAdminCredentials.email
        adminPasswordInput = rememberedAdminCredentials.password
        hasPrefilledAdminCreds = true
      }
    }
  }

  LaunchedEffect(churches, isMemberMode) {
    if (!isMemberMode && selectedChurchId == null && churches.size == 1) {
      selectedChurchId = churches.first().id
    }
  }

  val resolvedChurch =
    remember(churches, selectedChurchId, matrikilOrPhoneInput) {
      OmniAuthEvaluator.resolveChurchForMemberLogin(
        churches = churches,
        selectedChurchId = selectedChurchId,
        idOrPhoneInput = matrikilOrPhoneInput,
      )
        ?: churches.firstOrNull {
          it.id.equals(selectedChurchId ?: "", ignoreCase = true) ||
            (it.code ?: "").equals(selectedChurchId ?: "", ignoreCase = true)
        }
    }

  Box(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient),
  ) {
    OmniChurchSanctuaryHeroCanvas(
      modifier = Modifier.fillMaxWidth().height(320.dp).align(Alignment.TopCenter)
    )

    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .navigationBarsPadding()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      OmniChurchLogo(
        variant = LogoVariant.Full,
        modifier = Modifier.height(112.dp),
        contentDescription = "OmniChurch PRO Official Logo",
      )
      Spacer(modifier = Modifier.height(6.dp))
      OmniChurchProBadge(label = "PRO")

      AuthLanguageSelectorStrip(
        currentLanguageCode = currentLanguageCode,
        onSelectLanguage = onSelectLanguage,
      )

      Spacer(modifier = Modifier.height(14.dp))

      AuthPortalModeSegmentedTabs(
        isMemberMode = isMemberMode,
        memberTabLabel = copy.memberTabLabel,
        adminTabLabel = copy.adminTabLabel,
        onSelectMemberMode = { memberMode ->
          isMemberMode = memberMode
          localError = null
          onClearFeedback()
        },
      )

      Spacer(modifier = Modifier.height(14.dp))

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = if (isMemberMode) copy.memberTitleText else copy.adminTitleText,
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            when {
              isMemberMode -> copy.memberSubtitleText
              isAdminEmailMode -> copy.adminSubtitleText
              else -> copy.adminGoogleSubtitleText
            },
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )

        AuthChurchStatusBannerAndErrorSection(
          showPublicChurchSelector = !isMemberMode && !isAdminEmailMode && showLegacyAdminCodeFallback,
          churches = churches,
          churchesUiState = churchesUiState,
          resolvedChurch = resolvedChurch,
          isMemberMode = isMemberMode,
          autoMemberBadgeLabel = copy.autoMemberBadgeLabel,
          autoChurchBadgeLabel = copy.autoChurchBadgeLabel,
          displayedError = displayedError,
          onOpenChurchSelector = {
            if (churchesUiState is UiState.Error || (churchesUiState is UiState.Success && churches.isEmpty())) {
              onRetryLoadChurches()
            }
            showChurchSelectorSheet = true
          },
        )

        if (isMemberMode) {
          AuthMemberLoginSection(
            copy = copy,
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            sessionState = sessionState,
            isAuthenticating = isAuthenticating,
            matrikilOrPhoneInput = matrikilOrPhoneInput,
            onMatrikilOrPhoneChange = { matrikilOrPhoneInput = it },
            memberPinInput = memberPinInput,
            onMemberPinChange = { memberPinInput = it },
            onClearFeedback = onClearFeedback,
            onLocalError = { localError = it },
            onLoginMemberWithPin = onLoginMemberWithPin,
            onUnlockBiometricSession = onUnlockBiometricSession,
            onOptionalGoogleMemberSignIn = onOptionalGoogleMemberSignIn,
            onNeedsGoogleLinking = { googlePayload ->
              pendingGoogleLinkPayload = googlePayload
              linkMatrikilInput = matrikilOrPhoneInput
              linkPinInput = memberPinInput
            },
          )
        } else {
          AuthAdminFormSection(
            copy = copy,
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            isAuthenticating = isAuthenticating,
            isAdminEmailMode = isAdminEmailMode,
            onSelectAdminEmailMode = { emailMode ->
              isAdminEmailMode = emailMode
              if (emailMode) showLegacyAdminCodeFallback = false
            },
            adminEmailInput = adminEmailInput,
            onAdminEmailChange = {
              adminEmailInput = it
              hasUserEditedAdminCreds = true
            },
            adminPasswordInput = adminPasswordInput,
            onAdminPasswordChange = {
              adminPasswordInput = it
              hasUserEditedAdminCreds = true
            },
            isAdminPasswordVisible = isAdminPasswordVisible,
            onToggleAdminPasswordVisible = { isAdminPasswordVisible = !isAdminPasswordVisible },
            rememberAdminMe = rememberAdminMe,
            onToggleRememberAdmin = { checked ->
              rememberAdminMe = checked
              onToggleRememberAdmin(checked)
            },
            resetFeedbackMessage = resetFeedbackMessage,
            onResetFeedbackChange = { resetFeedbackMessage = it },
            onLocalError = { localError = it },
            onClearFeedback = onClearFeedback,
            onSendAdminPasswordReset = onSendAdminPasswordReset,
            onLoginAdminSmartWithEmailAndPassword = onLoginAdminSmartWithEmailAndPassword,
            adminGooglePayload = adminGooglePayload,
            onAdminGooglePayloadChange = { adminGooglePayload = it },
            showLegacyAdminCodeFallback = showLegacyAdminCodeFallback,
            onToggleLegacyAdminCodeFallback = {
              showLegacyAdminCodeFallback = !showLegacyAdminCodeFallback
            },
            adminPasscodeInput = adminPasscodeInput,
            onAdminPasscodeChange = { adminPasscodeInput = it },
            isAdminPasscodeVisible = isAdminPasscodeVisible,
            onToggleAdminPasscodeVisible = { isAdminPasscodeVisible = !isAdminPasscodeVisible },
            resolvedChurch = resolvedChurch,
            selectedChurchId = selectedChurchId,
            onLoginAdmin = onLoginAdmin,
            onLoginAdminSmartWithGoogle = onLoginAdminSmartWithGoogle,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AuthLegalFooter(
          legalLinks = legalLinks,
          languageCode = normalizedLang,
          onUnavailableMessage = { message ->
            scope.launch {
              snackbarHostState.currentSnackbarData?.dismiss()
              snackbarHostState.showSnackbar(message)
            }
          },
          modifier = Modifier.padding(horizontal = 8.dp),
        )
      }
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .imePadding()
          .padding(horizontal = 16.dp, vertical = 12.dp),
    ) { data ->
      Snackbar(
        snackbarData = data,
        containerColor = Color(0xFF0F172A),
        contentColor = PureWhite,
        shape = RoundedCornerShape(14.dp),
      )
    }
  }

  if (showChurchSelectorSheet) {
    AuthChurchSelectorBottomSheet(
      churches = churches,
      churchesUiState = churchesUiState,
      resolvedChurch = resolvedChurch,
      onRetryLoadChurches = onRetryLoadChurches,
      onSelectChurch = { id ->
        selectedChurchId = id
        showChurchSelectorSheet = false
      },
      onDismiss = { showChurchSelectorSheet = false },
    )
  }

  pendingGoogleLinkPayload?.let { googlePayload ->
    AuthLinkGoogleBottomSheet(
      googlePayload = googlePayload,
      linkMatrikilInput = linkMatrikilInput,
      onLinkMatrikilChange = { linkMatrikilInput = it },
      linkPinInput = linkPinInput,
      onLinkPinChange = { linkPinInput = it },
      onConfirmLink = {
        onLinkGoogleWithMatrikilPin(
          null,
          linkMatrikilInput,
          linkPinInput,
          googlePayload,
        ) {
          pendingGoogleLinkPayload = null
        }
      },
      onDismiss = { pendingGoogleLinkPayload = null },
    )
  }

  if (pendingAdminOwnedChurches.isNotEmpty()) {
    AuthMultiChurchSelectionSheet(
      title = copy.ownedChurchesSheetTitle,
      subtitle = copy.ownedChurchesSheetSubtitle,
      ownedChurches = pendingAdminOwnedChurches,
      onSelectChurch = onSelectOwnedAdminChurch,
      onDismiss = onCancelOwnedAdminChurchSelection,
    )
  }

  if (pendingMemberMatchedSessions.isNotEmpty()) {
    AuthMatchedMemberSelectionSheet(
      title = copy.ownedChurchesSheetTitle,
      subtitle = copy.memberMatchedChurchesSubtitle,
      matchedSessions = pendingMemberMatchedSessions,
      onSelectSession = onSelectMatchedMemberSession,
      onDismiss = onCancelMatchedMemberChurchSelection,
    )
  }
}
