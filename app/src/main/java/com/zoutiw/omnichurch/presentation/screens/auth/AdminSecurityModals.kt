package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassDialog
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
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
import com.google.firebase.auth.FirebaseUser

/**
 * Unified Admin Authentication & Unlock content used both inside the Auth Portal ("Espas Admin")
 * and inside the full-screen Anti-Bypass Lockout Gate when an Admin session is locked or missing
 * cloud ownership verification.
 */
@Composable
fun AdminAuthGateFormContent(
  church: Church?,
  firebaseUser: FirebaseUser?,
  isBusy: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  legacyVerifiedChurchId: String?,
  legacyVerifiedPin: String?,
  onClearMessages: () -> Unit,
  onSignInEmailPassword: (email: String, password: String) -> Unit,
  onSendPasswordReset: (email: String) -> Unit,
  onGoogleVerified: () -> Unit,
  onQuickPinSubmit: (pin: String) -> Unit,
  onBiometricSuccess: () -> Unit,
  onVerifyLegacyPin: (pin: String) -> Unit,
  onCompleteLegacyMigration: (email: String, newPass: String, confirmPass: String) -> Unit,
  onResetLegacyMigration: () -> Unit,
  onLocalError: (String) -> Unit,
  currentLanguageCode: String = "ht",
  modifier: Modifier = Modifier,
) {
  val lang = currentLanguageCode.lowercase()
  val rawContext = LocalContext.current
  val context = remember(rawContext) { rawContext.findActivity() ?: rawContext }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val canQuickPin =
    remember(church, firebaseUser) {
      church != null && SessionLockManager.canQuickPinUnlock(church, firebaseUser)
    }
  val isLegacyPinOnly = remember(church) { church?.isLegacyPinOnly == true }
  val isLegacyStep2 =
    isLegacyPinOnly &&
      church != null &&
      legacyVerifiedChurchId == church.id &&
      !legacyVerifiedPin.isNullOrBlank()

  var forceEmailPasswordMode by remember(church?.id, canQuickPin) { mutableStateOf(false) }
  var emailInput by remember(church?.id) {
    mutableStateOf(
      church?.adminGoogleEmail?.takeIf { it.isNotBlank() }
        ?: firebaseUser?.email?.takeIf { it.isNotBlank() }
        ?: ""
    )
  }
  var passwordInput by remember { mutableStateOf("") }
  var quickPinInput by remember { mutableStateOf("") }

  // Legacy migration inputs
  var legacyOldPinInput by remember { mutableStateOf("") }
  var legacyEmailInput by remember { mutableStateOf("") }
  var legacyPasswordInput by remember { mutableStateOf("") }
  var legacyConfirmPasswordInput by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!infoMessage.isNullOrBlank()) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SuccessGreen.copy(alpha = 0.20f))
            .border(1.dp, SuccessGreen, RoundedCornerShape(14.dp))
            .padding(12.dp)
      ) {
        Text(
          text = infoMessage,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    if (!errorMessage.isNullOrBlank()) {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ErrorRed.copy(alpha = 0.22f))
            .border(1.dp, ErrorRed, RoundedCornerShape(14.dp))
            .padding(12.dp)
            .testTag("admin_gate_error_banner")
      ) {
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    when {
      isLegacyPinOnly && !isLegacyStep2 -> {
        Text(
          text = "Migrasyon Sekirite Legliz Ansyen",
          style = MaterialTheme.typography.titleMedium,
          color = BrandGold,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Etap 1/2: Antre ansyen Kòd PIN Administratè a pou lye legliz la ak yon kont Imèl + Modpas ofisyèl.",
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )
        GlassInputField(
          value = legacyOldPinInput,
          onValueChange = {
            legacyOldPinInput = it
            onClearMessages()
          },
          label = "Ansyen Kòd PIN Administratè",
          placeholder = "Antre ansyen PIN lan",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "legacy_old_pin_input",
        )
        if (isBusy) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(36.dp))
        } else {
          GlassButton(
            text = "Verifye Ansyen PIN lan",
            icon = Icons.Default.Key,
            isPrimaryNeon = true,
            onClick = { onVerifyLegacyPin(legacyOldPinInput) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "legacy_verify_pin_button",
          )
        }
      }

      isLegacyPinOnly && isLegacyStep2 -> {
        Text(
          text = "Kreye Kont Pwopriyetè Ofisyèl",
          style = MaterialTheme.typography.titleMedium,
          color = SuccessGreen,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "Etap 2/2: PIN verifye! Antre Imèl Administratè a ak yon nouvo modpas (6+ karaktè).",
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )
        GlassInputField(
          value = legacyEmailInput,
          onValueChange = {
            legacyEmailInput = it
            onClearMessages()
          },
          label = "Imèl Administratè Ofisyèl",
          placeholder = "pastor@legliz.org",
          leadingIcon = Icons.Default.Email,
          keyboardType = KeyboardType.Email,
          testTag = "legacy_migrate_email_input",
        )
        GlassInputField(
          value = legacyPasswordInput,
          onValueChange = {
            legacyPasswordInput = it
            onClearMessages()
          },
          label = "Nouvo Modpas (min. 6 karaktè)",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "legacy_migrate_password_input",
        )
        GlassInputField(
          value = legacyConfirmPasswordInput,
          onValueChange = {
            legacyConfirmPasswordInput = it
            onClearMessages()
          },
          label = "Konfime Modpas la",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "legacy_migrate_confirm_input",
        )
        if (isBusy) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(36.dp))
        } else {
          GlassButton(
            text = "Sove & Aktive Kont Admin",
            icon = Icons.Default.AdminPanelSettings,
            isPrimaryNeon = true,
            onClick = {
              onCompleteLegacyMigration(
                legacyEmailInput,
                legacyPasswordInput,
                legacyConfirmPasswordInput,
              )
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "legacy_migrate_submit_button",
          )
          Text(
            text = "Retounen nan Etap 1",
            style = MaterialTheme.typography.labelMedium,
            color = MutedGlassText,
            modifier = Modifier.clickable { onResetLegacyMigration() }.padding(6.dp),
          )
        }
      }

      canQuickPin && !forceEmailPasswordMode -> {
        val ownerEmailLabel = firebaseUser?.email?.takeIf { it.isNotBlank() } ?: church?.adminGoogleEmail.orEmpty()
        if (ownerEmailLabel.isNotBlank()) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SuccessGreen.copy(alpha = 0.14f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = SuccessGreen,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text =
                when (lang) {
                  "fr" -> "Session active : $ownerEmailLabel"
                  "en" -> "Active session: $ownerEmailLabel"
                  "es" -> "Sesión activa: $ownerEmailLabel"
                  else -> "Sesyon aktif : $ownerEmailLabel"
                },
              style = MaterialTheme.typography.labelMedium,
              color = PureWhite,
            )
          }
        }

        GlassInputField(
          value = quickPinInput,
          onValueChange = {
            if (it.length <= 6 && it.all { ch -> ch.isDigit() }) {
              quickPinInput = it
            }
            onClearMessages()
          },
          label =
            when (lang) {
              "fr" -> "Code PIN de reprise rapide (4–6 chiffres)"
              "en" -> "Quick Resume PIN (4–6 digits)"
              "es" -> "PIN de reanudación rápida (4–6 dígitos)"
              else -> "PIN Reprann Aktivite Rapid (4–6 chif)"
            },
          placeholder = "••••",
          leadingIcon = Icons.Default.Pin,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.NumberPassword,
          testTag = "admin_quick_pin_input",
        )

        if (isBusy) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(36.dp))
        } else {
          GlassButton(
            text =
              when (lang) {
                "fr" -> "Reprendre l'activité avec le PIN"
                "en" -> "Resume Activity with PIN"
                "es" -> "Reanudar actividad con PIN"
                else -> "Reprann Aktivite ak PIN"
              },
            icon = Icons.Default.LockClock,
            isPrimaryNeon = true,
            onClick = { onQuickPinSubmit(quickPinInput) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "admin_quick_pin_submit_button",
          )

          if (BiometricQuickUnlockHelper.isBiometricAvailable(context)) {
            GlassButton(
              text =
                when (lang) {
                  "fr" -> "Reprendre par empreinte / Face ID"
                  "en" -> "Resume with Fingerprint / Face ID"
                  "es" -> "Reanudar con huella / Face ID"
                  else -> "Reprann ak Anprent / Face ID"
                },
              icon = Icons.Default.Fingerprint,
              isPrimaryNeon = false,
              accentColor = SuccessGreen,
              onClick = {
                BiometricQuickUnlockHelper.promptBiometricUnlock(
                  context = context,
                  title =
                    when (lang) {
                      "fr" -> "Reprise Administrateur OmniChurch"
                      "en" -> "OmniChurch Admin Resume"
                      "es" -> "Reanudación Admin OmniChurch"
                      else -> "Reprann Sesyon Admin OmniChurch"
                    },
                  subtitle = church?.name.orEmpty(),
                  cancelText =
                    when (lang) {
                      "fr" -> "Annuler"
                      "en" -> "Cancel"
                      "es" -> "Cancelar"
                      else -> "Anile"
                    },
                  onSuccess = onBiometricSuccess,
                  onError = { err -> onLocalError(err) },
                )
              },
              modifier = Modifier.fillMaxWidth(),
              testTag = "admin_biometric_unlock_button",
            )
          }
        }

        Text(
          text =
            when (lang) {
              "fr" -> "Utiliser l'e-mail et le mot de passe"
              "en" -> "Use Email & Password instead"
              "es" -> "Usar correo y contraseña"
              else -> "Itilize Imèl & Modpas pito"
            },
          style = MaterialTheme.typography.labelMedium,
          color = BlueNeon,
          fontWeight = FontWeight.SemiBold,
          modifier =
            Modifier.minimumInteractiveComponentSize()
              .clickable {
                forceEmailPasswordMode = true
                onClearMessages()
              }
              .padding(horizontal = 8.dp, vertical = 10.dp)
              .testTag("switch_to_email_password_button"),
        )
      }

      else -> {
        // Real Firebase Email + Password + Forgot Password + Google Credential Manager
        GlassInputField(
          value = emailInput,
          onValueChange = {
            emailInput = it
            onClearMessages()
          },
          label = "Imèl Administratè Legliz la",
          placeholder = "admin@legliz.org",
          leadingIcon = Icons.Default.Email,
          keyboardType = KeyboardType.Email,
          testTag = "admin_email_input",
        )

        GlassInputField(
          value = passwordInput,
          onValueChange = {
            passwordInput = it
            onClearMessages()
          },
          label = "Modpas Administratè",
          placeholder = "Antre modpas kont Firebase ou a",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "admin_password_input",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          Text(
            text = "Bliye Modpas?",
            style = MaterialTheme.typography.labelMedium,
            color = BrandGold,
            fontWeight = FontWeight.SemiBold,
            modifier =
              Modifier.clickable {
                onClearMessages()
                onSendPasswordReset(emailInput)
              }
                .padding(vertical = 2.dp, horizontal = 4.dp)
                .testTag("admin_forgot_password_button"),
          )
        }

        if (isBusy) {
          CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(36.dp))
        } else {
          GlassButton(
            text = "Konekte kòm Administratè",
            icon = Icons.Default.AdminPanelSettings,
            isPrimaryNeon = true,
            onClick = {
              onClearMessages()
              onSignInEmailPassword(emailInput, passwordInput)
            },
            modifier = Modifier.fillMaxWidth(),
            testTag = "admin_email_login_submit_button",
          )
        }

        if (canQuickPin) {
          Text(
            text = "Itilize PIN Rapid pito",
            style = MaterialTheme.typography.labelMedium,
            color = SuccessGreen,
            fontWeight = FontWeight.SemiBold,
            modifier =
              Modifier.clickable {
                forceEmailPasswordMode = false
                onClearMessages()
              }
                .padding(4.dp),
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Premium3DGoogleIconButton(
          caption = "Konekte ak Google Admin",
          onClick = {
            onClearMessages()
            launchOptionalGoogleSignIn(
              context = context,
              credentialManager = credentialManager,
              scope = scope,
              onSuccess = { onGoogleVerified() },
              onError = { msg -> onLocalError(msg) },
            )
          },
          testTag = "admin_google_verify_button",
        )
      }
    }
  }
}

