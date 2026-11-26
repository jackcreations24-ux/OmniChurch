package com.zoutiw.omnichurch.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Maps each [OmniReactionType] to its official transparent 3D realistic PNG drawable asset.
 */
@DrawableRes
fun OmniReactionType.toDrawableRes(): Int =
  when (this) {
    OmniReactionType.LIKE -> R.drawable.reaction_like
    OmniReactionType.LOVE -> R.drawable.reaction_love
    OmniReactionType.PRAY -> R.drawable.reaction_pray
    OmniReactionType.HAHA -> R.drawable.reaction_haha
    OmniReactionType.WOW -> R.drawable.reaction_wow
    OmniReactionType.SAD -> R.drawable.reaction_sad
    OmniReactionType.CLAP -> R.drawable.reaction_clap
  }

/**
 * Renders an official 3D realistic reaction icon preserving full transparency and proportions.
 */
@Composable
fun Omni3DReactionIcon(
  reactionType: OmniReactionType,
  size: Dp = 26.dp,
  modifier: Modifier = Modifier,
) {
  Image(
    painter = painterResource(id = reactionType.toDrawableRes()),
    contentDescription = reactionType.labelHt,
    contentScale = ContentScale.Fit,
    modifier = modifier.size(size),
  )
}

/**
 * Floating 3D Glass Reaction Selector Bar containing all 7 Ultra-Premium 3D Reactions:
 * reaction_like, reaction_love, reaction_pray, reaction_haha, reaction_wow, reaction_sad, reaction_clap.
 */
@Composable
fun Floating3DReactionSelectorBar(
  visible: Boolean,
  selectedReaction: OmniReactionType?,
  onSelectReaction: (OmniReactionType) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()

  AnimatedVisibility(
    visible = visible,
    enter =
      scaleIn(
        initialScale = 0.72f,
        animationSpec =
          spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
          ),
      ) + fadeIn(animationSpec = tween(durationMillis = 160)),
    exit =
      scaleOut(targetScale = 0.80f, animationSpec = tween(durationMillis = 130)) +
        fadeOut(animationSpec = tween(durationMillis = 130)),
    modifier = modifier,
  ) {
    val barShape = RoundedCornerShape(50)
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(bottom = 8.dp)
          .shadow(
            elevation = 20.dp,
            shape = barShape,
            ambientColor = BlueNeon,
            spotColor = BrandGold,
          )
          .clip(barShape)
          .background(
            Brush.verticalGradient(
              listOf(
                Color(0xF50E2654),
                Color(0xFA071532),
                Color(0xFC040B1D),
              )
            )
          )
          .border(
            width = 1.4.dp,
            brush =
              Brush.linearGradient(
                listOf(
                  PureWhite.copy(alpha = 0.80f),
                  BlueNeon.copy(alpha = 0.85f),
                  BrandGold.copy(alpha = 0.65f),
                  ElectricBlue.copy(alpha = 0.75f),
                )
              ),
            shape = barShape,
          )
          .drawBehind {
            drawLine(
              brush =
                Brush.horizontalGradient(
                  listOf(
                    Color.Transparent,
                    PureWhite.copy(alpha = 0.65f),
                    BlueNeon.copy(alpha = 0.55f),
                    BrandGold.copy(alpha = 0.45f),
                    Color.Transparent,
                  )
                ),
              start = Offset(size.width * 0.10f, 1.2.dp.toPx()),
              end = Offset(size.width * 0.90f, 1.2.dp.toPx()),
              strokeWidth = 1.6.dp.toPx(),
            )
          }
          .padding(horizontal = 8.dp, vertical = 8.dp)
          .testTag("floating_reaction_selector_bar"),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OmniReactionType.entries.forEachIndexed { index, reactionType ->
        val isSelected = selectedReaction == reactionType
        val entranceAnim = remember { Animatable(0f) }
        val tapPulseAnim = remember { Animatable(1f) }
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(Unit) {
          delay(index * 24L)
          entranceAnim.animateTo(
            targetValue = 1f,
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
              ),
          )
        }

        val pressScale by
          animateFloatAsState(
            targetValue =
              when {
                isPressed -> 1.28f
                isSelected -> 1.15f
                else -> 1f
              },
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
              ),
            label = "reaction_item_scale_${reactionType.key}",
          )

        val liftOffset by
          animateFloatAsState(
            targetValue =
              when {
                isPressed -> -10f
                isSelected -> -5f
                else -> 0f
              },
            animationSpec =
              spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
              ),
            label = "reaction_item_lift_${reactionType.key}",
          )

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.graphicsLayer {
                val combinedScale = entranceAnim.value * pressScale * tapPulseAnim.value
                scaleX = combinedScale
                scaleY = combinedScale
                translationY = liftOffset
                alpha = entranceAnim.value.coerceIn(0f, 1f)
              }
              .clip(RoundedCornerShape(16.dp))
              .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  scope.launch {
                    tapPulseAnim.animateTo(1.32f, tween(75))
                    tapPulseAnim.animateTo(
                      1f,
                      spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    )
                    onSelectReaction(reactionType)
                    onDismiss()
                  }
                },
              )
              .padding(horizontal = 2.dp, vertical = 2.dp)
              .testTag("reaction_option_${reactionType.key}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(38.dp)
                .then(
                  if (isSelected) {
                    Modifier.shadow(10.dp, CircleShape, ambientColor = BrandGold)
                      .clip(CircleShape)
                      .background(
                        Brush.radialGradient(
                          listOf(
                            BrandGold.copy(alpha = 0.35f),
                            BlueNeon.copy(alpha = 0.22f),
                            Color.Transparent,
                          )
                        )
                      )
                      .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(listOf(BrandGold, BlueNeon)),
                        shape = CircleShape,
                      )
                  } else {
                    Modifier
                  }
                ),
          ) {
            Omni3DReactionIcon(
              reactionType = reactionType,
              size = 33.dp,
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = reactionType.labelHt,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = if (isSelected) BrandGold else GlassWhite.copy(alpha = 0.85f),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
            maxLines = 1,
          )
        }
      }
    }
  }
}

