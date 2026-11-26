package com.zoutiw.omnichurch.presentation.screens.services

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.AnnouncementComment
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.presentation.components.Floating3DReactionSelectorBar
import com.zoutiw.omnichurch.presentation.components.GlassInputField
import com.zoutiw.omnichurch.presentation.components.GlassModalBottomSheet
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.presentation.components.Stacked3DReactionCluster
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.flow.Flow

@Composable
internal fun FeedCommentsBottomSheet(
  targetId: String,
  observeComments: (targetId: String) -> Flow<List<AnnouncementComment>>,
  reactionSummaries: Map<String, TargetReactionSummary>,
  onPostComment: (targetId: String, content: String) -> Unit,
  onReactToComment: (commentId: String, reactionType: String) -> Unit,
  onDismiss: () -> Unit,
  testTagPrefix: String,
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
                testTag = "${testTagPrefix}_react_btn_${comment.id}",
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
        testTag = "${testTagPrefix}_input_field",
      )
      IconButton(
        onClick = {
          if (commentInput.isNotBlank()) {
            onPostComment(targetId, commentInput)
            commentInput = ""
          }
        },
        modifier =
          Modifier.size(48.dp)
            .clip(CircleShape)
            .background(ElectricBlue)
            .testTag("${testTagPrefix}_send_button"),
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