/**
 * Full-screen Anti-Bypass Gate shown over the Admin workspace whenever the Admin session is locked
 * or the underlying Firebase Auth user is missing / mismatched.
 */
@Composable
fun BlockingAdminAuthGateOverlay(
  church: Church,
  firebaseUser: FirebaseUser?,
  isBusy: Boolean,
  errorMessage: String?,
  infoMessage: String?,
  legacyVerifiedChurchId: String?,
  legacyVerifiedPin: String?,
  onClearMessages: () -> Unit,
  onSignInEmailPassword: (email: String, password: String) -> Unit,
  onSendPasswordReset: (email: String) -> Unit,
  onGoogleVerified: () -> Unit,
  onQuickPinSubmit: (pin: String) -> Unit,
  onBiometricSuccess: () -> Unit,
  onVerifyLegacyPin: (pin: String) -> Unit,
  onCompleteLegacyMigration: (email: String, newPass: String, confirmPass: String) -> Unit,
  onResetLegacyMigration: () -> Unit,
  onLocalError: (String) -> Unit,
  onExitToAuthPortal: () -> Unit,
  currentLanguageCode: String = "ht",
) {
  val lang = currentLanguageCode.lowercase()
  val isQuickResumeReady =
    remember(church, firebaseUser) {
      SessionLockManager.canQuickPinUnlock(church, firebaseUser)
    }

  BackHandler(enabled = true) {
    // Prevent hardware back bypass while session is paused/locked.
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(OmniBrushes.DeepBackgroundGradient)
        .statusBarsPadding()
        .navigationBarsPadding()
        .verticalScroll(rememberScrollState())
        .padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    PremiumGlassCard(
      cornerRadius = 24.dp,
      isHighlighted = true,
      accentColor = BrandGold,
      contentPadding = PaddingValues(20.dp),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OmniChurchLogo(
          variant = LogoVariant.Icon,
          modifier = Modifier.size(46.dp),
          contentDescription = "OmniChurch PRO",
        )
        Text(
          text =
            if (isQuickResumeReady) {
              when (lang) {
                "fr" -> "Reprise d'activité rapide"
                "en" -> "Quick Activity Resume"
                "es" -> "Reanudación rápida"
                else -> "Reprann Aktivite Rapid"
              }
            } else {
              when (lang) {
                "fr" -> "Espace Administrateur Verrouillé"
                "en" -> "Admin Workspace Locked"
                "es" -> "Espacio de Administrador Bloqueado"
                else -> "Espas Administratè an Poz"
              }
            },
          style = MaterialTheme.typography.titleLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = "${church.name} (${church.effectiveCode})",
          style = MaterialTheme.typography.labelLarge,
          color = BlueNeon,
          fontWeight = FontWeight.SemiBold,
          textAlign = TextAlign.Center,
        )
        Text(
          text =
            if (isQuickResumeReady) {
              when (lang) {
                "fr" -> "Votre session est en pause pour votre sécurité. Entrez votre code PIN pour reprendre immédiatement."
                "en" -> "Your session paused for security. Enter your Quick PIN to resume right where you left off."
                "es" -> "Tu sesión se pausó por seguridad. Ingresa tu PIN rápido para continuar al instante."
                else -> "Sesyon ou a pran yon poz pou sekirite. Antre PIN ou pou reprann aktivite w imedyatman."
              }
            } else {
              when (lang) {
                "fr" -> "Vérifiez votre identité administrateur pour déverrouiller la gestion de votre église."
                "en" -> "Verify your administrator identity to unlock church management."
                "es" -> "Verifica tu identidad de administrador para desbloquear la gestión de tu iglesia."
                else -> "Verifye idantite administratè w pou debloke jesyon legliz ou a."
              }
            },
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          textAlign = TextAlign.Center,
        )

        AdminAuthGateFormContent(
          church = church,
          firebaseUser = firebaseUser,
          isBusy = isBusy,
          errorMessage = errorMessage,
          infoMessage = infoMessage,
          legacyVerifiedChurchId = legacyVerifiedChurchId,
          legacyVerifiedPin = legacyVerifiedPin,
          onClearMessages = onClearMessages,
          onSignInEmailPassword = onSignInEmailPassword,
          onSendPasswordReset = onSendPasswordReset,
          onGoogleVerified = onGoogleVerified,
          onQuickPinSubmit = onQuickPinSubmit,
          onBiometricSuccess = onBiometricSuccess,
          onVerifyLegacyPin = onVerifyLegacyPin,
          onCompleteLegacyMigration = onCompleteLegacyMigration,
          onResetLegacyMigration = onResetLegacyMigration,
          onLocalError = onLocalError,
          currentLanguageCode = currentLanguageCode,
        )

        Spacer(modifier = Modifier.height(4.dp))

        GlassButton(
          text =
            when (lang) {
              "fr" -> "Se déconnecter / Changer de compte"
              "en" -> "Sign Out / Switch Account"
              "es" -> "Cerrar sesión / Cambiar cuenta"
              else -> "Dekonekte / Chanje Kont"
            },
          icon = Icons.AutoMirrored.Filled.Logout,
          isPrimaryNeon = false,
          onClick = onExitToAuthPortal,
          modifier = Modifier.fillMaxWidth(),
          testTag = "admin_gate_sign_out_button",
        )
      }
    }
  }
}