/**
 * Stacked 3D Reaction Cluster & Interactive Trigger Pill used on Feed Cards, Announcements, and Comments.
 */
@Composable
fun Stacked3DReactionCluster(
  reactionSummary: TargetReactionSummary?,
  fallbackCount: Long,
  onTriggerClick: () -> Unit,
  modifier: Modifier = Modifier,
  showLabelWhenUnselected: String? = null,
  isCompact: Boolean = false,
  testTag: String = "reaction_cluster_btn",
) {
  val currentUserReaction = reactionSummary?.currentUserReaction
  val effectiveCount =
    maxOf(fallbackCount, (reactionSummary?.totalCount ?: 0).toLong())
      .let { count ->
        if (currentUserReaction != null && count == 0L) 1L else count
      }

  val displayedTypes =
    remember(reactionSummary, effectiveCount, currentUserReaction) {
      val top = reactionSummary?.topReactions.orEmpty()
      when {
        top.isNotEmpty() -> top.take(3)
        currentUserReaction != null -> listOf(currentUserReaction)
        effectiveCount > 0L ->
          listOf(OmniReactionType.LOVE, OmniReactionType.LIKE, OmniReactionType.PRAY)
        else -> listOf(OmniReactionType.LIKE, OmniReactionType.LOVE, OmniReactionType.PRAY)
      }
    }

  val isHighlighted = currentUserReaction != null
  val iconSize = if (isCompact) 20.dp else 24.dp
  val pillShape = RoundedCornerShape(50)

  val pressInteraction = remember { MutableInteractionSource() }
  val isPressed by pressInteraction.collectIsPressedAsState()
  val buttonScale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.94f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "cluster_btn_scale",
    )

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .scale(buttonScale)
        .clip(pillShape)
        .background(
          if (isHighlighted) {
            Brush.horizontalGradient(
              listOf(
                ElectricBlue.copy(alpha = 0.32f),
                BrandOrange.copy(alpha = 0.24f),
              )
            )
          } else {
            Brush.horizontalGradient(
              listOf(
                Color(0x550D2249),
                Color(0x33081630),
              )
            )
          }
        )
        .border(
          width = if (isHighlighted) 1.2.dp else 0.8.dp,
          brush =
            if (isHighlighted) {
              Brush.linearGradient(listOf(BrandGold, BlueNeon))
            } else {
              Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.26f), ElectricBlue.copy(alpha = 0.20f)))
            },
          shape = pillShape,
        )
        .clickable(
          interactionSource = pressInteraction,
          indication = null,
          onClick = onTriggerClick,
        )
        .padding(
          horizontal = if (isCompact) 8.dp else 10.dp,
          vertical = if (isCompact) 4.dp else 5.dp,
        )
        .testTag(testTag),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy((-5).dp),
    ) {
      displayedTypes.forEach { rType ->
        Omni3DReactionIcon(
          reactionType = rType,
          size = iconSize,
        )
      }
    }

    Spacer(modifier = Modifier.width(7.dp))

    AnimatedContent(
      targetState = effectiveCount,
      transitionSpec = {
        (scaleIn(initialScale = 0.8f) + fadeIn(tween(150))) togetherWith
          (scaleOut(targetScale = 0.8f) + fadeOut(tween(120)))
      },
      label = "reaction_count_anim",
    ) { countValue ->
      val labelSuffix =
        when {
          currentUserReaction != null -> " • ${currentUserReaction.labelHt}"
          !showLabelWhenUnselected.isNullOrBlank() -> " $showLabelWhenUnselected"
          else -> ""
        }
      Text(
        text = "$countValue$labelSuffix",
        style =
          if (isCompact) MaterialTheme.typography.labelMedium
          else MaterialTheme.typography.labelLarge,
        color = if (isHighlighted) BrandGold else PureWhite,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}
