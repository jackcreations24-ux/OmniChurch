package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseUser
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.core.utilities.SessionLockManager
import com.zoutiw.omnichurch.core.utilities.findActivity
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

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
        AdminLegacyPinMigrationStep1(
          legacyOldPinInput = legacyOldPinInput,
          onLegacyOldPinChange = { legacyOldPinInput = it },
          isBusy = isBusy,
          onClearMessages = onClearMessages,
          onVerifyLegacyPin = onVerifyLegacyPin,
        )
      }

      isLegacyPinOnly && isLegacyStep2 -> {
        AdminLegacyPinMigrationStep2(
          legacyEmailInput = legacyEmailInput,
          onLegacyEmailChange = { legacyEmailInput = it },
          legacyPasswordInput = legacyPasswordInput,
          onLegacyPasswordChange = { legacyPasswordInput = it },
          legacyConfirmPasswordInput = legacyConfirmPasswordInput,
          onLegacyConfirmPasswordChange = { legacyConfirmPasswordInput = it },
          isBusy = isBusy,
          onClearMessages = onClearMessages,
          onCompleteLegacyMigration = onCompleteLegacyMigration,
          onResetLegacyMigration = onResetLegacyMigration,
        )
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