/**
 * Create New Church dialog matching Section 6.A:
 * Tab 1 (Default): Email + Password (with multi-church owner reuse).
 * Tab 2: Google Sign-In (Credential Manager).
 */
@Composable
fun CreateChurchCloudDialog(
  isBusy: Boolean,
  errorMessage: String?,
  onDismiss: () -> Unit,
  onClearError: () -> Unit,
  onCreateWithEmailPassword: (
    churchName: String,
    churchCode: String,
    churchAddress: String,
    email: String,
    password: String,
    confirmPassword: String,
  ) -> Unit,
  onCreateWithGoogle: (
    churchName: String,
    churchCode: String,
    churchAddress: String,
  ) -> Unit,
  onLocalError: (String) -> Unit,
) {
  val rawContext = LocalContext.current
  val context = remember(rawContext) { rawContext.findActivity() ?: rawContext }
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var useEmailTab by remember { mutableStateOf(true) }
  var churchName by remember { mutableStateOf("") }
  var churchCode by remember { mutableStateOf("") }
  var churchAddress by remember { mutableStateOf("") }
  var ownerEmail by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  GlassDialog(
    onDismissRequest = onDismiss,
    title = "Kreye yon Nouvo Legliz",
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DeepNavy.copy(alpha = 0.85f))
            .border(1.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (useEmailTab) Brush.horizontalGradient(listOf(RoyalBlue, ElectricBlue))
                else Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
              )
              .clickable {
                useEmailTab = true
                onClearError()
              }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Imèl & Modpas",
            style = MaterialTheme.typography.labelMedium,
            color = if (useEmailTab) PureWhite else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
        Box(
          modifier =
            Modifier.weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (!useEmailTab) Brush.horizontalGradient(listOf(BrandOrange, BrandGold))
                else Brush.horizontalGradient(listOf(DeepNavy, DeepNavy))
              )
              .clickable {
                useEmailTab = false
                onClearError()
              }
              .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "Kont Google",
            style = MaterialTheme.typography.labelMedium,
            color = if (!useEmailTab) DeepNavy else MutedGlassText,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(ErrorRed.copy(alpha = 0.22f))
              .border(1.dp, ErrorRed, RoundedCornerShape(12.dp))
              .padding(10.dp)
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = PureWhite,
          )
        }
      }

      GlassInputField(
        value = churchName,
        onValueChange = {
          churchName = it
          onClearError()
        },
        label = "Non Ofisyèl Legliz la",
        placeholder = "Legliz Tabènak Delmas",
        leadingIcon = Icons.Default.AccountBalance,
        testTag = "create_church_name_input",
      )
      GlassInputField(
        value = churchCode,
        onValueChange = {
          churchCode = it.uppercase()
          onClearError()
        },
        label = "Kòd Legliz (Opsyonèl)",
        placeholder = "Egz: CH001",
        leadingIcon = Icons.Default.AddBusiness,
        testTag = "create_church_code_input",
      )
      GlassInputField(
        value = churchAddress,
        onValueChange = { churchAddress = it },
        label = "Adrès / Vil (Opsyonèl)",
        placeholder = "Port-au-Prince",
      )

      if (useEmailTab) {
        GlassInputField(
          value = ownerEmail,
          onValueChange = {
            ownerEmail = it
            onClearError()
          },
          label = "Imèl Pwopriyetè a",
          placeholder = "pastor@legliz.org",
          leadingIcon = Icons.Default.Email,
          keyboardType = KeyboardType.Email,
          testTag = "create_church_email_input",
        )
        GlassInputField(
          value = password,
          onValueChange = {
            password = it
            onClearError()
          },
          label = "Modpas (min. 6 karaktè)",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "create_church_password_input",
        )
        GlassInputField(
          value = confirmPassword,
          onValueChange = {
            confirmPassword = it
            onClearError()
          },
          label = "Konfime Modpas",
          leadingIcon = Icons.Default.Lock,
          visualTransformation = PasswordVisualTransformation(),
          keyboardType = KeyboardType.Password,
          testTag = "create_church_confirm_password_input",
        )

        if (isBusy) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(34.dp))
          }
        } else {
          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
              text = "Anile",
              isPrimaryNeon = false,
              onClick = onDismiss,
              modifier = Modifier.weight(1f),
            )
            GlassButton(
              text = "Kreye Legliz",
              isPrimaryNeon = true,
              onClick = {
                onCreateWithEmailPassword(
                  churchName,
                  churchCode,
                  churchAddress,
                  ownerEmail,
                  password,
                  confirmPassword,
                )
              },
              modifier = Modifier.weight(1f),
              testTag = "create_church_email_submit_btn",
            )
          }
        }
      } else {
        Text(
          text = "Konekte ak kont Google ou pou asosye legliz sa a dirèkteman ak UID Google ou.",
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
          textAlign = TextAlign.Center,
        )
        if (isBusy) {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BlueNeon, modifier = Modifier.size(34.dp))
          }
        } else {
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Premium3DGoogleIconButton(
              caption = "Kreye Legliz ak Google",
              onClick = {
                if (churchName.trim().length < 2) {
                  onLocalError("Tanpri antre non ofisyèl legliz la anvan.")
                  return@Premium3DGoogleIconButton
                }
                launchOptionalGoogleSignIn(
                  context = context,
                  credentialManager = credentialManager,
                  scope = scope,
                  onSuccess = {
                    onCreateWithGoogle(churchName, churchCode, churchAddress)
                  },
                  onError = { err -> onLocalError(err) },
                )
              },
              testTag = "create_church_google_submit_btn",
            )
            GlassButton(
              text = "Anile",
              isPrimaryNeon = false,
              onClick = onDismiss,
              modifier = Modifier.fillMaxWidth(),
            )
          }
        }
      }
    }
  }
}

