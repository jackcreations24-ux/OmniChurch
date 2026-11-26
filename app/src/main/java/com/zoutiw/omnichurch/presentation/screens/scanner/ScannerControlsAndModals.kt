package com.zoutiw.omnichurch.presentation.screens.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

/**
 * Compact header badge switch toggling between Prezans (Attendance) and Verifye (Verify Card)
 * without taking up vertical space or crowding narrow screens.
 */
@Composable
internal fun CompactPurposeBadgeSwitch(
  isVerifyMode: Boolean,
  copy: ScannerCopy,
  onSelectPurpose: (String) -> Unit,
) {
  val borderColor by
    animateColorAsState(
      targetValue = if (isVerifyMode) SuccessGreen.copy(alpha = 0.6f) else BlueNeon.copy(alpha = 0.5f),
      animationSpec = tween(220),
      label = "purpose_border",
    )

  Row(
    modifier =
      Modifier.clip(RoundedCornerShape(50))
        .background(Color(0xCC06132B))
        .border(1.dp, borderColor, RoundedCornerShape(50))
        .padding(3.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    CompactPurposeChip(
      label = copy.purposeAttendance,
      icon = Icons.Default.QrCodeScanner,
      selected = !isVerifyMode,
      activeBrush = OmniBrushes.ActiveBlueNeonBrush,
      inactiveColor = MutedGlassText,
      onClick = { onSelectPurpose("attendance") },
      testTag = "scanner_purpose_attendance",
    )
    CompactPurposeChip(
      label = copy.purposeVerifyShort,
      icon = Icons.Default.VerifiedUser,
      selected = isVerifyMode,
      activeBrush =
        Brush.horizontalGradient(
          listOf(SuccessGreen.copy(alpha = 0.9f), ElectricBlue.copy(alpha = 0.85f))
        ),
      inactiveColor = BrandGold,
      onClick = { onSelectPurpose("verify") },
      testTag = "scanner_purpose_verify",
    )
  }
}

@Composable
private fun CompactPurposeChip(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  activeBrush: Brush,
  inactiveColor: Color,
  onClick: () -> Unit,
  testTag: String,
) {
  val bgMod =
    if (selected) {
      Modifier.clip(RoundedCornerShape(50))
        .background(activeBrush)
        .border(0.8.dp, PureWhite.copy(alpha = 0.6f), RoundedCornerShape(50))
    } else {
      Modifier.clip(RoundedCornerShape(50))
    }

  Row(
    modifier =
      Modifier.then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) PureWhite else inactiveColor,
      modifier = Modifier.size(13.dp),
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = if (selected) PureWhite else inactiveColor,
      fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
      maxLines = 1,
      softWrap = false,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

/**
 * Slim single-line status indicator automatically reflecting either the single active service
 * (no manual selector needed) or Official Card Verification mode.
 */
@Composable
internal fun ScannerStatusStrip(
  isVerifyMode: Boolean,
  activeService: ChurchService?,
  copy: ScannerCopy,
  onOpenServices: () -> Unit,
) {
  val accentColor =
    when {
      isVerifyMode -> SuccessGreen
      activeService != null -> SuccessGreen
      else -> BrandOrange
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xA6081836))
        .border(1.dp, accentColor.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
        .padding(horizontal = 12.dp, vertical = 9.dp)
        .testTag(if (isVerifyMode) "scanner_verify_mode_banner" else "scanner_auto_service_strip"),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      if (isVerifyMode) {
        Icon(
          imageVector = Icons.Default.VerifiedUser,
          contentDescription = null,
          tint = SuccessGreen,
          modifier = Modifier.size(17.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = copy.verifyBannerSubtitle,
          style = MaterialTheme.typography.labelMedium,
          color = GlassWhite,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      } else if (activeService != null) {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(SuccessGreen),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = activeService.effectiveTitle,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f, fill = false),
        )
        if (activeService.dateFormatted.isNotBlank()) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = activeService.dateFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      } else {
        Box(
          modifier =
            Modifier.size(8.dp)
              .clip(CircleShape)
              .background(BrandOrange),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = copy.autoServiceNone,
          style = MaterialTheme.typography.labelMedium,
          color = BrandGold,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    if (!isVerifyMode && activeService == null) {
      Spacer(modifier = Modifier.width(8.dp))
      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(50))
            .background(ElectricBlue.copy(alpha = 0.28f))
            .border(1.dp, BlueNeon.copy(alpha = 0.65f), RoundedCornerShape(50))
            .clickable(onClick = onOpenServices)
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .testTag("scanner_create_active_service_btn"),
      ) {
        Text(
          text = copy.openServicesShort,
          style = MaterialTheme.typography.labelSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }
  }
}

/**
 * Ultra-clean 2-tab segmented control: Eskane QR | Kat NFC.
 */
@Composable
internal fun ScannerTwoTabBar(
  selectedMode: String,
  isVerifyMode: Boolean,
  copy: ScannerCopy,
  onSelectMode: (String) -> Unit,
) {
  val activeBrush =
    if (isVerifyMode) {
      Brush.horizontalGradient(listOf(SuccessGreen.copy(alpha = 0.85f), ElectricBlue.copy(alpha = 0.88f)))
    } else {
      OmniBrushes.ActiveBlueNeonBrush
    }

  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xA606132E))
        .border(1.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
        .padding(4.dp),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    ScannerModePill(
      label = copy.tabQr,
      icon = Icons.Default.QrCodeScanner,
      selected = selectedMode == "qr_scan",
      activeBrush = activeBrush,
      onClick = { onSelectMode("qr_scan") },
      testTag = "scanner_tab_qr_scan",
      modifier = Modifier.weight(1f),
    )
    ScannerModePill(
      label = copy.tabNfc,
      icon = Icons.Default.Nfc,
      selected = selectedMode == "nfc_scan",
      activeBrush = activeBrush,
      onClick = { onSelectMode("nfc_scan") },
      testTag = "scanner_tab_nfc",
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun ScannerModePill(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  activeBrush: Brush,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val bgMod =
    if (selected) {
      Modifier.shadow(8.dp, RoundedCornerShape(12.dp), ambientColor = ElectricBlue)
        .clip(RoundedCornerShape(12.dp))
        .background(activeBrush)
        .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(12.dp))
    } else {
      Modifier.clip(RoundedCornerShape(12.dp))
    }

  Row(
    modifier =
      modifier
        .then(bgMod)
        .clickable(onClick = onClick)
        .padding(horizontal = 10.dp, vertical = 10.dp)
        .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) PureWhite else MutedGlassText,
      modifier = Modifier.size(17.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) PureWhite else MutedGlassText,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}
