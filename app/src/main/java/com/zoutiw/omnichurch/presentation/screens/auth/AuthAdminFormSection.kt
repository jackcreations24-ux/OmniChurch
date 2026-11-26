package com.zoutiw.omnichurch.presentation.screens.auth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.data.models.OmniAuthEvaluator
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassSegmentedControl
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AuthAdminFormSection(
  copy: AuthPortalCopy,
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  isAuthenticating: Boolean,
  isAdminEmailMode: Boolean,
  onSelectAdminEmailMode: (Boolean) -> Unit,
  adminEmailInput: String,
  onAdminEmailChange: (String) -> Unit,
  adminPasswordInput: String,
  onAdminPasswordChange: (String) -> Unit,
  isAdminPasswordVisible: Boolean,
  onToggleAdminPasswordVisible: () -> Unit,
  rememberAdminMe: Boolean,
  onToggleRememberAdmin: (Boolean) -> Unit,
  resetFeedbackMessage: String?,
  onResetFeedbackChange: (String?) -> Unit,
  onLocalError: (String?) -> Unit,
  onClearFeedback: () -> Unit,
  onSendAdminPasswordReset: (
    email: String,
    onSent: (String) -> Unit,
    onError: (String) -> Unit,
  ) -> Unit,
  onLoginAdminSmartWithEmailAndPassword: (email: String, password: String, rememberMe: Boolean) -> Unit,
  adminGooglePayload: GoogleAccountPayload?,
  onAdminGooglePayloadChange: (GoogleAccountPayload?) -> Unit,
  showLegacyAdminCodeFallback: Boolean,
  onToggleLegacyAdminCodeFallback: () -> Unit,
  adminPasscodeInput: String,
  onAdminPasscodeChange: (String) -> Unit,
  isAdminPasscodeVisible: Boolean,
  onToggleAdminPasscodeVisible: () -> Unit,
  resolvedChurch: Church?,
  selectedChurchId: String?,
  onLoginAdmin: (
    selectedChurchId: String,
    enteredPasscode: String?,
    googleEmail: String?,
    googleUid: String?,
  ) -> Unit,
  onLoginAdminSmartWithGoogle: () -> Unit,
) {
  GlassSegmentedControl(
    leftLabel = copy.adminTabEmailLabel,
    rightLabel = copy.adminTabGoogleLabel,
    isLeftSelected = isAdminEmailMode,
    onSelectLeft = {
      onSelectAdminEmailMode(true)
      onLocalError(null)
      onResetFeedbackChange(null)
      onClearFeedback()
    },
    onSelectRight = {
      onSelectAdminEmailMode(false)
      onLocalError(null)
      onResetFeedbackChange(null)
      onClearFeedback()
    },
    leftIcon = Icons.Default.Email,
    rightIcon = Icons.Default.VerifiedUser,
    leftTestTag = "admin_mode_email_tab",
    rightTestTag = "admin_mode_code_tab",
  )

  if (isAdminEmailMode) {
    resetFeedbackMessage?.let { feedbackMsg ->
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
          text = feedbackMsg,
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
        onAdminEmailChange(it)
        onLocalError(null)
        onResetFeedbackChange(null)
        onClearFeedback()
      },
      label = copy.adminEmailLabel,
      placeholder = "egz: paste@leglizou.org",
      leadingIcon = Icons.Default.Email,
      keyboardType = KeyboardType.Email,
      testTag = "admin_email_input",
    )

    GlassInputField(
      value = adminPasswordInput,
      onValueChange = {
        onAdminPasswordChange(it)
        onLocalError(null)
        onResetFeedbackChange(null)
        onClearFeedback()
      },
      label = copy.adminPasswordLabel,
      placeholder = "Antre modpas legliz la",
      leadingIcon = Icons.Default.Lock,
      trailingIcon = {
        IconButton(
          onClick = onToggleAdminPasswordVisible,
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
              onToggleRememberAdmin(!rememberAdminMe)
            }
            .padding(end = 8.dp)
            .testTag("admin_remember_me_row"),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Checkbox(
          checked = rememberAdminMe,
          onCheckedChange = onToggleRememberAdmin,
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
          text = copy.rememberMeLabel,
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
              onLocalError(null)
              onResetFeedbackChange(null)
              onClearFeedback()
              onSendAdminPasswordReset(
                adminEmailInput,
                { msg -> onResetFeedbackChange(msg) },
                { err -> onLocalError(err) },
              )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("admin_forgot_password_button"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = copy.forgotPasswordLabel,
          style = MaterialTheme.typography.labelMedium,
          color = BrandGold,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    GlassButton(
      text = copy.connectButtonText,
      icon = Icons.Default.AdminPanelSettings,
      isPrimaryNeon = true,
      isLoading = isAuthenticating,
      accentColor = BrandOrange,
      onClick = {
        onLocalError(null)
        onResetFeedbackChange(null)
        onClearFeedback()
        val cleanEmail = adminEmailInput.trim()
        when {
          cleanEmail.isEmpty() -> onLocalError(OmniAuthEvaluator.ERROR_EMPTY_EMAIL)
          !cleanEmail.contains("@") || !cleanEmail.substringAfter("@").contains(".") ->
            onLocalError(OmniAuthEvaluator.ERROR_INVALID_EMAIL)
          adminPasswordInput.isBlank() ->
            onLocalError(OmniAuthEvaluator.ERROR_EMPTY_PASSWORD)
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
    adminGooglePayload?.let { payload ->
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
          text = "Google Admin: ${payload.email}",
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
        caption = adminGooglePayload?.let { "Chanje (${it.email})" } ?: "Google Admin",
        onClick = {
          onLocalError(null)
          onClearFeedback()
          launchOptionalGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onSuccess = { payload ->
              onAdminGooglePayloadChange(payload)
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
            onError = { msg -> onLocalError(msg) },
          )
        },
        testTag = "admin_google_verify_button",
      )
    }

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
        text = copy.adminQuickPinInfoText,
        style = MaterialTheme.typography.labelSmall,
        color = GlassWhite.copy(alpha = 0.88f),
      )
    }

    Box(
      modifier =
        Modifier.minimumInteractiveComponentSize()
          .clip(RoundedCornerShape(10.dp))
          .clickable {
            onToggleLegacyAdminCodeFallback()
            onLocalError(null)
            onClearFeedback()
          }
          .padding(horizontal = 10.dp, vertical = 6.dp)
          .testTag("toggle_legacy_admin_code_button"),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = if (showLegacyAdminCodeFallback) copy.hideLegacyCodeLabel else copy.showLegacyCodeLabel,
        style = MaterialTheme.typography.labelSmall,
        color = MutedGlassText,
        fontWeight = FontWeight.Medium,
      )
    }

    if (showLegacyAdminCodeFallback) {
      GlassInputField(
        value = adminPasscodeInput,
        onValueChange = {
          onAdminPasscodeChange(it)
          onLocalError(null)
          onClearFeedback()
        },
        label = "Ansyen Kòd Sekirite Legliz la",
        placeholder = "Antre Admin Passcode legliz la",
        leadingIcon = Icons.Default.Lock,
        trailingIcon = {
          IconButton(
            onClick = onToggleAdminPasscodeVisible,
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
          onLocalError(null)
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
