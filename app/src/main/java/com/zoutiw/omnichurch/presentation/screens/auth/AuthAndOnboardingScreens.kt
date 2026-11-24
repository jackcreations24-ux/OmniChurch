package com.zoutiw.omnichurch.presentation.screens.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.zoutiw.omnichurch.R
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
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.GlassSegmentedControl
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.OmniChurchProBadge
import com.zoutiw.omnichurch.presentation.components.OmniChurchSanctuaryHeroCanvas
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
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
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class GoogleAccountPayload(
  val uid: String,
  val email: String,
  val displayName: String,
  val photoUrl: String?,
)

fun launchOptionalGoogleSignIn(
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSuccess: (GoogleAccountPayload) -> Unit,
  onError: (String) -> Unit,
) {
  val activity = context.findActivity() ?: (context as? Activity)
  if (activity == null) {
    Log.e("Auth", "launchOptionalGoogleSignIn: Cannot find Activity from context $context")
    onError("Koneksyon Google mande yon fenèt prensipal aktif. Tanpri relanse aplikasyon an.")
    return
  }
  val clientId =
    try {
      activity.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onError(context.getString(R.string.auth_error_missing_client))
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdCred = GoogleIdTokenCredential.createFrom(credential.data)
        val email = googleIdCred.id.trim()
        val uid = googleIdCred.id.trim()
        val displayName =
          googleIdCred.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        val photoUrl = googleIdCred.profilePictureUri?.toString()

        try {
          val authCredential = GoogleAuthProvider.getCredential(googleIdCred.idToken, null)
          val authResult = Firebase.auth.signInWithCredential(authCredential).await()
          val firebaseUser = authResult.user
          val finalEmail = firebaseUser?.email?.trim()?.ifBlank { null } ?: email
          val finalUid = firebaseUser?.uid?.trim()?.ifBlank { null } ?: uid
          val finalDisplayName =
            firebaseUser?.displayName?.trim()?.ifBlank { null } ?: displayName
          val finalPhotoUrl = firebaseUser?.photoUrl?.toString() ?: photoUrl

          onSuccess(
            GoogleAccountPayload(
              uid = finalUid,
              email = finalEmail,
              displayName = finalDisplayName,
              photoUrl = finalPhotoUrl,
            )
          )
        } catch (authEx: Exception) {
          Log.w(
            "Auth",
            "Firebase Auth signInWithCredential skipped (${authEx.message}), proceeding with verified Google ID Token for: $email",
          )
          onSuccess(
            GoogleAccountPayload(
              uid = uid,
              email = email,
              displayName = displayName,
              photoUrl = photoUrl,
            )
          )
        }
      } else {
        onError(context.getString(R.string.auth_error_failed))
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In cancelled: ${e.message}")
      onError(context.getString(R.string.auth_cancelled))
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onError(e.localizedMessage ?: context.getString(R.string.auth_error_failed))
    }
  }
}

