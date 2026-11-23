package com.zoutiw.omnichurch.presentation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.utilities.ImageCompressor
import com.zoutiw.omnichurch.data.models.Announcement
import com.zoutiw.omnichurch.data.models.ChurchService
import com.zoutiw.omnichurch.data.models.Member
import com.zoutiw.omnichurch.data.models.OmniReactionType
import com.zoutiw.omnichurch.data.models.TargetReactionSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.zoutiw.omnichurch.ui.theme.BlueNeon
import com.zoutiw.omnichurch.ui.theme.BrandGold
import com.zoutiw.omnichurch.ui.theme.BrandOrange
import com.zoutiw.omnichurch.ui.theme.DeepNavy
import com.zoutiw.omnichurch.ui.theme.ElectricBlue
import com.zoutiw.omnichurch.ui.theme.ErrorRed
import com.zoutiw.omnichurch.ui.theme.GlassWhite
import com.zoutiw.omnichurch.ui.theme.LiveBadgeRed
import com.zoutiw.omnichurch.ui.theme.MutedGlassText
import com.zoutiw.omnichurch.ui.theme.OmniBrushes
import com.zoutiw.omnichurch.ui.theme.PrimaryNavy
import com.zoutiw.omnichurch.ui.theme.PureWhite
import com.zoutiw.omnichurch.ui.theme.RoyalBlue
import com.zoutiw.omnichurch.ui.theme.SkyBlue
import com.zoutiw.omnichurch.ui.theme.SuccessGreen

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
            shadow =
              Shadow(
                color = Color(0xEE010612),
                offset = Offset(2f, 4f),
                blurRadius = 10f,
              ),
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

  val shape = RoundedCornerShape(18.dp)
  val bgBrush =
    when {
      !effectiveEnabled ->
        Brush.verticalGradient(listOf(Color(0x55182C4D), Color(0x550C182B)))
      isGoldAccent ->
        Brush.verticalGradient(listOf(BrandGold, BrandOrange, Color(0xFFC86400)))
      accentColor != null ->
        Brush.verticalGradient(
          listOf(accentColor.copy(alpha = 0.45f), Color(0xCC0D1F42), Color(0xE6071329))
        )
      isPrimaryNeon ->
        Brush.verticalGradient(
          listOf(Color(0xFF28B8FF), ElectricBlue, RoyalBlue)
        )
      else ->
        Brush.verticalGradient(
          listOf(Color(0xCC19366B), Color(0xCC0D1F42), Color(0xE6071329))
        )
    }

  val borderBrush =
    when {
      isGoldAccent -> Brush.linearGradient(listOf(PureWhite, BrandGold, BrandOrange))
      accentColor != null -> Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.7f), accentColor))
      isPrimaryNeon -> Brush.linearGradient(listOf(PureWhite, BlueNeon, ElectricBlue))
      else -> Brush.linearGradient(listOf(GlassWhite.copy(alpha = 0.5f), ElectricBlue.copy(alpha = 0.25f)))
    }

  Box(
    modifier =
      modifier
        .scale(scale)
        .shadow(
          elevation = if (isPrimaryNeon || isGoldAccent) 14.dp else 8.dp,
          shape = shape,
          ambientColor = accentColor ?: if (isGoldAccent) BrandOrange else BlueNeon,
          spotColor = accentColor ?: if (isGoldAccent) BrandGold else ElectricBlue,
        )
        .clip(shape)
        .background(bgBrush)
        .border(1.3.dp, borderBrush, shape)
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(
                  Color.Transparent,
                  PureWhite.copy(alpha = if (isPrimaryNeon) 0.7f else 0.35f),
                  Color.Transparent,
                )
              ),
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
            else
              Brush.verticalGradient(
                listOf(Color(0xD920427E), Color(0xE60B1C3D))
              )
          )
          .border(
            1.3.dp,
            Brush.linearGradient(
              listOf(
                PureWhite.copy(alpha = 0.65f),
                BlueNeon.copy(alpha = 0.40f),
              )
            ),
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
        .background(Brush.verticalGradient(listOf(Color(0xFFFF5E73), ErrorRed)))
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

/** 7. GlassInputField — Styled translucent text field with neon focus rim. */
@Composable
fun GlassInputField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  placeholder: String = "",
  leadingIcon: ImageVector? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  keyboardType: KeyboardType? = null,
  singleLine: Boolean = true,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  testTag: String = "glass_input_field",
) {
  val effectiveKeyboardOptions =
    if (keyboardType != null) keyboardOptions.copy(keyboardType = keyboardType)
    else keyboardOptions

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label, color = GlassWhite.copy(alpha = 0.85f)) },
    placeholder =
      if (placeholder.isNotBlank()) {
        { Text(placeholder, color = MutedGlassText.copy(alpha = 0.6f)) }
      } else null,
    leadingIcon =
      if (leadingIcon != null) {
        {
          Icon(
            imageVector = leadingIcon,
            contentDescription = label,
            tint = BlueNeon,
            modifier = Modifier.size(20.dp),
          )
        }
      } else null,
    trailingIcon = trailingIcon,
    visualTransformation = visualTransformation,
    singleLine = singleLine,
    keyboardOptions = effectiveKeyboardOptions,
    shape = RoundedCornerShape(16.dp),
    colors =
      OutlinedTextFieldDefaults.colors(
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        focusedContainerColor = Color(0x880B1D3D),
        unfocusedContainerColor = Color(0x6607142B),
        focusedBorderColor = BlueNeon,
        unfocusedBorderColor = GlassWhite.copy(alpha = 0.30f),
        cursorColor = BlueNeon,
      ),
    modifier = modifier.fillMaxWidth().testTag(testTag),
  )
}

/** 8. GlassDialog — Modal 3D glass dialog. */

@Composable
fun GlassDialog(
  onDismissRequest: () -> Unit,
  title: String,
  content: @Composable ColumnScope.() -> Unit,
) {
  Dialog(onDismissRequest = onDismissRequest) {
    PremiumGlassCard(
      cornerRadius = 24.dp,
      isHighlighted = true,
      accentColor = ElectricBlue,
      contentPadding = PaddingValues(20.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = PureWhite,
        fontWeight = FontWeight.Bold,
      )
      Spacer(modifier = Modifier.height(14.dp))
      content()
    }
  }
}

/** 9. GlassModalBottomSheet — Translucent dark navy bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassModalBottomSheet(
  onDismissRequest: () -> Unit,
  title: String = "",
  content: @Composable ColumnScope.() -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = PrimaryNavy,
    contentColor = PureWhite,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 20.dp)
          .padding(bottom = 28.dp)
    ) {
      if (title.isNotBlank()) {
        Text(
          text = title,
          style = MaterialTheme.typography.headlineMedium,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
      }
      content()
    }
  }
}

/**
 * 10. PremiumProfileCard — Member header bar showing real member photo (or 3D avatar),
 *     personalized greeting ("Bonjou, Jackson 👋"), church role & notifications button with counter.
 */
