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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite

private val AnnouncementCampaignBadgeBrush =
  Brush.linearGradient(listOf(BrandGold, BrandOrange))
private val AnnouncementCampaignBorderColor = PureWhite.copy(alpha = 0.75f)
private val UnreadPillBgColor = BlueNeon.copy(alpha = 0.22f)
private val UnreadPillBorderColor = BlueNeon.copy(alpha = 0.85f)

/**
 * 13. PremiumAnnouncementCard — Self-sufficient 3D announcement card matching Phone 1's "Anons Ofisyèl",
 *     upgraded with unread indicator sign ("siy nouvo anons"), click-to-mark-read, timestamp,
 *     7 Ultra-Premium 3D Realistic Reactions, native Share, and '...' options menu.
 */
@Composable
fun PremiumAnnouncementCard(
  announcement: Announcement,
  onReactClick: () -> Unit,
  onCommentClick: () -> Unit,
  modifier: Modifier = Modifier,
  reactionSummary: TargetReactionSummary? = null,
  commentsCountOverride: Int? = null,
  languageCode: String = "ht",
  onSelectReaction: ((OmniReactionType) -> Unit)? = null,
  isAdmin: Boolean = false,
  canEdit: Boolean = isAdmin,
  canDelete: Boolean = isAdmin,
  isUnread: Boolean = false,
  onMarkRead: (() -> Unit)? = null,
  onCardClick: (() -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  onEditClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val copy = remember(languageCode) { AnnouncementStrings.forLanguage(languageCode) }
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }
  var showImageLightbox by remember { mutableStateOf(false) }
  var isContentOverflowing by remember(announcement.id, languageCode) { mutableStateOf(false) }

  val resolvedTitle = remember(announcement, languageCode) { announcement.localizedTitle(languageCode) }
  val resolvedContent = remember(announcement, languageCode) { announcement.localizedContent(languageCode) }
  val effectiveImage = announcement.effectiveImageUrl
  val shareTitle =
    remember(announcement.department, resolvedTitle) {
      if (announcement.department.isNotBlank()) "📢 $resolvedTitle (${announcement.department})"
      else "📢 $resolvedTitle"
    }
  val shareBody = resolvedContent
  val timestampLabel =
    remember(announcement.createdAt, announcement.updatedAt, announcement.date, announcement.time, languageCode) {
      AnnouncementStrings.formatTimestamp(announcement, languageCode)
    }

  val markReadIfNeeded: () -> Unit = {
    if (isUnread) {
      onMarkRead?.invoke()
    }
  }

  val triggerShare: () -> Unit = {
    markReadIfNeeded()
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
    isHighlighted = isUnread,
    accentColor = BlueNeon,
    onClick =
      if (onCardClick != null) {
        { onCardClick() }
      } else null,
    contentPadding = PaddingValues(0.dp),
    modifier = modifier.fillMaxWidth().testTag("feed_announcement_card_${announcement.id}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(contentAlignment = Alignment.TopEnd) {
          Box(
            modifier =
              Modifier.size(42.dp)
                .shadow(8.dp, CircleShape, ambientColor = BrandOrange)
                .clip(CircleShape)
                .background(AnnouncementCampaignBadgeBrush)
                .border(1.5.dp, AnnouncementCampaignBorderColor, CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = announcement.title,
              tint = PureWhite,
              modifier = Modifier.size(22.dp),
            )
          }
          if (isUnread) {
            Box(
              modifier =
                Modifier.size(12.dp)
                  .clip(CircleShape)
                  .background(BlueNeon)
                  .border(1.5.dp, PureWhite, CircleShape)
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.titleMedium,
            color = if (isUnread) BrandGold else PureWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          if (announcement.department.isNotBlank()) {
            Text(
              text = announcement.department,
              style = MaterialTheme.typography.bodySmall,
              color = MutedGlassText,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Schedule,
              contentDescription = null,
              tint = BlueNeon.copy(alpha = 0.85f),
              modifier = Modifier.size(12.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = timestampLabel,
              style = MaterialTheme.typography.labelSmall,
              color = GlassWhite.copy(alpha = 0.78f),
              modifier = Modifier.testTag("announcement_timestamp_${announcement.id}"),
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (isUnread) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(UnreadPillBgColor)
                .border(1.dp, UnreadPillBorderColor, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag("announcement_unread_badge_${announcement.id}"),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandGold)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = copy.newBadge,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          }
        }

        if (announcement.isPinned) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = copy.pinnedBadge,
            tint = BrandGold,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    if (resolvedContent.isNotBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = resolvedContent,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        color = GlassWhite,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layoutResult ->
          if (layoutResult.hasVisualOverflow) {
            isContentOverflowing = true
          }
        },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
      )
      if ((isContentOverflowing || resolvedContent.length > 180) && onCardClick != null) {
        AnnouncementReadMorePrompt(
          label = copy.readFullAction,
          onClick = onCardClick,
          modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp)
            .testTag("announcement_read_more_${announcement.id}"),
        )
      }
    }

    if (!effectiveImage.isNullOrBlank()) {
      Spacer(modifier = Modifier.height(10.dp))
      AnnouncementMediaShowcase(
        imageSource = effectiveImage,
        title = resolvedTitle,
        announcementId = announcement.id,
        onImageClick = {
          markReadIfNeeded()
          showImageLightbox = true
        },
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
    ) {
      Floating3DReactionSelectorBar(
        visible = showReactionSelector,
        selectedReaction = reactionSummary?.currentUserReaction,
        onSelectReaction = { chosenReaction ->
          markReadIfNeeded()
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
            markReadIfNeeded()
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
                .clickable {
                  markReadIfNeeded()
                  onCommentClick()
                }
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
                .clickable(onClick = triggerShare)
                .padding(6.dp)
                .testTag("announcement_share_btn_${announcement.id}"),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Pataje Anons la",
              tint = GlassWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(18.dp),
            )
          }

          Box(
            modifier =
              Modifier.clip(CircleShape)
                .clickable {
                  markReadIfNeeded()
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
  }

  if (showMoreMenuDialog) {
    AnnouncementCardMoreMenuDialog(
      context = context,
      announcement = announcement,
      resolvedTitle = resolvedTitle,
      shareTitle = shareTitle,
      shareBody = shareBody,
      copy = copy,
      canEdit = canEdit,
      isAdmin = isAdmin,
      canDelete = canDelete,
      onTriggerShare = triggerShare,
      onEditClick = onEditClick,
      onDeleteClick = onDeleteClick,
      onRequestDelete = { showConfirmDeleteDialog = true },
      onDismiss = { showMoreMenuDialog = false },
    )
  }

  if (showConfirmDeleteDialog && onDeleteClick != null) {
    AnnouncementCardConfirmDeleteDialog(
      announcementId = announcement.id,
      resolvedTitle = resolvedTitle,
      copy = copy,
      onConfirmDelete = onDeleteClick,
      onDismiss = { showConfirmDeleteDialog = false },
    )
  }

  if (showImageLightbox && !effectiveImage.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = effectiveImage,
      title = resolvedTitle,
      department = announcement.department,
      onDismiss = { showImageLightbox = false },
    )
  }
}
