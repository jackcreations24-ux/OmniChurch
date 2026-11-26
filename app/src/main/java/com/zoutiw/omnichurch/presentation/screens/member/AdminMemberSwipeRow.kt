package com.zoutiw.omnichurch.presentation.screens.member

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.presentation.components.MemberAvatar3D
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.MidnightNavy
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.SuccessGreen
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Borderless, 100% opaque swipeable member row with a pro left-swipe tray revealing
 * single centered quick actions (WhatsApp Message, Call, Copy ID) with no colored backgrounds
 * and zero duplicate buttons.
 */
@Composable
internal fun SwipeableMemberRow(
  member: Member,
  isSwipedOpen: Boolean,
  onSwipeStateChange: (Boolean) -> Unit,
  onDragWillStart: () -> Unit,
  onMessage: () -> Unit,
  onCall: () -> Unit,
  onCopyCode: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current

  val trayWidthDp = 180.dp
  val trayWidthPx = remember(density) { with(density) { trayWidthDp.toPx() } }
  val offsetX = remember { Animatable(0f) }

  val settleSpec = remember {
    spring<Float>(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow,
    )
  }

  LaunchedEffect(isSwipedOpen, trayWidthPx) {
    val target = if (isSwipedOpen) -trayWidthPx else 0f
    if (kotlin.math.abs(offsetX.value - target) > 1f) {
      offsetX.animateTo(target, settleSpec)
    }
  }

  val trayRevealFraction = (-offsetX.value / trayWidthPx).coerceIn(0f, 1f)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(DeepNavy)
        .testTag("admin_member_card_${member.id}"),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .clipToBounds()
          .background(DeepNavy),
    ) {
      // Background Quick-Action Tray revealed cleanly on swipe left (opaque, centered, no colored button BGs)
      if (offsetX.value < -2f) {
        Box(
          modifier =
            Modifier
              .matchParentSize()
              .background(DeepNavy),
          contentAlignment = Alignment.CenterEnd,
        ) {
          Row(
            modifier =
              Modifier
                .width(trayWidthDp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp)
                .alpha(trayRevealFraction),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            SwipeTrayActionButton(
              icon = Icons.AutoMirrored.Filled.Message,
              label = "Mesaj",
              iconTint = SuccessGreen,
              contentDescription = "Voye mesaj WhatsApp",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onMessage()
              },
              modifier = Modifier.testTag("swipe_action_msg_${member.id}"),
            )

            SwipeTrayActionButton(
              icon = Icons.Default.Call,
              label = "Rele",
              iconTint = BlueNeon,
              contentDescription = "Rele",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onCall()
              },
              modifier = Modifier.testTag("swipe_action_call_${member.id}"),
            )

            SwipeTrayActionButton(
              icon = Icons.Default.ContentCopy,
              label = "Kopye",
              iconTint = BrandGold,
              contentDescription = "Kopye ID",
              enabled = trayRevealFraction > 0.25f,
              onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
                onCopyCode()
              },
              modifier = Modifier.testTag("swipe_action_copy_${member.id}"),
            )
          }
        }
      }

      // Foreground 100% Opaque Borderless Row that slides horizontally on drag
      Row(
        modifier =
          Modifier
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .fillMaxWidth()
            .background(PrimaryNavy)
            .draggable(
              orientation = Orientation.Horizontal,
              state =
                rememberDraggableState { delta ->
                  scope.launch {
                    val newOffset = (offsetX.value + delta).coerceIn(-trayWidthPx, 0f)
                    offsetX.snapTo(newOffset)
                  }
                },
              onDragStarted = {
                onDragWillStart()
              },
              onDragStopped = { velocity ->
                scope.launch {
                  val shouldOpen =
                    when {
                      velocity < -450f -> true
                      velocity > 450f -> false
                      else -> offsetX.value < -trayWidthPx * 0.42f
                    }
                  if (shouldOpen) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSwipeStateChange(true)
                    offsetX.animateTo(-trayWidthPx, settleSpec)
                  } else {
                    onSwipeStateChange(false)
                    offsetX.animateTo(0f, settleSpec)
                  }
                }
              },
            )
            .clickable {
              val currentlyOpen = offsetX.value < -trayWidthPx * 0.2f
              if (currentlyOpen) {
                onSwipeStateChange(false)
                scope.launch { offsetX.animateTo(0f, settleSpec) }
              } else {
                onSwipeStateChange(true)
                scope.launch { offsetX.animateTo(-trayWidthPx, settleSpec) }
              }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 46.dp,
          photoUrl = member.effectivePhotoUrl,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (!member.titlePrefix.isNullOrBlank()) {
              Text(
                text = "${member.titlePrefix} ",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = BrandGold,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = member.effectiveFullName,
              style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "ID: ${member.effectiveMatrikil}",
              style = MaterialTheme.typography.bodySmall,
              color = BrandGold,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = " • ${member.department.ifBlank { "Adorasyon" }}",
              style = MaterialTheme.typography.bodySmall,
              color = GlassWhite.copy(alpha = 0.85f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (!member.phone.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = member.phone.trim(),
              style = MaterialTheme.typography.labelSmall,
              color = MutedGlassText,
            )
          }
        }
      }
    }

    HorizontalDivider(
      color = MidnightNavy,
      thickness = 0.7.dp,
      modifier = Modifier.padding(start = 74.dp, end = 16.dp),
    )
  }
}

@Composable
private fun SwipeTrayActionButton(
  icon: ImageVector,
  label: String,
  iconTint: Color,
  contentDescription: String,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .size(width = 52.dp, height = 56.dp)
        .clip(RoundedCornerShape(10.dp))
        .clickable(enabled = enabled, onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = iconTint,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = PureWhite,
      fontWeight = FontWeight.Medium,
      maxLines = 1,
      softWrap = false,
      textAlign = TextAlign.Center,
    )
  }
}