@Composable
fun PremiumProfileCard(
  memberName: String,
  subtitle: String,
  unreadNotificationsCount: Int,
  onProfileClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  photoUrl: String? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier.weight(1f)
          .clip(RoundedCornerShape(50))
          .clickable(onClick = onProfileClick)
          .padding(vertical = 4.dp)
          .testTag("header_profile_row"),
    ) {
      MemberAvatar3D(
        fullName = memberName,
        size = 50.dp,
        photoUrl = photoUrl,
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = stringResource(R.string.home_greeting, memberName.substringBefore(" ")),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(text = " 👋", fontSize = 17.sp)
        }
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = GlassWhite.copy(alpha = 0.85f),
        )
      }
    }

    NeonIconButton(
      icon = Icons.Default.Notifications,
      contentDescription = stringResource(R.string.nav_notifications),
      badgeCount = unreadNotificationsCount,
      onClick = onNotificationsClick,
      testTag = "header_notifications_button",
    )
  }
}

/**
 * 11. PremiumSectionHeader — Reusable section header with bold white title on the left
 *     and golden-orange "Tout >" action on the right.
 */
@Composable
fun PremiumSectionHeader(
  title: String,
  actionText: String? = null,
  onActionClick: (() -> Unit)? = null,
  trailingExtraContent: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().padding(top = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.headlineMedium,
      color = PureWhite,
      fontWeight = FontWeight.Bold,
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
      if (trailingExtraContent != null) {
        trailingExtraContent()
      }
      if (actionText != null && onActionClick != null) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable(onClick = onActionClick)
              .padding(horizontal = 6.dp, vertical = 4.dp)
              .testTag("feed_see_all_button"),
        ) {
          Text(
            text = actionText,
            style = MaterialTheme.typography.titleSmall,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
          )
          Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

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

const val OMNICHURCH_OFFICIAL_SHARE_URL =
  "https://ais-pre-hzeqr4nb7corovn7vkwgnr-412075223902.us-east5.run.app"

fun shareOmniChurchContent(
  context: Context,
  title: String,
  body: String,
) {
  val fullMessage = buildString {
    append(title.trim())
    if (body.isNotBlank()) {
      append("\n\n")
      append(body.trim())
    }
    append("\n\n📲 OmniChurch Pro (Legliz la nan poch ou):\n")
    append(OMNICHURCH_OFFICIAL_SHARE_URL)
  }
  runCatching {
    val sendIntent =
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, fullMessage)
      }
    val chooser =
      Intent.createChooser(sendIntent, "Pataje sou OmniChurch").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }.onFailure {
    copyOmniChurchTextToClipboard(context, title, fullMessage)
  }
}

fun shareOmniChurchGeneralApp(
  context: Context,
  churchName: String? = null,
) {
  val header =
    if (!churchName.isNullOrBlank()) {
      "🔥 Rejwenn $churchName sou OmniChurch Mobile!"
    } else {
      "🔥 Dekouvri OmniChurch Mobile — Legliz la nan poch ou!"
    }
  val body =
    "Swiv tout sèvis adorasyon an dirèk, anons ofisyèl, kat idantite manm dijital QR, ak prezans legliz ou sou OmniChurch Pro."
  shareOmniChurchContent(context, header, body)
}

fun copyOmniChurchTextToClipboard(
  context: Context,
  label: String,
  text: String,
) {
  runCatching {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Tèks la kopye avèk siksè!", Toast.LENGTH_SHORT).show()
  }
}

