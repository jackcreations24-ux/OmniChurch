package com.zoutiw.omnichurch.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue

private val NeumorphicTextShadow =
  Shadow(
    color = Color(0xEE010612),
    offset = Offset(2f, 4f),
    blurRadius = 10f,
  )

private val GlassButtonShape = RoundedCornerShape(18.dp)
private val DisabledButtonBgBrush =
  Brush.verticalGradient(listOf(Color(0x55182C4D), Color(0x550C182B)))
private val GoldButtonBgBrush =
  Brush.verticalGradient(listOf(BrandGold, BrandOrange, Color(0xFFC86400)))
private val PrimaryNeonButtonBgBrush =
  Brush.verticalGradient(listOf(Color(0xFF28B8FF), ElectricBlue, RoyalBlue))
private val SecondaryButtonBgBrush =
  Brush.verticalGradient(listOf(Color(0xCC19366B), Color(0xCC0D1F42), Color(0xE6071329)))

private val GoldButtonBorderBrush =
  Brush.linearGradient(listOf(PureWhite, BrandGold, BrandOrange))
private val PrimaryNeonButtonBorderBrush =
  Brush.linearGradient(listOf(PureWhite, BlueNeon, ElectricBlue))
private val SecondaryButtonBorderBrush =
  Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.5f), ElectricBlue.copy(alpha = 0.25f)))

private val PrimaryButtonSpecularBrush =
  Brush.horizontalGradient(
    listOf(
      Color.Transparent,
      PureWhite.copy(alpha = 0.7f),
      Color.Transparent,
    )
  )

private val SecondaryButtonSpecularBrush =
  Brush.horizontalGradient(
    listOf(
      Color.Transparent,
      PureWhite.copy(alpha = 0.35f),
      Color.Transparent,
    )
  )

private val NeonIconDefaultBgBrush =
  Brush.verticalGradient(listOf(Color(0xD920427E), Color(0xE60B1C3D)))
private val NeonIconBorderBrush =
  Brush.linearGradient(
    listOf(
      PureWhite.copy(alpha = 0.65f),
      BlueNeon.copy(alpha = 0.40f),
    )
  )
private val NotificationBadgeBgBrush =
  Brush.verticalGradient(listOf(Color(0xFFFF5E73), ErrorRed))

/**
 * 4a. Neumorphic3DActionButton — 3D Neumorphic action button with a 100% transparent background
 *     and no border ("bg transparan, san bodi"), featuring a glowing 3D icon medallion and extruded text.
 */
