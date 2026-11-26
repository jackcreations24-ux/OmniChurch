package com.zoutiw.omnichurch.presentation.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

/**
 * 14. GlassSegmentedControl — Two-option 3D glass segmented control with glowing neon active pill.
 */
@Composable
fun GlassSegmentedControl(
  leftLabel: String,
  leftIcon: ImageVector,
  rightLabel: String,
  rightIcon: ImageVector,
  isLeftSelected: Boolean,
  onSelectLeft: () -> Unit,
  onSelectRight: () -> Unit,
  leftTestTag: String = "segment_left",
  rightTestTag: String = "segment_right",
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(54.dp)
        .clip(RoundedCornerShape(50))
        .background(Color(0xCC0A1836))
        .border(1.2.dp, GlassWhite.copy(alpha = 0.28f), RoundedCornerShape(50))
        .padding(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left Segment
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .then(
            if (isLeftSelected) {
              Modifier.shadow(12.dp, RoundedCornerShape(50), ambientColor = BlueNeon)
                .clip(RoundedCornerShape(50))
                .background(OmniBrushes.ActiveBlueNeonBrush)
                .border(1.3.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(50))
            } else {
              Modifier.clip(RoundedCornerShape(50))
            }
          )
          .clickable(onClick = onSelectLeft)
          .testTag(leftTestTag),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = leftIcon,
          contentDescription = leftLabel,
          tint = PureWhite,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = leftLabel,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // Right Segment
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .then(
            if (!isLeftSelected) {
              Modifier.shadow(12.dp, RoundedCornerShape(50), ambientColor = BlueNeon)
                .clip(RoundedCornerShape(50))
                .background(OmniBrushes.ActiveBlueNeonBrush)
                .border(1.3.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(50))
            } else {
              Modifier.clip(RoundedCornerShape(50))
            }
          )
          .clickable(onClick = onSelectRight)
          .testTag(rightTestTag),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = rightIcon,
          contentDescription = rightLabel,
          tint = PureWhite,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = rightLabel,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * 15. PremiumSuccessPanel — Reusable emerald-green 3D attendance confirmation panel
 *     matching Phone 3's bottom panel in the reference image.
 */
@Composable
fun PremiumSuccessPanel(
  member: Member,
  serviceTitle: String,
  recordedTimeFormatted: String,
  status: String,
  modifier: Modifier = Modifier,
  isVisitor: Boolean = false,
  homeChurchName: String? = null,
) {
  val isLate = status == "late"
  val badgeLabel =
    if (isLate) stringResource(R.string.attendance_status_late)
    else stringResource(R.string.attendance_status_on_time)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = SuccessGreen)
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xE60B4F35),
              Color(0xF2072E22),
              Color(0xFA061826),
            )
          )
        )
        .border(
          1.6.dp,
          Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.85f), SuccessGreen, Color(0xFF0A6E46))),
          RoundedCornerShape(22.dp),
        )
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(Color.Transparent, PureWhite.copy(alpha = 0.7f), SuccessGreen, Color.Transparent)
              ),
            start = Offset(size.width * 0.2f, 1.dp.toPx()),
            end = Offset(size.width * 0.8f, 1.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
          )
        }
        .padding(14.dp)
        .testTag("scanner_success_confirmation_card"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = SuccessGreen,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "${stringResource(R.string.attendance_registered_title)} ✓",
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
      )
      if (isVisitor) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(ElectricBlue.copy(alpha = 0.35f))
              .border(1.dp, BlueNeon, RoundedCornerShape(50))
              .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text(
            text = "Vizitè",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 52.dp,
          photoUrl = member.effectivePhotoUrl,
          borderColor = SuccessGreen,
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = member.effectiveFullName,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (isVisitor) {
                val origin = homeChurchName?.takeIf { it.isNotBlank() } ?: member.churchName
                "Vizitè • Soti nan: $origin"
              } else {
                "ID: ${member.effectiveMatrikil}"
              },
            style = MaterialTheme.typography.bodySmall,
            color = if (isVisitor) BlueNeon else GlassWhite.copy(alpha = 0.88f),
            fontWeight = if (isVisitor) FontWeight.SemiBold else FontWeight.Normal,
          )
        }
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(10.dp))
            .background(
              if (isLate) BrandOrange.copy(alpha = 0.28f)
              else Color(0xFF0A6E46)
            )
            .border(
              1.2.dp,
              if (isLate) BrandGold else SuccessGreen,
              RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = badgeLabel,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "${stringResource(R.string.attendance_service_prefix)} $serviceTitle",
        style = MaterialTheme.typography.bodySmall,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = GlassWhite.copy(alpha = 0.85f),
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = recordedTimeFormatted,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

/** 16. GlassLoadingIndicator — Pulsing neon ring loader for real-time Firebase queries. */
@Composable
fun GlassLoadingIndicator(
  message: String = stringResource(R.string.common_loading),
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator(
      color = BlueNeon,
      trackColor = GlassWhite.copy(alpha = 0.15f),
      strokeWidth = 3.dp,
      modifier = Modifier.size(44.dp),
    )
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = GlassWhite.copy(alpha = 0.85f),
      textAlign = TextAlign.Center,
    )
  }
}

/** 17. PremiumEmptyState — 3D glassmorphic empty state when Firestore returns 0 documents. */
@Composable
fun PremiumEmptyState(
  title: String,
  subtitle: String,
  icon: ImageVector,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  PremiumGlassCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(24.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(ElectricBlue.copy(alpha = 0.45f), PrimaryNavy)
              )
            )
            .border(1.5.dp, BlueNeon.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = BlueNeon,
          modifier = Modifier.size(30.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = PureWhite,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MutedGlassText,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          GlassButton(
            text = actionLabel,
            onClick = onActionClick,
            modifier = Modifier.weight(1f),
            testTag = "empty_state_primary_action",
          )
          if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            GlassButton(
              text = secondaryActionLabel,
              onClick = onSecondaryActionClick,
              isPrimaryNeon = false,
              modifier = Modifier.weight(1f),
              testTag = "empty_state_secondary_action",
            )
          }
        }
      }
    }
  }
}