/**
 * 12. PremiumServiceCard — Self-sufficient 3D Church Service card matching the 3D quality,
 *     borderless full-bleed Portrait/Landscape media showcase, and balanced layout of
 *     PremiumAnnouncementCard while preserving its dedicated Liturgical & Attendance Service role.
 */
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
  canEdit: Boolean = isAdmin,
  isScanner: Boolean = false,
  churchMembers: List<Member> = emptyList(),
  churchLogoUrl: String? = null,
  languageCode: String = "ht",
  onToggleAttendanceWindow: ((Boolean) -> Unit)? = null,
  onOpenScanner: (() -> Unit)? = null,
  onViewAttendances: (() -> Unit)? = null,
  onShareClick: (() -> Unit)? = null,
  onEditClick: (() -> Unit)? = null,
  onDeleteClick: (() -> Unit)? = null,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  var showReactionSelector by remember { mutableStateOf(false) }
  var showMoreMenuDialog by remember { mutableStateOf(false) }
  var showConfirmDeleteDialog by remember { mutableStateOf(false) }
  var showFullImageDialog by remember { mutableStateOf(false) }

  var currentEpochMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  val isPermanentlyLocked = service.isPermanentlyLocked(currentEpochMillis)
  val isLiveActive = service.isLiveBadgeActive(currentEpochMillis)
  val isTimerTicking = service.isTimerTicking(currentEpochMillis)
  val isBeforeStart = service.isBeforeStart(currentEpochMillis)
  val timerLabel = service.elapsedTimerLabel(currentEpochMillis)

  LaunchedEffect(
    service.id,
    service.status,
    service.isOpenForAttendance,
    service.date,
    service.startTime,
    service.endTime,
  ) {
    currentEpochMillis = System.currentTimeMillis()
    while (true) {
      delay(1000L)
      val now = System.currentTimeMillis()
      currentEpochMillis = now
      if (service.isPermanentlyLocked(now)) break
    }
  }

  val serviceDateSubtitle =
    remember(
      service.date,
      service.dateFormatted,
      service.startTime,
      service.endTime,
      service.startTimeEpochMillis,
      languageCode,
    ) {
      service.formattedScheduleWithDay(languageCode)
    }
  val publishedTimestampText =
    remember(service.createdAt, service.dateFormatted, service.date, languageCode) {
      formatServiceTimestamp(service, languageCode)
    }
  val mediaUrl = service.effectiveImageUrl

  val shareTitle =
    "⛪ ${service.effectiveTitle}" +
      (service.churchName.takeIf { it.isNotBlank() }?.let { " — $it" } ?: "")
  val shareBody = buildString {
    append("📌 Kategori: ${service.categoryLabelHt}")
    if (serviceDateSubtitle.isNotBlank()) {
      append("\n📅 Dat & Lè: $serviceDateSubtitle")
    }
    service.theme?.takeIf { it.isNotBlank() }?.let { append("\n✨ Tèm: $it") }
    service.passage?.takeIf { it.isNotBlank() }?.let { append("\n📖 Pasaj: $it") }
    service.preacher?.takeIf { it.isNotBlank() }?.let { append("\n🎙️ Predikatè: $it") }
    service.leader?.takeIf { it.isNotBlank() }?.let { append("\n🙏 Dirijan: $it") }
    if (!service.description.isNullOrBlank()) {
      append("\n\n${service.description}")
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
    cornerRadius = 22.dp,
    isHighlighted = isLiveActive || (service.isOpenForAttendance && !isPermanentlyLocked),
    accentColor =
      when {
        isLiveActive -> LiveBadgeRed
        service.isOpenForAttendance && !isPermanentlyLocked -> SuccessGreen
        else -> ElectricBlue
      },
    contentPadding = PaddingValues(0.dp),
    modifier = modifier.fillMaxWidth().testTag("feed_service_card_${service.id}"),
  ) {
    // 1. Card Header — Church Profile Avatar + Author Identity first + Published Date cleanly aligned underneath
    val primaryAuthorTitle =
      service.churchName.trim().ifBlank {
        service.authorName.trim().ifBlank { service.categoryLabelHt }
      }
    val secondaryAuthorLabel =
      service.authorName.trim().takeIf {
        it.isNotBlank() && !it.equals(primaryAuthorTitle, ignoreCase = true)
      }
    val langNorm = languageCode.trim().lowercase()
    val statusEndedLabel =
      when (langNorm) {
        "fr" -> "TERMINÉ"
        "en" -> "ENDED"
        "es" -> "FINALIZADO"
        else -> "FINI"
      }
    val statusAttendanceOpenLabel =
      when (langNorm) {
        "fr" -> "PRÉSENCE OUVERTE"
        "en" -> "ATTENDANCE OPEN"
        "es" -> "ASISTENCIA ABIERTA"
        else -> "PREZANS OUVRI"
      }
    val statusScheduledLabel =
      when (langNorm) {
        "fr" -> "PROGRAMMÉ"
        "en" -> "SCHEDULED"
        "es" -> "PROGRAMADO"
        else -> "PWOGWAME"
      }

    Row(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ChurchProfileAvatar(
        logoUrl = churchLogoUrl,
        churchName = primaryAuthorTitle,
        size = 44.dp,
        modifier = Modifier.testTag("service_church_avatar_${service.id}"),
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        // Author / Church data first
        Text(
          text = primaryAuthorTitle,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("service_author_title_${service.id}"),
        )
        if (!secondaryAuthorLabel.isNullOrBlank()) {
          Text(
            text = secondaryAuthorLabel,
            style = MaterialTheme.typography.labelSmall,
            color = BlueNeon.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        // Published date cleanly aligned underneath with full width (no crowding)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier.padding(top = 1.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = BlueNeon.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
          )
          Text(
            text = publishedTimestampText,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("service_timestamp_${service.id}"),
          )
        }
      }
    }

    // 1b. Horizontal Live / Status / Category Badges Strip placed cleanly side-by-side underneath Header
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(start = 14.dp, end = 14.dp, bottom = 10.dp)
          .testTag("service_status_badges_row_${service.id}"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (isLiveActive) {
        Box(
          modifier =
            Modifier.shadow(8.dp, RoundedCornerShape(8.dp), ambientColor = LiveBadgeRed)
              .clip(RoundedCornerShape(8.dp))
              .background(
                Brush.horizontalGradient(listOf(LiveBadgeRed, Color(0xFFD91432)))
              )
              .border(1.dp, PureWhite.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 3.dp)
              .testTag("service_live_badge_${service.id}"),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier = Modifier.size(6.dp).clip(CircleShape).background(PureWhite)
            )
            Text(
              text = stringResource(R.string.feed_live_badge),
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite,
              fontWeight = FontWeight.ExtraBold,
              maxLines = 1,
            )
          }
        }

        if (isTimerTicking) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(8.dp))
                .background(Color(0xCC09142B))
                .border(0.9.dp, LiveBadgeRed.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
                .testTag("service_live_timer_${service.id}"),
          ) {
            Text(
              text = timerLabel,
              style =
                MaterialTheme.typography.labelSmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontFeatureSettings = "tnum",
                ),
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        }
      }

      if (isPermanentlyLocked) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(Color(0x661E293B))
              .border(0.9.dp, MutedGlassText.copy(alpha = 0.45f), RoundedCornerShape(50))
              .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = statusEndedLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MutedGlassText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      } else if (service.isOpenForAttendance) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(SuccessGreen.copy(alpha = 0.2f))
              .border(1.dp, SuccessGreen.copy(alpha = 0.8f), RoundedCornerShape(50))
              .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Box(
              modifier = Modifier.size(6.dp).clip(CircleShape).background(SuccessGreen)
            )
            Text(
              text = statusAttendanceOpenLabel,
              style = MaterialTheme.typography.labelSmall,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
            )
          }
        }
      } else if (isBeforeStart) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(ElectricBlue.copy(alpha = 0.18f))
              .border(0.9.dp, BlueNeon.copy(alpha = 0.55f), RoundedCornerShape(50))
              .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = statusScheduledLabel,
            style = MaterialTheme.typography.labelSmall,
            color = BlueNeon,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
          )
        }
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(8.dp))
            .background(ElectricBlue.copy(alpha = 0.22f))
            .border(0.8.dp, BlueNeon.copy(alpha = 0.52f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
      ) {
        Text(
          text = service.categoryLabelHt,
          style = MaterialTheme.typography.labelSmall,
          color = BlueNeon,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
      }
    }

    // 2. Service Title & Liturgical Details directly above the media showcase (Post-style Headline)
    val hasThemeOrPassage = !service.theme.isNullOrBlank() || !service.passage.isNullOrBlank()
    val hasDescription =
      !service.description.isNullOrBlank() && service.description != service.theme
    val officialRoleAvatars =
      remember(service.preacher, service.leader, service.reader, churchMembers) {
        buildList {
          service.preacher?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = "Predikatè",
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = BrandGold,
                badgeBackgroundColor = Color(0xEB2A1C05),
              )
            )
          }
          service.leader?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = "Dirijan",
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = BlueNeon,
                badgeBackgroundColor = Color(0xEB072142),
              )
            )
          }
          service.reader?.trim()?.takeIf { it.isNotBlank() }?.let { personName ->
            add(
              ServiceOfficialRoleDisplay(
                roleLabel = "Lektè",
                personName = personName,
                photoUrl = resolveMemberPhotoForRole(personName, churchMembers),
                accentColor = SuccessGreen,
                badgeBackgroundColor = Color(0xEB062C1E),
              )
            )
          }
        }
      }

    Column(
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      if (serviceDateSubtitle.isNotBlank()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier =
            Modifier.fillMaxWidth()
              .padding(bottom = 2.dp)
              .testTag("service_schedule_header_${service.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = BrandGold,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = serviceDateSubtitle,
            style = MaterialTheme.typography.titleSmall,
            color = BrandGold,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Text(
        text = service.effectiveTitle,
        style = MaterialTheme.typography.titleLarge,
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.testTag("service_headline_title_${service.id}"),
      )

      if (hasThemeOrPassage) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          if (!service.theme.isNullOrBlank()) {
            Text(
              text = "✨ Tèm: ${service.theme}",
              style = MaterialTheme.typography.bodyMedium,
              color = BrandGold,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.weight(1f, fill = false),
            )
          }
          if (!service.passage.isNullOrBlank()) {
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFF1E293B))
                  .border(0.8.dp, BrandGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                  .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.MenuBook,
                  contentDescription = null,
                  tint = BrandGold,
                  modifier = Modifier.size(13.dp),
                )
                Text(
                  text = service.passage,
                  style = MaterialTheme.typography.labelSmall,
                  color = PureWhite,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }
      }

      if (hasDescription) {
        Text(
          text = service.description.orEmpty(),
          style = MaterialTheme.typography.bodyMedium,
          color = GlassWhite.copy(alpha = 0.92f),
        )
      }
    }

    // 3. Full-Bleed Borderless Service Media Showcase with Predikatè / Dirijan / Lektè Avatar Overlay at Bottom in Front of Image
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = if (officialRoleAvatars.isNotEmpty()) 114.dp else 0.dp),
    ) {
      if (!mediaUrl.isNullOrBlank()) {
        AnnouncementMediaShowcase(
          imageSource = mediaUrl,
          title = service.effectiveTitle,
          announcementId = "srv_${service.id}",
          onImageClick = { showFullImageDialog = true },
          modifier = Modifier.testTag("service_media_${service.id}"),
        )
      } else {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(if (officialRoleAvatars.isNotEmpty()) 188.dp else 156.dp),
        ) {
          WorshipStageBannerCanvas(modifier = Modifier.fillMaxSize())
        }
      }

      if (officialRoleAvatars.isNotEmpty()) {
        Box(
          modifier =
            Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .height(106.dp)
              .background(
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color.Transparent,
                      Color(0x94040B1A),
                      Color(0xEE030917),
                    )
                )
              )
        )

        Row(
          modifier =
            Modifier
              .align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("service_officials_overlay_${service.id}"),
          horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
          verticalAlignment = Alignment.Top,
        ) {
          officialRoleAvatars.forEach { official ->
            ServiceOfficialAvatarBadge(
              official = official,
              modifier =
                if (officialRoleAvatars.size == 1) {
                  Modifier.widthIn(min = 96.dp, max = 148.dp)
                } else {
                  Modifier.weight(1f)
                },
            )
          }
        }
      }
    }

    // 4. Optional Admin & Scanner Attendance Controls Bar (Used in ServicesScreen)
    if (
      (isAdmin && onToggleAttendanceWindow != null) ||
        (isScanner && (onOpenScanner != null || onViewAttendances != null))
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (isAdmin && onToggleAttendanceWindow != null) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1933).copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f).padding(end = 8.dp),
            ) {
              Box(
                modifier =
                  Modifier.size(8.dp)
                    .clip(CircleShape)
                    .background(
                      when {
                        isPermanentlyLocked -> ErrorRed
                        service.isOpenForAttendance -> SuccessGreen
                        else -> MutedGlassText
                      }
                    )
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text =
                  when {
                    isPermanentlyLocked -> "Sèvis sa fini"
                    service.isOpenForAttendance ->
                      stringResource(R.string.scanner_open_for_attendance)
                    else -> stringResource(R.string.scanner_closed_for_attendance)
                  },
                style = MaterialTheme.typography.labelMedium,
                color =
                  when {
                    isPermanentlyLocked -> ErrorRed.copy(alpha = 0.9f)
                    service.isOpenForAttendance -> SuccessGreen
                    else -> MutedGlassText
                  },
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Switch(
              checked = !isPermanentlyLocked && service.isOpenForAttendance,
              enabled = !isPermanentlyLocked,
              onCheckedChange = { wantOpen ->
                if (!isPermanentlyLocked) {
                  onToggleAttendanceWindow(wantOpen)
                }
              },
              colors =
                SwitchDefaults.colors(
                  checkedThumbColor = PureWhite,
                  checkedTrackColor = SuccessGreen,
                  disabledUncheckedThumbColor = MutedGlassText.copy(alpha = 0.5f),
                  disabledUncheckedTrackColor = Color(0xFF1E293B),
                ),
              modifier = Modifier.testTag("toggle_service_open_${service.id}"),
            )
          }
        }

        if (isScanner && (onOpenScanner != null || onViewAttendances != null)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (!isPermanentlyLocked && (service.isOpenForAttendance || isLiveActive) && onOpenScanner != null) {
              GlassButton(
                text = stringResource(R.string.action_scan_qr),
                icon = Icons.Default.QrCodeScanner,
                onClick = onOpenScanner,
                modifier = Modifier.weight(1f),
                testTag = "scan_service_btn_${service.id}",
              )
            }
            if (onViewAttendances != null) {
              GlassButton(
                text = stringResource(R.string.attendance_registered_title),
                icon = Icons.Default.People,
                isPrimaryNeon = false,
                onClick = onViewAttendances,
                modifier = Modifier.weight(1f),
                testTag = "view_attendances_btn_${service.id}",
              )
            }
          }
        }
      }
    }

    // 5. Floating 7-Reaction 3D Selector Bar & Engagement Bar
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp)
    ) {
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
  }

  if (showFullImageDialog && !mediaUrl.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = mediaUrl,
      title = service.effectiveTitle,
      department = "${service.categoryLabelHt} • $serviceDateSubtitle",
      onDismiss = { showFullImageDialog = false },
    )
  }

  if (showMoreMenuDialog) {
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = service.effectiveTitle,
    ) {
      Text(
        text = "${service.categoryLabelHt} • $serviceDateSubtitle",
        style = MaterialTheme.typography.bodySmall,
        color = MutedGlassText,
      )
      Spacer(modifier = Modifier.height(12.dp))

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

      if (canEdit && onEditClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
        GlassButton(
          text = "Modifye Sèvis sa a",
          icon = Icons.Default.Edit,
          isPrimaryNeon = false,
          onClick = {
            showMoreMenuDialog = false
            onEditClick()
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "service_menu_edit_${service.id}",
        )
      }

      if (isAdmin && onDeleteClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        if (!(canEdit && onEditClick != null)) {
          HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
          Spacer(modifier = Modifier.height(10.dp))
        }
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

private data class ServiceOfficialRoleDisplay(
  val roleLabel: String,
  val personName: String,
  val photoUrl: String?,
  val accentColor: Color,
  val badgeBackgroundColor: Color,
)

@Composable
private fun ServiceOfficialAvatarBadge(
  official: ServiceOfficialRoleDisplay,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Avatar at top with small role badge anchored at bottom-center of avatar
    Box(
      modifier = Modifier.height(57.dp),
      contentAlignment = Alignment.TopCenter,
    ) {
      MemberAvatar3D(
        fullName = official.personName,
        size = 48.dp,
        photoUrl = official.photoUrl,
        borderColor = official.accentColor,
        modifier = Modifier.shadow(6.dp, CircleShape),
      )

      Box(
        modifier =
          Modifier
            .align(Alignment.BottomCenter)
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(official.badgeBackgroundColor)
            .border(1.dp, official.accentColor, RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = official.roleLabel,
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              lineHeight = 11.sp,
              fontWeight = FontWeight.ExtraBold,
            ),
          color = PureWhite,
          maxLines = 1,
          softWrap = false,
        )
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Member name placed cleanly beneath the badge
    Text(
      text = official.personName,
      style =
        MaterialTheme.typography.labelSmall.copy(
          fontSize = 11.sp,
          lineHeight = 13.sp,
          fontWeight = FontWeight.Bold,
          shadow =
            Shadow(
              color = Color(0xCC020612),
              offset = Offset(0f, 1.5f),
              blurRadius = 4f,
            ),
        ),
      color = PureWhite,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
    )
  }
}

private fun resolveMemberPhotoForRole(personName: String, churchMembers: List<Member>): String? {
  val cleanTarget = personName.trim()
  if (cleanTarget.isBlank() || churchMembers.isEmpty()) return null
  val normalizedTarget = cleanTarget.lowercase(java.util.Locale.getDefault())
  val exactMatch =
    churchMembers.firstOrNull { member ->
      val full = member.effectiveFullName.trim().lowercase(java.util.Locale.getDefault())
      val rawFull = member.fullName.trim().lowercase(java.util.Locale.getDefault())
      val prefixed =
        listOfNotNull(
            member.titlePrefix?.trim()?.takeIf { it.isNotBlank() },
            member.effectiveFullName.trim().takeIf { it.isNotBlank() },
          )
          .joinToString(" ")
          .lowercase(java.util.Locale.getDefault())
      normalizedTarget == full ||
        normalizedTarget == rawFull ||
        (prefixed.isNotBlank() && normalizedTarget == prefixed)
    }
  val resolvedMember =
    exactMatch
      ?: churchMembers.firstOrNull { member ->
        val full = member.effectiveFullName.trim().lowercase(java.util.Locale.getDefault())
        full.length >= 4 && normalizedTarget.contains(full)
      }
  return resolvedMember?.effectivePhotoUrl
}

@Composable
fun ChurchProfileAvatar(
  logoUrl: String?,
  churchName: String,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
) {
  val cleanUrl = remember(logoUrl) { logoUrl?.trim()?.takeIf { it.isNotEmpty() } }
  val decodedBitmap =
    remember(cleanUrl) {
      cleanUrl?.let { ImageCompressor.decodeBase64DataUrlToBitmap(it) }
    }
  var remoteLoadFailed by remember(cleanUrl) { mutableStateOf(false) }
  val initials =
    remember(churchName) {
      churchName
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "" }
    }

  Box(
    modifier =
      modifier
        .size(size)
        .shadow(8.dp, CircleShape, ambientColor = ElectricBlue)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(
              BlueNeon,
              ElectricBlue,
              Color(0xFF1D4ED8),
            )
          )
        )
        .border(1.2.dp, PureWhite.copy(alpha = 0.72f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    when {
      decodedBitmap != null -> {
        Image(
          bitmap = decodedBitmap.asImageBitmap(),
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
      cleanUrl != null && !remoteLoadFailed -> {
        AsyncImage(
          model = cleanUrl,
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          contentScale = ContentScale.Crop,
          onError = { remoteLoadFailed = true },
          modifier = Modifier.fillMaxSize(),
        )
      }
      initials.isNotBlank() -> {
        Text(
          text = initials,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.ExtraBold,
          maxLines = 1,
        )
      }
      else -> {
        Icon(
          imageVector = Icons.Default.Church,
          contentDescription = churchName.ifBlank { "Logo Legliz" },
          tint = PureWhite,
          modifier = Modifier.size(size * 0.5f),
        )
      }
    }
  }
}

private fun formatServiceTimestamp(
  service: ChurchService,
  languageCode: String = "ht",
): String {
  val lang = languageCode.trim().lowercase()
  val ts = service.createdAt
  if (ts != null) {
    val dateObj = ts.toDate()
    val nowCal = java.util.Calendar.getInstance()
    val targetCal = java.util.Calendar.getInstance().apply { time = dateObj }
    val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(dateObj)

    val isSameDay =
      nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        nowCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isSameDay) {
      return when (lang) {
        "fr" -> "Publié aujourd'hui • $timeStr"
        "en" -> "Published today • $timeStr"
        "es" -> "Publicado hoy • $timeStr"
        else -> "Pibliye jodi a • $timeStr"
      }
    }

    val yesterdayCal =
      (nowCal.clone() as java.util.Calendar).apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
    val isYesterday =
      yesterdayCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) ==
          targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) {
      return when (lang) {
        "fr" -> "Publié hier • $timeStr"
        "en" -> "Published yesterday • $timeStr"
        "es" -> "Publicado ayer • $timeStr"
        else -> "Pibliye yè • $timeStr"
      }
    }

    val dateFormatter =
      java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
    val formatted = dateFormatter.format(dateObj)
    return when (lang) {
      "fr" -> "Publié le $formatted"
      "en" -> "Published $formatted"
      "es" -> "Publicado $formatted"
      else -> "Pibliye $formatted"
    }
  }
  val prefix =
    when (lang) {
      "fr" -> "Publié"
      "en" -> "Published"
      "es" -> "Publicado"
      else -> "Pibliye"
    }
  return "$prefix • ${service.formattedScheduleWithDay(languageCode)}"
}

private fun formatAnnouncementTimestamp(announcement: Announcement): String {
  val ts = announcement.createdAt
  if (ts != null) {
    val dateObj = ts.toDate()
    val nowCal = java.util.Calendar.getInstance()
    val targetCal = java.util.Calendar.getInstance().apply { time = dateObj }
    val timeFormatter = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
    val timeStr = timeFormatter.format(dateObj)

    val isSameDay =
      nowCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        nowCal.get(java.util.Calendar.DAY_OF_YEAR) == targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isSameDay) {
      return "Jodi a • $timeStr"
    }

    val yesterdayCal =
      (nowCal.clone() as java.util.Calendar).apply {
        add(java.util.Calendar.DAY_OF_YEAR, -1)
      }
    val isYesterday =
      yesterdayCal.get(java.util.Calendar.YEAR) == targetCal.get(java.util.Calendar.YEAR) &&
        yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) ==
          targetCal.get(java.util.Calendar.DAY_OF_YEAR)
    if (isYesterday) {
      return "Yè • $timeStr"
    }

    val fullFormatter =
      java.text.SimpleDateFormat("dd MMM yyyy • hh:mm a", java.util.Locale.getDefault())
    return fullFormatter.format(dateObj)
  }

  val fallbackDate = announcement.date?.trim().orEmpty()
  return if (fallbackDate.isNotEmpty()) fallbackDate else "Kounye a"
}

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
    if (announcement.department.isNotBlank()) "📢 $resolvedTitle (${announcement.department})"
    else "📢 $resolvedTitle"
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
        // Golden-Orange 3D Megaphone Badge with optional Unread Indicator Dot
        Box(contentAlignment = Alignment.TopEnd) {
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
                .background(BlueNeon.copy(alpha = 0.22f))
                .border(1.dp, BlueNeon.copy(alpha = 0.85f), RoundedCornerShape(50))
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
      // Floating 7-Reaction 3D Selector Bar
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
    GlassDialog(
      onDismissRequest = { showMoreMenuDialog = false },
      title = resolvedTitle,
    ) {
      if (announcement.department.isNotBlank()) {
        Text(
          text = announcement.department,
          style = MaterialTheme.typography.bodySmall,
          color = MutedGlassText,
        )
        Spacer(modifier = Modifier.height(12.dp))
      }

      GlassButton(
        text = copy.shareAction,
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
        text = copy.shareAppAction,
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
        text = copy.copyTextAction,
        icon = Icons.Default.ContentCopy,
        isPrimaryNeon = false,
        onClick = {
          showMoreMenuDialog = false
          copyOmniChurchTextToClipboard(
            context = context,
            label = resolvedTitle,
            text = "$shareTitle\n\n$shareBody",
          )
        },
        modifier = Modifier.fillMaxWidth(),
        testTag = "announcement_menu_copy_${announcement.id}",
      )

      if (canEdit && onEditClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))
        GlassButton(
          text = copy.editAction,
          icon = Icons.Default.Edit,
          isPrimaryNeon = false,
          onClick = {
            showMoreMenuDialog = false
            onEditClick()
          },
          modifier = Modifier.fillMaxWidth(),
          testTag = "announcement_menu_edit_${announcement.id}",
        )
      }

      if (isAdmin && onDeleteClick != null) {
        Spacer(modifier = Modifier.height(10.dp))
        if (!(canEdit && onEditClick != null)) {
          HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
          Spacer(modifier = Modifier.height(10.dp))
        }
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
              contentDescription = copy.deleteAction,
              tint = PureWhite,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = copy.deleteAction,
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
      title = copy.deleteDialogTitle,
    ) {
      Text(
        text = "\"$resolvedTitle\"",
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
          text = copy.deleteConfirmYes,
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

  if (showImageLightbox && !effectiveImage.isNullOrBlank()) {
    AnnouncementMediaShowcaseLightbox(
      imageSource = effectiveImage,
      title = resolvedTitle,
      department = announcement.department,
      onDismiss = { showImageLightbox = false },
    )
  }
}

/**
 * Edge-to-edge borderless Image Showcase for Announcements supporting both
 * Portrait ("Potre") and Landscape ("Peyizaj") with exact aspect-ratio measurement
 * and zero vertical/horizontal overflow.
 */
@Composable
fun AnnouncementMediaShowcase(
  imageSource: String,
  title: String,
  announcementId: String,
  onImageClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap =
    remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }
  var remoteAspectRatio by remember(cleanSource) { mutableStateOf<Float?>(null) }
  var loadError by remember(cleanSource) { mutableStateOf(false) }

  val naturalRatio: Float? =
    remember(decodedBitmap, remoteAspectRatio) {
      if (decodedBitmap != null && decodedBitmap.height > 0 && decodedBitmap.width > 0) {
        decodedBitmap.width.toFloat() / decodedBitmap.height.toFloat()
      } else {
        remoteAspectRatio
      }
    }

  if (loadError && decodedBitmap == null) return

  val clampedRatio = naturalRatio?.coerceIn(0.65f, 2.25f)

  val sizingModifier =
    if (clampedRatio != null) {
      Modifier.fillMaxWidth().aspectRatio(clampedRatio)
    } else {
      Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 380.dp)
    }

  Box(
    modifier =
      modifier
        .then(sizingModifier)
        .clipToBounds()
        .clickable(onClick = onImageClick)
        .testTag("announcement_image_${announcementId}"),
    contentAlignment = Alignment.Center,
  ) {
    if (decodedBitmap != null) {
      Image(
        bitmap = decodedBitmap.asImageBitmap(),
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      AsyncImage(
        model = cleanSource,
        contentDescription = title,
        contentScale = if (clampedRatio != null) ContentScale.Crop else ContentScale.FillWidth,
        onSuccess = { state ->
          val d = state.result.drawable
          if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0) {
            remoteAspectRatio = d.intrinsicWidth.toFloat() / d.intrinsicHeight.toFloat()
          }
        },
        onError = { loadError = true },
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
internal fun AnnouncementMediaShowcaseLightbox(
  imageSource: String,
  title: String,
  department: String,
  onDismiss: () -> Unit,
) {
  val cleanSource = remember(imageSource) { imageSource.trim() }
  val decodedBitmap =
    remember(cleanSource) { ImageCompressor.decodeBase64DataUrlToBitmap(cleanSource) }

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
      ),
  ) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(Color(0xF2040A18))
          .clickable(onClick = onDismiss)
          .padding(16.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium,
              color = PureWhite,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (department.isNotBlank()) {
              Text(
                text = department,
                style = MaterialTheme.typography.bodySmall,
                color = BrandGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          Spacer(modifier = Modifier.width(12.dp))
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier.size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B))
                .border(1.dp, PureWhite.copy(alpha = 0.35f), CircleShape),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Fèmen",
              tint = PureWhite,
            )
          }
        }

        Box(
          modifier =
            Modifier.weight(1f)
              .fillMaxWidth()
              .padding(vertical = 12.dp)
              .clip(RoundedCornerShape(18.dp)),
          contentAlignment = Alignment.Center,
        ) {
          if (decodedBitmap != null) {
            Image(
              bitmap = decodedBitmap.asImageBitmap(),
              contentDescription = title,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            AsyncImage(
              model = cleanSource,
              contentDescription = title,
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )
          }
        }

        Text(
          text = "Peze nenpòt kote pou fèmen",
          style = MaterialTheme.typography.labelSmall,
          color = MutedGlassText,
          modifier = Modifier.padding(bottom = 8.dp),
        )
      }
    }
  }
}

