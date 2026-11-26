package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

/**
 * 1. GlassPanel — Translucent 3D glass container with layered depth, specular top edge highlight,
 *    and ambient glow.
 */
@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 22.dp,
  borderColors: List<Color> =
    listOf(
      GlassWhite.copy(alpha = 0.45f),
      ElectricBlue.copy(alpha = 0.25f),
      GlassWhite.copy(alpha = 0.12f),
    ),
  backgroundBrush: Brush = OmniBrushes.GlassCardBrush,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  Box(
    modifier =
      modifier
        .shadow(
          elevation = 12.dp,
          shape = shape,
          ambientColor = ElectricBlue.copy(alpha = 0.28f),
          spotColor = DeepNavy,
        )
        .clip(shape)
        .background(backgroundBrush)
        .border(
          width = 1.2.dp,
          brush = Brush.linearGradient(borderColors),
          shape = shape,
        )
        .drawBehind {
          // Specular top edge reflection line
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  PureWhite.copy(alpha = 0.45f),
                  BlueNeon.copy(alpha = 0.40f),
                  Color.Transparent,
                )
              ),
            start = Offset(size.width * 0.12f, 1.dp.toPx()),
            end = Offset(size.width * 0.88f, 1.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
          )
        }
        .padding(contentPadding),
    content = content,
  )
}

/**
 * 2. PremiumGlassCard — Interactive 3D glass card with smooth press scale feedback, specular top
 *    highlight, and optional neon illumination.
 */
@Composable
fun PremiumGlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 20.dp,
  isHighlighted: Boolean = false,
  accentColor: Color = BlueNeon,
  onClick: (() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(16.dp),
  content: @Composable ColumnScope.() -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed && onClick != null) 0.975f else 1f,
      animationSpec = tween(durationMillis = 120),
      label = "card_press_scale",
    )

  val shape = RoundedCornerShape(cornerRadius)
  val borderBrush =
    if (isHighlighted) {
      Brush.linearGradient(
        listOf(PureWhite.copy(alpha = 0.90f), accentColor, ElectricBlue.copy(alpha = 0.75f))
      )
    } else {
      Brush.linearGradient(
        listOf(
          GlassWhite.copy(alpha = 0.42f),
          ElectricBlue.copy(alpha = 0.24f),
          Color(0x1FDCEBFF),
        )
      )
    }

  val bgBrush =
    if (isHighlighted) {
      Brush.verticalGradient(
        listOf(
          Color(0xFF1AA8FF).copy(alpha = 0.75f),
          ElectricBlue.copy(alpha = 0.65f),
          RoyalBlue.copy(alpha = 0.88f),
        )
      )
    } else {
      Brush.verticalGradient(
        listOf(
          Color(0xD9152E5E),
          Color(0xE60B1A38),
          Color(0xF2061024),
        )
      )
    }

  val clickableMod =
    if (onClick != null) {
      Modifier.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
      )
    } else {
      Modifier
    }

  Column(
    modifier =
      modifier
        .scale(scale)
        .shadow(
          elevation = if (isHighlighted) 18.dp else 10.dp,
          shape = shape,
          ambientColor = if (isHighlighted) accentColor else ElectricBlue,
          spotColor = if (isHighlighted) accentColor else DeepNavy,
        )
        .clip(shape)
        .background(bgBrush)
        .border(width = if (isHighlighted) 1.7.dp else 1.1.dp, brush = borderBrush, shape = shape)
        .drawBehind {
          // Physical top-edge specular reflection
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  PureWhite.copy(alpha = if (isHighlighted) 0.75f else 0.32f),
                  accentColor.copy(alpha = if (isHighlighted) 0.60f else 0.22f),
                  Color.Transparent,
                )
              ),
            start = Offset(size.width * 0.14f, 1.dp.toPx()),
            end = Offset(size.width * 0.86f, 1.dp.toPx()),
            strokeWidth = 1.6.dp.toPx(),
          )
        }
        .then(clickableMod)
        .padding(contentPadding),
    content = content,
  )
}

/** 3. GlassTopBar — Sleek 3D top navigation bar matching the middle and right reference screens. */
@Composable
fun GlassTopBar(
  title: String,
  onBackClick: (() -> Unit)? = null,
  rightBadgeText: String? = null,
  onMenuClick: (() -> Unit)? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f),
    ) {
      if (onBackClick != null) {
        IconButton(
          onClick = onBackClick,
          modifier = Modifier.size(48.dp).testTag("top_bar_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.common_close),
            tint = PureWhite,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (trailingContent != null) {
      trailingContent()
    } else if (rightBadgeText != null) {
      Row(
        modifier =
          Modifier.shadow(8.dp, RoundedCornerShape(50), ambientColor = BrandOrange)
            .clip(RoundedCornerShape(50))
            .background(
              Brush.horizontalGradient(
                listOf(Color(0xFF6B4209), Color(0xFF9E630D), Color(0xFF523206))
              )
            )
            .border(
              1.3.dp,
              Brush.horizontalGradient(listOf(PureWhite.copy(alpha = 0.7f), BrandGold, BrandOrange)),
              RoundedCornerShape(50),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "👑 ",
          fontSize = 13.sp,
        )
        Text(
          text = rightBadgeText,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    } else if (onMenuClick != null) {
      IconButton(
        onClick = onMenuClick,
        modifier = Modifier.size(48.dp).testTag("top_bar_menu_button"),
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = stringResource(R.string.profile_screen_title),
          tint = PureWhite,
        )
      }
    }
  }
}
