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
  modifier: Modifier = Modifier,
) {
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
              text = "Pwopriyetè konekte: $ownerEmailLabel",
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
          label = "PIN Rapid Administratè (4–6 chif)",
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
            text = "Debloke ak PIN Rapid",
            icon = Icons.Default.LockClock,
            isPrimaryNeon = true,
            onClick = { onQuickPinSubmit(quickPinInput) },
            modifier = Modifier.fillMaxWidth(),
            testTag = "admin_quick_pin_submit_button",
          )

          if (BiometricQuickUnlockHelper.isBiometricAvailable(context)) {
            GlassButton(
              text = "Debloke ak Anprent / Face ID",
              icon = Icons.Default.Fingerprint,
              isPrimaryNeon = false,
              accentColor = SuccessGreen,
              onClick = {
                BiometricQuickUnlockHelper.promptBiometricUnlock(
                  context = context,
                  title = "Deblokaj Administratè OmniChurch",
                  subtitle = "Konfime idantite w pou louvri ${church?.name.orEmpty()}",
                  cancelText = "Anile",
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
          text = "Itilize Imèl & Modpas pito",
          style = MaterialTheme.typography.labelMedium,
          color = BlueNeon,
          fontWeight = FontWeight.SemiBold,
          modifier =
            Modifier.clickable {
              forceEmailPasswordMode = true
              onClearMessages()
            }
              .padding(6.dp)
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
) {
  BackHandler(enabled = true) {
    onExitToAuthPortal()
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
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Espas Admin Verouye",
          tint = BrandGold,
          modifier = Modifier.size(46.dp),
        )
        Text(
          text = "Espas Administratè Verouye",
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
          text = "Verifye idantite pwopriyetè a pou debloke jesyon legliz la.",
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
        )

        Spacer(modifier = Modifier.height(4.dp))

        GlassButton(
          text = "Dekonekte / Chanje Kont",
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
 * Quick PIN Unlock toggle + 4-6 digit PIN setup/update + Auto-Lock timeout selector + Manual Lock.
 */
@Composable
fun ChurchAdminSecuritySettingsCard(
  church: Church,
  isSaving: Boolean,
  onSaveSettings: (quickPinEnabled: Boolean, newPinOrNull: String?, autoLockMinutes: Int) -> Unit,
  onLockAdminNow: () -> Unit,
) {
  var showPinSetupDialog by remember { mutableStateOf(false) }
  var pendingEnableAfterPin by remember { mutableStateOf(false) }

  val quickPinOn = church.effectiveQuickPinEnabled
  val currentAutoLock = church.effectiveAutoLockMinutes

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
        text = "Sekirite Espas Admin (PIN & Auto-Lock)",
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Quick PIN Unlock Toggle
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Deblokaj Rapid ak PIN (4–6 chif)",
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            if (quickPinOn) "Aktive • Re-ouvri Espas Admin rapid apre verouyaj"
            else "Dezaktive • Mande Imèl + Modpas oswa Google chak fwa",
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
        )
      }
      Switch(
        checked = quickPinOn,
        enabled = !isSaving,
        onCheckedChange = { wantEnable ->
          if (wantEnable && church.adminPasscode.isNullOrBlank()) {
            pendingEnableAfterPin = true
            showPinSetupDialog = true
          } else {
            onSaveSettings(wantEnable, null, currentAutoLock)
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
        if (church.adminPasscode.isNullOrBlank()) "Kreye yon Kòd PIN Rapid"
        else "Chanje Kòd PIN Rapid la",
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
      text = "Delè Verouyaj Otomatik (Inaktivite)",
      style = MaterialTheme.typography.titleSmall,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Spacer(modifier = Modifier.height(8.dp))

    val options = listOf(5 to "5m", 10 to "10m", 15 to "15m", 30 to "30m", 0 to "Off")
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      options.forEach { (minutes, label) ->
        val selected = currentAutoLock == minutes
        Box(
          modifier =
            Modifier.weight(1f)
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
            style = MaterialTheme.typography.labelMedium,
            color = PureWhite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    GlassButton(
      text = "Verouye Espas Admin Kounye a",
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
      title = "Konfigire PIN Rapid Admin",
    ) {
      Text(
        text = "Antre yon kòd chif 4 a 6 chif. Li pral chiffres an SHA-256 pou senkronize ak Web la.",
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
        label = "Nouvo PIN (4–6 chif)",
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
        label = "Konfime PIN nan",
        leadingIcon = Icons.Default.Pin,
        visualTransformation = PasswordVisualTransformation(),
        keyboardType = KeyboardType.NumberPassword,
        testTag = "setup_quick_pin_confirm_input",
      )
      Spacer(modifier = Modifier.height(14.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = "Anile",
          isPrimaryNeon = false,
          onClick = { showPinSetupDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Anrejistre",
          isPrimaryNeon = true,
          onClick = {
            if (!pinInput.matches(Regex("^\\d{4,6}$"))) {
              dialogErr = "PIN lan dwe gen 4 a 6 chif."
              return@GlassButton
            }
            if (pinInput != pinConfirmInput) {
              dialogErr = "De PIN yo pa menm."
              return@GlassButton
            }
            showPinSetupDialog = false
            onSaveSettings(
              if (pendingEnableAfterPin) true else quickPinOn,
              pinInput,
              currentAutoLock,
            )
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
  onStayActive: () -> Unit,
  onLockNow: () -> Unit,
) {
  GlassDialog(
    onDismissRequest = onStayActive,
    title = "Verouyaj Otomatik",
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
        text = "Sesyon Admin pral verouye nan ${secondsRemaining}s poutèt inaktivite.",
        style = MaterialTheme.typography.bodyMedium,
        color = PureWhite,
        textAlign = TextAlign.Center,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = "Verouye Kounye a",
          isPrimaryNeon = false,
          accentColor = BrandOrange,
          onClick = onLockNow,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_lock_now",
        )
        GlassButton(
          text = "Kontinye",
          isPrimaryNeon = true,
          onClick = onStayActive,
          modifier = Modifier.weight(1f),
          testTag = "autolock_warning_continue",
        )
      }
    }
  }
}