/**
 * 14. GlassSegmentedControl — Two-option 3D glass segmented control with glowing neon active pill.
 */
@Composable
fun GlassSegmentedControl(
  leftLabel: String,
  leftIcon: ImageVector,
  rightLabel: String,
  rightIcon: ImageVector,
  isLeftSelected: Boolean,
  onSelectLeft: () -> Unit,
  onSelectRight: () -> Unit,
  leftTestTag: String = "segment_left",
  rightTestTag: String = "segment_right",
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .height(54.dp)
        .clip(RoundedCornerShape(50))
        .background(Color(0xCC0A1836))
        .border(1.2.dp, GlassWhite.copy(alpha = 0.28f), RoundedCornerShape(50))
        .padding(4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Left Segment
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .then(
            if (isLeftSelected) {
              Modifier.shadow(12.dp, RoundedCornerShape(50), ambientColor = BlueNeon)
                .clip(RoundedCornerShape(50))
                .background(OmniBrushes.ActiveBlueNeonBrush)
                .border(1.3.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(50))
            } else {
              Modifier.clip(RoundedCornerShape(50))
            }
          )
          .clickable(onClick = onSelectLeft)
          .testTag(leftTestTag),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = leftIcon,
          contentDescription = leftLabel,
          tint = PureWhite,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = leftLabel,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    // Right Segment
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxSize()
          .then(
            if (!isLeftSelected) {
              Modifier.shadow(12.dp, RoundedCornerShape(50), ambientColor = BlueNeon)
                .clip(RoundedCornerShape(50))
                .background(OmniBrushes.ActiveBlueNeonBrush)
                .border(1.3.dp, PureWhite.copy(alpha = 0.85f), RoundedCornerShape(50))
            } else {
              Modifier.clip(RoundedCornerShape(50))
            }
          )
          .clickable(onClick = onSelectRight)
          .testTag(rightTestTag),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = rightIcon,
          contentDescription = rightLabel,
          tint = PureWhite,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = rightLabel,
          style = MaterialTheme.typography.titleSmall,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

/**
 * 15. PremiumSuccessPanel — Reusable emerald-green 3D attendance confirmation panel
 *     matching Phone 3's bottom panel in the reference image.
 */
@Composable
fun PremiumSuccessPanel(
  member: Member,
  serviceTitle: String,
  recordedTimeFormatted: String,
  status: String,
  modifier: Modifier = Modifier,
  isVisitor: Boolean = false,
  homeChurchName: String? = null,
) {
  val isLate = status == "late"
  val badgeLabel =
    if (isLate) stringResource(R.string.attendance_status_late)
    else stringResource(R.string.attendance_status_on_time)

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .shadow(18.dp, RoundedCornerShape(22.dp), ambientColor = SuccessGreen)
        .clip(RoundedCornerShape(22.dp))
        .background(
          Brush.verticalGradient(
            listOf(
              Color(0xE60B4F35),
              Color(0xF2072E22),
              Color(0xFA061826),
            )
          )
        )
        .border(
          1.6.dp,
          Brush.linearGradient(listOf(PureWhite.copy(alpha = 0.85f), SuccessGreen, Color(0xFF0A6E46))),
          RoundedCornerShape(22.dp),
        )
        .drawBehind {
          drawLine(
            brush =
              Brush.horizontalGradient(
                listOf(Color.Transparent, PureWhite.copy(alpha = 0.7f), SuccessGreen, Color.Transparent)
              ),
            start = Offset(size.width * 0.2f, 1.dp.toPx()),
            end = Offset(size.width * 0.8f, 1.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
          )
        }
        .padding(14.dp)
        .testTag("scanner_success_confirmation_card"),
  ) {
    // Top Green Header Banner ("Prezans Anrejistre ✓")
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Default.CheckCircle,
        contentDescription = null,
        tint = SuccessGreen,
        modifier = Modifier.size(22.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "${stringResource(R.string.attendance_registered_title)} ✓",
        style = MaterialTheme.typography.titleMedium,
        color = PureWhite,
        fontWeight = FontWeight.ExtraBold,
      )
      if (isVisitor) {
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .background(ElectricBlue.copy(alpha = 0.35f))
              .border(1.dp, BlueNeon, RoundedCornerShape(50))
              .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Text(
            text = "Vizitè",
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.16f))
    Spacer(modifier = Modifier.height(10.dp))

    // Member Info Row + "A Lè" / "An Reta" Pill
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        MemberAvatar3D(
          fullName = member.effectiveFullName,
          size = 52.dp,
          photoUrl = member.effectivePhotoUrl,
          borderColor = SuccessGreen,
        )

        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = member.effectiveFullName,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              if (isVisitor) {
                val origin = homeChurchName?.takeIf { it.isNotBlank() } ?: member.churchName
                "Vizitè • Soti nan: $origin"
              } else {
                "ID: ${member.effectiveMatrikil}"
              },
            style = MaterialTheme.typography.bodySmall,
            color = if (isVisitor) BlueNeon else GlassWhite.copy(alpha = 0.88f),
            fontWeight = if (isVisitor) FontWeight.SemiBold else FontWeight.Normal,
          )
        }
      }

      Box(
        modifier =
          Modifier.clip(RoundedCornerShape(10.dp))
            .background(
              if (isLate) BrandOrange.copy(alpha = 0.28f)
              else Color(0xFF0A6E46)
            )
            .border(
              1.2.dp,
              if (isLate) BrandGold else SuccessGreen,
              RoundedCornerShape(10.dp),
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = badgeLabel,
          style = MaterialTheme.typography.labelLarge,
          color = PureWhite,
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GlassWhite.copy(alpha = 0.14f))
    Spacer(modifier = Modifier.height(8.dp))

    // Service & Timestamp Footer Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "${stringResource(R.string.attendance_service_prefix)} $serviceTitle",
        style = MaterialTheme.typography.bodySmall,
        color = PureWhite,
        fontWeight = FontWeight.SemiBold,
      )
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Schedule,
          contentDescription = null,
          tint = GlassWhite.copy(alpha = 0.85f),
          modifier = Modifier.size(14.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = recordedTimeFormatted,
          style = MaterialTheme.typography.bodySmall,
          color = PureWhite,
          fontWeight = FontWeight.SemiBold,
        )
      }
    }
  }
}

