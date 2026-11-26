package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun AdminLegacyPinMigrationStep1(
  legacyOldPinInput: String,
  onLegacyOldPinChange: (String) -> Unit,
  isBusy: Boolean,
  onClearMessages: () -> Unit,
  onVerifyLegacyPin: (String) -> Unit,
) {
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
      onLegacyOldPinChange(it)
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

@Composable
internal fun AdminLegacyPinMigrationStep2(
  legacyEmailInput: String,
  onLegacyEmailChange: (String) -> Unit,
  legacyPasswordInput: String,
  onLegacyPasswordChange: (String) -> Unit,
  legacyConfirmPasswordInput: String,
  onLegacyConfirmPasswordChange: (String) -> Unit,
  isBusy: Boolean,
  onClearMessages: () -> Unit,
  onCompleteLegacyMigration: (email: String, newPass: String, confirmPass: String) -> Unit,
  onResetLegacyMigration: () -> Unit,
) {
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
      onLegacyEmailChange(it)
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
      onLegacyPasswordChange(it)
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
      onLegacyConfirmPasswordChange(it)
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
