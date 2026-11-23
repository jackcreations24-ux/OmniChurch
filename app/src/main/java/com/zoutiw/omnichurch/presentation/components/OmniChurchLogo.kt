package com.zoutiw.omnichurch.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoutiw.omnichurch.R

enum class LogoVariant {
  Full,
  Icon,
}

private val RoyalNavyStart = Color(0xFF07152F)
private val DeepSlateEnd = Color(0xFF0F172A)
private val LiquidGoldBadge = Color(0xFFFBBF24)
private val ElectricCyanIndicator = Color(0xFF38BDF8)

/**
 * Official OmniChurch PRO Brand Logo Composable.
 *
 * Renders the authentic transparent PNG assets directly onto the surface with zero artificial
 * framing, borders, drop-shadows, glowing halos, or animation overlays.
 * - [LogoVariant.Full]: 1293x1217 full flame + wordmark (`R.drawable.omnichurch_logo`)
 * - [LogoVariant.Icon]: 1254x1254 gold-framed squircle flame mark (`R.drawable.omnichurch_icon`)
 */
@Composable
fun OmniChurchLogo(
  variant: LogoVariant,
  modifier: Modifier = Modifier,
  contentDescription: String = "OmniChurch PRO Official Logo",
) {
  val drawableRes =
    when (variant) {
      LogoVariant.Full -> R.drawable.omnichurch_logo
      LogoVariant.Icon -> R.drawable.omnichurch_icon
    }
  val naturalRatio =
    when (variant) {
      LogoVariant.Full -> 1293f / 1217f
      LogoVariant.Icon -> 1f
    }
  Image(
    painter = painterResource(id = drawableRes),
    contentDescription = contentDescription,
    contentScale = ContentScale.Fit,
    modifier = modifier.aspectRatio(naturalRatio).testTag("omnichurch_logo_${variant.name.lowercase()}"),
  )
}

/**
 * Compact Liquid Gold PRO badge paired cleanly beside or beneath the official OmniChurch logo.
 */
@Composable
fun OmniChurchProBadge(
  modifier: Modifier = Modifier,
  label: String = "PRO",
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(50))
        .background(LiquidGoldBadge)
        .padding(horizontal = 10.dp, vertical = 2.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
      color = RoyalNavyStart,
      fontWeight = FontWeight.ExtraBold,
    )
  }
}

/**
 * Official Branded Compose Splash Screen displayed during cold-start session initialization.
 * Transitions seamlessly from the Android 12+ `Theme.App.Starting` splash screen (`#07152F`).
 */
@Composable
fun OmniChurchSplashScreen(
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(RoyalNavyStart, DeepSlateEnd)
          )
        )
        .testTag("omnichurch_splash_screen"),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 32.dp),
    ) {
      OmniChurchLogo(
        variant = LogoVariant.Full,
        modifier = Modifier.widthIn(max = 220.dp).height(180.dp),
      )
      Spacer(modifier = Modifier.height(10.dp))
      OmniChurchProBadge(label = "PRO")
      Spacer(modifier = Modifier.height(28.dp))
      CircularProgressIndicator(
        color = ElectricCyanIndicator,
        strokeWidth = 2.5.dp,
        modifier = Modifier.size(28.dp),
      )
    }
  }
}