/** 16. GlassLoadingIndicator — Pulsing neon ring loader for real-time Firebase queries. */
@Composable
fun GlassLoadingIndicator(
  message: String = stringResource(R.string.common_loading),
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    CircularProgressIndicator(
      color = BlueNeon,
      trackColor = GlassWhite.copy(alpha = 0.15f),
      strokeWidth = 3.dp,
      modifier = Modifier.size(44.dp),
    )
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = GlassWhite.copy(alpha = 0.85f),
      textAlign = TextAlign.Center,
    )
  }
}

/** 17. PremiumEmptyState — 3D glassmorphic empty state when Firestore returns 0 documents. */
@Composable
fun PremiumEmptyState(
  title: String,
  subtitle: String,
  icon: ImageVector,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  secondaryActionLabel: String? = null,
  onSecondaryActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  PremiumGlassCard(
    modifier = modifier.fillMaxWidth(),
    cornerRadius = 22.dp,
    contentPadding = PaddingValues(24.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier =
          Modifier.size(64.dp)
            .clip(CircleShape)
            .background(
              Brush.radialGradient(
                listOf(ElectricBlue.copy(alpha = 0.45f), PrimaryNavy)
              )
            )
            .border(1.5.dp, BlueNeon.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = BlueNeon,
          modifier = Modifier.size(30.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = PureWhite,
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MutedGlassText,
        textAlign = TextAlign.Center,
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          GlassButton(
            text = actionLabel,
            onClick = onActionClick,
            modifier = Modifier.weight(1f),
            testTag = "empty_state_primary_action",
          )
          if (secondaryActionLabel != null && onSecondaryActionClick != null) {
            GlassButton(
              text = secondaryActionLabel,
              onClick = onSecondaryActionClick,
              isPrimaryNeon = false,
              modifier = Modifier.weight(1f),
              testTag = "empty_state_secondary_action",
            )
          }
        }
      }
    }
  }
}

