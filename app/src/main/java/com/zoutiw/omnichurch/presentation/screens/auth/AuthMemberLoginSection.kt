package com.zoutiw.omnichurch.presentation.screens.auth

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.zoutiw.omnichurch.core.utilities.BiometricQuickUnlockHelper
import com.zoutiw.omnichurch.data.models.OmniSessionState
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.Premium3DGoogleIconButton
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AuthMemberLoginSection(
  copy: AuthPortalCopy,
  context: Context,
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  sessionState: OmniSessionState,
  isAuthenticating: Boolean,
  matrikilOrPhoneInput: String,
  onMatrikilOrPhoneChange: (String) -> Unit,
  memberPinInput: String,
  onMemberPinChange: (String) -> Unit,
  onClearFeedback: () -> Unit,
  onLocalError: (String?) -> Unit,
  onLoginMemberWithPin: (selectedChurchId: String?, idOrPhone: String, pin: String) -> Unit,
  onUnlockBiometricSession: () -> Unit,
  onOptionalGoogleMemberSignIn: (
    selectedChurchId: String?,
    googlePayload: GoogleAccountPayload,
    onNeedsLinking: () -> Unit,
  ) -> Unit,
  onNeedsGoogleLinking: (GoogleAccountPayload) -> Unit,
) {
  GlassInputField(
    value = matrikilOrPhoneInput,
    onValueChange = {
      onMatrikilOrPhoneChange(it)
      onLocalError(null)
      onClearFeedback()
    },
    label = copy.memberIdInputLabel,
    placeholder = copy.memberIdPlaceholder,
    leadingIcon = Icons.Default.Badge,
    testTag = "member_matrikil_input",
  )

  GlassInputField(
    value = memberPinInput,
    onValueChange = {
      onMemberPinChange(it)
      onLocalError(null)
      onClearFeedback()
    },
    label = copy.memberPinInputLabel,
    placeholder = copy.memberPinPlaceholder,
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
      text = copy.connectButtonText,
      icon = Icons.Default.VerifiedUser,
      isPrimaryNeon = true,
      isLoading = isAuthenticating,
      onClick = {
        onLocalError(null)
        onClearFeedback()
        onLoginMemberWithPin(null, matrikilOrPhoneInput, memberPinInput)
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
          onError = { err -> onLocalError(err) },
        )
      },
      modifier = Modifier.fillMaxWidth(),
      testTag = "biometric_quick_unlock_button",
    )
  }

  Spacer(modifier = Modifier.height(4.dp))

  Premium3DGoogleIconButton(
    caption = copy.googleButtonCaption,
    onClick = {
      onLocalError(null)
      onClearFeedback()
      launchOptionalGoogleSignIn(
        context = context,
        credentialManager = credentialManager,
        scope = scope,
        onSuccess = { googlePayload ->
          onOptionalGoogleMemberSignIn(
            null,
            googlePayload,
            { onNeedsGoogleLinking(googlePayload) },
          )
        },
        onError = { msg -> onLocalError(msg) },
      )
    },
    testTag = "optional_google_login_button",
  )
}
