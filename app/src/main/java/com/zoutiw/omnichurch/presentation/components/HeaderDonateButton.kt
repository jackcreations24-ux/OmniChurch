package com.zoutiw.omnichurch.presentation.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.zoutiw.omnichurch.R
import com.zoutiw.omnichurch.core.donations.DeveloperDonationConfig
import com.zoutiw.omnichurch.ui.theme.BrandGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compact glossy gold Donate image button for the app header.
 * - Replaces the small header notification bell with zero extra card box, border, or text wrapper.
 * - Runs a GPU-only periodic shake animation (`graphicsLayer`) synchronized with Firestore config.
 * - Automatically respects Android reduced-motion accessibility settings.
 */
@Composable
fun HeaderDonateButton(
  donationConfig: DeveloperDonationConfig,
  currentLanguageCode: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!donationConfig.enabled) return

  val context = LocalContext.current
  val isReducedMotion =
    remember(context) {
      runCatching {
        Settings.Global.getFloat(
          context.contentResolver,
          Settings.Global.ANIMATOR_DURATION_SCALE,
          1f,
        ) == 0f
      }.getOrDefault(false)
    }

  val shakeRotation = remember { Animatable(0f) }
  val shakeScale = remember { Animatable(1f) }

  LaunchedEffect(
    donationConfig.enableShakeAnimation,
    donationConfig.shakeIntervalSeconds,
    isReducedMotion,
  ) {
    shakeRotation.snapTo(0f)
    shakeScale.snapTo(1f)
    if (!donationConfig.enableShakeAnimation || isReducedMotion) return@LaunchedEffect

    val intervalMs =
      donationConfig.shakeIntervalSeconds
        .coerceIn(
          DeveloperDonationConfig.MIN_SHAKE_INTERVAL_SECONDS,
          DeveloperDonationConfig.MAX_SHAKE_INTERVAL_SECONDS,
        ) * 1000L

    while (true) {
      delay(intervalMs)
      val scaleJob =
        launch {
          shakeScale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
          )
          shakeScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 310, easing = FastOutSlowInEasing),
          )
        }

      // Smooth ~550ms gentle attention wiggle: 0 -> -5 -> 5 -> -3 -> 2 -> 0
      shakeRotation.animateTo(-5f, tween(durationMillis = 90, easing = FastOutSlowInEasing))
      shakeRotation.animateTo(5f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
      shakeRotation.animateTo(-3f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
      shakeRotation.animateTo(2f, tween(durationMillis = 110, easing = FastOutSlowInEasing))
      shakeRotation.animateTo(0f, tween(durationMillis = 130, easing = FastOutSlowInEasing))
      scaleJob.join()
    }
  }

  val contentDesc =
    when (currentLanguageCode.trim().lowercase()) {
      "en" -> "Donate"
      "fr" -> "Faire un don"
      "es" -> "Donar"
      else -> "Fè yon donasyon"
    }

  val interactionSource = remember { MutableInteractionSource() }

  Box(
    modifier =
      modifier
        .heightIn(min = 44.dp)
        .clip(RoundedCornerShape(50))
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(bounded = true, color = BrandGold),
          role = Role.Button,
          onClick = onClick,
        )
        .padding(horizontal = 2.dp, vertical = 4.dp)
        .testTag("header_donate_button"),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.btn_donate_gold),
      contentDescription = contentDesc,
      contentScale = ContentScale.Fit,
      modifier =
        Modifier
          .height(34.dp)
          .wrapContentWidth()
          .graphicsLayer {
            rotationZ = shakeRotation.value
            scaleX = shakeScale.value
            scaleY = shakeScale.value
          },
    )
  }
}