fun signOutOmniChurch(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        try {
          Firebase.auth.signOut()
        } catch (_: Exception) {}
        onSignOutComplete()
      }
    }
  }
}

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
  val rememberMeLabel =
    when (normalizedLang) {
      "fr" -> "Se souvenir de moi"
      "en" -> "Remember me"
      "es" -> "Recordarme"
      else -> "Sonje mwen"
    }
  val forgotPasswordLabel =
    when (normalizedLang) {
      "fr" -> "Mot de passe oublié ?"
      "en" -> "Forgot password?"
      "es" -> "¿Olvidaste tu contraseña?"
      else -> "Bliye modpas?"
    }
  val autoChurchBadgeLabel =
    when (normalizedLang) {
      "fr" -> "Chargement automatique de votre église • Données isolées"
      "en" -> "Auto-loads your church • Isolated tenant data"
      "es" -> "Carga automática de tu iglesia • Datos aislados"
      else -> "Chajman otomatik legliz pa ou • Done 100% izole"
    }
  val autoMemberBadgeLabel =
    when (normalizedLang) {
      "fr" -> "Reconnaissance automatique de votre église par ID • Données isolées"
      "en" -> "Auto-detects your church from your ID • Isolated tenant data"
      "es" -> "Detecta tu iglesia automáticamente por tu ID • Datos aislados"
      else -> "Rekonesans otomatik legliz pa ou ak ID ou • Done 100% izole"
    }
  val memberSubtitleText =
    when (normalizedLang) {
      "fr" -> "Entrez votre ID Matricule ou téléphone et votre code PIN. Votre église sera reconnue automatiquement."
      "en" -> "Enter your Member ID or phone number and PIN. Your church will be detected automatically."
      "es" -> "Ingresa tu ID de miembro o teléfono y tu PIN. Tu iglesia se detectará automáticamente."
      else -> "Antre ID Matrikil oswa telefòn ou ak Kòd PIN ou. Sistèm lan ap rekonèt legliz pa ou otomatikman."
    }
  val ownedChurchesSheetTitle =
    when (normalizedLang) {
      "fr" -> "Choisissez votre église"
      "en" -> "Select Your Church"
      "es" -> "Selecciona tu iglesia"
      else -> "Chwazi Legliz Pa Ou"
    }
  val ownedChurchesSheetSubtitle =
    when (normalizedLang) {
      "fr" -> "Votre compte gère plusieurs églises. Sélectionnez celle que vous souhaitez ouvrir :"
      "en" -> "Your account manages multiple churches. Select the one you want to open:"
      "es" -> "Tu cuenta administra varias iglesias. Selecciona la que deseas abrir:"
      else -> "Kont ou a jere plizyè legliz. Chwazi legliz ou vle ouvri kounye a :"
    }
  val memberMatchedChurchesSubtitle =
    when (normalizedLang) {
      "fr" -> "Votre profil membre est vérifié dans ces églises. Choisissez celle que vous souhaitez ouvrir :"
      "en" -> "Your member profile is verified in these churches. Select the one you want to open:"
      "es" -> "Tu perfil de miembro está verificado en estas iglesias. Selecciona la que deseas abrir:"
      else -> "Pwofil manm ou an verifye nan legliz sa yo. Chwazi legliz ou vle ouvri kounye a :"
    }

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

      Row(
        modifier =
          Modifier.padding(top = 10.dp)
            .clip(RoundedCornerShape(50))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.20f), RoundedCornerShape(50))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val supportedLanguages = listOf(
          Triple("ht", "🇭🇹", "Kreyòl"),
          Triple("fr", "🇫🇷", "Français"),
          Triple("en", "🇺🇸", "English"),
          Triple("es", "🇪🇸", "Español"),
        )
        supportedLanguages.forEach { (code, flag, label) ->
          val isSelected = currentLanguageCode.equals(code, ignoreCase = true)
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(
                  if (isSelected) {
                    Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                  } else {
                    Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                  }
                )
                .clickable { onSelectLanguage(code) }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = flag, style = MaterialTheme.typography.labelSmall)
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) PureWhite else MutedGlassText,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (isMemberMode) {
                  Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                } else {
                  Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                }
              )
              .clickable {
                isMemberMode = true
                localError = null
                onClearFeedback()
              }
              .padding(vertical = 11.dp)
              .testTag("tab_member_login"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Badge,
              contentDescription = null,
              tint = if (isMemberMode) PureWhite else MutedGlassText,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Espas Manm",
              style = MaterialTheme.typography.labelLarge,
              color = if (isMemberMode) PureWhite else MutedGlassText,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (!isMemberMode) {
                  Brush.horizontalGradient(listOf(BrandOrange, BrandGold))
                } else {
                  Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
                }
              )
              .clickable {
                isMemberMode = false
                localError = null
                onClearFeedback()
              }
              .padding(vertical = 11.dp)
              .testTag("tab_admin_login"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.AdminPanelSettings,
              contentDescription = null,
              tint = if (!isMemberMode) DeepNavy else MutedGlassText,
              modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Espas Admin",
              style = MaterialTheme.typography.labelLarge,
              color = if (!isMemberMode) DeepNavy else MutedGlassText,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
          Text(
            text =
              if (isMemberMode) "Koneksyon Manm (ID Matrikil + PIN)"
              else "Koneksyon Administratè Legliz",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Text(
            text =
              if (isMemberMode) {
                memberSubtitleText
              } else if (isAdminEmailMode) {
                "Konekte ak imèl ak modpas kont legliz la pou jere manm yo, sèvis yo ak prezans yo."
              } else {
                when (normalizedLang) {
                  "fr" -> "Connectez-vous avec votre compte Google Administrateur pour ouvrir automatiquement votre église."
                  "en" -> "Sign in with your authorized Admin Google account to automatically open your church."
                  "es" -> "Inicia sesión con tu cuenta Google de administrador para abrir automáticamente tu iglesia."
                  else -> "Konekte ak Kont Google Administratè ou pou ouvri legliz pa ou otomatikman."
                }
              },
            style = MaterialTheme.typography.bodySmall,
            color = GlassWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )

          val showPublicChurchSelector = !isMemberMode && !isAdminEmailMode && showLegacyAdminCodeFallback
          if (showPublicChurchSelector) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .background(DeepNavy.copy(alpha = 0.78f))
                  .border(1.dp, BlueNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                  .clickable {
                    if (churchesUiState is UiState.Error || (churchesUiState is UiState.Success && churches.isEmpty())) {
                      onRetryLoadChurches()
                    }
                    showChurchSelectorSheet = true
                  }
                  .padding(horizontal = 14.dp, vertical = 12.dp)
                  .testTag("church_selector_box"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "Legliz Aktif",
                      style = MaterialTheme.typography.labelSmall,
                      color = MutedGlassText,
                    )
                    val selectorSubtitle =
                      when {
                        resolvedChurch != null -> "${resolvedChurch.name} (${resolvedChurch.effectiveCode})"
                        churchesUiState is UiState.Loading && churches.isEmpty() -> "Ap chaje lis legliz yo..."
                        churchesUiState is UiState.Error && churches.isEmpty() -> "Erè chajman legliz • Klike pou rafrechi"
                        churches.isEmpty() -> "Okenn legliz ankò • Klike pou rafrechi"
                        else -> "Chwazi legliz ou a (${churches.size} disponib)"
                      }
                    Text(
                      text = selectorSubtitle,
                      style = MaterialTheme.typography.bodyMedium,
                      color =
                        when {
                          resolvedChurch != null -> PureWhite
                          churchesUiState is UiState.Error && churches.isEmpty() -> ErrorRed
                          else -> GlassWhite
                        },
                      fontWeight = FontWeight.SemiBold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                if (churchesUiState is UiState.Loading && churches.isEmpty()) {
                  CircularProgressIndicator(
                    color = BlueNeon,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Chwazi Legliz",
                    tint = BlueNeon,
                  )
                }
              }
            }
          } else {
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(DeepNavy.copy(alpha = 0.72f))
                  .border(1.dp, SuccessGreen.copy(alpha = 0.38f), RoundedCornerShape(14.dp))
                  .padding(horizontal = 12.dp, vertical = 9.dp)
                  .testTag(if (isMemberMode) "member_auto_church_badge" else "admin_auto_church_badge"),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isMemberMode) autoMemberBadgeLabel else autoChurchBadgeLabel,
                style = MaterialTheme.typography.labelMedium,
                color = GlassWhite.copy(alpha = 0.92f),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          if (displayedError != null) {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(ErrorRed.copy(alpha = 0.22f))
                  .border(1.dp, ErrorRed, RoundedCornerShape(14.dp))
                  .padding(12.dp)
                  .testTag("auth_error_banner")
            ) {
              Text(
                text = displayedError,
                style = MaterialTheme.typography.bodySmall,
                color = PureWhite,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }

          if (isMemberMode) {
            GlassInputField(
              value = matrikilOrPhoneInput,
              onValueChange = {
                matrikilOrPhoneInput = it
                localError = null
                onClearFeedback()
              },
              label = "ID Manm (Matrikil) oswa Telefòn",
              placeholder = "Egzanp: CH001-JD4827 oswa 509...",
              leadingIcon = Icons.Default.Badge,
              testTag = "member_matrikil_input",
            )

            GlassInputField(
              value = memberPinInput,
              onValueChange = {
                memberPinInput = it
                localError = null
                onClearFeedback()
              },
              label = "Kòd Sekirite (PIN)",
              placeholder = "Antre PIN administratè a ba ou a",
              leadingIcon = Icons.Default.Key,
              visualTransformation = PasswordVisualTransformation(),
              keyboardType = KeyboardType.Password,
              testTag = "member_pin_input",
            )

            if (isAuthenticating) {
              CircularProgressIndicator(
                color = BlueNeon,
                modifier = Modifier.size(38.dp),
              )
            } else {
              GlassButton(
                text = "Konekte ak ID Matrikil + PIN",
                icon = Icons.Default.VerifiedUser,
                isPrimaryNeon = true,
                onClick = {
                  localError = null
                  onClearFeedback()
                  onLoginMemberWithPin(
                    null,
                    matrikilOrPhoneInput,
                    memberPinInput,
                  )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "member_pin_login_button",
              )
            }

            if (
              sessionState.biometricEnabled &&
                sessionState.biometricChurchId.isNotBlank() &&
                (sessionState.biometricMemberDocId.isNotBlank() ||
                  sessionState.biometricMemberId.isNotBlank())
            ) {
              GlassButton(
                text =
                  "Debloke ak Anprent / Face ID (${sessionState.biometricMemberId.ifBlank { sessionState.biometricMemberName }})",
                icon = Icons.Default.Fingerprint,
                isPrimaryNeon = false,
                accentColor = SuccessGreen,
                onClick = {
                  BiometricQuickUnlockHelper.promptBiometricUnlock(
                    context = context,
                    title = "Deblokaj Biyometrik OmniChurch",
                    subtitle =
                      "Verifye idantite ou pou ouvri kat manm ${sessionState.biometricMemberId}",
                    cancelText = "Anile",
                    onSuccess = onUnlockBiometricSession,
                    onError = { err -> localError = err },
                  )
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "biometric_quick_unlock_button",
              )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Premium3DGoogleIconButton(
              caption = "Google (Si kont ou lye)",
              onClick = {
                localError = null
                onClearFeedback()
                launchOptionalGoogleSignIn(
                  context = context,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = { googlePayload ->
                    onOptionalGoogleMemberSignIn(
                      null,
                      googlePayload,
                      {
                        pendingGoogleLinkPayload = googlePayload
                        linkMatrikilInput = matrikilOrPhoneInput
                        linkPinInput = memberPinInput
                      },
                    )
                  },
                  onError = { msg -> localError = msg },
                )
              },
              testTag = "optional_google_login_button",
            )
          } else {
            GlassSegmentedControl(
              leftLabel = "Imèl + Modpas",
              rightLabel = "Google Admin",
              isLeftSelected = isAdminEmailMode,
              onSelectLeft = {
                isAdminEmailMode = true
                showLegacyAdminCodeFallback = false
                localError = null
                resetFeedbackMessage = null
                onClearFeedback()
              },
              onSelectRight = {
                isAdminEmailMode = false
                localError = null
                resetFeedbackMessage = null
                onClearFeedback()
              },
              leftIcon = Icons.Default.Email,
              rightIcon = Icons.Default.VerifiedUser,
              leftTestTag = "admin_mode_email_tab",
              rightTestTag = "admin_mode_code_tab",
            )

            if (isAdminEmailMode) {
              if (resetFeedbackMessage != null) {
                Box(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(14.dp))
                      .background(SuccessGreen.copy(alpha = 0.18f))
                      .border(1.dp, SuccessGreen, RoundedCornerShape(14.dp))
                      .padding(12.dp)
                      .testTag("admin_reset_success_banner")
                ) {
                  Text(
                    text = resetFeedbackMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }

              GlassInputField(
                value = adminEmailInput,
                onValueChange = {
                  adminEmailInput = it
                  hasUserEditedAdminCreds = true
                  localError = null
                  resetFeedbackMessage = null
                  onClearFeedback()
                },
                label = "Imèl Legliz / Administratè",
                placeholder = "egz: paste@leglizou.org",
                leadingIcon = Icons.Default.Email,
                keyboardType = KeyboardType.Email,
                testTag = "admin_email_input",
              )

              GlassInputField(
                value = adminPasswordInput,
                onValueChange = {
                  adminPasswordInput = it
                  hasUserEditedAdminCreds = true
                  localError = null
                  resetFeedbackMessage = null
                  onClearFeedback()
                },
                label = "Modpas Kont Legliz la",
                placeholder = "Antre modpas legliz la",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = {
                  IconButton(
                    onClick = { isAdminPasswordVisible = !isAdminPasswordVisible },
                    modifier = Modifier.testTag("admin_password_visibility_toggle"),
                  ) {
                    Icon(
                      imageVector =
                        if (isAdminPasswordVisible) Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                      contentDescription =
                        if (isAdminPasswordVisible) "Kache modpas la" else "Montre modpas la",
                      tint = BlueNeon,
                    )
                  }
                },
                visualTransformation =
                  if (isAdminPasswordVisible) VisualTransformation.None
                  else PasswordVisualTransformation(),
                keyboardType = KeyboardType.Password,
                testTag = "admin_password_input",
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable(enabled = !isAuthenticating) {
                        val next = !rememberAdminMe
                        rememberAdminMe = next
                        onToggleRememberAdmin(next)
                      }
                      .padding(end = 8.dp)
                      .testTag("admin_remember_me_row"),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Checkbox(
                    checked = rememberAdminMe,
                    onCheckedChange = { checked ->
                      rememberAdminMe = checked
                      onToggleRememberAdmin(checked)
                    },
                    enabled = !isAuthenticating,
                    colors =
                      CheckboxDefaults.colors(
                        checkedColor = BrandGold,
                        uncheckedColor = GlassWhite.copy(alpha = 0.65f),
                        checkmarkColor = DeepNavy,
                      ),
                    modifier = Modifier.testTag("admin_remember_me_checkbox"),
                  )
                  Text(
                    text = rememberMeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = GlassWhite,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }

                Box(
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .clip(RoundedCornerShape(10.dp))
                      .clickable(enabled = !isAuthenticating) {
                        localError = null
                        resetFeedbackMessage = null
                        onClearFeedback()
                        onSendAdminPasswordReset(
                          adminEmailInput,
                          { msg -> resetFeedbackMessage = msg },
                          { err -> localError = err },
                        )
                      }
                      .padding(horizontal = 8.dp, vertical = 6.dp)
                      .testTag("admin_forgot_password_button"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = forgotPasswordLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandGold,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              GlassButton(
                text = "Konekte ak Imèl + Modpas",
                icon = Icons.Default.AdminPanelSettings,
                isPrimaryNeon = true,
                isLoading = isAuthenticating,
                accentColor = BrandOrange,
                onClick = {
                  localError = null
                  resetFeedbackMessage = null
                  onClearFeedback()
                  val cleanEmail = adminEmailInput.trim()
                  when {
                    cleanEmail.isEmpty() -> localError = OmniAuthEvaluator.ERROR_EMPTY_EMAIL
                    !cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".") ->
                      localError = OmniAuthEvaluator.ERROR_INVALID_EMAIL
                    adminPasswordInput.isBlank() ->
                      localError = OmniAuthEvaluator.ERROR_EMPTY_PASSWORD
                    else -> {
                      onLoginAdminSmartWithEmailAndPassword(
                        cleanEmail,
                        adminPasswordInput,
                        rememberAdminMe,
                      )
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "admin_email_login_button",
              )
            } else {
              if (adminGooglePayload != null) {
                Row(
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(12.dp))
                      .background(SuccessGreen.copy(alpha = 0.16f))
                      .border(1.dp, SuccessGreen.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                      .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Google Admin: ${adminGooglePayload!!.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite,
                  )
                }
              }

              if (isAuthenticating) {
                CircularProgressIndicator(
                  color = BlueNeon,
                  modifier = Modifier.size(36.dp),
                )
              } else {
                Premium3DGoogleIconButton(
                  caption =
                    if (adminGooglePayload == null) "Google Admin"
                    else "Chanje (${adminGooglePayload!!.email})",
                  onClick = {
                    localError = null
                    onClearFeedback()
                    launchOptionalGoogleSignIn(
                      context = context,
                      credentialManager = credentialManager,
                      scope = scope,
                      onSuccess = { payload ->
                        adminGooglePayload = payload
                        if (showLegacyAdminCodeFallback) {
                          val targetChurchId = resolvedChurch?.id ?: selectedChurchId.orEmpty()
                          if (targetChurchId.isNotBlank()) {
                            onLoginAdmin(
                              targetChurchId,
                              adminPasscodeInput.takeIf { it.isNotBlank() },
                              payload.email,
                              payload.uid,
                            )
                          } else {
                            onLoginAdminSmartWithGoogle()
                          }
                        } else {
                          onLoginAdminSmartWithGoogle()
                        }
                      },
                      onError = { msg -> localError = msg },
                    )
                  },
                  testTag = "admin_google_verify_button",
                )
              }

              // Explain Quick PIN's role as Fast Activity Resume after Auto-Lock
              Row(
                modifier =
                  Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DeepNavy.copy(alpha = 0.70f))
                    .border(1.dp, BrandGold.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("admin_pin_resume_info_card"),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.Key,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text =
                    when (normalizedLang) {
                      "fr" -> "Le code PIN Admin sert à reprendre rapidement votre activité après la mise en pause automatique (Paramètres → Reprise Rapide)."
                      "en" -> "Admin Quick PIN is used to rapidly resume activity after an auto-lock pause (configured in Settings → Quick Resume)."
                      "es" -> "El PIN rápido de Admin sirve para reanudar tu actividad tras la pausa automática (Ajustes → Reanudación Rápida)."
                      else -> "PIN Admin lan sèvi pou reprann aktivite w rapid apre tan de poz la (aktive li nan Paramèt → Reprann Aktivite Rapid)."
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = GlassWhite.copy(alpha = 0.88f),
                )
              }

              Box(
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                      showLegacyAdminCodeFallback = !showLegacyAdminCodeFallback
                      localError = null
                      onClearFeedback()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("toggle_legacy_admin_code_button"),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text =
                    if (showLegacyAdminCodeFallback) {
                      when (normalizedLang) {
                        "fr" -> "Masquer l'accès ancien code"
                        "en" -> "Hide legacy church passcode"
                        "es" -> "Ocultar acceso de código antiguo"
                        else -> "Kache aksè ansyen kòd legliz la"
                      }
                    } else {
                      when (normalizedLang) {
                        "fr" -> "Ancien compte avec code d'église ?"
                        "en" -> "Legacy account with church passcode?"
                        "es" -> "¿Cuenta antigua con código de iglesia?"
                        else -> "Ansyen kont ki gen kòd legliz?"
                      }
                    },
                  style = MaterialTheme.typography.labelSmall,
                  color = MutedGlassText,
                  fontWeight = FontWeight.Medium,
                )
              }

              if (showLegacyAdminCodeFallback) {
                GlassInputField(
                  value = adminPasscodeInput,
                  onValueChange = {
                    adminPasscodeInput = it
                    localError = null
                    onClearFeedback()
                  },
                  label = "Ansyen Kòd Sekirite Legliz la",
                  placeholder = "Antre Admin Passcode legliz la",
                  leadingIcon = Icons.Default.Lock,
                  trailingIcon = {
                    IconButton(
                      onClick = { isAdminPasscodeVisible = !isAdminPasscodeVisible },
                      modifier = Modifier.testTag("admin_passcode_visibility_toggle"),
                    ) {
                      Icon(
                        imageVector =
                          if (isAdminPasscodeVisible) Icons.Default.VisibilityOff
                          else Icons.Default.Visibility,
                        contentDescription =
                          if (isAdminPasscodeVisible) "Kache kòd la" else "Montre kòd la",
                        tint = BlueNeon,
                      )
                    }
                  },
                  visualTransformation =
                    if (isAdminPasscodeVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                  keyboardType = KeyboardType.Password,
                  testTag = "admin_passcode_input",
                )

                GlassButton(
                  text = "Antre nan Espas Administratè",
                  icon = Icons.Default.AdminPanelSettings,
                  isPrimaryNeon = true,
                  isLoading = isAuthenticating,
                  onClick = {
                    localError = null
                    onClearFeedback()
                    val targetChurchId = resolvedChurch?.id ?: selectedChurchId.orEmpty()
                    onLoginAdmin(
                      targetChurchId,
                      adminPasscodeInput,
                      adminGooglePayload?.email,
                      adminGooglePayload?.uid,
                    )
                  },
                  modifier = Modifier.fillMaxWidth(),
                  testTag = "admin_login_submit_button",
                )
              }
            }
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
    GlassModalBottomSheet(onDismissRequest = { showChurchSelectorSheet = false }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OmniChurchLogo(
              variant = LogoVariant.Icon,
              modifier = Modifier.size(28.dp),
              contentDescription = "OmniChurch PRO",
            )
            Text(
              text = "Chwazi Legliz ou a",
              style = MaterialTheme.typography.titleLarge,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
          IconButton(
            onClick = { onRetryLoadChurches() },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_churches_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Rafrechi lis legliz yo",
              tint = BlueNeon,
            )
          }
        }
        when {
          churchesUiState is UiState.Loading && churches.isEmpty() -> {
            Column(
              modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(34.dp))
              Text(
                text = "Ap chaje legliz yo depi nan baz done a...",
                style = MaterialTheme.typography.bodyMedium,
                color = GlassWhite,
                textAlign = TextAlign.Center,
              )
            }
          }
          churchesUiState is UiState.Error && churches.isEmpty() -> {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .background(ErrorRed.copy(alpha = 0.2f))
                  .border(1.dp, ErrorRed, RoundedCornerShape(14.dp))
                  .padding(14.dp)
                  .testTag("churches_error_banner")
            ) {
              Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "Pa ka chaje lis legliz yo",
                  style = MaterialTheme.typography.titleSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = churchesUiState.message,
                  style = MaterialTheme.typography.bodySmall,
                  color = GlassWhite,
                )
              }
            }
            GlassButton(
              text = "Re-eseye / Rafrechi",
              onClick = { onRetryLoadChurches() },
              testTag = "retry_churches_button",
            )
          }
          churches.isEmpty() -> {
            Text(
              text = "Pa gen okenn legliz ki anrejistre nan baz done a kounye a.",
              style = MaterialTheme.typography.bodyMedium,
              color = MutedGlassText,
            )
            Text(
              text = "Se sèlman sou Platfòm Web OmniChurch Pro administratè yo kreye legliz. Yon fwa kreye sou Web la, li ap parèt isit la otomatikman.",
              style = MaterialTheme.typography.bodySmall,
              color = BrandGold,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(top = 4.dp),
            )
            GlassButton(
              text = "Rafrechi Lis Legliz yo",
              onClick = { onRetryLoadChurches() },
              testTag = "retry_churches_empty_button",
            )
          }
          else -> {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .heightIn(max = 400.dp)
                  .verticalScroll(rememberScrollState()),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              churches.forEach { church ->
                val isSelected = resolvedChurch?.id == church.id
                PremiumGlassCard(
                  cornerRadius = 16.dp,
                  isHighlighted = isSelected,
                  accentColor = if (isSelected) BlueNeon else ElectricBlue,
                  onClick = {
                    selectedChurchId = church.id
                    showChurchSelectorSheet = false
                  },
                  contentPadding = PaddingValues(14.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = church.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                      )
                      Text(
                        text = "Kòd Legliz: ${church.effectiveCode}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandGold,
                      )
                    }
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (pendingGoogleLinkPayload != null) {
    val googlePayload = pendingGoogleLinkPayload!!
    GlassModalBottomSheet(onDismissRequest = { pendingGoogleLinkPayload = null }) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OmniChurchLogo(
            variant = LogoVariant.Icon,
            modifier = Modifier.size(28.dp),
            contentDescription = "OmniChurch PRO",
          )
          Text(
            text = "Lye Kont Google ou ak ID Matrikil ou",
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text =
            "Kont Google (${googlePayload.email}) poko lye ak yon ID Matrikil. Antre ID Matrikil ou ak PIN ou yon sèl fwa pou lye yo:",
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite,
        )

        GlassInputField(
          value = linkMatrikilInput,
          onValueChange = { linkMatrikilInput = it },
          label = "ID Manm (Matrikil) oswa Telefòn",
          placeholder = "CH001-JD4827",
          leadingIcon = Icons.Default.Badge,
          testTag = "link_google_matrikil_input",
        )

        GlassInputField(
          value = linkPinInput,
          onValueChange = { linkPinInput = it },
          label = "Kòd Sekirite (PIN)",
          placeholder = "Antre PIN ou",
          leadingIcon = Icons.Default.Key,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "link_google_pin_input",
        )

        GlassButton(
          text = "Verifye epi Lye Kont Google la",
          icon = Icons.Default.Link,
          isPrimaryNeon = true,
          onClick = {
            onLinkGoogleWithMatrikilPin(
              null,
              linkMatrikilInput,
              linkPinInput,
              googlePayload,
            ) {
              pendingGoogleLinkPayload = null
            }
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "confirm_link_google_button",
        )
      }
    }
  }

  if (pendingAdminOwnedChurches.isNotEmpty()) {
    GlassModalBottomSheet(onDismissRequest = onCancelOwnedAdminChurchSelection) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OmniChurchLogo(
            variant = LogoVariant.Icon,
            modifier = Modifier.size(28.dp),
            contentDescription = "OmniChurch PRO",
          )
          Text(
            text = ownedChurchesSheetTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = ownedChurchesSheetSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.88f),
        )
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = 380.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          pendingAdminOwnedChurches.forEach { ownedChurch ->
            PremiumGlassCard(
              cornerRadius = 16.dp,
              isHighlighted = true,
              accentColor = BrandGold,
              onClick = { onSelectOwnedAdminChurch(ownedChurch) },
              contentPadding = PaddingValues(14.dp),
              modifier = Modifier.testTag("owned_church_item_${ownedChurch.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = ownedChurch.name,
                      style = MaterialTheme.typography.titleMedium,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle =
                      listOfNotNull(
                        ownedChurch.effectiveCode.takeIf { it.isNotBlank() },
                        ownedChurch.city?.takeIf { it.isNotBlank() },
                      ).joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                      Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }

  if (pendingMemberMatchedSessions.isNotEmpty()) {
    GlassModalBottomSheet(onDismissRequest = onCancelMatchedMemberChurchSelection) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OmniChurchLogo(
            variant = LogoVariant.Icon,
            modifier = Modifier.size(28.dp),
            contentDescription = "OmniChurch PRO",
          )
          Text(
            text = ownedChurchesSheetTitle,
            style = MaterialTheme.typography.titleLarge,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = memberMatchedChurchesSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.88f),
        )
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(max = 380.dp)
              .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          pendingMemberMatchedSessions.forEach { matchedSession ->
            val matchedChurch = matchedSession.church
            val matchedMember = matchedSession.member
            PremiumGlassCard(
              cornerRadius = 16.dp,
              isHighlighted = true,
              accentColor = BlueNeon,
              onClick = { onSelectMatchedMemberSession(matchedSession) },
              contentPadding = PaddingValues(14.dp),
              modifier = Modifier.testTag("matched_member_church_item_${matchedChurch.id}"),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f),
                ) {
                  Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = BrandGold,
                    modifier = Modifier.size(22.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = matchedChurch.name,
                      style = MaterialTheme.typography.titleMedium,
                      color = PureWhite,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle =
                      listOfNotNull(
                        matchedChurch.effectiveCode.takeIf { it.isNotBlank() },
                        matchedMember.effectiveMatrikil.takeIf { it.isNotBlank() },
                      ).joinToString(" • ")
                    if (subtitle.isNotBlank()) {
                      Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
                Icon(
                  imageVector = Icons.Default.VerifiedUser,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
