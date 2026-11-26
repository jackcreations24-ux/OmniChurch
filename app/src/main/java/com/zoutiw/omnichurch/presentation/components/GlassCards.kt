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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

@Composable
fun PremiumServiceCard(
  service: ChurchService,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  commentsCountOverride: Int? = null,
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  onShareClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
  onClick: (() -> Unit)? = null,
  onEditClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  val serviceDateSubtitle =
    service.dateFormatted.ifBlank { "${service.date} • ${service.startTime}".trim(' ', '•') }
  val shareTitle =
    "⛪ ${service.effectiveTitle}" +
      (service.churchName.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
  val shareBody = buildString {
    if (serviceDateSubtitle.isNotBlank()) {
      append("📅 Dat & Lè: $serviceDateSubtitle")
    }
    if (!service.description.isNullOrBlank()) {
      if (isNotEmpty()) append("\n")
      append(service.description)
    }
  }

  val triggerShare: () -> Unit = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (onShareClick != null) {
      onShareClick()
    } else {
      shareOmniChurchContent(
        context = context,
        title = shareTitle,
        body = shareBody,
      )
    }
  }

  PremiumGlassCard(
    cornerRadius = 0.dp,
    contentPadding = PaddingValues(0.dp),
    onClick = onClick,
    modifier = modifier.fillMaxWidth().testTag("feed_service_card_${service.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        MemberAvatar3D(
          fullName = service.effectiveTitle,
          size = 42.dp,
          photoUrl = service.imageUrl,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = service.effectiveTitle,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = serviceDateSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
        }
      }

      if (service.isLive) {
        Box(
          modifier =
            Modifier.shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = LiveBadgeRed)
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.horizontalGradient(listOf(LiveBadgeRed, Color(0xFFD91432)))
              )
              .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Text(
            text = stringResource(R.string.feed_live_badge),
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
      if (!service.imageUrl.isNullOrBlank()) {
        AsyncImage(
          model = service.imageUrl,
          contentDescription = service.effectiveTitle,
          contentScale = ContentScale.FillWidth,
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        WorshipStageBannerCanvas(modifier = Modifier.fillMaxWidth().height(152.dp))
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Floating3DReactionSelectorBar(
      visible = showReactionSelector,
      selectedReaction = reactionSummary?.currentUserReaction,
      onSelectReaction = { chosenReaction ->
        if (onSelectReaction != null) {
          onSelectReaction(chosenReaction)
        } else {
          onReactClick()
        }
      },
      onDismiss = { showReactionSelector = false },
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Stacked3DReactionCluster(
        reactionSummary = reactionSummary,
        fallbackCount = service.reactionsCount,
        onTriggerClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          showReactionSelector = !showReactionSelector
        },
        testTag = "service_react_btn_${service.id}",
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onCommentClick)
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("service_comment_btn_${service.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = stringResource(R.string.feed_comment_hint),
            tint = GlassWhite,
            modifier = Modifier.size(19.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          val displayedCommentsCount =
            maxOf(service.commentsCount, (commentsCountOverride ?: 0).toLong())
          Text(
            text = displayedCommentsCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = GlassWhite,
          )
        }

        Box(
          modifier =
            Modifier.clip(CircleShape)
              .clickable(onClick = triggerShare)
              .padding(6.dp)
              .testTag("service_share_btn_${service.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Pataje Sèvis la",
            tint = GlassWhite.copy(alpha = 0.90f),
            modifier = Modifier.size(19.dp),
          )
        }

        Box(
          modifier =
            Modifier.clip(CircleShape)
              .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showMoreMenuDialog = true
              }
              .padding(6.dp)
              .testTag("service_more_btn_${service.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Plis Opsyon",
            tint = GlassWhite.copy(alpha = 0.90f),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }

  if (showMoreMenuDialog) {
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = service.effectiveTitle,
    ) {
      Text(
        text = serviceDateSubtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))

      if (isAdmin && onEditClick != null) {
        GlassButton(
          text = "Modifye Sèvis sa a (Admin)",
          icon = Icons.Default.Edit,
          isPrimaryNeon = true,
          onClick = {
            showMoreMenuDialog = false
            onEditClick()
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "service_menu_edit_${service.id}",
        )
        Spacer(modifier = Modifier.height(8.dp))
      }

      GlassButton(
        text = "Pataje Sèvis sa a",
        icon = Icons.Default.Share,
        onClick = {
          showMoreMenuDialog = false
          triggerShare()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_share_${service.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Pataje Aplikasyon OmniChurch la",
        icon = Icons.Default.Share,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          shareOmniChurchGeneralApp(context, service.churchName)
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_share_app_${service.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Kopye Tèks Piblikasyon an",
        icon = Icons.Default.ContentCopy,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          copyOmniChurchTextToClipboard(
            context = context,
            label = service.effectiveTitle,
            text = "$shareTitle\n$shareBody",
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "service_menu_copy_${service.id}",
      )

      if (isAdmin && onDeleteClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(ErrorRed.copy(alpha = 0.22f))
              .border(1.dp, ErrorRed.copy(alpha = 0.75f), RoundedCornerShape(14.dp))
              .clickable {
                showMoreMenuDialog = false
                showConfirmDeleteDialog = true
              }
              .padding(vertical = 12.dp, horizontal = 14.dp)
              .testTag("service_menu_delete_${service.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Efase Sèvis la",
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Efase Sèvis sa a (Admin)",
              style = MaterialTheme.typography.titleSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }
  }

  if (showConfirmDeleteDialog && onDeleteClick != null) {
    GlassDialog(
      onDismissRequest = { showConfirmDeleteDialog = false },
      title = "Efase Sèvis la?",
    ) {
      Text(
        text = "Èske ou sèten ou vle efase sèvis \"${service.effectiveTitle}\" nan baz done legliz la? Aksyon sa a pa ka defèt.",
        style = MaterialTheme.typography.bodyMedium,
        color = GlassWhite,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        GlassButton(
          text = stringResource(R.string.common_cancel),
          isPrimaryNeon = false,
          onClick = { showConfirmDeleteDialog = false },
          modifier = Modifier.weight(1f),
        )
        GlassButton(
          text = "Wi, Efase",
          icon = Icons.Default.DeleteOutline,
          onClick = {
            showConfirmDeleteDialog = false
            onDeleteClick()
          },
          modifier = Modifier.weight(1f),
          testTag = "service_confirm_delete_btn_${service.id}",
        )
      }
    }
  }
}

@Composable
fun GlassLoadingIndicator(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier.fillMaxWidth().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    CircularProgressIndicator(color = BrandGold)
  }
}
