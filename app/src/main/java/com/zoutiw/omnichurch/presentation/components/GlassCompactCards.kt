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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@Composable
fun PremiumAnnouncementCard(
  announcement: Announcement,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  commentsCountOverride: Int? = null,
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  onShareClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }

  val shareTitle = "📢 ${announcement.title} (${announcement.department})"
  val shareBody = announcement.content

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
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(16.dp),
    modifier = modifier.fillMaxWidth().testTag("feed_announcement_card_${announcement.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .shadow(8.dp, CircleShape, ambientColor = BrandOrange)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(listOf(BrandGold, BrandOrange))
              )
              .border(1.5.dp, PureWhite.copy(alpha = 0.75f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = announcement.title,
            tint = PureWhite,
            modifier = Modifier.size(22.dp),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = announcement.title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = announcement.department,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGlassText,
          )
        }
      }

      if (announcement.isPinned) {
        Icon(
          imageVector = Icons.Default.Star,
          contentDescription = null,
          tint = BrandGold,
          modifier = Modifier.size(20.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = announcement.content,
      style = MaterialTheme.typography.bodyLarge,
      color = GlassWhite,
    )

    Spacer(modifier = Modifier.height(10.dp))

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
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Stacked3DReactionCluster(
        reactionSummary = reactionSummary,
        fallbackCount = announcement.reactionsCount,
        showLabelWhenUnselected = stringResource(R.string.feed_react_amen),
        onTriggerClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          showReactionSelector = !showReactionSelector
        },
        testTag = "announcement_react_btn_${announcement.id}",
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onCommentClick)
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .testTag("announcement_comment_btn_${announcement.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = stringResource(R.string.feed_comment_hint),
            tint = GlassWhite,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(5.dp))
          val displayedCommentsCount =
            maxOf(announcement.commentsCount, (commentsCountOverride ?: 0).toLong())
          Text(
            text = displayedCommentsCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = GlassWhite,
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
              .testTag("announcement_more_btn_${announcement.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Plis Opsyon",
            tint = GlassWhite.copy(alpha = 0.90f),
            modifier = Modifier.size(19.dp),
          )
        }
      }
    }
  }

  if (showMoreMenuDialog) {
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = announcement.title,
    ) {
      Text(
        text = announcement.department,
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))

      GlassButton(
        text = "Pataje Anons sa a",
        icon = Icons.Default.Share,
        onClick = {
          showMoreMenuDialog = false
          triggerShare()
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_share_${announcement.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Pataje Aplikasyon OmniChurch la",
        icon = Icons.Default.Share,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          shareOmniChurchGeneralApp(context)
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_share_app_${announcement.id}",
      )

      Spacer(modifier = Modifier.height(8.dp))

      GlassButton(
        text = "Kopye Tèks Anons la",
        icon = Icons.Default.ContentCopy,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          copyOmniChurchTextToClipboard(
            context = context,
            label = announcement.title,
            text = "$shareTitle\n\n$shareBody",
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_copy_${announcement.id}",
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
              .testTag("announcement_menu_delete_${announcement.id}"),
          contentAlignment = Alignment.Center,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Efase Anons la",
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Efase Anons sa a (Admin)",
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
      title = "Efase Anons la?",
    ) {
      Text(
        text = "Èske ou sèten ou vle efase anons \"${announcement.title}\" nan baz done legliz la?",
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
          testTag = "announcement_confirm_delete_btn_${announcement.id}",
        )
      }
    }
  }
}