/**
 * Admin Security Settings card displayed in ProfileAndSettingsScreen for Church Owners/Admins:
 * Quick PIN Fast Activity Resume toggle + 4-6 digit PIN setup/update + Auto-Lock pause timeout + Manual Lock.
 */
@Composable
fun ChurchAdminSecuritySettingsCard(
  church: Church,
  isSaving: Boolean,
  currentLanguageCode: String = "ht",
  onSaveSettings: (quickPinEnabled: Boolean, newPinOrNull: String?, autoLockMinutes: Int) -> Unit,
  onLockAdminNow: () -> Unit,
) {
  val lang = currentLanguageCode.lowercase()
  var showPinSetupDialog by remember { mutableStateOf(false) }
  var pendingEnableAfterPin by remember { mutableStateOf(false) }

  val quickPinOn = church.effectiveQuickPinEnabled
  val currentAutoLock = church.effectiveAutoLockMinutes
  val autoLockOptions = listOf(0 to "Off", 1 to "1m", 5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m")

  PremiumGlassCard(
    cornerRadius = 22.dp,
    accentColor = ElectricBlue,
    contentPadding = PaddingValues(16.dp),
    modifier = Modifier.testTag("church_admin_security_card"),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = null,
        tint = BrandGold,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          when (lang) {
            "fr" -> "Reprise Rapide (PIN) & Pause Auto"
            "en" -> "Quick Resume (PIN) & Auto-Lock"
            "es" -> "Reanudación Rápida (PIN) y Pausa"
            else -> "Reprann Aktivite Rapid (PIN) & Poz"
          },
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick PIN Resume Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
        Text(
          text =
            when (lang) {
              "fr" -> "Reprise rapide avec PIN (4–6 chiffres)"
              "en" -> "Quick Resume with PIN (4–6 digits)"
              "es" -> "Reanudación rápida con PIN (4–6 dígitos)"
              else -> "Reprann Aktivite Rapid ak PIN (4–6 chif)"
            },
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            if (quickPinOn) {
              when (lang) {
                "fr" -> "Actif • Reprenez votre session admin par PIN après une pause"
                "en" -> "Enabled • Resume your admin session with PIN after auto-lock"
                "es" -> "Activo • Reanuda tu sesión admin con PIN tras la pausa"
                else -> "Aktive • Reprann aktivite admin lan rapid ak PIN apre tan de poz la"
              }
            } else {
              when (lang) {
                "fr" -> "Désactivé • Demande e-mail + mot de passe après verrouillage"
                "en" -> "Disabled • Requires Email + Password after lock"
                "es" -> "Desactivado • Solicita correo + contraseña al bloquear"
                else -> "Dezaktive • Mande Imèl + Modpas apre verouyaj"
              }
            },
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
        )
      }
      Switch(
        checked = quickPinOn,
        enabled = !isSaving,
        onCheckedChange = { wantEnable ->
          val targetAutoLock = if (wantEnable && currentAutoLock <= 0) 5 else currentAutoLock
          if (wantEnable && church.adminPasscode.isBlank()) {
            pendingEnableAfterPin = true
            showPinSetupDialog = true
          } else {
            onSaveSettings(wantEnable, null, targetAutoLock)
          }
        },
        colors =
          SwitchDefaults.colors(
            checkedThumbColor = PureWhite,
            checkedTrackColor = SuccessGreen,
          ),
        modifier = Modifier.testTag("admin_quick_pin_switch"),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    GlassButton(
      text =
        if (church.adminPasscode.isBlank()) {
          when (lang) {
            "fr" -> "Créer un code PIN de reprise"
            "en" -> "Create Quick Resume PIN"
            "es" -> "Crear PIN de reanudación"
            else -> "Kreye yon Kòd PIN Rapid"
          }
        } else {
          when (lang) {
            "fr" -> "Modifier le code PIN de reprise"
            "en" -> "Change Quick Resume PIN"
            "es" -> "Cambiar PIN de reanudación"
            else -> "Chanje Kòd PIN Rapid la"
          }
        },
      icon = Icons.Default.Pin,
      isPrimaryNeon = false,
      enabled = !isSaving,
      onClick = {
        pendingEnableAfterPin = quickPinOn
        showPinSetupDialog = true
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "admin_change_quick_pin_btn",
    )

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text =
        when (lang) {
          "fr" -> "Temps de pause automatique (Inactivité)"
          "en" -> "Auto-Lock Pause Timeout (Inactivity)"
          "es" -> "Tiempo de pausa automática (Inactividad)"
          else -> "Tan de Poz Otomatik (Inaktivite)"
        },
      style = MaterialTheme.typography.titleSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text =
        if (currentAutoLock > 0) {
          when (lang) {
            "fr" -> "Verrouillage automatique actif après $currentAutoLock min d'inactivité"
            "en" -> "Auto-lock active after $currentAutoLock min of inactivity"
            "es" -> "Bloqueo automático activo tras $currentAutoLock min de inactividad"
            else -> "Poz otomatik aktif apre $currentAutoLock minit inaktivite"
          }
        } else {
          when (lang) {
            "fr" -> "Désactivé (Off) • Le verrouillage se fait uniquement manuellement"
            "en" -> "Disabled (Off) • Session only locks when you lock manually"
            "es" -> "Desactivado (Off) • Solo se bloquea de forma manual"
            else -> "Dezaktive (Off) • Verouyaj la ap fèt sèlman lè w klike manyèlman"
          }
        },
      style = MaterialTheme.typography.bodySmall,
      color = if (currentAutoLock > 0) SuccessGreen else MutedGlassText,
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      autoLockOptions.forEach { (minutes, label) ->
        val selected = currentAutoLock == minutes
        Box(
          modifier =
            Modifier.weight(1f)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(12.dp))
              .background(if (selected) ElectricBlue else Color(0x660B1D3D))
              .border(
                1.dp,
                if (selected) PureWhite else GlassWhite.copy(alpha = 0.25f),
                RoundedCornerShape(12.dp),
              )
              .clickable(enabled = !isSaving) {
                onSaveSettings(quickPinOn, null, minutes)
              }
              .padding(vertical = 10.dp)
              .testTag("autolock_option_$minutes"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    GlassButton(
      text =
        when (lang) {
          "fr" -> "Mettre en pause / Verrouiller maintenant"
          "en" -> "Pause / Lock Admin Now"
          "es" -> "Pausar / Bloquear ahora"
          else -> "Mete an Poz / Verouye Kounye a"
        },
      icon = Icons.Default.Lock,
      isPrimaryNeon = false,
      accentColor = BrandOrange,
      onClick = onLockAdminNow,
      modifier = Modifier.fillMaxWidth(),
      testTag = "admin_lock_now_btn",
    )
  }

  if (showPinSetupDialog) {
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var dialogErr by remember { mutableStateOf<String?>(null) }

    GlassDialog(
      onDismissRequest = { showPinSetupDialog = false },
      title =
        when (lang) {
          "fr" -> "Configurer le PIN de reprise rapide"
          "en" -> "Set Up Quick Resume PIN"
          "es" -> "Configurar PIN de reanudación"
          else -> "Konfigire PIN Reprann Aktivite"
        },
    ) {
      Text(
        text =
          when (lang) {
            "fr" -> "Choisissez 4 à 6 chiffres pour reprendre rapidement votre espace admin après une pause automatique."
            "en" -> "Choose 4 to 6 digits to rapidly resume your admin workspace after an auto-lock pause."
            "es" -> "Elige de 4 a 6 dígitos para reanudar rápidamente tu espacio admin tras una pausa."
            else -> "Chwazi 4 a 6 chif pou reprann espas admin lan rapid apre tan de poz otomatik la."
          },
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(10.dp))
      if (dialogErr != null) {
        Text(
          text = dialogErr!!,
          style = MaterialTheme.typography.bodySmall,
          color = ErrorRed,
          fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(6.dp))
      }
      GlassInputField(
        value = pinInput,
        onValueChange = {
          if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it
          dialogErr = null
        },
        label =
          when (lang) {
            "fr" -> "Nouveau PIN (4–6 chiffres)"
            "en" -> "New PIN (4–6 digits)"
            "es" -> "Nuevo PIN (4–6 dígitos)"
            else -> "Nouvo PIN (4–6 chif)"
          },
        leadingIcon = Icons.Default.Pin,
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.NumberPassword,
        testTag = "setup_quick_pin_input",
      )
      Spacer(modifier = Modifier.height(8.dp))
      GlassInputField(
        value = pinConfirmInput,
        onValueChange = {
          if (it.length <= 6 && it.all { c -> c.isDigit() }) pinConfirmInput = it
          dialogErr = null
        },
        label =
          when (lang) {
            "fr" -> "Confirmer le PIN"
            "en" -> "Confirm PIN"
            "es" -> "Confirmar PIN"
            else -> "Konfime PIN nan"
          },
        leadingIcon = Icons.Default.Pin,
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.NumberPassword,
        testTag = "setup_quick_pin_confirm_input",
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Annuler"
              "en" -> "Cancel"
              "es" -> "Cancelar"
              else -> "Anile"
            },
          isPrimaryNeon = false,
          onClick = { showPinSetupDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Enregistrer"
              "en" -> "Save"
              "es" -> "Guardar"
              else -> "Anrejistre"
            },
          isPrimaryNeon = true,
          onClick = {
            if (!pinInput.matches(Regex("^\\d{4,6}$"))) {
              dialogErr =
                when (lang) {
                  "fr" -> "Le PIN doit contenir entre 4 et 6 chiffres."
                  "en" -> "PIN must be 4 to 6 digits."
                  "es" -> "El PIN debe tener entre 4 y 6 dígitos."
                  else -> "PIN lan dwe gen 4 a 6 chif."
                }
              return@GlassButton
            }
            if (pinInput != pinConfirmInput) {
              dialogErr =
                when (lang) {
                  "fr" -> "Les deux codes PIN ne correspondent pas."
                  "en" -> "The two PINs do not match."
                  "es" -> "Los dos PIN no coinciden."
                  else -> "De PIN yo pa menm."
                }
              return@GlassButton
            }
            showPinSetupDialog = false
            val nextEnabled = if (pendingEnableAfterPin) true else quickPinOn
            val nextAutoLock = if (nextEnabled && currentAutoLock <= 0) 5 else currentAutoLock
            onSaveSettings(nextEnabled, pinInput, nextAutoLock)
          },
          modifier = Modifier.weight(1f),
          testTag = "setup_quick_pin_save_btn",
        )
      }
    }
  }
}

/**
 * 30-second countdown warning dialog before automatic inactivity lockout.
 */
@Composable
fun AutoLockWarningDialog(
  secondsRemaining: Int,
  currentLanguageCode: String = "ht",
  onStayActive: () -> Unit,
  onLockNow: () -> Unit,
) {
  val lang = currentLanguageCode.lowercase()
  GlassDialog(
    onDismissRequest = onStayActive,
    title =
      when (lang) {
        "fr" -> "Mise en pause automatique"
        "en" -> "Automatic Pause"
        "es" -> "Pausa automática"
        else -> "Poz Otomatik"
      },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Default.LockClock,
        contentDescription = null,
        tint = BrandGold,
        modifier = Modifier.size(40.dp),
      )
      Text(
        text =
          when (lang) {
            "fr" -> "Votre session Admin va se verrouiller dans ${secondsRemaining}s pour cause d'inactivité."
            "en" -> "Your Admin session will pause in ${secondsRemaining}s due to inactivity."
            "es" -> "Tu sesión Admin se bloqueará en ${secondsRemaining}s por inactividad."
            else -> "Sesyon Admin ou a pral mete an poz nan ${secondsRemaining}s poutèt inaktivite."
          },
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        textAlign = TextAlign.Center,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Verrouiller"
              "en" -> "Lock Now"
              "es" -> "Bloquear"
              else -> "Verouye"
            },
          isPrimaryNeon = false,
          accentColor = BrandOrange,
          onClick = onLockNow,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_lock_now",
        )
        GlassButton(
          text =
            when (lang) {
              "fr" -> "Continuer"
              "en" -> "Stay Active"
              "es" -> "Continuar"
              else -> "Kontinye"
            },
          isPrimaryNeon = true,
          onClick = onStayActive,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_continue",
        )
      }
    }
  }
}
