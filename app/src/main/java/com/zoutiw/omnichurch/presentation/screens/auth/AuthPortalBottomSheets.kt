package com.zoutiw.omnichurch.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.AuthenticatedMemberSession
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.components.GlassButton
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun AuthChurchSelectorBottomSheet(
  churches: List<Church>,
  churchesUiState: UiState<List<Church>>,
  resolvedChurch: Church?,
  onRetryLoadChurches: () -> Unit,
  onSelectChurch: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  GlassModalBottomSheet(onDismissRequest = onDismiss) {
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
          onClick = onRetryLoadChurches,
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
            onClick = onRetryLoadChurches,
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
            onClick = onRetryLoadChurches,
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
                onClick = { onSelectChurch(church.id) },
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

@Composable
internal fun AuthLinkGoogleBottomSheet(
  googlePayload: GoogleAccountPayload,
  linkMatrikilInput: String,
  onLinkMatrikilChange: (String) -> Unit,
  linkPinInput: String,
  onLinkPinChange: (String) -> Unit,
  onConfirmLink: () -> Unit,
  onDismiss: () -> Unit,
) {
  GlassModalBottomSheet(onDismissRequest = onDismiss) {
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
        onValueChange = onLinkMatrikilChange,
        label = "ID Manm (Matrikil) oswa Telefòn",
        placeholder = "CH001-JD4827",
        leadingIcon = Icons.Default.Badge,
        testTag = "link_google_matrikil_input",
      )

      GlassInputField(
        value = linkPinInput,
        onValueChange = onLinkPinChange,
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
        onClick = onConfirmLink,
        modifier = Modifier.fillMaxWidth(),
        testTag = "confirm_link_google_button",
      )
    }
  }
}
