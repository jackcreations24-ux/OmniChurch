package com.zoutiw.omnichurch.presentation.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.data.models.formatCreoleDateWithDay
import com.zoutiw.omnichurch.presentation.components.Floating3DReactionSelectorBar
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.LogoVariant
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.OmniChurchLogo
import com.zoutiw.omnichurch.presentation.components.OmniChurchProBadge
import com.zoutiw.omnichurch.presentation.components.PremiumProfileCard
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.flow.Flow

@Composable
internal fun MemberHomeTopHeader(
  isChromeVisible: Boolean,
  headerBackdropAlpha: Float,
  displayName: String,
  spaceSubtitle: String,
  donationConfig: DeveloperDonationConfig,
  currentLanguageCode: String,
  memberPhotoUrl: String?,
  onHeaderHeightMeasured: (Int) -> Unit,
  onOpenProfile: () -> Unit,
  onOpenDonate: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = isChromeVisible,
    enter = slideInVertically(
      initialOffsetY = { -it },
      animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(durationMillis = 120)),
    exit = slideOutVertically(
      targetOffsetY = { -it },
      animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
    ) + fadeOut(animationSpec = tween(durationMillis = 110)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF061124).copy(alpha = headerBackdropAlpha))
        .onSizeChanged { size ->
          if (size.height > 0) onHeaderHeightMeasured(size.height)
        }
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
      BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        val isTabletOrExpanded = maxWidth >= 600.dp
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
        ) {
          if (isTabletOrExpanded) {
            OmniChurchLogo(
              variant = LogoVariant.Full,
              modifier = Modifier.height(44.dp),
              contentDescription = "OmniChurch PRO Official Logo",
            )
            Spacer(modifier = Modifier.width(8.dp))
            OmniChurchProBadge()
          } else {
            OmniChurchLogo(
              variant = LogoVariant.Icon,
              modifier = Modifier.size(32.dp),
              contentDescription = "OmniChurch PRO Official Icon",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "OmniChurch",
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
            )
            Spacer(modifier = Modifier.width(6.dp))
            OmniChurchProBadge()
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      PremiumProfileCard(
        memberName = displayName,
        subtitle = spaceSubtitle,
        photoUrl = memberPhotoUrl,
        donationConfig = donationConfig,
        currentLanguageCode = currentLanguageCode,
        onProfileClick = onOpenProfile,
        onDonateClick = onOpenDonate,
      )

      if (headerBackdropAlpha > 0.1f) {
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.12f), thickness = 0.8.dp)
      }
    }
  }
}

@Composable
internal fun MemberHomeCommentsSheet(
  targetId: String,
  isService: Boolean,
  observeComments: (targetId: String) -> Flow<List<AnnouncementComment>>,
  reactionSummaries: Map<String, TargetReactionSummary>,
  onPostComment: (targetId: String, isService: Boolean, content: String) -> Unit,
  onReactToComment: (commentId: String, reactionType: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val comments by
    observeComments(targetId).collectAsStateWithLifecycle(initialValue = emptyList())
  var commentInput by remember { mutableStateOf("") }
  var activeReactionCommentId by remember { mutableStateOf<String?>(null) }
  val haptic = LocalHapticFeedback.current

  GlassModalBottomSheet(
    onDismissRequest = onDismiss,
    title = stringResource(R.string.feed_comments_title, comments.size),
  ) {
    if (comments.isEmpty()) {
      Text(
        text = stringResource(R.string.feed_no_comments),
        style = MaterialTheme.typography.bodyMedium,
        color = MutedGlassText,
        modifier = Modifier.padding(vertical = 16.dp),
      )
    } else {
      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 310.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(comments, key = { it.id }) { comment ->
          val commentReactionSummary = reactionSummaries[comment.id]
          val isSelectorOpen = activeReactionCommentId == comment.id

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x660B1D3D))
                .border(0.8.dp, GlassWhite.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                .padding(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f),
              ) {
                MemberAvatar3D(
                  fullName = comment.userName,
                  size = 34.dp,
                  photoUrl = comment.userPhotoUrl,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = comment.userName,
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandGold,
                  )
                  Text(
                    text = comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PureWhite,
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Stacked3DReactionCluster(
                reactionSummary = commentReactionSummary,
                fallbackCount = (commentReactionSummary?.totalCount ?: 0).toLong(),
                isCompact = true,
                onTriggerClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  activeReactionCommentId =
                    if (isSelectorOpen) null else comment.id
                },
                testTag = "comment_react_btn_${comment.id}",
              )
            }

            if (isSelectorOpen) {
              Spacer(modifier = Modifier.height(8.dp))
              Floating3DReactionSelectorBar(
                visible = true,
                selectedReaction = commentReactionSummary?.currentUserReaction,
                onSelectReaction = { chosenReaction ->
                  onReactToComment(comment.id, chosenReaction.key)
                  activeReactionCommentId = null
                },
                onDismiss = { activeReactionCommentId = null },
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      GlassInputField(
        value = commentInput,
        onValueChange = { commentInput = it },
        label = stringResource(R.string.feed_comment_hint),
        modifier = Modifier.weight(1f),
        testTag = "comment_input_field",
      )
      IconButton(
        onClick = {
          if (commentInput.isNotBlank()) {
            onPostComment(targetId, isService, commentInput)
            commentInput = ""
          }
        },
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(ElectricBlue)
            .testTag("comment_send_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = stringResource(R.string.feed_comment_send),
          tint = PureWhite,
        )
      }
    }
  }
}

@Composable
internal fun HeroSanctuaryBannerCard(churchName: String?) {
  val resolvedChurchTitle =
    churchName?.trim()?.takeIf { it.isNotBlank() } ?: "OmniChurch"
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(top = 4.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
  ) {
    Text(
      text = stringResource(R.string.home_hero_welcome),
      style = MaterialTheme.typography.headlineMedium.copy(fontSize = 23.sp),
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text =
        buildAnnotatedString {
          withStyle(
            SpanStyle(
              brush = Brush.horizontalGradient(listOf(BrandGold, BrandOrange, BrandGold)),
              fontWeight = FontWeight.ExtraBold,
            )
          ) {
            append(resolvedChurchTitle)
          }
        },
      style = MaterialTheme.typography.displayMedium.copy(fontSize = 28.sp, lineHeight = 34.sp),
    )
  }
}

internal fun formatHomeServiceDateLabel(
  dateIso: String,
  startTime: String,
  endTime: String,
  languageCode: String = "ht",
): String {
  val start12 = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(startTime)
  val end12 = com.zoutiw.omnichurch.data.models.formatServiceClockAmPm(endTime)
  val timeSpan =
    when {
      start12.isNotBlank() && end12.isNotBlank() && !end12.equals(start12, ignoreCase = true) -> "$start12 - $end12"
      start12.isNotBlank() -> start12
      else -> ""
    }
  val naturalDate = com.zoutiw.omnichurch.data.models.formatLocalizedServiceDate(dateIso, languageCode).ifBlank {
    formatCreoleDateWithDay(dateIso)
  }
  return if (timeSpan.isNotBlank()) "$naturalDate • $timeSpan" else naturalDate
}