@Composable
fun Neumorphic3DActionButton(
  text: String,
  icon: ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "neumorphic_3d_action_button",
) {
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.95f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
      label = "neumorphic_scale",
    )

  Box(
    modifier =
      modifier
        .scale(scale)
        .background(Color.Transparent)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
          },
        )
        .padding(horizontal = 16.dp, vertical = 10.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      // Borderless 3D Neumorphic Sculpted Medallion for the Icon with 100% transparent base
      Box(
        modifier =
          Modifier.size(46.dp).drawBehind {
            val r = size.minDimension / 2f
            // Deep subtle ambient 3D shadow under the medallion
            drawCircle(
              color = Color(0x6001050E),
              radius = r * 0.92f,
              center = Offset(center.x + 2.dp.toPx(), center.y + 3.dp.toPx()),
            )
            // Top-left light specular reflection
            drawCircle(
              color = Color(0x3560C6FF),
              radius = r * 0.90f,
              center = Offset(center.x - 1.5.dp.toPx(), center.y - 1.5.dp.toPx()),
            )
            // Ambient transparent glow
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      Color(0x3038A8FF),
                      Color.Transparent,
                    ),
                  center = center,
                  radius = r * 1.05f,
                ),
              radius = r,
            )
          },
        contentAlignment = Alignment.Center,
      ) {
        // 3D Extruded Shadow Icon Layer
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = Color(0xCC010612),
          modifier = Modifier.size(28.dp).offset(x = 1.5.dp, y = 2.dp),
        )
        // Foreground Lit 3D Icon Layer
        Icon(
          imageVector = icon,
          contentDescription = text,
          tint = PureWhite,
          modifier = Modifier.size(28.dp),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Text(
        text = text,
        style =
          MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.sp,
            shadow = NeumorphicTextShadow,
          ),
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** 4b. GlassButton — 3D realistic bevelled button with neon or metallic lighting. */
@Composable
fun GlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  subtitle: String? = null,
  icon: ImageVector? = null,
  isPrimaryNeon: Boolean = true,
  isGoldAccent: Boolean = false,
  isLoading: Boolean = false,
  accentColor: Color? = null,
  enabled: Boolean = true,
  testTag: String = "glass_button",
) {
  val effectiveEnabled = enabled && !isLoading
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (isPressed && effectiveEnabled) 0.96f else 1f,
      animationSpec = tween(100),
      label = "btn_scale",
    )

  val customAccentBgBrush =
    remember(accentColor) {
      accentColor?.let {
        Brush.verticalGradient(
          listOf(it.copy(alpha = 0.45f), Color(0xCC0D1F42), Color(0xE6071329))
        )
      }
    }
  val customAccentBorderBrush =
    remember(accentColor) {
      accentColor?.let {
        Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.7f), it))
      }
    }

  val bgBrush =
    when {
      !effectiveEnabled -> DisabledButtonBgBrush
      isGoldAccent -> GoldButtonBgBrush
      customAccentBgBrush != null -> customAccentBgBrush
      isPrimaryNeon -> PrimaryNeonButtonBgBrush
      else -> SecondaryButtonBgBrush
    }

  val borderBrush =
    when {
      isGoldAccent -> GoldButtonBorderBrush
      customAccentBorderBrush != null -> customAccentBorderBrush
      isPrimaryNeon -> PrimaryNeonButtonBorderBrush
      else -> SecondaryButtonBorderBrush
    }

  val specularBrush =
    if (isPrimaryNeon) PrimaryButtonSpecularBrush else SecondaryButtonSpecularBrush

  Box(
    modifier =
      modifier
        .scale(scale)
        .shadow(
          elevation = if (isPrimaryNeon || isGoldAccent) 14.dp else 8.dp,
          shape = GlassButtonShape,
          ambientColor = accentColor ?: if (isGoldAccent) BrandOrange else BlueNeon,
          spotColor = accentColor ?: if (isGoldAccent) BrandGold else ElectricBlue,
        )
        .clip(GlassButtonShape)
        .background(bgBrush)
        .border(1.3.dp, borderBrush, GlassButtonShape)
        .drawBehind {
          drawLine(
            brush = specularBrush,
            start = Offset(size.width * 0.15f, 1.dp.toPx()),
            end = Offset(size.width * 0.85f, 1.dp.toPx()),
            strokeWidth = 1.5.dp.toPx(),
          )
        }
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          enabled = effectiveEnabled,
          onClick = onClick,
        )
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    if (isLoading) {
      CircularProgressIndicator(
        color = PureWhite,
        strokeWidth = 2.dp,
        modifier = Modifier.size(20.dp),
      )
    } else if (subtitle != null) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = text,
            tint = PureWhite,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = GlassWhite.copy(alpha = 0.85f),
          textAlign = TextAlign.Center,
        )
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
      ) {
        if (icon != null) {
          Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (isGoldAccent) DeepNavy else PureWhite,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
          text = text,
          style = MaterialTheme.typography.titleSmall,
          color = if (isGoldAccent) DeepNavy else PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/** 5. NeonIconButton — Circular 3D glass icon button with unread notification badge support. */
@Composable
fun NeonIconButton(
  icon: ImageVector,
  contentDescription: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  badgeCount: Int = 0,
  isHighlighted: Boolean = false,
  testTag: String = "neon_icon_button",
) {
  Box(modifier = modifier) {
    Box(
      modifier =
        Modifier.size(48.dp)
          .shadow(8.dp, CircleShape, ambientColor = ElectricBlue)
          .clip(CircleShape)
          .background(
            if (isHighlighted) OmniBrushes.ActiveBlueNeonBrush
            else NeonIconDefaultBgBrush
          )
          .border(
            1.3.dp,
            NeonIconBorderBrush,
            CircleShape,
          )
          .clickable(onClick = onClick)
          .testTag(testTag),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = PureWhite,
        modifier = Modifier.size(22.dp),
      )
    }

    if (badgeCount > 0) {
      GlassNotificationBadge(
        count = badgeCount,
        modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp),
      )
    }
  }
}

/** 6. GlassNotificationBadge — Glowing crimson pill badge for unread alerts. */
@Composable
fun GlassNotificationBadge(count: Int, modifier: Modifier = Modifier) {
  if (count <= 0) return
  Box(
    modifier =
      modifier
        .size(20.dp)
        .shadow(6.dp, CircleShape, ambientColor = ErrorRed)
        .clip(CircleShape)
        .background(NotificationBadgeBgBrush)
        .border(1.2.dp, PureWhite, CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = if (count > 99) "99+" else count.toString(),
      color = PureWhite,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}

