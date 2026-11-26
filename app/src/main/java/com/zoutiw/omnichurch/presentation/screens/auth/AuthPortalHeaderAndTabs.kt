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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.Church
import com.zoutiw.omnichurch.presentation.viewmodels.UiState
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
internal fun AuthLanguageSelectorStrip(
  currentLanguageCode: String,
  onSelectLanguage: (String) -> Unit,
) {
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
}

@Composable
internal fun AuthPortalModeSegmentedTabs(
  isMemberMode: Boolean,
  memberTabLabel: String,
  adminTabLabel: String,
  onSelectMemberMode: (Boolean) -> Unit,
) {
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
          .clickable { onSelectMemberMode(true) }
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
          text = memberTabLabel,
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
          .clickable { onSelectMemberMode(false) }
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
          text = adminTabLabel,
          style = MaterialTheme.typography.labelLarge,
          color = if (!isMemberMode) DeepNavy else MutedGlassText,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
internal fun AuthChurchStatusBannerAndErrorSection(
  showPublicChurchSelector: Boolean,
  churches: List<Church>,
  churchesUiState: UiState<List<Church>>,
  resolvedChurch: Church?,
  isMemberMode: Boolean,
  autoMemberBadgeLabel: String,
  autoChurchBadgeLabel: String,
  displayedError: String?,
  onOpenChurchSelector: () -> Unit,
) {
  if (showPublicChurchSelector) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DeepNavy.copy(alpha = 0.78f))
          .border(1.dp, BlueNeon.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
          .clickable(onClick = onOpenChurchSelector)
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
}
