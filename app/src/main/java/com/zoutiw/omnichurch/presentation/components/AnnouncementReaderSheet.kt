package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementReaderSheet(
  announcement: Announcement,
  languageCode: String,
  reactionSummary: TargetReactionSummary?,
  commentsCountOverride: Int?,
  onDismiss: () -> Unit,
  onMarkRead: (String) -> Unit,
  onSelectReaction: (OmniReactionType) -> Unit,
  onOpenComments: () -> Unit,
  onShareClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val copy = remember(languageCode) { AnnouncementStrings.forLanguage(languageCode) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var showReactionSelector by remember { mutableStateOf(false) }
  var showFullImage by remember { mutableStateOf(false) }

  LaunchedEffect(announcement.id) {
    onMarkRead(announcement.id)
  }

  val resolvedTitle = remember(announcement, languageCode) { announcement.localizedTitle(languageCode) }
  val resolvedContent = remember(announcement, languageCode) { announcement.localizedContent(languageCode) }
  val timestampLabel = remember(announcement, languageCode) {
    AnnouncementStrings.formatTimestamp(announcement, languageCode)
  }
  val effectiveImage = announcement.effectiveImageUrl
  val displayedCommentsCount =
    maxOf(announcement.commentsCount, (commentsCountOverride ?: 0).toLong())

  val shareTitle =
    if (announcement.department.isNotBlank()) "📢 $resolvedTitle (${announcement.department})"
    else "📢 $resolvedTitle"

  val triggerShare: () -> Unit = {
    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    if (onShareClick != null) {
      onShareClick()
    } else {
      shareOmniChurchContent(
        context = context,
        title = shareTitle,
        body = resolvedContent,
      )
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = Color(0xFF071630),
    contentColor = PureWhite,
    dragHandle = {
      Box(
        modifier =
          Modifier.padding(top = 10.dp, bottom = 6.dp)
            .width(42.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(50))
            .background(GlassWhite.copy(alpha = 0.35f))
      )
    },
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 18.dp, vertical = 6.dp)
          .testTag("announcement_reader_sheet_${announcement.id}"),
    ) {
      // Top Header Bar
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Box(
            modifier =
              Modifier.size(38.dp)
                .shadow(6.dp, CircleShape, ambientColor = BrandOrange)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandGold, BrandOrange)))
                .border(1.2.dp, PureWhite.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.Campaign,
              contentDescription = null,
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
          }
          Column(modifier = Modifier.weight(1f)) {
            if (announcement.department.isNotBlank()) {
              Text(
                text = announcement.department,
                style = MaterialTheme.typography.labelLarge,
                color = BrandGold,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
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
                color = MutedGlassText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(SuccessGreen.copy(alpha = 0.16f))
                .border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 4.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = copy.readStatusLabel,
                tint = SuccessGreen,
                modifier = Modifier.size(13.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = copy.readStatusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = PureWhite,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("announcement_reader_close_btn"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = copy.closeLabel,
              tint = GlassWhite,
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))

      // Scrollable Reading Body
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .weight(1f, fill = false)
            .heightIn(max = 520.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Badges row (Pinned / Draft / Audience / Event Date)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (announcement.isPinned) {
            ReaderMetaChip(
              icon = Icons.Default.PushPin,
              text = copy.pinnedBadge,
              tint = BrandGold,
            )
          }
          if (!announcement.isPublished) {
            ReaderMetaChip(
              icon = Icons.Default.Campaign,
              text = copy.draftBadge,
              tint = BrandOrange,
            )
          }
          if (announcement.targetAudience.isNotBlank()) {
            ReaderMetaChip(
              icon = Icons.Default.Groups,
              text = announcement.targetAudience,
              tint = BlueNeon,
            )
          }
          val eventDate =
            listOfNotNull(
                announcement.date?.trim()?.takeIf { it.isNotEmpty() },
                announcement.time?.trim()?.takeIf { it.isNotEmpty() },
              )
              .joinToString(" • ")
          if (eventDate.isNotEmpty()) {
            ReaderMetaChip(
              icon = Icons.Default.CalendarToday,
              text = eventDate,
              tint = GlassWhite,
            )
          }
        }

        // Selectable Title + Full Body Content
        SelectionContainer {
          Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
              text = resolvedTitle,
              style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 30.sp,
              ),
              color = PureWhite,
              modifier = Modifier.testTag("announcement_reader_title"),
            )

            if (announcement.authorName.isNotBlank()) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = MutedGlassText,
                  modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = "${copy.publishedByLabel}: ${announcement.authorName}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MutedGlassText,
                )
              }
            }

            if (!effectiveImage.isNullOrBlank()) {
              ReaderPosterPreview(
                imageSource = effectiveImage,
                title = resolvedTitle,
                caption = announcement.imageCaption,
                tapHint = copy.tapToZoomHint,
                onClick = { showFullImage = true },
              )
            }

            if (resolvedContent.isNotBlank()) {
              Text(
                text = resolvedContent,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontSize = 16.sp,
                  lineHeight = 25.sp,
                ),
                color = GlassWhite,
                modifier = Modifier.fillMaxWidth().testTag("announcement_reader_content"),
              )
            }
          }
        }
      }

      HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))
      Spacer(modifier = Modifier.height(10.dp))

      // Reaction selector bar inside reader
      Floating3DReactionSelectorBar(
        visible = showReactionSelector,
        selectedReaction = reactionSummary?.currentUserReaction,
        onSelectReaction = { chosen ->
          onSelectReaction(chosen)
          showReactionSelector = false
        },
        onDismiss = { showReactionSelector = false },
      )

      // Action Bar (Reactions, Comments, Copy, Share)
      Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
          testTag = "reader_react_btn_${announcement.id}",
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .background(Color(0x55132952))
                .border(0.8.dp, GlassWhite.copy(alpha = 0.2f), RoundedCornerShape(50))
                .clickable(onClick = onOpenComments)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("reader_comment_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ChatBubbleOutline,
              contentDescription = stringResource(R.string.feed_comment_hint),
              tint = GlassWhite,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = displayedCommentsCount.toString(),
              style = MaterialTheme.typography.labelLarge,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
            )
          }

          IconButton(
            onClick = {
              copyOmniChurchTextToClipboard(
                context = context,
                label = resolvedTitle,
                text = "$shareTitle\n\n$resolvedContent",
              )
            },
            modifier = Modifier.minimumInteractiveComponentSize().testTag("reader_copy_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = copy.copyTextAction,
              tint = GlassWhite.copy(alpha = 0.90f),
              modifier = Modifier.size(19.dp),
            )
          }

          IconButton(
            onClick = triggerShare,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("reader_share_btn_${announcement.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = copy.shareAction,
              tint = BlueNeon,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }
    }
  }

  if (showFullImage && !effectiveImage.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = effectiveImage,
      title = resolvedTitle,
      department = announcement.department,
      onDismiss = { showFullImage = false },
    )
  }
}