/**
 * 18. PremiumQRContainer & GlassQRContainer — High-contrast pure white QR container framed by
 *     glowing electric-blue 3D corner brackets and an optional centered OmniChurch flame badge
 *     (matching Phone 2 in the reference image while remaining 100% scannable).
 */
@Composable
fun PremiumQRContainer(
  qrBitmap: Bitmap?,
  size: Dp = 200.dp,
  showLaserLine: Boolean = false,
  showCenterFlameBadge: Boolean = true,
  modifier: Modifier = Modifier,
) {
  GlassQRContainer(
    qrBitmap = qrBitmap,
    size = size,
    showLaserLine = showLaserLine,
    showCenterFlameBadge = showCenterFlameBadge,
    modifier = modifier,
  )
}

@Composable
fun GlassQRContainer(
  qrBitmap: Bitmap?,
  size: Dp = 210.dp,
  showLaserLine: Boolean = false,
  showCenterFlameBadge: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "qr_laser")
  val laserProgress by
    infiniteTransition.animateFloat(
      initialValue = 0.12f,
      targetValue = 0.88f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 2200, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "laser_y",
    )

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    // Pure white inner card so QR contrast is never compromised by glass effects
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(8.dp)
          .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = BlueNeon, spotColor = ElectricBlue)
          .clip(RoundedCornerShape(22.dp))
          .background(PureWhite)
          .padding(10.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (qrBitmap != null) {
        Image(
          bitmap = qrBitmap.asImageBitmap(),
          contentDescription = "Official Member QR Code",
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize().testTag("official_qr_bitmap"),
        )
        if (showCenterFlameBadge) {
          Box(
            modifier =
              Modifier.size(size * 0.16f)
                .clip(RoundedCornerShape(8.dp))
                .background(PureWhite)
                .padding(2.dp),
            contentAlignment = Alignment.Center,
          ) {
            OmniChurch3DFlameIcon(size = size * 0.14f)
          }
        }
      } else {
        CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(36.dp))
      }
    }

    // Glowing Electric-Blue 3D Corner Brackets
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val bracketLen = w * 0.23f
      val stroke = 4.2.dp.toPx()
      val cornerRad = 22.dp.toPx()
      val pad = 3.dp.toPx()

      val bracketBrush = Brush.linearGradient(listOf(BlueNeon, ElectricBlue, BlueNeon))

      // Top-Left Bracket
      val tlPath =
        Path().apply {
          moveTo(pad, pad + bracketLen)
          lineTo(pad, pad + cornerRad)
          quadraticTo(pad, pad, pad + cornerRad, pad)
          lineTo(pad + bracketLen, pad)
        }
      drawPath(tlPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Top-Right Bracket
      val trPath =
        Path().apply {
          moveTo(w - pad - bracketLen, pad)
          lineTo(w - pad - cornerRad, pad)
          quadraticTo(w - pad, pad, w - pad, pad + cornerRad)
          lineTo(w - pad, pad + bracketLen)
        }
      drawPath(trPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Bottom-Left Bracket
      val blPath =
        Path().apply {
          moveTo(pad, h - pad - bracketLen)
          lineTo(pad, h - pad - cornerRad)
          quadraticTo(pad, h - pad, pad + cornerRad, h - pad)
          lineTo(pad + bracketLen, h - pad)
        }
      drawPath(blPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      // Bottom-Right Bracket
      val brPath =
        Path().apply {
          moveTo(w - pad - bracketLen, h - pad)
          lineTo(w - pad - cornerRad, h - pad)
          quadraticTo(w - pad, h - pad, w - pad, h - pad - cornerRad)
          lineTo(w - pad, h - pad - bracketLen)
        }
      drawPath(brPath, bracketBrush, style = Stroke(width = stroke, cap = StrokeCap.Round))

      if (showLaserLine) {
        val y = h * laserProgress
        drawLine(
          brush =
            Brush.horizontalGradient(
              listOf(Color.Transparent, SuccessGreen, PureWhite, SuccessGreen, Color.Transparent)
            ),
          start = Offset(w * 0.08f, y),
          end = Offset(w * 0.92f, y),
          strokeWidth = 3.dp.toPx(),
          cap = StrokeCap.Round,
        )
      }
    }
  }
}

