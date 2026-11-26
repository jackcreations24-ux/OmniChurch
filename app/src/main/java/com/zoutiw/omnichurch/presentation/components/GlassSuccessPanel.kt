package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
fun PremiumSuccessPanel(
  member: Member,
  serviceTitle: String,
  recordedTimeFormatted: String,
  status: String,
  modifier: Modifier = Modifier,
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
            text = "ID: ${member.effectiveMatrikil}",
            style = MaterialTheme.typography.bodySmall,
            color = GlassWhite.copy(alpha = 0.88f),
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
