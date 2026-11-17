package com.zoutiw.omnichurch.presentation.screens.services

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.presentation.components.GlassTopBar
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.PremiumGlassCard
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.presentation.components.WorshipStageBannerCanvas
import com.zoutiw.omnichurch.presentation.components.shareOmniChurchContent
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
fun ServiceDetailsScreen(
  service: ChurchService,
  reactionSummary: TargetReactionSummary?,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBackClick)
  val context = LocalContext.current

  Column(
    modifier = modifier.fillMaxSize().background(OmniBrushes.DeepBackgroundGradient).statusBarsPadding(),
  ) {
    GlassTopBar(
      title = service.effectiveTitle,
      onBackClick = onBackClick,
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, GlassWhite.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
      ) {
        if (!service.imageUrl.isNullOrBlank()) {
          AsyncImage(
            model = service.imageUrl,
            contentDescription = service.effectiveTitle,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          WorshipStageBannerCanvas(modifier = Modifier.fillMaxWidth().height(220.dp))
        }
      }

      PremiumGlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = service.effectiveTitle,
              style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = BrandGold,
                modifier = Modifier.size(15.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = service.dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = GlassWhite,
              )
            }
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(50))
              .background(SuccessGreen.copy(alpha = 0.25f))
              .border(1.dp, SuccessGreen, RoundedCornerShape(50))
              .padding(horizontal = 12.dp, vertical = 6.dp)
          ) {
            Text(
              text = service.category.uppercase(),
              style = MaterialTheme.typography.labelSmall,
              color = SuccessGreen,
              fontWeight = FontWeight.Bold,
            )
          }
        }

        if (!service.description.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = GlassWhite.copy(alpha = 0.15f))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = service.description,
            style = MaterialTheme.typography.bodyMedium,
            color = GlassWhite,
          )
        }
      }

      if (!service.leader.isNullOrBlank() || !service.reader.isNullOrBlank() || !service.preacher.isNullOrBlank()) {
        PremiumGlassCard(
          cornerRadius = 20.dp,
          contentPadding = PaddingValues(16.dp),
        ) {
          Text(
            text = "Responsab Sèvis la",
            style = MaterialTheme.typography.titleMedium,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(12.dp))

          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!service.leader.isNullOrBlank()) {
              ParticipantDetailRow(role = "Adoratè / Dirijan", name = service.leader)
            }
            if (!service.reader.isNullOrBlank()) {
              ParticipantDetailRow(role = "Lektè Vèsè", name = service.reader)
            }
            if (!service.preacher.isNullOrBlank()) {
              ParticipantDetailRow(role = "Predikatè Sèvis", name = service.preacher)
            }
          }
        }
      }

      PremiumGlassCard(
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Stacked3DReactionCluster(
            reactionSummary = reactionSummary,
            fallbackCount = service.reactionsCount,
            onTriggerClick = onReactClick,
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onCommentClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
              Icon(
                imageVector = Icons.Default.ChatBubbleOutline,
                contentDescription = null,
                tint = GlassWhite,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${service.commentsCount} Kòmantè",
                style = MaterialTheme.typography.labelLarge,
                color = GlassWhite,
              )
            }

            IconButton(
              onClick = {
                shareOmniChurchContent(
                  context = context,
                  title = "⛪ ${service.effectiveTitle}",
                  body = "📅 Dat: ${service.dateFormatted}",
                )
              },
              modifier = Modifier.size(38.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Pataje",
                tint = PureWhite,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}

@Composable
private fun ParticipantDetailRow(
  role: String,
  name: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0x550A1836))
      .border(1.dp, GlassWhite.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MemberAvatar3D(fullName = name, size = 42.dp)
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = role,
        style = MaterialTheme.typography.labelSmall,
        color = BrandGold,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