/**
 * 19. GlassBottomNavigation — Ultra-premium floating 3D glass bottom navigation bar with active
 *     blue pill indicator and central glowing neon QR orb button (reproducing the reference image).
 */
@Composable
fun rememberFeedChromeNestedScrollConnection(
  listState: LazyListState,
  onChromeVisibilityChange: (Boolean) -> Unit,
): NestedScrollConnection {
  val isAtTop by remember(listState) {
    derivedStateOf {
      listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
    }
  }
  LaunchedEffect(isAtTop) {
    if (isAtTop) {
      onChromeVisibilityChange(true)
    }
  }
  return remember(listState, onChromeVisibilityChange) {
    var accumulatedDy = 0f
    object : NestedScrollConnection {
      override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        val dy = consumed.y
        if (dy == 0f) return Offset.Zero

        accumulatedDy =
          if ((accumulatedDy > 0f && dy < 0f) || (accumulatedDy < 0f && dy > 0f)) {
            dy
          } else {
            (accumulatedDy + dy).coerceIn(-120f, 120f)
          }

        val currentlyAtTop =
          listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 24
        if (currentlyAtTop) {
          onChromeVisibilityChange(true)
          accumulatedDy = 0f
        } else if (accumulatedDy < -14f) {
          onChromeVisibilityChange(false)
        } else if (accumulatedDy > 8f) {
          onChromeVisibilityChange(true)
        }
        return Offset.Zero
      }
    }
  }
}

enum class OmniNavTab {
  HOME,
  SERVICES,
  MEMBER_CARD_OR_SCANNER,
  NOTIFICATIONS,
  PROFILE,
}

@Composable
fun GlassBottomNavigation(
  selectedTab: OmniNavTab,
  unreadNotificationsCount: Int,
  onTabSelected: (OmniNavTab) -> Unit,
  onCenterQrOrbClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 14.dp, vertical = 8.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    // Glass pill bar
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .height(68.dp)
          .shadow(
            elevation = 20.dp,
            shape = RoundedCornerShape(28.dp),
            ambientColor = ElectricBlue,
            spotColor = DeepNavy,
          )
          .clip(RoundedCornerShape(28.dp))
          .background(
            Brush.verticalGradient(
              listOf(
                Color(0xE6152F61),
                Color(0xF2081630),
                Color(0xFA040C1C),
              )
            )
          )
          .border(
            1.3.dp,
            Brush.linearGradient(
              listOf(
                GlassWhite.copy(alpha = 0.50f),
                ElectricBlue.copy(alpha = 0.35f),
                BrandGold.copy(alpha = 0.25f),
              )
            ),
            RoundedCornerShape(28.dp),
          )
          .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      BottomNavTabItem(
        label = stringResource(R.string.nav_home),
        icon = Icons.Default.Home,
        isSelected = selectedTab == OmniNavTab.HOME,
        onClick = { onTabSelected(OmniNavTab.HOME) },
        testTag = "nav_tab_home",
      )
      BottomNavTabItem(
        label = stringResource(R.string.nav_services),
        icon = Icons.Default.CalendarMonth,
        isSelected = selectedTab == OmniNavTab.SERVICES,
        onClick = { onTabSelected(OmniNavTab.SERVICES) },
        testTag = "nav_tab_services",
      )

      // Spacer for center floating QR Orb
      Spacer(modifier = Modifier.width(64.dp))

      BottomNavTabItem(
        label = stringResource(R.string.nav_notifications),
        icon = Icons.Default.Notifications,
        isSelected = selectedTab == OmniNavTab.NOTIFICATIONS,
        badgeCount = unreadNotificationsCount,
        onClick = { onTabSelected(OmniNavTab.NOTIFICATIONS) },
        testTag = "nav_tab_notifications",
      )
      BottomNavTabItem(
        label = stringResource(R.string.nav_profile),
        icon = Icons.Default.Person,
        isSelected = selectedTab == OmniNavTab.PROFILE,
        onClick = { onTabSelected(OmniNavTab.PROFILE) },
        testTag = "nav_tab_profile",
      )
    }

    // Center Floating 3D Neon QR Orb
    Box(
      modifier =
        Modifier.offset(y = (-10).dp)
          .size(66.dp)
          .shadow(
            elevation = 22.dp,
            shape = CircleShape,
            ambientColor = BlueNeon,
            spotColor = ElectricBlue,
          )
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(BlueNeon, ElectricBlue, RoyalBlue)
            )
          )
          .border(
            width = 2.6.dp,
            brush = Brush.verticalGradient(listOf(PureWhite, BlueNeon, ElectricBlue)),
            shape = CircleShape,
          )
          .clickable(onClick = onCenterQrOrbClick)
          .testTag("nav_center_qr_orb"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.QrCode2,
        contentDescription = stringResource(R.string.nav_qr_center),
        tint = PureWhite,
        modifier = Modifier.size(32.dp),
      )
    }
  }
}

@Composable
private fun BottomNavTabItem(
  label: String,
  icon: ImageVector,
  isSelected: Boolean,
  badgeCount: Int = 0,
  onClick: () -> Unit,
  testTag: String,
) {
  val bgMod =
    if (isSelected) {
      Modifier.shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = BlueNeon)
        .clip(RoundedCornerShape(20.dp))
        .background(
          Brush.verticalGradient(
            listOf(Color(0xFF22ACFF), ElectricBlue, RoyalBlue)
          )
        )
        .border(1.1.dp, PureWhite.copy(alpha = 0.78f), RoundedCornerShape(20.dp))
    } else {
      Modifier.clip(RoundedCornerShape(20.dp))
    }

  Box(
    modifier =
      bgMod
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 7.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Box {
        Icon(
          imageVector = icon,
          contentDescription = label,
          tint = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.78f),
          modifier = Modifier.size(22.dp),
        )
        if (badgeCount > 0) {
          GlassNotificationBadge(
            count = badgeCount,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
        color = if (isSelected) PureWhite else GlassWhite.copy(alpha = 0.80f),
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        maxLines = 1,
      )
    }
  }
}
